package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AdminNutrizionistaDto;
import it.nutrizionista.restnutrizionista.dto.AdminStatsDto;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.MetodoRegistrazione;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Logica della dashboard super admin: conteggi e lista nutrizionisti con stato attività. */
@Service
public class AdminService {

    private static final String RUOLO_NUTRIZIONISTA = "NUTRIZIONISTA";

    @Autowired private UtenteRepository utenteRepository;
    @Autowired private ClienteRepository clienteRepository;

    /** Un nutrizionista è "attivo" se ha effettuato un login negli ultimi N giorni. */
    @Value("${admin.attivita.soglia-giorni:30}")
    private int sogliaGiorniAttivita;

    @Transactional(readOnly = true)
    public AdminStatsDto stats() {
        Instant limite = limiteAttivita();
        long totale = utenteRepository.countByRuoloExcludingMetodo(
                RUOLO_NUTRIZIONISTA, MetodoRegistrazione.DEMO);
        long attivi = utenteRepository.countActiveByRuoloExcludingMetodo(
                RUOLO_NUTRIZIONISTA, MetodoRegistrazione.DEMO, limite);
        return new AdminStatsDto(totale, attivi, totale - attivi, sogliaGiorniAttivita);
    }

    @Transactional(readOnly = true)
    public List<AdminNutrizionistaDto> nutrizionisti() {
        Instant limite = limiteAttivita();
        List<Utente> lista = utenteRepository.findByRuoloExcludingMetodo(
                RUOLO_NUTRIZIONISTA, MetodoRegistrazione.DEMO);
        // Conteggio clienti in un'unica query batch invece di una count per riga
        Map<Long, Long> clientiPerNutrizionista = lista.isEmpty() ? Map.of()
                : clienteRepository.countByNutrizionistaIds(lista.stream().map(Utente::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(
                                ClienteRepository.ConteggioClientiPerNutrizionista::getNutrizionistaId,
                                ClienteRepository.ConteggioClientiPerNutrizionista::getTotale));
        return lista.stream()
                .map(u -> toDto(u, limite, clientiPerNutrizionista.getOrDefault(u.getId(), 0L)))
                .toList();
    }

    private Instant limiteAttivita() {
        return Instant.now().minus(Duration.ofDays(sogliaGiorniAttivita));
    }

    private AdminNutrizionistaDto toDto(Utente u, Instant limite, long numeroClienti) {
        AdminNutrizionistaDto dto = new AdminNutrizionistaDto();
        dto.setId(u.getId());
        dto.setNome(u.getNome());
        dto.setCognome(u.getCognome());
        dto.setEmail(u.getEmail());
        dto.setRegistratoIl(u.getCreatedAt());
        dto.setUltimoAccesso(u.getLastLoginAt());
        dto.setAttivo(u.getLastLoginAt() != null && u.getLastLoginAt().isAfter(limite));
        dto.setMetodoRegistrazione(u.getMetodoRegistrazione() != null ? u.getMetodoRegistrazione().name() : null);
        dto.setNumeroClienti(numeroClienti);
        return dto;
    }
}
