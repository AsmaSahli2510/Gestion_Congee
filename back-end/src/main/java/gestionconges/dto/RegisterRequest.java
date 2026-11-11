package gestionconges.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Le nom de l'entreprise est obligatoire")
        String nomEntreprise,

        @NotBlank(message = "Le nom du responsable RH est obligatoire")
        String nomRh,

        @NotBlank(message = "Le prénom du responsable RH est obligatoire")
        String prenomRh,

        @NotBlank(message = "L'adresse email est obligatoire")
        @Email(message = "Format d'email invalide")
        String emailRh,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 6, message = "Le mot de passe doit comporter au moins 6 caractères")
        String motDePasse
) {
}