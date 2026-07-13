# Linee guida design PDF Statera — cosa può usare Claude Design

Questo documento definisce i **vincoli tecnici** entro cui devono stare i design HTML/CSS destinati
ai PDF di Statera. Il motore di rendering è **OpenHTMLToPDF 1.1.40** (basato su Apache PDFBox 3 +
Batik per l'SVG). Non è un browser: supporta **CSS 2.1 + un sottoinsieme di CSS 3** e **non esegue
JavaScript**. Rispettare queste regole significa che il design generato va in PDF **senza dover
essere riscritto**.

> Regola d'oro: **pensa "email HTML 2010", non "sito web moderno"**. Layout a tabelle, CSS inline,
> tutto statico e autocontenuto.

---

## ✅ SI PUÒ USARE

**Layout**
- `<table>` reali e `display: table / table-row / table-cell` → **questo è il modo per fare colonne,
  griglie e affiancamenti**. (es. le 3 KPI card = una `<table>` con 3 celle).
- `float`, `block`, `inline`, `inline-block` (con moderazione).
- Larghezze/altezze esplicite in `px`, `%`, `mm`; `border-collapse`, `border-spacing`.

**Box & decorazioni**
- `margin`, `padding`, `border`, `border-radius`, `background-color`.
- `background: linear-gradient(...)` (gradienti lineari OK; radiali/conici NO).
- Bordi arrotondati, linee, divisori, tabelle a righe zebrate (`:nth-child`, `:last-child`).

**Tipografia**
- Font **Inter** (già embeddato, pesi 400/500/600/700). Usare `font-family: 'Inter', sans-serif`.
- `font-size`, `font-weight`, `line-height`, `letter-spacing`, `text-transform`, `text-align`,
  `color`, `white-space: nowrap`, `font-variant-numeric: tabular-nums`.

**Colori** — palette brand:
`#10b981 #059669 #047857` (emerald), `#0f172a / #1f2937` (testo), `#475569 #64748b #94a3b8` (grigi),
`#e5e7eb / #e8ebef` (bordi), `#f6f8fa / #f8fafc` (superfici). Accenti: `#3b82f6` blu, `#f59e0b` ambra,
`#8b5cf6` viola, `#ef4444` rosso. **Usare valori esadecimali** (le variabili CSS `var(--x)` sono
supportate ma è più sicuro l'esadecimale diretto).

**Grafica → SVG inline** (renderizzato da Batik):
- Icone, loghi, donut/anelli (cerchio con `stroke-dasharray`), grafici a linee/aree, sparkline.
- Ogni `<svg xmlns="http://www.w3.org/2000/svg">`, tutti i tag chiusi.

**Pagina & stampa**
- `@page { size: A4; margin: ... }`.
- Interruzioni di pagina: `page-break-before/after/inside: avoid | always` (**sintassi CSS2**, NON
  `break-inside`).

**Immagini**
- Solo `<img>` con **data URI base64** (`src="data:image/png;base64,..."`). Nessuna URL esterna.

---

## ❌ DA EVITARE (non funziona o rende male)

| Non usare | Perché | Usare invece |
|---|---|---|
| `display: flex` | **ignorato** → collassa a block | `display: table` / `<table>` |
| `display: grid` | **ignorato** | `<table>` con celle |
| `gap` (flex/grid) | ignorato | `padding` sulle celle / `border-spacing` |
| `margin-left: auto` per allineare a destra | ignorato | cella con `text-align: right` |
| `conic-gradient` (donut/torte) | non supportato | **SVG** (cerchio con `stroke-dasharray`) |
| **JavaScript** (qualsiasi) | non eseguito → pagina vuota | HTML **statico** già completo |
| Webfont da CDN (`@import`, `<link>` a Google Fonts) | bloccati | font **Inter** già embeddato |
| Icon-font via JS (FontAwesome `all.js`) | non eseguito | **SVG inline** |
| Risorse esterne (img/css via URL) | bloccate | tutto **inline / base64** |
| `position: absolute` per layout | fragile/imprevedibile | flusso normale + tabelle |
| `object-fit`, `inset` (shorthand), `clip-path`, `backdrop-filter`, `filter`, `transform` complessi | non supportati | dimensioni esplicite |
| `box-shadow`, `opacity` | supporto parziale/incerto | usare con cautela, non essenziali |
| `overflow: hidden` + `border-radius` per clippare | non clippa bene il contenuto | evitare di farci affidamento |

---

## 📐 Requisiti tecnici dell'HTML (importante)

Il motore parsa l'HTML come **XML/XHTML valido** (strict). Il design deve quindi:
- avere **tutti i tag chiusi** e i void self-chiusi (`<img/>`, `<br/>`, `<meta/>`);
- usare `&#160;` (non `&nbsp;`) e `&amp;` per la `&`;
- gli **SVG** ben formati con `xmlns`, senza `<script>` né animazioni;
- ⚠️ dentro gli `<svg>`, gli elementi `<text>` **non** usano Inter (Batik usa font propri): per numeri
  nei grafici va bene, ma non affidarsi al testo SVG per contenuti importanti — meglio testo HTML.

## Struttura di riferimento (già in uso)
- Formato **A4**, margini ~13–15 mm.
- **Referti** (misurazione, plicometria): masthead logo+titolo, striscia paziente a 3 celle, sezioni
  con KPI card, tabelle dati, footer firma.
- **Schede**: header + striscia paziente, due colonne (pasti | riquadri laterali), donut macro SVG.
- Riusare palette e struttura sopra per coerenza di brand.
