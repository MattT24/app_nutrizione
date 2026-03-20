package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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

    // Ritorna la LISTA degli orari di tutti i giorni della settimana
    @Transactional(readOnly = true)
    public List<OrariStudioDto> getOrariStudioMe(String email) {
        Utente nutrizionista = utenteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Nutrizionista non trovato con email: " + email));

        List<OrariStudio> orari = orariStudioRepository.findByNutrizionista(nutrizionista);

        return orari.stream()
                .map(DtoMapper::toOrariStudioDto)
                .collect(Collectors.toList());
    }

    // Salva o aggiorna l'orario di UN SINGOLO giorno della settimana
    @Transactional
    public OrariStudioDto upsertOrariStudioMe(String email, OrariStudioFormDto formDto) {
        Utente nutrizionista = utenteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Nutrizionista non trovato con email: " + email));

        validateOrariStudio(formDto);

        // Cerca se esiste già un orario salvato per questo nutrizionista e per questo SPECIFICO giorno
        OrariStudio orari = orariStudioRepository.findByNutrizionistaAndGiornoSettimana(nutrizionista, formDto.getGiornoSettimana())
                .orElse(null);

        if (orari == null) {
            orari = DtoMapper.toOrariStudio(formDto, nutrizionista);
        } else {
            DtoMapper.updateOrariStudioFromFormDto(orari, formDto);
        }

        OrariStudio saved = orariStudioRepository.save(orari);
        return DtoMapper.toOrariStudioDto(saved);
    }

    // (Opzionale) Metodo extra per salvare l'intera settimana in un colpo solo passando un Array dal frontend
    @Transactional
    public List<OrariStudioDto> upsertOrariStudioSettimana(String email, List<OrariStudioFormDto> formDtos) {
        return formDtos.stream()
                .map(formDto -> upsertOrariStudioMe(email, formDto))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteOrariStudio(Long id) {
        OrariStudio esistente = orariStudioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orario studio non trovato"));
        
        // Verifica se appartiene a chi lo sta cancellando potrebbe essere fatto qui o nel controller
        orariStudioRepository.delete(esistente);
    }

    private void validateOrariStudio(OrariStudioFormDto formDto) {
        if (formDto == null) {
            throw new RuntimeException("Dati orari studio mancanti");
        }

        if (formDto.getGiornoSettimana() == null) {
            throw new RuntimeException("Giorno della settimana obbligatorio");
        }

        // Se il giorno è impostato come "NON lavorativo", skippiamo i controlli sugli orari
        if (!formDto.isGiornoLavorativo()) {
            return;
        }

        if (formDto.getOraApertura() == null) {
            throw new RuntimeException("Ora apertura obbligatoria nei giorni lavorativi");
        }
        if (formDto.getOraChiusura() == null) {
            throw new RuntimeException("Ora chiusura obbligatoria nei giorni lavorativi");
        }
        if (!formDto.getOraApertura().isBefore(formDto.getOraChiusura())) {
            throw new RuntimeException("L'ora di apertura deve essere antecedente all'ora di chiusura");
        }

        // Validazione Pausa (opzionale, ma se si valorizza un campo, servono entrambi)
        if (formDto.getInizioPausaPranzo() != null || formDto.getFinePausaPranzo() != null) {
            
            if (formDto.getInizioPausaPranzo() == null || formDto.getFinePausaPranzo() == null) {
                throw new RuntimeException("Pausa non valida: specifica sia inizio che fine");
            }
            if (!formDto.getInizioPausaPranzo().isBefore(formDto.getFinePausaPranzo())) {
                throw new RuntimeException("Pausa non valida: l'inizio deve essere antecedente alla fine");
            }
            // Controllo che la pausa sia dentro i margini di lavoro
            if (formDto.getInizioPausaPranzo().isBefore(formDto.getOraApertura())
                    || formDto.getFinePausaPranzo().isAfter(formDto.getOraChiusura())) {
                throw new RuntimeException("Pausa non valida: deve stare dentro l'orario di apertura/chiusura dello studio");
            }
        }
    }
}