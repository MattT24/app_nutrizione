# LEGAL-REVIEW — Campi/decisioni provvisori da confermare col legale

> **Documento vivo (in accumulo).** Statera tratta **dati sanitari (art. 9 GDPR)**. Durante la remediation
> abbiamo adottato **interpretazioni legali provvisorie** (in configurazione, mai hardcoded come definitive):
> qui sono raccolte per la **revisione legale** prima della produzione.
>
> **✅ STATO: LISTA COMPLETA — pronta per la consegna al legale (2026-07-22).** Tutti i meccanismi Fase 3 (A5.3,
> A6 retention/erasure, A7 audit, A4/A9, F-USER-DEL) sono implementati e verificati; le interpretazioni/durate
> provvisorie sono raccolte in §1-§13. §11 (AI Act) e §12 (break-glass, Keycloak) restano *future* e non bloccano
> la consegna. ⚠️ Nulla viene cancellato automaticamente: la retention gira in **dry-run (purge OFF)** finché le
> durate non sono confermate col legale. Prima versione: 2026-07-20.
>
> Formato per item: **Decisione provvisoria adottata** · **Quesito per il legale** · **Rif.**

---

## 1. Limitazione del trattamento (art. 18) — confine "bloccato vs consentito"
- **Decisione provvisoria:** durante la limitazione si **bloccano** le operazioni che *scrivono/producono/inviano*
  dati del cliente (scritture cliniche, produzione/persistenza PDF, tutti gli share, appuntamenti che agganciano
  il cliente); si **consentono** letture/liste, export-PDF transitorio per il titolare, download, revoca, e la
  **cancellazione art. 17** (`deleteMyCliente`). **HTTP 423 Locked** quando bloccato.
- **Incluso (decisione 2026-07-20):** l'**annullamento di un appuntamento** (`delete` → stato `ANNULLATO`, che è
  un **soft-delete**: la riga sopravvive coi dati del paziente) è **BLOCCATO** — trattato come *scrittura*, NON
  come l'erasure dell'art. 17.
- **Quesito legale:** il confine adottato è corretto? In particolare, annullare un appuntamento (soft-delete) di
  un cliente limitato è legittimamente da bloccare, o è un atto di *riduzione/ritiro* consentito?
- **Rif.:** `LimitazioneTrattamentoValidator`, `AppuntamentoService`; CLAUDE.md §A5.3.

## 2. Retention / storage limitation (art. 5(1)(e), art. 17) — meccanismo A6 IMPLEMENTATO+verificato (2026-07-22)
- **Decisione provvisoria:** dati **clinici** conservati **10 anni dall'ultima attività clinica** (ancoraggio
  giuridico: prescrizione decennale **art. 2946 c.c.**); **quarantena 90 giorni** prima della cancellazione;
  **legal-hold** generico (art. 17(3): contenzioso/obbligo) che sospende la cancellazione.
- **Quesito legale:** le durate per categoria di dato sanitario sono corrette? La quarantena (grace period) è
  adeguata? Il legal-hold copre le eccezioni giuste?
- **Durate as-built (da confermare col legale prima di attivare il purge):** clinico **10 anni dall'ultimo
  contatto clinico**, quarantena (grace) **90 giorni**, legal-hold generico. Il meccanismo A6 è implementato e
  verificato ma gira in **`dry-run` (purge OFF)** di default: **nessuna cancellazione automatica** avviene finché
  queste durate non sono confermate e il purge non è attivato esplicitamente dopo review.

## 3. Audit log — retention & conservazione post-erasure (A7)
- **Decisione provvisoria:** log accessi ai dati sanitari conservato **≥24 mesi**; `AuditLog.clienteId`
  **preservato** anche dopo la cancellazione del cliente (obbligo legale **art. 17(3)(b)**).
- **Quesito legale:** durata minima corretta? La conservazione dell'id cliente nel log dopo l'erasure è
  giustificata (obbligo legale) o va anonimizzata?
- **Rif.:** `AuditLog`, `AuditLogCleanupScheduler`.

