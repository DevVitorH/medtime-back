package br.com.viverbem.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Código de uso único que o dependente gera para alguém virar seu supervisor (vale 30 minutos). */
@Entity @Table(name = "codigo_convite")
public class CodigoConvite {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "dependente_id") private Usuario dependente;
  @Column(nullable = false, unique = true, length = 6) private String codigo;
  @Column(name = "expira_em", nullable = false) private Instant expiraEm;
  @Column(name = "usado_em") private Instant usadoEm; // nulo = ainda não usado

  public Long getId() { return id; }
  public Usuario getDependente() { return dependente; } public void setDependente(Usuario v) { dependente = v; }
  public String getCodigo() { return codigo; } public void setCodigo(String v) { codigo = v; }
  public Instant getExpiraEm() { return expiraEm; } public void setExpiraEm(Instant v) { expiraEm = v; }
  public Instant getUsadoEm() { return usadoEm; } public void setUsadoEm(Instant v) { usadoEm = v; }
}
