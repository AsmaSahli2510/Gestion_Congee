package gestionconges.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;

public record RefusDemandeRequest(
        @JsonAlias({"motifRejet", "comment", "commentaire"})
        String motifRejet
) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RefusDemandeRequest {
    }
}