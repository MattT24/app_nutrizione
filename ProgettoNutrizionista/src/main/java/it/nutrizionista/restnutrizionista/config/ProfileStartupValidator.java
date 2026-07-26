package it.nutrizionista.restnutrizionista.config;

import java.util.Arrays;
import java.util.Set;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Guard di startup sul profilo Spring attivo (nota Code — fail-closed sul taglio dev/prod).
 *
 * <p>Il base {@code application.properties} NON ha più un profilo di default: se l'app parte senza un
 * profilo PRIMARIO esplicito ({@code dev|prod|test}) l'avvio FALLISCE. Impedisce lo scenario più
 * pericoloso — una <b>produzione avviata senza {@code SPRING_PROFILES_ACTIVE=prod}</b> userebbe i
 * default dev ({@code ddl-auto=update} che muta lo schema prod, cookie non-{@code Secure}, CORS dev).
 * Il profilo secondario {@code keycloak} si combina sempre con dev/prod (es. {@code dev,keycloak}).
 *
 * <p>⚠️ Legge {@link Environment#getActiveProfiles()}, NON {@code @Value}: {@code @ActiveProfiles} setta
 * i profili sull'{@code Environment} (non come property), quindi {@code @Value("${spring.profiles.active:}")}
 * sarebbe vuoto in un run IDE con {@code @ActiveProfiles} = falso fallimento.
 */
@Component
public class ProfileStartupValidator {

    private static final Set<String> PROFILI_PRIMARI = Set.of("dev", "prod", "test");

    private final Environment environment;

    public ProfileStartupValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        boolean haPrimario = Arrays.stream(environment.getActiveProfiles()).anyMatch(PROFILI_PRIMARI::contains);
        if (!haPrimario) {
            throw new IllegalStateException(
                "Nessun profilo Spring PRIMARIO attivo (attesi 'dev'|'prod'|'test'; trovati: "
                + Arrays.toString(environment.getActiveProfiles()) + "). Impostare SPRING_PROFILES_ACTIVE "
                + "(prod in produzione, dev in locale). (fail-closed: senza profilo esplicito l'app userebbe "
                + "i default dev — ddl-auto=update, cookie non-Secure — anche in produzione)");
        }
    }
}
