# Keycloak (DEV) — Statera

Artefatti di **sviluppo locale** per la migrazione dell'autenticazione a
**Keycloak 26.7** (IdP OIDC, pattern **BFF**). Nessuno di questi file tocca il
codice Java, il `pom.xml` o `application.properties`, né influenza `./mvnw test`.

> ⚠️ **SOLO-DEV.** Il client secret e tutte le password presenti qui (admin,
> utente di test) sono valori di comodo per lo sviluppo locale. **Mai in
> produzione**: in prod si usano segreti forti generati e gestiti fuori dal
> repo (variabili d'ambiente / secret manager), DB gestito esterno e TLS.

## Contenuto

| File | Scopo |
|---|---|
| `../../docker-compose.keycloak.yml` | Avvia Keycloak 26.7.0 in dev con import automatico del realm. |
| `statera-realm.json` | Export minimale del realm `statera` (client BFF, ruoli, policy, brute force, eventi, IdP Google placeholder, utente di test). |

## Avvio

Dalla root del progetto backend (`ProgettoNutrizionista/`):

```bash
docker compose -f docker-compose.keycloak.yml up
# in background:  docker compose -f docker-compose.keycloak.yml up -d
# stop:           docker compose -f docker-compose.keycloak.yml down
```

- **Admin console:** http://localhost:8081 — utente `admin`, password `admin`.
- La porta host è **8081** (→ 8080 nel container) per non collidere col backend
  Spring Boot su `http://localhost:8080`.
- Il realm **`statera`** viene **importato in automatico** all'avvio
  (`start-dev --import-realm`, leggendo `statera-realm.json` montato in
  `/opt/keycloak/data/import`).
- **Issuer del realm:** `http://localhost:8081/realms/statera`

> Nota storage: in dev si usa lo storage embedded `dev-file` (nessun DB
> container separato). In produzione serve `start` (production mode) con DB
> gestito esterno + hostname/TLS.

## Cosa contiene il realm

- **Client `statera-bff`** — confidential (`publicClient: false`),
  Authorization Code + **PKCE S256**, `directAccessGrants` disabilitato.
  - `secret`: `dev-secret-change-me` (**solo-dev**).
  - `redirectUris`: `http://localhost:8080/login/oauth2/code/keycloak` (default
    di Spring Security per il registration id `keycloak`) + `http://localhost:8080/*`.
  - `webOrigins`: `http://localhost:8080`, `http://localhost:4200`.
  - Attributi: `post.logout.redirect.uris`, `backchannel.logout.url`
    (`http://localhost:8080/logout/connect/back-channel/keycloak`).
- **Ruoli realm:** `NUTRIZIONISTA`, `SUPER_ADMIN` (allineati a `RBAC-TARGET.md`).
- **Password policy:** `length(12) and upperCase(1) and lowerCase(1) and digits(1) and notUsername`.
- **Brute force:** attiva (`failureFactor 5`, `waitIncrement 60s`, `maxFailureWait 900s`, non permanente).
- **Eventi:** login/admin abilitati, retention 30 giorni (`eventsExpiration: 2592000`).
- **OTP:** `otpPolicyType: totp` (default del realm per WebAuthn/OTP lasciati invariati).
- **Utente di test (solo dev):** `nutritionista.test@statera.local` /
  `Password123!` (temporanea, richiede `UPDATE_PASSWORD` al primo login),
  ruolo `NUTRIZIONISTA`, email verificata.

## Abilitare Google (federazione)

Nel realm è presente un identity provider `google` **disabilitato**
(`enabled: false`) con le credenziali come placeholder
(`${GOOGLE_CLIENT_ID}` / `${GOOGLE_CLIENT_SECRET}`, sostituiti da Keycloak con le
variabili d'ambiente omonime all'import). Per attivarlo:

1. Ottieni Client ID e Client Secret OAuth da Google Cloud Console.
2. Fornisci i valori reali, in uno dei due modi:
   - **via env** al container (poi ri-avvia con re-import): imposta
     `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` nell'ambiente del servizio
     `keycloak` (es. blocco `environment:` del compose o file `.env`); **oppure**
   - **dall'admin console:** Realm `statera` → *Identity providers* → *google* →
     inserisci Client ID / Secret a mano.
3. Metti l'IdP su **`enabled: true`** (nell'admin console, o nel JSON se
   ri-generi l'import).

> ⚠️ Le credenziali Google reali NON vanno committate nel repo.

## Puntare il backend a Keycloak (dev)

La migrazione avviene **dietro feature flag doppio-binario** (vedi
`docs/HANDOFF-KEYCLOAK.md`). Quando si abilita il binario Keycloak, il backend
va configurato (in `application-dev.properties` / variabili d'ambiente, **non
toccati da questi artefatti**) con l'issuer e la registration client `keycloak`:

```properties
# Flag applicativo per attivare il binario Keycloak (nome indicativo).
auth.provider=keycloak

# Client OAuth2 del BFF (registration id = keycloak -> redirect URI
# /login/oauth2/code/keycloak, coerente con il realm importato).
spring.security.oauth2.client.registration.keycloak.client-id=statera-bff
spring.security.oauth2.client.registration.keycloak.client-secret=dev-secret-change-me
spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email

# Provider: issuer del realm (Keycloak espone il discovery OIDC su questo URI).
spring.security.oauth2.client.provider.keycloak.issuer-uri=http://localhost:8081/realms/statera
```

> Dipendenze di riferimento (best-practice ufficiale, vedi `HANDOFF-KEYCLOAK.md`):
> `spring-boot-starter-oauth2-client` (lato BFF, `oauth2Login`) +
> `spring-boot-starter-oauth2-resource-server` (validazione JWT lato API).
> **NIENTE** adapter Keycloak legacy (`keycloak-spring-boot-starter`, deprecato
> e rimosso da Keycloak 20).

> ⚠️ **Ripeto: solo-dev.** `client secret`, password admin e password utente qui
> presenti sono per lo sviluppo locale. In produzione: segreti forti generati,
> gestiti fuori dal repo, e IdP con DB/TLS gestiti.
