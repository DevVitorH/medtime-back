package br.com.viverbem.repository;

import br.com.viverbem.model.CodigoConvite;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodigoConviteRepository extends JpaRepository<CodigoConvite, Long> {
  Optional<CodigoConvite> findByCodigo(String codigo);
  boolean existsByCodigo(String codigo);
  /** Códigos do dependente ainda válidos (para invalidar ao gerar um novo). */
  List<CodigoConvite> findByDependenteIdAndUsadoEmIsNullAndExpiraEmAfter(Long dependenteId, Instant agora);
}
