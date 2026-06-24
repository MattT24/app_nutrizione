package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.BatchSize;

import it.nutrizionista.restnutrizionista.enums.Allergene;
import it.nutrizionista.restnutrizionista.enums.FonteAllergene;
import it.nutrizionista.restnutrizionista.enums.StatoAllergene;

@Entity
@Table(name = "alimenti_base",
       uniqueConstraints = @UniqueConstraint(name = "uq_alimento_owner_barcode", columnNames = {"created_by", "barcode"}))
@EntityListeners(AuditingEntityListener.class)
public class AlimentoBase {
 
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@Column(name = "nome", nullable = false)
	private String nome;
	
	@OneToMany(mappedBy = "alimento", fetch = FetchType.LAZY)
    private List<AlimentoPasto> alimentiScelti;
	

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false, mappedBy = "alimento")
	private Macro macroNutrienti;

    @Column(name = "misura_grammi", nullable = false)
    private Double misuraInGrammi;

    @OneToMany(mappedBy = "alimento", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ValoreMicro> micronutrienti = new HashSet<>();

    @Column(name = "categoria", length = 50)
    private String categoria;

	@Column(name = "url_immagine")
    private String urlImmagine;
    
    /** null = alimento globale (Admin), valorizzato = alimento privato del nutrizionista */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Utente createdBy;
    
    @CreatedDate
    @Column(nullable = false) 
    private Instant createdAt;
    @LastModifiedDate
    @Column(nullable = false) 
    private Instant updatedAt;
    
	@ElementCollection
    @CollectionTable(
        name = "alimento_tracce", 
        joinColumns = @JoinColumn(name = "alimento_id")
    )
    @Column(name = "nutriente")
    @BatchSize(size = 50)
    private Set<String> tracce = new HashSet<>();

    // ── Tag dietetici certificati (D3) ──────────────────────────────
    @Column(name = "senza_glutine")
    private Boolean senzaGlutine;

    @Column(name = "senza_lattosio")
    private Boolean senzaLattosio;

    @Column(name = "vegano")
    private Boolean vegano;

    // ── Integrazione OpenFoodFacts: barcode, allergeni tri-stato, score, qualità ──
    /** EAN/UPC dell'alimento OFF (null per CREA e alimenti manuali). Unique per (created_by, barcode). */
    @Column(name = "barcode", length = 20)
    private String barcode;

    /**
     * Allergeni tri-stato (Reg. UE 1169/2011). L'assenza di una entry = SCONOSCIUTO
     * (distinto da {@link StatoAllergene#ASSENTE}). LAZY + @BatchSize: niente N+1 nelle liste.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "alimento_allergene", joinColumns = @JoinColumn(name = "alimento_id"))
    @MapKeyColumn(name = "allergene", length = 32)
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "stato", length = 16)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 50)
    private Map<Allergene, StatoAllergene> allergeni = new EnumMap<>(Allergene.class);

    /** Provenienza prevalente dell'informazione allergeni (tracciabilità/confidenza). */
    @Enumerated(EnumType.STRING)
    @Column(name = "fonte_allergeni", length = 32)
    private FonteAllergene fonteAllergeni;

    /** Nutri-Score a–e (OFF). String per ospitare "unknown"/"not-applicable" → normalizzati a null in import. */
    @Column(name = "nutriscore_grade")
    private String nutriscoreGrade;

    /** Gruppo NOVA 1–4 (grado di processamento, OFF). */
    @Column(name = "nova_group")
    private Integer novaGroup;

    /** Green/Eco-Score a–e (OFF: environmental_score_grade ?? ecoscore_grade). */
    @Column(name = "environmental_score_grade")
    private String environmentalScoreGrade;

    /** Semaforo nutrienti OFF: chiavi fat/saturated-fat/sugars/salt → low/moderate/high. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "alimento_nutrient_level", joinColumns = @JoinColumn(name = "alimento_id"))
    @MapKeyColumn(name = "nutriente")
    @Column(name = "livello")
    @BatchSize(size = 50)
    private Map<String, String> nutrientLevels = new HashMap<>();

    /** Additivi OFF (E-number canonici, es. "en:e322"). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "alimento_additivo", joinColumns = @JoinColumn(name = "alimento_id"))
    @Column(name = "additivo")
    @BatchSize(size = 50)
    private Set<String> additivi = new HashSet<>();

    /** Testo ingredienti (lingua IT se disponibile, OFF). */
    @Column(name = "ingredients_text", columnDefinition = "TEXT")
    private String ingredientsText;

    /** Porzione in grammi (OFF serving_quantity). */
    @Column(name = "serving_quantity_g")
    private Double servingQuantityG;

    /** Completezza scheda OFF 0–1 (per needsReview). */
    @Column(name = "completezza_off")
    private Double completezzaOff;

    /** true se dati incompleti/contraddittori → richiede revisione del nutrizionista. */
    @Column(name = "needs_review")
    private Boolean needsReview;

    // Getter e Setter
    public Set<String> getTracce() {
        return tracce;
    }

    public void setTracce(Set<String> tracce) {
        this.tracce = tracce;
    }
	public String getUrlImmagine() {
		return urlImmagine;
	}
	public void setUrlImmagine(String urlImmagine) {
		this.urlImmagine = urlImmagine;
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Macro getMacronutrienti() {
		return macroNutrienti;
	}
	public void setMacronutrienti(Macro macronutrienti) {
		this.macroNutrienti = macronutrienti;
	}
	public Double getMisuraInGrammi() {
		return misuraInGrammi;
	}
	public void setMisuraInGrammi(Double misuraInGrammi) {
		this.misuraInGrammi = misuraInGrammi;
	}
	public Instant getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
	public Instant getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public List<AlimentoPasto> getAlimentiPasto() {
		return alimentiScelti;
	}
	public void setAlimentiPasto(List<AlimentoPasto> alimentiPasto) {
		this.alimentiScelti = alimentiPasto;
	}

	public List<AlimentoPasto> getAlimentiScelti() {
		return alimentiScelti;
	}
	public void setAlimentiScelti(List<AlimentoPasto> alimentiScelti) {
		this.alimentiScelti = alimentiScelti;
	}
	public Macro getMacroNutrienti() {
		return macroNutrienti;
	}
	public void setMacroNutrienti(Macro macroNutrienti) {
		this.macroNutrienti = macroNutrienti;
	}
	public Set<ValoreMicro> getMicronutrienti() {
		return micronutrienti;
	}
	public void setMicronutrienti(Set<ValoreMicro> micronutrienti) {
		this.micronutrienti = micronutrienti;
	}
	
	public String getCategoria() {
		return categoria;
	}
	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}
	public Utente getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(Utente createdBy) {
		this.createdBy = createdBy;
	}

    // ── Getters/Setters tag booleani (D3) ──────────────────────────
    public Boolean getSenzaGlutine() { return senzaGlutine; }
    public void setSenzaGlutine(Boolean senzaGlutine) { this.senzaGlutine = senzaGlutine; }

    public Boolean getSenzaLattosio() { return senzaLattosio; }
    public void setSenzaLattosio(Boolean senzaLattosio) { this.senzaLattosio = senzaLattosio; }

    public Boolean getVegano() { return vegano; }
    public void setVegano(Boolean vegano) { this.vegano = vegano; }

    // ── Getters/Setters integrazione OFF ──
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public Map<Allergene, StatoAllergene> getAllergeni() { return allergeni; }
    public void setAllergeni(Map<Allergene, StatoAllergene> allergeni) { this.allergeni = allergeni; }

    public FonteAllergene getFonteAllergeni() { return fonteAllergeni; }
    public void setFonteAllergeni(FonteAllergene fonteAllergeni) { this.fonteAllergeni = fonteAllergeni; }

    public String getNutriscoreGrade() { return nutriscoreGrade; }
    public void setNutriscoreGrade(String nutriscoreGrade) { this.nutriscoreGrade = nutriscoreGrade; }

    public Integer getNovaGroup() { return novaGroup; }
    public void setNovaGroup(Integer novaGroup) { this.novaGroup = novaGroup; }

    public String getEnvironmentalScoreGrade() { return environmentalScoreGrade; }
    public void setEnvironmentalScoreGrade(String environmentalScoreGrade) { this.environmentalScoreGrade = environmentalScoreGrade; }

    public Map<String, String> getNutrientLevels() { return nutrientLevels; }
    public void setNutrientLevels(Map<String, String> nutrientLevels) { this.nutrientLevels = nutrientLevels; }

    public Set<String> getAdditivi() { return additivi; }
    public void setAdditivi(Set<String> additivi) { this.additivi = additivi; }

    public String getIngredientsText() { return ingredientsText; }
    public void setIngredientsText(String ingredientsText) { this.ingredientsText = ingredientsText; }

    public Double getServingQuantityG() { return servingQuantityG; }
    public void setServingQuantityG(Double servingQuantityG) { this.servingQuantityG = servingQuantityG; }

    public Double getCompletezzaOff() { return completezzaOff; }
    public void setCompletezzaOff(Double completezzaOff) { this.completezzaOff = completezzaOff; }

    public Boolean getNeedsReview() { return needsReview; }
    public void setNeedsReview(Boolean needsReview) { this.needsReview = needsReview; }
}


