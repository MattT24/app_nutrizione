package it.nutrizionista.restnutrizionista.service;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import it.nutrizionista.restnutrizionista.dto.AlimentoBaseDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoBaseFormDto;
import it.nutrizionista.restnutrizionista.dto.MacroDto;
import it.nutrizionista.restnutrizionista.dto.OffProductDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.enums.Allergene;
import it.nutrizionista.restnutrizionista.enums.FonteAllergene;
import it.nutrizionista.restnutrizionista.enums.StatoAllergene;
import it.nutrizionista.restnutrizionista.exception.ConflictException;
import it.nutrizionista.restnutrizionista.exception.UnprocessableEntityException;

/**
 * Integrazione Open Food Facts.
 *
 * <ul>
 *   <li><b>Ricerca</b>: Search-a-licious (Elasticsearch) + {@code @Cacheable}. Campi ridotti
 *       ({@code FIELDS_SEARCH}): la search NON espone additivi/tracce/ingredienti/porzione (piano E.7).</li>
 *   <li><b>Import</b>: OFF API <b>v3.6</b> {@code /product/{barcode}} (envelope con {@code status} stringa, E.10).
 *       Mapping deterministico allergeni tri-stato (match esatto {@code en:}, no contains), policy
 *       "prodotti incompleti" (E.1) e dedup {@code (created_by, barcode)} (§6).</li>
 * </ul>
 *
 * @security SSRF = URL costante + barcode validato; image whitelist = solo images.openfoodfacts.org;
 *           sanitize() su testo; User-Agent/timeout in RestTemplateConfig.
 */
@Service
public class OpenFoodFactsService {

    private static final String SEARCH_URL = "https://search.openfoodfacts.org/search";
    /**
     * Product API v3.6 per l'import. ATTENZIONE: da v3.5 (schema 1003+) i nutrienti NON sono più nel
     * vecchio campo piatto {@code nutriments} (che la v3.6 ritorna VUOTO), ma in
     * {@code nutrition.aggregated_set.nutrients} → si richiede {@code fields=nutrition} e si fa il parsing
     * del nuovo schema. Fallback difensivo: se la v3.6 non fornisce i nutrienti si rifà il fetch su
     * {@link #OFF_PRODUCT_V2} (la v3 è "under active development"). Allergeni/score arrivano già dalla v3.6.
     */
    private static final String OFF_PRODUCT = "https://world.openfoodfacts.org/api/v3.6/product/";
    /** Fallback legacy: la v2 popola ancora il campo piatto {@code nutriments}. */
    private static final String OFF_PRODUCT_V2 = "https://world.openfoodfacts.org/api/v2/product/";

    /** Campi per la ricerca (search-a-licious): sottoinsieme realmente esposto + ecoscore_grade legacy (E.7). */
    private static final String FIELDS_SEARCH =
            "code,product_name,product_name_it,brands,categories,image_url,nutriments,"
            + "allergens_tags,ingredients_analysis_tags,nutriscore_grade,nova_group,ecoscore_grade,nutrient_levels";

    /** Campi completi per il dettaglio/import (product API v3.6). */
    private static final String FIELDS_PRODUCT =
            "code,product_name,product_name_it,generic_name_it,brands,categories,categories_tags,labels_tags,"
            + "image_url,image_front_url,image_ingredients_url,image_nutrition_url,serving_quantity,"
            + "nutrition,nutriments,nutrient_levels,nutriscore_grade,nova_group,environmental_score_grade,ecoscore_grade,"
            + "allergens_tags,traces_tags,ingredients_text_it,ingredients_tags,ingredients_analysis_tags,"
            + "additives_tags,sources,sources_fields,data_quality_errors_tags,data_quality_warnings_tags,"
            + "states_tags,completeness";

    /** Fallback v2: servono solo i nutrienti piatti (tutto il resto arriva dalla v3.6). */
    private static final String FIELDS_PRODUCT_V2 = "code,product_name,nutriments";

