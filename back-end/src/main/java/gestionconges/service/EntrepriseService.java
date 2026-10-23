package gestionconges.service;

import gestionconges.entity.Entreprise;
import gestionconges.entity.Equipe;
import gestionconges.entity.MouvementSolde;
import gestionconges.entity.SoldeConge;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.enums.StatutUtilisateur;
import gestionconges.repository.EntrepriseRepository;
import gestionconges.repository.EquipeRepository;
import gestionconges.repository.MouvementSoldeRepository;
import gestionconges.repository.SoldeCongeRepository;
import gestionconges.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

@Service
public class EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EquipeRepository equipeRepository;
    private final SoldeCongeRepository soldeCongeRepository;
    private final MouvementSoldeRepository mouvementSoldeRepository;
    private final PasswordEncoder passwordEncoder;

    public EntrepriseService(EntrepriseRepository entrepriseRepository,
                             UtilisateurRepository utilisateurRepository,
                             EquipeRepository equipeRepository,
                             SoldeCongeRepository soldeCongeRepository,
                             MouvementSoldeRepository mouvementSoldeRepository,
                             PasswordEncoder passwordEncoder) {
        this.entrepriseRepository = entrepriseRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.equipeRepository = equipeRepository;
        this.soldeCongeRepository = soldeCongeRepository;
        this.mouvementSoldeRepository = mouvementSoldeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Utilisateur creerNouvelleEntrepriseEtRh(String nomEntreprise, String nomRh, String prenomRh, String emailRh, String motDePasse) {
        emailRh = emailRh.trim().toLowerCase();
        nomEntreprise = nomEntreprise.trim();

        if (utilisateurRepository.existsByEmail(emailRh)) {
            throw new IllegalArgumentException("Un compte avec cette adresse email existe déjà.");
        }

        // Générer identifiant unique pour l'entreprise
        String codeBase = nomEntreprise.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (codeBase.length() > 8) {
            codeBase = codeBase.substring(0, 8);
        }
        if (codeBase.isBlank()) {
            codeBase = "ENT";
        }
        String identifiantUnique = codeBase + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        Entreprise entreprise = new Entreprise(nomEntreprise, identifiantUnique);
        entreprise.setJoursCongesAnnuels(30);
        entreprise = entrepriseRepository.save(entreprise);

        // Créer l'utilisateur RH rattaché
        Utilisateur rh = new Utilisateur();
        rh.setNom(nomRh.trim());
        rh.setPrenom(prenomRh.trim());
        rh.setEmail(emailRh);
        rh.setMotDePasse(passwordEncoder.encode(motDePasse));
        rh.setRole(Role.RH);
        rh.setStatut(StatutUtilisateur.ACTIF);
        rh.setPoste("Responsable Ressources Humaines");
        rh.setEntreprise(entreprise);
        rh.setDateCreation(LocalDateTime.now());
        rh = utilisateurRepository.save(rh);

        // Créer une première équipe par défaut
        Equipe equipeDefaut = new Equipe("Direction & Administration", entreprise);
        equipeDefaut.setDescription("Équipe administrative et direction générale");
        equipeDefaut.setManager(rh);
        equipeDefaut = equipeRepository.save(equipeDefaut);

        rh.setEquipe(equipeDefaut);
        rh = utilisateurRepository.save(rh);

        // Initialiser le solde de congés pour l'année courante
        int annee = Year.now().getValue();
        SoldeConge solde = new SoldeConge(entreprise, rh, annee, 30, 0);
        solde = soldeCongeRepository.save(solde);

        // Mouvement initial de solde
        MouvementSolde mouvement = new MouvementSolde(
                entreprise,
                solde,
                rh,
                30,
                "Attribution initiale annuelle de création d'entreprise",
                rh
        );
        mouvementSoldeRepository.save(mouvement);

        return rh;
    }

    public Entreprise trouverParId(Long id) {
        return entrepriseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise introuvable : " + id));
    }

    @Transactional
    public Entreprise mettreAJourEntreprise(Long id, String nom, String secteur, String description, Integer joursConges) {
        Entreprise entreprise = trouverParId(id);
        if (nom != null && !nom.isBlank()) {
            entreprise.setNom(nom.trim());
        }
        entreprise.setSecteur(secteur);
        entreprise.setDescription(description);
        if (joursConges != null && joursConges > 0) {
            entreprise.setJoursCongesAnnuels(joursConges);
        }
        return entrepriseRepository.save(entreprise);
    }
}
