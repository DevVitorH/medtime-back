package br.com.viverbem.repository;

import br.com.viverbem.model.Vinculo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VinculoRepository extends JpaRepository<Vinculo, Long> {
  List<Vinculo> findByDependenteId(Long dependenteId);   // os supervisores de alguém
  List<Vinculo> findBySupervisorId(Long supervisorId);   // as pessoas que alguém acompanha
  boolean existsByDependenteIdAndSupervisorId(Long dependenteId, Long supervisorId);
}
