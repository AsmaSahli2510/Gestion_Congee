package gestionconges.service;

import gestionconges.entity.DemandeConge;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.TypeConge;

import java.time.LocalDate;
import java.util.List;

public interface DemandeCongeService {

    List<DemandeConge> getToutesLesDemandes();

    List<DemandeConge> getDemandesEnAttente();

    List<DemandeConge> getDemandesParUtilisateur(Long utilisateurId);

    List<DemandeConge> getDemandesEntreprise(Long entrepriseId);

    List<DemandeConge> getDemandesEnAttenteEntreprise(Long entrepriseId);

    List<DemandeConge> getDemandesEquipe(Long equipeId);

    List<DemandeConge> getDemandesEnAttenteEquipe(Long equipeId);

    DemandeConge trouverDemande(Long id);

    DemandeConge trouverDemandeParEntreprise(Long id, Long entrepriseId);

    int compterJoursOuvrables(LocalDate debut, LocalDate fin);

    DemandeConge creerDemande(DemandeConge demande, Utilisateur utilisateur);

    DemandeConge modifierDemande(Long demandeId, LocalDate dateDebut, LocalDate dateFin, TypeConge typeConge, String motif, String justificatif, Long utilisateurId);

    DemandeConge annulerDemande(Long demandeId, Long utilisateurId);

    DemandeConge approuverDemande(Long demandeId, Utilisateur valideur);

    DemandeConge refuserDemande(Long demandeId, Utilisateur valideur, String commentaire);
}