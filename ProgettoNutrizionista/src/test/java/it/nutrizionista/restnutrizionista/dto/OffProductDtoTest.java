package it.nutrizionista.restnutrizionista.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Verifica la deserializzazione dell'envelope OFF v3.6 ({@code status} stringa, {@code product},
 * campi estesi e @JsonProperty), incluso {@code ecoscore_grade} (legacy) e l'ignore dei campi sconosciuti.
 */
class OffProductDtoTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void deserializzaEnvelopeV3ConCampiEstesi() throws Exception {
        String json = """
            {
              "status": "success_with_warnings",
              "result": { "id": "product_found", "name": "Product found", "lc_name": "Prodotto trovato" },
              "warnings": [ { "field": { "id": "x" } } ],
              "product": {
                "code": "3017620422003",
                "product_name": "Nutella",
                "product_name_it": "Nutella",
                "brands": "Ferrero",
                "allergens_tags": ["en:milk", "en:nuts", "en:soybeans"],
                "traces_tags": ["en:peanuts"],
                "additives_tags": ["en:e322"],
                "ingredients_analysis_tags": ["en:non-vegan", "en:vegetarian"],
                "nutriscore_grade": "e",
                "nova_group": 4,
                "ecoscore_grade": "d",
                "nutrient_levels": { "fat": "high", "salt": "low" },
                "sources": [ { "id": "org-gs1", "manufacturer": "1", "fields": ["product_name"] } ],
                "completeness": 0.79,
                "campo_sconosciuto_da_ignorare": 123,
                "nutriments": {
                  "energy-kcal_100g": 539.0,
                  "energy-kj_100g": 2252.0,
                  "proteins_100g": 6.3,
                  "carbohydrates_100g": 57.5,
                  "fat_100g": 30.9,
                  "salt_100g": 0.107,
                  "sodium_100g": 0.0428
                }
              }
            }
            """;

        OffProductDto dto = om.readValue(json, OffProductDto.class);

        assertThat(dto.getStatus()).isEqualTo("success_with_warnings");
        assertThat(dto.getResult().getLcName()).isEqualTo("Prodotto trovato");

        OffProductDto.Product p = dto.getProduct();
        assertThat(p).isNotNull();
        assertThat(p.getCode()).isEqualTo("3017620422003");
        assertThat(p.getAllergensTags()).containsExactly("en:milk", "en:nuts", "en:soybeans");
        assertThat(p.getTracesTags()).containsExactly("en:peanuts");
        assertThat(p.getAdditivesTags()).containsExactly("en:e322");
        assertThat(p.getIngredientsAnalysisTags()).contains("en:non-vegan", "en:vegetarian");
        assertThat(p.getNutriscoreGrade()).isEqualTo("e");
        assertThat(p.getNovaGroup()).isEqualTo(4);
        assertThat(p.getEcoscoreGrade()).isEqualTo("d");           // legacy (E.6)
        assertThat(p.getNutrientLevels()).containsEntry("fat", "high").containsEntry("salt", "low");
        assertThat(p.getCompleteness()).isEqualTo(0.79);
        assertThat(p.getSources()).hasSize(1);
        assertThat(p.getSources().get(0).getManufacturer()).isEqualTo("1");

        OffProductDto.Nutriments n = p.getNutriments();
        assertThat(n.getEnergyKcal100g()).isEqualTo(539.0);
        assertThat(n.getEnergyKj100g()).isEqualTo(2252.0);
        assertThat(n.getSalt100g()).isEqualTo(0.107);
        assertThat(n.getSodium100g()).isEqualTo(0.0428);
    }

    @Test
    void deserializzaEnvelopeV2_statusInteroCoercitoAString() throws Exception {
        // Il product API v2 (usato per l'import: la v3.6 ritorna nutriments vuoto) manda status=1 (intero).
        // Jackson lo coerce nel campo String → "1"; il controllo "trovato" (status != "failure") resta valido.
        String json = """
            {
              "status": 1,
              "status_verbose": "product found",
              "product": {
                "code": "8017596064349",
                "product_name": "Fette biscottate integrali",
                "allergens_tags": ["en:gluten"],
                "nutriscore_grade": "c",
                "nova_group": 3,
                "ecoscore_grade": "a-plus",
                "nutriments": { "energy-kcal_100g": 390, "proteins_100g": 13, "carbohydrates_100g": 68, "fat_100g": 5.8, "salt_100g": 1.5 }
              }
            }
            """;
        OffProductDto dto = om.readValue(json, OffProductDto.class);
        assertThat(dto.getStatus()).isEqualTo("1");
        assertThat("failure".equalsIgnoreCase(dto.getStatus())).isFalse();
        assertThat(dto.getProduct()).isNotNull();
        assertThat(dto.getProduct().getNutriments().getEnergyKcal100g()).isEqualTo(390.0);
        assertThat(dto.getProduct().getAllergensTags()).containsExactly("en:gluten");
        assertThat(dto.getProduct().getEcoscoreGrade()).isEqualTo("a-plus");
    }

    @Test
    void productAssente_quandoNonTrovato() throws Exception {
        String json = """
            { "status": "failure", "result": { "id": "product_not_found" }, "errors": [ {} ] }
            """;
        OffProductDto dto = om.readValue(json, OffProductDto.class);
        assertThat(dto.getStatus()).isEqualTo("failure");
        assertThat(dto.getProduct()).isNull();
    }
}
