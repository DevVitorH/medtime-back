package br.com.viverbem.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** "Hoje" sempre no fuso de Sergipe (America/Fortaleza, UTC-3), não no fuso do servidor. */
public final class Fuso {
  public static final ZoneId ZONA = ZoneId.of("America/Fortaleza");
  private Fuso() {}

  public static LocalDate hoje() { return LocalDate.now(ZONA); }
  public static LocalDateTime agora() { return LocalDateTime.now(ZONA); }
}
