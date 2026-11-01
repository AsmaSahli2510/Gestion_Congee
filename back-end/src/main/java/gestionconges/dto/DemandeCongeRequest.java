package gestionconges.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import gestionconges.enums.TypeConge;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class DemandeCongeRequest {

    @NotNull(message = "La date de début est obligatoire")
    @JsonAlias({"start", "dateDebut"})
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    @JsonAlias({"end", "dateFin"})
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFin;

    @JsonAlias({"type", "typeConge"})
    private String typeConge;

    @JsonAlias({"reason", "motif"})
    private String motif;

    @JsonAlias({"attachment", "justificatif"})
    private String justificatif;

    public DemandeCongeRequest() {
    }

    public DemandeCongeRequest(LocalDate dateDebut, LocalDate dateFin, String typeConge, String motif, String justificatif) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.typeConge = typeConge;
        this.motif = motif;
        this.justificatif = justificatif;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public String getTypeConge() {
        return typeConge;
    }

    public void setTypeConge(String typeConge) {
        this.typeConge = typeConge;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public String getJustificatif() {
        return justificatif;
    }

    public void setJustificatif(String justificatif) {
        this.justificatif = justificatif;
    }

    public TypeConge resolveTypeConge() {
        return typeConge != null ? TypeConge.fromValue(typeConge) : TypeConge.ANNUEL;
    }
}
