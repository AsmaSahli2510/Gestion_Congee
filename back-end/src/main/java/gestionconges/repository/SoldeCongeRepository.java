package gestionconges.repository;

import gestionconges.entity.SoldeConge;
import gestionconges.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SoldeCongeRepository extends JpaRepository<SoldeConge, Long> {

    Optional<SoldeConge> findByUtilisateurAndAnnee(Utilisateur utilisateur, Integer annee);

    Optional<SoldeConge> findByUtilisateur_IdAndAnnee(Long utilisateurId, Integer annee);

    List<SoldeConge> findByUtilisateur_Id(Long utilisateurId);

    List<SoldeConge> findByEntreprise_IdAndAnnee(Long entrepriseId, Integer annee);

    List<SoldeConge> findByUtilisateur_Equipe_IdAndAnnee(Long equipeId, Integer annee);

    Optional<SoldeConge> findByIdAndEntreprise_Id(Long id, Long entrepriseId);
}
