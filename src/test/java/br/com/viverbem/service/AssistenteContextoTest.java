package br.com.viverbem.service;

import static br.com.viverbem.dto.Dtos.MedicamentoRes;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Confere o texto que o assistente envia para a IA, com dados fictícios (sem banco e sem chamar a IA).
 * O contexto é impresso no console do teste (nunca no log da API). Rodar: mvn test
 */
class AssistenteContextoTest {
  private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 21, 16, 34); // sábado
  private static final List<String> TODOS = List.of("SEG", "TER", "QUA", "QUI", "SEX", "SAB", "DOM");

  private static MedicamentoRes med(long id, String nome, String dose, String horario, String status, String tomadoEm) {
    return new MedicamentoRes(id, nome, dose, horario, "", tomadoEm != null, TODOS, LocalDate.of(2026, 11, 21), status, tomadoEm);
  }

  private static String contexto(String caso, List<MedicamentoRes> deHoje) {
    String texto = AssistenteService.montarContexto(AGORA, deHoje);
    System.out.println("\n--- " + caso + " ---\n" + texto);
    return texto;
  }

  @Test
  void proximoEhOPendenteDepoisDosTomados() {
    String t = contexto("Losartana pendente, outros tomados", List.of(
        med(1, "a", "1", "08:00", "TOMADO_COM_ATRASO", "13:15"),
        med(2, "2", "2", "09:00", "TOMADO_COM_ATRASO", "15:25"),
        med(3, "Losartana", "20mg", "17:00", "PENDENTE", null)));
    assertThat(t).startsWith("Agora: sábado, 21/11/2026, 16:34.");
    assertThat(t).contains("08:00 a, 1 (tomado às 13:15, com atraso)");
    assertThat(t).contains("17:00 Losartana, 20mg (pendente)");
    assertThat(t).endsWith("Próximo remédio: Losartana, 20mg às 17:00.");
  }

  @Test
  void nenhumRemedioHoje() {
    String t = contexto("Nenhum remédio hoje", List.of());
    assertThat(t).contains("Remédios de hoje: nenhum remédio agendado para hoje.");
    assertThat(t).endsWith("Próximo remédio: nenhum remédio pendente hoje.");
  }

  @Test
  void todosTomados() {
    String t = contexto("Todos tomados", List.of(
        med(1, "Losartana", "20mg", "08:00", "TOMADO", "08:05"),
        med(2, "Metformina", "850mg", "12:00", "TOMADO", "12:10")));
    assertThat(t).contains("08:00 Losartana, 20mg (tomado às 08:05)");
    assertThat(t).endsWith("Próximo remédio: nenhum remédio pendente hoje.");
  }

  @Test
  void semPendenteOProximoEhOAtrasadoMaisAntigo() {
    String t = contexto("Só atrasados", List.of(
        med(1, "Losartana", "20mg", "08:00", "ATRASADO", null),
        med(2, "Metformina", "850mg", "12:00", "ATRASADO", null)));
    assertThat(t).contains("08:00 Losartana, 20mg (atrasado, ainda não tomado)");
    assertThat(t).endsWith("Próximo remédio: Losartana, 20mg, que era às 08:00 e está atrasado.");
  }

  @Test
  void pendenteDentroDaToleranciaVemAntesDoAtrasado() {
    // 16:34: o das 16:20 ainda está na tolerância (PENDENTE); o das 08:00 está ATRASADO.
    String t = contexto("Pendente na tolerância + atrasado", List.of(
        med(1, "Losartana", "20mg", "08:00", "ATRASADO", null),
        med(2, "Vitamina D", "1 gota", "16:20", "PENDENTE", null),
        med(3, "Metformina", "850mg", "20:00", "PENDENTE", null)));
    assertThat(t).endsWith("Próximo remédio: Vitamina D, 1 gota às 16:20.");
  }
}
