package br.com.viverbem.service;

import static br.com.viverbem.dto.Dtos.*;

import br.com.viverbem.model.Usuario;
import br.com.viverbem.repository.UsuarioRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Cadastro e login com e-mail + senha numérica. */
@Service
public class AuthService {
  static final int MAX_TENTATIVAS = 5;
  static final Duration TEMPO_BLOQUEIO = Duration.ofMinutes(15);
  static final Duration VALIDADE_TOKEN = Duration.ofHours(24);
  static final String ERRO_LOGIN = "E-mail ou senha incorretos.";
  static final String ERRO_BLOQUEIO = "Muitas tentativas. Tente de novo em alguns minutos.";

  private final UsuarioRepository usuarios;
  private final UsuarioService usuarioService;
  private final BCryptPasswordEncoder senhaEncoder;

  public AuthService(UsuarioRepository usuarios, UsuarioService usuarioService, BCryptPasswordEncoder senhaEncoder) {
    this.usuarios = usuarios; this.usuarioService = usuarioService; this.senhaEncoder = senhaEncoder;
  }

  @Transactional
  public AuthRes cadastrar(CadastrarReq r) {
    String email = normalizar(r.email());
    if (usuarios.findByEmail(email).isPresent())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já tem conta. Use Entrar.");
    Usuario u = new Usuario();
    u.setEmail(email);
    u.setSenhaHash(senhaEncoder.encode(r.senha()));
    u.setAceiteLgpd(true);
    return novaSessao(u);
  }

  /**
   * noRollbackFor: a contagem de senhas erradas precisa ser gravada
   * mesmo quando o método termina com erro (senão o bloqueio nunca acontece).
   */
  @Transactional(noRollbackFor = ResponseStatusException.class)
  public AuthRes entrar(EntrarReq r) {
    Usuario u = usuarios.findByEmail(normalizar(r.email()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ERRO_LOGIN));
    Instant agora = Instant.now();
    if (u.getBloqueadoAte() != null && u.getBloqueadoAte().isAfter(agora))
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, ERRO_BLOQUEIO);

    if (!senhaEncoder.matches(r.senha(), u.getSenhaHash())) {
      int falhas = u.getTentativasFalhas() + 1;
      if (falhas >= MAX_TENTATIVAS) {
        u.setTentativasFalhas(0);
        u.setBloqueadoAte(agora.plus(TEMPO_BLOQUEIO));
        usuarios.save(u);
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, ERRO_BLOQUEIO);
      }
      u.setTentativasFalhas(falhas);
      usuarios.save(u);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ERRO_LOGIN);
    }

    u.setTentativasFalhas(0);
    u.setBloqueadoAte(null);
    return novaSessao(u);
  }

  /** Gera um token novo, válido por 24 horas (o anterior deixa de valer). */
  private AuthRes novaSessao(Usuario u) {
    u.setToken(UUID.randomUUID().toString());
    u.setTokenExpiraEm(Instant.now().plus(VALIDADE_TOKEN));
    u = usuarios.save(u);
    return new AuthRes(u.getToken(), u.getTokenExpiraEm(), usuarioService.dados(u));
  }

  private String normalizar(String email) { return email.trim().toLowerCase(); }
}
