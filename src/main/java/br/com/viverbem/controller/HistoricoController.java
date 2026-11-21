package br.com.viverbem.controller;

import static br.com.viverbem.dto.Dtos.*;

import br.com.viverbem.config.Fuso;
import br.com.viverbem.model.Usuario;
import br.com.viverbem.service.HistoricoService;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/historico")
public class HistoricoController {
  private final HistoricoService service;
  public HistoricoController(HistoricoService service) { this.service = service; }

  /** mes = yyyy-MM. "hoje" pode ser informado em ?data= (usado na demonstração com data fixa). */
  @GetMapping
  public HistoricoRes historico(@RequestAttribute("usuario") Usuario u,
      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
    LocalDate hoje = data != null ? data : Fuso.hoje();
    return service.calcular(u.getId(), mes != null ? mes : YearMonth.from(hoje), hoje);
  }
}
