package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class EstendiAccountDemoRequest {
    @NotNull @Min(1) @Max(30)
    private Integer giorni;
    public Integer getGiorni() { return giorni; }
    public void setGiorni(Integer giorni) { this.giorni = giorni; }
}
