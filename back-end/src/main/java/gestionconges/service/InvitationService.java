package gestionconges.service;

import gestionconges.entity.Invitation;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;

import java.util.List;

public interface InvitationService {

    Invitation creerInvitation(Long entrepriseId, String email, Role role, Long equipeId, String poste);

    List<Invitation> getInvitations(Long entrepriseId);

    Invitation trouverParToken(String token);

    Utilisateur activerCompte(String token, String nom, String prenom, String motDePasse);

    void supprimerInvitation(Long invitationId, Long entrepriseId);
}