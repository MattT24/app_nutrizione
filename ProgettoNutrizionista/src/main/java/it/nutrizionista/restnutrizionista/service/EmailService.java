package it.nutrizionista.restnutrizionista.service;

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

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Async
    public void sendPdfEmail(String to, String subject, String text, byte[] pdfBytes, String filename) {
        try {
            log.info("Invio email a {} con allegato '{}'...", to, filename);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);

            ByteArrayResource pdfResource = new ByteArrayResource(pdfBytes);
            helper.addAttachment(filename, pdfResource);

            javaMailSender.send(message);
            log.info("Email inviata con successo a {}", to);

        } catch (MessagingException e) {
            log.error("Errore durante l'invio dell'email a {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Errore nell'invio dell'email", e);
        }
    }
}
