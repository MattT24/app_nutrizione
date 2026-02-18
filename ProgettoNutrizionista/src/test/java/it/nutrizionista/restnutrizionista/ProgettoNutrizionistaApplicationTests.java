package it.nutrizionista.restnutrizionista;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

@SpringBootTest
@ActiveProfiles("test")
class ProgettoNutrizionistaApplicationTests extends SafeTestDatabaseBase {

	@Test
	void contextLoads() {
	}

}
