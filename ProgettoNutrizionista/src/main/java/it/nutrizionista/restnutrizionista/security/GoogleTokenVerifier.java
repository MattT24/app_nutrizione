package it.nutrizionista.restnutrizionista.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Verifica server-side degli ID token emessi da Google Identity Services:
 * firma, issuer, audience (deve corrispondere al nostro client-id) e scadenza.
 * Non fidarsi mai di email/nome/cognome passati dal client: vanno sempre letti
 * dal payload restituito da questa classe.
 */
@Component
public class GoogleTokenVerifier {

    @Value("${google.oauth.client-id}")
    private String clientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /**
     * Verifica l'ID token. Lancia IllegalArgumentException se non valido, scaduto,
     * con audience/issuer errati o con email non verificata da Google.
     */
    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new IllegalArgumentException("ID token Google non valido o scaduto");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new IllegalArgumentException("L'email dell'account Google non è verificata");
            }
            return payload;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Verifica ID token Google fallita", e);
        }
    }
}
