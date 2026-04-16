package it.nutrizionista.restnutrizionista.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "promemoria")
@EntityListeners(AuditingEntityListener.class)
public class Promemoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "utente_id")
    private Utente nutrizionista;

    @Column(nullable = false, length = 1000)
    private String testo;

    @Column(nullable = false)
    private LocalDate data;

    private LocalTime ora;

    @Column(name = "end_data")
    private LocalDate endData;

    @Column(name = "end_ora")
    private LocalTime endOra;

    private boolean allDay;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Promemoria() {
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Utente getNutrizionista() { return nutrizionista; }
    public void setNutrizionista(Utente nutrizionista) { this.nutrizionista = nutrizionista; }

    public String getTesto() { return testo; }
    public void setTesto(String testo) { this.testo = testo; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public LocalTime getOra() { return ora; }
    public void setOra(LocalTime ora) { this.ora = ora; }

    public LocalDate getEndData() { return endData; }
    public void setEndData(LocalDate endData) { this.endData = endData; }

    public LocalTime getEndOra() { return endOra; }
    public void setEndOra(LocalTime endOra) { this.endOra = endOra; }

    public boolean isAllDay() { return allDay; }
    public void setAllDay(boolean allDay) { this.allDay = allDay; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
