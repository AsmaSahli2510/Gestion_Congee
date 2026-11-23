package gestionconges.service.impl;

import gestionconges.entity.Entreprise;
import gestionconges.entity.Equipe;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.exception.ResourceNotFoundException;
import gestionconges.repository.EntrepriseRepository;
import gestionconges.repository.EquipeRepository;
import gestionconges.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du service EquipeService")
class EquipeServiceTest {

    private static final Long ENTREPRISE_ID = 1L;
    private static final Long EQUIPE_ID = 100L;

    @Mock private EquipeRepository equipeRepository;
    @Mock private EntrepriseRepository entrepriseRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @InjectMocks private EquipeServiceImpl equipeService;

    private Entreprise entreprise;
    private Equipe equipe;

    @BeforeEach
    void setUp() {
        entreprise = new Entreprise("LeaveFlow", "LF-001");
        entreprise.setId(ENTREPRISE_ID);

        equipe = new Equipe("Développement", entreprise);
        equipe.setId(EQUIPE_ID);
    }

    // ------------------------------------------------------------------ CREATE

    @Test
    @DisplayName("creerEquipe - sauvegarde l'équipe et retourne l'entité (nom trimé)")
    void creerEquipe_succes() {
        when(entrepriseRepository.findById(ENTREPRISE_ID)).thenReturn(Optional.of(entreprise));
        when(equipeRepository.save(any(Equipe.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipe resultat = equipeService.creerEquipe(ENTREPRISE_ID, "  Marketing  ", "Équipe com", null);

        assertThat(resultat.getNom()).isEqualTo("Marketing");
        assertThat(resultat.getDescription()).isEqualTo("Équipe com");
        assertThat(resultat.getEntreprise()).isSameAs(entreprise);
        assertThat(resultat.getManager()).isNull();
        verify(equipeRepository).save(any(Equipe.class));
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerEquipe - avec manager : le manager est rattaché à l'équipe et sauvegardé")
    void creerEquipe_avecManager() {
        Utilisateur manager = new Utilisateur();
        manager.setId(20L);
        when(entrepriseRepository.findById(ENTREPRISE_ID)).thenReturn(Optional.of(entreprise));
        when(utilisateurRepository.findByIdAndEntreprise_Id(20L, ENTREPRISE_ID)).thenReturn(Optional.of(manager));
        when(equipeRepository.save(any(Equipe.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipe resultat = equipeService.creerEquipe(ENTREPRISE_ID, "Support", null, 20L);

        assertThat(resultat.getManager()).isSameAs(manager);
        assertThat(manager.getEquipe()).isSameAs(resultat);
        verify(utilisateurRepository).save(manager);
    }

    @Test
    @DisplayName("creerEquipe - entreprise inexistante : ResourceNotFoundException, rien n'est sauvegardé")
    void creerEquipe_entrepriseIntrouvable() {
        when(entrepriseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipeService.creerEquipe(99L, "Test", null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Entreprise");

        verify(equipeRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- READ ALL

    @Test
    @DisplayName("getEquipes - retourne toutes les équipes de l'entreprise")
    void getEquipes_retourneListe() {
        Equipe autre = new Equipe("Marketing", entreprise);
        when(equipeRepository.findByEntreprise_Id(ENTREPRISE_ID)).thenReturn(List.of(equipe, autre));

        List<Equipe> resultat = equipeService.getEquipes(ENTREPRISE_ID);

        assertThat(resultat).hasSize(2).containsExactly(equipe, autre);
        verify(equipeRepository).findByEntreprise_Id(ENTREPRISE_ID);
    }

    @Test
    @DisplayName("getEquipes - aucune équipe : liste vide")
    void getEquipes_listeVide() {
        when(equipeRepository.findByEntreprise_Id(ENTREPRISE_ID)).thenReturn(List.of());

        assertThat(equipeService.getEquipes(ENTREPRISE_ID)).isEmpty();
    }

    // ------------------------------------------------------------- READ BY ID

    @Test
    @DisplayName("trouverParIdEtEntreprise - équipe existante")
    void trouverParId_existante() {
        when(equipeRepository.findByIdAndEntreprise_Id(EQUIPE_ID, ENTREPRISE_ID)).thenReturn(Optional.of(equipe));

        Equipe resultat = equipeService.trouverParIdEtEntreprise(EQUIPE_ID, ENTREPRISE_ID);

        assertThat(resultat).isSameAs(equipe);
    }

    @Test
    @DisplayName("trouverParIdEtEntreprise - introuvable : ResourceNotFoundException")
    void trouverParId_introuvable() {
        when(equipeRepository.findByIdAndEntreprise_Id(999L, ENTREPRISE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipeService.trouverParIdEtEntreprise(999L, ENTREPRISE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Équipe introuvable avec l'identifiant : 999");
    }

    // ------------------------------------------------------------------ UPDATE

    @Test
    @DisplayName("modifierEquipe - récupère, modifie et sauvegarde l'équipe")
    void modifierEquipe_succes() {
        when(equipeRepository.findByIdAndEntreprise_Id(EQUIPE_ID, ENTREPRISE_ID)).thenReturn(Optional.of(equipe));
        when(equipeRepository.save(equipe)).thenReturn(equipe);

        Equipe resultat = equipeService.modifierEquipe(EQUIPE_ID, ENTREPRISE_ID, "  Dev & Ops ", "Nouvelle desc", null, false);

        assertThat(resultat.getNom()).isEqualTo("Dev & Ops");
        assertThat(resultat.getDescription()).isEqualTo("Nouvelle desc");
        assertThat(resultat.isActive()).isFalse();
        assertThat(resultat.getManager()).isNull();
        verify(equipeRepository).save(equipe);
    }

    @Test
    @DisplayName("modifierEquipe - un EMPLOYE désigné manager devient MANAGER")
    void modifierEquipe_promeutLeManager() {
        Utilisateur employe = new Utilisateur();
        employe.setId(30L);
        employe.setRole(Role.EMPLOYE);
        when(equipeRepository.findByIdAndEntreprise_Id(EQUIPE_ID, ENTREPRISE_ID)).thenReturn(Optional.of(equipe));
        when(utilisateurRepository.findByIdAndEntreprise_Id(30L, ENTREPRISE_ID)).thenReturn(Optional.of(employe));
        when(equipeRepository.save(equipe)).thenReturn(equipe);

        Equipe resultat = equipeService.modifierEquipe(EQUIPE_ID, ENTREPRISE_ID, "Dev", null, 30L, true);

        assertThat(resultat.getManager()).isSameAs(employe);
        assertThat(employe.getRole()).isEqualTo(Role.MANAGER);
        assertThat(employe.getEquipe()).isSameAs(equipe);
        verify(utilisateurRepository).save(employe);
    }

    @Test
    @DisplayName("modifierEquipe - introuvable : exception et aucune sauvegarde")
    void modifierEquipe_introuvable() {
        when(equipeRepository.findByIdAndEntreprise_Id(999L, ENTREPRISE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipeService.modifierEquipe(999L, ENTREPRISE_ID, "X", null, null, true))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(equipeRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ DELETE

    @Test
    @DisplayName("supprimerEquipe - détache les membres puis supprime l'équipe")
    void supprimerEquipe_succes() {
        Utilisateur membre = new Utilisateur();
        membre.setEquipe(equipe);
        when(equipeRepository.findByIdAndEntreprise_Id(EQUIPE_ID, ENTREPRISE_ID)).thenReturn(Optional.of(equipe));
        when(utilisateurRepository.findByEquipe_Id(EQUIPE_ID)).thenReturn(List.of(membre));

        equipeService.supprimerEquipe(EQUIPE_ID, ENTREPRISE_ID);

        assertThat(membre.getEquipe()).isNull();
        verify(utilisateurRepository).saveAll(List.of(membre));
        verify(equipeRepository).delete(equipe);
    }

    @Test
    @DisplayName("supprimerEquipe - introuvable : exception et aucune suppression")
    void supprimerEquipe_introuvable() {
        when(equipeRepository.findByIdAndEntreprise_Id(999L, ENTREPRISE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipeService.supprimerEquipe(999L, ENTREPRISE_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(equipeRepository, never()).delete(any());
    }
}