package gestionconges.service;

import gestionconges.entity.Utilisateur;
import gestionconges.enums.StatutUtilisateur;
import gestionconges.repository.UtilisateurRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    public CustomUserDetailsService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Aucun utilisateur trouvé avec cet email : " + email));

        boolean disabled = utilisateur.getStatut() == StatutUtilisateur.INACTIF;

        return User.withUsername(utilisateur.getEmail())
                .password(utilisateur.getMotDePasse())
                .roles(utilisateur.getRole().name())
                .disabled(disabled)
                .build();
    }
}