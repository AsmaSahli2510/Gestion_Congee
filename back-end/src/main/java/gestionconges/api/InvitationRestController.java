package gestionconges.api;

import gestionconges.dto.InvitationRequest;
import gestionconges.entity.Invitation;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invitations")
@CrossOrigin(origins = "*")
public class InvitationRestController {

    @Autowired private InvitationService invitationService;
    @Autowired private UtilisateurRepository utilisateurRepository;

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
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> creerInvitation(@Valid @RequestBody InvitationRequest request, Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getRole() != Role.RH || user.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Accès réservé au RH"));
        }

        Invitation inv = invitationService.creerInvitation(
                user.getEntreprise().getId(),
                request.getEmail(),
                request.getRole(),
                request.getEquipeId(),
                request.getPoste()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "invitation", inv,
                "token", inv.getToken(),
                "link", "/invitations/activer?token=" + inv.getToken(),
                "message", "Invitation générée avec succès"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerInvitation(@PathVariable("id") Long id, Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getRole() != Role.RH || user.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Accès réservé au RH"));
        }

        invitationService.supprimerInvitation(id, user.getEntreprise().getId());
        return ResponseEntity.ok(Map.of("success", true, "message", "Invitation supprimée"));
    }
}
