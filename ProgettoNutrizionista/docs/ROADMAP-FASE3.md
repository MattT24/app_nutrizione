# ROADMAP-FASE3 — Ordine di programmazione post quick-win (Statera)

> **Documento di pianificazione, non codice.** Cattura le tre Wave di lavoro post-Fase 2, il
> registro delle decisioni prese e le domande aperte che sbloccano Wave 2/3.
>
> **Data**: 2026-07-12
> **Riferimenti**: `docs/COMPLIANCE-STATUS.md` (matrice A–E + checklist "Prima della produzione"),
> `docs/AUTH-ASSESSMENT.md` (opzioni auth). Convenzioni operative in `CLAUDE.md`.

## Stato

- **Fase 1 (analisi)**: completa — gap analysis A–E + assessment auth.
- **Fase 2 (10 quick win + CLAUDE.md)**: completa e validata — backend 91 test verdi, frontend
  124 test verdi, **0 vulnerabilità in produzione**.
- **Già chiuso in Fase 2** (non rientra nella roadmap): B4 `.env`/spring-dotenv · A8 `/uploads`
  ristretto + handler errori generico + email fuori dai log · A1 IDOR principali + test
  anti-leakage · E4b destinatario email vincolato al cliente · D2 disclaimer anti-MDR ·
  E2 self-host font/icone · B5 `npm audit` + upgrade Angular a **21.2.18** (entro v21).
- **Fase 3 Wave 1**: completa e validata (A5.1, A1 residuo, ambienti+profili+CORS, CI+Dependabot).
- **Fase 3 Wave 2**: **A7 (audit log) completato** — backend **106 test verdi**. Prossimi candidati
  Wave 2: **A4/A9 FATTO**, **D1 FATTO**, **F-D1a FATTO (BE+FE, e2e #1-#4)**, **F-OWN-SWEEP FATTO (IDOR, chiude F-D1b;
  148 test verdi)**, **F-DEL-CASCADE FATTO (delete cliente sistematico + anonimizzazione EventoGamification + regression-guard)**,
  **A5.3 FATTO (limitazione del trattamento art. 18 — `LimitazioneTrattamentoValidator` → 423, 163 test verdi)**;
  restano il finding derivato dual-FK `AlimentoAlternativo`, A6 retention (attende durate), D4, E2 cookie banner.

## Criterio di ordinamento

(1) dipendenze/blocchi tra item; (2) rischio sui dati sanitari (art. 9); (3) sforzo;
(4) se richiede una **decisione non-codice** dell'utente (hosting, legale, auth, retention) →
in coda finché la decisione non c'è.

---

## Registro decisioni (prese in Fase 3, definitive salvo revisione)

