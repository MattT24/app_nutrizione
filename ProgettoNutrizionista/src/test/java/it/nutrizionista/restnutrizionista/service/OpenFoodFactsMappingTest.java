package it.nutrizionista.restnutrizionista.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.nutrizionista.restnutrizionista.dto.AlimentoBaseFormDto;
import it.nutrizionista.restnutrizionista.dto.OffProductDto;

/**
 * Unit del mapping deterministico OFF → {@link AlimentoBaseFormDto}
 * ({@code OpenFoodFactsService.mapToFormDto}, PR-2 §2.4). Pura logica su payload sintetici:
 * nessun HTTP/DB (RestTemplate/AlimentoBaseService non sono usati dal mapping).
 */
class OpenFoodFactsMappingTest {

    private final OpenFoodFactsService service = new OpenFoodFactsService();

    /** Prodotto "sano": completezza alta + 1 allergene dichiarato → isolano il contributo
     *  di eco-score/serving/warnings/vegetariano senza far scattare needsReview per altri motivi. */
    private OffProductDto.Product baseProduct() {
        OffProductDto.Product p = new OffProductDto.Product();
        p.setProductName("Prodotto Test");
        p.setCompleteness(1.0);
        p.setAllergensTags(List.of("en:milk"));
        return p;
    }

    private AlimentoBaseFormDto map(OffProductDto.Product p) {
        return service.mapToFormDto(p, "123", null);
    }

    // ── #1 Eco-score: ecoscore_grade ha precedenza su environmental_score_grade ──

    @Test
    void ecoscoreGrade_haPrecedenzaSuEnvironmental() {
        OffProductDto.Product p = baseProduct();
        p.setEcoscoreGrade("a");
        p.setEnvironmentalScoreGrade("e");
        assertThat(map(p).getEnvironmentalScoreGrade()).isEqualTo("a");
    }

    @Test
    void soloEcoscoreGrade_valorizzato() {
        OffProductDto.Product p = baseProduct();
        p.setEcoscoreGrade("c");
        assertThat(map(p).getEnvironmentalScoreGrade()).isEqualTo("c");
    }

    // ── #2 serving_quantity assente → default 100 ──

    @Test
    void servingQuantityAssente_default100() {
        OffProductDto.Product p = baseProduct();
        p.setServingQuantity(null);
        assertThat(map(p).getServingQuantityG()).isEqualTo(100.0);
    }

    @Test
    void servingQuantityPresente_preservato() {
        OffProductDto.Product p = baseProduct();
        p.setServingQuantity(30.0);
        assertThat(map(p).getServingQuantityG()).isEqualTo(30.0);
    }

    // ── #5 needsReview include data_quality_warnings_tags ──

    @Test
    void dataQualityWarnings_needsReviewTrue() {
        OffProductDto.Product p = baseProduct(); // completeness 1.0, allergeni presenti, nessun error
        p.setDataQualityWarningsTags(List.of("en:nutrition-value-suspicious"));
        assertThat(map(p).getNeedsReview()).isTrue();
    }

    @Test
    void nessunProblemaQualita_needsReviewFalse() {
        assertThat(map(baseProduct()).getNeedsReview()).isFalse();
    }

    // ── #6 vegetariano da ingredients_analysis_tags ──

    @Test
    void tagVegetarian_valorizzaFlag() {
        OffProductDto.Product p = baseProduct();
        p.setIngredientsAnalysisTags(List.of("en:vegetarian"));
        assertThat(map(p).getVegetariano()).isTrue();
    }

    @Test
    void vegano_implicaVegetariano() {
        OffProductDto.Product p = baseProduct();
        p.setIngredientsAnalysisTags(List.of("en:vegan"));
        AlimentoBaseFormDto form = map(p);
        assertThat(form.getVegano()).isTrue();
        assertThat(form.getVegetariano()).isTrue();
    }

    // ── PR-4: marca da brands (human-readable) con fallback brands_tags ──

    @Test
    void marca_daBrandsPrimoToken() {
        OffProductDto.Product p = baseProduct();
        p.setBrands("Ferrero, Nutella");
        assertThat(map(p).getMarca()).isEqualTo("Ferrero");
    }

    @Test
    void marca_fallbackBrandsTags_quandoBrandsAssente() {
        OffProductDto.Product p = baseProduct();
        p.setBrands(null);
        p.setBrandsTags(List.of("ferrero", "altro"));
        assertThat(map(p).getMarca()).isEqualTo("ferrero");
    }

    @Test
    void marca_null_senzaBrand() {
        // Scenario CREA/manuale: né brands né brands_tags → marca null.
        assertThat(map(baseProduct()).getMarca()).isNull();
    }
}
