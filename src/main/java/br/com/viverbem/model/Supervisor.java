package br.com.viverbem.model;

import jakarta.persistence.*;

@Entity @Table(name = "supervisor")
public class Supervisor {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id") private Usuario usuario;
  @Column(nullable = false) private String nome;
  @Column(nullable = false) private String parentesco;
  @Column(nullable = false) private boolean online;

  public Long getId() { return id; }
  public Usuario getUsuario() { return usuario; } public void setUsuario(Usuario v) { usuario = v; }
  public String getNome() { return nome; } public void setNome(String v) { nome = v; }
  public String getParentesco() { return parentesco; } public void setParentesco(String v) { parentesco = v; }
  public boolean isOnline() { return online; } public void setOnline(boolean v) { online = v; }
}
