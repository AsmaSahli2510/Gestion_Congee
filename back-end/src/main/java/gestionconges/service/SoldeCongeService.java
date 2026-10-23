package gestionconges.service;

import gestionconges.entity.Entreprise;
import gestionconges.entity.MouvementSolde;
import gestionconges.entity.SoldeConge;
import gestionconges.entity.Utilisateur;
import gestionconges.repository.EntrepriseRepository;
import gestionconges.repository.MouvementSoldeRepository;
import gestionconges.repository.SoldeCongeRepository;
import gestionconges.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.Optional;

@Service
public class SoldeCongeService {

    private final SoldeCongeRepository soldeCongeRepository;
    private final MouvementSoldeRepository mouvementSoldeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final NotificationService notificationService;

    public SoldeCongeService(SoldeCongeRepository soldeCongeRepository,
                             MouvementSoldeRepository mouvementSoldeRepository,
                             UtilisateurRepository utilisateurRepository,
                             EntrepriseRepository entrepriseRepository,
                             NotificationService notificationService) {
        this.soldeCongeRepository = soldeCongeRepository;
        this.mouvementSoldeRepository = mouvementSoldeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.notificationService = notificationService;
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
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + utilisateurId));

        Entreprise entreprise = utilisateur.getEntreprise();
        int joursParDefaut = entreprise != null ? entreprise.getJoursCongesAnnuels() : 30;

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

    public List<SoldeConge> getSoldesEntreprise(Long entrepriseId, Integer annee) {
        if (annee == null) {
            annee = Year.now().getValue();
        }
        return soldeCongeRepository.findByEntreprise_IdAndAnnee(entrepriseId, annee);
    }

    public List<SoldeConge> getSoldesEquipe(Long equipeId, Integer annee) {
        if (annee == null) {
            annee = Year.now().getValue();
        }
        return soldeCongeRepository.findByUtilisateur_Equipe_IdAndAnnee(equipeId, annee);
    }

    public List<MouvementSolde> getHistoriqueUtilisateur(Long utilisateurId) {
        return mouvementSoldeRepository.findByUtilisateur_IdOrderByDateMouvementDesc(utilisateurId);
    }

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
