package br.com.viverbem.controller;

import static br.com.viverbem.dto.Dtos.*;

import br.com.viverbem.config.Fuso;
import br.com.viverbem.model.Usuario;
import br.com.viverbem.service.MedicamentoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/medicamentos")
public class MedicamentoController {
  private final MedicamentoService service;
  public MedicamentoController(MedicamentoService service) { this.service = service; }

  @GetMapping
  public List<MedicamentoRes> listar(@RequestAttribute("usuario") Usuario u,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
    return service.listar(u, data != null ? data : Fuso.hoje());
  }

  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  public MedicamentoRes criar(@RequestAttribute("usuario") Usuario u, @Valid @RequestBody MedicamentoReq req) {
    return service.criar(u, req);
  }

  @PutMapping("/{id}")
  public MedicamentoRes atualizar(@RequestAttribute("usuario") Usuario u, @PathVariable Long id,
      @Valid @RequestBody MedicamentoReq req) {
    return service.atualizar(u, id, req);
  }

  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void excluir(@RequestAttribute("usuario") Usuario u, @PathVariable Long id) {
    service.excluir(u, id);
  }

  @PostMapping("/{id}/tomar")
  public MedicamentoRes tomar(@RequestAttribute("usuario") Usuario u, @PathVariable Long id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
    return service.tomar(u, id, data != null ? data : Fuso.hoje());
  }
}
