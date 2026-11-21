package br.com.viverbem.controller;

import static br.com.viverbem.dto.Dtos.*;

import br.com.viverbem.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
  private final AuthService service;
  public AuthController(AuthService service) { this.service = service; }

  @PostMapping("/cadastrar") @ResponseStatus(HttpStatus.CREATED)
  public AuthRes cadastrar(@Valid @RequestBody CadastrarReq req) { return service.cadastrar(req); }

  @PostMapping("/entrar")
  public AuthRes entrar(@Valid @RequestBody EntrarReq req) { return service.entrar(req); }
}
