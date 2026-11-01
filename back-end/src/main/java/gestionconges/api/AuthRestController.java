package gestionconges.api;

import gestionconges.dto.ActivationCompteRequest;
import gestionconges.dto.LoginRequest;
import gestionconges.dto.RegisterRequest;
import gestionconges.entity.Invitation;
import gestionconges.entity.SoldeConge;
import gestionconges.entity.Utilisateur;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.EntrepriseService;
import gestionconges.service.InvitationService;
import gestionconges.service.SoldeCongeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
@CrossOrigin(origins = "*")
public class AuthRestController {

   @Autowired private AuthenticationManager authenticationManager;
   @Autowired private UtilisateurRepository utilisateurRepository;
   @Autowired private EntrepriseService entrepriseService;
   @Autowired private InvitationService invitationService;
   @Autowired private SoldeCongeService soldeCongeService;


    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest loginRequest,
            @CookieValue(value = "JSESSIONID", required = false) String sessionId,
            HttpServletRequest request) {

        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authentication);

            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            Utilisateur utilisateur = utilisateurRepository.findByEmail(email).orElseThrow();
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
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            Principal principal) {
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
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> creerEntreprise(@Valid @RequestBody RegisterRequest registerRequest) {
        Utilisateur rh = entrepriseService.creerNouvelleEntrepriseEtRh(
                registerRequest.getNomEntreprise(),
                registerRequest.getNomRh(),
                registerRequest.getPrenomRh(),
                registerRequest.getEmailRh(),
                registerRequest.getMotDePasse()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Votre espace entreprise a été créé avec succès !",
                "user", rh,
                "role", "rh"
        ));
    }

    @GetMapping("/invitation")
    public ResponseEntity<?> verifierInvitation(@RequestParam(name = "token") String token) {
        Invitation inv = invitationService.trouverParToken(token);
        Map<String, Object> res = new HashMap<>();
        res.put("valid", true);
        res.put("email", inv.getEmail());
        res.put("role", inv.getRole().name().toLowerCase());
        res.put("poste", inv.getPoste());
        res.put("team", inv.getTeam());
        res.put("entrepriseNom", inv.getEntreprise() != null ? inv.getEntreprise().getNom() : "");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/activer-invitation")
    public ResponseEntity<?> activerInvitation(@Valid @RequestBody ActivationCompteRequest request) {
        Utilisateur user = invitationService.activerCompte(
                request.getToken(),
                request.getNom(),
                request.getPrenom(),
                request.getMotDePasse()
        );
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Compte activé avec succès ! Vous pouvez vous connecter.",
                "user", user,
                "role", user.getRole().name().toLowerCase()
        ));
    }

    @PostMapping("/mot-de-passe-oublie")
    public ResponseEntity<?> motDePasseOublie(@RequestBody Map<String, String> body) {
        String email = body != null ? body.get("email") : "";
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Si un compte est associé à l'adresse " + (email != null ? email : "") + ", un lien de réinitialisation sécurisé vous a été envoyé."
        ));
    }
}
