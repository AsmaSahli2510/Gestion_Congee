package gestionconges.repository;

import gestionconges.entity.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {
    Optional<Entreprise> findByIdentifiantUnique(String identifiantUnique);
    boolean existsByIdentifiantUnique(String identifiantUnique);
}
