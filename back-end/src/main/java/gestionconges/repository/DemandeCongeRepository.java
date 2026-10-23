package gestionconges.repository;

import gestionconges.entity.DemandeConge;
import gestionconges.enums.StatutConge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}