package gestionconges.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gestionconges.enums.Role;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invitations")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id")
    private Equipe equipe;

    @Column(name = "poste")
    private String poste;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    @Column(nullable = false)
    private boolean utilisee = false;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Invitation() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Entreprise getEntreprise() {
        return entreprise;
    }

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Equipe getEquipe() {
        return equipe;
    }

    public void setEquipe(Equipe equipe) {
        this.equipe = equipe;
    }

    public String getPoste() {
        return poste;
    }

    public void setPoste(String poste) {
        this.poste = poste;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDateTime dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public boolean isUtilisee() {
        return utilisee;
    }

    public void setUtilisee(boolean utilisee) {
        this.utilisee = utilisee;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public boolean isExpiree() {
        return LocalDateTime.now().isAfter(dateExpiration);
    }

    @JsonProperty("team")
    public String getTeam() {
        return equipe != null ? equipe.getNom() : null;
    }

    @JsonProperty("expiresAt")
    public String getExpiresAt() {
        return dateExpiration != null ? dateExpiration.toString() : null;
    }

    @JsonProperty("used")
    public boolean isUsed() {
        return utilisee;
    }

    @JsonProperty("entrepriseNom")
    public String getEntrepriseNom() {
        return entreprise != null ? entreprise.getNom() : null;
    }
}
