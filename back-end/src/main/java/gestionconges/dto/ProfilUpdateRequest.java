package gestionconges.dto;

public class ProfilUpdateRequest {

    private String nom;
    private String prenom;
    private String telephone;
    private String ancienMotDePasse;
    private String nouveauMotDePasse;

    public ProfilUpdateRequest() {
    }

    public ProfilUpdateRequest(String nom, String prenom, String telephone, String ancienMotDePasse, String nouveauMotDePasse) {
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.ancienMotDePasse = ancienMotDePasse;
        this.nouveauMotDePasse = nouveauMotDePasse;
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

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getAncienMotDePasse() {
        return ancienMotDePasse;
    }

    public void setAncienMotDePasse(String ancienMotDePasse) {
        this.ancienMotDePasse = ancienMotDePasse;
    }

    public String getNouveauMotDePasse() {
        return nouveauMotDePasse;
    }

    public void setNouveauMotDePasse(String nouveauMotDePasse) {
        this.nouveauMotDePasse = nouveauMotDePasse;
    }
}
