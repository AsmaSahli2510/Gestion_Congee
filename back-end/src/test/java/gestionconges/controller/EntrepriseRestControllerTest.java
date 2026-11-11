package gestionconges.controller;

import gestionconges.entity.Entreprise;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.exception.GlobalExceptionHandler;
import gestionconges.exception.ResourceNotFoundException;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.EntrepriseService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du contrôleur EntrepriseRestController")
class EntrepriseRestControllerTest {

    private static final String BASE_URL = "/api/entreprise";
    private static final String EMAIL_RH = "rh@acme.tn";

    @Mock private EntrepriseService entrepriseService;
    @Mock private UtilisateurRepository utilisateurRepository;
    @InjectMocks private EntrepriseRestController controller;

    private MockMvc mockMvc;
    private final Principal principalRh = () -> EMAIL_RH;

    private Entreprise entreprise;
    private Utilisateur rh;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        entreprise = new Entreprise("Acme", "ACME-1234");
        entreprise.setId(1L);
        entreprise.setSecteur("Technologie");

        rh = new Utilisateur();
        rh.setId(10L);
        rh.setEmail(EMAIL_RH);
        rh.setRole(Role.RH);
        rh.setEntreprise(entreprise);
    }

    private void connecter(Utilisateur user) {
        when(utilisateurRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    // -------------------------------------------------------------------- READ

    @Test
    @DisplayName("GET - retourne l'entreprise de l'utilisateur connecté : 200 + corps")
    void getEntreprise_succes() throws Exception {
        connecter(rh);

        mockMvc.perform(get(BASE_URL).principal(principalRh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nom").value("Acme"))
                .andExpect(jsonPath("$.secteur").value("Technologie"));
    }

    @Test
    @DisplayName("GET - utilisateur sans entreprise : 404")
    void getEntreprise_sansEntreprise_404() throws Exception {
        rh.setEntreprise(null);
        connecter(rh);

        mockMvc.perform(get(BASE_URL).principal(principalRh))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Aucune entreprise rattachée"));
    }

    @Test
    @DisplayName("GET - utilisateur connecté introuvable : 400")
    void getEntreprise_utilisateurIntrouvable_400() throws Exception {
        when(utilisateurRepository.findByEmail(EMAIL_RH)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL).principal(principalRh))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ UPDATE

    @Test
    @DisplayName("PUT - le RH modifie l'entreprise : 200 + entreprise mise à jour")
    void modifierEntreprise_succes() throws Exception {
        Entreprise miseAJour = new Entreprise("Nouveau Nom", "ACME-1234");
        miseAJour.setId(1L);
        miseAJour.setSecteur("Finance");
        connecter(rh);
        when(entrepriseService.mettreAJourEntreprise(1L, "Nouveau Nom", "Finance", "Description", 25))
                .thenReturn(miseAJour);

        mockMvc.perform(put(BASE_URL).principal(principalRh)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Nouveau Nom\",\"secteur\":\"Finance\","
                                + "\"description\":\"Description\",\"joursCongesAnnuels\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.entreprise.nom").value("Nouveau Nom"))
                .andExpect(jsonPath("$.entreprise.secteur").value("Finance"));

        verify(entrepriseService).mettreAJourEntreprise(1L, "Nouveau Nom", "Finance", "Description", 25);
    }

    @Test
    @DisplayName("PUT - alias 'name' et 'acquisition' (2.5 x 12 = 30 jours) acceptés")
    void modifierEntreprise_alias() throws Exception {
        connecter(rh);
        when(entrepriseService.mettreAJourEntreprise(1L, "Alias", null, null, 30)).thenReturn(entreprise);

        mockMvc.perform(put(BASE_URL).principal(principalRh)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alias\",\"acquisition\":2.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(entrepriseService).mettreAJourEntreprise(1L, "Alias", null, null, 30);
    }

    @Test
    @DisplayName("PUT - utilisateur non RH : 403 et le service n'est pas appelé")
    void modifierEntreprise_nonRh_403() throws Exception {
        rh.setRole(Role.EMPLOYE);
        connecter(rh);

        mockMvc.perform(put(BASE_URL).principal(principalRh)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"X\"}"))
                .andExpect(status().isForbidden());

        verify(entrepriseService, never()).mettreAJourEntreprise(anyLong(), anyString(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("PUT - entreprise introuvable côté service : 404")
    void modifierEntreprise_introuvable_404() throws Exception {
        connecter(rh);
        when(entrepriseService.mettreAJourEntreprise(any(), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Entreprise", 1L));

        mockMvc.perform(put(BASE_URL).principal(principalRh)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"X\"}"))
                .andExpect(status().isNotFound());
    }
}