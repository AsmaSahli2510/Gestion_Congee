package gestionconges.api;

import gestionconges.dto.EquipeRequest;
import gestionconges.entity.Equipe;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.EquipeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/equipes")
@CrossOrigin(origins = "*")
public class EquipeRestController {

   @Autowired private EquipeService equipeService;
    @Autowired private UtilisateurRepository utilisateurRepository;

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
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> creerEquipe(@Valid @RequestBody EquipeRequest request, Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getRole() != Role.RH || user.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Seul le RH peut créer une équipe"));
        }

        Equipe cree = equipeService.creerEquipe(
                user.getEntreprise().getId(),
                request.getNom(),
                request.getDescription(),
                request.getManagerId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(cree);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modifierEquipe(
            @PathVariable("id") Long id,
            @Valid @RequestBody EquipeRequest request,
            Principal principal) {

        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getRole() != Role.RH || user.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Seul le RH peut modifier une équipe"));
        }

        Equipe modifiee = equipeService.modifierEquipe(
                id,
                user.getEntreprise().getId(),
                request.getNom(),
                request.getDescription(),
                request.getManagerId(),
                Boolean.TRUE.equals(request.getActive())
        );
        return ResponseEntity.ok(modifiee);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerEquipe(@PathVariable("id") Long id, Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getRole() != Role.RH || user.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Seul le RH peut supprimer une équipe"));
        }

        equipeService.supprimerEquipe(id, user.getEntreprise().getId());
        return ResponseEntity.ok(Map.of("success", true, "message", "Équipe supprimée avec succès"));
    }
}