    /** Soglia di completezza OFF sotto la quale il prodotto va marcato needsReview. */
    private static final double COMPLETENESS_REVIEW_THRESHOLD = 0.5;

    @Autowired private RestTemplate restTemplate;
    @Autowired private AlimentoBaseService alimentoBaseService;

    /**
     * Ricerca prodotti su OFF via Search-a-licious. Risultati cachati (stessa query+pagina).
     * Ritorna il JSON grezzo (passthrough alla UI).
     */
    @Cacheable(value = "offSearch", key = "#query.toLowerCase() + '_' + #page + '_' + #size")
    public String searchProducts(String query, int page, int size) {
        if (query == null || query.trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query minimo 2 caratteri");
        }

        String url = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                .queryParam("q", sanitize(query))
                .queryParam("langs", "it,en")
                .queryParam("page", Math.max(1, page))
                .queryParam("page_size", Math.min(Math.max(1, size), 24))
                .queryParam("fields", FIELDS_SEARCH)
                .build().encode().toUriString();

        try {
            return restTemplate.getForObject(url, String.class);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Troppe ricerche consecutive. Attendi qualche secondo.");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Impossibile raggiungere Open Food Facts");
        }
    }

    /**
     * Importa un prodotto OFF nel catalogo personale del nutrizionista corrente.
     * Blocca con 409 (existingId) se il barcode è già nel catalogo dell'utente.
     *
     * @param barcode EAN-8/EAN-13/UPC (solo cifre, 8-13 caratteri)
     */
    public AlimentoBaseDto importProduct(String barcode) {
        if (barcode == null || !barcode.matches("^\\d{8,13}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Barcode non valido");
        }

        String url = UriComponentsBuilder.fromHttpUrl(OFF_PRODUCT + barcode + ".json")
                .queryParam("fields", FIELDS_PRODUCT)
                .queryParam("lc", "it")
                .queryParam("cc", "it")
                .build().toUriString();

        OffProductDto response;
        try {
            response = restTemplate.getForObject(url, OffProductDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prodotto non trovato su Open Food Facts");
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Troppe ricerche consecutive. Attendi qualche secondo.");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Impossibile raggiungere Open Food Facts");
        }

        // "trovato" = product != null (v3: status stringa, gestito "failure"; v2 fallback: status=1 coerciato a "1")
        if (response == null || response.getProduct() == null
                || "failure".equalsIgnoreCase(response.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prodotto non trovato su Open Food Facts");
        }

        OffProductDto.Product product = response.getProduct();
        // Barcode canonico: quello restituito da OFF (normalizza gli zeri iniziali), fallback all'input
        String code = (product.getCode() != null && !product.getCode().isBlank()) ? product.getCode().trim() : barcode;

        // Dedup (created_by, barcode): blocca il re-import con 409 strutturato
        Optional<AlimentoBase> esistente = alimentoBaseService.findOwnedByBarcode(code);
        if (esistente.isPresent()) {
            AlimentoBase ex = esistente.get();
            throw new ConflictException("Hai già importato questo prodotto nel tuo catalogo", ex.getId(), ex.getNome());
        }

        // Macro: v3.6 (nutrition) → fallback v2 (nutriments) → policy 422 (E.1)
        MacroDto macro = buildMacro(product);
        if (macro == null) {
            OffProductDto.Product v2product = fetchV2Product(code);
            if (v2product != null) macro = buildMacro(v2product);
        }
        if (macro == null) {
            throw new UnprocessableEntityException(
                    "Dati nutrizionali insufficienti: impossibile importare un alimento senza i macronutrienti di base (kcal, proteine, carboidrati, grassi).");
        }

        AlimentoBaseFormDto form = mapToFormDto(product, code, macro);
        return alimentoBaseService.createPersonale(form);
    }

    // ═══════════════════════════════════════════════════════════
    //  Mapping deterministico OFF → AlimentoBaseFormDto
    // ═══════════════════════════════════════════════════════════

    private AlimentoBaseFormDto mapToFormDto(OffProductDto.Product product, String code, MacroDto macro) {
        AlimentoBaseFormDto form = new AlimentoBaseFormDto();

        // ── Nome: it → generic_name_it → product_name; + brand + suffisso [OFF] ──
        String rawName = firstNonBlank(product.getProductNameIt(), product.getGenericNameIt(), product.getProductName());
        String nome = sanitize(rawName);
        String brand = sanitize(product.getBrands());
        String baseName = !brand.equals("Sconosciuto") ? nome + " (" + brand + ")" : nome;
        form.setNome(baseName + " [OFF]");

        form.setBarcode(code);
        form.setCategoria(resolveCategoria(product));
        form.setUrlImmagine(sanitizeUrl(firstNonBlank(product.getImageUrl(), product.getImageFrontUrl())));
        form.setMisuraInGrammi(100.0);
        form.setServingQuantityG(product.getServingQuantity());
        form.setIngredientsText(product.getIngredientsTextIt());

        // ── Macro (risolti a monte: v3.6 nutrition → fallback v2) ──
        form.setMacroNutrienti(macro);

        // ── Allergeni tri-stato (match ESATTO en:) ──
        Map<Allergene, StatoAllergene> allergeni = buildAllergeni(product);
        form.setAllergeni(allergeni);
        form.setFonteAllergeni(resolveFonte(product));

        // ── Flag legacy derivati (retrocompat con le 3 regole hardcoded finché non si attiva AllergeneRule) ──
        deriveLegacyFlags(form, product, allergeni);

        // ── Score, nutrient levels, additivi ──
        form.setNutriscoreGrade(normalizeGrade(product.getNutriscoreGrade()));
        form.setNovaGroup(product.getNovaGroup());
        form.setEnvironmentalScoreGrade(
                normalizeGrade(firstNonBlank(product.getEnvironmentalScoreGrade(), product.getEcoscoreGrade())));
        if (product.getNutrientLevels() != null) {
            form.setNutrientLevels(new java.util.HashMap<>(product.getNutrientLevels()));
        }
        if (product.getAdditivesTags() != null) {
            form.setAdditivi(new HashSet<>(product.getAdditivesTags()));
        }

        // ── needsReview: dati di qualità incompleti/contraddittori ──
        form.setNeedsReview(computeNeedsReview(product, allergeni));

        // Tracce testuali (retrocompat): traces_tags ripulite dei prefissi
        form.setTracce(cleanTraces(product.getTracesTags()));

        return form;
    }

    /**
     * Costruisce i macro da un prodotto OFF, leggendo PRIMA la nuova struttura v3.6
     * {@code nutrition.aggregated_set.nutrients} e in fallback il vecchio {@code nutriments} piatto (v2/legacy).
     * Ritorna {@code null} se mancano i 4 macro obbligatori (kcal/proteine/carbo/grassi): il chiamante
     * proverà il fallback v2 e poi applicherà la policy 422 (E.1). Fallback energia: kcal = kJ / 4.184.
     * Opzionali null-preserving (chiave assente → null → "n.d."; 0 dichiarato → 0 reale).
     */
    MacroDto buildMacro(OffProductDto.Product product) {  // package-private per i test
        Map<String, Double> n = readNutrients(product);

        Double kcal = n.get("energy-kcal");
        if (kcal == null && n.get("energy-kj") != null) {
            kcal = Math.round((n.get("energy-kj") / 4.184) * 100.0) / 100.0;
        }
        Double proteine = n.get("proteins");
        Double carbo = n.get("carbohydrates");
        Double grassi = n.get("fat");
        if (kcal == null || proteine == null || carbo == null || grassi == null) {
            return null; // 4 macro obbligatori incompleti → fallback v2 / 422 a cura del chiamante
        }

        MacroDto macro = new MacroDto();
        macro.setCalorie(kcal);
        macro.setProteine(proteine);
        macro.setCarboidrati(carbo);
        macro.setGrassi(grassi);
        macro.setFibre(n.get("fiber"));
        macro.setZuccheri(n.get("sugars"));
        macro.setZuccheriAggiunti(n.get("added-sugars"));
        macro.setGrassiSaturi(n.get("saturated-fat"));
        macro.setGrassiTrans(n.get("trans-fat"));
        macro.setColesterolo(n.get("cholesterol"));
        macro.setSodio(n.get("sodium"));
        macro.setSale(n.get("salt"));          // sale etichettato OFF (preferito da getSaleEffettivo)
        macro.setEnergiaKj(n.get("energy-kj"));
        return macro;
    }

    /**
     * Normalizza i nutrienti per-100g in una mappa a chiavi canoniche
     * (energy-kcal, energy-kj, proteins, carbohydrates, fat, fiber, sugars, added-sugars,
     * saturated-fat, trans-fat, cholesterol, salt, sodium). Sorgenti, in ordine:
     *  1) v3.6: {@code nutrition.aggregated_set.nutrients} (solo per-100g), valore = value ?? value_computed;
     *  2) legacy/v2: {@code nutriments} piatto (chiavi *_100g).
     */
    private Map<String, Double> readNutrients(OffProductDto.Product product) {
        Map<String, Double> m = new java.util.HashMap<>();

        OffProductDto.Nutrition nutrition = product.getNutrition();
        if (nutrition != null && nutrition.getAggregatedSet() != null
                && nutrition.getAggregatedSet().getNutrients() != null) {
            OffProductDto.AggregatedSet set = nutrition.getAggregatedSet();
            boolean per100 = set.getPer() == null || "100g".equalsIgnoreCase(set.getPer());
            if (per100) {
                for (Map.Entry<String, OffProductDto.Nutrient> e : set.getNutrients().entrySet()) {
                    OffProductDto.Nutrient nut = e.getValue();
                    if (nut == null || e.getKey() == null) continue;
                    if (nut.getSourcePer() != null && !"100g".equalsIgnoreCase(nut.getSourcePer())) continue;
                    Double v = nut.getValue() != null ? nut.getValue() : nut.getValueComputed();
                    if (v != null) m.put(e.getKey().toLowerCase(), v);
                }
            }
            if (!m.isEmpty()) return m; // schema v3.6 valorizzato
        }

        // Fallback: vecchio campo piatto nutriments (v2 / v3 ≤ 3.4)
        OffProductDto.Nutriments leg = product.getNutriments();
        if (leg != null) {
            putIfNotNull(m, "energy-kcal", leg.getEnergyKcal100g());
            putIfNotNull(m, "energy-kj", leg.getEnergyKj100g());
            putIfNotNull(m, "proteins", leg.getProteins100g());
            putIfNotNull(m, "carbohydrates", leg.getCarbohydrates100g());
            putIfNotNull(m, "fat", leg.getFat100g());
            putIfNotNull(m, "fiber", leg.getFiber100g());
            putIfNotNull(m, "sugars", leg.getSugars100g());
            putIfNotNull(m, "added-sugars", leg.getAddedSugars100g());
            putIfNotNull(m, "saturated-fat", leg.getSaturatedFat100g());
            putIfNotNull(m, "trans-fat", leg.getTransFat100g());
            putIfNotNull(m, "cholesterol", leg.getCholesterol100g());
            putIfNotNull(m, "salt", leg.getSalt100g());
            putIfNotNull(m, "sodium", leg.getSodium100g());
        }
        return m;
    }

    private static void putIfNotNull(Map<String, Double> m, String key, Double value) {
        if (value != null) m.put(key, value);
    }

    /** Fallback v2: rifà il fetch del prodotto sulla v2 (nutrienti piatti). Ritorna null se non disponibile. */
    private OffProductDto.Product fetchV2Product(String barcode) {
        String url = UriComponentsBuilder.fromHttpUrl(OFF_PRODUCT_V2 + barcode + ".json")
                .queryParam("fields", FIELDS_PRODUCT_V2)
                .queryParam("lc", "it")
                .queryParam("cc", "it")
                .build().toUriString();
        try {
            OffProductDto r = restTemplate.getForObject(url, OffProductDto.class);
            if (r != null && r.getProduct() != null && !"failure".equalsIgnoreCase(r.getStatus())) {
                return r.getProduct();
            }
        } catch (RestClientException ignored) {
            // best-effort: se anche la v2 fallisce, il chiamante applica la policy 422
        }
        return null;
    }

    /** Allergeni tri-stato con match esatto: allergens_tags → PRESENTE, traces_tags → TRACCE, label free → ASSENTE, E220-228 → SOLFITI. */
    private Map<Allergene, StatoAllergene> buildAllergeni(OffProductDto.Product product) {
        Map<Allergene, StatoAllergene> map = new EnumMap<>(Allergene.class);

        if (product.getAllergensTags() != null) {
            for (String tag : product.getAllergensTags()) {
                Allergene a = OffTaxonomyMapping.fromTag(tag);
                if (a != null) map.put(a, StatoAllergene.PRESENTE);
            }
        }
        if (product.getTracesTags() != null) {
            for (String tag : product.getTracesTags()) {
                Allergene a = OffTaxonomyMapping.fromTag(tag);
                if (a != null) map.putIfAbsent(a, StatoAllergene.TRACCE);
            }
        }
        // Label "free" → ASSENTE (solo se non già PRESENTE/TRACCE). Attenzione: en:no-lactose NON tocca l'allergene LATTE.
        if (product.getLabelsTags() != null && product.getLabelsTags().contains("en:gluten-free")) {
            map.putIfAbsent(Allergene.GLUTINE, StatoAllergene.ASSENTE);
        }
        // Solfiti dagli additivi E220–E228 (override: PRESENTE)
        if (product.getAdditivesTags() != null) {
            for (String add : product.getAdditivesTags()) {
                if (OffTaxonomyMapping.isSulphiteAdditive(add)) {
                    map.put(Allergene.SOLFITI, StatoAllergene.PRESENTE);
                }
            }
        }
        return map;
    }

    /** Provenienza best-effort (E.5): import produttore → OFF_DICHIARATO, altrimenti OFF_DERIVATO. */
    private FonteAllergene resolveFonte(OffProductDto.Product product) {
        boolean manufacturer = false;
        if (product.getSources() != null) {
            for (OffProductDto.Source s : product.getSources()) {
                if (s != null && "1".equals(s.getManufacturer())) { manufacturer = true; break; }
            }
        }
        if (!manufacturer && product.getSourcesFields() != null) {
            for (String key : product.getSourcesFields().keySet()) {
                if (key != null && key.startsWith("org-")) { manufacturer = true; break; }
            }
        }
        return manufacturer ? FonteAllergene.OFF_DICHIARATO : FonteAllergene.OFF_DERIVATO;
    }

    /** Deriva i booleani legacy senzaGlutine/senzaLattosio/vegano dai dati OFF (retrocompat regole hardcoded). */
    private void deriveLegacyFlags(AlimentoBaseFormDto form, OffProductDto.Product product, Map<Allergene, StatoAllergene> allergeni) {
        StatoAllergene glu = allergeni.get(Allergene.GLUTINE);
        if (glu == StatoAllergene.PRESENTE || glu == StatoAllergene.TRACCE) form.setSenzaGlutine(false);
        else if (glu == StatoAllergene.ASSENTE) form.setSenzaGlutine(true);

        List<String> labels = product.getLabelsTags();
        boolean noLactose = labels != null && (labels.contains("en:no-lactose") || labels.contains("en:lactose-free"));
        StatoAllergene latte = allergeni.get(Allergene.LATTE);
        if (noLactose) form.setSenzaLattosio(true);
        else if (latte == StatoAllergene.PRESENTE || latte == StatoAllergene.TRACCE) form.setSenzaLattosio(false);

        List<String> analysis = product.getIngredientsAnalysisTags();
        boolean veganLabel = labels != null && (labels.contains("en:vegan") || labels.contains("it:vegano"));
        boolean veganAnalysis = analysis != null && analysis.contains("en:vegan");
        boolean nonVeganAnalysis = analysis != null && analysis.contains("en:non-vegan");
        if (veganLabel || veganAnalysis) form.setVegano(true);
        else if (nonVeganAnalysis) form.setVegano(false);
    }

    private Boolean computeNeedsReview(OffProductDto.Product product, Map<Allergene, StatoAllergene> allergeni) {
        // Solo gli ERRORS qualità: i WARNINGS sono troppo comuni su OFF (renderebbero needsReview quasi sempre true)
        boolean qualityIssues = product.getDataQualityErrorsTags() != null && !product.getDataQualityErrorsTags().isEmpty();
        boolean lowCompleteness = product.getCompleteness() != null && product.getCompleteness() < COMPLETENESS_REVIEW_THRESHOLD;
        boolean noAllergenData = allergeni.isEmpty()
                && (product.getAllergensTags() == null || product.getAllergensTags().isEmpty());
        return qualityIssues || lowCompleteness || noAllergenData;
    }

    // ═══════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════

    private Set<String> cleanTraces(List<String> tracesTags) {
        Set<String> out = new HashSet<>();
        if (tracesTags != null) {
            for (String t : tracesTags) {
                if (t == null) continue;
                out.add(t.replace("en:", "").replace("it:", "").replace("fr:", "").trim());
            }
        }
        return out;
    }

    /** Normalizza un grade OFF (a–e): lowercase; "unknown"/"not-applicable"/vuoto → null. */
    private String normalizeGrade(String grade) {
        if (grade == null) return null;
        String s = grade.trim().toLowerCase();
        if (s.isEmpty() || s.equals("unknown") || s.equals("not-applicable")) return null;
        return s;
    }

    private String firstNonBlank(String... values) {
        if (values != null) {
            for (String v : values) {
                if (v != null && !v.isBlank()) return v;
            }
        }
        return null;
    }

    private String sanitize(String s) {
        if (s == null || s.isBlank()) return "Sconosciuto";
        return s.replaceAll("<[^>]*>", "").trim();
    }

    private String sanitizeUrl(String url) {
        if (url == null) return null;
        if (url.startsWith("https://images.openfoodfacts.org/")) return url;
        return null;
    }

    private String extractFirstCategory(String categories) {
        if (categories == null || categories.isEmpty()) return "Altro";
        String first = sanitize(categories.split(",")[0].trim());
        return first.length() > 50 ? first.substring(0, 50) : first;
    }

    /** Categoria: flat {@code categories} se presente (v2), altrimenti dal {@code categories_tags}
     *  più specifico (v3.6 non popola più il flat categories). */
    private String resolveCategoria(OffProductDto.Product product) {
        String flat = product.getCategories();
        if (flat != null && !flat.isBlank()) return extractFirstCategory(flat);
        List<String> tags = product.getCategoriesTags();
        if (tags != null && !tags.isEmpty()) return humanizeTag(tags.get(tags.size() - 1));
        return "Altro";
    }

    /** Umanizza un tag OFF canonico: "en:wholemeal-rusks" → "Wholemeal rusks". */
    private String humanizeTag(String tag) {
        if (tag == null || tag.isBlank()) return "Altro";
        String t = tag.contains(":") ? tag.substring(tag.indexOf(':') + 1) : tag;
        t = sanitize(t.replace('-', ' ').trim());
        if (t.isBlank() || "Sconosciuto".equals(t)) return "Altro";
        t = Character.toUpperCase(t.charAt(0)) + t.substring(1);
        return t.length() > 50 ? t.substring(0, 50) : t;
    }
}
