# RBAC-TARGET — Modello ruoli target + decisioni

> **Documento di decisione (non codice).** Fissa la struttura gerarchica dei ruoli target di Statera e le
> decisioni prese, come input per il **piano di migrazione RBAC** (autore: agente implementazioni; verifica:
> agente Code). Decisioni prese con l'utente il **2026-07-19**.
>
> Riferimenti: `docs/COMPLIANCE-STATUS.md` (B3), `docs/ROADMAP-FASE3.md`,
> `nutrizionista_front/docs/KNOWLEDGE_BASE.md` (§7 gap invarianti, P0).

---

## 1. Perché

Oggi coesistono **due concetti di admin incoerenti** e un **over-grant** di permessi:
- **`ADMIN`** (id 1, seed `005`): ha **tutti i permessi granulari** → di fatto un *"super-nutrizionista"* che
  può leggere/scrivere i dati app di **tutti** i nutrizionisti, ma **non** entra in `/api/admin/**` (gli manca
  il permesso `SUPER_ADMIN`). La sua authority-string `ADMIN` **non è controllata da nessuno** (verificato:
  nessun `hasRole/hasAuthority('ADMIN')`) → concetto di admin **inerte/legacy**. L'account "admin" dev è qui
  (`005:159`, utente "Demo" su `@r_admin`).
- **`SUPER_ADMIN`** (id 60001, `SuperAdminSeeder`, 13/07): ha **solo** il permesso `SUPER_ADMIN` → gate
  `/api/admin/**`, ma **non** i permessi granulari dell'app → non può usare il gestionale.
- **`NUTRIZIONISTA`** (id 2): funzioni app, ma **over-granted** con `ALIMENTO_CREATE/UPDATE/DELETE` globali
  (causa di **P0** — IDOR write/delete sul catalogo, vedi KB §7).

Fatti verificati: le authority sono costruite **solo dai permessi** (`UserDetailsServiceImpl:44-46`), i ruoli
non sono emessi come authority; l'**impersonation** esiste ma è **demo-only** (`AdminDemoAccountService.impersona`,
claim JWT `impersonation`, time-boxed, revoca server-side); il ruolo ADMIN è referenziato **solo** in seed/
migrazioni (`005` crea + account dev; `006/008/013` grant), **nessun codice Java** dipende dal ruolo ADMIN.

**Best practice adottata:** separazione **control plane / tenant plane** con **least privilege** e
**break-glass auditato** per l'accesso eccezionale ai dati del tenant (niente "god-mode" ambientale su dati
art. 9 — art. 32, minimizzazione + separation of duties).

## 2. Struttura gerarchica target (2 ruoli)

| Ruolo | Piano | Cosa può | Cosa NON può |
|---|---|---|---|
| **NUTRIZIONISTA** | tenant / app | Tutte le funzioni cliniche **sui propri dati** (ownership + limitazione già enforced); crea/edita i **propri alimenti personali**. **Unico** ruolo che naviga il gestionale. | Toccare il catalogo **globale**; entrare nel control plane. |
| **SUPER_ADMIN** | control plane | `/api/admin/**`: account/demo, assistenza, **audit read (A7 globale)**, monitoraggio; (eventuale) gestione del **catalogo globale**; **break-glass impersonation** auditata. | Accesso **ambientale** ai dati clinici dei tenant (ci arriva **solo** via break-glass). Nessun permesso app granulare. |
| ~~ADMIN~~ | — | **RITIRATO**. | — |

## 3. Decisioni prese (2026-07-19)

