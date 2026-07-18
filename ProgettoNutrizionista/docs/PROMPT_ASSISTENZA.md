# FEATURE: Sezione "Assistenza" — ticket + chat nutrizionista ↔ super admin

## CONTESTO (leggi prima di scrivere codice)

Due repository:
- **Backend**: `app_nutrizione/ProgettoNutrizionista` — Spring Boot 3, Spring Security con JWT stateless
  (HS256, authorities = alias dei Permessi nel token), JPA/Hibernate su MySQL/TiDB con
  `spring.jpa.hibernate.ddl-auto=update` (le tabelle nuove si creano da sole), springdoc, NIENTE WebSocket nel pom.
- **Frontend**: `nutrizionistaFrontEnd/nutrizionista_front` — Angular 21 **ZONELESS** (niente zone.js:
  ogni stato mutato dopo `await`/timer DEVE stare in `signal()` o la vista non si aggiorna),
  standalone components, PrimeNG 21 (tema Aura, `darkModeSelector: '.theme-dark'`), FontAwesome nella navbar.

Pattern ESISTENTI da riusare (non reinventare):
- `security/JwtAuthFilter` + `@PreAuthorize("hasAuthority('<ALIAS_PERMESSO>')")` sui controller.
- `SecurityConfig`: la filter chain vincola già `/api/admin/**` a `SUPER_ADMIN` (difesa in profondità:
  metti gli endpoint admin della feature SOTTO `/api/admin/assistenza/**` così eredi la regola).
- `config/SuperAdminSeeder`: seeder idempotente ad ogni avvio per ruoli/permessi — estendilo o crea
  `AssistenzaSeeder` con lo stesso stile per il nuovo permesso.
- `service/CurrentUserService.getMe()`: identità SEMPRE dal JWT, MAI da id passati dal client.
- `security/LoginAttemptService`: rate limiter in-memory con `ConcurrentHashMap` + `@Scheduled` di pulizia —
  copia questo pattern per i rate limit della feature.
- `exception/GlobalExceptionHandler`: 404 `NotFoundException`, 409 `ConflictException`,
  429 `TooManyRequestsException`, 400 `BadRequestException`.
- Entity: classi con getter/setter manuali, `@EntityListeners(AuditingEntityListener.class)`,
  `@CreatedDate/@LastModifiedDate`, colonne snake_case, commenti Javadoc in italiano.
- Frontend: services in `src/app/services/` con `apiUrl = 'http://localhost:8080/api/...'`,
  token via interceptor già esistente; rotta nutrizionista dentro `MainLayoutComponent` in `app.routes.ts`
  (lazy `loadComponent`); voce menu in `components/navbar/navbar.ts` → array `menuItems`;
  pannello admin in `screens/admin-dashboard` (già sempre dark, già in signals).

## FUNZIONALITÀ RICHIESTA (flusso stile "richieste messaggi Instagram")

1. Il nutrizionista ha in sidebar la voce **Assistenza** (rotta `/assistenza`).
2. Se non ha ticket attivi vede un form **"Richiedi assistenza"**: oggetto (max 120 char) +
   descrizione del problema (max 2000 char) con contatore caratteri.
3. L'invio crea un **ticket in stato IN_ATTESA** visibile al super admin nel suo pannello.
4. Il super admin vede le richieste pendenti (badge con conteggio) e può **Accettare** o
   **Rifiutare** (rifiuto con motivo opzionale, max 500 char).
5. All'accettazione si apre la **chat 1-a-1** ticket-scoped: entrambi scrivono messaggi testuali.
6. Il ticket può essere **chiuso** dal super admin o dal nutrizionista proprietario; chiuso = chat in sola lettura.
7. Il nutrizionista con ticket RIFIUTATO vede il motivo e può crearne uno nuovo.

## MODELLO DATI (backend)

- Enum `StatoTicket { IN_ATTESA, ACCETTATO, RIFIUTATO, CHIUSO }` nel package `enums`.
- Entity `TicketAssistenza` (tabella `ticket_assistenza`):
  `id`, `nutrizionista` (ManyToOne Utente, not null), `oggetto` (not null, len 120),
  `descrizione` (columnDefinition TEXT, not null), `stato` (@Enumerated(STRING), not null),
  `motivoRifiuto` (nullable, 500), `acceptedAt`, `closedAt`, `createdAt/updatedAt` (auditing),
  `@Version private Long version` (optimistic locking: impedisce doppia accettazione concorrente).
  Indici: `@Index(columnList="stato")`, `@Index(columnList="nutrizionista_id, stato")`.
