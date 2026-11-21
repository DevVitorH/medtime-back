package br.com.viverbem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** BCrypt para guardar senhas: o banco guarda só o hash, nunca a senha. */
@Configuration
public class SenhaConfig {
  @Bean
  public BCryptPasswordEncoder senhaEncoder() { return new BCryptPasswordEncoder(); }
}
