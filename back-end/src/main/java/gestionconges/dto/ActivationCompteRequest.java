package gestionconges.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivationCompteRequest(
        @NotBlank(message = "Le token d'invitation est obligatoire")
        String token,

        @NotBlank(message = "Le nom est obligatoire")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        String prenom,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 6, message = "Le mot de passe doit comporter au moins 6 caractères")
        String motDePasse
) {
}