package gestionconges.service.impl;

import gestionconges.entity.DemandeConge;
import gestionconges.entity.Entreprise;
import gestionconges.entity.Equipe;
import gestionconges.entity.SoldeConge;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.enums.StatutConge;
import gestionconges.enums.TypeConge;
import gestionconges.exception.ResourceNotFoundException;
import gestionconges.repository.DemandeCongeRepository;
import gestionconges.repository.MouvementSoldeRepository;
import gestionconges.repository.SoldeCongeRepository;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.NotificationService;
import gestionconges.service.SoldeCongeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du service DemandeCongeService")
class DemandeCongeServiceTest {

    // Lundi 7 décembre 2026 -> vendredi 11 décembre 2026 (5 jours ouvrables)
    private static final LocalDate LUNDI = LocalDate.of(2026, 12, 7);
    private static final LocalDate VENDREDI = LocalDate.of(2026, 12, 11);
    private static final LocalDate SAMEDI = LocalDate.of(2026, 12, 12);
    private static final LocalDate DIMANCHE = LocalDate.of(2026, 12, 13);

    @Mock private DemandeCongeRepository demandeRepository;
    @Mock private SoldeCongeRepository soldeRepository;
    @Mock private MouvementSoldeRepository mouvementSoldeRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private NotificationService notificationService;
    @Mock private SoldeCongeService soldeCongeService;
    @InjectMocks private DemandeCongeServiceImpl service;

    private Entreprise entreprise;
    private Utilisateur manager;
    private Utilisateur employe;

    @BeforeEach
    void setUp() {
        entreprise = new Entreprise("LeaveFlow", "LF-001");
        entreprise.setId(1L);

        manager = new Utilisateur();
        manager.setId(5L);
        manager.setRole(Role.MANAGER);
        manager.setEntreprise(entreprise);

        Equipe equipe = new Equipe("Dev", entreprise);
        equipe.setManager(manager);

        employe = new Utilisateur();
        employe.setId(10L);
        employe.setRole(Role.EMPLOYE);
        employe.setEntreprise(entreprise);
        employe.setEquipe(equipe);
    }

    // ----------------------------------------------------------------- helpers

    private DemandeConge nouvelleDemande(LocalDate debut, LocalDate fin, TypeConge type) {
        DemandeConge d = new DemandeConge();
        d.setDateDebut(debut);
        d.setDateFin(fin);
        d.setTypeConge(type);
        d.setMotif("Vacances");
        return d;
    }

    private DemandeConge demandeExistante(Long id, StatutConge statut, LocalDate debut, LocalDate fin) {
        DemandeConge d = nouvelleDemande(debut, fin, TypeConge.ANNUEL);
        d.setId(id);
        d.setUtilisateur(employe);
        d.setEntreprise(entreprise);
        d.setNombreJours(5);
        d.setStatut(statut);
        return d;
    }

    private SoldeConge solde(int acquis, int utilises) {
        return new SoldeConge(entreprise, employe, 2026, acquis, utilises);
    }

    // ------------------------------------------------------------------ CREATE

    @Test
    @DisplayName("creerDemande - sauvegarde la demande (5 jours, EN_ATTENTE) et notifie le manager")
    void creerDemande_succes() {
        DemandeConge demande = nouvelleDemande(LUNDI, VENDREDI, TypeConge.ANNUEL);
        when(demandeRepository.findByUtilisateur_Id(10L)).thenReturn(List.of());
        when(soldeCongeService.getOuCreerSolde(10L, 2026)).thenReturn(solde(30, 0));
        when(demandeRepository.save(any(DemandeConge.class))).thenAnswer(inv -> inv.getArgument(0));

        DemandeConge resultat = service.creerDemande(demande, employe);

        assertThat(resultat.getNombreJours()).isEqualTo(5);
        assertThat(resultat.getStatut()).isEqualTo(StatutConge.EN_ATTENTE);
        assertThat(resultat.getUtilisateur()).isSameAs(employe);
        assertThat(resultat.getEntreprise()).isSameAs(entreprise);
        verify(demandeRepository).save(demande);
        verify(notificationService).creerNotification(eq(entreprise), eq(manager), anyString(), anyString(),
                eq("/manager/demandes"));
    }

