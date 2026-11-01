package gestionconges.dto;

import gestionconges.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class InvitationRequest {

    @NotBlank(message = "L'adresse email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    private Role role = Role.EMPLOYE;

    private Long equipeId;

    private String poste;

    public InvitationRequest() {
    }

    public InvitationRequest(String email, Role role, Long equipeId, String poste) {
        this.email = email;
        this.role = role != null ? role : Role.EMPLOYE;
        this.equipeId = equipeId;
        this.poste = poste;
    }

    public String getEmail() {
        return email != null ? email.trim().toLowerCase() : null;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role != null ? role : Role.EMPLOYE;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getEquipeId() {
        return equipeId;
    }

    public void setEquipeId(Long equipeId) {
        this.equipeId = equipeId;
    }

    public String getPoste() {
        return poste;
    }

    public void setPoste(String poste) {
        this.poste = poste;
    }
}
