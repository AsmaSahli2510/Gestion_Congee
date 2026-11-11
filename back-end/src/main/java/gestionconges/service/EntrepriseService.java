package gestionconges.service;

import gestionconges.entity.Entreprise;
import gestionconges.entity.Utilisateur;

public interface EntrepriseService {

    Utilisateur creerNouvelleEntrepriseEtRh(String nomEntreprise, String nomRh, String prenomRh, String emailRh, String motDePasse);

    Entreprise trouverParId(Long id);

    Entreprise mettreAJourEntreprise(Long id, String nom, String secteur, String description, Integer joursConges);
}