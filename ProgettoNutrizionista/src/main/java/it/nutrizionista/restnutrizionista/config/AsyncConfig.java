package it.nutrizionista.restnutrizionista.config;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskDecorator;
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
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * A1 Follow-up — executor {@code @Async} di <b>DEFAULT</b>. Il nome {@code "taskExecutor"} è il fallback
     * documentato di Spring per gli {@code @Async} NON qualificati quando esistono più bean {@code Executor}
     * ({@code getBean(TaskExecutor.class)} → NoUnique → {@code getBean("taskExecutor")}). Copre le email
     * {@code @Async} di {@code EmailService} (prima su {@code SimpleAsyncTaskExecutor} unbounded e senza MDC):
     * ora <b>pool bounded</b> ({@link ThreadPoolExecutor.CallerRunsPolicy}) + <b>correlation-id propagato</b>
     * (A1). {@code auditExecutor} resta separato ({@code @Async("auditExecutor")}). In profilo {@code test} è
     * un {@code SyncTaskExecutor} (support/TestAsyncConfig) per determinismo.
     */
    @Bean("taskExecutor")
    @Profile("!test")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * Copia l'MDC (incl. {@code correlationId} — A1) dal thread chiamante al thread async, e ripristina il
     * context precedente dopo il task (thread poolato → niente leak cross-task). Condiviso da entrambi gli executor.
     */
    private static TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> captured = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previous = MDC.getCopyOfContextMap();
                if (captured != null) {
                    MDC.setContextMap(captured);
                } else {
                    MDC.clear();
                }
                try {
                    runnable.run();
                } finally {
                    if (previous != null) {
                        MDC.setContextMap(previous);
                    } else {
                        MDC.clear();
                    }
                }
            };
        };
    }
}
