package gestionconges.dto;

import gestionconges.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InvitationRequest(
        @NotBlank(message = "L'adresse email est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,

        Role role,
        Long equipeId,
        String poste
) {
    /** Rôle par défaut : EMPLOYE. */
    public InvitationRequest {
        if (role == null) {
            role = Role.EMPLOYE;
        }
    }
}