| Tema | Decisione | Note |
|---|---|---|
| **Auth (target)** | **Opzione B — Keycloak**, *dopo* la scelta hosting | Nessun lavoro auth in-house ora. MFA/reset/lockout/audit-login/social arrivano dall'IdP. Fallback = Opzione A (Spring Session + Redis). Opzione C (JWT custom) scartata. |
| **B1 `jwt.secret`** | **Rimandato** — si tiene il valore di test | Rotazione banale quando serve (meccanismo `.env` già pronto da QW-3). Invaliderà i 2 token di test → impatto nullo. |
| **Hosting** | **Da decidere** — valutazione costi in corso | È il perno che sblocca A3/B7/A2/config-prod e l'hosting di Keycloak. Confronto sotto (§Hosting). |
| **Retention** | **In attesa del titolare** | Servono le durate di conservazione per categoria di dato prima di scrivere codice (A6). |
| **Legale** | **In attesa** | I *testi* (Privacy/ToS/consenso/DPA) richiedono supporto legale; il *meccanismo* consenso è codice, anticipabile (vedi Wave 2). |
| **CI + Dependabot** | **Approvato — in Wave 1** | Guardrail anti-regressione; avrebbe intercettato da solo le high di Angular. |
| **A7 — meccanismo audit** | **Hook espliciti a livello service** (non AOP) | Scelto in Wave 2 al posto della formulazione originale "AOP/@Aspect". Motivo: l'unico choke-point centralizzato (`OwnershipValidator.getOwned*`) ha granularità sbagliata (invocato ~90× per read/write/delete → doppio-log, nessuna `action` semantica); l'AOP realistico sarebbe comunque un'annotazione per-endpoint (dimenticabile come l'esplicito) ma con dipendenza `spring-aop`, ThreadLocal e pitfall di proxy. L'esplicito dà semantica corretta, zero dipendenze, sync/async per-evento. Copertura garantita da regola in CLAUDE.md + test. |
| **A7 — eventi critici** | **Scrittura sincrona bloccante** (SHARE/EXPORT/DOWNLOAD in `REQUIRES_NEW` prima del side-effect; DELETE stessa tx) | "No log ⇒ no disclosure". Ha richiesto di rendere **sincrono** `EmailService.sendPdfEmail` (era `@Async`) per audit accurato dell'invio dei PDF sanitari (SUCCESS pre-invio + FAILURE su errore). Le altre email (appuntamenti/gamification) restano `@Async`. |
| **A7 — IP / X-Forwarded-For** | **`getRemoteAddr()` ora; `X-Forwarded-For` con il reverse proxy (A3)** | Dietro il reverse proxy prod (same-origin) `getRemoteAddr()` restituirebbe l'IP del proxy: abilitare `server.forward-headers-strategy=framework` (o `NATIVE`) **insieme** al reverse proxy in A3. NON attivarlo prima (senza proxy fidato = spoofing di `X-Forwarded-For`). |

---

## Wave 1 — ORA (codice, sbloccato, nessuna decisione esterna)

Blocco coeso "deploy-readiness + hardening". Tutto testabile in locale. **✅ COMPLETATA e validata** (backend 92 test, frontend 126 test, build puliti; annotazioni CLAUDE.md fatte).

1. **A5.1 — Cancellazione cliente completa.** `ClienteService.deleteMyCliente` non rimuove i
   `DocumentoFascicolo` né i file su disco → orfani / possibile violazione FK. Riusare
   `FascicoloService` (con ownership). Bug dati reale, piccolo.
2. **A1 residuo — Ownership uniformata (B3).** Allineare gli ultimi check "artigianali"
   (es. `AppuntamentoService.verificaProprietario` con `ResponseStatusException`) al validator
   canonico `OwnershipValidator`. Comportamento invariato, solo un pattern in tutto il codice.
