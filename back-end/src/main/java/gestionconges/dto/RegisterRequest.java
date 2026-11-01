package gestionconges.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    private String nomEntreprise;

    @NotBlank(message = "Le nom du responsable RH est obligatoire")
    private String nomRh;

    @NotBlank(message = "Le prénom du responsable RH est obligatoire")
    private String prenomRh;

    @NotBlank(message = "L'adresse email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String emailRh;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit comporter au moins 6 caractères")
    private String motDePasse;

    public RegisterRequest() {
    }

    public RegisterRequest(String nomEntreprise, String nomRh, String prenomRh, String emailRh, String motDePasse) {
        this.nomEntreprise = nomEntreprise;
        this.nomRh = nomRh;
        this.prenomRh = prenomRh;
        this.emailRh = emailRh;
        this.motDePasse = motDePasse;
    }

    public String getNomEntreprise() {
        return nomEntreprise != null ? nomEntreprise.trim() : null;
    }

    public void setNomEntreprise(String nomEntreprise) {
        this.nomEntreprise = nomEntreprise;
    }

    public String getNomRh() {
        return nomRh != null ? nomRh.trim() : null;
    }

    public void setNomRh(String nomRh) {
        this.nomRh = nomRh;
    }

    public String getPrenomRh() {
        return prenomRh != null ? prenomRh.trim() : null;
    }

    public void setPrenomRh(String prenomRh) {
        this.prenomRh = prenomRh;
    }

    public String getEmailRh() {
        return emailRh != null ? emailRh.trim().toLowerCase() : null;
    }

    public void setEmailRh(String emailRh) {
        this.emailRh = emailRh;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }
}
