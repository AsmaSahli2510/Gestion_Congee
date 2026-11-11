package gestionconges.service;
import org.springframework.security.core.userdetails.UserDetailsService;

/** Service d'authentification : charge un utilisateur à partir de son e-mail (contrat Spring Security). */
public interface CustomUserDetailsService extends UserDetailsService {
}