package gestionconges.repository;

import gestionconges.entity.Equipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipeRepository extends JpaRepository<Equipe, Long> {
    List<Equipe> findByEntreprise_Id(Long entrepriseId);
    List<Equipe> findByEntreprise_IdAndActiveTrue(Long entrepriseId);
    Optional<Equipe> findByIdAndEntreprise_Id(Long id, Long entrepriseId);
    Optional<Equipe> findByManager_Id(Long managerId);
    long countByEntreprise_Id(Long entrepriseId);
}
