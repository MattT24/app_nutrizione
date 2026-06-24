package it.nutrizionista.restnutrizionista.engine;

import java.util.EnumMap;
import java.util.Map;

import it.nutrizionista.restnutrizionista.enums.Allergene;
import it.nutrizionista.restnutrizionista.enums.TagStandard;

/**
 * Mappa statica 1:1 tra i {@link TagStandard} {@code ALL_*} del paziente e i 14 {@link Allergene}
 * dell'alimento, più le label IT per i messaggi clinici. Usata da {@link AllergeneRule}.
 */
public final class TagStandardAllergeneMapping {

    private TagStandardAllergeneMapping() {}

    private static final Map<TagStandard, Allergene> ALLERGENE_BY_TAG = new EnumMap<>(TagStandard.class);
    private static final Map<Allergene, String> LABEL = new EnumMap<>(Allergene.class);

    static {
        ALLERGENE_BY_TAG.put(TagStandard.ALL_GLUTINE, Allergene.GLUTINE);
        ALLERGENE_BY_TAG.put(TagStandard.ALL_CROSTACEI, Allergene.CROSTACEI);
        ALLERGENE_BY_TAG.put(TagStandard.ALL_UOVA, Allergene.UOVA);
        ALLERGENE_BY_TAG.put(TagStandard.ALL_PESCE, Allergene.PESCE);
        ALLERGENE_BY_TAG.put(TagStandard.ALL_ARACHIDI, Allergene.ARACHIDI);
        ALLERGENE_BY_TAG.put(TagStandard.ALL_SOIA, Allergene.SOIA);
        ALLERGENE_BY_TAG.put(TagStandard.ALL_LATTE, Allergene.LATTE);
        ALLERGENE_BY_TAG.put(TagStandard.ALL_FRUTTA_GUSCIO, Allergene.FRUTTA_GUSCIO);
        ALLERGENE_BY_TAG.put(TagStandard.ALL_SEDANO, Allergene.SEDANO);
        ALLERGENE_BY_TAG.put(TagStandard.ALL_SENAPE, Allergene.SENAPE);
        ALLERGENE_BY_TAG.put(TagStandard.ALL_SESAMO, Allergene.SESAMO);
        ALLERGENE_BY_TAG.put(TagStandard.ALL_SOLFITI, Allergene.SOLFITI);
        ALLERGENE_BY_TAG.put(TagStandard.ALL_LUPINI, Allergene.LUPINI);
        ALLERGENE_BY_TAG.put(TagStandard.ALL_MOLLUSCHI, Allergene.MOLLUSCHI);

        LABEL.put(Allergene.GLUTINE, "glutine");
        LABEL.put(Allergene.CROSTACEI, "crostacei");
        LABEL.put(Allergene.UOVA, "uova");
        LABEL.put(Allergene.PESCE, "pesce");
        LABEL.put(Allergene.ARACHIDI, "arachidi");
        LABEL.put(Allergene.SOIA, "soia");
        LABEL.put(Allergene.LATTE, "latte");
        LABEL.put(Allergene.FRUTTA_GUSCIO, "frutta a guscio");
        LABEL.put(Allergene.SEDANO, "sedano");
        LABEL.put(Allergene.SENAPE, "senape");
        LABEL.put(Allergene.SESAMO, "sesamo");
        LABEL.put(Allergene.SOLFITI, "solfiti");
        LABEL.put(Allergene.LUPINI, "lupini");
        LABEL.put(Allergene.MOLLUSCHI, "molluschi");
    }

    /** @return l'{@link Allergene} associato al tag {@code ALL_*}, o {@code null} per gli altri tag. */
    public static Allergene allergeneFor(TagStandard tag) {
        return tag == null ? null : ALLERGENE_BY_TAG.get(tag);
    }

    /** Label IT minuscola per i messaggi clinici (es. "frutta a guscio"). */
    public static String label(Allergene allergene) {
        String l = LABEL.get(allergene);
        return l != null ? l : (allergene != null ? allergene.name().toLowerCase() : "");
    }
}
