package it.nutrizionista.restnutrizionista.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.repository.MisurazioneAntropometricaRepository;
import it.nutrizionista.restnutrizionista.repository.PlicometriaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;

@Service
public class PdfService {

    private final SpringTemplateEngine templateEngine;
    private final MisurazioneAntropometricaRepository misurazioneRepository;
    private final PlicometriaRepository plicometriaRepository;
    private final SchedaRepository schedaRepository;

    public PdfService(SpringTemplateEngine templateEngine,
                      MisurazioneAntropometricaRepository misurazioneRepository,
                      PlicometriaRepository plicometriaRepository,
                      SchedaRepository schedaRepository) {
        this.templateEngine = templateEngine;
        this.misurazioneRepository = misurazioneRepository;
        this.plicometriaRepository = plicometriaRepository;
        this.schedaRepository = schedaRepository;
    }

    @Transactional(readOnly = true)
    public byte[] generaPdfMisurazione(Long id) {
        MisurazioneAntropometrica misurazione = misurazioneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Misurazione non trovata"));
        Context context = new Context();
        context.setVariable("misurazione", misurazione);
        String html = templateEngine.process("pdf/misurazione", context);
        return renderPdf(html);
    }

    @Transactional(readOnly = true)
    public byte[] generaPdfPlicometria(Long id) {
        Plicometria plicometria = plicometriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plicometria non trovata"));
        Context context = new Context();
        context.setVariable("plicometria", plicometria);
        String html = templateEngine.process("pdf/plicometria", context);
        return renderPdf(html);
    }

    @Transactional(readOnly = true)
    public byte[] generaPdfScheda(Long id) {
        Scheda scheda = schedaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheda non trovata"));
        
        // Forza caricamento lazy collection
        scheda.getPasti().size();
        scheda.getPasti().forEach(pasto -> {
            if (pasto.getAlimentiPasto() != null) {
                pasto.getAlimentiPasto().size();
                pasto.getAlimentiPasto().forEach(alimentoPasto -> {
                    if(alimentoPasto.getAlimento() != null) {
                        alimentoPasto.getAlimento().getNome(); // force fetching
                    }
                    if(alimentoPasto.getNomeOverride() != null) {
                        alimentoPasto.getNomeOverride().getNomeCustom(); // force fetching
                    }
                });
            }
        });

        Context context = new Context();
        context.setVariable("scheda", scheda);

        // Carica dati nutrizionista e logo
        if (scheda.getCliente() != null && scheda.getCliente().getNutrizionista() != null) {
            var nutrizionista = scheda.getCliente().getNutrizionista();
            nutrizionista.getNome(); // force fetching
            context.setVariable("nutrizionista", nutrizionista);

            // Converti logo in base64 per il PDF
            if (nutrizionista.getFilePathLogo() != null && !nutrizionista.getFilePathLogo().isEmpty()) {
                try {
                    String logoPathStr = nutrizionista.getFilePathLogo();
                    java.nio.file.Path logoPath = java.nio.file.Paths.get(logoPathStr);
                    
                    // Se il path è relativo, assicuriamoci che sia corretto
                    if (!logoPath.isAbsolute()) {
                        // Proviamo a cercarlo nella cartella di lavoro corrente o relativa a 'uploads'
                        if (!java.nio.file.Files.exists(logoPath)) {
                            logoPath = java.nio.file.Paths.get(System.getProperty("user.dir"), logoPathStr);
                        }
                    }

                    if (java.nio.file.Files.exists(logoPath)) {
                        byte[] logoBytes = java.nio.file.Files.readAllBytes(logoPath);
                        String base64 = java.util.Base64.getEncoder().encodeToString(logoBytes);
                        String mimeType = java.nio.file.Files.probeContentType(logoPath);
                        if (mimeType == null) mimeType = "image/png";
                        context.setVariable("logoBase64", "data:" + mimeType + ";base64," + base64);
                    } else {
                        System.err.println("PDF Service: Logo non trovato al percorso: " + logoPath.toAbsolutePath());
                    }
                } catch (Exception e) {
                    System.err.println("PDF Service: Errore durante il caricamento del logo: " + e.getMessage());
                }
            }
        }

        String html = templateEngine.process("pdf/scheda", context);
        return renderPdf(html);
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Errore durante la generazione del PDF", e);
        }
    }
}