## 4. Base giuridica dell'account & consenso paziente (art. 6/9)
- **Decisione provvisoria:** base giuridica dell'account **B2B = contratto (art. 6(1)(b))**, NON "consenso"
  (presa visione/accettazione di Privacy/Termini/DPA alla registrazione). Il **consenso art. 9 dei pazienti** è
  gestito **offline** (i pazienti non usano l'app) e **non è modellato** nel software.
- **Quesito legale:** l'inquadramento contratto-vs-consenso è corretto? Il consenso del paziente al trattamento
  dei dati art. 9 va raccolto/registrato e, se sì, come (e chi è responsabile)?
- **Rif.:** `AccettazioneDocumento`, `AuthService.register`; CLAUDE.md §A4/A9.

## 5. Testi legali & informative (E3 / A4 / A9)
- **Stato:** il **meccanismo** di accettazione versionata esiste; mancano i **testi reali**: Privacy Policy,
  Termini di Servizio, Cookie Policy, **DPA**, informativa art. 13/14.
- **Quesito legale:** redazione dei documenti + versioning; cosa deve accettare il nutrizionista alla
  registrazione e al bump di versione.

## 6. DPA con i sub-responsabili (C2)
- **Sub-responsabili:** **Clever Cloud (hosting, UE — deciso 2026-07-22; verificare certificazione HDS + ISO 27001 e DPA/scope)**, TiDB Cloud (UE), Google/Gmail SMTP, Google Identity Services, OpenFoodFacts (no PII); (fase Keycloak: l'IdP self-hosted su Clever Cloud UE non aggiunge un terzo).
- **Quesito legale:** quali **DPA** vanno stipulati/verificati? (Gmail *consumer* per PDF sanitari è il punto
  più delicato → previsto passaggio a provider transazionale con DPA.)

## 7. Qualificazione MDR / destinazione d'uso (D1)
- **Decisione provvisoria/posizionamento:** il motore di alert clinici (`ALERT_GRAVE`, tag `PAT_*`) è un
  **aiuto alla compilazione**; ogni blocco grave è **superabile con conferma consapevole auditata** → la
  decisione resta del professionista (posizionamento anti-dispositivo-medico). Disclaimer anti-MDR in UI/PDF/email.
- **Quesito legale:** serve un **documento di destinazione d'uso (intended use)** formale? Il posizionamento
  evita la qualificazione come dispositivo medico (MDR)?
- **Rif.:** CLAUDE.md §D1/D2.

## 8. Cifratura at-rest dei dati art. 9 (A2, art. 32)
- **Stato:** at-rest oggi demandato al layer **TiDB Cloud** (KMS del cloud); nessuna cifratura di campo
  (`@Convert`) nell'app.
- **Quesito legale/DPO:** l'at-rest gestito da TiDB è **sufficiente** per il threat model dei dati sanitari, o
  serve cifratura field-level (difesa in profondità contro l'accesso al dump)?

## 9. Cookie / consenso trasferimenti a terzi (E2)
- **Stato:** font/icone self-hostati; resta il caricamento di **Google Identity Services** (`accounts.google.com`)
  inevitabile finché c'è il login Google → IP dell'utente a terzi.
- **Quesito legale:** serve un **cookie/consent banner (CMP)** e/o copertura in informativa per GSI?

## 10. Ripartizione ruoli GDPR (titolare / responsabile)
- **Assunzione:** il nutrizionista/clinica è **titolare**; Statera è **responsabile del trattamento**.
- **Quesito legale:** la ripartizione è corretta e va **documentata contrattualmente** (DPA tra Statera e il
  singolo nutrizionista)?

## 11. AI Act (D3) — futuro
- **Nota:** nessuna feature IA oggi. **Checkpoint obbligatorio**: valutazione AI Act (rischio) **prima** di
  sviluppare qualunque feature IA che elabori dati sanitari.

## 12. Break-glass / impersonation (fase Keycloak) — accesso operatore ai dati art. 9
- **Decisione provvisoria:** l'operatore di piattaforma (SUPER_ADMIN) **non** ha accesso ambientale ai dati
  clinici; vi accede solo via **impersonation esplicita, time-boxed, auditata** (oggi solo demo; il break-glass
  sui nutrizionisti reali è deferito a Keycloak).
- **Quesito legale:** base giuridica dell'accesso di supporto ai dati art. 9 in impersonation; **notifica** al
  soggetto impersonato; requisito di **attribuzione dell'azione all'operatore reale** nell'audit.

---

## 13. Conservazione della prova di presa-visione all'erasure dell'account (art. 17(3) vs art. 6(1)(b))
- **Contesto:** `AccettazioneDocumento` registra l'accettazione di Privacy/ToS/DPA del nutrizionista alla
  registrazione (prova della base giuridica art. 6(1)(b) + informativa). Alla cancellazione dell'account
  (art. 17, `UtenteService.deleteAccount`) oggi viene **cancellata** con gli altri dati utente (scelta
  provvisoria implementata + testata).
- **Quesito legale:** la prova di presa-visione va **conservata** (denormalizzata senza FK, come l'`AuditLog`)
  per un periodo di prescrizione — sotto art. 17(3)(e) (difesa in giudizio) / (b) (obbligo) — per poter
  dimostrare che i documenti erano stati accettati anche dopo la chiusura dell'account? Oppure la cancellazione
  integrale è corretta (contratto cessato → nessuna finalità residua)? (Precauzionalmente Code propende per
  **conservare**, coerente con come si tratta l'`AuditLog`.)
- **Impatto tecnico se 'conservare':** denormalizzare `AccettazioneDocumento` (snapshot utente/email, niente FK),
  escluderla da `eliminaFigliNonCascadeUtente`, aggiornare il guard-test. **Non urgente:** la cancellazione
  account non è oggi raggiungibile (nessun ruolo ha `UTENTE_DELETE`) → decidere col legale prima di esporre la feature.

---

### Stato di completezza — ✅ LISTA COMPLETA per la consegna (2026-07-22)
Tutti i meccanismi lato-codice della Fase 3 (A5.3 limitazione, A6 retention/erasure, A7 audit, A4/A9 accettazioni,
F-USER-DEL) sono implementati e verificati; le relative **interpretazioni/durate provvisorie** sono qui raccolte
(§1-§13). **La lista è pronta per la consegna al legale.** Restano di natura *futura* (non bloccano la consegna):
§11 (AI Act, nessuna feature IA oggi) e §12 (break-glass, fase Keycloak) — verranno raffinati quando quelle fasi
partiranno. Eventuale **SLA sui clienti in hold** (coorte senza scadenza) = raffinamento di §2 col titolare.
