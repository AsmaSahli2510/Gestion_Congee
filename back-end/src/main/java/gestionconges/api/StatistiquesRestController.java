package gestionconges.api;

import gestionconges.entity.Equipe;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.repository.EquipeRepository;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.StatistiquesService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/statistiques")
public class StatistiquesRestController {

    private final StatistiquesService statistiquesService;
    private final UtilisateurRepository utilisateurRepository;
    private final EquipeRepository equipeRepository;

    public StatistiquesRestController(StatistiquesService statistiquesService,
                                      UtilisateurRepository utilisateurRepository,
                                      EquipeRepository equipeRepository) {
        this.statistiquesService = statistiquesService;
        this.utilisateurRepository = utilisateurRepository;
        this.equipeRepository = equipeRepository;
    }

    private Utilisateur getUtilisateurConnecte(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return utilisateurRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur connecté introuvable"));
    }

    @GetMapping
    public ResponseEntity<?> getStatistiques(Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);

        if (user.getRole() == Role.RH && user.getEntreprise() != null) {
            Map<String, Object> stats = statistiquesService.getStatsEntreprise(user.getEntreprise().getId());
            return ResponseEntity.ok(stats);
        } else if (user.getRole() == Role.MANAGER) {
            Equipe equipe = user.getEquipe() != null ? user.getEquipe() : equipeRepository.findByManager_Id(user.getId()).orElse(null);
            if (equipe != null) {
                Map<String, Object> stats = statistiquesService.getStatsEquipe(equipe.getId());
                return ResponseEntity.ok(stats);
            }
        }

        return ResponseEntity.ok(Map.of());
    }

    // Export CSV conforme RG08
    @GetMapping({"/export-csv", "/export/demandes.csv"})
    public ResponseEntity<byte[]> exporterCsv(Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getRole() != Role.RH || user.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        byte[] csv = statistiquesService.exporterDemandesCsv(user.getEntreprise().getId());
        String filename = "demandes_conges_" + user.getEntreprise().getIdentifiantUnique() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
