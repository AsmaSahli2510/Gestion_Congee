package gestionconges.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TypeConge {
    ANNUEL("Congés payés", true, false),
    RTT("RTT", true, false),
    MALADIE("Maladie", false, true),
    EXCEPTIONNEL("Événement Familial / Exceptionnel", false, true),
    SANS_SOLDE("Sans solde", false, false),
    MATERNITE("Congé Maternité", false, true),
    PATERNITE("Congé Paternité", false, true);

    private final String libelle;
    private final boolean deduitDuSolde;
    private final boolean justificatifRequis;

    TypeConge(String libelle, boolean deduitDuSolde, boolean justificatifRequis) {
        this.libelle = libelle;
        this.deduitDuSolde = deduitDuSolde;
        this.justificatifRequis = justificatifRequis;
    }

    @JsonValue
    public String getLibelle() {
        return libelle;
    }

    public boolean isDeduitDuSolde() {
        return deduitDuSolde;
    }

    public boolean isJustificatifRequis() {
        return justificatifRequis;
    }

    @JsonCreator
    public static TypeConge fromValue(String value) {
        if (value == null || value.isBlank()) {
            return ANNUEL;
        }
        String v = value.trim().toUpperCase()
                .replace("É", "E")
                .replace("È", "E");
        if (v.contains("PAYE") || v.contains("ANNUEL")) return ANNUEL;
        if (v.contains("RTT")) return RTT;
        if (v.contains("MALADIE")) return MALADIE;
        if (v.contains("SANS") || v.contains("SOLDE")) return SANS_SOLDE;
        if (v.contains("MATERNITE")) return MATERNITE;
        if (v.contains("PATERNITE")) return PATERNITE;
        if (v.contains("EXCEPTIONNEL") || v.contains("FAMILIAL")) return EXCEPTIONNEL;
        try {
            return TypeConge.valueOf(v);
        } catch (Exception e) {
            return ANNUEL;
        }
    }
}