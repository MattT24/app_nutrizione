package it.nutrizionista.restnutrizionista.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Executor dedicato alla scrittura async dell'audit (letture/liste, vedi {@code AuditService.record}).
     * Bounded con {@link ThreadPoolExecutor.CallerRunsPolicy}: se la coda è piena l'evento viene scritto
     * sincronamente dal thread chiamante anziché essere perso.
     *
     * <p>In profilo {@code test} è sostituito da un {@code SyncTaskExecutor} (vedi
     * {@code support/TestAsyncConfig}) così gli eventi async sono deterministici negli assert.
     */
    @Bean("auditExecutor")
    @Profile("!test")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("audit-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
