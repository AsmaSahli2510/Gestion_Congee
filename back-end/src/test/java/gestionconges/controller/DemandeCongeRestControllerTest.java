package gestionconges.controller;

import gestionconges.entity.DemandeConge;
import gestionconges.entity.Entreprise;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.enums.StatutConge;
import gestionconges.enums.TypeConge;
import gestionconges.exception.GlobalExceptionHandler;
import gestionconges.exception.ResourceNotFoundException;
import gestionconges.repository.EquipeRepository;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.DemandeCongeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du contrôleur DemandeCongeRestController")
class DemandeCongeRestControllerTest {

    private static final String BASE_URL = "/api/demandes";
    private static final String EMAIL = "sara@entreprise.tn";

    // Lundi 7 décembre 2026 -> vendredi 11 décembre 2026 (5 jours ouvrables)
    private static final LocalDate DEBUT = LocalDate.of(2026, 12, 7);
    private static final LocalDate FIN = LocalDate.of(2026, 12, 11);

    @Mock private DemandeCongeService demandeCongeService;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private EquipeRepository equipeRepository;
    @InjectMocks private DemandeCongeRestController controller;

    private MockMvc mockMvc;
    private final Principal principal = () -> EMAIL;

    private Entreprise entreprise;
    private Utilisateur employe;
    private DemandeConge demande;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        entreprise = new Entreprise("LeaveFlow", "LF-001");
        entreprise.setId(1L);

        employe = new Utilisateur();
        employe.setId(10L);
        employe.setEmail(EMAIL);
        employe.setNom("Zarrad");
        employe.setPrenom("Sara");
        employe.setRole(Role.EMPLOYE);
        employe.setEntreprise(entreprise);

