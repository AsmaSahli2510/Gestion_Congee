package gestionconges.service.impl;

import gestionconges.entity.Entreprise;
import gestionconges.entity.Equipe;
import gestionconges.entity.SoldeConge;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.enums.StatutUtilisateur;
import gestionconges.exception.ResourceNotFoundException;
import gestionconges.repository.EntrepriseRepository;
import gestionconges.repository.EquipeRepository;
import gestionconges.repository.MouvementSoldeRepository;
import gestionconges.repository.SoldeCongeRepository;
import gestionconges.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du service EntrepriseServiceImpl")
class EntrepriseServiceTest {

    @Mock private EntrepriseRepository entrepriseRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private EquipeRepository equipeRepository;
    @Mock private SoldeCongeRepository soldeCongeRepository;
    @Mock private MouvementSoldeRepository mouvementSoldeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private EntrepriseServiceImpl service;

    private Entreprise entreprise;

    @BeforeEach
    void setUp() {
        // Le champ @Value n'est pas rempli sans Spring : on le fixe à la main
        ReflectionTestUtils.setField(service, "defaultJoursConges", 25);

        entreprise = new Entreprise("Acme", "ACME-1234");
        entreprise.setId(1L);
        entreprise.setJoursCongesAnnuels(30);
    }

    // ------------------------------------------------------------------ CREATE

    @Test
    @DisplayName("creerNouvelleEntrepriseEtRh - crée l'entreprise, le RH, l'équipe par défaut, le solde et le mouvement")
    void creerNouvelleEntrepriseEtRh_succes() {
        when(utilisateurRepository.existsByEmail("rh@acme.tn")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("ENCODED");
        when(entrepriseRepository.save(any(Entreprise.class))).thenAnswer(inv -> inv.getArgument(0));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(equipeRepository.save(any(Equipe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(soldeCongeRepository.save(any(SoldeConge.class))).thenAnswer(inv -> inv.getArgument(0));

        Utilisateur rh = service.creerNouvelleEntrepriseEtRh(" Acme Corp ", " Ben Ali ", " Sara ", " RH@Acme.TN ", "secret");

        // Le RH
        assertThat(rh.getEmail()).isEqualTo("rh@acme.tn");          // e-mail nettoyé et en minuscules
        assertThat(rh.getNom()).isEqualTo("Ben Ali");
        assertThat(rh.getPrenom()).isEqualTo("Sara");
        assertThat(rh.getMotDePasse()).isEqualTo("ENCODED");        // mot de passe encodé
        assertThat(rh.getRole()).isEqualTo(Role.RH);
        assertThat(rh.getStatut()).isEqualTo(StatutUtilisateur.ACTIF);

        // L'entreprise : nom nettoyé et jours de congés = valeur de configuration
        assertThat(rh.getEntreprise().getNom()).isEqualTo("Acme Corp");
        assertThat(rh.getEntreprise().getJoursCongesAnnuels()).isEqualTo(25);

        // L'équipe par défaut
        assertThat(rh.getEquipe()).isNotNull();
        assertThat(rh.getEquipe().getNom()).isEqualTo("Direction & Administration");

        // Les sauvegardes
        verify(entrepriseRepository).save(any(Entreprise.class));
        verify(utilisateurRepository, times(2)).save(any(Utilisateur.class));
        verify(equipeRepository).save(any(Equipe.class));
        verify(soldeCongeRepository).save(any(SoldeConge.class));
        verify(mouvementSoldeRepository).save(any());
    }

    @Test
    @DisplayName("creerNouvelleEntrepriseEtRh - le solde initial correspond aux jours annuels de l'entreprise")
    void creerNouvelleEntrepriseEtRh_soldeInitial() {
        when(utilisateurRepository.existsByEmail("rh@acme.tn")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("ENCODED");
        when(entrepriseRepository.save(any(Entreprise.class))).thenAnswer(inv -> inv.getArgument(0));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(equipeRepository.save(any(Equipe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(soldeCongeRepository.save(any(SoldeConge.class))).thenAnswer(inv -> inv.getArgument(0));

        service.creerNouvelleEntrepriseEtRh("Acme", "Ben Ali", "Sara", "rh@acme.tn", "secret");

        ArgumentCaptor<SoldeConge> captor = ArgumentCaptor.forClass(SoldeConge.class);
        verify(soldeCongeRepository).save(captor.capture());
        assertThat(captor.getValue().getJoursAcquis()).isEqualTo(25);
        assertThat(captor.getValue().getJoursUtilises()).isZero();
    }

    @Test
    @DisplayName("creerNouvelleEntrepriseEtRh - e-mail déjà utilisé : IllegalArgumentException, rien n'est sauvegardé")
    void creerNouvelleEntrepriseEtRh_emailExistant() {
        when(utilisateurRepository.existsByEmail("rh@acme.tn")).thenReturn(true);

        assertThatThrownBy(() ->
                service.creerNouvelleEntrepriseEtRh("Acme", "Ben Ali", "Sara", "rh@acme.tn", "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existe déjà");

        verify(entrepriseRepository, never()).save(any());
        verify(utilisateurRepository, never()).save(any());
    }

    // ------------------------------------------------------------- READ BY ID

    @Test
    @DisplayName("trouverParId - entreprise existante")
    void trouverParId_existante() {
        when(entrepriseRepository.findById(1L)).thenReturn(Optional.of(entreprise));

        assertThat(service.trouverParId(1L)).isSameAs(entreprise);
    }

    @Test
    @DisplayName("trouverParId - introuvable : ResourceNotFoundException")
    void trouverParId_introuvable() {
        when(entrepriseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.trouverParId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Entreprise introuvable avec l'identifiant : 99");
    }

    // ------------------------------------------------------------------ UPDATE

    @Test
    @DisplayName("mettreAJourEntreprise - récupère, modifie (nom trimé, secteur, description, jours) et sauvegarde")
    void mettreAJourEntreprise_succes() {
        when(entrepriseRepository.findById(1L)).thenReturn(Optional.of(entreprise));
        when(entrepriseRepository.save(entreprise)).thenReturn(entreprise);

        Entreprise resultat = service.mettreAJourEntreprise(1L, " Nouveau Nom ", "Technologie", "Description", 20);

        assertThat(resultat.getNom()).isEqualTo("Nouveau Nom");
        assertThat(resultat.getSecteur()).isEqualTo("Technologie");
        assertThat(resultat.getDescription()).isEqualTo("Description");
        assertThat(resultat.getJoursCongesAnnuels()).isEqualTo(20);
        verify(entrepriseRepository).save(entreprise);
    }

    @Test
    @DisplayName("mettreAJourEntreprise - nom vide et jours <= 0 : ces deux valeurs ne changent pas")
    void mettreAJourEntreprise_valeursIgnorees() {
        when(entrepriseRepository.findById(1L)).thenReturn(Optional.of(entreprise));
        when(entrepriseRepository.save(entreprise)).thenReturn(entreprise);

        Entreprise resultat = service.mettreAJourEntreprise(1L, "   ", "Finance", null, 0);

        assertThat(resultat.getNom()).isEqualTo("Acme");
        assertThat(resultat.getJoursCongesAnnuels()).isEqualTo(30);
        assertThat(resultat.getSecteur()).isEqualTo("Finance");
    }

    @Test
    @DisplayName("mettreAJourEntreprise - introuvable : ResourceNotFoundException, rien sauvegardé")
    void mettreAJourEntreprise_introuvable() {
        when(entrepriseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.mettreAJourEntreprise(99L, "X", null, null, 10))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(entrepriseRepository, never()).save(any());
    }
}