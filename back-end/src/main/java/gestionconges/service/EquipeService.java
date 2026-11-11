package gestionconges.service;

import gestionconges.entity.Equipe;
import gestionconges.entity.Utilisateur;

import java.util.List;

public interface EquipeService {

    List<Equipe> getEquipes(Long entrepriseId);

    List<Equipe> getEquipesActives(Long entrepriseId);

    Equipe trouverParIdEtEntreprise(Long equipeId, Long entrepriseId);

    Equipe creerEquipe(Long entrepriseId, String nom, String description, Long managerId);

    Equipe modifierEquipe(Long equipeId, Long entrepriseId, String nom, String description, Long managerId, boolean active);

    void supprimerEquipe(Long equipeId, Long entrepriseId);

    void assignerMembre(Long utilisateurId, Long equipeId, Long entrepriseId);

    List<Utilisateur> getMembresEquipe(Long equipeId);
}