package gestionconges.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AjustementSoldeRequest {

    private Long soldeId;

    private Long userId;

    @NotNull(message = "La variation de solde est obligatoire")
    @JsonAlias({"amount", "variation"})
    private Integer variation;

    @NotBlank(message = "Le motif de l'ajustement est obligatoire")
    @JsonAlias({"reason", "raison"})
    private String raison;

    public AjustementSoldeRequest() {
    }

    public AjustementSoldeRequest(Long soldeId, Long userId, Integer variation, String raison) {
        this.soldeId = soldeId;
        this.userId = userId;
        this.variation = variation;
        this.raison = raison;
    }

    public Long getSoldeId() {
        return soldeId;
    }

    public void setSoldeId(Long soldeId) {
        this.soldeId = soldeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getVariation() {
        return variation;
    }

    public void setVariation(Integer variation) {
        this.variation = variation;
    }

    public String getRaison() {
        return raison;
    }

    public void setRaison(String raison) {
        this.raison = raison;
    }
}
