package it.nutrizionista.restnutrizionista;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ProgettoNutrizionistaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProgettoNutrizionistaApplication.class, args);
	}

}
