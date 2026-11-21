package br.com.viverbem.controller;

import static br.com.viverbem.dto.Dtos.*;

import br.com.viverbem.config.Fuso;
import br.com.viverbem.model.Usuario;
import br.com.viverbem.service.VinculoService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/** Visão do supervisor: resumo (somente leitura) de uma pessoa que ele acompanha. */
@RestController @RequestMapping("/api/dependentes")
public class DependenteController {
  private final VinculoService service;
  public DependenteController(VinculoService service) { this.service = service; }

  @GetMapping("/{id}/resumo")
  public ResumoRes resumo(@RequestAttribute("usuario") Usuario u, @PathVariable Long id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
    return service.resumo(u, id, data != null ? data : Fuso.hoje());
  }
}