3. **Ambienti + profili + CORS parametrizzato.** Frontend: `environments/environment(.prod).ts`
   con `apiBaseUrl`, sostituire gli ~20 `http://localhost:8080` hardcoded, `fileReplacements` in
   `angular.json`. Backend: `application-dev/prod.properties` (prod: `show-sql=false`, valutare
   `ddl-auto=validate`). CORS: un unico `CorsConfigurationSource` con origins da
   `app.cors.allowed-origins`, al posto dei 36 `@CrossOrigin` hardcoded.
   **Solo il meccanismo env-driven; i valori di produzione restano vuoti** (dipendono dall'hosting).
   **⚠️ Vincolo di topologia prod (deciso in Wave 1):** prod = frontend+backend **same-origin**
   (`apiBaseUrl=''` → chiamate relative `/api/...`). Il CORS è quindi **dev-only** (solo
   `localhost:4200 → localhost:8080`); in prod nessuna richiesta cross-origin. Ne consegue che il
   deploy prod **richiede un reverse proxy** (stesso host) che serva Angular e instradi `/api/**`
   (+ `/uploads/loghi/**`) al backend. **Non** due domini separati. → si salda con **A3 (TLS/hosting)**.
   Annotato anche in `CLAUDE.md` (sez. Note Operative → CORS).
4. **B5 — CI + Dependabot.** GitHub Actions sui due repo (backend `mvnw test`; frontend
   `npm ci` + build + test + `npm audit --omit=dev`). `dependabot.yml` (npm + maven, settimanale,
   grouped).

**Fuori da Wave 1**: B1 (rimandato); qualsiasi valore di produzione (dominio, segreti reali).

**Verifica Wave 1**: `mvnw test` + `npm run build`/test verdi; avvio profilo dev funzionante;
cancellazione cliente rimuove documenti + file; CI verde sui due repo; aggiornare `CLAUDE.md` e
spuntare gli item in `COMPLIANCE-STATUS.md`.

---

## Wave 2 — PROSSIMO (codice sostanzioso; micro-decisione o design dedicato)

- **A7 — Audit log accessi ai dati sanitari (P0 sanitari). ✅ FATTO.** Entità `AuditLog` immutabile
  append-only (chi/cosa/quando/da dove/paziente/tipo/esito), popolata con **hook espliciti a livello
  service** (NON AOP — vedi Registro decisioni); eventi critici SHARE/EXPORT_PDF/DOWNLOAD sincroni in
  tx propria ("no log ⇒ no disclosure") + FAILURE su invio non riuscito, DELETE nella stessa tx,
  letture/liste async, accessi NEGATI in `OwnershipValidator`. Endpoint `/api/audit` (`AUDIT_READ`
  admin) + `/api/audit/me` (`AUDIT_READ_OWN` self, tenant forzato). Retention scheduler ≥24 mesi
  (floor), purge off di default. Migrazione `013_audit_log.sql`. **Indipendente dalla scelta auth**.
  Requisito Garante. *Rimandati:* alert-anomalie automatico e UI "storico accessi".
- **A4/A9 — Meccanismo accettazione documenti. ✅ FATTO.** Entità **`AccettazioneDocumento`** (tipo/versione/
  accettato_at/revocato_at, no `AuditingListener`) + enum `TipoDocumento` (PRIVACY_POLICY/TERMINI_SERVIZIO/DPA);
  `@AssertTrue` su `RegisterRequest`/`GoogleRegisterRequest` + hook in `AuthService` (stessa tx); endpoint
  `/api/accettazioni` (`/me`,`/pending`,`/accetta`,`/me/{tipo}/revoca`) con **gate ri-accettazione server-side** (versioni
  via `@Value`, default in codice); checkbox FE nei due screen di registrazione; migrazione `014_accettazioni_documento.sql`.
  **Terminologia:** base giuridica account = **contratto (art. 6(1)(b))**, non "consenso" (presa visione/accettazione).
  *Rimandati a Wave 3:* i **testi legali** (Privacy/Termini/DPA) e il **gate FE al login** (redirect a pagina di
  ri-accettazione su `/pending`), da agganciare al bump `app.accettazione.versione.*`.
- **A6 — Retention/anonimizzazione.** *Prerequisito: durate dal titolare.* Poi: campo "fine
  trattamento/ultimo contatto", soft-delete/archiviazione, job `@Scheduled` di purge/anonimizzazione
  (riuso pattern gamification esistente).
- **✅ A5.3 — Limitazione del trattamento (art. 18) — FATTO.** Stato `Cliente.trattamentoLimitato` (+data/motivo,
  migr. `017`); check centralizzato `LimitazioneTrattamentoValidator.assertNonLimitato(cliente)` chiamato **dopo**
  l'ownership in tutti i punti write/produce/send (~50 metodi in 16 service) → **`TrattamentoLimitatoException` HTTP 423**.
  Regola: *blocca ciò che scrive/produce/invia, non ciò che il titolare legge* (export/download consentiti; share bloccato;
  delete art. 17 non bloccato). Endpoint `PATCH /api/clienti/{id}/limitazione(/revoca)` (perm. `CLIENTE_UPDATE`), atto
  auditato `LIMITAZIONE_ATTIVATA`/`REVOCATA` (stessa tx). FE: badge/banner/modale-motivo in `cliente-dettaglio`, badge in
  lista, azioni disabilitate. Test: `LimitazioneTrattamentoIntegrationTest` (11 casi, incl. **cross-tenant→403 non 423**) +
  `LimitazioneTrattamentoValidatorTest`. Interpretazione art. 18 **da confermare col legale** (come la retention).
  *(A5.2 export/portabilità = owner esterno, solo coordinamento.)*
- **D1 — Blocco clinico grave superabile con conferma consapevole. ✅ FATTO.** *Tutti* gli `ALERT_GRAVE`
  (non solo allergeni) in `AlimentoPastoService.associaAlimento` sono superabili con **flag dedicato
  `confermaBloccoGrave`** (distinto da `forzaInserimento` dei WARNING → un client "solo-warning" non
  scavalca inavvertitamente un blocco grave). L'override è **auditato** (A7 `OVERRIDE_ALERT_GRAVE`, stessa tx,
  motivi nel campo `dettaglio`; migrazione `015`); il metodo è ora **ownership-scoped** (`getOwnedPasto`).
  FE: `confirm()` rafforzato in `scheda-dieta.ts`. Rafforza il posizionamento anti-MDR (il software avvisa,
  il professionista decide). *Rimandato:* sistema di conferma unico/riusabile (modale dedicato) al posto dei
  `confirm()` nativi. 6 unit test (`AlimentoPastoServiceTest`).
