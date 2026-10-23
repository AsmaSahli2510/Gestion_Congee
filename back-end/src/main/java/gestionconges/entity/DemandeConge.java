package gestionconges.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gestionconges.enums.StatutConge;
import gestionconges.enums.TypeConge;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "demandes_conge")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DemandeConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "nombre_jours")
    private Integer nombreJours = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_conge", length = 30)
    private TypeConge typeConge = TypeConge.ANNUEL;

    @Column(name = "motif", columnDefinition = "TEXT")
    private String motif;

    @Column(name = "justificatif")
    private String justificatif;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutConge statut = StatutConge.EN_ATTENTE;

    @Column(name = "commentaire_decision", columnDefinition = "TEXT")
    private String commentaireDecision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valideur_id")
    private Utilisateur valideur;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public DemandeConge() {
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

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public Integer getNombreJours() {
        return nombreJours != null ? nombreJours : 1;
    }

    public void setNombreJours(Integer nombreJours) {
        this.nombreJours = nombreJours;
    }

    public TypeConge getTypeConge() {
        return typeConge != null ? typeConge : TypeConge.ANNUEL;
    }

    public void setTypeConge(TypeConge typeConge) {
        this.typeConge = typeConge;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public String getJustificatif() {
        return justificatif;
    }

    public void setJustificatif(String justificatif) {
        this.justificatif = justificatif;
    }

    public StatutConge getStatut() {
        return statut != null ? statut : StatutConge.EN_ATTENTE;
    }

    public void setStatut(StatutConge statut) {
        this.statut = statut;
    }

    public String getCommentaireDecision() {
        return commentaireDecision;
    }

    public void setCommentaireDecision(String commentaireDecision) {
        this.commentaireDecision = commentaireDecision;
    }

    public Utilisateur getValideur() {
        return valideur;
    }

    public void setValideur(Utilisateur valideur) {
        this.valideur = valideur;
    }

    public LocalDateTime getDateDecision() {
        return dateDecision;
    }

    public void setDateDecision(LocalDateTime dateDecision) {
        this.dateDecision = dateDecision;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    @JsonProperty("userId")
    public Long getUserId() {
        return utilisateur != null ? utilisateur.getId() : null;
    }

    @JsonProperty("userName")
    public String getUserName() {
        return utilisateur != null ? utilisateur.getNomComplet().trim() : null;
    }

    @JsonProperty("userEmail")
    public String getUserEmail() {
        return utilisateur != null ? utilisateur.getEmail() : null;
    }

    @JsonProperty("userTeam")
    public String getUserTeam() {
        return (utilisateur != null && utilisateur.getEquipe() != null) ? utilisateur.getEquipe().getNom() : "Sans équipe";
    }

    @JsonProperty("start")
    public String getStart() {
        return dateDebut != null ? dateDebut.toString() : null;
    }

    @JsonProperty("end")
    public String getEnd() {
        return dateFin != null ? dateFin.toString() : null;
    }

    @JsonProperty("days")
    public Integer getDays() {
        return nombreJours != null ? nombreJours : 1;
    }

    @JsonProperty("type")
    public String getType() {
        return typeConge != null ? typeConge.getLibelle() : "Congés payés";
    }

    @JsonProperty("status")
    public String getStatus() {
        return statut != null ? statut.getLibelle() : "SOUMISE";
    }

    @JsonProperty("reason")
    public String getReason() {
        return motif;
    }

    @JsonProperty("comment")
    public String getComment() {
        return commentaireDecision;
    }

    @JsonProperty("actor")
    public String getActor() {
        return valideur != null ? valideur.getNomComplet().trim() : null;
    }

    @JsonProperty("created")
    public String getCreated() {
        return dateCreation != null ? dateCreation.toLocalDate().toString() : null;
    }
}