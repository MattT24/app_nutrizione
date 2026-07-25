package it.nutrizionista.restnutrizionista.service;

import it.nutrizionista.restnutrizionista.exception.EmailDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    /** Maschera il local-part dell'email nei log (minimizzazione GDPR: niente indirizzi in chiaro). */
    private static String mask(String email) {
        if (email == null || email.isBlank()) return "<n/d>";
        int at = email.indexOf('@');
        if (at <= 1) return "***" + (at >= 0 ? email.substring(at) : "");
        return email.charAt(0) + "***" + email.substring(at);
    }

    /**
     * Invio <b>sincrono</b> di un PDF sanitario. A differenza delle altre email (fire-and-forget
     * {@code @Async}), qui il chiamante attende l'esito e le eccezioni risalgono: è un evento critico
     * di audit (A7, scelta "sincrona bloccante") in cui l'audit del SHARE/DOWNLOAD deve riflettere la
     * reale divulgazione (il chiamante registra {@code SUCCESS} prima dell'invio e {@code FAILURE} se fallisce).
     */
    public void sendPdfEmail(String to, String subject, String text, byte[] pdfBytes, String filename) {
        try {
            log.info("Invio email a {} con allegato '{}'...", mask(to), filename);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text
                    + "\n\n—\nStatera è uno strumento di supporto al professionista e non sostituisce il parere medico; il piano resta responsabilità del nutrizionista.",
                    false);

            ByteArrayResource pdfResource = new ByteArrayResource(pdfBytes);
            helper.addAttachment(filename, pdfResource);

            javaMailSender.send(message);
            log.info("Email inviata con successo a {}", mask(to));

        } catch (MessagingException e) {
            log.error("Errore durante l'invio dell'email a {}: {}", mask(to), e.getMessage(), e);
            throw new EmailDeliveryException("Errore nell'invio dell'email", e);
        }
    }

    @Async
    public void sendAppointmentConfirmation(
            String to,
            String clienteNome,
            LocalDate data,
            LocalTime ora,
            LocalTime endOra,
            String modalita,
            String luogo,
            String descrizione,
            String nutrizionistaFullName) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Conferma appuntamento – Statera Nutrition");

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

            String modalitaLabel = switch (modalita) {
                case "IN_STUDIO" -> "In studio";
                case "ONLINE"    -> "Online";
                case "DOMICILIO" -> "A domicilio";
                default          -> modalita;
            };

            StringBuilder body = new StringBuilder();
            body.append("Gentile ").append(clienteNome).append(",\n\n");
            body.append("Il tuo appuntamento è stato confermato con i seguenti dettagli:\n\n");
            body.append("  Data:      ").append(data.format(dateFmt)).append("\n");
            if (ora != null) {
                body.append("  Orario:    ").append(ora.format(timeFmt));
                if (endOra != null) body.append(" – ").append(endOra.format(timeFmt));
                body.append("\n");
            }
            body.append("  Modalità:  ").append(modalitaLabel).append("\n");
            if (luogo != null && !luogo.isBlank()) {
                body.append("  Luogo:     ").append(luogo).append("\n");
            }
            if (descrizione != null && !descrizione.isBlank()) {
                body.append("  Note:      ").append(descrizione).append("\n");
            }
            body.append("\nPer qualsiasi necessità non esitare a contattarci.\n\n");
            body.append("A presto,\n").append(nutrizionistaFullName);

            helper.setText(body.toString(), false);
            javaMailSender.send(message);
            log.info("Email di conferma appuntamento inviata a {}", mask(to));
        } catch (MessagingException e) {
            log.error("Errore durante l'invio della conferma appuntamento a {}: {}", mask(to), e.getMessage(), e);
        }
    }

    @Async
    public void sendAppointmentReminder(
            String to,
            String clienteNome,
            LocalDate data,
            LocalTime ora,
            LocalTime endOra,
            String modalita,
            String luogo,
            String descrizione,
            String nutrizionistaFullName) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Promemoria appuntamento di domani – Statera Nutrition");

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

            String modalitaLabel = switch (modalita) {
                case "IN_STUDIO" -> "In studio";
                case "ONLINE"    -> "Online";
                case "DOMICILIO" -> "A domicilio";
                default          -> modalita;
            };

            StringBuilder body = new StringBuilder();
            body.append("Gentile ").append(clienteNome).append(",\n\n");
            body.append("Ti ricordiamo che domani hai un appuntamento:\n\n");
            body.append("  Data:      ").append(data.format(dateFmt)).append("\n");
            if (ora != null) {
                body.append("  Orario:    ").append(ora.format(timeFmt));
                if (endOra != null) body.append(" – ").append(endOra.format(timeFmt));
                body.append("\n");
            }
            body.append("  Modalità:  ").append(modalitaLabel).append("\n");
            if (luogo != null && !luogo.isBlank()) {
                body.append("  Luogo:     ").append(luogo).append("\n");
            }
            if (descrizione != null && !descrizione.isBlank()) {
                body.append("  Note:      ").append(descrizione).append("\n");
            }
            body.append("\nPer qualsiasi necessità non esitare a contattarci.\n\n");
            body.append("A presto,\n").append(nutrizionistaFullName);

            helper.setText(body.toString(), false);
            javaMailSender.send(message);
            log.info("Email promemoria appuntamento inviata a {}", mask(to));
        } catch (MessagingException e) {
            log.error("Errore durante l'invio del promemoria appuntamento a {}: {}", mask(to), e.getMessage(), e);
        }
    }

    /** Notifica il nutrizionista di un nuovo traguardo gamification sbloccato (badge o salita di livello). */
    @Async
    public void sendGamificationMilestone(String to, String nutrizionistaNome, String titoloTraguardo, String descrizioneTraguardo) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Nuovo traguardo raggiunto: " + titoloTraguardo + " – Statera Nutrition");

            StringBuilder body = new StringBuilder();
            body.append("Ciao ").append(nutrizionistaNome).append(",\n\n");
            body.append("Hai raggiunto un nuovo traguardo: ").append(titoloTraguardo).append("\n");
            body.append(descrizioneTraguardo).append("\n\n");
            body.append("Continua così!\n\n");
            body.append("Il team Statera Nutrition");

            helper.setText(body.toString(), false);
            javaMailSender.send(message);
            log.info("Email traguardo gamification inviata a {}", mask(to));
        } catch (MessagingException e) {
            log.error("Errore durante l'invio dell'email traguardo gamification a {}: {}", mask(to), e.getMessage(), e);
        }
    }
}
