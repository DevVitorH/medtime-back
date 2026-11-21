package br.com.viverbem.repository;

import br.com.viverbem.model.Medicamento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {
  List<Medicamento> findByUsuarioIdAndAtivoTrueOrderByHorario(Long usuarioId);
  Optional<Medicamento> findByIdAndUsuarioId(Long id, Long usuarioId);
}
