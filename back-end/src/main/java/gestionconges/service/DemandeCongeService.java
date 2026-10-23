package gestionconges.service;

import gestionconges.entity.DemandeConge;
import gestionconges.entity.Entreprise;
import gestionconges.entity.MouvementSolde;
import gestionconges.entity.SoldeConge;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.enums.StatutConge;
import gestionconges.enums.TypeConge;
import gestionconges.repository.DemandeCongeRepository;
import gestionconges.repository.MouvementSoldeRepository;
import gestionconges.repository.SoldeCongeRepository;
import gestionconges.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
public class DemandeCongeService {

    private final DemandeCongeRepository demandeRepository;
    private final SoldeCongeRepository soldeRepository;
    private final MouvementSoldeRepository mouvementSoldeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final SoldeCongeService soldeCongeService;

    public DemandeCongeService(
            DemandeCongeRepository demandeRepository,
            SoldeCongeRepository soldeRepository,
            MouvementSoldeRepository mouvementSoldeRepository,
            UtilisateurRepository utilisateurRepository,
            NotificationService notificationService,
            SoldeCongeService soldeCongeService) {
        this.demandeRepository = demandeRepository;
        this.soldeRepository = soldeRepository;
        this.mouvementSoldeRepository = mouvementSoldeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.notificationService = notificationService;
        this.soldeCongeService = soldeCongeService;
    }

    private static final List<StatutConge> STATUTS_EN_ATTENTE = List.of(StatutConge.SOUMISE, StatutConge.EN_ATTENTE);

    public List<DemandeConge> getToutesLesDemandes() {
        return demandeRepository.findAll();
    }

    public List<DemandeConge> getDemandesEnAttente() {
        return demandeRepository.findByStatutIn(STATUTS_EN_ATTENTE);
    }

    public List<DemandeConge> getDemandesParUtilisateur(Long utilisateurId) {
        return demandeRepository.findByUtilisateur_IdOrderByDateCreationDesc(utilisateurId);
    }

    public List<DemandeConge> getDemandesEntreprise(Long entrepriseId) {
        return demandeRepository.findByEntreprise_IdOrderByDateCreationDesc(entrepriseId);
    }

    public List<DemandeConge> getDemandesEnAttenteEntreprise(Long entrepriseId) {
        return demandeRepository.findByEntreprise_IdAndStatutInOrderByDateCreationDesc(entrepriseId, STATUTS_EN_ATTENTE);
    }

    public List<DemandeConge> getDemandesEquipe(Long equipeId) {
        return demandeRepository.findByUtilisateur_Equipe_IdOrderByDateCreationDesc(equipeId);
    }

    public List<DemandeConge> getDemandesEnAttenteEquipe(Long equipeId) {
        return demandeRepository.findByUtilisateur_Equipe_IdAndStatutInOrderByDateCreationDesc(equipeId, STATUTS_EN_ATTENTE);
    }

    public DemandeConge trouverDemande(Long id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande de congé introuvable : " + id));
    }

