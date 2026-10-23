package gestionconges.api;

import gestionconges.entity.Equipe;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.EquipeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/equipes")
public class EquipeRestController {

    private final EquipeService equipeService;
    private final UtilisateurRepository utilisateurRepository;

    public EquipeRestController(EquipeService equipeService, UtilisateurRepository utilisateurRepository) {
        this.equipeService = equipeService;
        this.utilisateurRepository = utilisateurRepository;
    }

    private Utilisateur getUtilisateurConnecte(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return utilisateurRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur connecté introuvable"));
    }

    @GetMapping
    public ResponseEntity<List<Equipe>> getEquipes(Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getEntreprise() == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(equipeService.getEquipes(user.getEntreprise().getId()));
    }

    @PostMapping
    public ResponseEntity<?> creerEquipe(@RequestBody Map<String, Object> body, Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getRole() != Role.RH || user.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Seul le RH peut créer une équipe"));
        }

        String nom = (String) body.get("nom");
        if (nom == null || nom.isBlank()) {
            nom = (String) body.get("name");
        }
        if (nom == null || nom.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le nom de l'équipe est obligatoire"));
        }

        String description = (String) body.get("description");
        Long managerId = null;
        if (body.get("managerId") != null) {
            managerId = Long.valueOf(body.get("managerId").toString());
        } else if (body.get("manager") != null) {
            try {
                managerId = Long.valueOf(body.get("manager").toString());
            } catch (Exception ignored) {}
        }

        try {
            Equipe cree = equipeService.creerEquipe(user.getEntreprise().getId(), nom, description, managerId);
            return ResponseEntity.status(HttpStatus.CREATED).body(cree);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modifierEquipe(@PathVariable Long id, @RequestBody Map<String, Object> body, Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getRole() != Role.RH || user.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Seul le RH peut modifier une équipe"));
        }

        String nom = (String) (body.get("nom") != null ? body.get("nom") : body.get("name"));
        if (nom == null || nom.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le nom de l'équipe est obligatoire"));
        }

        String description = (String) body.get("description");
        Long managerId = null;
        if (body.get("managerId") != null) {
            managerId = Long.valueOf(body.get("managerId").toString());
        }

        boolean active = body.get("active") != null ? Boolean.parseBoolean(body.get("active").toString()) : true;

        try {
            Equipe modifiee = equipeService.modifierEquipe(id, user.getEntreprise().getId(), nom, description, managerId, active);
            return ResponseEntity.ok(modifiee);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerEquipe(@PathVariable Long id, Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getRole() != Role.RH || user.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Seul le RH peut supprimer une équipe"));
        }

        try {
            equipeService.supprimerEquipe(id, user.getEntreprise().getId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Équipe supprimée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
