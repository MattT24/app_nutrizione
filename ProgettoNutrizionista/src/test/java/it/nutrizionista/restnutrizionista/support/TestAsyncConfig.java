package it.nutrizionista.restnutrizionista.support;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;

/**
 * In profilo test gli executor async sono <b>sincroni</b> ({@code auditExecutor} + il default {@code taskExecutor}):
 * gli eventi async (audit letture/liste, email {@code @Async}) girano inline, così gli assert sono deterministici
 * senza attese. Sostituiscono i {@code ThreadPoolTaskExecutor} di produzione (annotati {@code @Profile("!test")}).
 */
@Configuration
@Profile("test")
public class TestAsyncConfig {

    @Bean("auditExecutor")
    public Executor auditExecutor() {
        return new SyncTaskExecutor();
    }

    /** Follow-up A1: default {@code @Async} sincrono in test (le email {@code @Async} non diventano flaky). */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
