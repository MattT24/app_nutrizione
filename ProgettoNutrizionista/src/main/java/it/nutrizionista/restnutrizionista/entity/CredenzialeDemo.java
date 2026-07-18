package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * Credenziale separata usata esclusivamente dal login demo. L'account standard
 * continua a usare email/password di {@link Utente} senza alcun cambio di flusso.
 */
@Entity
@Table(name = "credenziali_demo",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_credenziale_demo_username", columnNames = "username"),
            @UniqueConstraint(name = "uk_credenziale_demo_utente", columnNames = "utente_id")
        },
        indexes = {
            @Index(name = "idx_demo_scadenza", columnList = "expires_at"),
            @Index(name = "idx_demo_disabilitato_scadenza", columnList = "disabled_at, expires_at")
        })
@EntityListeners(AuditingEntityListener.class)
public class CredenzialeDemo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente utente;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Column(name = "token_version", nullable = false)
    private long tokenVersion;

    @Column(name = "created_by_admin_id", nullable = false)
    private Long createdByAdminId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Utente getUtente() { return utente; }
    public void setUtente(Utente utente) { this.utente = utente; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getDisabledAt() { return disabledAt; }
    public void setDisabledAt(Instant disabledAt) { this.disabledAt = disabledAt; }
    public long getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(long tokenVersion) { this.tokenVersion = tokenVersion; }
    public Long getCreatedByAdminId() { return createdByAdminId; }
    public void setCreatedByAdminId(Long createdByAdminId) { this.createdByAdminId = createdByAdminId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