    @Test
    @DisplayName("creerDemande - demande d'un manager : tous les RH sont notifiés")
    void creerDemande_manager_notifieRh() {
        Utilisateur rh = new Utilisateur();
        rh.setId(99L);
        rh.setRole(Role.RH);
        when(demandeRepository.findByUtilisateur_Id(5L)).thenReturn(List.of());
        when(soldeCongeService.getOuCreerSolde(5L, 2026)).thenReturn(solde(30, 0));
        when(demandeRepository.save(any(DemandeConge.class))).thenAnswer(inv -> inv.getArgument(0));
        when(utilisateurRepository.findByEntreprise_IdAndRole(1L, Role.RH)).thenReturn(List.of(rh));

        service.creerDemande(nouvelleDemande(LUNDI, VENDREDI, TypeConge.ANNUEL), manager);

        verify(notificationService).creerNotification(eq(entreprise), eq(rh), anyString(), anyString(),
                eq("/rh/demandes"));
    }

    @Test
    @DisplayName("creerDemande - type non déduit du solde (MALADIE) : le solde n'est pas consulté")
    void creerDemande_typeNonDeduit() {
        when(demandeRepository.findByUtilisateur_Id(10L)).thenReturn(List.of());
        when(demandeRepository.save(any(DemandeConge.class))).thenAnswer(inv -> inv.getArgument(0));

        service.creerDemande(nouvelleDemande(LUNDI, VENDREDI, TypeConge.MALADIE), employe);

        verify(soldeCongeService, never()).getOuCreerSolde(any(), any());
    }

