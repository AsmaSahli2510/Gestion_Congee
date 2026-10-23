package gestionconges.repository;

import gestionconges.entity.MouvementSolde;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MouvementSoldeRepository extends JpaRepository<MouvementSolde, Long> {
    List<MouvementSolde> findByUtilisateur_IdOrderByDateMouvementDesc(Long utilisateurId);
    List<MouvementSolde> findByEntreprise_IdOrderByDateMouvementDesc(Long entrepriseId);
    List<MouvementSolde> findBySolde_IdOrderByDateMouvementDesc(Long soldeId);
}
