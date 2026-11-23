package gestionconges.controller;

import gestionconges.entity.Entreprise;
import gestionconges.entity.Equipe;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.exception.GlobalExceptionHandler;
import gestionconges.exception.ResourceNotFoundException;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.EquipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du contrôleur EquipeRestController")
class EquipeRestControllerTest {

    private static final String BASE_URL = "/api/equipes";
    private static final String EMAIL_RH = "rh@entreprise.tn";

    @Mock private EquipeService equipeService;
    @Mock private UtilisateurRepository utilisateurRepository;
    @InjectMocks private EquipeRestController controller;

    private MockMvc mockMvc;
    private final Principal principalRh = () -> EMAIL_RH;

    private Entreprise entreprise;
    private Utilisateur rh;
    private Equipe equipe;

    @BeforeEach
    void setUp() {
        // standaloneSetup : on ne charge que le contrôleur (+ le handler d'erreurs), sans Spring Security ni base de données
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        entreprise = new Entreprise("LeaveFlow", "LF-001");
        entreprise.setId(1L);

        rh = new Utilisateur();
        rh.setId(10L);
        rh.setEmail(EMAIL_RH);
        rh.setRole(Role.RH);
        rh.setEntreprise(entreprise);

        equipe = new Equipe("Développement", entreprise);
        equipe.setId(100L);
        equipe.setDescription("Équipe technique");
    }

    /** Simule l'utilisateur connecté (le contrôleur le retrouve via son e-mail). */
    private void connecter(Utilisateur user) {
        when(utilisateurRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    // ------------------------------------------------------------------ CREATE

    @Test
    @DisplayName("POST - création réussie : 201 + corps de la réponse")
    void creerEquipe_succes() throws Exception {
        connecter(rh);
        when(equipeService.creerEquipe(1L, "Développement", "Équipe technique", null)).thenReturn(equipe);

        mockMvc.perform(post(BASE_URL).principal(principalRh)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Développement\",\"description\":\"Équipe technique\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.nom").value("Développement"))
                .andExpect(jsonPath("$.description").value("Équipe technique"))
                .andExpect(jsonPath("$.active").value(true));

        verify(equipeService).creerEquipe(1L, "Développement", "Équipe technique", null);
    }

    @Test
    @DisplayName("POST - nom vide : 400 et le service n'est jamais appelé")
    void creerEquipe_nomVide_400() throws Exception {
        mockMvc.perform(post(BASE_URL).principal(principalRh)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.nom").value("Le nom de l'équipe est obligatoire"));

        verify(equipeService, never()).creerEquipe(anyLong(), anyString(), any(), any());
    }

    @Test
    @DisplayName("POST - utilisateur non RH : 403")
    void creerEquipe_nonRh_403() throws Exception {
        Utilisateur employe = new Utilisateur();
        employe.setEmail("emp@entreprise.tn");
        employe.setRole(Role.EMPLOYE);
        employe.setEntreprise(entreprise);
        connecter(employe);

        mockMvc.perform(post(BASE_URL).principal(() -> "emp@entreprise.tn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Test\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Seul le RH peut créer une équipe"));

        verify(equipeService, never()).creerEquipe(anyLong(), anyString(), any(), any());
    }

    // ---------------------------------------------------------------- READ ALL

    @Test
    @DisplayName("GET all - retourne la liste des équipes de l'entreprise")
    void getEquipes_succes() throws Exception {
        Equipe autre = new Equipe("Marketing", entreprise);
        autre.setId(101L);
        connecter(rh);
        when(equipeService.getEquipes(1L)).thenReturn(List.of(equipe, autre));

        mockMvc.perform(get(BASE_URL).principal(principalRh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nom").value("Développement"))
                .andExpect(jsonPath("$[1].nom").value("Marketing"));
    }

    @Test
    @DisplayName("GET all - utilisateur sans entreprise : liste vide, service non appelé")
    void getEquipes_sansEntreprise_listeVide() throws Exception {
        rh.setEntreprise(null);
        connecter(rh);

        mockMvc.perform(get(BASE_URL).principal(principalRh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(equipeService, never()).getEquipes(anyLong());
    }

    // ------------------------------------------------------------- READ BY ID

    @Test
    @DisplayName("GET /{id} - équipe existante : 200 + corps")
    void getEquipe_existante() throws Exception {
        connecter(rh);
        when(equipeService.trouverParIdEtEntreprise(100L, 1L)).thenReturn(equipe);

        mockMvc.perform(get(BASE_URL + "/100").principal(principalRh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.nom").value("Développement"));
    }

    @Test
    @DisplayName("GET /{id} - équipe introuvable : 404")
    void getEquipe_introuvable_404() throws Exception {
        connecter(rh);
        when(equipeService.trouverParIdEtEntreprise(999L, 1L))
                .thenThrow(new ResourceNotFoundException("Équipe", 999L));

        mockMvc.perform(get(BASE_URL + "/999").principal(principalRh))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Équipe introuvable avec l'identifiant : 999"));
    }

    // ------------------------------------------------------------------ UPDATE

    @Test
    @DisplayName("PUT - modification réussie : 200 + corps mis à jour")
    void modifierEquipe_succes() throws Exception {
        Equipe modifiee = new Equipe("Dev & Ops", entreprise);
        modifiee.setId(100L);
        modifiee.setDescription("Nouvelle description");
        connecter(rh);
        when(equipeService.modifierEquipe(100L, 1L, "Dev & Ops", "Nouvelle description", null, true))
                .thenReturn(modifiee);

        mockMvc.perform(put(BASE_URL + "/100").principal(principalRh)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Dev & Ops\",\"description\":\"Nouvelle description\",\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Dev & Ops"))
                .andExpect(jsonPath("$.description").value("Nouvelle description"));

        verify(equipeService).modifierEquipe(100L, 1L, "Dev & Ops", "Nouvelle description", null, true);
    }

    @Test
    @DisplayName("PUT - équipe introuvable : 404")
    void modifierEquipe_introuvable_404() throws Exception {
        connecter(rh);
        when(equipeService.modifierEquipe(eq(999L), eq(1L), anyString(), any(), any(), anyBoolean()))
                .thenThrow(new ResourceNotFoundException("Équipe", 999L));

        mockMvc.perform(put(BASE_URL + "/999").principal(principalRh)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Équipe introuvable avec l'identifiant : 999"));
    }

    // ------------------------------------------------------------------ DELETE

    @Test
    @DisplayName("DELETE - suppression réussie : 200 et le service est appelé")
    void supprimerEquipe_succes() throws Exception {
        connecter(rh);

        mockMvc.perform(delete(BASE_URL + "/100").principal(principalRh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Équipe supprimée avec succès"));

        verify(equipeService).supprimerEquipe(100L, 1L);
    }

    @Test
    @DisplayName("DELETE - équipe introuvable : 404")
    void supprimerEquipe_introuvable_404() throws Exception {
        connecter(rh);
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Équipe", 999L))
                .when(equipeService).supprimerEquipe(999L, 1L);

        mockMvc.perform(delete(BASE_URL + "/999").principal(principalRh))
                .andExpect(status().isNotFound());
    }
}