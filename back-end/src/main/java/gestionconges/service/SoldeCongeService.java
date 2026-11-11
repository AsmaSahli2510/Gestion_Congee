package gestionconges.service;

import gestionconges.entity.MouvementSolde;
import gestionconges.entity.SoldeConge;
import gestionconges.entity.Utilisateur;

import java.util.List;

public interface SoldeCongeService {

    SoldeConge getOuCreerSolde(Long utilisateurId, Integer annee);

    List<SoldeConge> getSoldesEntreprise(Long entrepriseId, Integer annee);

    List<SoldeConge> getSoldesEquipe(Long equipeId, Integer annee);

    List<MouvementSolde> getHistoriqueUtilisateur(Long utilisateurId);

    List<MouvementSolde> getHistoriqueEntreprise(Long entrepriseId);

    SoldeConge ajusterSolde(Long soldeId, Long entrepriseId, Integer variation, String raison, Utilisateur auteur);
}