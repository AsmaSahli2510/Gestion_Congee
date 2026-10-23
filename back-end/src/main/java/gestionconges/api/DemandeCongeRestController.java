package gestionconges.api;

import gestionconges.entity.DemandeConge;
import gestionconges.entity.Equipe;
import gestionconges.entity.Utilisateur;
import gestionconges.enums.Role;
import gestionconges.enums.TypeConge;
import gestionconges.repository.EquipeRepository;
import gestionconges.repository.UtilisateurRepository;
import gestionconges.service.DemandeCongeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demandes")
public class DemandeCongeRestController {

    private final DemandeCongeService demandeCongeService;
    private final UtilisateurRepository utilisateurRepository;
    private final EquipeRepository equipeRepository;

    public DemandeCongeRestController(
            DemandeCongeService demandeCongeService,
            UtilisateurRepository utilisateurRepository,
            EquipeRepository equipeRepository) {
        this.demandeCongeService = demandeCongeService;
        this.utilisateurRepository = utilisateurRepository;
        this.equipeRepository = equipeRepository;
    }

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

    // Liste des demandes selon le rôle (RH: société, Manager: équipe, Employé: personnelles)
    @GetMapping
    public ResponseEntity<List<DemandeConge>> getDemandes(
            @RequestParam(required = false) String statut,
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

        if (statut != null && !statut.isBlank() && !statut.equalsIgnoreCase("TOUTES") && !statut.equalsIgnoreCase("TOUS")) {
            demandes = demandes.stream()
                    .filter(d -> d.getStatut().name().equalsIgnoreCase(statut) || d.getStatut().getLibelle().equalsIgnoreCase(statut))
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
    public ResponseEntity<?> getDemande(@PathVariable Long id, Principal principal) {
        Utilisateur utilisateur = getUtilisateurConnecte(principal);
        try {
            DemandeConge demande = demandeCongeService.trouverDemande(id);

            // RG06 : Contrôle entreprise_id à chaque opération
            if (utilisateur.getEntreprise() != null && demande.getEntreprise() != null
                    && !demande.getEntreprise().getId().equals(utilisateur.getEntreprise().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Accès refusé : ressource appartenant à une autre entreprise"));
            }

            return ResponseEntity.ok(demande);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    // Créer une nouvelle demande (M2)
    @PostMapping
    public ResponseEntity<?> creerDemande(@RequestBody Map<String, Object> body, Principal principal) {
        Utilisateur utilisateur = getUtilisateurConnecte(principal);

        try {
            String startStr = (String) (body.containsKey("start") ? body.get("start") : body.get("dateDebut"));
            String endStr = (String) (body.containsKey("end") ? body.get("end") : body.get("dateFin"));
            String typeStr = (String) (body.containsKey("type") ? body.get("type") : body.get("typeConge"));
            String reason = (String) (body.containsKey("reason") ? body.get("reason") : body.get("motif"));
            String attachment = (String) (body.containsKey("attachment") ? body.get("attachment") : body.get("justificatif"));

            if (startStr == null || endStr == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Dates de début et de fin obligatoires"));
            }

            DemandeConge demande = new DemandeConge();
            demande.setDateDebut(LocalDate.parse(startStr));
            demande.setDateFin(LocalDate.parse(endStr));
            demande.setTypeConge(typeStr != null ? TypeConge.fromValue(typeStr) : TypeConge.ANNUEL);
            demande.setMotif(reason);
            demande.setJustificatif(attachment);

            DemandeConge cree = demandeCongeService.creerDemande(demande, utilisateur);
            return ResponseEntity.status(HttpStatus.CREATED).body(cree);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la création : " + e.getMessage()));
        }
    }

    // RG04 : Modifier une demande tant qu'elle est en attente
    @PutMapping("/{id}")
    public ResponseEntity<?> modifierDemande(@PathVariable Long id, @RequestBody Map<String, Object> body, Principal principal) {
        Utilisateur utilisateur = getUtilisateurConnecte(principal);

        try {
            String startStr = (String) (body.containsKey("start") ? body.get("start") : body.get("dateDebut"));
            String endStr = (String) (body.containsKey("end") ? body.get("end") : body.get("dateFin"));
            String typeStr = (String) (body.containsKey("type") ? body.get("type") : body.get("typeConge"));
            String reason = (String) (body.containsKey("reason") ? body.get("reason") : body.get("motif"));
            String attachment = (String) (body.containsKey("attachment") ? body.get("attachment") : body.get("justificatif"));

            LocalDate debut = startStr != null ? LocalDate.parse(startStr) : null;
            LocalDate fin = endStr != null ? LocalDate.parse(endStr) : null;
            TypeConge type = typeStr != null ? TypeConge.fromValue(typeStr) : null;

            DemandeConge modifiee = demandeCongeService.modifierDemande(id, debut, fin, type, reason, attachment, utilisateur.getId());
            return ResponseEntity.ok(modifiee);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la modification : " + e.getMessage()));
        }
    }

    // RG04 : Annuler une demande tant qu'elle est en attente
    @DeleteMapping("/{id}")
    public ResponseEntity<?> annulerDemande(@PathVariable Long id, Principal principal) {
        Utilisateur utilisateur = getUtilisateurConnecte(principal);
        try {
            DemandeConge annulee = demandeCongeService.annulerDemande(id, utilisateur.getId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Demande annulée", "demande", annulee));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<?> annulerDemandePost(@PathVariable Long id, Principal principal) {
        return annulerDemande(id, principal);
    }

    // RG05, RG06 : Approuver une demande
    @PostMapping("/{id}/approuver")
    public ResponseEntity<?> approuverDemande(@PathVariable Long id, Principal principal) {
        Utilisateur valideur = getUtilisateurConnecte(principal);
        try {
            DemandeConge approuvee = demandeCongeService.approuverDemande(id, valideur);
            return ResponseEntity.ok(Map.of("success", true, "message", "Demande approuvée avec succès", "demande", approuvee));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'approbation : " + e.getMessage()));
        }
    }

    // RG07 : Rejeter une demande avec motif
    @PostMapping("/{id}/refuser")
    public ResponseEntity<?> refuserDemande(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body, Principal principal) {
        Utilisateur valideur = getUtilisateurConnecte(principal);
        String motif = null;
        if (body != null) {
            motif = body.containsKey("motifRejet") ? body.get("motifRejet") : body.get("comment");
        }

        try {
            DemandeConge rejetee = demandeCongeService.refuserDemande(id, valideur, motif);
            return ResponseEntity.ok(Map.of("success", true, "message", "Demande rejetée", "demande", rejetee));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors du rejet : " + e.getMessage()));
        }
    }
}
