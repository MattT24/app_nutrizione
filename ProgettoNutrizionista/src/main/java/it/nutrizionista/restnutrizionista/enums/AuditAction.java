package it.nutrizionista.restnutrizionista.enums;

/**
 * Tipo di operazione registrata nell'audit log degli accessi ai dati sanitari (A7).
 * Persistita come stringa ({@code @Enumerated(STRING)}, {@code length=32}).
 */
public enum AuditAction {
    /** Lettura di una singola risorsa clinica per id. */
    READ,
    /** Lettura di un elenco di risorse cliniche di un cliente. */
    LIST,
    /** Esportazione/generazione di un PDF di un referto o scheda. */
    EXPORT_PDF,
    /** Download di un documento del fascicolo sanitario. */
    DOWNLOAD,
    /** Condivisione via email di un dato sanitario (il dato esce dal sistema). */
    SHARE,
    /** Cancellazione di un cliente e dei suoi dati (art. 17 GDPR). */
    DELETE,
    /**
     * Override consapevole di un blocco clinico grave ({@code ALERT_GRAVE}): il professionista
     * forza l'inserimento di un alimento segnalato come grave per il paziente (es. allergene
     * dichiarato), assumendosene la responsabilità. È il primo evento di <b>scrittura</b> tracciato
     * (A7 nasce per letture/uscite/DELETE): serve come prova che la decisione è stata presa in modo
     * consapevole (tutela paziente/professionista/azienda). I motivi del blocco sono in {@code dettaglio}.
     */
    OVERRIDE_ALERT_GRAVE,
    /**
     * Accesso generico: usato per le righe con esito {@code DENIED}, dove l'intento del
     * chiamante non è noto (l'ownership fallisce prima che il tipo di operazione sia deciso).
     */
    ACCESS
}
