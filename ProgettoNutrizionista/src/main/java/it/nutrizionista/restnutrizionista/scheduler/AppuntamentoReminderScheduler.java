package it.nutrizionista.restnutrizionista.scheduler;

import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
import it.nutrizionista.restnutrizionista.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class AppuntamentoReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppuntamentoReminderScheduler.class);

    private final AppuntamentoRepository appuntamentoRepository;
    private final EmailService emailService;

    public AppuntamentoReminderScheduler(AppuntamentoRepository appuntamentoRepository, EmailService emailService) {
        this.appuntamentoRepository = appuntamentoRepository;
        this.emailService = emailService;
    }

    // Esegue ogni giorno alle 08:00
    @Scheduled(cron = "0 0 8 * * *")
    public void inviaPromemoria() {
        LocalDate domani = LocalDate.now().plusDays(1);
        List<Appuntamento> appuntamenti = appuntamentoRepository
                .findByDataAndStatoNotWithEmail(domani, Appuntamento.StatoAppuntamento.ANNULLATO);

        log.info("Promemoria appuntamenti: trovati {} appuntamenti per il {}", appuntamenti.size(), domani);

        int inviati = 0;
        int errori = 0;

        for (Appuntamento app : appuntamenti) {
            try {
                String nomeCliente = app.getClienteNome() != null ? app.getClienteNome() : "Cliente";
                String nutrizionistaName = app.getNutrizionista().getNome() + " " + app.getNutrizionista().getCognome();
                String modalita = app.getModalita() != null ? app.getModalita().name() : "";

                emailService.sendAppointmentReminder(
                        app.getEmailCliente(),
                        nomeCliente,
                        app.getData(),
                        app.getOra(),
                        app.getEndOra(),
                        modalita,
                        app.getLuogo(),
                        app.getDescrizioneAppuntamento(),
                        nutrizionistaName
                );
                inviati++;
            } catch (Exception e) {
                errori++;
                log.error("Errore promemoria per appuntamento id={}: {}", app.getId(), e.getMessage());
            }
        }

        log.info("Promemoria completato: {} inviati, {} errori", inviati, errori);
    }
}
