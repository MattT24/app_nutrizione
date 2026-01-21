package it.nutrizionista.restnutrizionista.entity;
 
import java.time.Instant;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "valori_micronutrienti",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {
            "alimento_id",
            "micronutriente_id"
        })
    }
)
public class ValoreMicro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alimento_id", nullable = false)
    private AlimentoBase alimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "micronutriente_id", nullable = false)
    private Micro micronutriente;

    @Column(nullable = false)
    private Double valore;
    
    @CreatedDate
    @Column(nullable = false) 
    private Instant createdAt;
    @LastModifiedDate
    @Column(nullable = false) 
    private Instant updatedAt;
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public AlimentoBase getAlimento() {
		return alimento;
	}
	public void setAlimento(AlimentoBase alimento) {
		this.alimento = alimento;
	}
	public Micro getMicronutriente() {
		return micronutriente;
	}
	public void setMicronutriente(Micro micronutriente) {
		this.micronutriente = micronutriente;
	}
	public Double getValore() {
		return valore;
	}
	public void setValore(Double valore) {
		this.valore = valore;
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
    
	@Override
	public boolean equals(Object o) {
	    if (this == o) return true;
	    if (!(o instanceof ValoreMicro)) return false;
	    ValoreMicro that = (ValoreMicro) o;
	    return alimento.equals(that.alimento)
	        && micronutriente.equals(that.micronutriente);
	}

	@Override
	public int hashCode() {
	    return Objects.hash(alimento, micronutriente);
	}
}

