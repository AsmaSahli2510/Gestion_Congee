package gestionconges.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class RefusDemandeRequest {

    @JsonAlias({"motifRejet", "comment", "commentaire"})
    private String motifRejet;

    public RefusDemandeRequest() {
    }

    public RefusDemandeRequest(String motifRejet) {
        this.motifRejet = motifRejet;
    }

    public String getMotifRejet() {
        return motifRejet;
    }

    public void setMotifRejet(String motifRejet) {
        this.motifRejet = motifRejet;
    }
}
