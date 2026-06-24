package it.nutrizionista.restnutrizionista.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import it.nutrizionista.restnutrizionista.enums.Allergene;

/** Unit test della mappa canonica OFF (en:*) → Allergene e del riconoscimento solfiti. */
class OffTaxonomyMappingTest {

    @Test
    void mappaI14AllergeniCanonici() {
        assertThat(OffTaxonomyMapping.fromTag("en:gluten")).isEqualTo(Allergene.GLUTINE);
        assertThat(OffTaxonomyMapping.fromTag("en:milk")).isEqualTo(Allergene.LATTE);
        assertThat(OffTaxonomyMapping.fromTag("en:nuts")).isEqualTo(Allergene.FRUTTA_GUSCIO);
        assertThat(OffTaxonomyMapping.fromTag("en:soybeans")).isEqualTo(Allergene.SOIA);
        assertThat(OffTaxonomyMapping.fromTag("en:molluscs")).isEqualTo(Allergene.MOLLUSCHI);
        assertThat(OffTaxonomyMapping.fromTag("en:sulphur-dioxide-and-sulphites")).isEqualTo(Allergene.SOLFITI);
    }

    @Test
    void riconosceSinonimiECaseInsensitive() {
        assertThat(OffTaxonomyMapping.fromTag("en:tree-nuts")).isEqualTo(Allergene.FRUTTA_GUSCIO);
        assertThat(OffTaxonomyMapping.fromTag("EN:MILK")).isEqualTo(Allergene.LATTE);
        assertThat(OffTaxonomyMapping.fromTag("  en:eggs  ")).isEqualTo(Allergene.UOVA);
    }

    @Test
    void tagSconosciutoOrNull_ritornaNull() {
        assertThat(OffTaxonomyMapping.fromTag("en:water")).isNull();
        assertThat(OffTaxonomyMapping.fromTag(null)).isNull();
    }

    @Test
    void riconosceSolfitiE220_E228() {
        assertThat(OffTaxonomyMapping.isSulphiteAdditive("en:e220")).isTrue();
        assertThat(OffTaxonomyMapping.isSulphiteAdditive("en:e228")).isTrue();
        assertThat(OffTaxonomyMapping.isSulphiteAdditive("EN:E224")).isTrue();
        assertThat(OffTaxonomyMapping.isSulphiteAdditive("en:e300")).isFalse();
        assertThat(OffTaxonomyMapping.isSulphiteAdditive(null)).isFalse();
    }
}
