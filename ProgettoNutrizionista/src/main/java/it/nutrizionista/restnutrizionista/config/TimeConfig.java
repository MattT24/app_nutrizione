package it.nutrizionista.restnutrizionista.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Clock UTC iniettabile per testare esattamente i confini di scadenza. */
@Configuration
public class TimeConfig {
    @Bean
    public Clock applicationClock() { return Clock.systemUTC(); }
}