- **✅ F-D1a — check allergeni nei percorsi template/duplicazione (FATTO, BE+FE).** Tutti i percorsi che
  creano `AlimentoPasto` via template/duplicazione passano ora dal `ClinicalEngineService.conflittiClinici`:
  #1 `PastoTemplateApplyService` (skip-and-report), #2 `SchedaTemplateService.creaSchedaDaTemplate` e
  #3 `applicaAScheda` (block-and-report → 409 `ClinicalConflictBody`), #4 `SchedaService.copyBulk` cross-paziente
  (skip-and-report 200 con `conflittiClinici` strutturato). Override consapevole **per-item** auditato
  (`OVERRIDE_ALERT_GRAVE`). FE: `components/conflitti-clinici-modal` riusato sui 4 percorsi (in `scheda-dieta`
  il modale condiviso usa un'**azione-pending** anti stale-state). Test: `FoodImportSafetyE2ETest` (#1-#4),
  `ClinicalEngineConflittiUnitTest`, `scheda-dieta.spec.ts` (sequenza anti-stale).
- **✅ F-OWN-SWEEP — sweep sistematico dell'ownership (FATTO, chiude anche F-D1b).** Passata su tutto il service
  layer (3 agenti): il grosso era già scoped; corretti **5 punti** non-scoped → **3 ALTO** mutating (`AlimentoPastoService.eliminaAssociazione`
  + `aggiornaQuantita` [ex F-D1b] → `getOwnedPasto`; `AlimentoAlternativoService.update` → nuovo `getOwnedAlimentoAlternativo`)
  e **2 MEDIO** (`listAlimentiByPasto` → `getOwnedPasto`; `FascicoloService.salvaDocumento` → `getOwnedCliente` **prima**
  del dedup, che espone il DTO). Inoltre i 4 `getOwned*` sub-risorsa (AlimentoPasto/Pasto/Appuntamento/OrariStudio)
  ora **auditano il DENIED** via `deny()` con nuovi `AuditEntityType` (`PASTO`/`ALIMENTO_PASTO`/`APPUNTAMENTO`/`ORARI_STUDIO`,
  additivi, no migrazione). Test: `OwnershipAntiLeakageIntegrationTest` esteso a **16 casi** (5 fix + regression-guard su
  Appuntamento/Misurazione/Plicometria + asserzione evento A7 DENIED) → **superficie ownership ora regression-guarded**.
  Suite BE **148 verdi**. Verificato che `AlimentoAlternativo` ha sempre `alimentoPasto` valorizzato (nessun 6° finding sul dual-parent).
- **✅ F-DEL-CASCADE (analogo delete di F-OWN-SWEEP): cancellazione cliente completa e regression-guarded (FATTO).**
  Inventario esaustivo (6 agenti + critico su 53 entità/40 repo): la cancellazione era **già funzionalmente completa**
  su tutti i dati vincolati da FK (i "2 buchi" storici — fascicolo A5.1, `AttivitaRecente` — erano già chiusi) → non un
  bug-fix ma **hardening**. Interventi: (1) **scelta di meccanismo ORM-first ibrido** motivata dalla portabilità (FK di
  TiDB no-op < v6.6, GA solo da v8.5 → un `ON DELETE CASCADE` DB passerebbe verde su H2 e potrebbe non cancellare su
  TiDB; SQL ORM/bulk identico sui due DB); **niente nuovi cascade DB**. (2) `deleteMyCliente` refactorizzato in
  `svuotaAlberoSchede` + `eliminaFigliNonCascade` (punto UNICO, auto-documentante, per i figli a FK non-mappata).
  (3) **Chiusa l'unica lacuna reale, silenziosa e non-FK:** `EventoGamification.clienteId` (denormalizzato, senza FK)
  ora **anonimizzato a NULL** in stessa tx — contrapposto ad `AuditLog.clienteId` che resta **intatto** (retention A7,
  art. 17(3)(b)). (4) `ClienteDeleteCascadeCompletoIntegrationTest` esteso: semina un figlio per OGNI tipo del grafo
  (diretti + transitivo con `AlimentoAlternativo` a **doppia FK** + `NomeOverride` + i due `clienteId` no-FK) → zero
  orfani + le **due asserzioni opposte** (EventoGamification `clienteId=NULL`, AuditLog `clienteId=id` **per id della
  riga**). Regola di coverage in CLAUDE.md. Verde.
- **🟡 Finding derivato — semplificazione dual-FK di `AlimentoAlternativo` (aperto, tracciato).** La FK `alimento_pasto_id`
  è marcata **"legacy"** e coesiste con `pasto_id`; solo la prima è in `orphanRemoval`, per questo il delete richiede lo
  svuotamento bulk esplicito. Consolidare su una sola FK è una modifica al **data-model** con rischi propri (migrazione +
  ricodifica di `AlimentoAlternativoService`/`SchedaService`/template) → intervento a sé, fuori da F-DEL-CASCADE.
- **B2 — Robustezza credenziali** (policy password, lockout/rate-limit, reset via email): **solo
  se NON si adotta Keycloak a breve**. Altrimenti arriva dall'IdP → non anticipare.
- **D4 — Accessibilità WCAG** sui form gestionali sotto-etichettati. Incrementale (P2).
- **E2 residuo — Cookie/consent banner** (CMP + blocco pre-consenso per GSI/risorse terze). Parte
  tecnica ora; i testi in Wave 3.

---

## Wave 3 — RIMANDATO (bloccato da decisioni: infra / legale / auth)

Non sono codice "puro": vanno sbloccati da una scelta.

- **★ Hosting/IaC (C3/C1)** — il perno. Sblocca A3, B7, A2, config-prod, verifica residenza UE,
  hosting di Keycloak. **Da decidere presto** (collo di bottiglia a 3–4 item PROD).
- **A3 — TLS/reverse proxy** (dopo hosting): HTTPS terminato da nginx/Caddy davanti al backend.
  ⚠️ Contestualmente abilitare `server.forward-headers-strategy=framework` (o `NATIVE`) così l'audit
  A7 registra l'IP reale del client (`X-Forwarded-For`) e non quello del proxy. Solo con proxy fidato.
- **B7 — Backup manuale cifrato e testato** (dopo scelta storage/hosting).
- **A2 — Cifratura at-rest di campo** — *prima accertare* se l'at-rest di TiDB Cloud basta
  (TiDB cifra a riposo via KMS del cloud): se il threat model non richiede difesa in profondità
  contro l'accesso al dump, A2 field-level può essere declassato. Attenzione: `@Convert` rompe
  query/indici sulle colonne cifrate → applicare solo a campi sensibili non filtrati.
- **★ Auth Opzione B / Keycloak** — dopo hosting: import hash BCrypt, pattern BFF, cookie
  HttpOnly, feature flag, convivenza sessioni. Federazione Google + Apple (Apple = Developer
  Program $99/anno, client secret da rigenerare). Fase a sé, grande sforzo.
- **LEGALE** — E3 documenti (Privacy/ToS/Cookie policy); testi consenso A4/A9; C2 DPA fornitori
  (TiDB/Google/OpenFoodFacts); D1 documento intended-use MDR.
- **E4a — Provider email transazionale con DPA** (oggi Gmail consumer): scelta provider, poi codice.
  Rafforzato da A7: `sendPdfEmail` è ora **sincrono** → un transazionale (più veloce/affidabile di SMTP
  Gmail consumer) migliora sia la compliance (DPA) sia la latenza/robustezza dello share. Nel frattempo
  mitigato dai timeout SMTP in `application.properties`.
- **D3 — Valutazione AI Act**: checkpoint obbligatorio prima di sviluppare qualunque feature IA.

**Fuori scope (OOS)**: A5.2 export (owner: altro collega, solo coordinamento); E1
billing/fatturazione (owner esterno).

---

## Decisioni aperte che sbloccano Wave 2/3

1. **Hosting/deploy**: dove gira la produzione? *(sblocca TLS, backup, config-prod, Keycloak)*
2. **Retention**: durate di conservazione per categoria di dato *(serve il titolare)*
3. **Legale**: c'è supporto per i testi (Privacy/ToS/consenso/DPA) o si rimandano? *(il meccanismo
   consenso si costruisce comunque prima)*
