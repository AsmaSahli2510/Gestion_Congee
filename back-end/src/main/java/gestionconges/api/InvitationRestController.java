package gestionconges.api;

import gestionconges.entity.Invitation;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.InvitationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invitations")
public class InvitationRestController {

    private final InvitationService invitationService;
    private final UtilisateurRepository utilisateurRepository;

    public InvitationRestController(InvitationService invitationService, UtilisateurRepository utilisateurRepository) {
        this.invitationService = invitationService;
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
    public ResponseEntity<List<Invitation>> getInvitations(Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getRole() != Role.RH || user.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(invitationService.getInvitations(user.getEntreprise().getId()));
    }

    @PostMapping
    public ResponseEntity<?> creerInvitation(@RequestBody Map<String, Object> body, Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getRole() != Role.RH || user.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Accès réservé au RH"));
        }

        String email = (String) body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email obligatoire"));
        }

        String roleStr = (String) body.get("role");
        Role role = Role.EMPLOYE;
        if (roleStr != null && !roleStr.isBlank()) {
            try {
                role = Role.valueOf(roleStr.trim().toUpperCase());
            } catch (Exception ignored) {}
        }

        Long equipeId = null;
        if (body.get("equipeId") != null) {
            equipeId = Long.valueOf(body.get("equipeId").toString());
        }

        String poste = (String) body.get("poste");

        try {
            Invitation inv = invitationService.creerInvitation(user.getEntreprise().getId(), email, role, equipeId, poste);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "invitation", inv,
                    "token", inv.getToken(),
                    "link", "/invitations/activer?token=" + inv.getToken(),
                    "message", "Invitation générée avec succès"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerInvitation(@PathVariable Long id, Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getRole() != Role.RH || user.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Accès réservé au RH"));
        }

        invitationService.supprimerInvitation(id, user.getEntreprise().getId());
        return ResponseEntity.ok(Map.of("success", true, "message", "Invitation supprimée"));
    }
}
