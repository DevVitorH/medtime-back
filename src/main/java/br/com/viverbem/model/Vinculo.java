package br.com.viverbem.model;

import jakarta.persistence.*;
import java.time.Instant;

/** O supervisor acompanha o dependente (os dois são usuários). Um par só pode existir uma vez. */
@Entity
@Table(name = "vinculo", uniqueConstraints = @UniqueConstraint(columnNames = {"dependente_id", "supervisor_id"}))
public class Vinculo {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "dependente_id") private Usuario dependente;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supervisor_id") private Usuario supervisor;
  @Column(name = "criado_em", nullable = false) private Instant criadoEm = Instant.now();

  public Long getId() { return id; }
  public Usuario getDependente() { return dependente; } public void setDependente(Usuario v) { dependente = v; }
  public Usuario getSupervisor() { return supervisor; } public void setSupervisor(Usuario v) { supervisor = v; }
  public Instant getCriadoEm() { return criadoEm; }
}
