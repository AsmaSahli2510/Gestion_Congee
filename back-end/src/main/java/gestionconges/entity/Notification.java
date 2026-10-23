package gestionconges.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private Utilisateur destinataire;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "lecture", nullable = false)
    private boolean lecture = false;

    @Column(name = "lien")
    private String lien;

    public Notification() {
    }

    public Notification(Entreprise entreprise, Utilisateur destinataire, String titre, String message, String lien) {
        this.entreprise = entreprise;
        this.destinataire = destinataire;
        this.titre = titre;
        this.message = message;
        this.lien = lien;
        this.lecture = false;
        this.dateCreation = LocalDateTime.now();
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

    public Utilisateur getDestinataire() {
        return destinataire;
    }

    public void setDestinataire(Utilisateur destinataire) {
        this.destinataire = destinataire;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public boolean isLecture() {
        return lecture;
    }

    public void setLecture(boolean lecture) {
        this.lecture = lecture;
    }

    public String getLien() {
        return lien;
    }

    public void setLien(String lien) {
        this.lien = lien;
    }

    @JsonProperty("userId")
    public Long getUserId() {
        return destinataire != null ? destinataire.getId() : null;
    }

    @JsonProperty("text")
    public String getText() {
        return message;
    }

    @JsonProperty("read")
    public boolean isRead() {
        return lecture;
    }
}
