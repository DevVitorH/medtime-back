package br.com.viverbem.service;

import static br.com.viverbem.dto.Dtos.MedicamentoRes;

import br.com.viverbem.config.Fuso;
import br.com.viverbem.model.Usuario;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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
 * IA generativa (LLM via API do Google Gemini, plano gratuito): responde dúvidas sobre medicamentos e sobre a agenda de hoje.
 * O contexto (data, hora, remédios e próximo remédio) é montado aqui no servidor, no fuso America/Fortaleza,
 * com a mesma lista da tela inicial. A chave fica só no servidor (GEMINI_API_KEY). Nada da pergunta vai para o log.
 */
@Service
public class AssistenteService {
  static final String AVISO = "Informação de apoio. Confirme com seu médico ou farmacêutico.";
  private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/{modelo}:generateContent";
  private static final Locale PT_BR = Locale.forLanguageTag("pt-BR"); // funciona no Java 17
  private static final DateTimeFormatter AGORA = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy, HH:mm", PT_BR);
  // Prompt de sistema. A agenda vem só dos dados do servidor; informação geral de remédio vem do conhecimento do modelo.
  private static final String REGRAS = """
      Você é o Assistente MedTime, que ajuda pessoas com mais de 50 anos a entender seus remédios. \
      Fale em português simples, com frases curtas e sem termos técnicos difíceis.

      ESCOPO: responda apenas sobre medicamentos (para que servem, como agem em linhas gerais, cuidados comuns, \
      efeitos colaterais comuns, interações comuns, como tomar em geral, como com água ou com comida, \
      o que a bula costuma orientar se esquecer uma dose) e sobre a agenda de remédios da pessoa. \
      Se a pergunta for de outro assunto, diga em uma frase que só ajuda com medicamentos e convide a perguntar sobre eles.

      FONTES:
      1) Agenda da pessoa (horários, o que já tomou, próximo remédio): use SOMENTE os dados fornecidos. \
      Nunca invente horário, dose ou remédio.
      2) Informação geral sobre um medicamento: use conhecimento geral confiável (bula e órgãos de saúde). \
      Se não conhecer o remédio ou tiver dúvida, diga isso claramente em vez de inventar.

      FORMATO: no máximo 4 frases curtas. A primeira já responde a pergunta \
      (ex.: "A losartana é usada para controlar a pressão alta."). Depois, 1 ou 2 cuidados importantes. \
      Sem listas longas e sem markdown.

      SEGURANÇA: não diagnostique. Não diga se a pessoa deve mudar a dose, parar, trocar ou combinar remédios: \
      nesses casos oriente falar com o médico ou farmacêutico. Não indique doses para a pessoa. \
      Se ela descrever sintoma forte, possível overdose, reação alérgica, falta de ar, dor no peito ou desmaio, \
      oriente procurar atendimento de urgência ou ligar 192 (SAMU). \
      Lembre de conversar com o médico ou farmacêutico de forma natural e curta, sem repetir sempre a mesma frase.

      EXEMPLOS:
      Pergunta: "Para que serve a losartana?"
      Resposta: "A losartana é usada para controlar a pressão alta e ajuda a proteger o coração e os rins. \
      Tome sempre no horário combinado com seu médico e avise se sentir tontura. Não pare nem mude a dose sem falar com ele."
      Pergunta: "Posso parar de tomar se a pressão normalizou?"
      Resposta: "A pressão normal pode ser sinal de que o remédio está funcionando, então não pare por conta própria. \
      Converse com seu médico antes de qualquer mudança."
      Pergunta: "Qual a capital da França?"
      Resposta: "Eu só ajudo com remédios e horários. Quer saber algo sobre seus medicamentos?"
      """;

  private final RestClient http;
  private final MedicamentoService medicamentos;
  private final String chave;
  private final String modelo;

  public AssistenteService(MedicamentoService medicamentos,
      @Value("${gemini.api-key:}") String chave, @Value("${gemini.model}") String modelo) {
    this.medicamentos = medicamentos; this.chave = chave; this.modelo = modelo;
    // Tempo limite para a API não ficar presa se o Gemini demorar.
    SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
    fabrica.setConnectTimeout(Duration.ofSeconds(10));
    fabrica.setReadTimeout(Duration.ofSeconds(20));
    this.http = RestClient.builder().requestFactory(fabrica).build();
  }

