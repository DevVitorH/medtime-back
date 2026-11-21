package br.com.viverbem.model;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity @Table(name = "medicamento")
public class Medicamento {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id") private Usuario usuario;
  @Column(nullable = false) private String nome;
  @Column(nullable = false) private String dose;
  @Column(nullable = false) private LocalTime horario;
  private String orientacao;
  @Column(nullable = false) private boolean ativo = true;
  @Column(name = "data_inicio") private LocalDate dataInicio; // preenchida no cadastro (hoje)
  @Column(name = "data_fim") private LocalDate dataFim;       // opcional: será usada ao excluir

  /** Dias da semana em tabela própria (3FN): medicamento_dia(medicamento_id, dia). */
  @ElementCollection
  @CollectionTable(name = "medicamento_dia", joinColumns = @JoinColumn(name = "medicamento_id"))
  @Enumerated(EnumType.STRING) @Column(name = "dia", nullable = false)
  private Set<DayOfWeek> diasSemana = new HashSet<>();

  /** Regra única de agenda: dia da semana marcado e data dentro do período do remédio. */
  public boolean agendadoEm(LocalDate data) {
    return diasSemana.contains(data.getDayOfWeek())
        && dataInicio != null && !data.isBefore(dataInicio)
        && (dataFim == null || !data.isAfter(dataFim));
  }

  public Long getId() { return id; }
  public Usuario getUsuario() { return usuario; } public void setUsuario(Usuario v) { usuario = v; }
  public String getNome() { return nome; } public void setNome(String v) { nome = v; }
  public String getDose() { return dose; } public void setDose(String v) { dose = v; }
  public LocalTime getHorario() { return horario; } public void setHorario(LocalTime v) { horario = v; }
  public String getOrientacao() { return orientacao; } public void setOrientacao(String v) { orientacao = v; }
  public boolean isAtivo() { return ativo; } public void setAtivo(boolean v) { ativo = v; }
  public LocalDate getDataInicio() { return dataInicio; } public void setDataInicio(LocalDate v) { dataInicio = v; }
  public LocalDate getDataFim() { return dataFim; } public void setDataFim(LocalDate v) { dataFim = v; }
  public Set<DayOfWeek> getDiasSemana() { return diasSemana; } public void setDiasSemana(Set<DayOfWeek> v) { diasSemana = v; }
}
