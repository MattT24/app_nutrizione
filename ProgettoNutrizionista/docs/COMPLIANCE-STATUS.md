# COMPLIANCE-STATUS — Gap analysis sicurezza/compliance Statera

> **Fase 1 — SOLO analisi.** Nessuna modifica al codice. Documento di stato prodotto rivedendo con
> l'utente, uno a uno, tutti i findings dell'esplorazione read-only.
>
> **Data**: 2026-07-12
> **Perimetro**: applicativo Statera (backend `app_nutrizione` + frontend `nutrizionista_front`).
> **Base normativa**: GDPR (in particolare art. 9 — dati sanitari), art. 32 (misure di sicurezza),
> provvedimenti del Garante in materia di dossier/fascicolo sanitario.

## Nota metodologica sulle evidenze

- Le citazioni **frontend** (`nutrizionista_front`) sono state **ri-verificate in questa sessione**.
- Le citazioni **backend** (`app_nutrizione`) provengono dall'esplorazione locale già completata e sono
  marcate **`[verificato in locale]`**: il repo backend non è montato in questa sessione, quindi vanno
  ricontrollate a campione sulla macchina di sviluppo.
- Legenda stato: ✅ conforme · 🟡 parziale/da irrobustire · ❌ assente/non conforme · ➖ non applicabile
  o fuori perimetro. Il simbolo ⚠️ segnala impatto diretto su dati sanitari.
- Legenda fase: **QW** quick win (basso sforzo/alto valore) · **PROD** bloccante prima della produzione ·
  **NEXT** fase successiva (approfondimento/sviluppo pianificato) · **OOS** fuori scope (owner esterno).

## Contesto operativo (dichiarato dal team)

- Prodotto **in fase di sviluppo**: gli unici utenti con password sono account di test
  (1 admin + 1 nutrizionista, password "password"). Non esistono dati personali reali di utenti in
  produzione. Molti rischi classificati P0 sono quindi **bloccanti prima del go-live**, non incidenti in
  corso.
- Le voci **PROD** confluiscono nella *Checklist "Prima della produzione"* in fondo, pensata per essere
  copiata nel `CLAUDE.md` del team.
- Export dati (art. 20) è **in carico ad altro collega** → non trattato qui se non come punto di verifica.
- Billing/fatturazione elettronica è **previsto ma gestito da un owner esterno** → fuori scope.

---

## 1. Sommario esecutivo

**Conteggio stato per area**

| Area | ✅ | 🟡 | ❌ | ➖ |
|------|----|----|----|----|
| A — Protezione dati | 0 | 3 | 6 | 0 |
| B — Sicurezza tecnica | 1 | 3 | 3 | 0 |
| C — Infrastruttura | 1 | 1 | 1 | 1 |
| D — Qualificazione prodotto | 1 | 2 | 1 | 0 |
| E — Commerciale/legale | 0 | 1 | 2 | 1 |

**Top 5 rischi critici** (dettaglio in §3)

1. **IDOR multi-tenant** su PDF/Fascicolo/TDEE — lettura/invio di dati sanitari di altri tenant (A1).
2. **`jwt.secret` pubblico** — token forgiabili, bypass autenticazione (B1) — da chiudere prima del go-live.
3. **`/uploads/**` pubblico** — PDF di fascicoli sanitari scaricabili senza login (A8).
4. ~~**Audit trail assente** sugli accessi ai dati sanitari — requisito Garante (A7).~~ ✅ RISOLTO (Wave 2).
5. **Base giuridica assente** — nessun consenso art. 9, informativa, DPA in registrazione (A4 + A9 + E3).

**Quick win concordati** (basso sforzo, da fare a breve)

Proteggere `/uploads/**` dietro endpoint autenticato con ownership check (A8) · `npm audit fix` (B5) ·
vincolare il destinatario email all'email registrata del cliente (E4b) · disclaimer anti-MDR in UI/PDF/
email (D2) · self-host di font e icone (E2) · handler errori con messaggio generico + email fuori dai
log (A8) · route guard + gestione 401 lato frontend (vedi AUTH-ASSESSMENT) · `.env` dedicato al backend
per i segreti (A3/B4).

---

## 2. Matrice per area A–E