  public String perguntar(Usuario u, String pergunta) {
    if (chave.isBlank()) throw naoConfigurado();
    // Mesma lista da tela inicial: só os remédios que valem hoje, já com o status calculado.
    LocalDateTime agora = Fuso.agora();
    List<MedicamentoRes> deHoje = medicamentos.listar(u, agora.toLocalDate());
    String sistema = REGRAS + "\nAgenda da pessoa (dados fornecidos):\n" + montarContexto(agora, deHoje);
    // Formato do generateContent: instrução de sistema + a pergunta do usuário.
    Map<String, Object> corpo = Map.of(
        "systemInstruction", Map.of("parts", List.of(Map.of("text", sistema))),
        "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", pergunta)))),
        // Temperatura baixa: respostas mais consistentes. O limite de saída inclui os tokens de "raciocínio"
        // do modelo (no flash-lite o raciocínio padrão é mínimo, então 400 basta para até 4 frases).
        "generationConfig", Map.of("temperature", 0.3, "maxOutputTokens", 400));
    Map<?, ?> r;
    try {
      r = http.post().uri(URL, modelo)
          .header("x-goog-api-key", chave) // no cabeçalho, para a chave não aparecer em URL nem em log
          .contentType(MediaType.APPLICATION_JSON).body(corpo).retrieve().body(Map.class);
    } catch (HttpClientErrorException e) {
      int status = e.getStatusCode().value();
      if (status == 429) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "O assistente recebeu muitas perguntas agora. Tente de novo mais tarde.");
      if (status == 400 || status == 401 || status == 403 || status == 404) throw naoConfigurado(); // chave ou modelo inválido
      throw indisponivel();
    } catch (RestClientException e) { // rede, tempo esgotado, erro 5xx do Gemini
      throw indisponivel();
    }
    String texto = juntarTextos(r);
    if (texto.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
        "O assistente não conseguiu responder. Tente perguntar de outro jeito.");
    return texto + "\n\n" + AVISO;
  }

  /**
   * Texto pronto que vai para a IA. Ex.: "Agora: sábado, 21/11/2026, 16:34.
   * Remédios de hoje: 08:00 Losartana, 20mg (tomado às 08:05). Próximo remédio: ...".
   * A lista já vem filtrada (só hoje) e em ordem de horário.
   */
  static String montarContexto(LocalDateTime agora, List<MedicamentoRes> deHoje) {
    String lista = deHoje.isEmpty() ? "nenhum remédio agendado para hoje"
        : deHoje.stream().map(m -> m.horario() + " " + nomeEDose(m) + " (" + situacao(m) + ")")
            .collect(Collectors.joining("; "));
    return "Agora: " + AGORA.format(agora) + ".\n"
        + "Remédios de hoje: " + lista + ".\n"
        + "Próximo remédio: " + proximo(deHoje) + ".";
  }

  /**
   * Próximo remédio: o primeiro PENDENTE em ordem de horário (inclui o que ainda está dentro da tolerância);
   * se não houver, o ATRASADO mais antigo; se não houver nada, "nenhum remédio pendente hoje".
   */
  static String proximo(List<MedicamentoRes> deHoje) {
    return deHoje.stream().filter(m -> "PENDENTE".equals(m.status())).findFirst()
        .map(m -> nomeEDose(m) + " às " + m.horario())
        .or(() -> deHoje.stream().filter(m -> "ATRASADO".equals(m.status())).findFirst()
            .map(m -> nomeEDose(m) + ", que era às " + m.horario() + " e está atrasado"))
        .orElse("nenhum remédio pendente hoje");
  }

  private static String nomeEDose(MedicamentoRes m) { return m.nome() + ", " + m.dose(); }

  /** Status em palavras (usa o status já calculado por MedicamentoService.calcularStatus). */
  private static String situacao(MedicamentoRes m) {
    String hora = m.tomadoEm() == null ? "" : " às " + m.tomadoEm();
    return switch (m.status()) {
      case "TOMADO" -> "tomado" + hora;
      case "TOMADO_COM_ATRASO" -> "tomado" + hora + ", com atraso";
      case "ATRASADO" -> "atrasado, ainda não tomado";
      default -> "pendente";
    };
  }

  /** Junta o texto de todas as partes da resposta (candidates[].content.parts[].text). */
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
