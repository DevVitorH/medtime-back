package br.com.viverbem.config;

import br.com.viverbem.dto.Dtos.ErroRes;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/** Todos os erros saem como {"mensagem": "..."} — é o que o app mostra ao usuário. */
@RestControllerAdvice
public class ErroHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErroRes> validacao(MethodArgumentNotValidException e) {
    String msg = e.getBindingResult().getFieldErrors().stream().findFirst()
        .map(f -> f.getDefaultMessage()).orElse("Dados inválidos.");
    return ResponseEntity.badRequest().body(new ErroRes(msg));
  }

  @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
  ResponseEntity<ErroRes> requisicaoInvalida(Exception e) {
    return ResponseEntity.badRequest().body(new ErroRes("Requisição inválida."));
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ErroRes> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode()).body(new ErroRes(e.getReason()));
  }
}
