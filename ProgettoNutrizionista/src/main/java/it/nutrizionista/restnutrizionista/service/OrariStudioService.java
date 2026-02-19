package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.OrariStudioDto;
import it.nutrizionista.restnutrizionista.dto.OrariStudioFormDto;
import it.nutrizionista.restnutrizionista.entity.OrariStudio;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.OrariStudioRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;

@Service
public class OrariStudioService {

    @Autowired
    private OrariStudioRepository orariStudioRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Transactional(readOnly = true)
    public OrariStudioDto getOrariStudioMe(String email) {
        Utente nutrizionista = utenteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Nutrizionista non trovato con email: " + email));

        OrariStudio orari = orariStudioRepository.findByNutrizionista(nutrizionista)
                .orElseThrow(() -> new RuntimeException("Orari studio non impostati"));

        return DtoMapper.toOrariStudioDto(orari);
    }

    @Transactional
    public OrariStudioDto upsertOrariStudioMe(String email, OrariStudioFormDto formDto) {
        Utente nutrizionista = utenteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Nutrizionista non trovato con email: " + email));

        validateOrariStudio(formDto);

        OrariStudio orari = orariStudioRepository.findByNutrizionista(nutrizionista).orElse(null);

        if (orari == null) {
            orari = DtoMapper.toOrariStudio(formDto, nutrizionista);
        } else {
            DtoMapper.updateOrariStudioFromFormDto(orari, formDto);
        }

        OrariStudio saved = orariStudioRepository.save(orari);
        return DtoMapper.toOrariStudioDto(saved);
    }

    private void validateOrariStudio(OrariStudioFormDto formDto) {
        if (formDto == null) {
            throw new RuntimeException("Dati orari studio mancanti");
        }

        if (formDto.getOraApertura() == null) {
            throw new RuntimeException("Ora apertura obbligatoria");
        }
        if (formDto.getOraChiusura() == null) {
            throw new RuntimeException("Ora chiusura obbligatoria");
        }
        if (!formDto.getOraApertura().isBefore(formDto.getOraChiusura())) {
            throw new RuntimeException("Ora apertura deve essere prima dell'ora chiusura");
        }

        // Pausa (opzionale): se uno è valorizzato, devono esserlo entrambi
        if (formDto.getPausaInizio() != null || formDto.getPausaFine() != null) {
            if (formDto.getPausaInizio() == null || formDto.getPausaFine() == null) {
                throw new RuntimeException("Pausa non valida: specifica sia inizio che fine");
            }
            if (!formDto.getPausaInizio().isBefore(formDto.getPausaFine())) {
                throw new RuntimeException("Pausa non valida: inizio deve essere prima della fine");
            }
            if (formDto.getPausaInizio().isBefore(formDto.getOraApertura())
                    || formDto.getPausaFine().isAfter(formDto.getOraChiusura())) {
                throw new RuntimeException("Pausa non valida: deve stare dentro l'orario di apertura/chiusura");
            }
        }
    }
}
