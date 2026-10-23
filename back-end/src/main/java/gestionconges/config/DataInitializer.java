package gestionconges.config;

import gestionconges.entity.*;
import gestionconges.enums.Role;
import gestionconges.enums.StatutConge;
import gestionconges.enums.StatutUtilisateur;
import gestionconges.enums.TypeConge;
import gestionconges.repository.*;
import gestionconges.service.DemandeCongeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final EntrepriseRepository entrepriseRepository;
    private final EquipeRepository equipeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DemandeCongeRepository demandeRepository;
    private final SoldeCongeRepository soldeRepository;
    private final MouvementSoldeRepository mouvementSoldeRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemandeCongeService demandeCongeService;

    public DataInitializer(EntrepriseRepository entrepriseRepository,
                           EquipeRepository equipeRepository,
                           UtilisateurRepository utilisateurRepository,
                           DemandeCongeRepository demandeRepository,
                           SoldeCongeRepository soldeRepository,
                           MouvementSoldeRepository mouvementSoldeRepository,
                           PasswordEncoder passwordEncoder,
                           DemandeCongeService demandeCongeService) {
        this.entrepriseRepository = entrepriseRepository;
        this.equipeRepository = equipeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.demandeRepository = demandeRepository;
        this.soldeRepository = soldeRepository;
        this.mouvementSoldeRepository = mouvementSoldeRepository;
        this.passwordEncoder = passwordEncoder;
        this.demandeCongeService = demandeCongeService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Créer ou récupérer l'entreprise démo
        Entreprise entreprise = entrepriseRepository.findByIdentifiantUnique("LEAVEFLOW-01")
                .orElseGet(() -> {
                    Entreprise ent = new Entreprise("LeaveFlow Tech", "LEAVEFLOW-01");
                    ent.setSecteur("Technologie & Services Numériques");
                    ent.setDescription("Entreprise de démonstration pour la plateforme de gestion des congés LeaveFlow");
                    ent.setJoursCongesAnnuels(30);
                    return entrepriseRepository.save(ent);
                });

        // 2. Créer ou récupérer l'équipe principale
        Equipe equipeTech = equipeRepository.findByEntreprise_Id(entreprise.getId())
                .stream().findFirst().orElseGet(() -> {
                    Equipe eq = new Equipe("Pôle Ingénierie & Produit", entreprise);
                    eq.setDescription("Développement logiciel, design et gestion de produit");
                    return equipeRepository.save(eq);
                });

        // 3. Associer les utilisateurs existants sans entreprise et vérifier les comptes démo
        List<Utilisateur> tous = utilisateurRepository.findAll();
        for (Utilisateur u : tous) {
            boolean modified = false;
            if (u.getEntreprise() == null) {
                u.setEntreprise(entreprise);
                modified = true;
            }
            if (u.getStatut() == null) {
                u.setStatut(StatutUtilisateur.ACTIF);
                modified = true;
            }
            if (u.getEquipe() == null && u.getRole() != Role.RH) {
                u.setEquipe(equipeTech);
                modified = true;
            }
            if (u.getPoste() == null) {
                if (u.getRole() == Role.RH) u.setPoste("Responsable RH");
                else if (u.getRole() == Role.MANAGER) u.setPoste("Engineering Manager");
                else u.setPoste("Ingénieur Logiciel");
                modified = true;
            }
            if (modified) {
                utilisateurRepository.save(u);
            }
        }

        // Créer ou mettre à jour les comptes démo pour garantir l'accès avec "postgress"
        Utilisateur rh = utilisateurRepository.findByEmail("test@rh.com").orElseGet(() -> {
            Utilisateur u = new Utilisateur();
            u.setEmail("test@rh.com");
            u.setNom("Dupont");
            u.setPrenom("Claire");
            return u;
        });
        rh.setMotDePasse(passwordEncoder.encode("postgress"));
        rh.setRole(Role.RH);
        rh.setStatut(StatutUtilisateur.ACTIF);
        rh.setPoste("Directrice Ressources Humaines");
        rh.setEntreprise(entreprise);
        rh = utilisateurRepository.save(rh);

        Utilisateur manager = utilisateurRepository.findByEmail("test1@gmail.com").orElseGet(() -> {
            Utilisateur u = new Utilisateur();
            u.setEmail("test1@gmail.com");
            u.setNom("Martin");
            u.setPrenom("Thomas");
            return u;
        });
        manager.setMotDePasse(passwordEncoder.encode("postgress"));
        manager.setRole(Role.MANAGER);
        manager.setStatut(StatutUtilisateur.ACTIF);
        manager.setPoste("Engineering Manager");
        manager.setEntreprise(entreprise);
        manager.setEquipe(equipeTech);
        manager = utilisateurRepository.save(manager);

        if (equipeTech.getManager() == null || !equipeTech.getManager().getId().equals(manager.getId())) {
            equipeTech.setManager(manager);
            equipeRepository.save(equipeTech);
        }

        Utilisateur employe = utilisateurRepository.findByEmail("test@employe.tn").orElseGet(() -> {
            Utilisateur u = new Utilisateur();
            u.setEmail("test@employe.tn");
            u.setNom("Zarrad");
            u.setPrenom("Sara");
            return u;
        });
        employe.setMotDePasse(passwordEncoder.encode("postgress"));
        employe.setRole(Role.EMPLOYE);
        employe.setStatut(StatutUtilisateur.ACTIF);
        employe.setPoste("Développeuse Full-Stack");
        employe.setEntreprise(entreprise);
        employe.setEquipe(equipeTech);
        employe = utilisateurRepository.save(employe);

        // 4. Initialiser soldes pour les utilisateurs s'ils n'en ont pas
        int annee = Year.now().getValue();
        for (Utilisateur u : List.of(rh, manager, employe)) {
            if (soldeRepository.findByUtilisateur_IdAndAnnee(u.getId(), annee).isEmpty()) {
                SoldeConge solde = new SoldeConge(entreprise, u, annee, 30, 0);
                solde = soldeRepository.save(solde);
                MouvementSolde mouv = new MouvementSolde(
                        entreprise,
                        solde,
                        u,
                        30,
                        "Initialisation annuelle des congés 2026",
                        rh
                );
                mouvementSoldeRepository.save(mouv);
            }
        }

        // 5. Initialiser les comptes de démonstration LeaveFlow (conformité Cahier des charges et frontend)
        Equipe equipeProduit = equipeRepository.findByEntreprise_Id(entreprise.getId()).stream()
                .filter(e -> "Produit".equalsIgnoreCase(e.getNom()) || e.getNom().contains("Produit"))
                .findFirst().orElseGet(() -> {
                    Equipe eq = new Equipe("Produit", entreprise);
                    eq.setDescription("Équipe conception et développement produit");
                    return equipeRepository.save(eq);
                });

        Equipe equipeMarketing = equipeRepository.findByEntreprise_Id(entreprise.getId()).stream()
                .filter(e -> "Marketing".equalsIgnoreCase(e.getNom()))
                .findFirst().orElseGet(() -> {
                    Equipe eq = new Equipe("Marketing", entreprise);
                    eq.setDescription("Pôle marketing et communication");
                    return equipeRepository.save(eq);
                });

        // RH Démo: rh@demo.fr (Lina Ben Ali)
        Utilisateur rhDemo = utilisateurRepository.findByEmail("rh@demo.fr").orElseGet(() -> {
            Utilisateur u = new Utilisateur();
            u.setEmail("rh@demo.fr");
            u.setNom("Ben Ali");
            u.setPrenom("Lina");
            return u;
        });
        rhDemo.setMotDePasse(passwordEncoder.encode("Demo2026!"));
        rhDemo.setRole(Role.RH);
        rhDemo.setStatut(StatutUtilisateur.ACTIF);
        rhDemo.setPoste("Responsable RH");
        rhDemo.setEntreprise(entreprise);
        rhDemo.setEquipe(equipeTech);
        rhDemo = utilisateurRepository.save(rhDemo);

        // Manager Démo: manager@demo.fr (Sarra Zarrad)
        Utilisateur managerDemo = utilisateurRepository.findByEmail("manager@demo.fr").orElseGet(() -> {
            Utilisateur u = new Utilisateur();
            u.setEmail("manager@demo.fr");
            u.setNom("Zarrad");
            u.setPrenom("Sarra");
            return u;
        });
        managerDemo.setMotDePasse(passwordEncoder.encode("Demo2026!"));
        managerDemo.setRole(Role.MANAGER);
        managerDemo.setStatut(StatutUtilisateur.ACTIF);
        managerDemo.setPoste("Lead Product Manager");
        managerDemo.setEntreprise(entreprise);
        managerDemo.setEquipe(equipeProduit);
        managerDemo = utilisateurRepository.save(managerDemo);

        equipeProduit.setManager(managerDemo);
        equipeRepository.save(equipeProduit);

        // Employé Démo: employe@demo.fr (Asma Sahli)
        Utilisateur empDemo = utilisateurRepository.findByEmail("employe@demo.fr").orElseGet(() -> {
            Utilisateur u = new Utilisateur();
            u.setEmail("employe@demo.fr");
            u.setNom("Sahli");
            u.setPrenom("Asma");
            return u;
        });
        empDemo.setMotDePasse(passwordEncoder.encode("Demo2026!"));
        empDemo.setRole(Role.EMPLOYE);
        empDemo.setStatut(StatutUtilisateur.ACTIF);
        empDemo.setPoste("Designer UI/UX");
        empDemo.setEntreprise(entreprise);
        empDemo.setEquipe(equipeProduit);
        empDemo = utilisateurRepository.save(empDemo);

        // Soldes démo
        for (Utilisateur u : List.of(rhDemo, managerDemo, empDemo)) {
            if (soldeRepository.findByUtilisateur_IdAndAnnee(u.getId(), annee).isEmpty()) {
                int init = u.getRole() == Role.RH ? 24 : (u.getRole() == Role.MANAGER ? 21 : 18);
                SoldeConge s = new SoldeConge(entreprise, u, annee, 30, 30 - init);
                soldeRepository.save(s);
            }
        }

        // 6. Rattacher les demandes orphelines à l'entreprise et calculer les jours ouvrables
        List<DemandeConge> demandes = demandeRepository.findAll();
        for (DemandeConge d : demandes) {
            boolean mod = false;
            if (d.getEntreprise() == null && d.getUtilisateur() != null && d.getUtilisateur().getEntreprise() != null) {
                d.setEntreprise(d.getUtilisateur().getEntreprise());
                mod = true;
            }
            if (d.getNombreJours() == null || d.getNombreJours() <= 0) {
                int nb = demandeCongeService.compterJoursOuvrables(d.getDateDebut(), d.getDateFin());
                d.setNombreJours(Math.max(1, nb));
                mod = true;
            }
            if (mod) {
                demandeRepository.save(d);
            }
        }
    }
}
