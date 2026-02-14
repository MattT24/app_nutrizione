# Backend: Gestione Alimenti Alternativi

## Contesto
Un alimento alternativo è una proposta sostitutiva associata a un record di `AlimentoPasto` (alimento scelto all’interno di un `Pasto` di una `Scheda` di un `Cliente`).

Relazione di ownership:
`Utente (nutrizionista) → Cliente → Scheda → Pasto → AlimentoPasto → AlimentoAlternativo`

## Modello dati
Entity: [AlimentoAlternativo.java](file:///c:/Users/Utente/Documents/GitHub/app_nutrizione/ProgettoNutrizionista/src/main/java/it/nutrizionista/restnutrizionista/entity/AlimentoAlternativo.java)

Campi principali:
- `id`: PK
- `alimentoPasto`: FK verso `alimenti_pasto`
- `alimentoAlternativo`: FK verso `alimenti_base`
- `quantita`: grammi finali proposti (integer)
- `priorita`: ordinamento (1 = prima alternativa)
- `mode`: modalità per eventuale ricalcolo automatico (`CALORIE|PROTEINE|CARBOIDRATI|GRASSI`)
- `manual`: se true la quantità è considerata override manuale (non ricalcolata automaticamente)
- `note`: testo libero (max 500)
- `createdAt`, `updatedAt`: auditing

Vincolo DB esistente:
- unique(`alimento_pasto_id`, `alimento_alternativo_id`)

## Logica di business
Service: [AlimentoAlternativoService.java](file:///c:/Users/Utente/Documents/GitHub/app_nutrizione/ProgettoNutrizionista/src/main/java/it/nutrizionista/restnutrizionista/service/AlimentoAlternativoService.java)

### Ownership
Ogni operazione sulle alternative richiede che l’`AlimentoPasto` appartenga al nutrizionista loggato (ownership validata tramite [OwnershipValidator.java](file:///c:/Users/Utente/Documents/GitHub/app_nutrizione/ProgettoNutrizionista/src/main/java/it/nutrizionista/restnutrizionista/service/OwnershipValidator.java)).

### Quantità “smart”
Calcolatore: [AlternativeSuggestionCalculator.java](file:///c:/Users/Utente/Documents/GitHub/app_nutrizione/ProgettoNutrizionista/src/main/java/it/nutrizionista/restnutrizionista/service/AlternativeSuggestionCalculator.java)

Regola (policy):
- `manual=true`: la `quantita` è considerata override manuale e non viene ricalcolata automaticamente.
- `manual=false`:
  - se `quantita` non viene fornita (null), il backend calcola una quantità suggerita in base alla modalità (`mode`) usando:
  - `AlimentoBase.misuraInGrammi`
  - `Macro` (kcal/proteine/carboidrati/grassi)
  - se `quantita` viene fornita, viene salvata; al prossimo cambio della quantità dell’alimento principale può essere ricalcolata (se previsto dal flusso applicativo).

Ricalcolo automatico:
- quando cambia `AlimentoPasto.quantita`, vengono ricalcolate le alternative con `manual=false` (hook in [AlimentoPastoService.java](file:///c:/Users/Utente/Documents/GitHub/app_nutrizione/ProgettoNutrizionista/src/main/java/it/nutrizionista/restnutrizionista/service/AlimentoPastoService.java)).

## API REST (nuove, annidate)
Controller: [AlimentoPastoAlternativeController.java](file:///c:/Users/Utente/Documents/GitHub/app_nutrizione/ProgettoNutrizionista/src/main/java/it/nutrizionista/restnutrizionista/controller/AlimentoPastoAlternativeController.java)

Base path:
`/api/alimenti_pasto/{alimentoPastoId}/alternative`

Permessi richiesti:
- `ALIMENTO_ALTERNATIVO_READ|CREATE|UPDATE|DELETE` (via `@PreAuthorize`)

### GET list
`GET /api/alimenti_pasto/{alimentoPastoId}/alternative`

Response 200:
```json
[
  {
    "id": 1,
    "alimentoPasto": { "id": 10, "quantita": 150, "alimento": { "id": 7, "nome": "..." } },
    "alimentoAlternativo": { "id": 99, "nome": "..." },
    "quantita": 180,
    "priorita": 1,
    "mode": "CALORIE",
    "manual": false,
    "note": null
  }
]
```

### POST create
`POST /api/alimenti_pasto/{alimentoPastoId}/alternative`

Request body: [AlimentoAlternativoUpsertDto.java](file:///c:/Users/Utente/Documents/GitHub/app_nutrizione/ProgettoNutrizionista/src/main/java/it/nutrizionista/restnutrizionista/dto/AlimentoAlternativoUpsertDto.java)
```json
{
  "alimentoAlternativoId": 99,
  "quantita": null,
  "priorita": null,
  "mode": "CALORIE",
  "manual": false,
  "note": "opzionale"
}
```

Response 201: `AlimentoAlternativoDto`

### PUT update (singola alternativa)
`PUT /api/alimenti_pasto/{alimentoPastoId}/alternative/{alternativeId}`

```json
{
  "alimentoAlternativoId": 99,
  "quantita": 200,
  "priorita": 2,
  "mode": "PROTEINE",
  "manual": true,
  "note": "testo"
}
```

Response 200: `AlimentoAlternativoDto`

### DELETE
`DELETE /api/alimenti_pasto/{alimentoPastoId}/alternative/{alternativeId}`

Response 204.

### PUT bulk upsert (lista dinamica)
`PUT /api/alimenti_pasto/{alimentoPastoId}/alternative`

Request body: lista di `AlimentoAlternativoUpsertDto`. Se `id` è presente, viene eseguito update; se `id` è assente viene creata una nuova alternativa.

```json
[
  { "id": 1, "alimentoAlternativoId": 99, "quantita": 180, "priorita": 1, "mode": "CALORIE", "manual": false },
  { "alimentoAlternativoId": 123, "quantita": null, "priorita": 2, "mode": "CARBOIDRATI", "manual": false }
]
```

Response 200: lista di `AlimentoAlternativoDto`.

## API legacy (compatibilità)
Controller esistente: [AlimentoAlternativoController.java](file:///c:/Users/Utente/Documents/GitHub/app_nutrizione/ProgettoNutrizionista/src/main/java/it/nutrizionista/restnutrizionista/controller/AlimentoAlternativoController.java)

Gli endpoint legacy restano disponibili:
- `POST /api/alimenti_alternativi`
- `PUT /api/alimenti_alternativi`
- `DELETE /api/alimenti_alternativi/{id}`
- `GET /api/alimenti_alternativi/{id}`
- `GET /api/alimenti_alternativi/alimento_pasto/{alimentoPastoId}`

## Errori e codici HTTP
Gestione centralizzata: [GlobalExceptionHandler.java](file:///c:/Users/Utente/Documents/GitHub/app_nutrizione/ProgettoNutrizionista/src/main/java/it/nutrizionista/restnutrizionista/exception/GlobalExceptionHandler.java)

Mappatura:
- 400: validazione/parametri non validi (`BadRequestException`, `MethodArgumentNotValidException`)
- 403: risorsa non accessibile per ownership (`ForbiddenException`, `SecurityException`)
- 404: risorsa non trovata (`NotFoundException`)
- 409: conflitto (duplicati) (`ConflictException`)

## Note
- Migrazioni DB e DB integration test non sono inclusi (fuori scope per scelta). 
