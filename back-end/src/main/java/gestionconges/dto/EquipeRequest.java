package gestionconges.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record EquipeRequest(
        @NotBlank(message = "Le nom de l'équipe est obligatoire")
        @JsonAlias({"nom", "name"})
        String nom,

        String description,

        @JsonAlias({"managerId", "manager"})
        Long managerId,

        Boolean active
) {
    /** Une équipe est active par défaut. */
    public EquipeRequest {
        if (active == null) {
            active = true;
        }
    }
}