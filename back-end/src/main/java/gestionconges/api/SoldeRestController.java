package gestionconges.api;

import gestionconges.entity.Equipe;
import gestionconges.entity.MouvementSolde;
import gestionconges.entity.SoldeConge;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.repository.EquipeRepository;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.SoldeCongeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/soldes")
public class SoldeRestController {

    private final SoldeCongeService soldeCongeService;
    private final UtilisateurRepository utilisateurRepository;
    private final EquipeRepository equipeRepository;

    public SoldeRestController(SoldeCongeService soldeCongeService,
                               UtilisateurRepository utilisateurRepository,
                               EquipeRepository equipeRepository) {
        this.soldeCongeService = soldeCongeService;
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

    // Solde personnel de l'utilisateur connecté
    @GetMapping({"", "/mon-solde"})
    public ResponseEntity<?> getMonSolde(Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        int annee = Year.now().getValue();
        SoldeConge solde = soldeCongeService.getOuCreerSolde(user.getId(), annee);
        List<MouvementSolde> mouvements = soldeCongeService.getHistoriqueUtilisateur(user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("solde", solde);
        data.put("balance", solde.getJoursRestants());
        data.put("joursAcquis", solde.getJoursAcquis());
        data.put("joursUtilises", solde.getJoursUtilises());
        data.put("joursRestants", solde.getJoursRestants());
        data.put("mouvements", mouvements);

        return ResponseEntity.ok(data);
    }

    // Historique des mouvements de l'utilisateur
    @GetMapping("/mouvements")
    public ResponseEntity<List<MouvementSolde>> getMesMouvements(Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        return ResponseEntity.ok(soldeCongeService.getHistoriqueUtilisateur(user.getId()));
    }

    // Soldes des membres de l'équipe (Manager / RH)
    @GetMapping("/equipe")
    public ResponseEntity<?> getSoldesEquipe(Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        int annee = Year.now().getValue();

        Equipe equipe = user.getEquipe();
        if (equipe == null && user.getRole() == Role.MANAGER) {
            equipe = equipeRepository.findByManager_Id(user.getId()).orElse(null);
        }

        if (equipe == null) {
            return ResponseEntity.ok(List.of());
        }

        List<SoldeConge> soldes = soldeCongeService.getSoldesEquipe(equipe.getId(), annee);
        return ResponseEntity.ok(soldes);
    }

    // Soldes de toute la société (RH uniquement)
    @GetMapping("/entreprise")
    public ResponseEntity<?> getSoldesEntreprise(Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getRole() != Role.RH || user.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Accès réservé aux administrateurs RH"));
        }

        int annee = Year.now().getValue();
        List<SoldeConge> soldes = soldeCongeService.getSoldesEntreprise(user.getEntreprise().getId(), annee);
        List<MouvementSolde> mouvements = soldeCongeService.getHistoriqueEntreprise(user.getEntreprise().getId());

        Map<String, Object> data = new HashMap<>();
        data.put("soldes", soldes);
        data.put("mouvements", mouvements);

        return ResponseEntity.ok(data);
    }

    // Ajustement de solde par le RH avec traçabilité (M4)
    @PostMapping("/ajuster")
    public ResponseEntity<?> ajusterSolde(@RequestBody Map<String, Object> body, Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getRole() != Role.RH || user.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Accès réservé aux administrateurs RH"));
        }

        try {
            Long soldeId = null;
            if (body.get("soldeId") != null) {
                soldeId = Long.valueOf(body.get("soldeId").toString());
            } else if (body.get("userId") != null) {
                Long targetUserId = Long.valueOf(body.get("userId").toString());
                int annee = Year.now().getValue();
                SoldeConge sc = soldeCongeService.getOuCreerSolde(targetUserId, annee);
                soldeId = sc.getId();
            }

            if (soldeId == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "soldeId ou userId obligatoire"));
            }

            Object amountObj = body.get("variation") != null ? body.get("variation") : body.get("amount");
            if (amountObj == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "La variation de solde est obligatoire"));
            }
            int variation = (int) Math.round(Double.parseDouble(amountObj.toString()));

            String raison = (String) (body.get("raison") != null ? body.get("raison") : body.get("reason"));
            if (raison == null || raison.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le motif de l'ajustement est obligatoire"));
            }

            SoldeConge soldeAjuste = soldeCongeService.ajusterSolde(soldeId, user.getEntreprise().getId(), variation, raison, user);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Ajustement enregistré avec succès dans le journal des mouvements",
                    "solde", soldeAjuste
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
