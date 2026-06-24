package it.nutrizionista.restnutrizionista.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.nutrizionista.restnutrizionista.dto.MacroDto;
import it.nutrizionista.restnutrizionista.dto.OffProductDto;

/**
 * Test del parsing macro dal NUOVO schema nutrition v3.6 ({@code nutrition.aggregated_set.nutrients})
 * e del fallback al vecchio {@code nutriments} piatto (v2). {@code buildMacro} non usa le dipendenze
 * iniettate, quindi si istanzia il service direttamente.
 */
class OffNutritionMappingTest {

    private final ObjectMapper om = new ObjectMapper();
    private final OpenFoodFactsService service = new OpenFoodFactsService();

    private OffProductDto.Product parse(String json) throws Exception {
        return om.readValue(json, OffProductDto.class).getProduct();
    }

    @Test
    void parsaMacroDalNuovoSchemaNutritionV36() throws Exception {
        // Struttura reale v3.6 (?fields=nutrition) — barcode 8017596064349
        String json = """
            { "status": "success", "product": { "code": "8017596064349", "nutrition": { "aggregated_set": {
              "per": "100g", "preparation": "as_sold", "nutrients": {
                "energy-kcal":   { "value": 390, "value_computed": 389.8, "unit": "kcal", "source_per": "100g" },
                "proteins":      { "value": 13,  "unit": "g", "source_per": "100g" },
                "carbohydrates": { "value": 68,  "unit": "g" },
                "fat":           { "value": 5.8, "unit": "g" },
                "saturated-fat": { "value": 0.9, "unit": "g" },
                "sugars":        { "value": 4.5, "unit": "g" },
                "added-sugars":  { "value": 1.75, "modifier": "~", "unit": "g" },
                "fiber":         { "value": 6.8, "unit": "g" },
                "salt":          { "value": 1.5, "unit": "g" },
                "sodium":        { "value": 0.6, "unit": "g" }
              } } } } }
            """;
        MacroDto m = service.buildMacro(parse(json));
        assertThat(m).isNotNull();
        assertThat(m.getCalorie()).isEqualTo(390.0);
        assertThat(m.getProteine()).isEqualTo(13.0);
        assertThat(m.getCarboidrati()).isEqualTo(68.0);
        assertThat(m.getGrassi()).isEqualTo(5.8);
        assertThat(m.getGrassiSaturi()).isEqualTo(0.9);
        assertThat(m.getZuccheri()).isEqualTo(4.5);
        assertThat(m.getZuccheriAggiunti()).isEqualTo(1.75);
        assertThat(m.getFibre()).isEqualTo(6.8);
        assertThat(m.getSale()).isEqualTo(1.5);
        assertThat(m.getSodio()).isEqualTo(0.6);
    }

    @Test
    void preferisceValueSuValueComputed_eDerivaKcalDaKj() throws Exception {
        // Solo energia in kJ (niente energy-kcal) → kcal derivata = kJ / 4.184
        String json = """
            { "product": { "nutrition": { "aggregated_set": { "per": "100g", "nutrients": {
              "energy-kj":     { "value": 2252 },
              "proteins":      { "value": 13 },
              "carbohydrates": { "value": 68 },
              "fat":           { "value": 5.8 }
            } } } } }
            """;
        MacroDto m = service.buildMacro(parse(json));
        assertThat(m).isNotNull();
        assertThat(m.getCalorie()).isEqualTo(Math.round((2252.0 / 4.184) * 100.0) / 100.0); // ~538.24
        assertThat(m.getEnergiaKj()).isEqualTo(2252.0);
    }

    @Test
    void fallbackAlNutrimentsPiattoLegacyV2() throws Exception {
        // v2: niente nutrition, nutriments piatto valorizzato
        String json = """
            { "status": 1, "product": { "code": "x", "nutriments": {
              "energy-kcal_100g": 390, "proteins_100g": 13, "carbohydrates_100g": 68, "fat_100g": 5.8, "salt_100g": 1.5
            } } }
            """;
        MacroDto m = service.buildMacro(parse(json));
        assertThat(m).isNotNull();
        assertThat(m.getCalorie()).isEqualTo(390.0);
        assertThat(m.getSale()).isEqualTo(1.5);
    }

    @Test
    void nutritionVuota_eNutrimentsAssente_ritornaNull() throws Exception {
        // Caso v3.6 "rotto": nutrition con aggregated_set vuoto e nessun nutriments → null (→ il chiamante fa fallback v2 / 422)
        String json = """
            { "status": "success", "product": { "code": "x", "nutrition": { "aggregated_set": { "per": "100g", "nutrients": {} } } } }
            """;
        assertThat(service.buildMacro(parse(json))).isNull();
    }

    @Test
    void macroObbligatoriIncompleti_ritornaNull() throws Exception {
        // Manca il grasso tra gli obbligatori → null
        String json = """
            { "product": { "nutrition": { "aggregated_set": { "per": "100g", "nutrients": {
              "energy-kcal": { "value": 390 }, "proteins": { "value": 13 }, "carbohydrates": { "value": 68 }
            } } } } }
            """;
        assertThat(service.buildMacro(parse(json))).isNull();
    }
}
