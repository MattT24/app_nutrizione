# HANDOFF — Impianto Keycloak (autenticazione allo stato dell'arte)

> **Modello a due agenti:** l'implementatore scrive il **piano di design** (in plan-mode) e lo gira a **Code**
> per la verifica **prima** di implementare; poi implementazione fasata con verifica per fase. **Fonti
> autoritative:** `docs/AUTH-ASSESSMENT.md` (opzione **B** decisa: Keycloak + **BFF**; piano migrazione §e),
> `docs/RBAC-TARGET.md` (modello a **2 ruoli** che l'IdP importa), `nutrizionista_front/docs/KNOWLEDGE_BASE.md §6`,
> hosting = **Clever Cloud** (deciso 2026-07-22). **Vincolo cardine:** Keycloak **sostituisce** il custom-JWT →
> **NON** costruire nuovo macchinario custom-JWT; il vecchio `JwtAuthFilter`/`JwtUtils` si **dismette** a
> migrazione completa.

## ⚠️ Regola trasversale (non negoziabile, richiesta utente 2026-07-23)
1. **Verifica online sulla documentazione UFFICIALE ad OGNI passo** (keycloak.org/docs, docs.spring.io/spring-security,
   keycloak/keycloak-quickstarts). Gli aggregatori terzi lag-gano (es. davano Keycloak 26.6.4 mentre l'ufficiale
   era già 26.7.0) → **pinnare/decidere solo su fonte ufficiale**.
2. **Aderenza alle best-practice ufficiali di QUESTA stack** (Keycloak + Spring Security + BFF), non guide
   generiche/obsolete (vedi §"Best-practice verificate": adapter Keycloak legacy = deprecato/rimosso).
3. **Versioni pinnate e tracciate** (vedi §Versioning) + **ri-verifica dell'ultima patch all'impianto** (cadenza
   Keycloak ~bisettimanale per le patch, ~4 minor/anno).
4. **Per qualsiasi passaggio extra/decisione da definire → chiedere all'utente** (non assumere).

## Versioning tracciato (verificato online 2026-07-23)
| Componente | Versione | Note |
|---|---|---|
| **Keycloak (IdP)** | **26.7.0** | Ultima stabile su keycloak.org/downloads (2026-07-23). Supporto **rolling** solo sull'ultima major (no LTS congelato) → all'impianto ri-verificare l'ultima patch 26.x. |
| **Spring Boot** | **3.5.14** | Esistente (CLAUDE.md). ⚠️ Boot 3.5 EOL 30/06/2026 → la migrazione Boot 4 resta intervento a sé. |
| **Spring Security** | **6.x** | Bundled con Boot 3.5. Fornisce OAuth2 nativo (client + resource-server). |
| **Java** | **17** | Esistente. |
| **Dipendenze auth (best-practice)** | `spring-boot-starter-oauth2-resource-server` + `spring-boot-starter-oauth2-client` | ⚠️ **NIENTE** `keycloak-spring-boot-starter`/adapter (deprecato Keycloak 20, **rimosso**). |
| **Angular (FE)** | **21** | Esistente; pinnato ^21 (mai v22). |
| **Reference impl** | `keycloak/keycloak-quickstarts` | Repo ufficiale di esempi (usarlo come riferimento, non copiare cieco). |

## Best-practice verificate online (2026-07-23) — vincolanti
- **Integrazione Spring: SOLO Spring Security nativo.** L'adapter Keycloak per Spring Boot è **deprecato da
  Keycloak 20 e rimosso**; guide con `KeycloakWebSecurityConfigurerAdapter`/`keycloak-spring-boot-starter` sono
  obsolete. Usare `spring-security-oauth2-resource-server` (validazione JWT lato API) + `oauth2-client`/`oauth2Login`
  (lato BFF), con un **`JwtAuthenticationConverter`/`GrantedAuthoritiesMapper` custom** che mappa i ruoli/permessi
  Keycloak → authorities Spring. Vantaggio: **portabilità** su qualsiasi IdP OIDC, zero dipendenze keycloak-specifiche.
- **BFF (Backend-for-Frontend):** la SPA **non** è un client OAuth2 "public" (nemmeno con PKCE). Il **BFF è il
  confidential client** che fa l'OAuth2 con Keycloak, **tiene access+refresh token lato server**, e rilascia alla
  SPA un **cookie di sessione** `Secure` + `HttpOnly` + `SameSite=strict` (**senza attributo `Domain`** → legato
  all'host). Ogni chiamata della SPA passa dal BFF (**same-origin**, es. `/bff/api/...`): il BFF valida il cookie,
  allega il token, inoltra all'API. Elimina strutturalmente il token dal browser (anti-XSS) e chiude il CSRF.
- **CSRF/Angular:** Angular imposta `X-XSRF-TOKEN` in automatico **solo su URL same-origin** (senza authority) →
  chiamare `/bff/...` (path relativo), non `http://host/bff/...`.
- Riferimenti BFF: Spring Security OAuth2 + BFF (spring.io), Curity/OWASP SPA best-practices (BFF come strategia
  anti-XSS). BE-as-BFF con `oauth2Login` **oppure** BFF via Spring Cloud Gateway → **decidere nel piano**.

## Fasatura
- **Fase 0 (subito, indipendente):** rotazione `jwt.secret` forte ≥256-bit casuale (B1) — vale in ogni scenario.
- **Fase 1 — DESIGN (questo step):** il piano (deliverable dell'implementatore → verifica Code).
- **Fase 2 — DEV (NON gated):** Keycloak in dev (docker-compose) + realm/client + refactor BFF (Spring Security
  nativo) + import hash BCrypt + mapping RBAC→IdP + **feature-flag doppio-binario**; suite verde.
- **Fase 3 — PROD cutover (GATED sul provisioning Clever Cloud):** deploy IdP UE + reverse-proxy same-origin/HTTPS
  (A3) + CSP/HSTS (B6) + MFA/policy/lockout (B2) attivi + **dismissione custom-JWT**.

## Aree che il PIANO deve coprire (ognuna: decisione + razionale + link doc ufficiale)
1. **Topologia BFF** — BE-as-BFF (Spring `oauth2Login`, emette il cookie) vs BFF separato (Spring Cloud Gateway).
   *(Steer: BE-as-BFF, più semplice per team piccolo + same-origin reverse-proxy.)* + come cambia il FE
   (`authInterceptor`/`localStorage` → cookie; guard/401 già presenti).
2. **Realm/client Keycloak** — realm Statera, client(s), lifetime access/refresh, **revoca sessione** nativa.
3. **Mapping RBAC→IdP** — 2 ruoli (NUTRIZIONISTA/SUPER_ADMIN) + permessi → realm/client-roles o groups; **come le
   authority (solo-permessi) entrano nel claim**; ⚠️ **preservare** il discriminante-**via-ruolo** (mai
   `hasAuthority` sul ruolo) e la **risoluzione tenant server-side** (`CurrentUserService`, NON dal token).
4. **Import utenti** — hash **BCrypt** esistenti importati (nessuna ri-registrazione); federazione **Google**
   (mantenuto) + **Apple** (⚠️ **decisione: ora o post-cutover?** $99/yr Apple Developer + rotazione client-secret).
5. **MFA/passkey, policy password, lockout, reset** (B2) — configurati nell'IdP.
6. **Migrazione/rollout** — doppio-binario dietro **feature flag** + finestra convivenza + **rollback**. ⚠️
   **Decisione: "Ponte" (cookie+refresh interim del §e) o DIRECT a Keycloak?** *(Steer: **direct** — la Ponte
   serviva a IdP gated; ora no → evita macchinario usa-e-getta.)*
7. **GDPR / criteri di accettazione (carry-forward, VINCOLANTI):** (a) **break-glass/impersonation nativo Keycloak**
   (token-exchange + revoca) con **attribuzione A7 dell'operatore REALE**; (b) **audit dei login** (Keycloak events
   login/logout/MFA/lockout) → come confluiscono in A7 o log equivalente; (c) residenza **UE** dell'IdP; (d) impatto
   sui testi legali/DPA (`LEGAL-REVIEW §5/§6/§12`).
8. **Topologia prod** — dove sta l'IdP (sottodominio `auth.*` vs path), same-origin FE+`/api`, TLS (A3), CSP/HSTS (B6).

## Vincoli (invariati)
Nessun commit/push; **niente custom-JWT nuovo**; build+test **foreground**; **difesa in profondità server-side**
(l'IdP autentica, ma ownership/tenant/limitazione/A7 restano enforced nel service); **reversibilità** (feature-flag
+ rollback); durante la migrazione **nessuna regressione** su IDOR/limitazione/A7 già chiusi.

## Verifica Code — per fase
- **Piano (Fase 1):** completezza 8 aree; mapping RBAC→IdP preserva discriminante-via-ruolo + tenant server-side;
  fattibilità import BCrypt; criteri GDPR (A7-attribution operatore reale, login-audit) espliciti come acceptance;
  decisione Ponte-vs-direct presa; rollback presente; **ogni scelta ancorata a doc ufficiale citato**.
- **Dev (Fase 2):** token **fuori dal browser** (cookie HttpOnly, niente `localStorage`); BE valida via IdP **ma**
  ownership/tenant/limitazione/A7 restano server-side (ri-eseguo anti-leakage/limitazione/audit test → **zero
  regressioni**); nessuna dipendenza keycloak-adapter legacy; feature-flag reversibile; suite verde.
- **Cutover (Fase 3):** same-origin/HTTPS, CSP/HSTS, MFA/lockout attivi, A7 attribuisce l'operatore reale in
  impersonation, custom-JWT dismesso; verifica su Clever Cloud reale.

## Decisioni aperte (l'implementatore le presenta nel piano con raccomandazione → l'utente decide al review)
BFF topology (BE-as-BFF vs Gateway) · Apple ora/dopo · **Ponte vs direct** · dove sta l'IdP (sottodominio vs path) ·
Fase 0 `jwt.secret` ora.

## Fonti ufficiali (verificate 2026-07-23)
- Keycloak — keycloak.org/downloads (26.7.0), keycloak.org/docs, github.com/keycloak/keycloak-quickstarts,
  github.com/keycloak/keycloak/discussions/10187 (deprecazione adapter Spring).
- Spring Security OAuth2 — docs.spring.io/spring-security (resource-server + oauth2-client/BFF).
- BFF — Baeldung (Spring Cloud Gateway BFF OAuth2), Curity/OWASP SPA best-practices.
