package br.com.viverbem.controller;

import static br.com.viverbem.dto.Dtos.*;

import br.com.viverbem.model.Usuario;
import br.com.viverbem.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Perfil do usuário logado ("me" = dono do token). */
@RestController @RequestMapping("/api/usuarios/me")
public class UsuarioController {
  private final UsuarioService service;
  public UsuarioController(UsuarioService service) { this.service = service; }

  @GetMapping
  public UsuarioRes dados(@RequestAttribute("usuario") Usuario u) { return service.dados(u); }

  @PutMapping
  public UsuarioRes atualizar(@RequestAttribute("usuario") Usuario u, @Valid @RequestBody PerfilReq req) {
    return service.atualizar(u, req);
  }
}
