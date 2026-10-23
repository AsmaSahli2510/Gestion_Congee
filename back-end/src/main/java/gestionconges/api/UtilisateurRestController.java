package gestionconges.api;

import gestionconges.entity.Equipe;
import gestionconges.entity.SoldeConge;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.enums.StatutUtilisateur;
import gestionconges.repository.EquipeRepository;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.EquipeService;
import gestionconges.service.SoldeCongeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Year;
import java.util.*;

@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurRestController {

    private final UtilisateurRepository utilisateurRepository;
    private final EquipeRepository equipeRepository;
    private final EquipeService equipeService;
    private final SoldeCongeService soldeCongeService;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurRestController(UtilisateurRepository utilisateurRepository,
                                     EquipeRepository equipeRepository,
                                     EquipeService equipeService,
                                     SoldeCongeService soldeCongeService,
                                     PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.equipeRepository = equipeRepository;
        this.equipeService = equipeService;
        this.soldeCongeService = soldeCongeService;
        this.passwordEncoder = passwordEncoder;
    }

    private Utilisateur getUtilisateurConnecte(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return utilisateurRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur connecté introuvable"));
    }

    // Liste des utilisateurs avec leurs soldes selon le périmètre (RH: entreprise, Manager: équipe)
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getUtilisateurs(Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        if (user.getEntreprise() == null) {
            return ResponseEntity.ok(List.of());
        }

        List<Utilisateur> list;
        if (user.getRole() == Role.RH) {
            list = utilisateurRepository.findByEntreprise_Id(user.getEntreprise().getId());
        } else if (user.getRole() == Role.MANAGER) {
            Equipe equipe = user.getEquipe() != null ? user.getEquipe() : equipeRepository.findByManager_Id(user.getId()).orElse(null);
            if (equipe != null) {
                list = utilisateurRepository.findByEquipe_Id(equipe.getId());
            } else {
                list = List.of(user);
            }
        } else {
            list = user.getEquipe() != null ? utilisateurRepository.findByEquipe_Id(user.getEquipe().getId()) : List.of(user);
        }

        int annee = Year.now().getValue();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Utilisateur u : list) {
            SoldeConge s = soldeCongeService.getOuCreerSolde(u.getId(), annee);
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getNomComplet().trim());
            map.put("nom", u.getNom());
            map.put("prenom", u.getPrenom());
            map.put("email", u.getEmail());
            map.put("role", u.getRole().name().toLowerCase());
            map.put("team", u.getEquipe() != null ? u.getEquipe().getNom() : "Sans équipe");
            map.put("equipeId", u.getEquipe() != null ? u.getEquipe().getId() : null);
            map.put("poste", u.getPoste() != null ? u.getPoste() : "");
            map.put("telephone", u.getTelephone() != null ? u.getTelephone() : "");
            map.put("balance", s.getJoursRestants());
            map.put("active", u.getStatut() == StatutUtilisateur.ACTIF);
            map.put("statut", u.getStatut().name());
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    // Profil personnel
    @GetMapping("/profil")
    public ResponseEntity<?> getMonProfil(Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);
        int annee = Year.now().getValue();
        SoldeConge s = soldeCongeService.getOuCreerSolde(user.getId(), annee);

        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getNomComplet().trim());
        map.put("nom", user.getNom());
        map.put("prenom", user.getPrenom());
        map.put("email", user.getEmail());
        map.put("role", user.getRole().name().toLowerCase());
        map.put("team", user.getEquipe() != null ? user.getEquipe().getNom() : "Sans équipe");
        map.put("poste", user.getPoste() != null ? user.getPoste() : "");
        map.put("telephone", user.getTelephone() != null ? user.getTelephone() : "");
        map.put("balance", s.getJoursRestants());

        return ResponseEntity.ok(map);
    }

    // Modifier son profil
    @PutMapping("/profil")
    public ResponseEntity<?> modifierProfil(@RequestBody Map<String, String> body, Principal principal) {
        Utilisateur user = getUtilisateurConnecte(principal);

        String nom = body.get("nom");
        String prenom = body.get("prenom");
        String telephone = body.get("telephone");
        String ancienMotDePasse = body.get("ancienMotDePasse");
        String nouveauMotDePasse = body.get("nouveauMotDePasse");

        if (nom != null && !nom.isBlank()) user.setNom(nom.trim());
        if (prenom != null && !prenom.isBlank()) user.setPrenom(prenom.trim());
        if (telephone != null) user.setTelephone(telephone.trim());

        if (nouveauMotDePasse != null && !nouveauMotDePasse.isBlank()) {
            if (ancienMotDePasse == null || !passwordEncoder.matches(ancienMotDePasse, user.getMotDePasse())) {
                return ResponseEntity.badRequest().body(Map.of("message", "L'ancien mot de passe est incorrect."));
            }
            if (nouveauMotDePasse.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("message", "Le nouveau mot de passe doit comporter au moins 6 caractères."));
            }
            user.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        }

        utilisateurRepository.save(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "Profil mis à jour avec succès", "user", user));
    }

    // Activer / désactiver un collaborateur (RH)
    @PostMapping("/{id}/statut")
    public ResponseEntity<?> basculerStatut(@PathVariable Long id, Principal principal) {
        Utilisateur rh = getUtilisateurConnecte(principal);
        if (rh.getRole() != Role.RH || rh.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Accès réservé au RH"));
        }

        if (rh.getId().equals(id)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Impossible de désactiver son propre compte administrateur RH"));
        }

        Utilisateur cible = utilisateurRepository.findByIdAndEntreprise_Id(id, rh.getEntreprise().getId()).orElse(null);
        if (cible == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Utilisateur introuvable"));
        }

        boolean active = cible.getStatut() == StatutUtilisateur.ACTIF;
        cible.setStatut(active ? StatutUtilisateur.INACTIF : StatutUtilisateur.ACTIF);
        utilisateurRepository.save(cible);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "active", !active,
                "message", "Statut du compte mis à jour"
        ));
    }

    // Affecter / changer d'équipe (RH)
    @PostMapping("/{id}/affecter")
    public ResponseEntity<?> affecterEquipe(@PathVariable Long id, @RequestBody Map<String, Object> body, Principal principal) {
        Utilisateur rh = getUtilisateurConnecte(principal);
        if (rh.getRole() != Role.RH || rh.getEntreprise() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Accès réservé au RH"));
        }

        Long equipeId = null;
        if (body.get("equipeId") != null) {
            equipeId = Long.valueOf(body.get("equipeId").toString());
        }

        try {
            equipeService.assignerMembre(id, equipeId, rh.getEntreprise().getId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Affectation mise à jour avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
