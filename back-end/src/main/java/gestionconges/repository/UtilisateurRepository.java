package gestionconges.repository;

import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.enums.StatutUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @Query("SELECT u FROM Utilisateur u WHERE u.entreprise.id = :entrepriseId AND u.statut = :statut")
    List<Utilisateur> findByEntrepriseIdAndStatut(
            @Param("entrepriseId") Long entrepriseId,
            @Param("statut") StatutUtilisateur statut
    );

    @Modifying
    @Transactional
    @Query("UPDATE Utilisateur u SET u.statut = :statut WHERE u.id = :id")
    int updateStatutUtilisateur(
            @Param("id") Long id,
            @Param("statut") StatutUtilisateur statut
    );
}
