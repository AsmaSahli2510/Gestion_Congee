package gestionconges.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "soldes_conge")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SoldeConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "annee", nullable = false)
    private Integer annee;

    @Column(name = "jours_acquis", nullable = false)
    private Integer joursAcquis;

    @Column(name = "jours_utilises", nullable = false)
    private Integer joursUtilises;

    public SoldeConge() {
    }

    public SoldeConge(Entreprise entreprise, Utilisateur utilisateur, Integer annee, Integer joursAcquis, Integer joursUtilises) {
        this.entreprise = entreprise;
        this.utilisateur = utilisateur;
        this.annee = annee;
        this.joursAcquis = joursAcquis;
        this.joursUtilises = joursUtilises;
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

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Integer getAnnee() {
        return annee;
    }

    public void setAnnee(Integer annee) {
        this.annee = annee;
    }

    public Integer getJoursAcquis() {
        return joursAcquis != null ? joursAcquis : 0;
    }

    public void setJoursAcquis(Integer joursAcquis) {
        this.joursAcquis = joursAcquis;
    }

    public Integer getJoursUtilises() {
        return joursUtilises != null ? joursUtilises : 0;
    }

    public void setJoursUtilises(Integer joursUtilises) {
        this.joursUtilises = joursUtilises;
    }

    @Transient
    @JsonProperty("joursRestants")
    public Integer getJoursRestants() {
        int acquis = joursAcquis != null ? joursAcquis : 0;
        int utilises = joursUtilises != null ? joursUtilises : 0;
        return Math.max(0, acquis - utilises);
    }

    @Transient
    @JsonProperty("joursDisponibles")
    public Integer getJoursDisponibles() {
        return getJoursRestants();
    }

    @Transient
    @JsonProperty("balance")
    public Integer getBalance() {
        return getJoursRestants();
    }

    @JsonProperty("userId")
    public Long getUserId() {
        return utilisateur != null ? utilisateur.getId() : null;
    }

    @JsonProperty("userName")
    public String getUserName() {
        return utilisateur != null ? utilisateur.getNomComplet().trim() : null;
    }

    @JsonProperty("team")
    public String getTeam() {
        return (utilisateur != null && utilisateur.getEquipe() != null) ? utilisateur.getEquipe().getNom() : "Sans équipe";
    }
}