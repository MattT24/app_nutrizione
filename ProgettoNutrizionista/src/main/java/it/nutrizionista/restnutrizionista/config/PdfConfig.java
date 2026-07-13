package it.nutrizionista.restnutrizionista.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Template engine dedicato ai PDF, in modalità XML. OpenHTMLToPDF parsa l'HTML come XML strict:
 * serve la modalità XML di Thymeleaf per preservare i self-closing degli elementi SVG
 * (icone/grafici) — in modalità HTML verrebbero serializzati senza chiusura, rompendo il parser.
 * È separato dal SpringTemplateEngine autoconfigurato (usato altrove) e iniettato via @Qualifier.
 */
@Configuration
public class PdfConfig {

    @Bean
    public TemplateEngine pdfTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.XML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
