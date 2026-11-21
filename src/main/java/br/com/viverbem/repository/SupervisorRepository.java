package br.com.viverbem.repository;

import br.com.viverbem.model.Supervisor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupervisorRepository extends JpaRepository<Supervisor, Long> {
  Optional<Supervisor> findFirstByUsuarioId(Long usuarioId);
}
