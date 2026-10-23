package gestionconges.api;

import gestionconges.entity.Invitation;
import gestionconges.entity.SoldeConge;
import gestionconges.entity.Utilisateur;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.EntrepriseService;
import gestionconges.service.InvitationService;
import gestionconges.service.SoldeCongeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseService entrepriseService;
    private final InvitationService invitationService;
    private final SoldeCongeService soldeCongeService;

    public AuthRestController(AuthenticationManager authenticationManager,
                              UtilisateurRepository utilisateurRepository,
                              EntrepriseService entrepriseService,
                              InvitationService invitationService,
                              SoldeCongeService soldeCongeService) {
        this.authenticationManager = authenticationManager;
        this.utilisateurRepository = utilisateurRepository;
        this.entrepriseService = entrepriseService;
        this.invitationService = invitationService;
        this.soldeCongeService = soldeCongeService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpServletRequest request) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email et mot de passe requis"));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email.trim().toLowerCase(), password)
            );

            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authentication);

            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            Utilisateur utilisateur = utilisateurRepository.findByEmail(email.trim().toLowerCase()).orElseThrow();
            int annee = Year.now().getValue();
            SoldeConge solde = soldeCongeService.getOuCreerSolde(utilisateur.getId(), annee);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", utilisateur);
            response.put("role", utilisateur.getRole().name().toLowerCase());
            response.put("balance", solde.getJoursRestants());
            response.put("message", "Connexion réussie");

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Email ou mot de passe incorrect."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Erreur lors de l'authentification : " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("authenticated", false, "message", "Non authentifié"));
        }

        Utilisateur utilisateur = utilisateurRepository.findByEmail(principal.getName()).orElse(null);
        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("authenticated", false, "message", "Utilisateur introuvable"));
        }

        int annee = Year.now().getValue();
        SoldeConge solde = soldeCongeService.getOuCreerSolde(utilisateur.getId(), annee);

        Map<String, Object> data = new HashMap<>();
        data.put("authenticated", true);
        data.put("user", utilisateur);
        data.put("role", utilisateur.getRole().name().toLowerCase());
        data.put("balance", solde.getJoursRestants());
        if (utilisateur.getEntreprise() != null) {
            data.put("company", utilisateur.getEntreprise());
        }

        return ResponseEntity.ok(data);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Déconnexion réussie"));
    }

    @PostMapping({"/register", "/creer-entreprise"})
    public ResponseEntity<?> creerEntreprise(@RequestBody Map<String, String> body) {
        String nomEntreprise = body.get("nomEntreprise");
        String nomRh = body.get("nomRh");
        String prenomRh = body.get("prenomRh");
        String emailRh = body.get("emailRh");
        String motDePasse = body.get("motDePasse");

        if (nomEntreprise == null || nomEntreprise.isBlank()
                || nomRh == null || nomRh.isBlank()
                || prenomRh == null || prenomRh.isBlank()
                || emailRh == null || emailRh.isBlank()
                || motDePasse == null || motDePasse.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Tous les champs sont obligatoires."));
        }

        if (motDePasse.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Le mot de passe doit comporter au moins 6 caractères."));
        }

        try {
            Utilisateur rh = entrepriseService.creerNouvelleEntrepriseEtRh(nomEntreprise, nomRh, prenomRh, emailRh, motDePasse);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Votre espace entreprise a été créé avec succès !",
                    "user", rh,
                    "role", "rh"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/invitation")
    public ResponseEntity<?> verifierInvitation(@RequestParam String token) {
        try {
            Invitation inv = invitationService.trouverParToken(token);
            Map<String, Object> res = new HashMap<>();
            res.put("valid", true);
            res.put("email", inv.getEmail());
            res.put("role", inv.getRole().name().toLowerCase());
            res.put("poste", inv.getPoste());
            res.put("team", inv.getTeam());
            res.put("entrepriseNom", inv.getEntreprise() != null ? inv.getEntreprise().getNom() : "");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/activer-invitation")
    public ResponseEntity<?> activerInvitation(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String nom = body.get("nom");
        String prenom = body.get("prenom");
        String motDePasse = body.get("motDePasse");

        if (token == null || nom == null || prenom == null || motDePasse == null
                || token.isBlank() || nom.isBlank() || prenom.isBlank() || motDePasse.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Tous les champs sont obligatoires."));
        }

        if (motDePasse.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Le mot de passe doit comporter au moins 6 caractères."));
        }

        try {
            Utilisateur user = invitationService.activerCompte(token, nom, prenom, motDePasse);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Compte activé avec succès ! Vous pouvez vous connecter.",
                    "user", user,
                    "role", user.getRole().name().toLowerCase()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/mot-de-passe-oublie")
    public ResponseEntity<?> motDePasseOublie(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Si un compte est associé à l'adresse " + (email != null ? email : "") + ", un lien de réinitialisation sécurisé vous a été envoyé."
        ));
    }
}