### A — Protezione dati

| ID | Requisito | Stato | Evidenza | Decisione | Fase |
|----|-----------|:-----:|----------|-----------|:----:|
| A1 | Isolamento multi-tenant su tutte le risorse cliente | ✅ | **FATTO**: tutti gli endpoint elencati passano ora da `OwnershipValidator.getOwned*` (Fase 2 QW-2: PdfService/Fascicolo/TDEE con `/recenti` scoped/OrariStudio delete/Appuntamento.clienteId) + `OwnershipAntiLeakageIntegrationTest`; residuo "artigianale" `AppuntamentoService.verificaProprietario` uniformato al validator (Fase 3 Wave 1). **Residuo minore aperto**: `AlimentoBaseService` (ownership su `createdBy`, 400 anziché 403, senza `getOwned*` — AlimentoBase è catalogo in parte globale) → da decidere. | FATTO |
| A2 | Cifratura at-rest dei dati sanitari | ❌⚠️ | Nessun `@Convert`/`AttributeConverter`/jasypt; anagrafica + dati sanitari in chiaro nella stessa tabella `clienti` (`Cliente.java:44-97`) `[verificato in locale]` | At-rest oggi demandato (da confermare) al layer TiDB. Analisi specifica della cifratura di campo in fase successiva. | NEXT |
| A3 | Cifratura in transito end-to-end | 🟡 | DB↔TiDB in TLS `VERIFY_IDENTITY` (`application.properties:6`) ✓; backend serve **solo HTTP** (`server.port=8080`, nessun `server.ssl.*`); FE chiama `http://localhost:8080` `[verificato in locale]` | Studio approfondito quando sarà scelta la piattaforma di deploy (dipende da C3). Soluzione attesa: **reverse proxy che termina HTTPS** davanti al backend. Prerequisito: lavoro ambienti (`environments/` Angular + profili Spring). | PROD |
| A4 | Base giuridica art. 9 — consenso | ❌ | Nessuna entità Consenso, nessun campo versione/data/revoca `[verificato in locale]` | Assenza totale confermata. Approfondimento futuro; da avere prima del go-live. | PROD |
| A5.1 | Diritto alla cancellazione (art. 17) completo | ✅ | **FATTO** (Fase 3 Wave 1): `ClienteService.deleteMyCliente` ora rimuove anche i `DocumentoFascicolo` + i file su disco via `FascicoloService.eliminaDocumentiDiCliente`; test `ClienteDeleteFascicoloIntegrationTest` (nessun orfano). | FATTO |
| A5.2 | Diritto alla portabilità (art. 20) | 🟡 | Nessun endpoint di export strutturato `[verificato in locale]` | In carico ad altro collega → **non toccare**, solo punto di verifica futura. | OOS |
| A5.3 | Diritto di limitazione (art. 18) | ❌ | Nessun meccanismo di "congelamento" del cliente `[verificato in locale]` | Da sviluppare. | NEXT |
| A6 | Retention / anonimizzazione dati clinici | ❌ | Nessuna policy sui dati clinici; unico `@Scheduled` di cleanup = gamification (730gg) `[verificato in locale]` | Approfondimento successivo (dopo i quick win) con approccio in §"Nota A6". | NEXT |
| A7 | Audit log accessi ai dati sanitari (≥24 mesi) | ✅ | Entità `AuditLog` immutabile append-only (chi/cosa/quando/da-dove/paziente/tipo/esito, no FK su utente/cliente → sopravvive a cancellazioni). Popolamento **esplicito service-layer**: eventi critici SHARE/EXPORT_PDF/DOWNLOAD sincroni in tx propria (REQUIRES_NEW, "no log ⇒ no disclosure") con riga FAILURE su invio non riuscito; DELETE nella stessa tx; letture/liste cliniche async; accessi NEGATI (`recordDenied`, REQUIRES_NEW). Endpoint consultazione `/api/audit` (`AUDIT_READ` admin) + `/api/audit/me` (`AUDIT_READ_OWN` self, tenant forzato). Retention scheduler ≥24 mesi (floor), purge disattivo di default (attesa A6). Migrazione `013_audit_log.sql`. Test: `AuditServiceTest`, `AuditIntegrationTest`, `AuditConsultazioneMultiTenantTest`, `AuditLogCleanupSchedulerTest`. | **FATTO** (Wave 2). Limite noto MVP: dinieghi di *ruolo* (`@PreAuthorize`) non auditati; alert anomalie e UI storico rimandati. |
| A8 | Minimizzazione diffusione / hardening esposizione | 🟡 | id numerici in path (no PII in URL) ✓; ma `show-sql=true`; handler `RuntimeException→400` che espone `getMessage()`; `EmailService` logga gli indirizzi email; **`/uploads/**` pubblico senza auth** (`SecurityConfig.java:63`) `[verificato in locale]` | `/uploads/**` = **P0**: servire i PDF via endpoint autenticato con ownership check, non come static resource. `show-sql`: `false` in prod (richiede prima i profili Spring). Handler: messaggio generico al client, dettaglio solo nei log server. Email: togliere/mascherare dai log. | QW (uploads, handler, log) + PROD (show-sql) |
| A9 | Informativa / DPA alla registrazione | ❌ | Nessun campo di accettazione in `RegisterRequest`/`Utente`; nessun checkbox nel FE `[verificato in locale]` | Assenza confermata. Da implementare prima del go-live (con E3). | PROD |

