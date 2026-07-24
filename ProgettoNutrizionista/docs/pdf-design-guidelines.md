# Linee guida design PDF Statera — cosa può usare Claude Design

Questo documento definisce i **vincoli tecnici** entro cui devono stare i design HTML/CSS destinati
ai PDF di Statera. Dal luglio 2026 il motore di rendering è **Chromium headless via Playwright**
(`ChromiumPdfRenderer`, `com.microsoft.playwright:playwright`, immagine `mcr.microsoft.com/playwright/java`)
che sostituisce il precedente OpenHTMLToPDF+Batik+OGNL. È un **browser reale**: supporta **CSS
moderno completo** (flexbox, grid, gap, `@page`, ecc.). I template Thymeleaf sono in modalità **HTML**.

**Restano solo due vincoli reali**, imposti apposta dal renderer (non dal motore):
1. **Niente JavaScript**: il render gira con `setJavaScriptEnabled(false)`. Il design deve essere
   **statico e già completo** nell'HTML (nessuna animazione/idratazione JS).
2. **Niente rete**: ogni richiesta di rete è **abortita** (`route("**/*").abort()`, difesa in profondità
   su dati art. 9). Quindi **nessuna risorsa esterna**: font da CDN, `<img>` con URL, CSS via `<link>`
   NON caricano. Tutto va **inline / base64**; i font sono quelli **installati nel container**.

> Regola d'oro: **pagina web moderna ma autocontenuta e senza JS**. Layout con flexbox/grid liberi,
> ma ogni asset dentro l'HTML (CSS inline, immagini base64) e font di sistema.

---

## ✅ SI PUÒ USARE (è un browser vero)

**Layout** — CSS moderno pieno:
- `display: flex` / `display: grid`, `gap`, `flex-wrap`, `align-items`, `justify-content`,
  `grid-template-columns`, `margin: auto`, `position` (relative/absolute/sticky), ecc.
- `<table>` resta valido, ma **non è più obbligatorio** per fare colonne/griglie.

**Box & decorazioni** — pieno supporto CSS3:
- `margin`, `padding`, `border`, `border-radius`, `background` (inclusi `linear-gradient`,
  `radial-gradient`, `conic-gradient`), `box-shadow`, `opacity`, `transform`, `filter`,
  `object-fit`, `clip-path`, `overflow: hidden` + `border-radius` (clippa correttamente).

**Tipografia**
- Font **Inter** (installato a livello OS nel container, pesi 400/500/600/700): `font-family: 'Inter', sans-serif`.
- Tutte le proprietà tipografiche CSS moderne (`letter-spacing`, `font-variant-numeric: tabular-nums`, ecc.).
- ⚠️ **Niente webfont da CDN** (`@import`/`<link>` a Google Fonts): la rete è bloccata → userebbe il
  fallback. Se serve un font diverso da Inter, va installato nell'immagine Docker (`Dockerfile`).

**Colori** — palette brand:
`#10b981 #059669 #047857` (emerald), `#0f172a / #1f2937` (testo), `#475569 #64748b #94a3b8` (grigi),
`#e5e7eb / #e8ebef` (bordi), `#f6f8fa / #f8fafc` (superfici). Accenti: `#3b82f6` blu, `#f59e0b` ambra,
`#8b5cf6` viola, `#ef4444` rosso. Le variabili CSS `var(--x)` sono pienamente supportate.

**Grafica → SVG inline** (renderizzato da Chromium, non più Batik):
- Icone, loghi, donut/anelli, grafici a linee/aree, sparkline.
- Il testo dentro gli `<svg>` (`<text>`) usa correttamente **Inter** (il vecchio limite Batik non esiste più).

**Pagina & stampa**
- `@page { size: A4; margin: ... }` (rispettato grazie a `preferCSSPageSize` in `page.pdf`).
- `print-background` è attivo → sfondi/gradienti stampati.
- Interruzioni di pagina: sia la sintassi moderna **`break-inside: avoid`** / `break-before` /
  `break-after`, sia la legacy `page-break-*` (entrambe supportate da Chromium).

**Immagini**
- Preferire **data URI base64** (`src="data:image/png;base64,..."`). Il logo del nutrizionista arriva
  già come `logoBase64`. URL esterne **non** caricano (rete bloccata).

---

## ❌ DA EVITARE

| Non usare | Perché |
|---|---|
| **JavaScript** (qualsiasi) | disabilitato nel renderer → non viene eseguito; la pagina deve essere già completa |
| Webfont da CDN (`@import`, `<link>` a Google Fonts) | rete bloccata → non caricano; usare Inter (OS) |
| Risorse esterne (img/css/js via URL o `data:` verso host) | rete abortita → non caricano; tutto inline/base64 |
| Contenuti che dipendono dall'interazione/hover/animazione | il PDF è uno snapshot statico |

Tutto il resto del CSS moderno è disponibile: non ci sono più i limiti "email HTML 2010" del vecchio motore.

---

## 📐 Note sull'HTML
- I template sono **HTML5** processati da Thymeleaf in modalità HTML (non più XML/XHTML strict): non
  serve più chiudere i void tag come XML, ma un HTML ben formato resta buona pratica.
- Il motore è SpEL (`SpringTemplateEngine`): l'accesso a una **chiave di Map assente** con notazione a
  punto (`${mappa.chiave}`) **lancia** (non ritorna null come il vecchio OGNL) → nei template garantire
  che le chiavi lette esistano sempre (vedi `PdfGenerationIntegrationTest`, che nasce da questo bug).

## Struttura di riferimento (già in uso)
- Formato **A4**, margini ~13–15 mm.
- **Referti** (misurazione, plicometria): masthead logo+titolo, striscia paziente, KPI card, tabelle, footer.
- **Schede**: header + striscia paziente, layout a colonne (pasti | riquadri laterali), donut macro SVG.
- **Disclaimer anti-MDR (D2)**: il footer (`templates/pdf/fragments/pdf.html`) DEVE mantenere il
  disclaimer "strumento di supporto al professionista, non sostituisce il parere medico".
- Riusare palette e struttura sopra per coerenza di brand.
