package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.enums.AuditOutcome;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Audit log GDPR (finding A7) degli accessi ai dati sanitari: log <b>append-only e immutabile</b>
 * (chi / cosa / quando / da dove / paziente / tipo / esito).
 *
 * <p>Scelte di design:
 * <ul>
 *   <li><b>Nessuna FK</b> su {@code utente_id}/{@code cliente_id}: l'audit deve sopravvivere alla
 *       cancellazione dell'account o del cliente (retention &ge; 24 mesi, accountability). L'attore è
 *       anche denormalizzato in {@code utente_email} (snapshot del "chi").</li>
 *   <li><b>Immutabilità</b>: nessun setter; l'unico costruttore valorizza tutti i campi alla creazione;
 *       {@code created_at} è {@code updatable=false} e popolato da {@link AuditingEntityListener}. Non
 *       esiste alcun percorso di UPDATE nel codice.</li>
 *   <li>PK surrogata singola (TiDB vieta {@code ALTER ... MODIFY} su colonne di PRIMARY KEY).</li>
 * </ul>
 * Popolato esplicitamente dai service via {@code AuditService} (vedi CLAUDE.md — regola di coverage).
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_cliente_data", columnList = "cliente_id, created_at"),
        @Index(name = "idx_audit_utente_data", columnList = "utente_id, created_at"),
        @Index(name = "idx_audit_action_data", columnList = "action, created_at"),
        @Index(name = "idx_audit_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Attore (nutrizionista/utente). Plain Long, NO FK: sopravvive alla cancellazione dell'account. */
    @Column(name = "utente_id")
    private Long utenteId;

    /** Snapshot dell'email dell'attore (il "chi" resta identificabile anche se l'account non esiste più). */
    @Column(name = "utente_email", length = 255)
    private String utenteEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 32)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 48)
    private AuditEntityType entityType;

    /** Id della risorsa acceduta (null per le operazioni di tipo {@code LIST}). */
    @Column(name = "entity_id")
    private Long entityId;

    /** Paziente. Plain Long, NO FK: sopravvive alla cancellazione del cliente / retention. */
    @Column(name = "cliente_id")
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "esito", nullable = false, length = 16)
    private AuditOutcome esito;

    /** Origine ("da dove"): IPv6-safe (length 45). */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /** Per {@code SHARE}: destinatario dell'email (dove è finito il dato). */
    @Column(name = "destinatario", length = 255)
    private String destinatario;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    /** Richiesto da JPA. Non usare direttamente: le righe di audit si creano solo via {@code AuditService}. */
    protected AuditLog() {
    }

    /**
     * Unico costruttore. Tutti i campi sono definiti alla costruzione; non esistono setter →
     * l'entità è immutabile dopo la creazione. {@code id} è generato e {@code createdAt} è
     * popolato da {@link AuditingEntityListener} al persist.
     */
    public AuditLog(Long utenteId, String utenteEmail, AuditAction action, AuditEntityType entityType,
                    Long entityId, Long clienteId, AuditOutcome esito, String ipAddress,
                    String userAgent, String destinatario) {
        this.utenteId = utenteId;
        this.utenteEmail = utenteEmail;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.clienteId = clienteId;
        this.esito = esito;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.destinatario = destinatario;
    }

    // Solo getter — nessun setter (immutabilità applicativa).
    public Long getId() {
        return id;
    }

    public Long getUtenteId() {
        return utenteId;
    }

    public String getUtenteEmail() {
        return utenteEmail;
    }

    public AuditAction getAction() {
        return action;
    }

    public AuditEntityType getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public AuditOutcome getEsito() {
        return esito;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
