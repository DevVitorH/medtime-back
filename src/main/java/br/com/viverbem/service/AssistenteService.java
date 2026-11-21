package br.com.viverbem.service;

import br.com.viverbem.model.Usuario;
import br.com.viverbem.repository.MedicamentoRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/** IA generativa (LLM via API): responde dúvidas usando a lista de remédios do próprio usuário. */
@Service
public class AssistenteService {
  private final RestClient http = RestClient.create();
  private final MedicamentoRepository meds;
  private final String chave;
  private final String modelo;

  public AssistenteService(MedicamentoRepository meds,
      @Value("${anthropic.api-key:}") String chave, @Value("${anthropic.model}") String modelo) {
    this.meds = meds; this.chave = chave; this.modelo = modelo;
  }

  public String perguntar(Usuario u, String pergunta) {
    if (chave.isBlank()) return "O assistente ainda não foi configurado no servidor. Por enquanto, fale com seu médico ou farmacêutico.";
    String lista = meds.findByUsuarioIdAndAtivoTrueOrderByHorario(u.getId()).stream()
        .map(m -> m.getNome() + " (" + m.getDose() + ", às " + m.getHorario() + ")").collect(Collectors.joining("; "));
    String sistema = "Você é o assistente do aplicativo ViverBem, para pessoas com 50 anos ou mais. "
        + "Responda em português do Brasil, em no máximo 3 frases curtas, com palavras simples e sem termos técnicos. "
        + "Nunca faça diagnóstico e nunca sugira mudar doses ou parar remédios: nesses casos, oriente a falar com o médico ou farmacêutico. "
        + "Remédios do usuário: " + (lista.isBlank() ? "nenhum cadastrado" : lista) + ".";
    Map<String, Object> corpo = Map.of("model", modelo, "max_tokens", 400, "system", sistema,
        "messages", List.of(Map.of("role", "user", "content", pergunta)));
    try {
      Map<?, ?> r = http.post().uri("https://api.anthropic.com/v1/messages")
          .header("x-api-key", chave).header("anthropic-version", "2023-06-01")
          .contentType(MediaType.APPLICATION_JSON).body(corpo).retrieve().body(Map.class);
      return (String) ((Map<?, ?>) ((List<?>) r.get("content")).get(0)).get("text");
    } catch (RestClientException | NullPointerException | ClassCastException e) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "O assistente não conseguiu responder agora. Tente de novo.");
    }
  }
}