    @Test
    @DisplayName("creerDemande - dates manquantes : IllegalArgumentException, rien sauvegardé")
    void creerDemande_datesNulles() {
        DemandeConge demande = nouvelleDemande(null, null, TypeConge.ANNUEL);

        assertThatThrownBy(() -> service.creerDemande(demande, employe))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatoires");

        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerDemande - date de fin avant la date de début : IllegalArgumentException")
    void creerDemande_finAvantDebut() {
        DemandeConge demande = nouvelleDemande(VENDREDI, LUNDI, TypeConge.ANNUEL);

        assertThatThrownBy(() -> service.creerDemande(demande, employe))
                .isInstanceOf(IllegalArgumentException.class);

        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerDemande - période = week-end uniquement : IllegalArgumentException")
    void creerDemande_weekendSeulement() {
        DemandeConge demande = nouvelleDemande(SAMEDI, DIMANCHE, TypeConge.ANNUEL);

        assertThatThrownBy(() -> service.creerDemande(demande, employe))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aucun jour ouvrable");

        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerDemande - chevauchement avec une demande approuvée : IllegalArgumentException")
    void creerDemande_chevauchement() {
        DemandeConge existante = demandeExistante(50L, StatutConge.APPROUVEE, LUNDI.plusDays(2), VENDREDI.plusDays(2));
        when(demandeRepository.findByUtilisateur_Id(10L)).thenReturn(List.of(existante));

        assertThatThrownBy(() -> service.creerDemande(nouvelleDemande(LUNDI, VENDREDI, TypeConge.ANNUEL), employe))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà une demande");

        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerDemande - solde insuffisant : IllegalArgumentException, rien sauvegardé")
    void creerDemande_soldeInsuffisant() {
        when(demandeRepository.findByUtilisateur_Id(10L)).thenReturn(List.of());
        when(soldeCongeService.getOuCreerSolde(10L, 2026)).thenReturn(solde(30, 28)); // il reste 2 jours

        assertThatThrownBy(() -> service.creerDemande(nouvelleDemande(LUNDI, VENDREDI, TypeConge.ANNUEL), employe))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solde insuffisant");

        verify(demandeRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- READ ALL

    @Test
    @DisplayName("getDemandesParUtilisateur - retourne les demandes de l'utilisateur")
    void getDemandesParUtilisateur_liste() {
        DemandeConge d1 = demandeExistante(1L, StatutConge.EN_ATTENTE, LUNDI, VENDREDI);
        DemandeConge d2 = demandeExistante(2L, StatutConge.APPROUVEE, LUNDI.plusWeeks(2), VENDREDI.plusWeeks(2));
        when(demandeRepository.findByUtilisateur_IdOrderByDateCreationDesc(10L)).thenReturn(List.of(d1, d2));

        List<DemandeConge> resultat = service.getDemandesParUtilisateur(10L);

        assertThat(resultat).containsExactly(d1, d2);
    }

    @Test
    @DisplayName("getDemandesParUtilisateur - aucune demande : liste vide")
    void getDemandesParUtilisateur_vide() {
        when(demandeRepository.findByUtilisateur_IdOrderByDateCreationDesc(10L)).thenReturn(List.of());

        assertThat(service.getDemandesParUtilisateur(10L)).isEmpty();
    }

    @Test
    @DisplayName("getToutesLesDemandes - retourne findAll()")
    void getToutesLesDemandes() {
        DemandeConge d1 = demandeExistante(1L, StatutConge.EN_ATTENTE, LUNDI, VENDREDI);
        when(demandeRepository.findAll()).thenReturn(List.of(d1));

        assertThat(service.getToutesLesDemandes()).containsExactly(d1);
    }

    // ------------------------------------------------------------- READ BY ID

    @Test
    @DisplayName("trouverDemande - demande existante")
    void trouverDemande_existante() {
        DemandeConge d = demandeExistante(100L, StatutConge.EN_ATTENTE, LUNDI, VENDREDI);
        when(demandeRepository.findById(100L)).thenReturn(Optional.of(d));

        assertThat(service.trouverDemande(100L)).isSameAs(d);
    }

    @Test
    @DisplayName("trouverDemande - introuvable : ResourceNotFoundException")
    void trouverDemande_introuvable() {
        when(demandeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.trouverDemande(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Demande de congé introuvable : 999");
    }

    // ------------------------------------------------------------------ UPDATE

    @Test
    @DisplayName("modifierDemande - récupère, modifie (dates, jours, motif) et sauvegarde")
    void modifierDemande_succes() {
        DemandeConge existante = demandeExistante(100L, StatutConge.EN_ATTENTE, LUNDI, VENDREDI);
        LocalDate nouveauDebut = LocalDate.of(2026, 12, 14); // lundi
        LocalDate nouvelleFin = LocalDate.of(2026, 12, 16);  // mercredi -> 3 jours
        when(demandeRepository.findByIdAndUtilisateur_Id(100L, 10L)).thenReturn(Optional.of(existante));
        when(demandeRepository.findByUtilisateur_Id(10L)).thenReturn(List.of(existante)); // elle-même : ignorée
        when(soldeCongeService.getOuCreerSolde(10L, 2026)).thenReturn(solde(30, 0));
        when(demandeRepository.save(existante)).thenReturn(existante);

        DemandeConge resultat = service.modifierDemande(100L, nouveauDebut, nouvelleFin, TypeConge.ANNUEL,
                "Nouveau motif", null, 10L);

        assertThat(resultat.getDateDebut()).isEqualTo(nouveauDebut);
        assertThat(resultat.getDateFin()).isEqualTo(nouvelleFin);
        assertThat(resultat.getNombreJours()).isEqualTo(3);
        assertThat(resultat.getMotif()).isEqualTo("Nouveau motif");
        verify(demandeRepository).save(existante);
    }

    @Test
    @DisplayName("modifierDemande - introuvable : ResourceNotFoundException, rien sauvegardé")
    void modifierDemande_introuvable() {
        when(demandeRepository.findByIdAndUtilisateur_Id(999L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modifierDemande(999L, LUNDI, VENDREDI, TypeConge.ANNUEL, "x", null, 10L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("modifierDemande - demande déjà approuvée : IllegalStateException")
    void modifierDemande_nonModifiable() {
        DemandeConge approuvee = demandeExistante(100L, StatutConge.APPROUVEE, LUNDI, VENDREDI);
        when(demandeRepository.findByIdAndUtilisateur_Id(100L, 10L)).thenReturn(Optional.of(approuvee));

        assertThatThrownBy(() -> service.modifierDemande(100L, LUNDI, VENDREDI, TypeConge.ANNUEL, "x", null, 10L))
                .isInstanceOf(IllegalStateException.class);

        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("modifierDemande - date de fin avant le début : IllegalArgumentException")
    void modifierDemande_datesInvalides() {
        DemandeConge existante = demandeExistante(100L, StatutConge.EN_ATTENTE, LUNDI, VENDREDI);
        when(demandeRepository.findByIdAndUtilisateur_Id(100L, 10L)).thenReturn(Optional.of(existante));

        assertThatThrownBy(() -> service.modifierDemande(100L, VENDREDI, LUNDI, TypeConge.ANNUEL, "x", null, 10L))
                .isInstanceOf(IllegalArgumentException.class);

        verify(demandeRepository, never()).save(any());
    }

    // ------------------------------------------------- DELETE (= annulation)
    // Dans ce projet, « supprimer » une demande = la passer au statut ANNULEE (suppression logique).

    @Test
    @DisplayName("annulerDemande - passe le statut à ANNULEE et sauvegarde")
    void annulerDemande_succes() {
        DemandeConge existante = demandeExistante(100L, StatutConge.EN_ATTENTE, LUNDI, VENDREDI);
        when(demandeRepository.findByIdAndUtilisateur_Id(100L, 10L)).thenReturn(Optional.of(existante));
        when(demandeRepository.save(existante)).thenReturn(existante);

        DemandeConge resultat = service.annulerDemande(100L, 10L);

        assertThat(resultat.getStatut()).isEqualTo(StatutConge.ANNULEE);
        verify(demandeRepository).save(existante);
    }

    @Test
    @DisplayName("annulerDemande - introuvable : ResourceNotFoundException")
    void annulerDemande_introuvable() {
        when(demandeRepository.findByIdAndUtilisateur_Id(999L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.annulerDemande(999L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("annulerDemande - demande déjà approuvée : IllegalStateException, statut inchangé")
    void annulerDemande_nonAnnulable() {
        DemandeConge approuvee = demandeExistante(100L, StatutConge.APPROUVEE, LUNDI, VENDREDI);
        when(demandeRepository.findByIdAndUtilisateur_Id(100L, 10L)).thenReturn(Optional.of(approuvee));

        assertThatThrownBy(() -> service.annulerDemande(100L, 10L))
                .isInstanceOf(IllegalStateException.class);

        assertThat(approuvee.getStatut()).isEqualTo(StatutConge.APPROUVEE);
        verify(demandeRepository, never()).save(any());
    }

    // ------------------------------------------------- Calcul des jours ouvrables

    @Test
    @DisplayName("compterJoursOuvrables - lundi→vendredi = 5, vendredi→lundi = 2, week-end = 0, fin<début = 0")
    void compterJoursOuvrables() {
        assertThat(service.compterJoursOuvrables(LUNDI, VENDREDI)).isEqualTo(5);
        assertThat(service.compterJoursOuvrables(VENDREDI, LUNDI.plusWeeks(1))).isEqualTo(2);
        assertThat(service.compterJoursOuvrables(SAMEDI, DIMANCHE)).isZero();
        assertThat(service.compterJoursOuvrables(VENDREDI, LUNDI)).isZero();
    }
}