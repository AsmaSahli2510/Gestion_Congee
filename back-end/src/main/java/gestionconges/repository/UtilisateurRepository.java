package gestionconges.repository;

import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Utilisateur> findByEntreprise_Id(Long entrepriseId);

    List<Utilisateur> findByEntreprise_IdAndRole(Long entrepriseId, Role role);

    List<Utilisateur> findByEquipe_Id(Long equipeId);

    Optional<Utilisateur> findByIdAndEntreprise_Id(Long id, Long entrepriseId);

    long countByEntreprise_Id(Long entrepriseId);

    long countByEquipe_Id(Long equipeId);
}
