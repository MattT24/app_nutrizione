package it.nutrizionista.restnutrizionista.engine;

import it.nutrizionista.restnutrizionista.dto.MotivoValutazioneDto;
import it.nutrizionista.restnutrizionista.dto.ValutazioneClinicaDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AvversionePersonale;
import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;
import it.nutrizionista.restnutrizionista.enums.TagStandard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Regola Clinica PoC #2 (Matematica con Soglie Esternalizzate).
 * Valuta il profilo salino dell'alimento per pazienti ipertesi.
 *
 * Soglie configurabili in application.properties:
 *   clinica.soglia.sale-warning-g=0.6
 *   clinica.soglia.sale-grave-g=1.5
 *
 * Valori di default basati su linee guida OMS (per 100g di prodotto):
 * - Soglia WARNING: > 0.6g di sale (240mg di sodio)
 * - Soglia ALERT_GRAVE: > 1.5g di sale (600mg di sodio)
 *
 * @order 10 — Priorità alta (rischio cardiovascolare), ma inferiore ad allergie
 */
@Component
@Order(10)
public class IpertensioneRule implements AlimentoRuleValidator {

    private final double sogliaWarningSaleG;
    private final double sogliaGraveSaleG;

    // Iniezione esplicita via costruttore (No Lombok — pattern AlimentoBase)
    public IpertensioneRule(
            @Value("${clinica.soglia.sale-warning-g:0.6}") double sogliaWarningSaleG,
            @Value("${clinica.soglia.sale-grave-g:1.5}") double sogliaGraveSaleG) {
        this.sogliaWarningSaleG = sogliaWarningSaleG;
        this.sogliaGraveSaleG = sogliaGraveSaleG;
    }

    @Override
    public ValutazioneClinicaDto valida(
            AlimentoBase alimento,
            Set<TagStandard> tagStandard,
            Set<AvversionePersonale> blacklistManuale) {

        // Regola applicabile SOLO ai pazienti ipertesi
        if (!tagStandard.contains(TagStandard.PAT_IPERTENSIONE)) {
            return new ValutazioneClinicaDto(LivelloAllerta.SAFE, List.of());
        }

        // Recupera il contenuto di sodio (in mg per 100g) dai macro dell'alimento
        if (alimento.getMacroNutrienti() == null || alimento.getMacroNutrienti().getSodio() == null) {
            return new ValutazioneClinicaDto(LivelloAllerta.SAFE, List.of());
        }

        // Conversione sodio (mg) → sale (g): Sale = Sodio_mg × 2.5 / 1000
        double sodioMg = alimento.getMacroNutrienti().getSodio();
        double saleG = sodioMg * 2.5 / 1000.0;

        if (saleG > sogliaGraveSaleG) {
            MotivoValutazioneDto motivo = new MotivoValutazioneDto(
                    "PAT_IPERTENSIONE_GRAVE",
                    String.format("🔴 Contenuto di sale molto elevato (%.2fg/100g, soglia: %.2fg). Sconsigliato per pazienti ipertesi.", saleG, sogliaGraveSaleG)
            );
            return new ValutazioneClinicaDto(LivelloAllerta.ALERT_GRAVE, List.of(motivo));
        }

        if (saleG > sogliaWarningSaleG) {
            MotivoValutazioneDto motivo = new MotivoValutazioneDto(
                    "PAT_IPERTENSIONE_WARNING",
                    String.format("🟡 Contenuto di sale elevato (%.2fg/100g, soglia: %.2fg). Consumare con moderazione.", saleG, sogliaWarningSaleG)
            );
            return new ValutazioneClinicaDto(LivelloAllerta.WARNING, List.of(motivo));
        }

        return new ValutazioneClinicaDto(LivelloAllerta.SAFE, List.of());
    }
}
