package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotNull;

public class PastoApplyTemplateRequest {
    @NotNull(message = "templateId obbligatorio")
    private Long templateId;

    private PastoApplyTemplateMode mode = PastoApplyTemplateMode.MERGE;

    private PastoApplyTemplateRestrizioniPolicy restrizioniPolicy = PastoApplyTemplateRestrizioniPolicy.SKIP_WARNINGS;

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public PastoApplyTemplateMode getMode() {
        return mode;
    }

    public void setMode(PastoApplyTemplateMode mode) {
        this.mode = mode;
    }

    public PastoApplyTemplateRestrizioniPolicy getRestrizioniPolicy() {
        return restrizioniPolicy;
    }

    public void setRestrizioniPolicy(PastoApplyTemplateRestrizioniPolicy restrizioniPolicy) {
        this.restrizioniPolicy = restrizioniPolicy;
    }
}
