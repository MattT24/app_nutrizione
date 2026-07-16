package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public class PastoApplyTemplateRequest {
    @NotNull(message = "templateId obbligatorio")
    private Long templateId;

    private PastoApplyTemplateMode mode = PastoApplyTemplateMode.MERGE;

    private PastoApplyTemplateRestrizioniPolicy restrizioniPolicy = PastoApplyTemplateRestrizioniPolicy.SKIP_WARNINGS;

    /**
     * F-D1a — id degli alimenti con conflitto clinico GRAVE inclusi consapevolmente (override auditato).
     * I gravi non presenti in questa lista vengono saltati e riportati; assente/vuota = nessun grave incluso.
     */
    private List<Long> alimentiForzatiIds;

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

    public List<Long> getAlimentiForzatiIds() {
        return alimentiForzatiIds;
    }

    public void setAlimentiForzatiIds(List<Long> alimentiForzatiIds) {
        this.alimentiForzatiIds = alimentiForzatiIds;
    }
}
