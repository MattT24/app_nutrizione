package it.nutrizionista.restnutrizionista.service;

import it.nutrizionista.restnutrizionista.dto.CalcoloTdeeDto;
import it.nutrizionista.restnutrizionista.dto.CalcoloTdeeFormDto;
import it.nutrizionista.restnutrizionista.entity.CalcoloTdee;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.repository.CalcoloTdeeRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CalcoloTdeeService {

    @Autowired
    private CalcoloTdeeRepository calcoloTdeeRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Transactional
    public CalcoloTdeeDto calcolaESalva(CalcoloTdeeFormDto form) {
        Cliente cliente = clienteRepository.findById(form.getClienteId())
                .orElseThrow(() -> new NotFoundException("Cliente non trovato con ID: " + form.getClienteId()));

        double bmr = (10 * form.getPeso()) + (6.25 * form.getAltezza()) - (5 * form.getEta());
        if (form.getSesso().equalsIgnoreCase("M") || form.getSesso().equalsIgnoreCase("MASCHIO")) {
            bmr += 5;
        } else {
            bmr -= 161;
        }

        double tdee = bmr * form.getLivelloAttivita();

        CalcoloTdee entity = new CalcoloTdee();
        entity.setCliente(cliente);
        entity.setDataCalcolo(LocalDate.now());
        entity.setSesso(form.getSesso());
        entity.setEta(form.getEta());
        entity.setPeso(form.getPeso());
        entity.setAltezza(form.getAltezza());
        entity.setLivelloAttivita(form.getLivelloAttivita());
        entity.setBmr(bmr);
        entity.setTdee(tdee);

        calcoloTdeeRepository.save(entity);

        return mapToDto(entity);
    }

    @Transactional(readOnly = true)
    public List<CalcoloTdeeDto> getStoricoCliente(Long clienteId) {
        List<CalcoloTdee> calcoli = calcoloTdeeRepository.findByClienteIdOrderByDataCalcoloDesc(clienteId);
        return calcoli.stream().map(this::mapToDto).collect(Collectors.toList());
    }
    
    @Transactional
    public void eliminaCalcolo(Long calcoloId) {
        if (!calcoloTdeeRepository.existsById(calcoloId)) {
            throw new NotFoundException("Calcolo non trovato");
        }
        calcoloTdeeRepository.deleteById(calcoloId);
    }
    
    @Transactional
    public void eliminaTuttiCalcoliCliente(Long clienteId) {
        calcoloTdeeRepository.deleteByClienteId(clienteId);
    }

    private CalcoloTdeeDto mapToDto(CalcoloTdee entity) {
        CalcoloTdeeDto dto = new CalcoloTdeeDto();
        dto.setId(entity.getId());
        dto.setDataCalcolo(entity.getDataCalcolo());
        dto.setSesso(entity.getSesso());
        dto.setEta(entity.getEta());
        dto.setPeso(entity.getPeso());
        dto.setAltezza(entity.getAltezza());
        dto.setLivelloAttivita(entity.getLivelloAttivita());
        dto.setBmr(Math.round(entity.getBmr()));
        dto.setTdee(Math.round(entity.getTdee()));
        dto.setClienteId(entity.getCliente().getId());
        return dto;
    }
}