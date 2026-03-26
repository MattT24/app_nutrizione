package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import it.nutrizionista.restnutrizionista.dto.AlimentoBaseDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoBaseFormDto;
import it.nutrizionista.restnutrizionista.dto.MacroDto;
import it.nutrizionista.restnutrizionista.dto.OffProductDto;

/**
 * Service per l'integrazione con Open Food Facts.
 *
 * Ricerca: Search-a-licious (Elasticsearch, veloce, no Anubis) + @Cacheable
 * Import: OFF API v2 /product (per barcode)
 *
 * @security-auditor: SSRF = URL costante + input validato, Data Poisoning = sanitize(),
 *                    Image whitelist = solo images.openfoodfacts.org, IDOR = createPersonale via JWT
 */
@Service
public class OpenFoodFactsService {

    /** Search-a-licious: motore di ricerca OFF basato su Elasticsearch */
    private static final String SEARCH_URL = "https://search.openfoodfacts.org/search";
    /** OFF API v2 per fetch singolo prodotto per barcode */
    private static final String OFF_PRODUCT = "https://world.openfoodfacts.org/api/v2/product/";
    private static final String FIELDS = "code,product_name,brands,categories,image_url,nutriments";

    @Autowired private RestTemplate restTemplate;
    @Autowired private AlimentoBaseService alimentoBaseService;

    /**
     * Ricerca prodotti su OFF via Search-a-licious.
     * Risultati cachati per 10 minuti (stessa query + pagina = stessa risposta).
     *
     * @param query  testo di ricerca (min 2 caratteri)
     * @param page   pagina (1-based)
     * @param size   risultati per pagina (max 24)
     * @return JSON grezzo della risposta Search-a-licious
     */
    @Cacheable(value = "offSearch", key = "#query.toLowerCase() + '_' + #page + '_' + #size")
    public String searchProducts(String query, int page, int size) {
        if (query == null || query.trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query minimo 2 caratteri");
        }

        String safeQuery = sanitize(query).replace(" ", "+");
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 24);

        String url = SEARCH_URL
                + "?q=" + safeQuery
                + "&langs=it,en"
                + "&page=" + safePage
                + "&page_size=" + safeSize
                + "&fields=" + FIELDS;

        try {
            return restTemplate.getForObject(url, String.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Impossibile raggiungere Open Food Facts");
        }
    }

    /**
     * Importa un prodotto OFF nel catalogo personale del nutrizionista corrente.
     *
     * @param barcode EAN-8/EAN-13/UPC (solo cifre, 8-13 caratteri)
     * @return DTO dell'alimento appena salvato
     */
    public AlimentoBaseDto importProduct(String barcode) {
        if (barcode == null || !barcode.matches("^\\d{8,13}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Barcode non valido");
        }

        String url = OFF_PRODUCT + barcode + ".json?fields=" + FIELDS;

        OffProductDto response;
        try {
            response = restTemplate.getForObject(url, OffProductDto.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Impossibile raggiungere Open Food Facts");
        }

        if (response == null || response.getStatus() != 1 || response.getProduct() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Prodotto non trovato su Open Food Facts");
        }

        AlimentoBaseFormDto form = mapToFormDto(response.getProduct());
        return alimentoBaseService.createPersonale(form);
    }

    // ═══════════════════════════════════════════════════════════
    //  Mapping & Sanitizzazione
    // ═══════════════════════════════════════════════════════════

    private AlimentoBaseFormDto mapToFormDto(OffProductDto.Product product) {
        OffProductDto.Nutriments nut = product.getNutriments();
        AlimentoBaseFormDto form = new AlimentoBaseFormDto();

        String nome = sanitize(product.getProductName());
        String brand = sanitize(product.getBrands());
        String baseName = !brand.equals("Sconosciuto") ? nome + " — " + brand : nome;
        form.setNome(baseName + " — OFF");

        form.setCategoria(extractFirstCategory(product.getCategories()));
        form.setUrlImmagine(sanitizeUrl(product.getImageUrl()));
        form.setMisuraInGrammi(100.0);

        MacroDto macro = new MacroDto();
        macro.setCalorie(safe(nut != null ? nut.getEnergyKcal100g() : null));
        macro.setProteine(safe(nut != null ? nut.getProteins100g() : null));
        macro.setCarboidrati(safe(nut != null ? nut.getCarbohydrates100g() : null));
        macro.setGrassi(safe(nut != null ? nut.getFat100g() : null));
        macro.setFibre(safe(nut != null ? nut.getFiber100g() : null));
        macro.setZuccheri(safe(nut != null ? nut.getSugars100g() : null));
        macro.setGrassiSaturi(safe(nut != null ? nut.getSaturatedFat100g() : null));
        macro.setSodio(safe(nut != null ? nut.getSodium100g() : null));
        form.setMacroNutrienti(macro);

        return form;
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
        String first = categories.split(",")[0].trim();
        first = sanitize(first);
        return first.length() > 50 ? first.substring(0, 50) : first;
    }

    private double safe(Double d) {
        return d != null ? d : 0.0;
    }
}