4. **Auth**: confermata Opzione B (Keycloak dopo hosting); rivalutare solo se il go-live rischia di
   precedere l'IdP → in tal caso B2 in-house come ponte.

### Hosting — confronto sintetico (per la decisione 1)

Scope: solo *backend Spring + frontend Angular statico + futuro Keycloak*; il DB resta su TiDB
Cloud (esterno). Per dati art. 9 preferire provider con **sede legale UE** (non solo region UE).

| Provider | Tipo | Costo indicativo | Ops (sforzo) | GDPR | Keycloak |
|---|---|---|---|---|---|
| **Hetzner** (DE) | VPS/IaaS | ~€5–12/mese | Alto (OS, TLS, backup, patch) | UE, sede UE | Ottimo (tutto su 1 macchina) |
| **Clever Cloud** (FR) | PaaS | ~€20–50/mese | Basso (TLS/scaling/backup gestiti) | UE-only, ISO 27001 + HDS | Buono (container/add-on) |
| **Scalingo** (FR) | PaaS | pay-as-you-go, ordine simile | Basso | UE-only, ISO 27001 + HDS + SecNumCloud | Buono |
| PaaS US (Render/Fly/…) | PaaS | ~€10–30/mese | Basso | region UE ma **sede US** → nuance CLOUD Act | Buono |

**Dimensionamento**: l'app è piccola → **4 GB RAM / 2 CPU** sono comodi (backend ~1 GB +
Keycloak ~1 GB + proxy + OS). Nessuna necessità di potenza elevata; il costo/effort vero è
operativo (manutenzione) e di compliance (backup, TLS), non di CPU. Server fisico on-prem
sconsigliato per dati sanitari (responsabilità di sicurezza fisica/continuità/backup).

**Raccomandazione**: se driver = costo/controllo → Hetzner; se driver = minimo ops + postura
sanitaria certificata → Clever Cloud/Scalingo. Per team piccolo su dati art. 9, si tende ai PaaS
UE certificati (stessa logica di Keycloak: far fare la sicurezza a chi è certificato).

---

## Metodo di esecuzione

Come in Fase 2: un item alla volta, build/test verde prima del successivo, commit coerenti e
separati per item, due git root distinti. Wave 2 e 3 si pianificano in dettaglio quando ci si
arriva / quando arrivano le decisioni aperte sopra.