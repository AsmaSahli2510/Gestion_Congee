package gestionconges.controller;

import gestionconges.entity.Utilisateur;
import gestionconges.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@CrossOrigin(origins = "*")
public class WebViewController {

    private final UtilisateurRepository utilisateurRepository;

    @Autowired
    public WebViewController(@Autowired UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @GetMapping("/")
    public String index(Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        Utilisateur user = utilisateurRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        return switch (user.getRole()) {
            case RH -> "redirect:/rh/demandes";
            case MANAGER -> "redirect:/manager/dashboard";
            case EMPLOYE -> "redirect:/employe/demandes";
        };
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {
        if (error != null) {
            model.addAttribute("error", "Email ou mot de passe incorrect.");
        }
        if (logout != null) {
            model.addAttribute("message", "Déconnexion réussie.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        return "register";
    }

    @GetMapping("/employe/demandes")
    public String employeDemandes(Model model, Principal principal) {
        return "employe/demandes";
    }

    @GetMapping("/employe/nouvelle-demande")
    public String nouvelleDemande(Model model, Principal principal) {
        return "employe/nouvelle-demande";
    }

    @GetMapping("/manager/dashboard")
    public String managerDashboard(Model model, Principal principal) {
        return "manager/dashboard";
    }

    @GetMapping("/manager/demandes")
    public String managerDemandes(Model model, Principal principal) {
        return "manager/demandes";
    }

    @GetMapping("/rh/demandes")
    public String rhDemandes(Model model, Principal principal) {
        return "rh/demandes";
    }
}
