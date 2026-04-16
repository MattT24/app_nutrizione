package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entità che rappresenta un alimento alternativo (sostitutivo)
 * per un AlimentoPastoSchedaTemplate all'interno di un template.
 * Mirror di AlimentoAlternativo adattato al dominio SchedaTemplate.
 */
@Entity
@Table(name = "alimenti_scheda_template_alternative",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"alimento_pasto_scheda_template_id", "alimento_alternativo_id"},
            name = "uk_apt_alternativo"
        )
    })
@EntityListeners(AuditingEntityListener.class)
public class AlimentoSchedaTemplateAlternativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * L'alimento nel pasto del template per cui questa è un'alternativa
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alimento_pasto_scheda_template_id", nullable = false)
    private AlimentoPastoSchedaTemplate alimentoPastoSchedaTemplate;

    /**
     * L'alimento alternativo (sostitutivo) dal catalogo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alimento_alternativo_id", nullable = false)
    private AlimentoBase alimentoAlternativo;

    /**
     * Quantità in grammi consigliata per l'alternativa
     */
    @Column(nullable = false)
    private Integer quantita = 100;

    /**
     * Priorità di preferenza (1 = prima scelta, 2 = seconda, ecc.)
     */
    @Column(nullable = false)
    private Integer priorita = 1;

    /**
     * Modalità di calcolo suggerita (usata per ricalcoli automatici quando manual=false)
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlternativeMode mode = AlternativeMode.CALORIE;

    /**
     * Se true, la quantità è stata impostata manualmente e non viene ricalcolata automaticamente
     */
    @Column(nullable = false)
    private Boolean manual = false;

    /**
     * Note opzionali (es. "solo se biologico", "preferire fresco")
     */
    @Column(length = 500)
    private String note;

    /**
     * Nome custom per l'alimento alternativo (override del nome base)
     */
    @Column(name = "nome_custom")
    private String nomeCustom;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    // ── Constructors ──

    public AlimentoSchedaTemplateAlternativa() {}

    public AlimentoSchedaTemplateAlternativa(AlimentoPastoSchedaTemplate alimentoPastoSchedaTemplate,
                                             AlimentoBase alimentoAlternativo,
                                             Integer quantita) {
        this.alimentoPastoSchedaTemplate = alimentoPastoSchedaTemplate;
        this.alimentoAlternativo = alimentoAlternativo;
        this.quantita = quantita;
    }

    // ── Getters and Setters ──

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AlimentoPastoSchedaTemplate getAlimentoPastoSchedaTemplate() {
        return alimentoPastoSchedaTemplate;
    }

    public void setAlimentoPastoSchedaTemplate(AlimentoPastoSchedaTemplate alimentoPastoSchedaTemplate) {
        this.alimentoPastoSchedaTemplate = alimentoPastoSchedaTemplate;
    }

    public AlimentoBase getAlimentoAlternativo() {
        return alimentoAlternativo;
    }

    public void setAlimentoAlternativo(AlimentoBase alimentoAlternativo) {
        this.alimentoAlternativo = alimentoAlternativo;
    }

    public Integer getQuantita() {
        return quantita;
    }

    public void setQuantita(Integer quantita) {
        this.quantita = quantita;
    }

    public Integer getPriorita() {
        return priorita;
    }

    public void setPriorita(Integer priorita) {
        this.priorita = priorita;
    }

    public AlternativeMode getMode() {
        return mode;
    }

    public void setMode(AlternativeMode mode) {
        this.mode = mode;
    }

    public Boolean getManual() {
        return manual;
    }

    public void setManual(Boolean manual) {
        this.manual = manual;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getNomeCustom() {
        return nomeCustom;
    }

    public void setNomeCustom(String nomeCustom) {
        this.nomeCustom = nomeCustom;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlimentoSchedaTemplateAlternativa that = (AlimentoSchedaTemplateAlternativa) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
