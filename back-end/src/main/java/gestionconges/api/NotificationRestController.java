package gestionconges.api;

import gestionconges.entity.Notification;
import gestionconges.entity.Utilisateur;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationRestController {

    @Autowired private NotificationService notificationService;
    @Autowired private UtilisateurRepository utilisateurRepository;
    
    private Utilisateur getUtilisateurConnecte(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return utilisateurRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur connecté introuvable"));
    }

    @GetMapping
    public ResponseEntity<?> getNotifications(Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        List<Notification> list = notificationService.getNotifications(user.getId());
        long unreadCount = notificationService.getNombreNonLues(user.getId());

        Map<String, Object> res = new HashMap<>();
        res.put("notifications", list);
        res.put("nonLues", unreadCount);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/lire")
    public ResponseEntity<?> marquerCommeLue(@PathVariable("id") Long id, Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        notificationService.marquerCommeLue(id, user.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/toutes-lues")
    public ResponseEntity<?> marquerToutesCommeLues(Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        notificationService.marquerToutesCommeLues(user.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }
}
