package br.com.viverbem.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Contratos JSON da API (iguais aos descritos em docs/API.md do front). */
public final class Dtos {
  private Dtos() {}

  /** Senha: só números, de 4 a 6 dígitos. */
  static final String SENHA_REGEX = "^\\d{4,6}$";
  static final String SENHA_MSG = "A senha deve ter de 4 a 6 números.";

  public record CadastrarReq(
      @NotBlank(message = "Informe seu e-mail.") @Email(message = "E-mail inválido.") String email,
      @NotBlank(message = "Informe a senha.") @Pattern(regexp = SENHA_REGEX, message = SENHA_MSG) String senha,
      @NotNull(message = "É preciso autorizar o uso de dados (LGPD).")
      @AssertTrue(message = "É preciso autorizar o uso de dados (LGPD).") Boolean aceiteLgpd) {}
  public record EntrarReq(
      @NotBlank(message = "Informe seu e-mail.") @Email(message = "E-mail inválido.") String email,
      @NotBlank(message = "Informe a senha.") @Pattern(regexp = SENHA_REGEX, message = SENHA_MSG) String senha) {}
  public record PerfilReq(
      @NotBlank(message = "Informe seu nome.") @Size(max = 100, message = "Nome muito longo.") String nome,
      @NotNull(message = "Informe sua idade.") @Min(value = 1, message = "A idade deve ser de 1 a 120.")
      @Max(value = 120, message = "A idade deve ser de 1 a 120.") Integer idade) {}

  /** nome e idade podem vir nulos logo depois do cadastro. */
  public record UsuarioRes(Long id, String nome, Integer idade, String email) {}
  public record AuthRes(String token, Instant expiraEm, UsuarioRes usuario) {}

  public record MedicamentoReq(
      @NotBlank(message = "Informe o nome do remédio.") String nome,
      @NotBlank(message = "Informe a dose.") String dose,
      @NotBlank(message = "Informe o horário.") @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Horário inválido. Use hh:mm, por exemplo 08:00.") String horario,
      String orientacao,
      @NotEmpty(message = "Escolha pelo menos um dia.")
      List<@NotNull(message = "Dia da semana inválido.")
           @Pattern(regexp = "SEG|TER|QUA|QUI|SEX|SAB|DOM", message = "Dia da semana inválido.") String> diasSemana) {}
  /**
   * diasSemana sempre na ordem SEG → DOM. status = PENDENTE | ATRASADO | TOMADO | TOMADO_COM_ATRASO
   * (calculado no servidor). tomadoEm = "HH:mm" da tomada naquela data, ou null.
   */
  public record MedicamentoRes(Long id, String nome, String dose, String horario, String orientacao, boolean tomado,
      List<String> diasSemana, LocalDate dataInicio, String status, String tomadoEm) {}

  public record DiaRes(int dia, String status) {}
  public record HistoricoRes(int diasSeguidos, int adesao, List<DiaRes> dias) {}

  public record PerguntaReq(@NotBlank(message = "Digite sua dúvida.") @Size(max = 500, message = "Pergunta muito longa.") String pergunta) {}
  public record PerguntaRes(String resposta) {}
  public record ErroRes(String mensagem) {}

  // Supervisão: vínculo entre dois usuários criado por código de convite.
  public record CodigoRes(String codigo, Instant expiraEm) {}
  public record ConectarReq(@NotBlank(message = "Digite o código.") String codigo) {}
  /** Uma pessoa do outro lado do vínculo (só o nome, para expor o mínimo de dados). */
  public record PessoaRes(Long vinculoId, Long usuarioId, String nome) {}
  public record VinculosRes(List<PessoaRes> supervisores, List<PessoaRes> dependentes) {}
  public record ResumoRes(String nome, LocalDate data, List<MedicamentoRes> medicamentos, int diasSeguidos, int adesao) {}
}
