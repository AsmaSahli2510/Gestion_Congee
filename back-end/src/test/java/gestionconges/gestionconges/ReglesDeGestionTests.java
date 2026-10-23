package gestionconges.gestionconges;

import gestionconges.entity.*;
import gestionconges.enums.Role;
import gestionconges.enums.StatutConge;
import gestionconges.enums.StatutUtilisateur;
import gestionconges.enums.TypeConge;
import gestionconges.repository.*;
import gestionconges.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ReglesDeGestionTests {

    @Autowired
    private EntrepriseService entrepriseService;

    @Autowired
    private DemandeCongeService demandeCongeService;

    @Autowired
    private SoldeCongeService soldeCongeService;

    @Autowired
    private EquipeService equipeService;

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private StatistiquesService statistiquesService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private DemandeCongeRepository demandeRepository;

    @Autowired
    private SoldeCongeRepository soldeRepository;

    @Autowired
    private EquipeRepository equipeRepository;

    private Utilisateur rhEnt1;
    private Utilisateur managerEnt1;
    private Utilisateur employeEnt1;
    private Equipe equipeEnt1;
    private Entreprise ent1;

    private Utilisateur rhEnt2;
    private Entreprise ent2;

    @BeforeEach
    void setUp() {
        // Entreprise 1
        rhEnt1 = entrepriseService.creerNouvelleEntrepriseEtRh(
                "Entreprise Alpha", "Dupont", "Jean", "rh.alpha@test.com", "Password123!"
        );
        ent1 = rhEnt1.getEntreprise();

        equipeEnt1 = equipeService.creerEquipe(ent1.getId(), "Equipe Alpha Tech", "Equipe de dev", null);

        managerEnt1 = new Utilisateur();
        managerEnt1.setEmail("manager.alpha@test.com");
        managerEnt1.setNom("Martin");
        managerEnt1.setPrenom("Paul");
        managerEnt1.setMotDePasse("Password123!");
        managerEnt1.setRole(Role.MANAGER);
        managerEnt1.setEntreprise(ent1);
        managerEnt1.setEquipe(equipeEnt1);
        managerEnt1.setStatut(StatutUtilisateur.ACTIF);
        managerEnt1 = utilisateurRepository.save(managerEnt1);

        equipeEnt1.setManager(managerEnt1);
        equipeRepository.save(equipeEnt1);

        employeEnt1 = new Utilisateur();
        employeEnt1.setEmail("employe.alpha@test.com");
        employeEnt1.setNom("Durand");
        employeEnt1.setPrenom("Alice");
        employeEnt1.setMotDePasse("Password123!");
        employeEnt1.setRole(Role.EMPLOYE);
        employeEnt1.setEntreprise(ent1);
        employeEnt1.setEquipe(equipeEnt1);
        employeEnt1.setStatut(StatutUtilisateur.ACTIF);
        employeEnt1 = utilisateurRepository.save(employeEnt1);

        int annee = Year.now().getValue();
        soldeCongeService.getOuCreerSolde(managerEnt1.getId(), annee);
        soldeCongeService.getOuCreerSolde(employeEnt1.getId(), annee);

        // Entreprise 2 (pour tester l'étanchéité multi-tenant)
        rhEnt2 = entrepriseService.creerNouvelleEntrepriseEtRh(
                "Entreprise Beta", "Leclerc", "Marc", "rh.beta@test.com", "Password123!"
        );
        ent2 = rhEnt2.getEntreprise();
    }

    @Test
    @DisplayName("RG01 : Un RH nouvellement inscrit crée une entreprise distincte, sans accès à celles déjà existantes")
    void testRG01_CreationNouvelleEntrepriseIsolee() {
        assertNotNull(ent1.getId());
        assertNotNull(ent2.getId());
        assertNotEquals(ent1.getId(), ent2.getId());
        assertNotEquals(ent1.getIdentifiantUnique(), ent2.getIdentifiantUnique());

        // Les demandes de l'entreprise 1 ne sont pas visibles dans l'entreprise 2
        List<DemandeConge> demandesEnt2 = demandeCongeService.getDemandesEntreprise(ent2.getId());
        assertTrue(demandesEnt2.isEmpty(), "Une nouvelle entreprise ne doit avoir aucune demande existante d'autres entreprises");
    }

    @Test
    @DisplayName("RG02 : Seul le RH de la société peut inviter un collaborateur dans sa société")
    void testRG02_InvitationCollaborateur() {
        Invitation invitation = invitationService.creerInvitation(
                ent1.getId(), "nouveau.collaborateur@test.com", Role.EMPLOYE, equipeEnt1.getId(), "Développeur"
        );

        assertNotNull(invitation.getToken());
        assertFalse(invitation.isUtilisee());
        assertEquals("nouveau.collaborateur@test.com", invitation.getEmail());
        assertEquals(ent1.getId(), invitation.getEntreprise().getId());

        // Activation de compte
        Utilisateur active = invitationService.activerCompte(
                invitation.getToken(), "Nouveau", "Collab", "PasswordSecret123!"
        );
        assertNotNull(active.getId());
        assertEquals(ent1.getId(), active.getEntreprise().getId());
        assertEquals(Role.EMPLOYE, active.getRole());

        // Invitation réutilisée -> doit échouer
        assertThrows(IllegalStateException.class, () ->
                invitationService.activerCompte(invitation.getToken(), "Test", "Test", "PasswordSecret123!")
        );
    }

    @Test
    @DisplayName("RG03 : Un manager n'accède qu'aux employés de son équipe")
    void testRG03_ManagerAccesSeulementSonEquipe() {
        // Créer une 2ème équipe dans l'entreprise 1
        Equipe autreEquipe = equipeService.creerEquipe(ent1.getId(), "Equipe Marketing", "Marketing", null);
        Utilisateur autreEmploye = new Utilisateur();
        autreEmploye.setEmail("marketing.employe@test.com");
        autreEmploye.setNom("Bernard");
        autreEmploye.setPrenom("Luc");
        autreEmploye.setMotDePasse("Password123!");
        autreEmploye.setRole(Role.EMPLOYE);
        autreEmploye.setEntreprise(ent1);
        autreEmploye.setEquipe(autreEquipe);
        final Utilisateur savedAutre = utilisateurRepository.save(autreEmploye);

        List<Utilisateur> membresManager = utilisateurRepository.findByEquipe_Id(equipeEnt1.getId());
        final Long empId = employeEnt1.getId();
        final Long autreId = savedAutre.getId();
        assertTrue(membresManager.stream().anyMatch(u -> u.getId().equals(empId)));
        assertFalse(membresManager.stream().anyMatch(u -> u.getId().equals(autreId)));
    }

    @Test
    @DisplayName("RG04 : Une demande en attente peut être modifiée ou annulée par son auteur")
    void testRG04_ModificationEtAnnulationDemandeEnAttente() {
        LocalDate lundi = LocalDate.now().plusWeeks(1);
        while (lundi.getDayOfWeek() != DayOfWeek.MONDAY) {
            lundi = lundi.plusDays(1);
        }
        LocalDate mardi = lundi.plusDays(1);
        LocalDate mercredi = lundi.plusDays(2);
        LocalDate jeudi = lundi.plusDays(3);

        DemandeConge d = new DemandeConge();
        d.setDateDebut(lundi);
        d.setDateFin(mercredi);
        d.setTypeConge(TypeConge.ANNUEL);
        d.setMotif("Vacances initiales");

        DemandeConge cree = demandeCongeService.creerDemande(d, employeEnt1);
        assertTrue(cree.getStatut().isEnAttente());
        assertEquals("SOUMISE", cree.getStatut().getLibelle());

        // Modification
        DemandeConge modifiee = demandeCongeService.modifierDemande(
                cree.getId(),
                mardi,
                jeudi,
                TypeConge.ANNUEL,
                "Dates modifiées",
                null,
                employeEnt1.getId()
        );
        assertEquals("Dates modifiées", modifiee.getMotif());
        assertEquals(mardi, modifiee.getDateDebut());

        // Annulation
        DemandeConge annulee = demandeCongeService.annulerDemande(cree.getId(), employeEnt1.getId());
        assertEquals(StatutConge.ANNULEE, annulee.getStatut());

        final LocalDate futureDebut = lundi.plusWeeks(2);
        final LocalDate futureFin = mardi.plusWeeks(2);

        // Tenter de modifier une demande déjà annulée doit lever une exception
        assertThrows(IllegalStateException.class, () ->
                demandeCongeService.modifierDemande(
                        cree.getId(),
                        futureDebut,
                        futureFin,
                        TypeConge.ANNUEL,
                        "Tentative",
                        null,
                        employeEnt1.getId()
                )
        );
    }

    @Test
    @DisplayName("RG05 : Une demande approuvée déclenche exactement une déduction du solde")
    void testRG05_DeductionExacteSoldeApresApprobation() {
        int annee = Year.now().getValue();
        SoldeConge soldeAvant = soldeCongeService.getOuCreerSolde(employeEnt1.getId(), annee);
        int joursRestantsAvant = soldeAvant.getJoursRestants();

        DemandeConge d = new DemandeConge();
        // 2 jours ouvrables (lundi au mardi)
        LocalDate lundi = LocalDate.now().plusWeeks(2);
        while (lundi.getDayOfWeek().getValue() > 5) {
            lundi = lundi.plusDays(1);
        }
        LocalDate mardi = lundi.plusDays(1);

        d.setDateDebut(lundi);
        d.setDateFin(mardi);
        d.setTypeConge(TypeConge.ANNUEL);
        d.setMotif("Court séjour");

        DemandeConge cree = demandeCongeService.creerDemande(d, employeEnt1);
        assertEquals(2, cree.getNombreJours());

        // Validation par le Manager
        DemandeConge approuvee = demandeCongeService.approuverDemande(cree.getId(), managerEnt1);
        assertEquals(StatutConge.APPROUVEE, approuvee.getStatut());

        SoldeConge soldeApres = soldeRepository.findByUtilisateur_IdAndAnnee(employeEnt1.getId(), annee).orElseThrow();
        assertEquals(joursRestantsAvant - 2, soldeApres.getJoursRestants(), "Le solde doit être exactement déduit de 2 jours");

        // Éviter double déduction si approuverDemande est rappelé
        demandeCongeService.approuverDemande(cree.getId(), managerEnt1);
        SoldeConge soldeApresReapp = soldeRepository.findByUtilisateur_IdAndAnnee(employeEnt1.getId(), annee).orElseThrow();
        assertEquals(joursRestantsAvant - 2, soldeApresReapp.getJoursRestants(), "Aucune double déduction ne doit survenir");
    }

    @Test
    @DisplayName("RG06 : Un employé/manager ne peut pas approuver sa propre demande (auto-approbation interdite)")
    void testRG06_InterdictionAutoApprobation() {
        DemandeConge d = new DemandeConge();
        LocalDate debut = LocalDate.now().plusWeeks(3);
        while (debut.getDayOfWeek().getValue() > 5) debut = debut.plusDays(1);
        d.setDateDebut(debut);
        d.setDateFin(debut);
        d.setTypeConge(TypeConge.ANNUEL);
        d.setMotif("Demande du manager");

        DemandeConge cree = demandeCongeService.creerDemande(d, managerEnt1);

        // Le manager tente de valider sa propre demande -> DOIT ÉCHOUER
        assertThrows(IllegalStateException.class, () ->
                demandeCongeService.approuverDemande(cree.getId(), managerEnt1)
        );

        // La validation par le RH doit réussir
        DemandeConge valideeRh = demandeCongeService.approuverDemande(cree.getId(), rhEnt1);
        assertEquals(StatutConge.APPROUVEE, valideeRh.getStatut());
    }

    @Test
    @DisplayName("RG07 : Chaque décision de validation est enregistrée avec date, acteur et commentaire éventuel")
    void testRG07_TracabiliteDecisionValidation() {
        DemandeConge d = new DemandeConge();
        LocalDate debut = LocalDate.now().plusWeeks(4);
        while (debut.getDayOfWeek().getValue() > 5) debut = debut.plusDays(1);
        d.setDateDebut(debut);
        d.setDateFin(debut);
        d.setTypeConge(TypeConge.ANNUEL);
        d.setMotif("Demande rejet");

        DemandeConge cree = demandeCongeService.creerDemande(d, employeEnt1);

        DemandeConge rejetee = demandeCongeService.refuserDemande(
                cree.getId(), managerEnt1, "Période de haute activité sur le projet"
        );

        assertTrue(rejetee.getStatut().isRejetee());
        assertEquals("REJETÉE", rejetee.getStatut().getLibelle());
        assertEquals(managerEnt1.getId(), rejetee.getValideur().getId());
        assertNotNull(rejetee.getDateDecision());
        assertEquals("Période de haute activité sur le projet", rejetee.getCommentaireDecision());
    }

    @Test
    @DisplayName("RG08 : Les statistiques d'une entreprise excluent toutes les données des autres entreprises")
    void testRG08_StatistiquesIsoleesEntreprises() {
        // Demande dans entreprise 1
        DemandeConge d1 = new DemandeConge();
        LocalDate debut = LocalDate.now().plusWeeks(5);
        while (debut.getDayOfWeek().getValue() > 5) debut = debut.plusDays(1);
        d1.setDateDebut(debut);
        d1.setDateFin(debut);
        d1.setTypeConge(TypeConge.ANNUEL);
        d1.setMotif("Demande Alpha");
        demandeCongeService.creerDemande(d1, employeEnt1);

        Map<String, Object> statsEnt1 = statistiquesService.getStatsEntreprise(ent1.getId());
        Map<String, Object> statsEnt2 = statistiquesService.getStatsEntreprise(ent2.getId());

        assertEquals(1L, ((Number) statsEnt1.get("demandesEnAttente")).longValue());
        assertEquals(0L, ((Number) statsEnt2.get("demandesEnAttente")).longValue(),
                "Les statistiques de l'Entreprise 2 ne doivent pas compter les demandes de l'Entreprise 1");
    }
}
