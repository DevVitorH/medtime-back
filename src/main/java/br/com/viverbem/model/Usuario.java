package br.com.viverbem.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "usuario")
public class Usuario {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private String nome;    // opcional: preenchido no Perfil depois do cadastro
  private Integer idade;  // opcional: preenchida no Perfil depois do cadastro
  @Column(nullable = false, unique = true) private String email;
  @Column(name = "senha_hash", nullable = false) private String senhaHash; // BCrypt, nunca a senha
  @Column(name = "aceite_lgpd", nullable = false) private boolean aceiteLgpd;
  @Column(unique = true) private String token;
  @Column(name = "token_expira_em") private Instant tokenExpiraEm;
  @Column(name = "tentativas_falhas", nullable = false) private int tentativasFalhas = 0;
  @Column(name = "bloqueado_ate") private Instant bloqueadoAte;

  public Long getId() { return id; }
  public String getNome() { return nome; } public void setNome(String v) { nome = v; }
  public Integer getIdade() { return idade; } public void setIdade(Integer v) { idade = v; }
  public String getEmail() { return email; } public void setEmail(String v) { email = v; }
  public String getSenhaHash() { return senhaHash; } public void setSenhaHash(String v) { senhaHash = v; }
  public boolean isAceiteLgpd() { return aceiteLgpd; } public void setAceiteLgpd(boolean v) { aceiteLgpd = v; }
  public String getToken() { return token; } public void setToken(String v) { token = v; }
  public Instant getTokenExpiraEm() { return tokenExpiraEm; } public void setTokenExpiraEm(Instant v) { tokenExpiraEm = v; }
  public int getTentativasFalhas() { return tentativasFalhas; } public void setTentativasFalhas(int v) { tentativasFalhas = v; }
  public Instant getBloqueadoAte() { return bloqueadoAte; } public void setBloqueadoAte(Instant v) { bloqueadoAte = v; }
}