    public DemandeConge trouverDemandeParEntreprise(Long id, Long entrepriseId) {
        return demandeRepository.findByIdAndEntreprise_Id(id, entrepriseId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable ou accès non autorisé"));
    }

    public int compterJoursOuvrables(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null || fin.isBefore(debut)) {
            return 0;
        }

        int total = 0;
        for (LocalDate jour = debut; !jour.isAfter(fin); jour = jour.plusDays(1)) {
            DayOfWeek dow = jour.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                total++;
            }
        }
        return total;
    }

    @Transactional
    public DemandeConge creerDemande(DemandeConge demande, Utilisateur utilisateur) {
        if (demande.getDateDebut() == null || demande.getDateFin() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
        }

        if (demande.getDateFin().isBefore(demande.getDateDebut())) {
            throw new IllegalArgumentException("La date de fin doit être postérieure ou égale à la date de début.");
        }

        int nbJours = compterJoursOuvrables(demande.getDateDebut(), demande.getDateFin());
        if (nbJours <= 0) {
            throw new IllegalArgumentException("La période sélectionnée ne contient aucun jour ouvrable (lundi au vendredi).");
        }

        demande.setNombreJours(nbJours);
        demande.setUtilisateur(utilisateur);
        demande.setEntreprise(utilisateur.getEntreprise());
        demande.setStatut(StatutConge.EN_ATTENTE);
        demande.setDateCreation(LocalDateTime.now());

        // Vérification du chevauchement de dates
        List<DemandeConge> existantes = demandeRepository.findByUtilisateur_Id(utilisateur.getId());
        for (DemandeConge ex : existantes) {
            if (ex.getStatut() == StatutConge.APPROUVEE || ex.getStatut().isEnAttente()) {
                boolean chevauchement = !(demande.getDateFin().isBefore(ex.getDateDebut()) || demande.getDateDebut().isAfter(ex.getDateFin()));
                if (chevauchement) {
                    throw new IllegalArgumentException("Vous avez déjà une demande en attente ou approuvée sur cette période ("
                            + ex.getDateDebut() + " au " + ex.getDateFin() + ").");
                }
            }
        }

        // Vérification du solde si le type de congé est déductible
        if (demande.getTypeConge().isDeduitDuSolde()) {
            int annee = demande.getDateDebut().getYear();
            SoldeConge solde = soldeCongeService.getOuCreerSolde(utilisateur.getId(), annee);
            if (solde.getJoursRestants() < nbJours) {
                throw new IllegalArgumentException(String.format(
                        "Solde insuffisant pour l'année %d : il vous reste %d jours, mais cette demande requiert %d jours ouvrables.",
                        annee, solde.getJoursRestants(), nbJours));
            }
        }

        DemandeConge saved = demandeRepository.save(demande);

        // Notifications in-app pour les valideurs
        Entreprise ent = utilisateur.getEntreprise();
        if (utilisateur.getRole() == Role.MANAGER) {
            // Manager demande : notifie le RH (RG06)
            List<Utilisateur> rhs = utilisateurRepository.findByEntreprise_IdAndRole(ent != null ? ent.getId() : null, Role.RH);
            for (Utilisateur rh : rhs) {
                notificationService.creerNotification(
                        ent,
                        rh,
                        "Nouvelle demande de congé (Manager)",
                        "Le manager " + utilisateur.getNomComplet() + " a soumis une demande de congés du "
                                + demande.getDateDebut() + " au " + demande.getDateFin() + " (" + nbJours + " j).",
                        "/rh/demandes"
                );
            }
        } else if (utilisateur.getEquipe() != null && utilisateur.getEquipe().getManager() != null) {
            // Employé d'une équipe : notifie son manager
            notificationService.creerNotification(
                    ent,
                    utilisateur.getEquipe().getManager(),
                    "Nouvelle demande d'équipe",
                    utilisateur.getNomComplet() + " a déposé une demande de congé (" + nbJours + " j) du "
                            + demande.getDateDebut() + " au " + demande.getDateFin() + ".",
                    "/manager/demandes"
            );
        }

        return saved;
    }

    // RG04 : Modifier une demande en attente (SOUMISE)
    @Transactional
    public DemandeConge modifierDemande(Long demandeId, LocalDate dateDebut, LocalDate dateFin, TypeConge typeConge, String motif, String justificatif, Long utilisateurId) {
        DemandeConge demande = demandeRepository.findByIdAndUtilisateur_Id(demandeId, utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable ou accès non autorisé"));

        if (!demande.getStatut().isEnAttente()) {
            throw new IllegalStateException("Seules les demandes en attente (SOUMISE) peuvent être modifiées.");
        }

        if (dateDebut == null || dateFin == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
        }

        if (dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException("La date de fin doit être postérieure ou égale à la date de début.");
        }

        int nbJours = compterJoursOuvrables(dateDebut, dateFin);
        if (nbJours <= 0) {
            throw new IllegalArgumentException("La période sélectionnée ne contient aucun jour ouvrable (lundi au vendredi).");
        }

        // Vérification de chevauchement avec d'autres demandes (hors celle-ci)
        List<DemandeConge> existantes = demandeRepository.findByUtilisateur_Id(utilisateurId);
        for (DemandeConge ex : existantes) {
            if (!ex.getId().equals(demandeId) && (ex.getStatut() == StatutConge.APPROUVEE || ex.getStatut().isEnAttente())) {
                boolean chevauchement = !(dateFin.isBefore(ex.getDateDebut()) || dateDebut.isAfter(ex.getDateFin()));
                if (chevauchement) {
                    throw new IllegalArgumentException("Vous avez déjà une autre demande sur cette période ("
                            + ex.getDateDebut() + " au " + ex.getDateFin() + ").");
                }
            }
        }

        TypeConge type = (typeConge != null) ? typeConge : demande.getTypeConge();
        if (type.isDeduitDuSolde()) {
            int annee = dateDebut.getYear();
            SoldeConge solde = soldeCongeService.getOuCreerSolde(utilisateurId, annee);
            if (solde.getJoursRestants() < nbJours) {
                throw new IllegalArgumentException(String.format(
                        "Solde insuffisant pour l'année %d : il vous reste %d jours, mais cette demande requiert %d jours ouvrables.",
                        annee, solde.getJoursRestants(), nbJours));
            }
        }

        demande.setDateDebut(dateDebut);
        demande.setDateFin(dateFin);
        demande.setNombreJours(nbJours);
        demande.setTypeConge(type);
        demande.setMotif(motif);
        if (justificatif != null) {
            demande.setJustificatif(justificatif);
        }

        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeConge annulerDemande(Long demandeId, Long utilisateurId) {
        DemandeConge demande = demandeRepository.findByIdAndUtilisateur_Id(demandeId, utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

        // RG04 : une demande en attente peut être modifiée ou annulée par son auteur
        if (!demande.getStatut().isEnAttente()) {
            throw new IllegalStateException("Seules les demandes en attente (SOUMISE) peuvent être annulées.");
        }

        demande.setStatut(StatutConge.ANNULEE);
        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeConge approuverDemande(Long demandeId, Utilisateur valideur) {
        DemandeConge demande = trouverDemande(demandeId);

        if (demande.getStatut() == StatutConge.APPROUVEE) {
            return demande; // Évite double déduction (RG05)
        }

        if (!demande.getStatut().isEnAttente()) {
            throw new IllegalStateException("Seules les demandes en attente (SOUMISE) peuvent être approuvées.");
        }

        // RG06 : Interdiction d'auto-approbation
        if (valideur.getId().equals(demande.getUtilisateur().getId())) {
            throw new IllegalStateException("Auto-approbation interdite : vous ne pouvez pas valider votre propre demande.");
        }

        // Si le demandeur est un manager, seul le RH peut valider
        if (demande.getUtilisateur().getRole() == Role.MANAGER && valideur.getRole() != Role.RH) {
            throw new IllegalStateException("Les demandes déposées par un manager doivent être validées par le RH.");
        }

        demande.setStatut(StatutConge.APPROUVEE);
        demande.setValideur(valideur);
        demande.setDateDecision(LocalDateTime.now());
        DemandeConge sauvée = demandeRepository.save(demande);

        // RG05 : Déduction exacte du solde et traçabilité
        if (demande.getTypeConge().isDeduitDuSolde()) {
            int annee = demande.getDateDebut().getYear();
            SoldeConge solde = soldeCongeService.getOuCreerSolde(demande.getUtilisateur().getId(), annee);
            int jours = demande.getNombreJours();
            solde.setJoursUtilises(solde.getJoursUtilises() + jours);
            soldeRepository.save(solde);

            MouvementSolde mouvement = new MouvementSolde(
                    demande.getEntreprise(),
                    solde,
                    demande.getUtilisateur(),
                    -jours,
                    String.format("Validation du congé (%s) du %s au %s",
                            demande.getTypeConge().getLibelle(), demande.getDateDebut(), demande.getDateFin()),
                    valideur
            );
            mouvementSoldeRepository.save(mouvement);
        }

        // Notification envoyée au demandeur
        notificationService.creerNotification(
                demande.getEntreprise(),
                demande.getUtilisateur(),
                "Demande de congé approuvée !",
                String.format("Votre demande de congé du %s au %s (%d jours) a été approuvée par %s.",
                        demande.getDateDebut(), demande.getDateFin(), demande.getNombreJours(), valideur.getNomComplet()),
                "/demandes"
        );

        return sauvée;
    }

    @Transactional
    public DemandeConge refuserDemande(Long demandeId, Utilisateur valideur, String commentaire) {
        DemandeConge demande = trouverDemande(demandeId);

        if (demande.getStatut() == StatutConge.APPROUVEE) {
            throw new IllegalStateException("Une demande déjà approuvée ne peut pas être refusée directement.");
        }

        if (valideur.getId().equals(demande.getUtilisateur().getId())) {
            throw new IllegalStateException("Auto-approbation ou auto-rejet interdit.");
        }

        if (demande.getUtilisateur().getRole() == Role.MANAGER && valideur.getRole() != Role.RH) {
            throw new IllegalStateException("Les demandes déposées par un manager doivent être traitées par le RH.");
        }

        demande.setStatut(StatutConge.REFUSEE);
        demande.setValideur(valideur);
        demande.setDateDecision(LocalDateTime.now());
        demande.setCommentaireDecision((commentaire != null && !commentaire.isBlank()) ? commentaire.trim() : "Refusée par le responsable");
        DemandeConge sauvée = demandeRepository.save(demande);

        // Notification envoyée au demandeur
        notificationService.creerNotification(
                demande.getEntreprise(),
                demande.getUtilisateur(),
                "Demande de congé rejetée",
                String.format("Votre demande de congé du %s au %s a été rejetée par %s. Motif : %s",
                        demande.getDateDebut(), demande.getDateFin(), valideur.getNomComplet(), demande.getCommentaireDecision()),
                "/demandes"
        );

        return sauvée;
    }
}
