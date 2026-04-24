package it.nutrizionista.restnutrizionista.enums;

public enum TagStandard {
    // ── Allergie Reg. UE 1169/2011 ──
    ALL_GLUTINE,
    ALL_CROSTACEI,
    ALL_UOVA,
    ALL_PESCE,
    ALL_ARACHIDI,
    ALL_SOIA,
    ALL_LATTE,
    ALL_FRUTTA_GUSCIO,

    // ── Stili e Scelte ──
    STILE_VEGETARIANO,
    STILE_VEGANO,
    STILE_PESCATARIANO,
    REL_HALAL,
    REL_KOSHER,

    // ── Patologie e Fisiologia (Trigger del Rule Engine) ──
    PAT_IPERTENSIONE,
    PAT_INSUFF_RENALE,
    PAT_SINDROME_IBS,
    PAT_REFLUSSO,
    FISIO_GRAVIDANZA,
    FISIO_DISFAGIA,
    
    // ── Interazioni Farmacologiche ──
    FARM_ANTICOAGULANTI,
    FARM_STATINE,

    // ── Intolleranze Sistemiche (WARNING nel Rule Engine) ──
    INT_LATTOSIO,
    INT_GLUTINE_NCGS,
    INT_ISTAMINA
}
