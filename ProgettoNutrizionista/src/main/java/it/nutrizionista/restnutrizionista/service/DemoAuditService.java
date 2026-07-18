package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.entity.AuditAccountDemo;
import it.nutrizionista.restnutrizionista.enums.AzioneAuditDemo;
import it.nutrizionista.restnutrizionista.enums.EsitoAuditDemo;
import it.nutrizionista.restnutrizionista.repository.AuditAccountDemoRepository;

/** Scrittura audit senza mai ricevere password, master secret o JWT. */
@Service
public class DemoAuditService {
    @Autowired private AuditAccountDemoRepository repository;

    public void recordSameTx(Long adminId, Long targetId, Long credenzialeId,
            AzioneAuditDemo azione, EsitoAuditDemo esito, String motivo, String ip, String userAgent) {
        repository.save(new AuditAccountDemo(adminId, targetId, credenzialeId,
                azione, esito, motivo, ip, limita(userAgent, 512)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordNewTx(Long adminId, Long targetId, Long credenzialeId,
            AzioneAuditDemo azione, EsitoAuditDemo esito, String motivo, String ip, String userAgent) {
        recordSameTx(adminId, targetId, credenzialeId, azione, esito, motivo, ip, userAgent);
    }

    private String limita(String valore, int max) {
        return valore == null || valore.length() <= max ? valore : valore.substring(0, max);
    }
}
