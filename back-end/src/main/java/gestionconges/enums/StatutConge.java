package gestionconges.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StatutConge {
    SOUMISE("SOUMISE", "status-pending"),
    EN_ATTENTE("SOUMISE", "status-pending"),
    APPROUVEE("APPROUVÉE", "status-approved"),
    REJETEE("REJETÉE", "status-rejected"),
    REFUSEE("REJETÉE", "status-rejected"),
    ANNULEE("ANNULÉE", "status-cancelled");

    private final String libelle;
    private final String cssClass;

    StatutConge(String libelle, String cssClass) {
        this.libelle = libelle;
        this.cssClass = cssClass;
    }

    @JsonValue
    public String getLibelle() {
        return libelle;
    }

    public String getCssClass() {
        return cssClass;
    }

    public boolean isEnAttente() {
        return this == SOUMISE || this == EN_ATTENTE;
    }

    public boolean isApprouvee() {
        return this == APPROUVEE;
    }

    public boolean isRejetee() {
        return this == REJETEE || this == REFUSEE;
    }

    public boolean isAnnulee() {
        return this == ANNULEE;
    }

    @JsonCreator
    public static StatutConge fromValue(String value) {
        if (value == null || value.isBlank()) {
            return EN_ATTENTE;
        }
        String val = value.trim().toUpperCase()
                .replace("É", "E")
                .replace("È", "E");
        switch (val) {
            case "SOUMISE":
            case "EN_ATTENTE":
            case "EN ATTENTE":
                return EN_ATTENTE;
            case "APPROUVEE":
            case "APPROUVÉE":
                return APPROUVEE;
            case "REJETEE":
            case "REJETÉE":
            case "REFUSEE":
            case "REFUSÉE":
                return REFUSEE;
            case "ANNULEE":
            case "ANNULÉE":
                return ANNULEE;
            default:
                try {
                    return StatutConge.valueOf(val);
                } catch (Exception e) {
                    return EN_ATTENTE;
                }
        }
    }
}