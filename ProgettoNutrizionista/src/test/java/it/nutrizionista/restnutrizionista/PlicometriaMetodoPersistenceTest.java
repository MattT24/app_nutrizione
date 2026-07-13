package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Metodo;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.PlicometriaRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

@SpringBootTest
@ActiveProfiles("test")
class PlicometriaMetodoPersistenceTest extends SafeTestDatabaseBase {
    @Autowired
    private UtenteRepository repoUtente;
    @Autowired
    private ClienteRepository repoCliente;
    @Autowired
    private PlicometriaRepository repoPlicometria;

    private Cliente cliente;

    @BeforeEach
    void setup() {
        repoPlicometria.deleteAll();
        repoCliente.deleteAll();
        repoUtente.deleteAll();

        Utente u = new Utente();
        u.setNome("Test");
        u.setCognome("Nutrizionista");
        u.setCodiceFiscale("TTSTNT00A00A000A");
        u.setEmail("nutri@test.it");
        u.setPassword("x");
        u.setTelefono("000");
        u.setIndirizzo("x");
        Utente nutrizionista = repoUtente.save(u);

        Cliente c = new Cliente();
        c.setSesso(Sesso.Maschio);
        c.setNome("Mario");
        c.setCognome("Rossi");
        c.setCodiceFiscale("MRARSS00A00A000A");
        c.setEmail("cliente@test.it");
        c.setDataNascita(LocalDate.of(1990, 1, 1));
        c.setPeso(70.0);
        c.setAltezza(175);
        c.setLivelloDiAttivita(it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita.SEDENTARIO);
        c.setIntolleranze("N");
        c.setFunzioniIntestinali("N");
        c.setProblematicheSalutari("N");
        c.setQuantitaEQualitaDelSonno("N");
        c.setAssunzioneFarmaci("N");
        c.setBeveAlcol(false);
        c.setFuma(false);
        c.setNutrizionista(nutrizionista);
        cliente = repoCliente.save(c);
    }

    @Test
    void save_withLongestMetodoName_persists() {
        Plicometria p = new Plicometria();
        p.setCliente(cliente);
        p.setDataMisurazione(LocalDate.now());
        p.setMetodo(Metodo.DURNIN_WOMERSLEY);

        Plicometria saved = repoPlicometria.save(p);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMetodo()).isEqualTo(Metodo.DURNIN_WOMERSLEY);
    }
}
