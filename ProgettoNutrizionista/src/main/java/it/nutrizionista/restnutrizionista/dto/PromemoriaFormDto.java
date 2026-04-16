package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public class PromemoriaFormDto {
    @NotBlank(message = "Il testo del promemoria è obbligatorio")
    private String testo;

    @NotNull(message = "La data di inizio è obbligatoraria")
    private LocalDate data;

    private LocalTime ora;
    
    private LocalDate endData;
    
    private LocalTime endOra;

    private boolean allDay;

    // Getters and Setters
    public String getTesto() { return testo; }
    public void setTesto(String testo) { this.testo = testo; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public LocalTime getOra() { return ora; }
    public void setOra(LocalTime ora) { this.ora = ora; }

    public LocalDate getEndData() { return endData; }
    public void setEndData(LocalDate endData) { this.endData = endData; }

    public LocalTime getEndOra() { return endOra; }
    public void setEndOra(LocalTime endOra) { this.endOra = endOra; }

    public boolean isAllDay() { return allDay; }
    public void setAllDay(boolean allDay) { this.allDay = allDay; }
}
