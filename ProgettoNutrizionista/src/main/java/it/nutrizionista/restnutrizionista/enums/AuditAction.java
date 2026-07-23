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
     * Attivazione della limitazione del trattamento (art. 18 GDPR, A5.3): il titolare marca il
     * cliente come "limitato" (dato conservato ma non più modificabile/producibile/inviabile). È un
     * evento di <b>scrittura amministrativa</b> tracciato: la motivazione va in {@code dettaglio}.
     */
    LIMITAZIONE_ATTIVATA,
    /** Revoca della limitazione del trattamento (A5.3): il cliente torna pienamente operativo. */
    LIMITAZIONE_REVOCATA,
    /**
     * Accesso generico: usato per le righe con esito {@code DENIED}, dove l'intento del
     * chiamante non è noto (l'ownership fallisce prima che il tipo di operazione sia deciso).
     */
    ACCESS,
    /**
     * Messa in quarantena di un cliente da parte del job di retention (A6, art. 5(1)(e)): il cliente
     * non ha attività clinica da oltre il termine di conservazione e viene marcato per la successiva
     * cancellazione fisica (dopo la grace window). Scrittura di sistema tracciata (attore null: nessun
     * utente loggato); il motivo/decorrenza va in {@code dettaglio}.
     */
    RETENTION_QUARANTENA,
    /**
     * Cancellazione fisica di un cliente da parte del job di retention (A6, art. 5(1)(e)), allo
     * scadere della grace window di quarantena. È il gemello "di sistema" di {@link #DELETE}
     * (art. 17, azione del titolare): entrambi passano dallo stesso metodo canonico di cancellazione.
     */
    RETENTION_PURGE
}
