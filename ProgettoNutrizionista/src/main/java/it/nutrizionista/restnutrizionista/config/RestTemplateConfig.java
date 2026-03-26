package it.nutrizionista.restnutrizionista.config;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configura il bean RestTemplate per chiamate esterne (Open Food Facts).
 *
 * - Timeout 3s connect / 5s read (DoS prevention)
 * - User-Agent obbligatorio (OFF blocca richieste senza User-Agent)
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(15000);    // 15s — query generiche su OFF richiedono più tempo

        RestTemplate rt = new RestTemplate(factory);

        // OFF richiede un User-Agent valido, altrimenti restituisce 403/503
        rt.setInterceptors(List.of(new UserAgentInterceptor()));

        return rt;
    }

    /** Interceptor che aggiunge User-Agent a tutte le richieste in uscita. */
    private static class UserAgentInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                ClientHttpRequestExecution execution) throws IOException {
            request.getHeaders().set("User-Agent",
                    "Statera/1.0 (d.vecchi.its2@gmail.com)");
            return execution.execute(request, body);
        }
    }
}
