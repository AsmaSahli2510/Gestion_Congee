package gestionconges.api;

import gestionconges.dto.DemandeCongeRequest;
import gestionconges.dto.RefusDemandeRequest;
import gestionconges.entity.DemandeConge;
import gestionconges.entity.Equipe;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.enums.TypeConge;
import gestionconges.repository.EquipeRepository;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.DemandeCongeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demandes")
@CrossOrigin(origins = "*")
public class DemandeCongeRestController {

    @Autowired
    private DemandeCongeService demandeCongeService;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private EquipeRepository equipeRepository;

    private Utilisateur getUtilisateurConnecte(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return utilisateurRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur connecté introuvable"));
    }

    private Equipe getEquipeDuManager(Utilisateur manager) {
        if (manager.getEquipe() != null) {
            return manager.getEquipe();
        }
        return equipeRepository.findByManager_Id(manager.getId()).orElse(null);
    }

    // Liste des demandes selon le rôle (RH: société, Manager: équipe, Employé:
    // personnelles)
    @GetMapping
    public ResponseEntity<List<DemandeConge>> getDemandes(
            @RequestParam(required = false, name = "statut") String statut,
            Principal principal) {

        Utilisateur utilisateur = getUtilisateurConnecte(principal);
        Long entrepriseId = utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getId() : null;

        List<DemandeConge> demandes;

        if (utilisateur.getRole() == Role.RH && entrepriseId != null) {
            demandes = demandeCongeService.getDemandesEntreprise(entrepriseId);
        } else if (utilisateur.getRole() == Role.MANAGER) {
            Equipe equipe = getEquipeDuManager(utilisateur);
            if (equipe != null) {
                demandes = demandeCongeService.getDemandesEquipe(equipe.getId());
            } else {
                demandes = demandeCongeService.getDemandesParUtilisateur(utilisateur.getId());
            }
        } else {
            demandes = demandeCongeService.getDemandesParUtilisateur(utilisateur.getId());
        }

        if (statut != null && !statut.isBlank() && !statut.equalsIgnoreCase("TOUTES")
                && !statut.equalsIgnoreCase("TOUS")) {
            demandes = demandes.stream()
                    .filter(d -> d.getStatut().name().equalsIgnoreCase(statut)
                            || d.getStatut().getLibelle().equalsIgnoreCase(statut))
                    .toList();
        }

        return ResponseEntity.ok(demandes);
    }

    // Demandes personnelles de l'utilisateur connecté
    @GetMapping("/mes-demandes")
    public ResponseEntity<List<DemandeConge>> getMesDemandes(Principal principal) {
        Utilisateur utilisateur = getUtilisateurConnecte(principal);
        return ResponseEntity.ok(demandeCongeService.getDemandesParUtilisateur(utilisateur.getId()));
    }

    // Demandes en attente à valider (Manager: équipe, RH: société)
    @GetMapping("/a-valider")
    public ResponseEntity<List<DemandeConge>> getDemandesAValider(Principal principal) {
        Utilisateur utilisateur = getUtilisateurConnecte(principal);
        Long entrepriseId = utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getId() : null;

        if (utilisateur.getRole() == Role.RH && entrepriseId != null) {
            return ResponseEntity.ok(demandeCongeService.getDemandesEnAttenteEntreprise(entrepriseId));
        } else if (utilisateur.getRole() == Role.MANAGER) {
            Equipe equipe = getEquipeDuManager(utilisateur);
            if (equipe != null) {
                return ResponseEntity.ok(demandeCongeService.getDemandesEnAttenteEquipe(equipe.getId()));
            }
        }

        return ResponseEntity.ok(List.of());
    }

    // Consulter une demande spécifique
    @GetMapping("/{id}")
    public ResponseEntity<?> getDemande(@PathVariable("id") Long id, Principal principal) {
        Utilisateur utilisateur = getUtilisateurConnecte(principal);
        DemandeConge demande = demandeCongeService.trouverDemande(id);

        // RG06 : Contrôle entreprise_id à chaque opération
        if (utilisateur.getEntreprise() != null && demande.getEntreprise() != null
                && !demande.getEntreprise().getId().equals(utilisateur.getEntreprise().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Accès refusé : ressource appartenant à une autre entreprise"));
        }

        return ResponseEntity.ok(demande);
    }

    // Créer une nouvelle demande avec validation DTO
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> creerDemande(@Valid @RequestBody DemandeCongeRequest dto, Principal principal) {
        Utilisateur utilisateur = getUtilisateurConnecte(principal);

        DemandeConge demande = new DemandeConge();
        demande.setDateDebut(dto.getDateDebut());
        demande.setDateFin(dto.getDateFin());
        demande.setTypeConge(dto.resolveTypeConge());
        demande.setMotif(dto.getMotif());
        demande.setJustificatif(dto.getJustificatif());

        DemandeConge cree = demandeCongeService.creerDemande(demande, utilisateur);
        return ResponseEntity.status(HttpStatus.CREATED).body(cree);
    }

    // RG04 : Modifier une demande tant qu'elle est en attente
    @PutMapping("/{id}")
    public ResponseEntity<?> modifierDemande(
            @PathVariable("id") Long id,
            @Valid @RequestBody DemandeCongeRequest dto,
            Principal principal) {

        Utilisateur utilisateur = getUtilisateurConnecte(principal);
        TypeConge type = dto.getTypeConge() != null ? dto.resolveTypeConge() : null;

        DemandeConge modifiee = demandeCongeService.modifierDemande(
                id,
                dto.getDateDebut(),
                dto.getDateFin(),
                type,
                dto.getMotif(),
                dto.getJustificatif(),
                utilisateur.getId());
        return ResponseEntity.ok(modifiee);
    }

    // Support de PATCH pour mise à jour partielle
    @PatchMapping("/{id}")
    public ResponseEntity<?> patchDemande(
            @PathVariable("id") Long id,
            @RequestBody DemandeCongeRequest dto,
            Principal principal) {
        return modifierDemande(id, dto, principal);
    }

    // RG04 : Annuler une demande tant qu'elle est en attente
    @DeleteMapping("/{id}")
    public ResponseEntity<?> annulerDemande(@PathVariable("id") Long id, Principal principal) {
        Utilisateur utilisateur = getUtilisateurConnecte(principal);
        DemandeConge annulee = demandeCongeService.annulerDemande(id, utilisateur.getId());
        return ResponseEntity.ok(Map.of("success", true, "message", "Demande annulée", "demande", annulee));
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<?> annulerDemandePost(@PathVariable("id") Long id, Principal principal) {
        return annulerDemande(id, principal);
    }

    // RG05, RG06 : Approuver une demande
    @PostMapping("/{id}/approuver")
    public ResponseEntity<?> approuverDemande(@PathVariable("id") Long id, Principal principal) {
        Utilisateur valideur = getUtilisateurConnecte(principal);
        DemandeConge approuvee = demandeCongeService.approuverDemande(id, valideur);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Demande approuvée avec succès",
                "demande", approuvee));
    }

    // RG07 : Rejeter une demande avec motif
    @PostMapping("/{id}/refuser")
    public ResponseEntity<?> refuserDemande(
            @PathVariable("id") Long id,
            @RequestBody(required = false) RefusDemandeRequest body,
            Principal principal) {

        Utilisateur valideur = getUtilisateurConnecte(principal);
        String motif = body != null ? body.getMotifRejet() : null;

        DemandeConge rejetee = demandeCongeService.refuserDemande(id, valideur, motif);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Demande rejetée",
                "demande", rejetee));
    }
}
