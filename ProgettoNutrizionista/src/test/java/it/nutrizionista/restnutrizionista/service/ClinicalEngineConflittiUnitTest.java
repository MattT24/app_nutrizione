package it.nutrizionista.restnutrizionista.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import it.nutrizionista.restnutrizionista.dto.ConflittoClinicoDto;
import it.nutrizionista.restnutrizionista.dto.MotivoValutazioneDto;
import it.nutrizionista.restnutrizionista.dto.ValutazioneClinicaDto;
import it.nutrizionista.restnutrizionista.engine.AlimentoRuleValidator;
import it.nutrizionista.restnutrizionista.engine.TagStandardAllergeneMapping;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;
import it.nutrizionista.restnutrizionista.enums.TagStandard;
import it.nutrizionista.restnutrizionista.repository.AvversionePersonaleRepository;

/**
 * Unit test di {@link ClinicalEngineService#conflittiClinici} (finding F-D1a): filtro dei non-SAFE
 * e — soprattutto — la <b>detection robusta</b> di {@code allergeneDichiarato} (nota di review #2:
 * la detection deve coprire TUTTI i codici allergene emessi dalle regole, senza sollevare eccezioni
 * sui codici non-enum). Usa un validator fake che ritorna un motivo con un {@code codiceTrigger}
 * controllato, così da pilotare esattamente il codice sotto test.
 */
class ClinicalEngineConflittiUnitTest {

    /** Il validator fake ritorna il DTO deciso dal test in funzione dell'alimento. */
    private Function<AlimentoBase, ValutazioneClinicaDto> resultFor;

    private ClinicalEngineService buildService() {
        AlimentoRuleValidator fake = (alimento, tags, blacklist) -> resultFor.apply(alimento);
        AvversionePersonaleRepository avvRepo = mock(AvversionePersonaleRepository.class);
        when(avvRepo.findByClienteIdWithAlimenti(anyLong())).thenReturn(Set.of());
        return new ClinicalEngineService(List.of(fake), avvRepo, false);
    }

    private AlimentoBase aliment(long id, String nome) {
        AlimentoBase a = mock(AlimentoBase.class);
        when(a.getId()).thenReturn(id);
        when(a.getNome()).thenReturn(nome);
        return a; // getAllergeni() → null: Hibernate.initialize(null) è no-op
    }

    private Cliente clienteVuoto() {
        Cliente c = mock(Cliente.class);
        when(c.getId()).thenReturn(1L);
        when(c.getTagStandard()).thenReturn(Set.of());
        return c;
    }

    private ValutazioneClinicaDto grave(String codiceTrigger) {
        return new ValutazioneClinicaDto(LivelloAllerta.ALERT_GRAVE,
                List.of(new MotivoValutazioneDto(codiceTrigger, "motivo di " + codiceTrigger)));
    }

    @Test
    void grave_daAllergene_marcaAllergeneDichiarato() {
        ClinicalEngineService svc = buildService();
        resultFor = a -> grave("ALL_GLUTINE");
        List<ConflittoClinicoDto> out = svc.conflittiClinici(clienteVuoto(), List.of(aliment(10L, "Pane")));
        assertThat(out).hasSize(1);
        assertThat(out.get(0).livello()).isEqualTo(LivelloAllerta.ALERT_GRAVE);
        assertThat(out.get(0).allergeneDichiarato()).isTrue();
    }

    @Test
    void detection_esaustiva_tuttiICodiciAllergene_dellaMappa() {
        ClinicalEngineService svc = buildService();
        // Ogni TagStandard mappato a un Allergene, usato come codiceTrigger, DEVE dare allergeneDichiarato=true.
        for (TagStandard tag : TagStandard.values()) {
            if (TagStandardAllergeneMapping.allergeneFor(tag) == null) continue;
            resultFor = a -> grave(tag.name());
            List<ConflittoClinicoDto> out = svc.conflittiClinici(clienteVuoto(), List.of(aliment(1L, "x")));
            assertThat(out).hasSize(1);
            assertThat(out.get(0).allergeneDichiarato())
                    .as("allergene dichiarato per codice %s", tag.name())
                    .isTrue();
        }
    }

    @Test
    void grave_nonAllergene_nonMarcaAllergene() {
        ClinicalEngineService svc = buildService();
        for (String codice : List.of("PAT_IPERTENSIONE_GRAVE", "AVVERSIONE_ALERT_GRAVE")) {
            resultFor = a -> grave(codice);
            List<ConflittoClinicoDto> out = svc.conflittiClinici(clienteVuoto(), List.of(aliment(1L, "x")));
            assertThat(out).hasSize(1);
            assertThat(out.get(0).allergeneDichiarato())
                    .as("NON allergene per codice %s", codice)
                    .isFalse();
        }
    }

    @Test
    void codiceNonEnum_nonSollevaEccezioni_eNonMarcaAllergene() {
        ClinicalEngineService svc = buildService();
        resultFor = a -> grave("CODICE_INESISTENTE_123");
        List<ConflittoClinicoDto> out = svc.conflittiClinici(clienteVuoto(), List.of(aliment(1L, "x")));
        assertThat(out).hasSize(1);
        assertThat(out.get(0).allergeneDichiarato()).isFalse();
    }

    @Test
    void warning_incluso_safeEInfo_esclusi() {
        ClinicalEngineService svc = buildService();
        AlimentoBase graveFood = aliment(1L, "Grave");
        AlimentoBase warn = aliment(2L, "Warn");
        AlimentoBase safe = aliment(3L, "Safe");
        AlimentoBase info = aliment(4L, "Info");
        resultFor = a -> switch (a.getNome()) {
            case "Grave" -> grave("ALL_ARACHIDI");
            case "Warn" -> new ValutazioneClinicaDto(LivelloAllerta.WARNING,
                    List.of(new MotivoValutazioneDto("INT_LATTOSIO", "tracce")));
            case "Info" -> new ValutazioneClinicaDto(LivelloAllerta.INFO, List.of());
            default -> new ValutazioneClinicaDto(LivelloAllerta.SAFE, List.of());
        };
        List<ConflittoClinicoDto> out = svc.conflittiClinici(clienteVuoto(), List.of(graveFood, warn, safe, info));
        // Solo grave + warning; SAFE e INFO esclusi.
        assertThat(out).extracting(ConflittoClinicoDto::nome).containsExactlyInAnyOrder("Grave", "Warn");
        assertThat(out).extracting(ConflittoClinicoDto::livello)
                .containsExactlyInAnyOrder(LivelloAllerta.ALERT_GRAVE, LivelloAllerta.WARNING);
    }
}
