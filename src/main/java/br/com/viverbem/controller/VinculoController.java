package br.com.viverbem.controller;

import static br.com.viverbem.dto.Dtos.*;

import br.com.viverbem.model.Usuario;
import br.com.viverbem.service.VinculoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/vinculos")
public class VinculoController {
  private final VinculoService service;
  public VinculoController(VinculoService service) { this.service = service; }

  /** Quem será acompanhado gera o código. */
  @PostMapping("/codigo") @ResponseStatus(HttpStatus.CREATED)
  public CodigoRes gerarCodigo(@RequestAttribute("usuario") Usuario u) { return service.gerarCodigo(u); }

  /** Quem vai acompanhar digita o código. */
  @PostMapping("/conectar")
  public PessoaRes conectar(@RequestAttribute("usuario") Usuario u, @Valid @RequestBody ConectarReq req) {
    return service.conectar(u, req.codigo());
  }

  @GetMapping
  public VinculosRes listar(@RequestAttribute("usuario") Usuario u) { return service.listar(u); }

  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remover(@RequestAttribute("usuario") Usuario u, @PathVariable Long id) { service.remover(u, id); }
}
