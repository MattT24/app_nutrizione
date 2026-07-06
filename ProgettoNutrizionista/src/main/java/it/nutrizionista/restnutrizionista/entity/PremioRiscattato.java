package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;

import it.nutrizionista.restnutrizionista.enums.TipoPremio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Storico dei premi riscattati con i punti gamification (sconti mensili e mese gratis).
 * Non esiste ancora un sistema di billing reale: questa riga è il "buono" registrato che
 * il team applicherà manualmente finché non verrà collegato un sistema di pagamento.
 */
@Entity
@Table(name = "premi_riscattati")
@EntityListeners(AuditingEntityListener.class)
public class PremioRiscattato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente nutrizionista;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_premio", nullable = false)
    private TipoPremio tipoPremio;

    @Column(name = "punti_spesi", nullable = false)
    private int puntiSpesi;

    @CreatedDate
    @Column(name = "data_riscatto", nullable = false, updatable = false)
    private Instant dataRiscatto;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Utente getNutrizionista() { return nutrizionista; }
    public void setNutrizionista(Utente nutrizionista) { this.nutrizionista = nutrizionista; }

    public TipoPremio getTipoPremio() { return tipoPremio; }
    public void setTipoPremio(TipoPremio tipoPremio) { this.tipoPremio = tipoPremio; }

    public int getPuntiSpesi() { return puntiSpesi; }
    public void setPuntiSpesi(int puntiSpesi) { this.puntiSpesi = puntiSpesi; }

    public Instant getDataRiscatto() { return dataRiscatto; }
    public void setDataRiscatto(Instant dataRiscatto) { this.dataRiscatto = dataRiscatto; }
}
