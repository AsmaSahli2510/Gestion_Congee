package gestionconges.service.impl;

import gestionconges.service.CustomUserDetailsService;

import gestionconges.entity.Utilisateur;
import gestionconges.enums.StatutUtilisateur;
import gestionconges.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsServiceImpl implements CustomUserDetailsService {

  @Autowired  private  UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional(readOnly = true)
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