- **D1 — Due ruoli.** NUTRIZIONISTA (tenant) + SUPER_ADMIN (control plane). **ADMIN ritirato.**
- **D2 — Account "admin" dev → NUTRIZIONISTA** (è un tester dell'app: oggi naviga il gestionale).
- **D3 — Catalogo globale = concern di piattaforma.** Alimentato da **seed + import OFF**; editing manuale
  eventuale **solo** control-plane (SUPER_ADMIN). Il NUTRIZIONISTA tiene **solo** i propri alimenti personali
  → **rimuovere `ALIMENTO_CREATE/UPDATE/DELETE` (globali) dal ruolo NUTRIZIONISTA** (tiene `ALIMENTO_READ` +
  `ALIMENTO_PERSONALE_CREATE`). Chiude **P0 alla radice** (oltre alla patch service-level già prevista).
- **D4 — Accesso operatore ai dati clinici = nessun accesso ambientale + break-glass.** SUPER_ADMIN **non**
  vede mai i dati clinici di default; unica via = **impersonation esplicita, time-boxed, auditata A7**
  (estende il meccanismo demo ai nutrizionisti reali). SUPER_ADMIN **non** ha permessi app granulari.

## 4. Decisioni ingegneristiche (raccomandate — da ratificare nel piano di migrazione)

- **E1 — Set permessi SUPER_ADMIN (control plane):** `SUPER_ADMIN` + assistenza (admin) + demo (admin) +
  **`AUDIT_READ`** (audit globale, oggi su ADMIN via `013`) + (se D3 prevede editing manuale) un permesso
  dedicato **catalogo-globale** (es. `CATALOGO_GLOBAL_MANAGE`). **NON** i permessi clinici granulari.
- **E2 — Set permessi NUTRIZIONISTA (ripulito):** invariato tranne rimozione dei 3 `ALIMENTO_*` globali
  (D3). Tiene `ALIMENTO_READ`, `ALIMENTO_PERSONALE_CREATE`, e il resto delle funzioni cliniche.
- **E3 — Catalogo globale nel codice:** `AlimentoBaseService.create/update/delete` diventano control-plane
  (gate su ruolo SUPER_ADMIN o su `CATALOGO_GLOBAL_MANAGE`); aggiungere **`updatePersonale`** (oggi manca:
  c'è `createPersonale`/`deletePersonale` ma non l'update del personale) così il nutrizionista edita i propri.
- **E4 — Break-glass impersonation nutrizionisti reali. ⚠️ DEFERITO A KEYCLOAK (deciso 2026-07-19).**
  **Vincolo auth:** il target è **Keycloak** (opzione B; **JWT custom HS256 scartato**, è uno stopgap — vedi
  KB §6 / ROADMAP). Keycloak porta **impersonation/token-exchange e revoca sessioni nativi**. Perciò **NON si
  costruisce macchinario custom-JWT usa-e-getta** per il break-glass sui nutrizionisti reali (nuovo
  `accountType`, kill-switch/`tokenVersion` su `Utente`, tabella sessioni, token sender-constrained, rami di
  `DemoTokenValidationService`): sarebbe buttato via alla migrazione. Inoltre siamo **pre-produzione** (nessun
  nutrizionista/paziente reale) → il break-glass sui reali **non serve prima di Keycloak**. **Decisione:** E4
  (break-glass su utenti reali) **non entra nel task RBAC pre-Keycloak**; l'impersonation **demo** resta com'è
  (per gli account demo). Il break-glass "vero" nasce **nativo in Keycloak**, con l'attribuzione A7 all'operatore
  reale come **criterio di accettazione della migrazione Keycloak** (requisito GDPR, sotto). La verifica del
  sottosistema esistente (sotto) resta come **riferimento**: cosa è riusabile e quali requisiti Keycloak deve
  soddisfare.
  - **Requisito GDPR portato avanti (auth-independent, criterio di accettazione Keycloak):** qualsiasi accesso
    in impersonation ai dati art. 9 DEVE essere attribuito in **A7 all'operatore reale** (non al target), con
    traccia inizio/fine + motivo + notifica al soggetto. Oggi **non** è così (gap #1 sotto): da risolvere
    quando il break-glass reale sarà implementato (in Keycloak).
  - Verifica dettagliata del sottosistema **demo** esistente (Code, review avversariale 4 lenti 2026-07-19):
    **verdetto SOLIDO_CON_GAP** (riferimento per la migrazione, non da estendere ora su utenti reali).
  - **✅ Riusabile as-is (solido):** gate `SUPER_ADMIN` (filter + `@PreAuthorize`); **doppia autenticazione**
    admin-password + master-password (BCrypt, `&` non short-circuit, **fail-closed** se master non configurata);
    **token JWT a scadenza assoluta 15 min** (`JwtUtils.generateJwtToken`, non demo-specifico); **revoca
    server-side** via `tokenVersion` + re-check che `actorAdminId` sia ancora SUPER_ADMIN ad ogni richiesta;
    rate-limit (`DemoRateLimitService`, generico adminId+ip); audit append-only senza segreti; motivo
    obbligatorio (10–500 char). **Punto decisivo:** il token porta già le **authorities PIENE del ruolo del
    target** (`DemoAuthService.creaRisposta:75-77`) → un SUPER_ADMIN che impersona un NUTRIZIONISTA ottiene
    lo scope pieno per *agire* (la dicitura "privilegi ridotti" è fuorviante: è solo de-escalation da
    SUPER_ADMIN + time-box).
  - **🔴 GAP DA CHIUDERE per un break-glass impeccabile su utenti reali:**
    1. **Accountability A7 (critico):** durante l'impersonation il `SecurityContext` ha come principal il
       **target** (`JwtAuthFilter:57-60` ignora `actorAdminId`) → `AuditService` A7 usa `getMe()` e attribuisce
       **tutte le azioni cliniche al nutrizionista impersonato, non all'operatore reale** → l'impersonation è
       **invisibile** nell'audit accessi art. 9 (l'opposto di ciò che serve). **Fix:** propagare `actorAdminId`
       nel `SecurityContext` (details/principal custom) + **estendere `AuditLog`** con `actor_admin_id` +
       flag `impersonation` (append-only preservato) → ogni riga dice "admin X ha agito impersonando Y su
       paziente Z".
    2. **Correlazione audit:** l'apertura del break-glass è registrata **solo** in `AuditAccountDemo` (silo
       demo), non in A7; nessun evento di **fine** sessione. **Fix:** scrivere anche in A7 la riga di apertura
       (attore reale + motivo + target) + traccia "dalle..alle..".
    3. **Decoupling da `CredenzialeDemo`:** il flusso parte da un id `CredenzialeDemo` (4 punti: selezione
       target, `creaRisposta` che esige demo non-null, validazione `accountType==DEMO`, revoca via
       `tokenVersion` su `CredenzialeDemo`); un nutrizionista reale non ha `CredenzialeDemo`. **Fix:** percorso
       parallelo che targettizza `Utente` (`POST /api/admin/users/{utenteId}/impersonate`) + nuovo
       `accountType="IMPERSONATION"` con ramo di validazione dedicato; **estrarre un builder di authorities
       condiviso** (oggi duplicato in `DemoAuthService.creaRisposta` e `AuthService.buildLoginResponseFor`).
    4. **Kill-switch per utenti reali:** `Utente` non ha `tokenVersion`/`enabled` → nessuna revoca prima dei
       15 min. **Fix (consigliato B):** tabella dedicata "sessioni impersonation" (id sessione nel claim,
       stato revocabile, inizio/fine/attore) — più pulita del toccare `Utente`.
    5. **Consenso/notifica (GDPR):** nessuna notifica al nutrizionista impersonato. **Fix:** notifica ex-post
       (+ eventuale dual-control) + base giuridica documentata (art. 6/9) + registro consultabile dal titolare.
    6. **Minori:** rate-limit in-memory non distribuito e conta solo i fallimenti (nessun tetto alle
       impersonation *riuscite*); token bearer non sender-constrained (replay entro 15 min → mitigato da TTL +
       `tokenVersion`); ramo impersonation ignora `disabledAt`/`expiresAt` diretti (coperto da `tokenVersion`);
       lieve incoerenza role-alias vs authority nel check attore; nessun endpoint di "stop impersonation".
  - **Nota di scope:** se si preferisce un break-glass **read-only** invece di full act-as, filtrare le
    authorities quando `impersonation=true` (whitelist di sola lettura) — ma l'attribuzione A7 all'operatore
    reale resta comunque obbligatoria per qualsiasi accesso. Decisione da prendere nel piano.