- Entity `MessaggioAssistenza` (tabella `messaggi_assistenza`):
  `id`, `ticket` (ManyToOne, not null), `mittente` (ManyToOne Utente, not null),
  `testo` (not null, len 2000), `letto` (boolean default false), `createdAt`.
  Indice: `@Index(columnList="ticket_id, id")` (il polling incrementale filtra per `id > afterId`).

## MACCHINA A STATI — SOLO LATO SERVER

- IN_ATTESA → ACCETTATO: solo SUPER_ADMIN.
- IN_ATTESA → RIFIUTATO: solo SUPER_ADMIN.
- IN_ATTESA → CHIUSO: il nutrizionista proprietario può annullare la propria richiesta.
- ACCETTATO → CHIUSO: SUPER_ADMIN o nutrizionista proprietario.
- QUALSIASI altra transizione → `ConflictException` (409).
- Messaggi scrivibili SOLO in stato ACCETTATO (mittente = proprietario o super admin), altrimenti 409.
- REGOLA: max **1 ticket "aperto"** (IN_ATTESA o ACCETTATO) per nutrizionista → verificato nel service
  dentro la transazione (`existsByNutrizionista_IdAndStatoIn`), 409 se violata.
- Le transizioni rileggono lo stato DENTRO la transazione (`@Transactional`) e si affidano a `@Version`
  per i conflitti concorrenti.

## API REST

### Lato nutrizionista — `/api/assistenza/**`, `@PreAuthorize("hasAuthority('ASSISTENZA_USE')")`
- `POST   /api/assistenza/tickets` — body `{oggetto, descrizione}` con `@Valid` (@NotBlank, @Size). 201.
  Rate limit: max 3 ticket per utente ogni 24h → 429.
- `GET    /api/assistenza/tickets/attivo` — il ticket IN_ATTESA/ACCETTATO corrente, o 204/null.
- `GET    /api/assistenza/tickets` — storico dei PROPRI ticket (paginato, max size 50).
- `POST   /api/assistenza/tickets/{id}/messaggi` — body `{testo}`. Rate limit anti-flood: 20 msg/min → 429.
- `GET    /api/assistenza/tickets/{id}/messaggi?afterId={n}` — polling incrementale: restituisce
  `{stato, messaggi: [...]}` (lo stato serve alla UI per reagire a chiusure/transizioni), max 200 messaggi a chiamata.
- `POST   /api/assistenza/tickets/{id}/chiudi`
- `POST   /api/assistenza/tickets/{id}/messaggi/letti` — marca come letti i messaggi ricevuti.

### Lato super admin — `/api/admin/assistenza/**`, `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` a livello classe
- `GET    /api/admin/assistenza/tickets?stato=IN_ATTESA|ACCETTATO|...` — lista con dati MINIMI del
  nutrizionista (id, nome, cognome, email — MAI codice fiscale/telefono/indirizzo).
- `GET    /api/admin/assistenza/tickets/conteggi` — `{inAttesa, accettati}` per i badge.
- `POST   /api/admin/assistenza/tickets/{id}/accetta`
- `POST   /api/admin/assistenza/tickets/{id}/rifiuta` — body `{motivo}` opzionale.
- `POST   /api/admin/assistenza/tickets/{id}/chiudi`
- `POST   /api/admin/assistenza/tickets/{id}/messaggi` + `GET .../messaggi?afterId=` (stessi contratti).

### Regole trasversali di sicurezza sugli endpoint
- Il mittente dei messaggi e il proprietario del ticket derivano SEMPRE da `CurrentUserService.getMe()`.
  Nessun DTO di input contiene id utente.
- **Anti-IDOR**: ogni accesso a `/tickets/{id}` lato nutrizionista verifica `ticket.nutrizionista.id == me.id`;
  se non è suo rispondi **404** (non 403: non rivelare che il ticket esiste).
- DTO di risposta dedicati (`TicketAssistenzaDto`, `MessaggioAssistenzaDto`): mai serializzare le entity
  (eviti lazy-loading leak e overexposure). `MessaggioAssistenzaDto` include `mioMessaggio: boolean`
  calcolato server-side rispetto al chiamante.
