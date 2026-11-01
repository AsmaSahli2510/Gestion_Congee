package gestionconges.api;

import gestionconges.entity.Entreprise;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.EntrepriseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/entreprise")
@CrossOrigin(origins = "*")
public class EntrepriseRestController {

    @Autowired private EntrepriseService entrepriseService;
    @Autowired private UtilisateurRepository utilisateurRepository;

    private Utilisateur getUtilisateurConnecte(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return utilisateurRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur connecté introuvable"));
    }

    @GetMapping
    public ResponseEntity<?> getEntreprise(Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Aucune entreprise rattachée"));
        }
        return ResponseEntity.ok(user.getEntreprise());
    }

    @PutMapping
    public ResponseEntity<?> modifierEntreprise(@RequestBody Map<String, Object> body, Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getRole() != Role.RH || user.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Seul l'administrateur RH peut modifier les paramètres de l'entreprise"));
        }

        String nom = (String) (body.get("nom") != null ? body.get("nom") : body.get("name"));
        String secteur = (String) body.get("secteur");
        String description = (String) body.get("description");

        Integer jours = null;
        if (body.get("joursCongesAnnuels") != null) {
            jours = Integer.valueOf(body.get("joursCongesAnnuels").toString());
        } else if (body.get("acquisition") != null) {
            try {
                double acq = Double.parseDouble(body.get("acquisition").toString());
                jours = (int) Math.round(acq * 12);
            } catch (Exception ignored) {}
        }

        Entreprise miseAJour = entrepriseService.mettreAJourEntreprise(user.getEntreprise().getId(), nom, secteur, description, jours);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Informations de l'entreprise mises à jour",
                "entreprise", miseAJour
        ));
    }
}
