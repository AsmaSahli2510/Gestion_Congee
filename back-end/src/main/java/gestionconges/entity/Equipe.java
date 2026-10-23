package gestionconges.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipes")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Equipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Utilisateur manager;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Equipe() {
    }

    public Equipe(String nom, Entreprise entreprise) {
        this.nom = nom;
        this.entreprise = entreprise;
        this.active = true;
        this.dateCreation = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Entreprise getEntreprise() {
        return entreprise;
    }

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
    }

    public Utilisateur getManager() {
        return manager;
    }

    public void setManager(Utilisateur manager) {
        this.manager = manager;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    @JsonProperty("name")
    public String getName() {
        return nom;
    }

    @JsonProperty("managerId")
    public Long getManagerId() {
        return manager != null ? manager.getId() : null;
    }

    @JsonProperty("managerName")
    public String getManagerName() {
        return manager != null ? manager.getNomComplet().trim() : null;
    }
}
