package gestionconges.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class PeriodeConge implements Serializable {

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "nombre_jours")
    private Integer nombreJours = 1;

    public PeriodeConge() {
    }

    public PeriodeConge(LocalDate dateDebut, LocalDate dateFin) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.nombreJours = calculerJoursOuvrables();
    }

    public PeriodeConge(LocalDate dateDebut, LocalDate dateFin, Integer nombreJours) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.nombreJours = (nombreJours != null && nombreJours > 0) ? nombreJours : calculerJoursOuvrables();
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

    public Integer getNombreJours() {
        return nombreJours != null ? nombreJours : 1;
    }

    public void setNombreJours(Integer nombreJours) {
        this.nombreJours = nombreJours;
    }

    public int calculerJoursOuvrables() {
        if (dateDebut == null || dateFin == null || dateFin.isBefore(dateDebut)) {
            return 0;
        }

        int total = 0;
        for (LocalDate jour = dateDebut; !jour.isAfter(dateFin); jour = jour.plusDays(1)) {
            DayOfWeek dow = jour.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                total++;
            }
        }
        return total;
    }

    public boolean isEnChevauchement(PeriodeConge autre) {
        if (autre == null || autre.getDateDebut() == null || autre.getDateFin() == null
                || this.dateDebut == null || this.dateFin == null) {
            return false;
        }
        return !(this.dateFin.isBefore(autre.getDateDebut()) || this.dateDebut.isAfter(autre.getDateFin()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PeriodeConge that)) return false;
        return Objects.equals(dateDebut, that.dateDebut) &&
                Objects.equals(dateFin, that.dateFin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dateDebut, dateFin);
    }
}
