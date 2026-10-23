package gestionconges.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mouvements_solde")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MouvementSolde {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solde_id", nullable = false)
    private SoldeConge solde;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private Integer variation; // e.g. -3 ou +25 ou +2

    @Column(nullable = false)
    private String raison;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id")
    private Utilisateur auteur;

    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement = LocalDateTime.now();

    public MouvementSolde() {
    }

    public MouvementSolde(Entreprise entreprise, SoldeConge solde, Utilisateur utilisateur, Integer variation, String raison, Utilisateur auteur) {
        this.entreprise = entreprise;
        this.solde = solde;
        this.utilisateur = utilisateur;
        this.variation = variation;
        this.raison = raison;
        this.auteur = auteur;
        this.dateMouvement = LocalDateTime.now();
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

    public SoldeConge getSolde() {
        return solde;
    }

    public void setSolde(SoldeConge solde) {
        this.solde = solde;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Integer getVariation() {
        return variation;
    }

    public void setVariation(Integer variation) {
        this.variation = variation;
    }

    public String getRaison() {
        return raison;
    }

    public void setRaison(String raison) {
        this.raison = raison;
    }

    public Utilisateur getAuteur() {
        return auteur;
    }

    public void setAuteur(Utilisateur auteur) {
        this.auteur = auteur;
    }

    public LocalDateTime getDateMouvement() {
        return dateMouvement;
    }

    public void setDateMouvement(LocalDateTime dateMouvement) {
        this.dateMouvement = dateMouvement;
    }

    @JsonProperty("userId")
    public Long getUserId() {
        return utilisateur != null ? utilisateur.getId() : null;
    }

    @JsonProperty("amount")
    public Integer getAmount() {
        return variation;
    }

    @JsonProperty("actor")
    public String getActor() {
        return auteur != null ? auteur.getNomComplet().trim() : null;
    }

    @JsonProperty("date")
    public String getDate() {
        return dateMouvement != null ? dateMouvement.toLocalDate().toString() : null;
    }
}
