package it.nutrizionista.restnutrizionista.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Unit test puro (nessun mock/Spring) sulle soglie dei livelli gamification: copre i bordi
 * esatti delle soglie, dove un refuso futuro (es. {@code >} invece di {@code >=}) si nota di
 * più a colpo d'occhio che leggendo il codice.
 */
class GamificationLivelloCatalogoTest {

    @Test
    void attualePer_puntiZero_ritornaTirocinante() {
        assertEquals("Tirocinante", GamificationLivelloCatalogo.attualePer(0).nome());
    }

    @Test
    void attualePer_puntoAppenaSottoSoglia_nonSaleDiLivello() {
        assertEquals("Tirocinante", GamificationLivelloCatalogo.attualePer(99).nome());
    }

    @Test
    void attualePer_puntoEsattoSullaSoglia_saleDiLivello() {
        assertEquals("Nutrizionista Junior", GamificationLivelloCatalogo.attualePer(100).nome());
    }

    @Test
    void attualePer_puntiOltreUltimaSoglia_restaSulLivelloMassimo() {
        assertEquals("Luminare della Nutrizione", GamificationLivelloCatalogo.attualePer(999_999).nome());
    }

    @Test
    void successivoPer_sottoSogliaJunior_ritornaJuniorComeProssimo() {
        assertEquals("Nutrizionista Junior", GamificationLivelloCatalogo.successivoPer(99).nome());
    }

    @Test
    void successivoPer_livelloMassimoGiaRaggiunto_ritornaNull() {
        assertNull(GamificationLivelloCatalogo.successivoPer(3000));
    }
}
