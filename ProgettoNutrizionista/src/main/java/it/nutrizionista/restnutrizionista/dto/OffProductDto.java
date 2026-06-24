package it.nutrizionista.restnutrizionista.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO interno per la deserializzazione della risposta Open Food Facts <strong>API v3.6</strong>
 * ({@code GET /api/v3.6/product/{barcode}.json}).
 *
 * <p>Envelope v3 (diverso da v2): {@code status} è una STRINGA
 * ({@code success}/{@code success_with_warnings}/{@code success_with_errors}/{@code failure}),
 * più {@code result}, {@code errors[]}, {@code warnings[]}, {@code product{}}.
 * Il controllo "trovato" è {@code product != null} (NON più {@code status == 1}).
 *
 * <p>Provenienza (verifica LIVE, piano E.5): {@code tags_sources} è oggi <em>assente</em>; si usano
 * {@code sources}/{@code sources_fields}. Eco-score (E.6): live torna {@code ecoscore_grade} (legacy),
 * quindi si leggono entrambi.
 *
 * @architecture DTO di confine — non viene mai esposto al frontend.
 * @security {@code @JsonIgnoreProperties(ignoreUnknown = true)} su tutte le classi annidate.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OffProductDto {

    private String status;            // "success" | "success_with_warnings" | "success_with_errors" | "failure"
    private Result result;
    private List<Object> errors;
    private List<Object> warnings;
    private Product product;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private String id;
        private String name;
        @JsonProperty("lc_name") private String lcName;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLcName() { return lcName; }
        public void setLcName(String lcName) { this.lcName = lcName; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Source {        // elemento di sources[] (E.5)
        private String id;
        private String name;
        private String url;
        private String manufacturer;    // "1" se import produttore → OFF_DICHIARATO
        @JsonProperty("import_t") private Long importT;
        private List<String> fields;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getManufacturer() { return manufacturer; }
        public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
        public Long getImportT() { return importT; }
        public void setImportT(Long importT) { this.importT = importT; }
        public List<String> getFields() { return fields; }
        public void setFields(List<String> fields) { this.fields = fields; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Product {
        private String code;
        @JsonProperty("product_name") private String productName;
        @JsonProperty("product_name_it") private String productNameIt;
        @JsonProperty("generic_name_it") private String genericNameIt;
        private String brands;
        private String categories;
        @JsonProperty("categories_tags") private List<String> categoriesTags;
        @JsonProperty("labels_tags") private List<String> labelsTags;
        @JsonProperty("image_url") private String imageUrl;
        @JsonProperty("image_front_url") private String imageFrontUrl;
        @JsonProperty("image_ingredients_url") private String imageIngredientsUrl;
        @JsonProperty("image_nutrition_url") private String imageNutritionUrl;
        @JsonProperty("serving_quantity") private Double servingQuantity;

        @JsonProperty("allergens_tags") private List<String> allergensTags;
        @JsonProperty("traces_tags") private List<String> tracesTags;
        @JsonProperty("ingredients_text_it") private String ingredientsTextIt;
        @JsonProperty("ingredients_tags") private List<String> ingredientsTags;
        @JsonProperty("ingredients_analysis_tags") private List<String> ingredientsAnalysisTags;
        @JsonProperty("additives_tags") private List<String> additivesTags;

        @JsonProperty("nutriscore_grade") private String nutriscoreGrade;
        @JsonProperty("nova_group") private Integer novaGroup;
        @JsonProperty("environmental_score_grade") private String environmentalScoreGrade; // v3.6 (rename in corso)
        @JsonProperty("ecoscore_grade") private String ecoscoreGrade;                       // legacy (torna live)
        @JsonProperty("nutrient_levels") private Map<String, String> nutrientLevels;

        @JsonProperty("data_quality_errors_tags") private List<String> dataQualityErrorsTags;
        @JsonProperty("data_quality_warnings_tags") private List<String> dataQualityWarningsTags;
        @JsonProperty("states_tags") private List<String> statesTags;
        private Double completeness;

        // Provenienza (E.5): tags_sources assente live → usare sources/sources_fields
        private List<Source> sources;
        @JsonProperty("sources_fields") private Map<String, Object> sourcesFields;
        @JsonProperty("tags_sources") private Map<String, Object> tagsSources; // opzionale/futuro

        private Nutriments nutriments;
        @JsonProperty("nutrition") private Nutrition nutrition;  // v3.5+ (schema 1003+): nuova struttura nutrienti annidata

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getProductNameIt() { return productNameIt; }
        public void setProductNameIt(String productNameIt) { this.productNameIt = productNameIt; }
        public String getGenericNameIt() { return genericNameIt; }
        public void setGenericNameIt(String genericNameIt) { this.genericNameIt = genericNameIt; }
        public String getBrands() { return brands; }
        public void setBrands(String brands) { this.brands = brands; }
        public String getCategories() { return categories; }
        public void setCategories(String categories) { this.categories = categories; }
        public List<String> getCategoriesTags() { return categoriesTags; }
        public void setCategoriesTags(List<String> categoriesTags) { this.categoriesTags = categoriesTags; }
        public List<String> getLabelsTags() { return labelsTags; }
        public void setLabelsTags(List<String> labelsTags) { this.labelsTags = labelsTags; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getImageFrontUrl() { return imageFrontUrl; }
        public void setImageFrontUrl(String imageFrontUrl) { this.imageFrontUrl = imageFrontUrl; }
        public String getImageIngredientsUrl() { return imageIngredientsUrl; }
        public void setImageIngredientsUrl(String imageIngredientsUrl) { this.imageIngredientsUrl = imageIngredientsUrl; }
        public String getImageNutritionUrl() { return imageNutritionUrl; }
        public void setImageNutritionUrl(String imageNutritionUrl) { this.imageNutritionUrl = imageNutritionUrl; }
        public Double getServingQuantity() { return servingQuantity; }
        public void setServingQuantity(Double servingQuantity) { this.servingQuantity = servingQuantity; }
        public List<String> getAllergensTags() { return allergensTags; }
        public void setAllergensTags(List<String> allergensTags) { this.allergensTags = allergensTags; }
        public List<String> getTracesTags() { return tracesTags; }
        public void setTracesTags(List<String> tracesTags) { this.tracesTags = tracesTags; }
        public String getIngredientsTextIt() { return ingredientsTextIt; }
        public void setIngredientsTextIt(String ingredientsTextIt) { this.ingredientsTextIt = ingredientsTextIt; }
        public List<String> getIngredientsTags() { return ingredientsTags; }
        public void setIngredientsTags(List<String> ingredientsTags) { this.ingredientsTags = ingredientsTags; }
        public List<String> getIngredientsAnalysisTags() { return ingredientsAnalysisTags; }
        public void setIngredientsAnalysisTags(List<String> ingredientsAnalysisTags) { this.ingredientsAnalysisTags = ingredientsAnalysisTags; }
        public List<String> getAdditivesTags() { return additivesTags; }
        public void setAdditivesTags(List<String> additivesTags) { this.additivesTags = additivesTags; }
        public String getNutriscoreGrade() { return nutriscoreGrade; }
        public void setNutriscoreGrade(String nutriscoreGrade) { this.nutriscoreGrade = nutriscoreGrade; }
        public Integer getNovaGroup() { return novaGroup; }
        public void setNovaGroup(Integer novaGroup) { this.novaGroup = novaGroup; }
        public String getEnvironmentalScoreGrade() { return environmentalScoreGrade; }
        public void setEnvironmentalScoreGrade(String environmentalScoreGrade) { this.environmentalScoreGrade = environmentalScoreGrade; }
        public String getEcoscoreGrade() { return ecoscoreGrade; }
        public void setEcoscoreGrade(String ecoscoreGrade) { this.ecoscoreGrade = ecoscoreGrade; }
        public Map<String, String> getNutrientLevels() { return nutrientLevels; }
        public void setNutrientLevels(Map<String, String> nutrientLevels) { this.nutrientLevels = nutrientLevels; }
        public List<String> getDataQualityErrorsTags() { return dataQualityErrorsTags; }
        public void setDataQualityErrorsTags(List<String> dataQualityErrorsTags) { this.dataQualityErrorsTags = dataQualityErrorsTags; }
        public List<String> getDataQualityWarningsTags() { return dataQualityWarningsTags; }
        public void setDataQualityWarningsTags(List<String> dataQualityWarningsTags) { this.dataQualityWarningsTags = dataQualityWarningsTags; }
        public List<String> getStatesTags() { return statesTags; }
        public void setStatesTags(List<String> statesTags) { this.statesTags = statesTags; }
        public Double getCompleteness() { return completeness; }
        public void setCompleteness(Double completeness) { this.completeness = completeness; }
        public List<Source> getSources() { return sources; }
        public void setSources(List<Source> sources) { this.sources = sources; }
        public Map<String, Object> getSourcesFields() { return sourcesFields; }
        public void setSourcesFields(Map<String, Object> sourcesFields) { this.sourcesFields = sourcesFields; }
        public Map<String, Object> getTagsSources() { return tagsSources; }
        public void setTagsSources(Map<String, Object> tagsSources) { this.tagsSources = tagsSources; }
        public Nutriments getNutriments() { return nutriments; }
        public void setNutriments(Nutriments nutriments) { this.nutriments = nutriments; }
        public Nutrition getNutrition() { return nutrition; }
        public void setNutrition(Nutrition nutrition) { this.nutrition = nutrition; }
    }

    /**
     * Nuova struttura nutrienti OFF (v3.5+, schema 1003+): i valori sono in
     * {@code nutrition.aggregated_set.nutrients["<chiave>"].value} (es. "energy-kcal","proteins"),
     * NON più nel vecchio oggetto piatto {@code nutriments} (che la v3.6 ritorna vuoto).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Nutrition {
        @JsonProperty("aggregated_set") private AggregatedSet aggregatedSet;
        public AggregatedSet getAggregatedSet() { return aggregatedSet; }
        public void setAggregatedSet(AggregatedSet aggregatedSet) { this.aggregatedSet = aggregatedSet; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AggregatedSet {
        private Map<String, Nutrient> nutrients;
        private String per;          // "100g" | "serving"
        private String preparation;  // "as_sold" | "prepared"
        public Map<String, Nutrient> getNutrients() { return nutrients; }
        public void setNutrients(Map<String, Nutrient> nutrients) { this.nutrients = nutrients; }
        public String getPer() { return per; }
        public void setPer(String per) { this.per = per; }
        public String getPreparation() { return preparation; }
        public void setPreparation(String preparation) { this.preparation = preparation; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Nutrient {
        private Double value;
        @JsonProperty("value_computed") private Double valueComputed;
        private String unit;
        @JsonProperty("source_per") private String sourcePer;
        private String modifier;     // "~","<",">" (stima/approssimazione)
        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
        public Double getValueComputed() { return valueComputed; }
        public void setValueComputed(Double valueComputed) { this.valueComputed = valueComputed; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getSourcePer() { return sourcePer; }
        public void setSourcePer(String sourcePer) { this.sourcePer = sourcePer; }
        public String getModifier() { return modifier; }
        public void setModifier(String modifier) { this.modifier = modifier; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Nutriments {
        @JsonProperty("energy-kcal_100g") private Double energyKcal100g;
        @JsonProperty("energy-kj_100g") private Double energyKj100g;
        @JsonProperty("proteins_100g") private Double proteins100g;
        @JsonProperty("carbohydrates_100g") private Double carbohydrates100g;
        @JsonProperty("sugars_100g") private Double sugars100g;
        @JsonProperty("added-sugars_100g") private Double addedSugars100g;
        @JsonProperty("fat_100g") private Double fat100g;
        @JsonProperty("saturated-fat_100g") private Double saturatedFat100g;
        @JsonProperty("trans-fat_100g") private Double transFat100g;
        @JsonProperty("cholesterol_100g") private Double cholesterol100g;
        @JsonProperty("fiber_100g") private Double fiber100g;
        @JsonProperty("salt_100g") private Double salt100g;
        @JsonProperty("sodium_100g") private Double sodium100g;
        @JsonProperty("calcium_100g") private Double calcium100g;
        @JsonProperty("iron_100g") private Double iron100g;
        @JsonProperty("potassium_100g") private Double potassium100g;

        public Double getEnergyKcal100g() { return energyKcal100g; }
        public void setEnergyKcal100g(Double v) { this.energyKcal100g = v; }
        public Double getEnergyKj100g() { return energyKj100g; }
        public void setEnergyKj100g(Double v) { this.energyKj100g = v; }
        public Double getProteins100g() { return proteins100g; }
        public void setProteins100g(Double v) { this.proteins100g = v; }
        public Double getCarbohydrates100g() { return carbohydrates100g; }
        public void setCarbohydrates100g(Double v) { this.carbohydrates100g = v; }
        public Double getSugars100g() { return sugars100g; }
        public void setSugars100g(Double v) { this.sugars100g = v; }
        public Double getAddedSugars100g() { return addedSugars100g; }
        public void setAddedSugars100g(Double v) { this.addedSugars100g = v; }
        public Double getFat100g() { return fat100g; }
        public void setFat100g(Double v) { this.fat100g = v; }
        public Double getSaturatedFat100g() { return saturatedFat100g; }
        public void setSaturatedFat100g(Double v) { this.saturatedFat100g = v; }
        public Double getTransFat100g() { return transFat100g; }
        public void setTransFat100g(Double v) { this.transFat100g = v; }
        public Double getCholesterol100g() { return cholesterol100g; }
        public void setCholesterol100g(Double v) { this.cholesterol100g = v; }
        public Double getFiber100g() { return fiber100g; }
        public void setFiber100g(Double v) { this.fiber100g = v; }
        public Double getSalt100g() { return salt100g; }
        public void setSalt100g(Double v) { this.salt100g = v; }
        public Double getSodium100g() { return sodium100g; }
        public void setSodium100g(Double v) { this.sodium100g = v; }
        public Double getCalcium100g() { return calcium100g; }
        public void setCalcium100g(Double v) { this.calcium100g = v; }
        public Double getIron100g() { return iron100g; }
        public void setIron100g(Double v) { this.iron100g = v; }
        public Double getPotassium100g() { return potassium100g; }
        public void setPotassium100g(Double v) { this.potassium100g = v; }
    }

    // ── Getters/Setters root ──
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Result getResult() { return result; }
    public void setResult(Result result) { this.result = result; }
    public List<Object> getErrors() { return errors; }
    public void setErrors(List<Object> errors) { this.errors = errors; }
    public List<Object> getWarnings() { return warnings; }
    public void setWarnings(List<Object> warnings) { this.warnings = warnings; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}
