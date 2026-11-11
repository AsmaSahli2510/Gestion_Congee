package gestionconges.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AjustementSoldeRequest(
        Long soldeId,
        Long userId,

        @NotNull(message = "La variation de solde est obligatoire")
        @JsonAlias({"amount", "variation"})
        Integer variation,

        @NotBlank(message = "Le motif de l'ajustement est obligatoire")
        @JsonAlias({"reason", "raison"})
        String raison
) {
}