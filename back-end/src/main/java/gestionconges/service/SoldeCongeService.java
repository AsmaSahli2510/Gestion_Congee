package gestionconges.service;

import gestionconges.entity.Entreprise;
import gestionconges.entity.MouvementSolde;
import gestionconges.entity.SoldeConge;
import gestionconges.entity.Utilisateur;
import gestionconges.exception.ResourceNotFoundException;
import gestionconges.repository.EntrepriseRepository;
import gestionconges.repository.MouvementSoldeRepository;
import gestionconges.repository.SoldeCongeRepository;
import gestionconges.repository.UtilisateurRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.Optional;

@Service
public class SoldeCongeService {

    private static final Logger log = LoggerFactory.getLogger(SoldeCongeService.class);

    @Value("${leaveflow.conges.jours-par-defaut:30}")
    private int defaultJoursConges;

    private final SoldeCongeRepository soldeCongeRepository;
    private final MouvementSoldeRepository mouvementSoldeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final NotificationService notificationService;

    @Autowired
    public SoldeCongeService(
            @Autowired SoldeCongeRepository soldeCongeRepository,
            @Autowired MouvementSoldeRepository mouvementSoldeRepository,
            @Autowired UtilisateurRepository utilisateurRepository,
            @Autowired EntrepriseRepository entrepriseRepository,
            @Autowired NotificationService notificationService) {
        this.soldeCongeRepository = soldeCongeRepository;
        this.mouvementSoldeRepository = mouvementSoldeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.notificationService = notificationService;
    }

    @PostConstruct
    public void init() {
        log.info("SoldeCongeService initialisé avec un quota par défaut de [{}] jours", defaultJoursConges);
    }

    @PreDestroy
    public void cleanup() {
        log.info("Arrêt de SoldeCongeService");
    }

    @Transactional
    public SoldeConge getOuCreerSolde(Long utilisateurId, Integer annee) {
        if (annee == null) {
            annee = Year.now().getValue();
        }

        Optional<SoldeConge> existant = soldeCongeRepository.findByUtilisateur_IdAndAnnee(utilisateurId, annee);
        if (existant.isPresent()) {
            return existant.get();
        }

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", utilisateurId));

        Entreprise entreprise = utilisateur.getEntreprise();
        int joursParDefaut = entreprise != null ? entreprise.getJoursCongesAnnuels() : defaultJoursConges;

        SoldeConge nouveauSolde = new SoldeConge(entreprise, utilisateur, annee, joursParDefaut, 0);
        nouveauSolde = soldeCongeRepository.save(nouveauSolde);

        if (entreprise != null) {
            MouvementSolde mouvement = new MouvementSolde(
                    entreprise,
                    nouveauSolde,
                    utilisateur,
                    joursParDefaut,
                    "Attribution annuelle automatique pour l'année " + annee,
                    utilisateur
            );
            mouvementSoldeRepository.save(mouvement);
        }

        return nouveauSolde;
    }

    @Transactional(readOnly = true)
    public List<SoldeConge> getSoldesEntreprise(Long entrepriseId, Integer annee) {
        if (annee == null) {
            annee = Year.now().getValue();
        }
        return soldeCongeRepository.findByEntreprise_IdAndAnnee(entrepriseId, annee);
    }

    @Transactional(readOnly = true)
    public List<SoldeConge> getSoldesEquipe(Long equipeId, Integer annee) {
        if (annee == null) {
            annee = Year.now().getValue();
        }
        return soldeCongeRepository.findByUtilisateur_Equipe_IdAndAnnee(equipeId, annee);
    }

    @Transactional(readOnly = true)
    public List<MouvementSolde> getHistoriqueUtilisateur(Long utilisateurId) {
        return mouvementSoldeRepository.findByUtilisateur_IdOrderByDateMouvementDesc(utilisateurId);
    }

    @Transactional(readOnly = true)
    public List<MouvementSolde> getHistoriqueEntreprise(Long entrepriseId) {
        return mouvementSoldeRepository.findByEntreprise_IdOrderByDateMouvementDesc(entrepriseId);
    }

    @Transactional
    public SoldeConge ajusterSolde(Long soldeId, Long entrepriseId, Integer variation, String raison, Utilisateur auteur) {
        SoldeConge solde = soldeCongeRepository.findByIdAndEntreprise_Id(soldeId, entrepriseId)
                .orElseThrow(() -> new IllegalArgumentException("Solde introuvable ou non autorisé"));

        int ancienAcquis = solde.getJoursAcquis();
        int nouveauAcquis = Math.max(0, ancienAcquis + variation);
        solde.setJoursAcquis(nouveauAcquis);
        solde = soldeCongeRepository.save(solde);

        MouvementSolde mouvement = new MouvementSolde(
                solde.getEntreprise(),
                solde,
                solde.getUtilisateur(),
                variation,
                (raison != null && !raison.isBlank()) ? raison.trim() : "Ajustement exceptionnel par RH",
                auteur
        );
        mouvementSoldeRepository.save(mouvement);

        // Notification envoyée au collaborateur
        notificationService.creerNotification(
                solde.getEntreprise(),
                solde.getUtilisateur(),
                "Ajustement de solde de congé",
                String.format("Votre solde pour l'année %d a été ajusté de %+d jours par %s. Raison : %s",
                        solde.getAnnee(), variation, auteur.getNomComplet(), raison),
                "/employe/solde"
        );

        return solde;
    }
}
