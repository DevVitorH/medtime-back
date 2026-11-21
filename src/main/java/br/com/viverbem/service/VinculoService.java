package br.com.viverbem.service;

import static br.com.viverbem.dto.Dtos.*;

import br.com.viverbem.model.CodigoConvite;
import br.com.viverbem.model.Usuario;
import br.com.viverbem.model.Vinculo;
import br.com.viverbem.repository.CodigoConviteRepository;
import br.com.viverbem.repository.UsuarioRepository;
import br.com.viverbem.repository.VinculoRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Supervisão entre usuários: o dependente gera um código, o supervisor digita o código e o vínculo é criado.
 * Os dados de saúde do dependente só são mostrados a quem tem vínculo com ele.
 */
@Service
public class VinculoService {
  private static final String LETRAS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem O, 0, I e 1 (confundem)
  private static final int TAMANHO_CODIGO = 6;
  static final Duration VALIDADE_CODIGO = Duration.ofMinutes(30);
  static final int MAX_ERROS = 5;
  static final Duration JANELA_ERROS = Duration.ofMinutes(15);
  static final Duration TEMPO_BLOQUEIO = Duration.ofMinutes(15);
  static final String ERRO_BLOQUEIO = "Muitas tentativas. Tente de novo em alguns minutos.";

  private final SecureRandom sorteio = new SecureRandom(); // imprevisível, próprio para códigos de acesso
  private final CodigoConviteRepository codigos;
  private final VinculoRepository vinculos;
  private final UsuarioRepository usuarios;
  private final MedicamentoService medicamentoService;
  private final HistoricoService historicoService;

  public VinculoService(CodigoConviteRepository codigos, VinculoRepository vinculos, UsuarioRepository usuarios,
      MedicamentoService medicamentoService, HistoricoService historicoService) {
    this.codigos = codigos; this.vinculos = vinculos; this.usuarios = usuarios;
    this.medicamentoService = medicamentoService; this.historicoService = historicoService;
  }

  /** Gera um código novo para o usuário logado ser acompanhado. O código anterior ainda não usado deixa de valer. */
  @Transactional
  public CodigoRes gerarCodigo(Usuario u) {
    Instant agora = Instant.now();
    codigos.findByDependenteIdAndUsadoEmIsNullAndExpiraEmAfter(u.getId(), agora)
        .forEach(antigo -> antigo.setExpiraEm(agora));
    String codigo;
    do { codigo = sortearCodigo(); } while (codigos.existsByCodigo(codigo));
    CodigoConvite c = new CodigoConvite();
    c.setDependente(u); c.setCodigo(codigo); c.setExpiraEm(agora.plus(VALIDADE_CODIGO));
    codigos.save(c);
    return new CodigoRes(codigo, c.getExpiraEm());
  }

  /**
   * O usuário logado digita o código e vira supervisor do dono do código.
   * noRollbackFor: a contagem de códigos errados precisa ser gravada mesmo quando termina com erro.
   */
  @Transactional(noRollbackFor = ResponseStatusException.class)
  public PessoaRes conectar(Usuario u, String digitado) {
    Instant agora = Instant.now();
    if (u.getCodigoBloqueadoAte() != null && u.getCodigoBloqueadoAte().isAfter(agora))
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, ERRO_BLOQUEIO);

    String codigo = digitado.replaceAll("\\s", "").toUpperCase(); // aceita minúsculas e espaços
    CodigoConvite c = codigos.findByCodigo(codigo).orElse(null);
    if (c == null) throw codigoErrado(u, agora, "Código inválido. Confira as letras e os números.");
    if (c.getUsadoEm() != null) throw codigoErrado(u, agora, "Este código já foi usado. Peça um novo.");
    if (!c.getExpiraEm().isAfter(agora)) throw codigoErrado(u, agora, "Este código expirou. Peça um novo.");

    Usuario dependente = c.getDependente();
    if (dependente.getId().equals(u.getId()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você não pode ser supervisor de si mesmo.");
    if (vinculos.existsByDependenteIdAndSupervisorId(dependente.getId(), u.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Vocês já estão conectados.");

    c.setUsadoEm(agora); // uso único
    Vinculo v = new Vinculo();
    v.setDependente(dependente); v.setSupervisor(u);
    vinculos.save(v);
    u.setTentativasCodigo(0); u.setPrimeiraFalhaCodigoEm(null); u.setCodigoBloqueadoAte(null);
    usuarios.save(u);
    return new PessoaRes(v.getId(), dependente.getId(), dependente.getNome());
  }

  /** Meus supervisores e as pessoas que eu acompanho. */
  @Transactional(readOnly = true)
  public VinculosRes listar(Usuario u) {
    List<PessoaRes> supervisores = vinculos.findByDependenteId(u.getId()).stream()
        .map(v -> pessoa(v, v.getSupervisor())).toList();
    List<PessoaRes> dependentes = vinculos.findBySupervisorId(u.getId()).stream()
        .map(v -> pessoa(v, v.getDependente())).toList();
    return new VinculosRes(supervisores, dependentes);
  }

  /** Qualquer um dos dois lados pode desfazer o vínculo. */
  @Transactional
  public void remover(Usuario u, Long vinculoId) {
    Vinculo v = vinculos.findById(vinculoId)
        .filter(x -> x.getDependente().getId().equals(u.getId()) || x.getSupervisor().getId().equals(u.getId()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado."));
    vinculos.delete(v);
  }

  /** Resumo do dia de um dependente. São dados de saúde: só para quem é supervisor dele (senão 403). */
  @Transactional(readOnly = true)
  public ResumoRes resumo(Usuario u, Long dependenteId, LocalDate data) {
    if (!vinculos.existsByDependenteIdAndSupervisorId(dependenteId, u.getId()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não acompanha esta pessoa.");
    Usuario dependente = usuarios.findById(dependenteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não acompanha esta pessoa."));
    List<MedicamentoRes> remedios = medicamentoService.listar(dependente, data);
    HistoricoRes historico = historicoService.calcular(dependente.getId(), YearMonth.from(data), data);
    return new ResumoRes(dependente.getNome(), data, remedios, historico.diasSeguidos(), historico.adesao());
  }

  private String sortearCodigo() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < TAMANHO_CODIGO; i++) sb.append(LETRAS.charAt(sorteio.nextInt(LETRAS.length())));
    return sb.toString();
  }

  /** Conta um código errado. Na 5ª falha dentro de 15 minutos, bloqueia por 15 minutos. */
  private ResponseStatusException codigoErrado(Usuario u, Instant agora, String mensagem) {
    boolean janelaVencida = u.getPrimeiraFalhaCodigoEm() == null
        || u.getPrimeiraFalhaCodigoEm().plus(JANELA_ERROS).isBefore(agora);
    if (janelaVencida) { u.setPrimeiraFalhaCodigoEm(agora); u.setTentativasCodigo(0); }
    u.setTentativasCodigo(u.getTentativasCodigo() + 1);
    if (u.getTentativasCodigo() >= MAX_ERROS) {
      u.setCodigoBloqueadoAte(agora.plus(TEMPO_BLOQUEIO));
      u.setTentativasCodigo(0); u.setPrimeiraFalhaCodigoEm(null);
      usuarios.save(u);
      return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, ERRO_BLOQUEIO);
    }
    usuarios.save(u);
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
  }

  private PessoaRes pessoa(Vinculo v, Usuario p) { return new PessoaRes(v.getId(), p.getId(), p.getNome()); }
}
