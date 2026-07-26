package it.nutrizionista.restnutrizionista.dto;

import it.nutrizionista.restnutrizionista.enums.TemplatePdf;
import jakarta.validation.constraints.NotNull;

/** Payload per il salvataggio della preferenza di default del template PDF (endpoint self-service). */
public class PdfTemplatePreferenceDto {

    @NotNull(message = "Il template è obbligatorio")
    private TemplatePdf templatePdf;

    public TemplatePdf getTemplatePdf() {
        return templatePdf;
    }
    public void setTemplatePdf(TemplatePdf templatePdf) {
        this.templatePdf = templatePdf;
    }
}