**Nota A6 — approccio retention concordato.** (1) Definire la policy *prima* del codice: per ogni
categoria di dato (anagrafica, misurazioni/plicometrie, schede, fascicolo, appuntamenti, log) fissare
durata e base giuridica **con il titolare** (professionista sanitario → termini legati a obblighi
professionali/contrattuali, non inventati nel codice). (2) Ancorare la decorrenza a un evento certo:
serve un campo tipo "fine trattamento / ultimo contatto" (oggi assente). (3) Implementare in due tempi:
prima soft-delete/archiviazione (dato fuori dall'operatività ma recuperabile), poi job `@Scheduled` di
purge o anonimizzazione allo scadere del termine, **riusando il pattern già presente** per la
gamification. (4) Documentare la policy nel registro dei trattamenti / informativa (si aggancia ad A9).

### B — Sicurezza tecnica

| ID | Requisito | Stato | Evidenza | Decisione | Fase |
|----|-----------|:-----:|----------|-----------|:----:|
| B1 | Segreto di firma JWT robusto e privato | ❌⚠️ | `jwt.secret` = token di esempio pubblico di jwt.io (`application.properties:72`, usato in `JwtUtils.java:26`) → **chiunque può forgiare token validi** `[verificato in locale]` | Oggi siamo in dev con soli utenti di test → **rotazione da fare prima della produzione**. Valore forte, generato casualmente, letto da `.env` (vedi A3/B4). | PROD |
| B2 | Robustezza credenziali | 🟡/❌ | Password BCrypt (strength 10) ✅; nessuna policy di complessità; no lockout/rate-limit sul login; cambio password senza verifica dell'attuale; no reset/"password dimenticata"; no MFA `[verificato in locale]` | Prima del go-live. Con la migrazione auth (Opzione B) MFA/policy/lockout/reset arrivano pronti da Keycloak. | PROD |
| B3 | Autorizzazione a più livelli (difesa in profondità) | 🟡 | `@PreAuthorize` su 35 controller (203 usi) ma **zero nei service**; `PromemoriaController` e `SystemController` **senza** `@PreAuthorize` `[verificato in locale]` | **Regola adottata per tutto il software**: nessun controller senza annotazione di autorizzazione esplicita (riesaminare i due scoperti); i service che toccano dati cliente passano **sempre** dall'`OwnershipValidator` canonico (si aggancia ad A1). | NEXT |
| B4 | Gestione segreti fuori dal codice/disco | ❌ | Segreti in chiaro in `application.properties` (DB, Gmail app-password, jwt); file **ignorato** da git (`git check-ignore` ✓, non tracciato) ma valori non in env/vault `[verificato in locale]` | Creare `.env` dedicato al backend; `application.properties` legge da variabili (`${JWT_SECRET}`, ecc.). Assorbito da A3. | PROD |
| B5 | Gestione vulnerabilità dipendenze / CI security | ❌⚠️ | `npm audit --omit=dev` = **7 high in produzione** (XSS Angular compiler ×4, lodash-es); 40 totali (2 critical in dev); lockfile presente ✓ (**ri-verificato in questa sessione**) | `npm audit fix` = quick win immediato. CI con SAST/SCA (GitHub Action: build + `npm audit`) + Dependabot sui due repo = attività futura in checklist. | QW (`audit fix`) + PROD (CI) |
| B6 | Header di sicurezza / CORS | 🟡 | Solo default Spring (no CSP/HSTS custom); CSRF disabilitato (coerente con bearer token); CORS `@CrossOrigin(localhost:4200)` **hardcoded** su 36 controller `[verificato in locale]` | CORS da **centralizzare e parametrizzare** (un solo punto, valore da ambiente), insieme al lavoro sui profili. Valutare CSP/HSTS al reverse proxy. | PROD |
| B7 | Backup & Disaster Recovery | ➖/❌ | Nessuno script/config nel repo (demandato a TiDB, da confermare) `[verificato in locale]` | Studiare un **backup manuale** (dump periodico cifrato verso storage separato, con retention) invece di affidarsi al solo automatico TiDB. | NEXT |

### C — Infrastruttura

| ID | Requisito | Stato | Evidenza | Decisione | Fase |
|----|-----------|:-----:|----------|-----------|:----:|
| C1 | Data residency UE | ✅ | TiDB Cloud su AWS **eu-central-1 (Francoforte, UE)** `[verificato in locale]` | Conforme oggi. **In produzione il provider potrebbe cambiare** → ripetere la verifica di residenza UE alla scelta definitiva. | — / PROD |
| C2 | DPA con i sub-responsabili | 🟡 | Sub-responsabili: TiDB (UE), Gmail SMTP (Google), Google Identity Services, OpenFoodFacts (no PII). Nessun DPA documentato `[verificato in locale]` | Nessun contatto avuto finora. Task futuro: verificare online termini/DPA di ciascun fornitore e cosa va compilato/firmato. Gmail consumer per PDF sanitari è il punto più critico (vedi E4). | NEXT |
| C3 | Hosting / IaC / containerizzazione | ❌ | Assente; hosting di produzione non deducibile dal codice `[verificato in locale]` | **Non ancora in discussione.** È il perno da cui dipendono A3 (TLS), B7 (backup), il lavoro ambienti. Da decidere prima del go-live. | PROD |
| C4 | Assenza di dati reali nel repository | ➖ | 28 PDF in `ProgettoNutrizionista/uploads/fascicoli/**/*.pdf` `[verificato in locale]` | **RICLASSIFICATO**: il team conferma che sono **dati mock su clienti inventati → nessun rischio privacy**. Decade da P0 a nota di igiene: in futuro non versionare i contenuti di `uploads/` (peso del repo + rischio che un giorno ci finisca un file reale) e annotare che i file sono fittizi, per evitare falsi allarmi in audit futuri. Nessuna riscrittura della history necessaria. | Nota |

### D — Qualificazione prodotto

| ID | Requisito | Stato | Evidenza | Decisione | Fase |
|----|-----------|:-----:|----------|-----------|:----:|
| D1 | Rischio qualificazione dispositivo medico (MDR) | 🟡⚠️ | Nessun claim di diagnosi/terapia/cura; ma motore di alert clinici per-paziente (`ALERT_GRAVE`/"Allergia mortale", tag `PAT_*`) `[verificato in locale]` | **Intended use dichiarato dal team**: sono aiuti alla compilazione delle diete che *prescindono* dalla decisione del nutrizionista; in caso di **allergia** il sistema *blocca* l'inserimento dell'alimento pericoloso; in caso di **intolleranza** avvisa ma consente l'inserimento (decisione al professionista). Raccomandazioni: (a) formalizzare l'intended use per iscritto (una pagina); (b) valutare se il blocco allergie debba diventare **superabile con conferma esplicita** ("sono consapevole, procedi"), così la decisione finale resta formalmente del professionista come per le intolleranze. | NEXT |
| D2 | Disclaimer "non sostituisce il parere del professionista" | ❌ | Assente in UI, PDF generati ed email `[verificato in locale]` | **Inserimento confermato** in UI, PDF generati ed email. È la mitigazione più economica del rischio D1. | QW |
| D3 | AI Act | ✅ | Nessuna feature AI/ML oggi (elaborazioni deterministiche) `[verificato in locale]` | Conforme oggi. Ma **l'integrazione IA arriverà**: checkpoint obbligatorio → **valutare l'AI Act prima di sviluppare** qualunque feature IA (un suggeritore di diete che elabora dati sanitari rischia la fascia alto-rischio). | PROD/NEXT |
| D4 | Accessibilità (WCAG) | 🟡 | ~55% dei controlli con `<label>` associata, ARIA sporadico, form gestionali sotto-etichettati (audit FE) | Nessun obbligo di legge probabile per un B2B professionale (gli obblighi Legge Stanca / European Accessibility Act colpiscono PA, grandi aziende, servizi consumer). **Debito tecnico da pianificare** (buona pratica + eventuale requisito commerciale futuro). | NEXT (P2) |

### E — Commerciale / billing / legale

| ID | Requisito | Stato | Evidenza | Decisione | Fase |
|----|-----------|:-----:|----------|-----------|:----:|
| E1 | Fatturazione elettronica SdI/FatturaPA, IVA/VIES/OSS, abbonamenti | ➖ | Assenti `[verificato in locale]` | Previsto ma **gestito da owner esterno** → fuori scope. | OOS |
| E2 | Cookie banner / minimizzazione trasferimenti a terzi | ❌ | Nessun banner/CMP; Google Fonts + cdnjs caricati da CDN esterne (`index.html:10-11`) → IP utente a terzi senza consenso; script GSI da `accounts.google.com/gsi/client` (`google-identity.service.ts:32`) (**ri-verificato in questa sessione**) | **Self-host di font e icone** = quick win (elimina il problema alla radice per fonts/cdnjs). Per GSI il caricamento esterno è inevitabile finché c'è il login Google → coprire con informativa. | QW + PROD (informativa) |
| E3 | Documenti legali nel prodotto (Privacy/ToS/Cookie policy) | ❌ | Assenti; nessun link; nessun consenso in registrazione `[verificato in locale]` | Redazione con commercialista / supporto legale, prima del go-live (si lega ad A9). | PROD |
| E4 | Trasmissione sicura dei PDF sanitari | 🟡⚠️ | Invio di PDF sanitari via **Gmail consumer** a indirizzo digitato a mano dall'operatore, senza verifica che sia l'interessato (controller Scheda/Misurazione/Plicometria/Fascicolo) `[verificato in locale]` | **Fix confermato**: (a) passare a provider email business/transazionale **con DPA** (fase successiva, con C2); (b) **vincolare/precompilare il destinatario all'email registrata del cliente**, override solo con conferma esplicita, + **log di ogni invio**. | QW (E4b vincolo) + NEXT (E4a provider) |

---

## 3. Rischi critici

1. **IDOR multi-tenant (A1)** — Diversi endpoint recuperano risorse cliente con `findById` non-scoped,
   ignorando l'`OwnershipValidator` esistente. Un tenant autenticato può leggere o inviare PDF, documenti
   di fascicolo e calcoli TDEE di altri tenant. `/api/tdee/recenti` restituisce esplicitamente gli ultimi
   10 calcoli globali. Impatto: violazione di riservatezza su dati art. 9. Nessun test anti-leakage.
2. **`jwt.secret` pubblico (B1)** — Il segreto di firma è un valore di esempio reperibile pubblicamente.
   Chiunque può forgiare un token valido per qualsiasi utente/ruolo → bypass totale dell'autenticazione,
   escalation, accesso cross-tenant. Mitigato oggi solo dal fatto che non esistono utenti reali. Da
   chiudere **prima** di qualunque esposizione pubblica.
3. **`/uploads/**` pubblico (A8)** — I PDF dei fascicoli sanitari sono serviti come risorsa statica
   `permitAll`: scaricabili senza autenticazione da chiunque conosca l'URL. I nomi UUID non sono
   enumerabili, ma gli URL trafilano per natura (cronologia, log proxy, inoltro email, header Referer).
4. ~~**Audit trail assente (A7)**~~ — ✅ **RISOLTO (Wave 2).** Entità `AuditLog` immutabile append-only
   che registra chi/cosa/quando/da-dove/paziente/tipo/esito degli accessi ai dati sanitari
   (letture, export/download/share PDF, delete, accessi negati); consultabile via `/api/audit`;
   retention ≥24 mesi (floor). Resta aperto solo l'alert-anomalie automatico (non richiesto per l'MVP).
