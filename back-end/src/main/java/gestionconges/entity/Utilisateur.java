package gestionconges.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gestionconges.enums.Role;
import gestionconges.enums.StatutUtilisateur;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "utilisateurs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutUtilisateur statut = StatutUtilisateur.ACTIF;

    @Column(name = "poste")
    private String poste;

    @Column(name = "telephone")
    private String telephone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id")
    private Equipe equipe;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Utilisateur() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public StatutUtilisateur getStatut() {
        return statut != null ? statut : StatutUtilisateur.ACTIF;
    }

    public void setStatut(StatutUtilisateur statut) {
        this.statut = statut;
    }

    public String getPoste() {
        return poste;
    }

    public void setPoste(String poste) {
        this.poste = poste;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Entreprise getEntreprise() {
        return entreprise;
    }

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
    }

    public Equipe getEquipe() {
        return equipe;
    }

    public void setEquipe(Equipe equipe) {
        this.equipe = equipe;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getNomComplet() {
        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
    }

    @JsonProperty("name")
    public String getName() {
        return getNomComplet().trim();
    }

    @JsonProperty("team")
    public String getTeam() {
        return equipe != null ? equipe.getNom() : "Sans équipe";
    }

    @JsonProperty("active")
    public boolean isActive() {
        return getStatut() == StatutUtilisateur.ACTIF;
    }

    @JsonProperty("entrepriseId")
    public Long getEntrepriseId() {
        return entreprise != null ? entreprise.getId() : null;
    }

    @JsonProperty("entrepriseNom")
    public String getEntrepriseNom() {
        return entreprise != null ? entreprise.getNom() : null;
    }

    @JsonProperty("equipeId")
    public Long getEquipeId() {
        return equipe != null ? equipe.getId() : null;
    }
}