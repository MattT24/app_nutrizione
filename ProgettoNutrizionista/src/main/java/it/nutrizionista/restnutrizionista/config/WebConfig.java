package it.nutrizionista.restnutrizionista.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Solo i loghi del nutrizionista sono serviti staticamente (risorse non sensibili).
        // I documenti di fascicolo (dati sanitari) NON sono esposti staticamente: si scaricano
        // esclusivamente via GET /api/fascicolo/{id}/download con ownership check.
        // es: http://localhost:8080/uploads/loghi/foto.jpg
        registry.addResourceHandler("/uploads/loghi/**")
                .addResourceLocations("file:" + uploadDir + "/loghi/");
    }
    
    
}
