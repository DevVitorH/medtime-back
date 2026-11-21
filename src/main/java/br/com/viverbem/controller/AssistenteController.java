package br.com.viverbem.controller;

import static br.com.viverbem.dto.Dtos.*;

import br.com.viverbem.model.Usuario;
import br.com.viverbem.service.AssistenteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/assistente")
public class AssistenteController {
  private final AssistenteService service;
  public AssistenteController(AssistenteService service) { this.service = service; }

  @PostMapping("/perguntar")
  public PerguntaRes perguntar(@RequestAttribute("usuario") Usuario u, @Valid @RequestBody PerguntaReq req) {
    return new PerguntaRes(service.perguntar(u, req.pergunta()));
  }
}
