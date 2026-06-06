package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO interno per la deserializzazione della risposta Open Food Facts API v2.
 * 
 * Struttura verificata su dati reali (barcode 8076802085738 — Barilla Penne Rigate N°73).
 *
 * @architecture: DTO di confine — non viene mai esposto al frontend.
 *   Il mapping OFF → AlimentoBaseFormDto avviene nel Controller.
 * @security-auditor: @JsonIgnoreProperties(ignoreUnknown = true) su tutte le classi
 *   per ignorare campi non previsti e prevenire deserialization attacks.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OffProductDto {

    private int status;

    @JsonProperty("status_verbose")
    private String statusVerbose;

    private Product product;

    // ── Inner: Product ──

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Product {

        @JsonProperty("product_name")
        private String productName;

        private String brands;

        private String categories;

        @JsonProperty("image_url")
        private String imageUrl;

        private String code;

        @JsonProperty("product_name_it")
        private String productNameIt;

        private Nutriments nutriments;

        @JsonProperty("allergens_tags")
        private List<String> allergensTags;

        @JsonProperty("traces_tags")
        private List<String> tracesTags;

        @JsonProperty("labels_tags")
        private List<String> labelsTags;

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getBrands() { return brands; }
        public void setBrands(String brands) { this.brands = brands; }
        public String getCategories() { return categories; }
        public void setCategories(String categories) { this.categories = categories; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getProductNameIt() { return productNameIt; }
        public void setProductNameIt(String productNameIt) { this.productNameIt = productNameIt; }
        public Nutriments getNutriments() { return nutriments; }
        public void setNutriments(Nutriments nutriments) { this.nutriments = nutriments; }
        public List<String> getAllergensTags() { return allergensTags; }
        public void setAllergensTags(List<String> allergensTags) { this.allergensTags = allergensTags; }
        public List<String> getTracesTags() { return tracesTags; }
        public void setTracesTags(List<String> tracesTags) { this.tracesTags = tracesTags; }
        public List<String> getLabelsTags() { return labelsTags; }
        public void setLabelsTags(List<String> labelsTags) { this.labelsTags = labelsTags; }
    }

    // ── Inner: Nutriments ──
    // Nomi campo verificati su risposta reale OFF:
    //   energy-kcal_100g, proteins_100g, carbohydrates_100g, fat_100g,
    //   fiber_100g, sugars_100g, saturated-fat_100g, sodium_100g

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Nutriments {

        @JsonProperty("energy-kcal_100g")
        private Double energyKcal100g;

        @JsonProperty("proteins_100g")
        private Double proteins100g;

        @JsonProperty("carbohydrates_100g")
        private Double carbohydrates100g;

        @JsonProperty("fat_100g")
        private Double fat100g;

        @JsonProperty("fiber_100g")
        private Double fiber100g;

        @JsonProperty("sugars_100g")
        private Double sugars100g;

        @JsonProperty("saturated-fat_100g")
        private Double saturatedFat100g;

        @JsonProperty("sodium_100g")
        private Double sodium100g;

        public Double getEnergyKcal100g() { return energyKcal100g; }
        public void setEnergyKcal100g(Double energyKcal100g) { this.energyKcal100g = energyKcal100g; }
        public Double getProteins100g() { return proteins100g; }
        public void setProteins100g(Double proteins100g) { this.proteins100g = proteins100g; }
        public Double getCarbohydrates100g() { return carbohydrates100g; }
        public void setCarbohydrates100g(Double carbohydrates100g) { this.carbohydrates100g = carbohydrates100g; }
        public Double getFat100g() { return fat100g; }
        public void setFat100g(Double fat100g) { this.fat100g = fat100g; }
        public Double getFiber100g() { return fiber100g; }
        public void setFiber100g(Double fiber100g) { this.fiber100g = fiber100g; }
        public Double getSugars100g() { return sugars100g; }
        public void setSugars100g(Double sugars100g) { this.sugars100g = sugars100g; }
        public Double getSaturatedFat100g() { return saturatedFat100g; }
        public void setSaturatedFat100g(Double saturatedFat100g) { this.saturatedFat100g = saturatedFat100g; }
        public Double getSodium100g() { return sodium100g; }
        public void setSodium100g(Double sodium100g) { this.sodium100g = sodium100g; }
    }

    // ── Getters/Setters root ──
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getStatusVerbose() { return statusVerbose; }
    public void setStatusVerbose(String statusVerbose) { this.statusVerbose = statusVerbose; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}
