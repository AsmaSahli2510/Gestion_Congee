package gestionconges.dto;

public record ProfilUpdateRequest(
        String nom,
        String prenom,
        String telephone,
        String ancienMotDePasse,
        String nouveauMotDePasse
) {
}