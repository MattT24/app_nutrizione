package it.nutrizionista.restnutrizionista.enums;

/**
 * Provenienza dell'informazione allergeni di un {@code AlimentoBase}, per tracciabilità
 * e affidabilità del dato (badge "non verificato"/confidenza nel FE).
 *
 * <ul>
 *   <li>{@link #OFF_DICHIARATO}     — dichiarato dal produttore (OFF import produttore: {@code sources[].manufacturer="1"} / {@code sources_fields.org-*}).</li>
 *   <li>{@link #OFF_INGREDIENTI}    — dedotto dal parsing ingredienti OFF.</li>
 *   <li>{@link #OFF_ADDITIVI}       — dedotto dagli additivi OFF (es. E220–E228 → SOLFITI).</li>
 *   <li>{@link #OFF_DERIVATO}       — best-effort OFF senza provenienza certa (fallback quando manca un import produttore — vedi piano E.5).</li>
 *   <li>{@link #CREA_AUTO}          — assegnato dalla pipeline deterministica CREA.</li>
 *   <li>{@link #MANUALE_VERIFICATO} — confermato a mano dal nutrizionista.</li>
 * </ul>
 */
public enum FonteAllergene {
    OFF_DICHIARATO,
    OFF_INGREDIENTI,
    OFF_ADDITIVI,
    OFF_DERIVATO,
    CREA_AUTO,
    MANUALE_VERIFICATO
}
