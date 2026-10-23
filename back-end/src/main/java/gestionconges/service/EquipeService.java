package gestionconges.service;

import gestionconges.entity.Entreprise;
import gestionconges.entity.Equipe;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.repository.EntrepriseRepository;
import gestionconges.repository.EquipeRepository;
import gestionconges.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EquipeService {

    private final EquipeRepository equipeRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final UtilisateurRepository utilisateurRepository;

    public EquipeService(EquipeRepository equipeRepository,
                         EntrepriseRepository entrepriseRepository,
                         UtilisateurRepository utilisateurRepository) {
        this.equipeRepository = equipeRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public List<Equipe> getEquipes(Long entrepriseId) {
        return equipeRepository.findByEntreprise_Id(entrepriseId);
    }

    public List<Equipe> getEquipesActives(Long entrepriseId) {
        return equipeRepository.findByEntreprise_IdAndActiveTrue(entrepriseId);
    }

    public Equipe trouverParIdEtEntreprise(Long equipeId, Long entrepriseId) {
        return equipeRepository.findByIdAndEntreprise_Id(equipeId, entrepriseId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable ou non autorisée"));
    }

    @Transactional
    public Equipe creerEquipe(Long entrepriseId, String nom, String description, Long managerId) {
        Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise introuvable : " + entrepriseId));

        Equipe equipe = new Equipe(nom.trim(), entreprise);
        equipe.setDescription(description);

        if (managerId != null) {
            Utilisateur manager = utilisateurRepository.findByIdAndEntreprise_Id(managerId, entrepriseId)
                    .orElse(null);
            equipe.setManager(manager);
        }

        equipe = equipeRepository.save(equipe);

        if (equipe.getManager() != null) {
            equipe.getManager().setEquipe(equipe);
            utilisateurRepository.save(equipe.getManager());
        }

        return equipe;
    }

    @Transactional
    public Equipe modifierEquipe(Long equipeId, Long entrepriseId, String nom, String description, Long managerId, boolean active) {
        Equipe equipe = trouverParIdEtEntreprise(equipeId, entrepriseId);
        equipe.setNom(nom.trim());
        equipe.setDescription(description);
        equipe.setActive(active);

        if (managerId != null) {
            Utilisateur manager = utilisateurRepository.findByIdAndEntreprise_Id(managerId, entrepriseId)
                    .orElse(null);
            equipe.setManager(manager);
            if (manager != null) {
                manager.setEquipe(equipe);
                // Si le manager n'a pas encore le rôle MANAGER ou RH, lui attribuer le rôle MANAGER
                if (manager.getRole() == Role.EMPLOYE) {
                    manager.setRole(Role.MANAGER);
                }
                utilisateurRepository.save(manager);
            }
        } else {
            equipe.setManager(null);
        }

        return equipeRepository.save(equipe);
    }

    @Transactional
    public void supprimerEquipe(Long equipeId, Long entrepriseId) {
        Equipe equipe = trouverParIdEtEntreprise(equipeId, entrepriseId);
        List<Utilisateur> membres = utilisateurRepository.findByEquipe_Id(equipeId);
        for (Utilisateur m : membres) {
            m.setEquipe(null);
        }
        utilisateurRepository.saveAll(membres);
        equipeRepository.delete(equipe);
    }

    @Transactional
    public void assignerMembre(Long utilisateurId, Long equipeId, Long entrepriseId) {
        Utilisateur utilisateur = utilisateurRepository.findByIdAndEntreprise_Id(utilisateurId, entrepriseId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        if (equipeId != null && equipeId > 0) {
            Equipe equipe = trouverParIdEtEntreprise(equipeId, entrepriseId);
            utilisateur.setEquipe(equipe);
        } else {
            utilisateur.setEquipe(null);
        }

        utilisateurRepository.save(utilisateur);
    }

    public List<Utilisateur> getMembresEquipe(Long equipeId) {
        return utilisateurRepository.findByEquipe_Id(equipeId);
    }
}
