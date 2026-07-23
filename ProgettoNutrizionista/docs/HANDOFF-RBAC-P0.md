# HANDOFF — P0 (IDOR catalogo) + RBAC pre-Keycloak

> **Documento di consegna all'agente implementatore.** Modello a due agenti: **l'implementatore scrive il
> piano e implementa; l'agente Code verifica** (codice + piani). Prodotto e verificato da Code (2026-07-19).
>
> **Leggi prima:** `CLAUDE.md`; `nutrizionista_front/docs/KNOWLEDGE_BASE.md` (§2 vincoli, §4 metodo, §7
> invarianti/gap, §8 A6); `docs/RBAC-TARGET.md` (design + decisioni RBAC); questo file (task + accettazione).
> Il dettaglio dei finding è in KB §7; il rationale delle scelte in RBAC-TARGET.

## Contesto & vincoli (non negoziabili)
- Statera tratta **dati sanitari (art. 9)**. Fase: **pre-produzione** (2 account dev, dati mock).
- **Nessun commit/push** (li gestisce l'utente). **Build+test verdi** (`./mvnw test`, foreground) prima di
  dichiarare "fatto"; ogni fix porta un **test di regressione** che fallirebbe senza il fix.
- **Difesa in profondità nel service** (permesso ≠ proprietà). Ordine `getOwned*` → `assertNonLimitato`
  (403 prima di 423) dove applicabile.
- **Discriminante admin = ruolo sull'entità** (`getCurrentUtente().getRuolo().getAlias()`), **MAI**
  `hasAuthority("ADMIN")`: le authority sono costruite **solo dai permessi** (`UserDetailsServiceImpl:44-46`),
  i ruoli non sono authority → `hasAuthority("ADMIN")` è sempre false e romperebbe l'ADMIN storico.
- **Auth target = Keycloak (JWT custom scartato).** **NON** costruire macchinario custom-JWT che l'IdP
  rimpiazzerà (impersonation/token-exchange, kill-switch token, `tokenVersion` su utenti reali, token
  sender-constrained). **E4/break-glass su utenti reali è FUORI SCOPE** (deferito a Keycloak).
- Migrazioni SQL **idempotenti** e portabili (niente `ON DELETE CASCADE` DB nuovi); `ddl-auto` non tocca la
  seed → SQL esplicito.

---

## Batch 0 — P0: IDOR write/delete sul catalogo `AlimentoBase` (SICUREZZA, per primo)

> **Nota (verifica Code 2026-07-19):** il guard su **`create` (globale) è spostato in Batch 2**. Verificato:
> `create` globale è chiamato solo dall'endpoint `add`; l'import OFF usa già `createPersonale`
> (`OpenFoodFactsService:167`); ma il **flusso FE di creazione** non è verificato → guardare `create` qui
> romperebbe la creazione lato nutrizionista se il FE usa l'endpoint globale. Batch 0 resta quindi **solo
> `update`/`delete`** (vero IDOR, self-contained); `create` va con Batch 2 (governance + switch FE).

**Dove:** `service/AlimentoBaseService.java` `update:80` (`findById:84`, nessun check), `delete:139`
(`deleteById` secco), `create/add:56` (`createdBy=null`); `controller/AlimentoBaseController.java` PUT `:38-43`
(`ALIMENTO_UPDATE`), DELETE `:45-50` (`ALIMENTO_DELETE`), POST `:24-29` (`ALIMENTO_CREATE`), `deletePersonale`
`:52-57` (`ALIMENTO_PERSONALE_CREATE`).

**Difetto:** nessun controllo `created_by`; `ALIMENTO_UPDATE/DELETE/CREATE` sono sul ruolo **NUTRIZIONISTA**
(seed `005:426`) → un nutrizionista può modificare/cancellare alimenti **globali** e **personali altrui**, e
creare globali. (Verificato: unica vera falla di ownership della self-review.)

**Fix (Opzione A, service-level — helper condiviso):**
```java
/** Catalogo misto: globale (created_by==null) → solo platform-admin; personale → solo il proprietario. 403. */
private void assertPuoModificareCatalogo(AlimentoBase a) {
    if (a.getCreatedBy() == null) {
        if (!isPlatformAdmin()) throw new ForbiddenException("Solo un amministratore può gestire il catalogo globale");
    } else if (!a.getCreatedBy().getId().equals(getCurrentUtente().getId())) {
        throw new ForbiddenException("Non puoi modificare questo alimento");
    }
}
private boolean isPlatformAdmin() {
    String r = getCurrentUtente().getRuolo().getAlias();   // "ADMIN" | "NUTRIZIONISTA" | "SUPER_ADMIN"
    return "ADMIN".equals(r) || "SUPER_ADMIN".equals(r);
}
```
Chiamalo in `update`/`delete` (dopo il `findById`) e riusalo in `deletePersonale` (DRY). **Non** è un
`getOwned*` (business-rule inline, 403). *(Il guard su `create` è in Batch 2 — vedi nota sopra.)*

**Test:** nutrizionista → **403** su globale e su personale-altrui (`update`/`delete`); nutrizionista → OK sul
proprio personale; ruolo **ADMIN → OK** su globale (non deve regredire). *(Test `create` → Batch 2.)*

**Accettazione:** nessun nutrizionista può **scrivere/cancellare** un `AlimentoBase` non suo o globale.
**Self-contained: nessuna modifica FE/seed.** Forward-compatible col ritiro ADMIN (poi `isPlatformAdmin`
matcha solo SUPER_ADMIN).

---

## Batch 1 — RBAC: ritiro del ruolo ADMIN (auth-independent)

**Prerequisito:** output della **query DB** (permessi `ALIMENTO_*`/per ruolo + "quali utenti sono sul ruolo
ADMIN"). **Da richiedere all'utente** (nessun client MySQL locale). Atteso: unico utente su ADMIN = account dev.

**Contesto verificato:** 3 ruoli — ADMIN (id 1, "tutti i permessi", authority-string **inerte**), NUTRIZIONISTA
(id 2), SUPER_ADMIN (id 60001, solo permesso `SUPER_ADMIN`). **Nessun codice Java dipende dal ruolo ADMIN**
(nessun `hasRole/hasAuthority('ADMIN')`); referenze solo in seed/migrazioni (`005/006/008/013`; stray
`ROLE_ADMIN` in `migrations/permessi orario_studio.sql:50`).

**Fix (nuova migrazione RBAC, numerata `019` — RBAC precede A6 nel sequencing → A6 diventa `020`; evitare due
file `019`; idempotente):** migra `utenti.ruolo_id` da ADMIN →
**NUTRIZIONISTA** (account dev — decisione D2); rimuovi il ruolo ADMIN + le sue `ruoli_permessi`; assegna a
SUPER_ADMIN i permessi control-plane mancanti (E1: es. `AUDIT_READ`); bonifica lo stray `ROLE_ADMIN`.

**Test:** account dev funziona come NUTRIZIONISTA; SUPER_ADMIN invariato; boot/seed verdi; nessun riferimento
rotto ad ADMIN.

**Conferma D2 (2026-07-19):** l'account admin dev → **NUTRIZIONISTA** (è **uno solo** sul ruolo ADMIN,
confermato dall'utente; migrazione **per ruolo**, email non hardcoded). **Conseguenze:**
- Dopo il ritiro, **nessun account dev raggiunge `/api/admin/**`** finché non si configura l'utente
  SUPER_ADMIN (`SuperAdminSeeder`: `superadmin.email` + `superadmin.password-hash`). Heads-up per il test
  del control plane (demo/assistenza/audit) — non blocca Batch 1.
- **Cleanup:** il ramo `"ADMIN".equals(alias)` di `isPlatformAdmin()` (Batch 0) diventa *dead code* dopo il
  ritiro → rimuoverlo (o lasciarlo innocuo); e spostare il caso di test `admin_puoCancellareGlobale` su
  **SUPER_ADMIN** (che diventa l'unico platform-admin). Coordinare con l'aggiunta del test SUPER_ADMIN
  suggerita su Batch 0.

---

## Batch 2 — Governance catalogo globale (root-cause di P0) — **solo BE + seed** (FE verificato: nessun cambio)

**✅ Coupling FE VERIFICATO PULITO (Code, indipendente, 2026-07-19):** grep su tutto `src/app` (base checkout) →
i 3 metodi globali di `AlimentoService` (`create` POST `/api/alimenti_base`, `update` PUT, `delete` DELETE
`/{id}` — `services/alimento-service.ts:82/92/97`) hanno **zero call-site**. Flussi reali del nutrizionista:
create → `createPersonale` (`top-tab`/`modale-aggiungi-alimento`) o `importFromOFF` (`off-search-panel` → BE
`createPersonale`); delete → `deletePersonale` (`catalogo-tab`/`miei-alimenti-tab`). **Nessun flusso di EDIT
alimento nel FE** (nessun caller di `update`). ⇒ Revoca dei permessi globali + re-gate **NON rompe nulla nel FE
→ Batch 2 = solo BE + seed, nessuna modifica FE.** (DB drift-check: nessun drift, NUTRIZIONISTA ha i 3 globali.)

**Obiettivo:** separare personale (nutrizionista) da globale (control-plane) → l'over-grant sparisce anche a
livello di permesso (difesa in profondità oltre al guard service di Batch 0).

**Fix BE:**
- **Re-gate CRUD globali → nuovo permesso `CATALOGO_GLOBAL_MANAGE`** (su SUPER_ADMIN): `@PreAuthorize` su
  `create`/`update`/`delete` di `AlimentoBaseController`. Preferito allo spostamento sotto `/api/admin` (il
  controller serve anche i READ del nutrizionista → nessuno split URL). Verificato: i globali **non hanno caller
  BE interni** (create solo via `add`; OFF usa `createPersonale`) → re-gate safe.
- **Revoca `ALIMENTO_CREATE/UPDATE/DELETE` (globali) dal ruolo NUTRIZIONISTA** (tiene `ALIMENTO_READ` +
  `ALIMENTO_PERSONALE_CREATE`). Safe: `createPersonale`/`deletePersonale` gate-ano su `ALIMENTO_PERSONALE_CREATE`
  (non toccato). NON tocca `ALIMENTO_ALTERNATIVO_*`/`DA_EVITARE_*` (altri controller/famiglie).
- **Guard service-level su `create`** (spostato da Batch 0): `AlimentoBaseService.create` → `if
  (!isPlatformAdmin()) throw` (creazione globale = solo control-plane). Difesa in profondità oltre al gate.
- **`updatePersonale` (nuovo, BE):** update del proprio personale con check `created_by` (riusa
  `assertPuoModificareCatalogo`), gate `ALIMENTO_PERSONALE_CREATE`. ⚠️ **Nessun consumer FE oggi** (non esiste
  edit UI) → completamento BE forward-looking + testabile; editor FE = lavoro separato.
- **🔴 F-MACRO-UPDATE (bug pre-esistente confermato da Code — DA FIXARE qui):** `DtoMapper.updateAlimentoBaseFromForm`
  (`:670-673`) **sostituisce** il `Macro` gestito con uno nuovo da `toMacro` (`:865-889`, non imposta
  `createdAt`); `Macro.createdAt` è `@CreatedDate @Column(nullable=false)` → merge-as-update mette
  `created_at=NULL` → **500** (o collisione unique `alimento_base_id`). **Fix:** aggiornare il `Macro`
  **in-place** (mutare l'istanza gestita: preserva `id` + `created_at`), non rimpiazzarla con un detached
  `toMacro(...)`. ⚠️ **Serve sia per `updatePersonale` sia per l'`update` globale** (che dopo il re-gate userà
  il SUPER_ADMIN) → è in questo batch a prescindere. Oggi è **latente** (il FE non ha edit → non 500-a in prod),
  ma reale. Sblocca l'update-OK nel test.

**Seed/migrazione `020` (idempotente, manuale):** crea permesso `CATALOGO_GLOBAL_MANAGE` (gruppo ALIMENTI) →
SUPER_ADMIN; revoca i 3 `ALIMENTO_*` globali dal NUTRIZIONISTA. **A6 slitta a `021`.**

**Test:** `updatePersonale` proprio → OK (+ update-OK sbloccato dal fix Macro) / altrui → **403**; `create`
globale nutrizionista → **403**, con `CATALOGO_GLOBAL_MANAGE` → OK; suite verde. **FE: nessun cambio.**

**Verifica Code (a implementazione fatta):** (a) fix Macro **in-place** senza reintrodurre `@CreatedDate`
`created_at=NULL`; (b) parità permessi (nutrizionista 403 sui globali via gate + guard service; OK sui personali);
(c) `CATALOGO_GLOBAL_MANAGE` creato+assegnato nella `020`; (d) nessun caller BE interno dei globali rotto; (e) suite verde.

---

## Batch 3 — B3: `@PreAuthorize` mancanti (KB §7)

`PromemoriaController` (6 endpoint `:26,31,36,41,47,54` — nuovi `PROMEMORIA_*` o riuso `APPUNTAMENTO_*`);
`UtenteController.uploadLogo:121` (`UTENTE_PROFILE`); `SystemController.getStandardTags:21`
(`isAuthenticated()` o `ALIMENTO_READ`). Test di autorizzazione (403 senza permesso / OK con).

---

## Batch 4 — Finding di remediation residui (confermati, auth-independent — NON perderli)

Dalla self-review (report 9 finding / KB §7), **non** inclusi nei Batch 0–3, **da schedulare dopo Batch 3**
(sono auth-independent → **non** deferiti a Keycloak):
- **Important:** A5.3 parità 423 su `AppuntamentoService.update:94` e `.delete:111` (`assertNonLimitato`
  mancante dopo l'ownership); A7-DENIED su `AlimentoAlternativoService` (6 metodi → usare
  `getOwnedAlimentoAlternativo`) + `SchedaTemplateService.applicaAScheda:201` (→ `getOwnedSchedaFullDetails`,
  sana anche 404→403).
- **Minori:** riga `FAILURE` su EXPORT_PDF/DOWNLOAD (o audit dopo la generazione); `ClienteDto` over-fetch
  misurazioni/plicometrie sui 4 endpoint di mutazione + campo morto `schede`.

Dettaglio + fix in KB §7. **F-USER-DEL** (cascata `Utente.clienti` `orphanRemoval` che bypassa
`eliminaClienteCompleto`) = follow-up del **metodo canonico A6**, correttamente tracciato lì.

---

## Fuori scope (deferito a Keycloak)

- **E4 / break-glass su nutrizionisti reali** → impersonation **nativa Keycloak** (token-exchange + revoca
  sessioni). L'impersonation **demo** resta com'è. Verifica del sottosistema demo esistente = **SOLIDO_CON_GAP**
  (riferimento in RBAC-TARGET §E4).
- **Requisito GDPR portato avanti (criterio di accettazione Keycloak):** ogni accesso in impersonation ai dati
  art. 9 deve essere attribuito in **A7 all'operatore reale**, non al target (oggi non è così — gap #1
  verificato: `JwtAuthFilter:57-60` ignora `actorAdminId`, `AuditService` usa `getMe()`).

---

## Sequencing & verifica

**Batch 0 (P0)** → **Batch 1 (retire ADMIN)** → **Batch 2 (catalogo root-cause, se il FE lo consente)** →
**Batch 3 (B3)**. Build/test verdi tra i batch. A verde di ciascun batch, **Code esegue la verifica
avversariale** su quei file — in particolare: ADMIN storico **non** regredisce sul catalogo globale; nutrizionista
bloccato su globale/personale-altrui; flusso FE personale integro; nessun riferimento rotto ad ADMIN.

---

## Prompt di avvio (bootstrap per una sessione implementatore pulita)

> Sei l'agente implementatore su **Statera** (gestionale nutrizionisti, dati art. 9; BE Spring Boot/JPA/TiDB,
> FE Angular 21). Modello a due agenti: **tu scrivi il piano e implementi; l'agente Code verifica**. Prima di
> agire leggi `CLAUDE.md`, `nutrizionista_front/docs/KNOWLEDGE_BASE.md` (§2 vincoli, §4 metodo, §7 invarianti/
> gap), `docs/RBAC-TARGET.md` (design RBAC) e `docs/HANDOFF-RBAC-P0.md` (task + accettazione). Rispetta i
> **vincoli**: nessun commit/push; build+test verdi prima di "fatto"; difesa in profondità nel service;
> discriminante admin via **ruolo sull'entità** non authority; **auth target = Keycloak → non costruire
> macchinario custom-JWT** (E4/break-glass fuori scope). Task = **Batch 0 (P0)** poi **Batch 1–3 (RBAC
> pre-Keycloak)** come da HANDOFF. **Mettiti in plan mode, verifica i call-site sul codice reale (non
> indovinare), produci il piano e sottoponilo prima di implementare.** Per Batch 1 richiedi all'utente
> l'output della query DB sui permessi/ruoli. Implementa batch per batch, build/test verdi tra uno e l'altro;
> a fine batch l'agente Code rifà la verifica.
