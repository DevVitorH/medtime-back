package br.com.viverbem.service;

import br.com.viverbem.model.Usuario;
import br.com.viverbem.repository.MedicamentoRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * IA generativa (LLM via API do Google Gemini, plano gratuito): responde dúvidas usando a lista de
 * remédios do próprio usuário. A chave fica só no servidor (GEMINI_API_KEY). Nada da pergunta vai para o log.
 */
@Service
public class AssistenteService {
  static final String AVISO = "Informação de apoio. Confirme com seu médico ou farmacêutico.";
  private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/{modelo}:generateContent";

  private final RestClient http;
  private final MedicamentoRepository meds;
  private final String chave;
  private final String modelo;

  public AssistenteService(MedicamentoRepository meds,
      @Value("${gemini.api-key:}") String chave, @Value("${gemini.model}") String modelo) {
    this.meds = meds; this.chave = chave; this.modelo = modelo;
    // Tempo limite para a API não ficar presa se o Gemini demorar.
    SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
    fabrica.setConnectTimeout(Duration.ofSeconds(10));
    fabrica.setReadTimeout(Duration.ofSeconds(20));
    this.http = RestClient.builder().requestFactory(fabrica).build();
  }

  public String perguntar(Usuario u, String pergunta) {
    if (chave.isBlank()) throw naoConfigurado();
    String lista = meds.findByUsuarioIdAndAtivoTrueOrderByHorario(u.getId()).stream()
        .map(m -> m.getNome() + " (" + m.getDose() + ", às " + m.getHorario() + ")").collect(Collectors.joining("; "));
    String sistema = "Você é o assistente do aplicativo ViverBem, para pessoas com 50 anos ou mais. "
        + "Responda em português do Brasil, em no máximo 3 frases curtas, com palavras simples e sem termos técnicos. "
        + "Nunca faça diagnóstico e nunca sugira mudar doses ou parar remédios: nesses casos, oriente a falar com o médico ou farmacêutico. "
        + "Remédios do usuário: " + (lista.isBlank() ? "nenhum cadastrado" : lista) + ".";
    // Formato do generateContent: instrução de sistema + a pergunta do usuário.
    Map<String, Object> corpo = Map.of(
        "systemInstruction", Map.of("parts", List.of(Map.of("text", sistema))),
        "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", pergunta)))),
        "generationConfig", Map.of("maxOutputTokens", 1024));
    Map<?, ?> r;
    try {
      r = http.post().uri(URL, modelo)
          .header("x-goog-api-key", chave) // no cabeçalho, para a chave não aparecer em URL nem em log
          .contentType(MediaType.APPLICATION_JSON).body(corpo).retrieve().body(Map.class);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 429) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "O assistente recebeu muitas perguntas agora. Tente de novo mais tarde.");
      if (e.getStatusCode().value() == 400 || e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403
          || e.getStatusCode().value() == 404) throw naoConfigurado(); // chave ou modelo inválido
      throw indisponivel();
    } catch (RestClientException e) { // rede, tempo esgotado, erro 5xx do Gemini
      throw indisponivel();
    }
    String texto = juntarTextos(r);
    if (texto.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
        "O assistente não conseguiu responder. Tente perguntar de outro jeito.");
    return texto + "\n\n" + AVISO;
  }

  /** Junta o texto de todas as partes de todas as respostas (candidates[].content.parts[].text). */
  private static String juntarTextos(Map<?, ?> r) {
    if (r == null || !(r.get("candidates") instanceof List<?> candidatos)) return ""; // ex.: pergunta bloqueada
    StringBuilder sb = new StringBuilder();
    for (Object c : candidatos) {
      if (!(c instanceof Map<?, ?> cand) || !(cand.get("content") instanceof Map<?, ?> conteudo)) continue;
      if (!(conteudo.get("parts") instanceof List<?> partes)) continue;
      for (Object p : partes) {
        if (p instanceof Map<?, ?> parte && parte.get("text") instanceof String t
            && !Boolean.TRUE.equals(parte.get("thought"))) sb.append(t);
      }
      break; // pedimos uma resposta só: usa o primeiro candidato que tiver conteúdo
    }
    return sb.toString().trim();
  }

  private static ResponseStatusException naoConfigurado() {
    return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
        "O assistente ainda não foi configurado no servidor. Por enquanto, fale com seu médico ou farmacêutico.");
  }

  private static ResponseStatusException indisponivel() {
    return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "O assistente não conseguiu responder agora. Tente de novo.");
  }
}
