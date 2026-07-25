# AUTH-ASSESSMENT — Analisi autenticazione/sessione Statera

> **Fase 1 — SOLO analisi.** Approfondimento dell'area B (autenticazione) del documento
> `COMPLIANCE-STATUS.md`. Nessuna modifica al codice.
>
> **Data**: 2026-07-12
> **Verifica online eseguita**: 2026-07-12 (Keycloak/Zitadel, Sign in with Apple — vedi §c).

## Nota sulle evidenze

Le citazioni **frontend** sono state ri-verificate in questa sessione. Le citazioni **backend** sono
marcate `[verificato in locale]` (repo backend non montato in questa sessione).

---

## a) Come funziona l'autenticazione oggi

**Login**
- `POST /api/auth/login` con email + password; in alternativa login/registrazione Google, verificati
  server-side via `GoogleTokenVerifier` `[verificato in locale]`.
- `AuthService` genera un **JWT HS256** (jjwt 0.12.6) con `subject = email` e `authorities` = alias dei
  permessi. **Non** contiene il tenantId. Segreto letto da `application.properties:72`. TTL **1 ora**
  (`JwtUtils.java`, `AuthController.java`) `[verificato in locale]`.

**Uso del token**
- Il frontend salva il JWT in **`localStorage`** (`login.ts:84,106`; letto in `auth-service.ts:57`).
- `authInterceptor` allega l'header `Bearer` alle sole chiamate verso il backend
  (`interceptors-interceptor.ts:8,11-17`).
- Il backend valida firma + scadenza in `JwtAuthFilter`, popola le authorities **dal token** (quindi
  potenzialmente *stale*), ma risolve il tenant **server-side** via `CurrentUserService.getMe()` — non
  dal token (scelta corretta) `[verificato in locale]`.

**Refresh / logout / sessione**
- **Refresh: inesistente** — nessun refresh token, nessun handler 401 nel frontend.
- **Logout: no-op lato server** (`AuthService.logout()` vuoto) → il token resta valido fino a 1 ora dopo
  il logout; il frontend si limita a svuotare `localStorage` + hard reload `[verificato in locale]`.
- **Nessun route guard** Angular: `app.routes.ts` non contiene alcun `canActivate` (verificato in questa
  sessione) → si può navigare su qualunque schermata senza login (le chiamate dati falliscono, ma la UI
  si apre).
- `checkAuthLocalStorage` è **definito ma mai invocato** (`auth-service.ts:56`, unica occorrenza) → la
  scadenza del token non viene mai verificata proattivamente lato client.

---

## b) Diagnosi dei rischi (per gravità)

1. **`jwt.secret` pubblico** → token forgiabili, bypass totale dell'autenticazione (vedi B1).
2. **Token in `localStorage`** → esfiltrabile via XSS; oggi ci sono **7 vulnerabilità XSS Angular high**
   aperte (vedi B5) → la combinazione è particolarmente pericolosa.
3. **Token non revocabili + logout non effettivo** → nessun modo di invalidare un token compromesso prima
   della scadenza; un token rubato resta valido fino a 1 ora.
4. **Nessun route guard / nessuna gestione del 401** → UX rotta e sessione scaduta non gestita.
5. **Authorities *stale* nel token** → una modifica di permessi non ha effetto finché il token non scade.
6. **No MFA, no policy password, no lockout, no reset** (vedi B2).
7. **Backend HTTP, no CSP/HSTS** (vedi A3/B6).
8. **IDOR** — broken access control a livello di istanza (vedi A1).

**Contesto mitigante.** Il prodotto è in fase di sviluppo: gli unici utenti con password sono 2 account
di test (admin + nutrizionista, password "password"). Non esistono utenti reali. Per questo la migrazione
descritta sotto è **roadmap "prima della produzione"**, non un intervento d'emergenza. Le voci che vanno
comunque chiuse prima del go-live sono nella *Checklist "Prima della produzione"* di `COMPLIANCE-STATUS.md`.

---

## c) Opzioni architetturali a confronto

| Criterio | **A** — Sessioni server (Spring Session + Redis, cookie HttpOnly) | **B** — IdP OIDC self-hosted UE (Keycloak/Zitadel) + pattern BFF | **C** — JWT custom irrobustito |
|---|---|---|---|
| Sforzo di migrazione | Basso-medio | Medio-alto (stand-up IdP, import utenti) | Basso all'inizio, cresce nel tempo |
| Revoca / logout reale | ✅ nativa | ✅ nativa | ❌ da costruire (blocklist) |
| Token fuori dal browser (anti-XSS) | ✅ cookie HttpOnly | ✅ cookie HttpOnly via BFF | ❌ resta in localStorage |
| MFA / passkey | ❌ da costruire | ✅ pronte nell'IdP | ❌ da costruire |
| Reset password / lockout / policy | ❌ a mano | ✅ pronti | ❌ a mano |
| Login social (Google, Apple) | ❌ da integrare a mano | ✅ provider built-in | 🟡 come oggi, custom |
| Codice di sicurezza da manutenere in proprio | poco | quasi zero | **massimo** |
| Infra aggiuntiva | Redis | container IdP + suo DB | nessuna |
| Adeguatezza dati sanitari | media | **alta** | bassa |

