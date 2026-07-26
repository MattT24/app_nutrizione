package it.nutrizionista.restnutrizionista.dto;

import it.nutrizionista.restnutrizionista.enums.TemaPdf;
import jakarta.validation.constraints.NotNull;

/** Payload per il salvataggio della preferenza di default del tema colore PDF (endpoint self-service). */
public class PdfTemaColorePreferenceDto {

    @NotNull(message = "Il tema colore è obbligatorio")
    private TemaPdf temaColore;

    public TemaPdf getTemaColore() {
        return temaColore;
    }
    public void setTemaColore(TemaPdf temaColore) {
        this.temaColore = temaColore;
    }
}
