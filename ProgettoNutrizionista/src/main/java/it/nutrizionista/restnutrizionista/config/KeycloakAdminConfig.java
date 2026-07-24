package it.nutrizionista.restnutrizionista.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;

import it.nutrizionista.restnutrizionista.security.KeycloakAdminClient;

/**
 * I10 — erasure CROSS-STORE (art. 17). Beans del {@link KeycloakAdminClient}, <b>gated</b>
 * {@code auth.provider=keycloak} (in legacy questa config NON è caricata → nessun bean → {@code UtenteService}
 * non chiama mai Keycloak → zero regressioni).
 *
 * <p>Costruisce: (1) un {@code OAuth2AuthorizedClientManager} <b>service-based</b>
 * ({@link AuthorizedClientServiceOAuth2AuthorizedClientManager}) con provider {@code client_credentials} — funziona
 * nei <b>thread di background</b> (after-commit/scheduler), dove non c'è request context (il manager request-based
 * fallirebbe); (2) un {@link RestClient} dedicato con <b>timeout bounded</b> (MUST I10: l'hook gira sincrono nel
 * thread di richiesta dopo il commit — senza timeout una KC appesa bloccherebbe la cancellazione).
 */
@Configuration
@ConditionalOnProperty(name = "auth.provider", havingValue = "keycloak")
public class KeycloakAdminConfig {

    @Bean
    public KeycloakAdminClient keycloakAdminClient(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService,
            @Value("${keycloak.admin.base-url:http://localhost:8081}") String baseUrl,
            @Value("${keycloak.admin.realm:statera}") String realm,
            @Value("${keycloak.invite.client-id:statera-bff}") String inviteClientId,
            @Value("${keycloak.invite.redirect-uri:http://localhost:4200/}") String inviteRedirectUri) {

        // Token-response-client (client_credentials) con timeout BOUNDED: chiude il gap del MUST — anche la fetch
        // del token su /token è limitata (non solo il DELETE), così una KC appesa non blocca l'after-commit.
        RestClient tokenRestClient = RestClient.builder()
                .messageConverters(c -> {
                    c.clear();
                    c.add(new FormHttpMessageConverter());
                    c.add(new OAuth2AccessTokenResponseHttpMessageConverter());
                })
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                .requestFactory(boundedFactory())
                .build();
        RestClientClientCredentialsTokenResponseClient tokenResponseClient =
                new RestClientClientCredentialsTokenResponseClient();
        tokenResponseClient.setRestClient(tokenRestClient);

        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials(cc -> cc.accessTokenResponseClient(tokenResponseClient))
                .build();
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(provider);

        RestClient rest = RestClient.builder().baseUrl(baseUrl).requestFactory(boundedFactory()).build();
        return new KeycloakAdminClient(manager, rest, realm, inviteClientId, inviteRedirectUri);
    }

    /** Timeout bounded condiviso (connect 3s / read 5s): l'hook I10 gira sincrono nel thread di richiesta dopo il commit. */
    private static SimpleClientHttpRequestFactory boundedFactory() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(3));
        f.setReadTimeout(Duration.ofSeconds(5));
        return f;
    }
}
