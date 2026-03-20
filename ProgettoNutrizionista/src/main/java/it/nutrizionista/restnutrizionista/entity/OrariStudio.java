package it.nutrizionista.restnutrizionista.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "orari_studio")
@EntityListeners(AuditingEntityListener.class)
public class OrariStudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Aggiunto collegamento obbligatorio al Nutrizionista!
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nutrizionista_id", nullable = false)
    private Utente nutrizionista;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek giornoSettimana;

    @Column(nullable = false)
    private boolean giornoLavorativo;

    // Legacy field to avoid 500 DB Constraint Error without dropping column via SQL
    @Column(name = "lavora_sabato", nullable = false, columnDefinition = "boolean default false")
    private boolean lavoraSabato = false;

    private LocalTime oraApertura;
    private LocalTime oraChiusura;

    private LocalTime inizioPausaPranzo;
    private LocalTime finePausaPranzo;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public OrariStudio() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Utente getNutrizionista() { return nutrizionista; }
    public void setNutrizionista(Utente nutrizionista) { this.nutrizionista = nutrizionista; }



    public DayOfWeek getGiornoSettimana() { return giornoSettimana; }
    public void setGiornoSettimana(DayOfWeek giornoSettimana) { this.giornoSettimana = giornoSettimana; }

    public boolean isGiornoLavorativo() { return giornoLavorativo; }
    public void setGiornoLavorativo(boolean giornoLavorativo) { this.giornoLavorativo = giornoLavorativo; }

    public LocalTime getOraApertura() { return oraApertura; }
    public void setOraApertura(LocalTime oraApertura) { this.oraApertura = oraApertura; }

    public LocalTime getOraChiusura() { return oraChiusura; }
    public void setOraChiusura(LocalTime oraChiusura) { this.oraChiusura = oraChiusura; }

    public LocalTime getInizioPausaPranzo() { return inizioPausaPranzo; }
    public void setInizioPausaPranzo(LocalTime inizioPausaPranzo) { this.inizioPausaPranzo = inizioPausaPranzo; }

    public LocalTime getFinePausaPranzo() { return finePausaPranzo; }
    public void setFinePausaPranzo(LocalTime finePausaPranzo) { this.finePausaPranzo = finePausaPranzo; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}