- Audit log su logger dedicato `AUDIT.assistenza` per ogni transizione di stato (chi, ticket id, da→a).
  MAI loggare il testo dei messaggi.
- Paginazione difensiva ovunque (size clampato server-side).

## PERMESSI E SEEDER
- Nuovo permesso `ASSISTENZA_USE` (nome "Richiedi assistenza") creato da un seeder idempotente
  (stile `SuperAdminSeeder`) e agganciato al ruolo `NUTRIZIONISTA` se il link non esiste già.
  ATTENZIONE: gli utenti già loggati non hanno il nuovo permesso nel JWT → il permesso compare al
  prossimo login (documentalo nel riepilogo finale).

## TRASPORTO CHAT: POLLING (scelta deliberata)
- Niente WebSocket: il pom non ha lo starter e il polling riusa al 100% la sicurezza HTTP/JWT esistente.
- Polling ogni 4 secondi con `afterId` dell'ultimo messaggio ricevuto (payload incrementale, indice dedicato).
- Il polling dei conteggi admin ogni 15 secondi.
- Documenta in un commento l'upgrade path futuro (spring-boot-starter-websocket + STOMP + auth JWT
  sull'handshake) senza implementarlo.

## FRONTEND — NUTRIZIONISTA
- `navbar.ts`: voce `{ id: 'assistenza', icon: faHeadset, label: 'Assistenza', route: '/assistenza' }`.
- `app.routes.ts`: rotta lazy `/assistenza` dentro il children di `MainLayoutComponent`.
- `services/assistenza.service.ts`: interfacce TS dei DTO + metodi HTTP (pattern degli altri service).
- Screen `screens/assistenza/`: componente standalone che in base allo stato del ticket attivo mostra:
  - nessun ticket → card con form richiesta (PrimeNG InputText + Textarea, validazione, contatori);
  - IN_ATTESA → stato "richiesta inviata, in attesa di accettazione" + bottone Annulla richiesta;
  - RIFIUTATO (ultimo ticket) → motivo del rifiuto + bottone nuova richiesta;
  - ACCETTATO → chat: bolle differenziate mie/sue, auto-scroll al fondo, invio con Enter e bottone,
    textarea disabilitata se il ticket risulta CHIUSO dal polling, banner "conversazione chiusa".
- VINCOLO ZONELESS: tutto lo stato (ticket, messaggi, loading, errori) in `signal()`;
  `setInterval` del polling creato in `ngOnInit` e SEMPRE cancellato in `ngOnDestroy`;
  nessun campo semplice mutato dopo await.
- Testo dei messaggi renderizzato SOLO con interpolation `{{ }}` (mai `[innerHTML]`) → XSS impossibile.

## FRONTEND — SUPER ADMIN
- In `screens/admin-dashboard`: aggiungi una sezione/tab "Assistenza" (la pagina è già sempre dark e in signals):
  - badge con conteggio richieste IN_ATTESA (polling 15s);
  - lista richieste pendenti stile inbox: oggetto, estratto descrizione, nome nutrizionista, data,
    bottoni Accetta / Rifiuta (dialog per il motivo);
  - lista conversazioni ACCETTATE con indicatore non letti; click → stessa vista chat del nutrizionista
    + bottone "Chiudi ticket" con conferma.

## VERIFICA END-TO-END OBBLIGATORIA (falla davvero, con l'app avviata)
1. Backend avviato → tabelle create, seeder logga il permesso.
2. Nutrizionista crea ticket → visibile come IN_ATTESA lato admin.
3. **Test IDOR**: con il token di un secondo nutrizionista, `GET /api/assistenza/tickets/{id}` del primo → 404.
4. Senza token → 401/403 su tutti gli endpoint; token nutrizionista su `/api/admin/assistenza/**` → 403.
5. Admin accetta → chat funzionante nei due sensi (verifica con curl i due token).
6. Messaggio su ticket IN_ATTESA o CHIUSO → 409. Secondo ticket con uno già aperto → 409.
7. Rate limit: 4° ticket in 24h → 429; flood di messaggi → 429.
8. Chiusura → chat sola lettura; compila backend (`mvnw compile`) e frontend (`ng build`) puliti.

## STILE
Commenti e nomi in italiano coerenti col progetto; field injection `@Autowired` come nel resto del codice;
DTO classi con getter/setter; nessuna dipendenza nuova nel pom; non toccare flussi esistenti se non dove indicato.
