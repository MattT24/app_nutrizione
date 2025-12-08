//package it.nutrizionista.restnutrizionista.service;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import it.nutrizionista.restnutrizionista.dto.AppuntamentoDto;
//import it.nutrizionista.restnutrizionista.dto.AppuntamentoFormDto;
//import it.nutrizionista.restnutrizionista.entity.Appuntamento;
//import it.nutrizionista.restnutrizionista.entity.Cliente;
//import it.nutrizionista.restnutrizionista.entity.Utente;
//import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
//import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
//import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
//import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
//
//@Service
//public class AppuntamentoService {
//
//    @Autowired
//    private AppuntamentoRepository appuntamentoRepository;
//
//    @Autowired
//    private UtenteRepository utenteRepository;
//
//    @Autowired
//    private ClienteRepository clienteRepository;
//
//    /**
//     * Crea un nuovo appuntamento (usa l'utente autenticato come nutrizionista)
//     * Supporta sia clienti registrati che non registrati
//     */
//    @Transactional
//    public AppuntamentoDto createAppuntamento(String email, AppuntamentoFormDto formDto) {
//        
//        Utente nutrizionista = utenteRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("Nutrizionista non trovato con email: " + email));
//
//        Cliente cliente = null;
//        
//        // Se c'è clienteId, cerca il cliente registrato
//        if (formDto.getClienteId() != null) {
//            cliente = clienteRepository.findById(formDto.getClienteId())
//                    .orElseThrow(() -> new RuntimeException("Cliente non trovato con id: " + formDto.getClienteId()));
//        } else {
//            // Validazione per cliente non registrato
//            if (formDto.getClienteNome() == null || formDto.getClienteNome().trim().isEmpty()) {
//                throw new RuntimeException("Il nome del cliente è obbligatorio");
//            }
//            if (formDto.getClienteCognome() == null || formDto.getClienteCognome().trim().isEmpty()) {
//                throw new RuntimeException("Il cognome del cliente è obbligatorio");
//            }
//            if (formDto.getEmailCliente() == null || formDto.getEmailCliente().trim().isEmpty()) {
//                throw new RuntimeException("L'email del cliente è obbligatoria");
//            }
//        }
//
//        // Validazioni
//        validateAppuntamento(formDto, nutrizionista, cliente, null);
//
//        // Crea l'appuntamento
//        Appuntamento appuntamento = DtoMapper.toAppuntamento(formDto, nutrizionista, cliente);
//
//        // Salva
//        Appuntamento saved = appuntamentoRepository.save(appuntamento);
//
//        return DtoMapper.toAppuntamentoDto(saved);
//    }
//
//    /**
//     * Aggiorna un appuntamento esistente
//     */
//    @Transactional
//    public AppuntamentoDto updateAppuntamento(Long appuntamentoId, AppuntamentoFormDto formDto) {
//        
//        Appuntamento appuntamento = appuntamentoRepository.findById(appuntamentoId)
//                .orElseThrow(() -> new RuntimeException("Appuntamento non trovato con id: " + appuntamentoId));
//
//        Cliente cliente = appuntamento.getCliente();
//        
//        // Se viene fornito un nuovo clienteId
//        if (formDto.getClienteId() != null) {
//            if (appuntamento.getCliente() == null || 
//                !formDto.getClienteId().equals(appuntamento.getCliente().getId())) {
//                cliente = clienteRepository.findById(formDto.getClienteId())
//                        .orElseThrow(() -> new RuntimeException("Cliente non trovato con id: " + formDto.getClienteId()));
//                appuntamento.setCliente(cliente);
//                // Reset campi temp se ora è un cliente registrato
//                appuntamento.setClienteNomeTemp(null);
//                appuntamento.setClienteCognomeTemp(null);
//               
//            }
//        } else {
//            // Cliente non registrato - validazioni
//            if (formDto.getClienteNome() == null || formDto.getClienteNome().trim().isEmpty()) {
//                throw new RuntimeException("Il nome del cliente è obbligatorio");
//            }
//            if (formDto.getClienteCognome() == null || formDto.getClienteCognome().trim().isEmpty()) {
//                throw new RuntimeException("Il cognome del cliente è obbligatorio");
//            }
//            // Reset cliente registrato se passa a non registrato
//            appuntamento.setCliente(null);
//            cliente = null;
//        }
//
//        // Validazioni
//        validateAppuntamento(formDto, appuntamento.getNutrizionista(), cliente, appuntamentoId);
//
//        // Aggiorna i campi
//        DtoMapper.updateAppuntamentoFromFormDto(appuntamento, formDto);
//
//        // Salva
//        Appuntamento updated = appuntamentoRepository.save(appuntamento);
//
//        return DtoMapper.toAppuntamentoDto(updated);
//    }
//
//    /**
//     * Recupera un appuntamento per ID
//     */
//    @Transactional(readOnly = true)
//    public AppuntamentoDto getAppuntamentoById(Long id) {
//        Appuntamento appuntamento = appuntamentoRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Appuntamento non trovato con id: " + id));
//        return DtoMapper.toAppuntamentoDto(appuntamento);
//    }
//
//    /**
//     * Recupera tutti gli appuntamenti
//     */
//    @Transactional(readOnly = true)
//    public List<AppuntamentoDto> getAllAppuntamenti() {
//        return appuntamentoRepository.findAll().stream()
//                .map(DtoMapper::toAppuntamentoDto)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Recupera gli appuntamenti di un cliente
//     */
//    @Transactional(readOnly = true)
//    public List<AppuntamentoDto> getAppuntamentiByCliente(Long clienteId) {
//        Cliente cliente = clienteRepository.findById(clienteId)
//                .orElseThrow(() -> new RuntimeException("Cliente non trovato con id: " + clienteId));
//        
//        return appuntamentoRepository.findByCliente(cliente).stream()
//                .map(DtoMapper::toAppuntamentoDto)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Recupera gli appuntamenti di un nutrizionista (usa l'utente autenticato)
//     */
//    @Transactional(readOnly = true)
//    public List<AppuntamentoDto> getAppuntamentiByNutrizionista(String email) {
//        Utente nutrizionista = utenteRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("Nutrizionista non trovato con email: " + email));
//        
//        return appuntamentoRepository.findByNutrizionista(nutrizionista).stream()
//                .map(DtoMapper::toAppuntamentoDto)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Recupera gli appuntamenti per data
//     */
//    @Transactional(readOnly = true)
//    public List<AppuntamentoDto> getAppuntamentiByData(LocalDate data) {
//        return appuntamentoRepository.findByData(data).stream()
//                .map(DtoMapper::toAppuntamentoDto)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Recupera gli appuntamenti per stato
//     */
//    @Transactional(readOnly = true)
//    public List<AppuntamentoDto> getAppuntamentiByStato(Appuntamento.StatoAppuntamento stato) {
//        return appuntamentoRepository.findByStato(stato).stream()
//                .map(DtoMapper::toAppuntamentoDto)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Recupera gli appuntamenti di un nutrizionista in un range di date
//     */
//    @Transactional(readOnly = true)
//    public List<AppuntamentoDto> getAppuntamentiByNutricionistaAndDateRange(
//            String email, LocalDate dataInizio, LocalDate dataFine) {
//        Utente nutrizionista = utenteRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("Nutrizionista non trovato con email: " + email));
//        
//        return appuntamentoRepository.findByNutrizionistaAndDataBetween(nutrizionista, dataInizio, dataFine)
//                .stream()
//                .map(DtoMapper::toAppuntamentoDto)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Cambia lo stato di un appuntamento
//     */
//    @Transactional
//    public AppuntamentoDto cambiaStatoAppuntamento(Long appuntamentoId, Appuntamento.StatoAppuntamento nuovoStato) {
//        Appuntamento appuntamento = appuntamentoRepository.findById(appuntamentoId)
//                .orElseThrow(() -> new RuntimeException("Appuntamento non trovato con id: " + appuntamentoId));
//        
//        validateCambioStato(appuntamento, nuovoStato);
//        
//        appuntamento.setStato(nuovoStato);
//        Appuntamento updated = appuntamentoRepository.save(appuntamento);
//        
//        return DtoMapper.toAppuntamentoDto(updated);
//    }
//
//    /**
//     * Elimina un appuntamento
//     */
//    @Transactional
//    public void deleteAppuntamento(Long id) {
//        Appuntamento appuntamento = appuntamentoRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Appuntamento non trovato con id: " + id));
//        
//        if (appuntamento.getStato() == Appuntamento.StatoAppuntamento.CONFERMATO) {
//            throw new RuntimeException("Non è possibile eliminare un appuntamento confermato. Annullalo prima.");
//        }
//        
//        appuntamentoRepository.deleteById(id);
//    }
//
//    /**
//     * Validazioni per la creazione/modifica di un appuntamento
//     */
//    private void validateAppuntamento(AppuntamentoFormDto formDto, Utente nutrizionista, 
//                                      Cliente cliente, Long appuntamentoIdToExclude) {
//        
//        // 1. Validazione data: non può essere nel passato
//        LocalDate oggi = LocalDate.now();
//        if (formDto.getData() != null && formDto.getData().isBefore(oggi)) {
//            throw new RuntimeException("La data dell'appuntamento non può essere nel passato");
//        }
//
//        // 2. Validazione data e ora
//        if (formDto.getData() != null && formDto.getOra() != null) {
//            if (formDto.getData().isEqual(oggi)) {
//                LocalTime adesso = LocalTime.now();
//                if (formDto.getOra().isBefore(adesso)) {
//                    throw new RuntimeException("L'orario dell'appuntamento non può essere nel passato");
//                }
//            }
//        }
//
//        // 3. Validazione orario di lavoro
//        if (formDto.getOra() != null) {
//            LocalTime orarioApertura = LocalTime.of(8, 0);
//            LocalTime orarioChiusura = LocalTime.of(20, 0);
//            if (formDto.getOra().isBefore(orarioApertura) || formDto.getOra().isAfter(orarioChiusura)) {
//                throw new RuntimeException("L'appuntamento deve essere fissato tra le 8:00 e le 20:00");
//            }
//        }
//
//        // 4. Validazione descrizione
//        if (formDto.getDescrizioneAppuntamento() == null || 
//            formDto.getDescrizioneAppuntamento().trim().isEmpty()) {
//            throw new RuntimeException("La descrizione dell'appuntamento è obbligatoria");
//        }
//
//        // 5. Validazione luogo per appuntamenti in presenza
//        if (formDto.getModalita() == Appuntamento.Modalita.IN_PRESENZA) {
//            if (formDto.getLuogo() == null || formDto.getLuogo().trim().isEmpty()) {
//                throw new RuntimeException("Il luogo è obbligatorio per gli appuntamenti in presenza");
//            }
//        }
//
//     // 6. Validazione sovrapposizione per il nutrizionista (stessa data + stessa ora)
//        if (formDto.getData() != null && formDto.getOra() != null) {
//
//            boolean appuntamentoEsistente = appuntamentoRepository
//                    .existsByNutrizionistaAndDataAndOra(
//                            nutrizionista,
//                            formDto.getData(),
//                            formDto.getOra()
//                    );
//
//            if (appuntamentoEsistente) {
//                throw new RuntimeException("Il nutrizionista ha già un appuntamento in questa data e ora");
//            }
//        }
//
//
//        // 7. Validazione sovrapposizione per il cliente (solo se registrato)
//        if (cliente != null && formDto.getData() != null && formDto.getOra() != null) {
//            List<Appuntamento> appuntamentiCliente = appuntamentoRepository
//                    .findByClienteAndData(cliente, formDto.getData());
//            
//            for (Appuntamento esistente : appuntamentiCliente) {
//                if (appuntamentoIdToExclude != null && esistente.getId().equals(appuntamentoIdToExclude)) {
//                    continue;
//                }
//                
//                LocalTime oraInizio = formDto.getOra();
//                LocalTime oraFine = formDto.getOra().plusHours(1);
//                LocalTime oraInizioEsistente = esistente.getOra();
//                LocalTime oraFineEsistente = esistente.getOra().plusHours(1);
//                
//                if (!(oraFine.isBefore(oraInizioEsistente) || oraInizio.isAfter(oraFineEsistente))) {
//                    throw new RuntimeException("Il cliente ha già un appuntamento in questo orario");
//                }
//            }
//        }
//
//        // 8. Validazione email cliente
//        if (formDto.getEmailCliente() != null && !formDto.getEmailCliente().trim().isEmpty()) {
//            if (!formDto.getEmailCliente().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
//                throw new RuntimeException("L'email del cliente non è valida");
//            }
//        }
//    }
//
//    /**
//     * Validazione per il cambio di stato
//     */
//    private void validateCambioStato(Appuntamento appuntamento, Appuntamento.StatoAppuntamento nuovoStato) {
//        Appuntamento.StatoAppuntamento statoCorrente = appuntamento.getStato();
//        
//        if (statoCorrente == Appuntamento.StatoAppuntamento.ANNULLATO) {
//            throw new RuntimeException("Non è possibile modificare lo stato di un appuntamento annullato");
//        }
//        
//        if (nuovoStato == Appuntamento.StatoAppuntamento.CONFERMATO) {
//            LocalDateTime dataOraAppuntamento = LocalDateTime.of(appuntamento.getData(), appuntamento.getOra());
//            if (dataOraAppuntamento.isBefore(LocalDateTime.now())) {
//                throw new RuntimeException("Non è possibile confermare un appuntamento già passato");
//            }
//        }
//        
//        if (nuovoStato == Appuntamento.StatoAppuntamento.ANNULLATO) {
//            if (appuntamento.getData().isEqual(LocalDate.now())) {
//                throw new RuntimeException("Non è possibile annullare un appuntamento il giorno stesso");
//            }
//        }
//    }
//}