### Verifica online (2026-07-12)

- **Keycloak** — licenza **Apache 2.0**, backed da Red Hat. Ultima release **26.7.0 (luglio 2026)**,
  cadenza ~4 minor/anno. **Google e Apple disponibili come identity provider built-in** (Apple integrato
  da Keycloak 24+). Guide ufficiali per il reverse proxy (HAProxy/Traefik). È il candidato con l'ecosistema
  più maturo e la licenza più permissiva.
- **Zitadel** — con la **v3 (2025) la licenza principale è passata da Apache 2.0 ad AGPL 3.0** (con
  carve-out MIT/Apache per SDK e alcune directory; licenza commerciale disponibile). Scritto in Go,
  **multi-tenancy nativa** (organizations) e **audit trail integrato** — quest'ultimo rilevante per il gap
  A7. Da valutare se il vincolo AGPL è accettabile per il vostro modello di distribuzione.
- **Sign in with Apple** — richiede l'**Apple Developer Program ($99/anno)**. A differenza degli altri
  provider, il *client secret* non è statico: è un **JWT firmato** con una private key (Services ID +
  key), **da rigenerare periodicamente** → onere operativo ricorrente da mettere in conto. In Keycloak si
  configura come identity provider Apple built-in.

---

## d) Raccomandazione

**Target: Opzione B — IdP OIDC self-hosted in UE + pattern BFF.** Trattandosi di dati sanitari,
MFA/passkey, revoca, lockout, reset e audit dei login non sono opzionali (art. 32 + provvedimenti
Garante); con un team piccolo conviene che li fornisca un prodotto maturo anziché scriverli e manutenerli
in casa. Il pattern **BFF** con cookie HttpOnly + `SameSite=Lax` (Lax, non Strict: evita il blocco del cookie sul
redirect-return OIDC — vedi `HANDOFF-KEYCLOAK.md`) elimina strutturalmente il token dal browser →
chiude alla radice sia il rischio "JWT in localStorage" sia il CSRF.

- **Candidato primario: Keycloak** — licenza permissiva (Apache 2.0), ecosistema maturo, Google e Apple
  come provider built-in, guide reverse proxy ufficiali.
- **Alternativa: Zitadel** — se l'**audit trail integrato** (che risponderebbe anche ad A7) e la
  multi-tenancy nativa pesano più del vincolo di licenza **AGPL 3.0**.
- **Fallback: Opzione A** (Spring Session + Redis + cookie HttpOnly) — se mettere in piedi e manutenere un
  IdP risultasse troppo oneroso per l'infra (ancora da decidere, C3). Dà revoca/logout-ovunque nativi con
  poco codice, ma **MFA e social login vanno aggiunti a parte**.
- **Opzione C sconsigliata** come destinazione: massimo codice di sicurezza custom = massimo rischio per un
  team piccolo.

**Login social**: **Google mantenuto** e **Apple da aggiungere** — entrambi federati nell'IdP (in
Keycloak sono provider built-in). Mettere in conto l'Apple Developer Program ($99/anno) e la rotazione
periodica del client secret Apple.

**Fix frontend indipendenti dalla scelta** (proponibili subito come quick win, ~30 righe totali):
- **Route guard** `canActivate` sulle rotte private → redirect a `/login` se il token manca o è scaduto.
- **Interceptor di risposta**: sul 401 dal backend, pulire il token e riportare al login (copre il caso
  "token scaduto a metà sessione", oggi non gestito).
- Nota importante: queste sono migliorie di **UX/igiene, non di sicurezza** — la sicurezza vera resta sul
  backend, perché tutto ciò che sta nel browser è manipolabile dall'utente.

---

## e) Piano di migrazione ad alto livello (senza implementare)

1. **Tappa 0 — rotazione `jwt.secret`** (vale per qualunque opzione, vedi B1): fix immediato/indipendente.
2. **Ponte** — introdurre cookie HttpOnly + un refresh token **revocabile server-side**, per ridurre da
   subito l'esposizione del token nel browser.
3. **Stand-up dell'IdP in UE** (Keycloak) — con **import degli hash BCrypt esistenti** (gli utenti non
   perdono la password) e **federazione di Google e Apple** come identity provider.
4. **Rollout graduale** — doppio binario token/sessione dietro **feature flag**, con finestra di
   convivenza per non invalidare le sessioni esistenti.
5. **Dismissione** del vecchio filtro JWT custom una volta completata la migrazione.