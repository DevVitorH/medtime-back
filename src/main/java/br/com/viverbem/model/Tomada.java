package br.com.viverbem.model;

import br.com.viverbem.config.Fuso;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Registro de que um medicamento foi tomado em um dia (1 por medicamento/dia). */
@Entity
@Table(name = "tomada", uniqueConstraints = @UniqueConstraint(columnNames = {"medicamento_id", "data"}))
public class Tomada {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "medicamento_id") private Medicamento medicamento;
  @Column(nullable = false) private LocalDate data;
  @Column(name = "tomado_em", nullable = false) private LocalDateTime tomadoEm = Fuso.agora();

  public Long getId() { return id; }
  public Medicamento getMedicamento() { return medicamento; } public void setMedicamento(Medicamento v) { medicamento = v; }
  public LocalDate getData() { return data; } public void setData(LocalDate v) { data = v; }
  public LocalDateTime getTomadoEm() { return tomadoEm; }
}
