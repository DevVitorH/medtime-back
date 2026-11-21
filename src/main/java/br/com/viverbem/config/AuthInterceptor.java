package br.com.viverbem.config;

import br.com.viverbem.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Exige "Authorization: Bearer <token>" válido (não expirado) e disponibiliza o usuário logado ao controller. */
@Component
public class AuthInterceptor implements HandlerInterceptor {
  private final UsuarioRepository usuarios;
  public AuthInterceptor(UsuarioRepository usuarios) { this.usuarios = usuarios; }

  @Override
  public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
    if ("OPTIONS".equals(req.getMethod())) return true; // pré-voo do CORS
    String h = req.getHeader("Authorization");
    if (h != null && h.startsWith("Bearer ")) {
      var u = usuarios.findByToken(h.substring(7).trim());
      // Token sem data ou com data no passado = sessão expirada.
      boolean valido = u.isPresent() && u.get().getTokenExpiraEm() != null
          && u.get().getTokenExpiraEm().isAfter(Instant.now());
      if (valido) { req.setAttribute("usuario", u.get()); return true; }
    }
    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    res.setContentType("application/json;charset=UTF-8");
    res.getWriter().write("{\"mensagem\":\"Sessão expirada. Entre novamente.\"}");
    return false;
  }
}
