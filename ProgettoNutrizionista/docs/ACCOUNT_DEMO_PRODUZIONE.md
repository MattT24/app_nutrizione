# Account demo: messa in produzione

## Modello di sicurezza

- Il login ordinario email/password e Google non viene modificato per gli account reali.
- Le credenziali demo sono isolate nella tabella `credenziali_demo`; nel record `utenti` esiste soltanto una password tecnica casuale non comunicata.
- La scadenza di 14 giorni e la disabilitazione sono verificate sul database a ogni richiesta autenticata, non soltanto nell'interfaccia.
- Rotazione password, disabilitazione, estensione e revoca incrementano `token_version`: tutti i JWT precedenti diventano immediatamente inutilizzabili.
- La chiave master non e mai accettata da `/api/auth/login` o `/api/auth/demo/login`.
- L'accesso assistito richiede contemporaneamente JWT `SUPER_ADMIN`, password personale dell'admin, chiave master e motivazione. Il JWT risultante dura 15 minuti, contiene soltanto i permessi del nutrizionista e non puo accedere a `/api/admin/**`.
- Ogni operazione sensibile e registrata in `audit_account_demo`; password, token e testi segreti non vengono registrati.

## Segreto master obbligatorio

Generare una frase casuale di almeno 24 caratteri con un password manager, creare un hash BCrypt con cost almeno 12 e configurare:

```text
DEMO_MASTER_PASSWORD_HASH=<hash BCrypt>
```

Conservare la frase in chiaro esclusivamente nel password manager del super admin e l'hash nel secret manager dell'ambiente. Non inserire nessuno dei due nel repository, nei file `.env` committati, nei log o nei ticket. Il profilo `prod` rifiuta l'avvio se l'hash manca o non rispetta il formato BCrypt/cost minimo.

## Checklist di rilascio

1. Configurare HTTPS end-to-end e cookie/header di sicurezza sul reverse proxy.
2. Impostare `SPRING_PROFILES_ACTIVE=prod`, `JWT_SECRET` casuale di almeno 256 bit e `DEMO_MASTER_PASSWORD_HASH` nel secret manager.
3. Eseguire un backup e verificare la creazione delle tabelle `credenziali_demo` e `audit_account_demo` durante il rollout controllato.
4. Creare un account demo di prova, verificare login e revoca immediata delle sessioni.
5. Verificare con un orologio/test controllato che dopo 14 giorni login e JWT restituiscano 401 e che dati e account restino presenti.
6. Verificare l'accesso assistito a un account scaduto e il 403 dello stesso token su `/api/admin/**`.
7. Collegare un alert ai fallimenti ripetuti di login/impersonificazione e proteggere i log di audit da modifica o cancellazione non autorizzata.
8. Ruotare la chiave master dopo sospetta esposizione o cambio degli amministratori autorizzati.

Nota: per non fidarsi di header falsificabili, il rate limit usa l'indirizzo visto dal server. Se in produzione esiste un reverse proxy, configurare esplicitamente Spring e il proxy per i forwarded header soltanto da proxy attendibili.
