package gestionconges.service;

import gestionconges.entity.Entreprise;
import gestionconges.entity.Equipe;
import gestionconges.entity.Invitation;
import gestionconges.entity.MouvementSolde;
import gestionconges.entity.SoldeConge;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.enums.StatutUtilisateur;
import gestionconges.repository.EntrepriseRepository;
import gestionconges.repository.EquipeRepository;
import gestionconges.repository.InvitationRepository;
import gestionconges.repository.MouvementSoldeRepository;
import gestionconges.repository.SoldeCongeRepository;
import gestionconges.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final EquipeRepository equipeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SoldeCongeRepository soldeCongeRepository;
    private final MouvementSoldeRepository mouvementSoldeRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public InvitationService(InvitationRepository invitationRepository,
                             EntrepriseRepository entrepriseRepository,
                             EquipeRepository equipeRepository,
                             UtilisateurRepository utilisateurRepository,
                             SoldeCongeRepository soldeCongeRepository,
                             MouvementSoldeRepository mouvementSoldeRepository,
                             PasswordEncoder passwordEncoder,
                             NotificationService notificationService) {
        this.invitationRepository = invitationRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.equipeRepository = equipeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.soldeCongeRepository = soldeCongeRepository;
        this.mouvementSoldeRepository = mouvementSoldeRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Transactional
    public Invitation creerInvitation(Long entrepriseId, String email, Role role, Long equipeId, String poste) {
        email = email.trim().toLowerCase();

        if (utilisateurRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Un utilisateur avec cette adresse email existe déjà sur la plateforme.");
        }

        if (invitationRepository.existsByEmailAndEntreprise_IdAndUtiliseeFalse(email, entrepriseId)) {
            throw new IllegalArgumentException("Une invitation en attente est déjà active pour cette adresse email.");
        }

        Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise introuvable"));

        Equipe equipe = null;
        if (equipeId != null && equipeId > 0) {
            equipe = equipeRepository.findByIdAndEntreprise_Id(equipeId, entrepriseId).orElse(null);
        }

        Invitation invitation = new Invitation();
        invitation.setEntreprise(entreprise);
        invitation.setEmail(email);
        invitation.setRole(role != null ? role : Role.EMPLOYE);
        invitation.setEquipe(equipe);
        invitation.setPoste(poste);
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setDateExpiration(LocalDateTime.now().plusDays(7)); // Validité 7 jours
        invitation.setUtilisee(false);
        invitation.setDateCreation(LocalDateTime.now());

        return invitationRepository.save(invitation);
    }

    public List<Invitation> getInvitations(Long entrepriseId) {
        return invitationRepository.findByEntreprise_IdOrderByDateCreationDesc(entrepriseId);
    }

    public Invitation trouverParToken(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invitation introuvable ou lien invalide."));

        if (invitation.isUtilisee()) {
            throw new IllegalStateException("Cette invitation a déjà été utilisée pour activer un compte.");
        }

        if (invitation.isExpiree()) {
            throw new IllegalStateException("Cette invitation a expiré. Veuillez contacter votre administrateur RH.");
        }

        return invitation;
    }

    @Transactional
    public Utilisateur activerCompte(String token, String nom, String prenom, String motDePasse) {
        Invitation invitation = trouverParToken(token);

        if (utilisateurRepository.existsByEmail(invitation.getEmail())) {
            throw new IllegalStateException("Un compte avec cette adresse email existe déjà.");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(invitation.getEmail());
        utilisateur.setNom(nom.trim());
        utilisateur.setPrenom(prenom.trim());
        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasse));
        utilisateur.setRole(invitation.getRole());
        utilisateur.setStatut(StatutUtilisateur.ACTIF);
        utilisateur.setPoste(invitation.getPoste() != null ? invitation.getPoste() : "Collaborateur");
        utilisateur.setEntreprise(invitation.getEntreprise());
        utilisateur.setEquipe(invitation.getEquipe());
        utilisateur.setDateCreation(LocalDateTime.now());

        utilisateur = utilisateurRepository.save(utilisateur);

        // Si rôle MANAGER et pas encore manager de l'équipe, on l'associe
        if (invitation.getRole() == Role.MANAGER && invitation.getEquipe() != null) {
            Equipe eq = invitation.getEquipe();
            if (eq.getManager() == null) {
                eq.setManager(utilisateur);
                equipeRepository.save(eq);
            }
        }

        // Initialiser le solde annuel
        int annee = Year.now().getValue();
        int jours = invitation.getEntreprise().getJoursCongesAnnuels();
        SoldeConge solde = new SoldeConge(invitation.getEntreprise(), utilisateur, annee, jours, 0);
        solde = soldeCongeRepository.save(solde);

        MouvementSolde mouvement = new MouvementSolde(
                invitation.getEntreprise(),
                solde,
                utilisateur,
                jours,
                "Attribution initiale de congés suite à l'activation du compte",
                utilisateur
        );
        mouvementSoldeRepository.save(mouvement);

        // Marquer l'invitation comme utilisée
        invitation.setUtilisee(true);
        invitationRepository.save(invitation);

        // Notification bienvenue
        notificationService.creerNotification(
                invitation.getEntreprise(),
                utilisateur,
                "Bienvenue sur LeaveFlow !",
                "Votre compte a été activé avec succès au sein de l'entreprise " + invitation.getEntreprise().getNom() + ".",
                "/demandes"
        );

        return utilisateur;
    }

    @Transactional
    public void supprimerInvitation(Long invitationId, Long entrepriseId) {
        invitationRepository.findById(invitationId).ifPresent(inv -> {
            if (inv.getEntreprise().getId().equals(entrepriseId)) {
                invitationRepository.delete(inv);
            }
        });
    }
}
