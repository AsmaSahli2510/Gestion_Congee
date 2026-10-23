package gestionconges.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "entreprises")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Entreprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(name = "identifiant_unique", nullable = false, unique = true)
    private String identifiantUnique;

    @Column(name = "secteur")
    private String secteur;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "jours_conges_annuels")
    private Integer joursCongesAnnuels = 30;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Entreprise() {
    }

    public Entreprise(String nom, String identifiantUnique) {
        this.nom = nom;
        this.identifiantUnique = identifiantUnique;
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

    public String getIdentifiantUnique() {
        return identifiantUnique;
    }

    public void setIdentifiantUnique(String identifiantUnique) {
        this.identifiantUnique = identifiantUnique;
    }

    public String getSecteur() {
        return secteur;
    }

    public void setSecteur(String secteur) {
        this.secteur = secteur;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getJoursCongesAnnuels() {
        return joursCongesAnnuels != null ? joursCongesAnnuels : 30;
    }

    public void setJoursCongesAnnuels(Integer joursCongesAnnuels) {
        this.joursCongesAnnuels = joursCongesAnnuels;
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

    @JsonProperty("acquisition")
    public double getAcquisition() {
        return Math.round((getJoursCongesAnnuels() / 12.0) * 10.0) / 10.0;
    }
}
