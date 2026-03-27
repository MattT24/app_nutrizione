package it.nutrizionista.restnutrizionista.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "documenti_fascicolo")
@EntityListeners(AuditingEntityListener.class)
public class DocumentoFascicolo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private String titolo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(name = "percorso_file", nullable = false, length = 1000)
    private String percorsoFile;

    @Column(name = "riferimento_id")
    private Long riferimentoId;

    @CreatedDate
    @Column(name = "data_creazione", nullable = false, updatable = false)
    private Instant dataCreazione;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getPercorsoFile() {
        return percorsoFile;
    }

    public void setPercorsoFile(String percorsoFile) {
        this.percorsoFile = percorsoFile;
    }

    public Long getRiferimentoId() {
        return riferimentoId;
    }

    public void setRiferimentoId(Long riferimentoId) {
        this.riferimentoId = riferimentoId;
    }

    public Instant getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(Instant dataCreazione) {
        this.dataCreazione = dataCreazione;
    }
}
