package it.nutrizionista.restnutrizionista.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import it.nutrizionista.restnutrizionista.security.AuthProvider;
import jakarta.annotation.PostConstruct;

/**
 * Guard di startup per il feature flag {@code auth.provider} (nota Code — fail-fast sulla selezione della chain).
 *
 * <p>Le due security config sono selezionate via {@code @ConditionalOnProperty} con {@code matchIfMissing=true}
 * sul legacy: questo copre solo il caso <i>assente</i>. Un valore <b>errato</b> (typo/spazio) non matcherebbe
 * nessuna delle due → Spring applicherebbe la security di <i>default</i> (trap fail-open). Qui l'avvio <b>fallisce</b>
 * se {@code auth.provider} non è nell'enum {@link AuthProvider} → direzione d'errore sicura (fail-closed all'avvio).
 */
@Component
public class AuthProviderStartupValidator {

    @Value("${auth.provider:legacy-jwt}")
    private String authProvider;

    @PostConstruct
    public void validate() {
        if (!AuthProvider.isValid(authProvider)) {
            throw new IllegalStateException(
                "Property 'auth.provider' non valida: '" + authProvider + "'. Valori ammessi: 'legacy-jwt' | 'keycloak'. "
                + "(fail-closed: un valore errato lascerebbe l'app senza la security chain attesa)");
        }
    }
}
