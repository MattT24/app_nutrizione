package it.nutrizionista.restnutrizionista.support;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;

/**
 * In profilo test l'{@code auditExecutor} è sincrono: gli eventi di audit async (letture/liste)
 * vengono scritti inline, così gli assert sono deterministici senza attese. Sostituisce il
 * {@code ThreadPoolTaskExecutor} di produzione (annotato {@code @Profile("!test")}).
 */
@Configuration
@Profile("test")
public class TestAsyncConfig {

    @Bean("auditExecutor")
    public Executor auditExecutor() {
        return new SyncTaskExecutor();
    }
}
