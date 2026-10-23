package gestionconges.service;

import gestionconges.entity.DemandeConge;
import gestionconges.entity.Equipe;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.StatutConge;
import gestionconges.enums.TypeConge;
import gestionconges.repository.DemandeCongeRepository;
import gestionconges.repository.EquipeRepository;
import gestionconges.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
public class StatistiquesService {

    private final DemandeCongeRepository demandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EquipeRepository equipeRepository;

    public StatistiquesService(DemandeCongeRepository demandeRepository,
                               UtilisateurRepository utilisateurRepository,
                               EquipeRepository equipeRepository) {
        this.demandeRepository = demandeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.equipeRepository = equipeRepository;
    }

    public Map<String, Object> getStatsEntreprise(Long entrepriseId) {
        Map<String, Object> stats = new HashMap<>();

        long totalEmployes = utilisateurRepository.countByEntreprise_Id(entrepriseId);
        long totalEquipes = equipeRepository.countByEntreprise_Id(entrepriseId);
        long demandesEnAttente = demandeRepository.countByEntreprise_IdAndStatut(entrepriseId, StatutConge.EN_ATTENTE);
        long demandesApprouvees = demandeRepository.countByEntreprise_IdAndStatut(entrepriseId, StatutConge.APPROUVEE);

        List<DemandeConge> toutes = demandeRepository.findByEntreprise_IdOrderByDateCreationDesc(entrepriseId);
        LocalDate aujourdhui = LocalDate.now();

        long employesActuellementAbsents = toutes.stream()
                .filter(d -> d.getStatut() == StatutConge.APPROUVEE)
                .filter(d -> !aujourdhui.isBefore(d.getDateDebut()) && !aujourdhui.isAfter(d.getDateFin()))
                .map(d -> d.getUtilisateur().getId())
                .distinct()
                .count();

        // Répartition par type de congé
        Map<String, Integer> repartitionParType = new LinkedHashMap<>();
        for (TypeConge t : TypeConge.values()) {
            repartitionParType.put(t.getLibelle(), 0);
        }
        for (DemandeConge d : toutes) {
            if (d.getStatut() == StatutConge.APPROUVEE) {
                String libelle = d.getTypeConge().getLibelle();
                repartitionParType.put(libelle, repartitionParType.getOrDefault(libelle, 0) + d.getNombreJours());
            }
        }

        // Taux d'absence estimé
        double tauxAbsence = 0.0;
        if (totalEmployes > 0) {
            tauxAbsence = ((double) employesActuellementAbsents / totalEmployes) * 100.0;
        }

        stats.put("totalEmployes", totalEmployes);
        stats.put("totalEquipes", totalEquipes);
        stats.put("demandesEnAttente", demandesEnAttente);
        stats.put("demandesApprouvees", demandesApprouvees);
        stats.put("employesActuellementAbsents", employesActuellementAbsents);
        stats.put("tauxAbsence", String.format(Locale.US, "%.1f", tauxAbsence));
        stats.put("repartitionParType", repartitionParType);

        return stats;
    }

    public Map<String, Object> getStatsEquipe(Long equipeId) {
        Map<String, Object> stats = new HashMap<>();

        long totalMembres = utilisateurRepository.countByEquipe_Id(equipeId);
        long demandesEnAttente = demandeRepository.countByUtilisateur_Equipe_IdAndStatut(equipeId, StatutConge.EN_ATTENTE);
        long demandesApprouvees = demandeRepository.countByUtilisateur_Equipe_IdAndStatut(equipeId, StatutConge.APPROUVEE);

        List<DemandeConge> demandes = demandeRepository.findByUtilisateur_Equipe_IdOrderByDateCreationDesc(equipeId);
        LocalDate aujourdhui = LocalDate.now();

        long membresActuellementAbsents = demandes.stream()
                .filter(d -> d.getStatut() == StatutConge.APPROUVEE)
                .filter(d -> !aujourdhui.isBefore(d.getDateDebut()) && !aujourdhui.isAfter(d.getDateFin()))
                .map(d -> d.getUtilisateur().getId())
                .distinct()
                .count();

        stats.put("totalMembres", totalMembres);
        stats.put("demandesEnAttente", demandesEnAttente);
        stats.put("demandesApprouvees", demandesApprouvees);
        stats.put("membresActuellementAbsents", membresActuellementAbsents);

        return stats;
    }

    public byte[] exporterDemandesCsv(Long entrepriseId) {
        List<DemandeConge> demandes = demandeRepository.findByEntreprise_IdOrderByDateCreationDesc(entrepriseId);

        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM pour ouverture propre dans Excel
        sb.append('\ufeff');
        sb.append("ID;Employé;Email;Équipe;Type de congé;Date début;Date fin;Jours ouvrables;Statut;Motif;Valideur;Date décision;Commentaire\n");

        for (DemandeConge d : demandes) {
            String nomEmploye = d.getUtilisateur() != null ? d.getUtilisateur().getNomComplet() : "";
            String email = d.getUtilisateur() != null ? d.getUtilisateur().getEmail() : "";
            String nomEquipe = (d.getUtilisateur() != null && d.getUtilisateur().getEquipe() != null)
                    ? d.getUtilisateur().getEquipe().getNom() : "Sans équipe";
            String type = d.getTypeConge() != null ? d.getTypeConge().getLibelle() : "";
            String statut = d.getStatut() != null ? d.getStatut().getLibelle() : "";
            String motif = d.getMotif() != null ? d.getMotif().replace(";", " ").replace("\n", " ") : "";
            String valideur = d.getValideur() != null ? d.getValideur().getNomComplet() : "";
            String dateDecision = d.getDateDecision() != null ? d.getDateDecision().toLocalDate().toString() : "";
            String commentaire = d.getCommentaireDecision() != null ? d.getCommentaireDecision().replace(";", " ").replace("\n", " ") : "";

            sb.append(d.getId()).append(";")
                    .append(nomEmploye).append(";")
                    .append(email).append(";")
                    .append(nomEquipe).append(";")
                    .append(type).append(";")
                    .append(d.getDateDebut()).append(";")
                    .append(d.getDateFin()).append(";")
                    .append(d.getNombreJours()).append(";")
                    .append(statut).append(";")
                    .append(motif).append(";")
                    .append(valideur).append(";")
                    .append(dateDecision).append(";")
                    .append(commentaire).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
