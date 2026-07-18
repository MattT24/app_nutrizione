package it.nutrizionista.restnutrizionista.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/** Impedisce l'avvio prod con credenziali amministrative implicite o deboli. */
@Component
@Profile("prod")
public class SuperAdminProductionSecurityValidator {

    @Value("${superadmin.email:}")
    private String email;

    @Value("${superadmin.password-hash:}")
    private String passwordHash;

    @PostConstruct
    void valida() {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalStateException(
                    "SUPERADMIN_EMAIL obbligatoria e valida in produzione");
        }
        if (passwordHash == null
                || !passwordHash.matches("^\\$2[aby]\\$(1[2-9]|2[0-9]|3[01])\\$.{53}$")) {
            throw new IllegalStateException(
                    "SUPERADMIN_PASSWORD_HASH obbligatorio in produzione: usare BCrypt con cost >= 12");
        }
    }
}
