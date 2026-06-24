package it.nutrizionista.restnutrizionista.service;

import java.util.Map;
import java.util.Set;

import it.nutrizionista.restnutrizionista.enums.Allergene;

/**
 * Mappa canonica tra gli id della tassonomia OpenFoodFacts ({@code en:*}) e i 14
 * {@link Allergene} UE. Match <strong>esatto</strong> (no {@code contains()}) — vedi piano §2.4.
 */
public final class OffTaxonomyMapping {

    private OffTaxonomyMapping() {}

    /** id canonico OFF → Allergene (con qualche sinonimo comune). */
    private static final Map<String, Allergene> BY_TAG = Map.ofEntries(
            Map.entry("en:gluten", Allergene.GLUTINE),
            Map.entry("en:crustaceans", Allergene.CROSTACEI),
            Map.entry("en:eggs", Allergene.UOVA),
            Map.entry("en:fish", Allergene.PESCE),
            Map.entry("en:peanuts", Allergene.ARACHIDI),
            Map.entry("en:soybeans", Allergene.SOIA),
            Map.entry("en:milk", Allergene.LATTE),
            Map.entry("en:nuts", Allergene.FRUTTA_GUSCIO),
            Map.entry("en:tree-nuts", Allergene.FRUTTA_GUSCIO),
            Map.entry("en:celery", Allergene.SEDANO),
            Map.entry("en:mustard", Allergene.SENAPE),
            Map.entry("en:sesame-seeds", Allergene.SESAMO),
            Map.entry("en:sesame", Allergene.SESAMO),
            Map.entry("en:sulphur-dioxide-and-sulphites", Allergene.SOLFITI),
            Map.entry("en:sulphites", Allergene.SOLFITI),
            Map.entry("en:lupin", Allergene.LUPINI),
            Map.entry("en:molluscs", Allergene.MOLLUSCHI)
    );

    /** Additivi E220–E228 = solfiti (spesso assenti da allergens_tags → vanno recuperati qui). */
    private static final Set<String> SULPHITE_E = Set.of(
            "en:e220", "en:e221", "en:e222", "en:e223", "en:e224",
            "en:e225", "en:e226", "en:e227", "en:e228"
    );

    /** @return l'{@link Allergene} per l'id OFF dato, o {@code null} se non mappato. */
    public static Allergene fromTag(String tag) {
        if (tag == null) return null;
        return BY_TAG.get(tag.trim().toLowerCase());
    }

    /** @return true se l'additivo OFF è un solfito (E220–E228). */
    public static boolean isSulphiteAdditive(String additiveTag) {
        if (additiveTag == null) return false;
        return SULPHITE_E.contains(additiveTag.trim().toLowerCase());
    }
}
