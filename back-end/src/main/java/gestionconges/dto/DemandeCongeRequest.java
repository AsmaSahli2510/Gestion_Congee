package gestionconges.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import gestionconges.enums.TypeConge;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DemandeCongeRequest(
        @NotNull(message = "La date de début est obligatoire")
        @JsonAlias({"start", "dateDebut"})
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        @JsonAlias({"end", "dateFin"})
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dateFin,

        @JsonAlias({"type", "typeConge"})
        String typeConge,

        @JsonAlias({"reason", "motif"})
        String motif,

        @JsonAlias({"attachment", "justificatif"})
        String justificatif
) {
    /** Convertit le texte reçu en {@link TypeConge} (ANNUEL par défaut). */
    public TypeConge resolveTypeConge() {
        return typeConge != null ? TypeConge.fromValue(typeConge) : TypeConge.ANNUEL;
    }
}