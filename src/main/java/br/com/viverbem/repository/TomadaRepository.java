package br.com.viverbem.repository;

import br.com.viverbem.model.Tomada;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TomadaRepository extends JpaRepository<Tomada, Long> {
  List<Tomada> findByMedicamentoUsuarioIdAndData(Long usuarioId, LocalDate data);
  List<Tomada> findByMedicamentoUsuarioIdAndDataBetween(Long usuarioId, LocalDate ini, LocalDate fim);
  boolean existsByMedicamentoIdAndData(Long medicamentoId, LocalDate data);
  boolean existsByMedicamentoId(Long medicamentoId);
}
