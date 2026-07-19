package it.nutrizionista.restnutrizionista.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/** Impedisce l'avvio in produzione con una chiave master demo assente o non BCrypt. */
@Component
@Profile("prod")
public class DemoProductionSecurityValidator {
    @Value("${demo.master-password-hash:}")
    private String masterPasswordHash;

    @PostConstruct
    void valida() {
        if (masterPasswordHash == null
                || !masterPasswordHash.matches("^\\$2[aby]\\$(1[2-9]|2[0-9]|3[01])\\$.{53}$")) {
            throw new IllegalStateException(
                    "DEMO_MASTER_PASSWORD_HASH obbligatorio in produzione: usare BCrypt con cost >= 12");
        }
    }
}
