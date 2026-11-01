package gestionconges.repository;

import gestionconges.entity.DemandeConge;
import gestionconges.enums.StatutConge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DemandeCongeRepository extends JpaRepository<DemandeConge, Long> {

    List<DemandeConge> findByStatut(StatutConge statut);

    List<DemandeConge> findByStatutIn(Collection<StatutConge> statuts);

    List<DemandeConge> findByUtilisateur_Id(Long utilisateurId);

    List<DemandeConge> findByUtilisateur_IdOrderByDateCreationDesc(Long utilisateurId);

    List<DemandeConge> findByUtilisateur_IdAndStatut(Long utilisateurId, StatutConge statut);

    List<DemandeConge> findByUtilisateur_IdAndStatutInOrderByDateCreationDesc(Long utilisateurId, Collection<StatutConge> statuts);

    Optional<DemandeConge> findByIdAndUtilisateur_Id(Long id, Long utilisateurId);

    // Multi-tenant & Équipe
    List<DemandeConge> findByEntreprise_IdOrderByDateCreationDesc(Long entrepriseId);

    List<DemandeConge> findByEntreprise_IdAndStatutOrderByDateCreationDesc(Long entrepriseId, StatutConge statut);

    List<DemandeConge> findByEntreprise_IdAndStatutInOrderByDateCreationDesc(Long entrepriseId, Collection<StatutConge> statuts);

    Optional<DemandeConge> findByIdAndEntreprise_Id(Long id, Long entrepriseId);

    List<DemandeConge> findByUtilisateur_Equipe_IdOrderByDateCreationDesc(Long equipeId);

    List<DemandeConge> findByUtilisateur_Equipe_IdAndStatutOrderByDateCreationDesc(Long equipeId, StatutConge statut);

    List<DemandeConge> findByUtilisateur_Equipe_IdAndStatutInOrderByDateCreationDesc(Long equipeId, Collection<StatutConge> statuts);

    long countByEntreprise_IdAndStatut(Long entrepriseId, StatutConge statut);

    long countByEntreprise_IdAndStatutIn(Long entrepriseId, Collection<StatutConge> statuts);

    long countByUtilisateur_Equipe_IdAndStatut(Long equipeId, StatutConge statut);

    long countByUtilisateur_Equipe_IdAndStatutIn(Long equipeId, Collection<StatutConge> statuts);

    // Requêtes JPQL personnalisées avec @Query et @Param selon le guide de bonnes pratiques
    @Query("SELECT d FROM DemandeConge d WHERE d.entreprise.id = :entrepriseId AND d.statut = :statut ORDER BY d.dateCreation DESC")
    List<DemandeConge> findDemandesByEntrepriseAndStatut(
            @Param("entrepriseId") Long entrepriseId,
            @Param("statut") StatutConge statut
    );

    @Query("SELECT d FROM DemandeConge d WHERE d.utilisateur.id = :utilisateurId " +
            "AND d.periode.dateDebut <= :fin AND d.periode.dateFin >= :debut " +
            "AND d.statut IN :statuts")
    List<DemandeConge> findChevauchements(
            @Param("utilisateurId") Long utilisateurId,
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin,
            @Param("statuts") Collection<StatutConge> statuts
    );

    // Requête de mise à jour avec @Modifying et @Transactional
    @Modifying
    @Transactional
    @Query("UPDATE DemandeConge d SET d.statut = :statut WHERE d.id = :id")
    int updateStatutDirect(@Param("id") Long id, @Param("statut") StatutConge statut);
}