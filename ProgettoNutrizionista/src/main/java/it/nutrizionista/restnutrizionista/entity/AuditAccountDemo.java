package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import it.nutrizionista.restnutrizionista.enums.AzioneAuditDemo;
import it.nutrizionista.restnutrizionista.enums.EsitoAuditDemo;
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

/** Audit append-only delle operazioni privilegiate sugli account demo. */
@Entity
@Table(name = "audit_account_demo", indexes = {
        @Index(name = "idx_audit_demo_admin_data", columnList = "admin_id, created_at"),
        @Index(name = "idx_audit_demo_target_data", columnList = "target_user_id, created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class AuditAccountDemo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "demo_credential_id")
    private Long demoCredentialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AzioneAuditDemo azione;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EsitoAuditDemo esito;

    @Column(length = 500)
    private String motivo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditAccountDemo() {}

    public AuditAccountDemo(Long adminId, Long targetUserId, Long demoCredentialId,
            AzioneAuditDemo azione, EsitoAuditDemo esito, String motivo,
            String ipAddress, String userAgent) {
        this.adminId = adminId;
        this.targetUserId = targetUserId;
        this.demoCredentialId = demoCredentialId;
        this.azione = azione;
        this.esito = esito;
        this.motivo = motivo;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public Long getId() { return id; }
    public Long getAdminId() { return adminId; }
    public Long getTargetUserId() { return targetUserId; }
    public Long getDemoCredentialId() { return demoCredentialId; }
    public AzioneAuditDemo getAzione() { return azione; }
    public EsitoAuditDemo getEsito() { return esito; }
    public String getMotivo() { return motivo; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public Instant getCreatedAt() { return createdAt; }
}