5. **Base giuridica assente (A4 + A9 + E3)** — Nessun consenso art. 9 modellato, nessuna informativa/DPA
   in registrazione, nessun documento legale nel prodotto. Il trattamento di dati sanitari è privo di base
   documentata. Da sanare prima del go-live.

---

## 4. Checklist "Prima della produzione"

> Sezione pensata per essere copiata nel `CLAUDE.md` del team. Nessuna di queste voci è un incidente in
> corso (siamo in dev con soli utenti di test); sono le condizioni da soddisfare **prima** di trattare
> dati reali di utenti.

- [ ] **Ruotare `jwt.secret`** con valore casuale forte, letto da `.env` (B1/B4)
- [ ] **`.env` dedicato al backend**; `application.properties` legge tutti i segreti da variabili (B4)
- [ ] **Policy password + lockout + reset** (o completare la migrazione auth Opzione B che li fornisce) (B2)
- [ ] **TLS end-to-end** via reverse proxy davanti al backend (A3, dipende da hosting C3)
- [x] **Profili Spring dev/prod** + `show-sql=false` in prod + **CORS parametrizzato** (A8/B6) — fatto (Fase 3 Wave 1). Resta il **TLS/reverse proxy** (A3), legato alla scelta hosting.
- [x] **`environments/` Angular** con endpoint backend configurabile — fatto (Fase 3 Wave 1): `environment(.prod).ts` + `apiBaseUrl`.
- [ ] **Consenso art. 9 + informativa/DPA in registrazione** (A4/A9)
- [ ] **Documenti legali** Privacy/ToS/Cookie policy nel prodotto, con supporto legale (E3)
- [ ] **Cookie/consenso** per i trasferimenti a terzi + self-host font/icone (E2)
- [ ] **Verifica DPA con i fornitori** (TiDB, Google/Gmail, OpenFoodFacts) (C2)
- [x] **CI** (build + test + `npm audit --omit=dev`) + **Dependabot** su entrambi i repo — fatto (Fase 3 Wave 1). Resta da aggiungere un **SAST** (es. CodeQL/Semgrep).
- [ ] **Retention/anonimizzazione** dei dati clinici implementata (A6)
- [x] **Audit log** degli accessi ai dati sanitari progettato e attivo, retention ≥24 mesi (A7) — fatto (Wave 2). In prod: eseguire `013_audit_log.sql`, valutare l'attivazione del purge (`app.audit.retention-purge-enabled`) e abilitare `X-Forwarded-For` col reverse proxy (vedi ROADMAP A3).
- [ ] **Scelta hosting/IaC** con verifica residenza UE (C1/C3)
- [ ] **Backup manuale** cifrato e testato (ripristino verificato) (B7)
- [ ] **Valutazione AI Act** completata prima di rilasciare qualunque feature IA (D3)
- [ ] **Migrazione auth Opzione B** (Keycloak) completata o pianificata con mitigazioni ponte (vedi AUTH-ASSESSMENT)

---

## 5. Domande aperte (non verificabili dal codice)

- **Reverse proxy / TLS in produzione**: esiste già un layer che termina HTTPS davanti al backend, o va
  previsto? (dipende dalla scelta hosting, C3)
- **Cifratura at-rest TiDB**: il layer di storage cifra effettivamente i dati a riposo? Con quali chiavi/
  gestione? (A2)
- **Backup TiDB**: i backup automatici del piano TiDB Cloud sono attivi e con quale retention? (B7)
- **DPA fornitori**: quali accordi sono già in essere o vanno stipulati con TiDB, Google, OpenFoodFacts? (C2)
- **Intended use MDR**: verrà formalizzato per iscritto il documento di destinazione d'uso? (D1)
- **Titolare vs responsabile**: la ripartizione dei ruoli GDPR tra Statera e il singolo nutrizionista è
  documentata contrattualmente?