- **E5 — Assorbe i finding B3** (KB §7): `@PreAuthorize` su `PromemoriaController` (nuovi `PROMEMORIA_*` o
  riuso `APPUNTAMENTO_*`), `UtenteController.uploadLogo` (`UTENTE_PROFILE`), `SystemController.getStandardTags`;
  e il **gemello `create`-globale** (già coperto da E3).

## 5. Vincoli / invarianti da preservare

- **Least privilege:** nessun ruolo "tutti i permessi" (l'anti-pattern ADMIN scompare).
- **Tenant plane invariato:** ownership (`OwnershipValidator`) + limitazione (`assertNonLimitato`) restano
  enforced sui nutrizionisti.
- **Ogni accesso art. 9 auditato (A7):** il break-glass è un **evento auditato**; l'impersonation NON deve
  bypassare l'audit.
- **SUPER_ADMIN non self-registrabile** (già così: `AuthService.register` assegna sempre NUTRIZIONISTA).
- **Migrazione portabile:** niente `ON DELETE CASCADE` DB nuovi; SQL idempotente; invalidazione dei 2 token
  dev = impatto nullo (decisione B1 pregressa).

## 6. Sketch di migrazione (per il piano dell'agente implementazioni — non esaustivo)

**SQL (nuova migrazione RBAC, numerata dopo `019`):**
- Revoca `ALIMENTO_CREATE/UPDATE/DELETE` dal ruolo NUTRIZIONISTA (`ruoli_permessi`).
- Migra gli utenti da ruolo ADMIN → NUTRIZIONISTA (atteso: solo l'account dev — **confermare con la query DB**).
- Rimuovi il ruolo ADMIN + le sue `ruoli_permessi`.
- Assegna a SUPER_ADMIN i permessi control-plane mancanti (E1).
- Bonifica lo stray `ROLE_ADMIN` legacy in `migrations/permessi orario_studio.sql:50`.

**Codice:**
- `AlimentoBaseService.create/update/delete` → control-plane; `updatePersonale` nuovo per il nutrizionista;
  `@PreAuthorize` dei CRUD globali su permesso admin (non `ALIMENTO_*`).
- Estensione impersonation ai nutrizionisti reali (E4).
- B3 (E5).

**Test:** nutrizionista non raggiunge globale/control-plane (403); operatore senza permessi app; break-glass
auditato (A7); account dev funziona come NUTRIZIONISTA; ADMIN retire non rompe seed/migrazioni.

## 7. Dipendenze / da confermare col DB (query rimandata, no client MySQL locale)

Query read-only `ALIMENTO_*`/permessi per ruolo + "chi è su ADMIN": serve al **piano di migrazione** (non a P0).
Atteso: unico utente su ADMIN = account dev; SUPER_ADMIN = solo permesso `SUPER_ADMIN`.

## 8. Sequencing e relazione con P0

1. **P0 ora** (fix IDOR `AlimentoBaseService.update/delete`, Opzione A service-level): discriminante
   `isPlatformAdmin()` = **ruolo ∈ {"ADMIN","SUPER_ADMIN"}** — **forward-compatible**: dopo il ritiro di ADMIN
   resta corretto (solo SUPER_ADMIN). P0 è la **patch interim + difesa in profondità**; questo task RBAC aggiunge
   poi il gate a livello di **permesso** (defense-in-depth su due strati).
2. **Task RBAC dedicato pre-Keycloak** (questo doc → piano dell'agente implementazioni → verifica Code):
   assorbe **solo la parte auth-independent** → D1–D3 (2 ruoli, retire ADMIN, governance catalogo) + E1–E3, E5
   (perm set + catalogo nel codice + B3). **E4/break-glass su utenti reali = DEFERITO a Keycloak** (non
   costruire macchinario custom-JWT usa-e-getta). Rischio proprio (seed/account) → task a sé.
3. **A6** dalla Fase 1 reale (dopo, o in parallelo se non collide).

## 9. Fuori scope (per ora)

Ruoli aggiuntivi (segretari/collaboratori), MFA/policy password (arrivano da Keycloak), consolidamento con
l'IdP (Keycloak importerà questa gerarchia). Il modello a 2 ruoli è comunque compatibile con la futura
migrazione auth.

**⚠️ Principio (vincolo Keycloak):** l'auth target è **Keycloak** (JWT custom scartato). Nel task RBAC
pre-Keycloak **non costruire macchinario custom-JWT** che l'IdP rimpiazzerà (impersonation/token-exchange,
revoca sessioni, kill-switch token, sender-constrained). Fare solo ciò che è **auth-independent** (design
ruoli, ownership, governance catalogo, schema/requisiti audit) e portare avanti i requisiti (es. attribuzione
A7 dell'operatore reale in impersonation) come **criteri di accettazione della migrazione Keycloak**.
