package gestionconges.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public class EquipeRequest {

    @NotBlank(message = "Le nom de l'équipe est obligatoire")
    @JsonAlias({"nom", "name"})
    private String nom;

    private String description;

    @JsonAlias({"managerId", "manager"})
    private Long managerId;

    private Boolean active = true;

    public EquipeRequest() {
    }

    public EquipeRequest(String nom, String description, Long managerId, Boolean active) {
        this.nom = nom;
        this.description = description;
        this.managerId = managerId;
        this.active = active != null ? active : true;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public Boolean getActive() {
        return active != null ? active : true;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