        demande = creerDemande(100L, StatutConge.EN_ATTENTE);
    }

    private DemandeConge creerDemande(Long id, StatutConge statut) {
        DemandeConge d = new DemandeConge();
        d.setId(id);
        d.setUtilisateur(employe);
        d.setEntreprise(entreprise);
        d.setDateDebut(DEBUT);
        d.setDateFin(FIN);
        d.setNombreJours(5);
        d.setTypeConge(TypeConge.ANNUEL);
        d.setMotif("Vacances");
        d.setStatut(statut);
        return d;
    }

    private void connecter(Utilisateur user) {
        when(utilisateurRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    // ------------------------------------------------------------------ CREATE

    @Test
    @DisplayName("POST - création réussie : 201 + corps, et le service reçoit les bonnes données")
    void creerDemande_succes() throws Exception {
        connecter(employe);
        when(demandeCongeService.creerDemande(any(DemandeConge.class), eq(employe))).thenReturn(demande);

        mockMvc.perform(post(BASE_URL).principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateDebut\":\"2026-12-07\",\"dateFin\":\"2026-12-11\","
                                + "\"typeConge\":\"ANNUEL\",\"motif\":\"Vacances\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.motif").value("Vacances"))
                .andExpect(jsonPath("$.days").value(5))
                .andExpect(jsonPath("$.start").value("2026-12-07"))
                .andExpect(jsonPath("$.end").value("2026-12-11"));

        // On vérifie que le contrôleur a bien converti le JSON en entité avant d'appeler le service
        ArgumentCaptor<DemandeConge> captor = ArgumentCaptor.forClass(DemandeConge.class);
        verify(demandeCongeService).creerDemande(captor.capture(), eq(employe));
        assertThat(captor.getValue().getDateDebut()).isEqualTo(DEBUT);
        assertThat(captor.getValue().getDateFin()).isEqualTo(FIN);
        assertThat(captor.getValue().getTypeConge()).isEqualTo(TypeConge.ANNUEL);
        assertThat(captor.getValue().getMotif()).isEqualTo("Vacances");
    }

    @Test
    @DisplayName("POST - date de début manquante : 400 et le service n'est pas appelé")
    void creerDemande_dateManquante_400() throws Exception {
        mockMvc.perform(post(BASE_URL).principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateFin\":\"2026-12-11\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.dateDebut").value("La date de début est obligatoire"));

        verify(demandeCongeService, never()).creerDemande(any(), any());
    }

    @Test
    @DisplayName("POST - règle métier violée (solde insuffisant) : 400 avec le message")
    void creerDemande_soldeInsuffisant_400() throws Exception {
        connecter(employe);
        when(demandeCongeService.creerDemande(any(DemandeConge.class), eq(employe)))
                .thenThrow(new IllegalArgumentException("Solde insuffisant"));

        mockMvc.perform(post(BASE_URL).principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateDebut\":\"2026-12-07\",\"dateFin\":\"2026-12-11\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Solde insuffisant"));
    }

    // ---------------------------------------------------------------- READ ALL

    @Test
    @DisplayName("GET all - un employé reçoit uniquement ses propres demandes")
    void getDemandes_employe() throws Exception {
        connecter(employe);
        when(demandeCongeService.getDemandesParUtilisateur(10L))
                .thenReturn(List.of(demande, creerDemande(101L, StatutConge.APPROUVEE)));

        mockMvc.perform(get(BASE_URL).principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[1].id").value(101));

        verify(demandeCongeService, never()).getDemandesEntreprise(anyLong());
    }

    @Test
    @DisplayName("GET all - le RH reçoit les demandes de toute l'entreprise")
    void getDemandes_rh() throws Exception {
        Utilisateur rh = new Utilisateur();
        rh.setId(20L);
        rh.setEmail("rh@entreprise.tn");
        rh.setRole(Role.RH);
        rh.setEntreprise(entreprise);
        when(utilisateurRepository.findByEmail("rh@entreprise.tn")).thenReturn(Optional.of(rh));
        when(demandeCongeService.getDemandesEntreprise(1L)).thenReturn(List.of(demande));

        mockMvc.perform(get(BASE_URL).principal(() -> "rh@entreprise.tn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET all - aucune demande : liste vide")
    void getDemandes_listeVide() throws Exception {
        connecter(employe);
        when(demandeCongeService.getDemandesParUtilisateur(10L)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL).principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET all - le paramètre ?statut=APPROUVEE filtre les résultats")
    void getDemandes_filtreStatut() throws Exception {
        connecter(employe);
        when(demandeCongeService.getDemandesParUtilisateur(10L))
                .thenReturn(List.of(demande, creerDemande(101L, StatutConge.APPROUVEE)));

        mockMvc.perform(get(BASE_URL).param("statut", "APPROUVEE").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(101));
    }

    // ------------------------------------------------------------- READ BY ID

    @Test
    @DisplayName("GET /{id} - demande existante : 200 + corps")
    void getDemande_existante() throws Exception {
        connecter(employe);
        when(demandeCongeService.trouverDemande(100L)).thenReturn(demande);

        mockMvc.perform(get(BASE_URL + "/100").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.motif").value("Vacances"))
                .andExpect(jsonPath("$.userId").value(10));
    }

    @Test
    @DisplayName("GET /{id} - demande introuvable : 404")
    void getDemande_introuvable_404() throws Exception {
        connecter(employe);
        when(demandeCongeService.trouverDemande(999L))
                .thenThrow(new ResourceNotFoundException("Demande de congé introuvable : 999"));

        mockMvc.perform(get(BASE_URL + "/999").principal(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Demande de congé introuvable : 999"));
    }

    @Test
    @DisplayName("GET /{id} - demande d'une autre entreprise : 403 (règle RG06)")
    void getDemande_autreEntreprise_403() throws Exception {
        Entreprise autre = new Entreprise("Autre", "AU-002");
        autre.setId(2L);
        demande.setEntreprise(autre);
        connecter(employe);
        when(demandeCongeService.trouverDemande(100L)).thenReturn(demande);

        mockMvc.perform(get(BASE_URL + "/100").principal(principal))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ UPDATE

    @Test
    @DisplayName("PUT - modification réussie : 200 + corps mis à jour")
    void modifierDemande_succes() throws Exception {
        DemandeConge modifiee = creerDemande(100L, StatutConge.EN_ATTENTE);
        modifiee.setMotif("Nouveau motif");
        connecter(employe);
        when(demandeCongeService.modifierDemande(100L, DEBUT, FIN, TypeConge.ANNUEL, "Nouveau motif", null, 10L))
                .thenReturn(modifiee);

        mockMvc.perform(put(BASE_URL + "/100").principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateDebut\":\"2026-12-07\",\"dateFin\":\"2026-12-11\","
                                + "\"typeConge\":\"ANNUEL\",\"motif\":\"Nouveau motif\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.motif").value("Nouveau motif"));

        verify(demandeCongeService).modifierDemande(100L, DEBUT, FIN, TypeConge.ANNUEL, "Nouveau motif", null, 10L);
    }

    @Test
    @DisplayName("PUT - demande introuvable : 404")
    void modifierDemande_introuvable_404() throws Exception {
        connecter(employe);
        when(demandeCongeService.modifierDemande(eq(999L), any(), any(), any(), any(), any(), eq(10L)))
                .thenThrow(new ResourceNotFoundException("Demande de congé introuvable : 999"));

        mockMvc.perform(put(BASE_URL + "/999").principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateDebut\":\"2026-12-07\",\"dateFin\":\"2026-12-11\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Demande de congé introuvable : 999"));
    }

    @Test
    @DisplayName("PUT - demande déjà traitée (non modifiable) : 400")
    void modifierDemande_dejaTraitee_400() throws Exception {
        connecter(employe);
        when(demandeCongeService.modifierDemande(eq(100L), any(), any(), any(), any(), any(), eq(10L)))
                .thenThrow(new IllegalStateException("Seules les demandes en attente peuvent être modifiées."));

        mockMvc.perform(put(BASE_URL + "/100").principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateDebut\":\"2026-12-07\",\"dateFin\":\"2026-12-11\"}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------- DELETE (= annulation)

    @Test
    @DisplayName("DELETE - annulation réussie : 200 et le service annulerDemande est appelé")
    void annulerDemande_succes() throws Exception {
        DemandeConge annulee = creerDemande(100L, StatutConge.ANNULEE);
        connecter(employe);
        when(demandeCongeService.annulerDemande(100L, 10L)).thenReturn(annulee);

        mockMvc.perform(delete(BASE_URL + "/100").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.demande.id").value(100));

        verify(demandeCongeService).annulerDemande(100L, 10L);
    }

    @Test
    @DisplayName("DELETE - demande introuvable : 404")
    void annulerDemande_introuvable_404() throws Exception {
        connecter(employe);
        when(demandeCongeService.annulerDemande(999L, 10L))
                .thenThrow(new ResourceNotFoundException("Demande de congé introuvable : 999"));

        mockMvc.perform(delete(BASE_URL + "/999").principal(principal))
                .andExpect(status().isNotFound());
    }
}