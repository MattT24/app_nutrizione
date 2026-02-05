package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AlimentoDaEvitareDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoDaEvitareFormDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoDaEvitare;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoDaEvitareRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import jakarta.validation.Valid;

@Service
public class AlimentoDaEvitareService {

	@Autowired private AlimentoDaEvitareRepository repo;
	@Autowired private ClienteRepository clienteRepo;
	@Autowired private AlimentoBaseRepository alimentoRepo;

	@Transactional
	public AlimentoDaEvitareDto create(@Valid AlimentoDaEvitareFormDto form) {
        // Verifica esistenza
		if (repo.existsByCliente_IdAndAlimento_Id(form.getCliente().getId(), form.getAlimento().getId())) {
			throw new RuntimeException("Questo alimento è già nella lista 'da evitare' del cliente");
		}
        
		AlimentoDaEvitare e = new AlimentoDaEvitare();
		Cliente cliente = clienteRepo.findById(form.getCliente().getId())
				.orElseThrow(() -> new RuntimeException("Cliente non trovato"));
        
		AlimentoBase alimento = alimentoRepo.findById(form.getAlimento().getId())
				.orElseThrow(() -> new RuntimeException("Alimento non trovato"));
        
		e.setCliente(cliente);
		e.setAlimento(alimento);
        // e.setTipo(form.getTipo()); // Se gestisci il tipo (Allergia/Gusto)
        
		return DtoMapper.toAlimentoDaEvitareDtoLight(repo.save(e));
	}

	@Transactional
	public AlimentoDaEvitareDto update(@Valid AlimentoDaEvitareFormDto form) {
		if (form.getId() == null) throw new RuntimeException("Id obbligatorio per update");
        
		AlimentoDaEvitare e = repo.findById(form.getId())
				.orElseThrow(() -> new RuntimeException("Elemento non trovato"));
        
        // Se si vuole cambiare alimento (raro, di solito si cancella e ricrea)
		AlimentoBase alimento = alimentoRepo.findById(form.getAlimento().getId())
				.orElseThrow(() -> new RuntimeException("Alimento non trovato"));
		e.setAlimento(alimento);
        
		return DtoMapper.toAlimentoDaEvitareDtoLight(repo.save(e));
	}

	@Transactional
	public void delete(Long id) { repo.deleteById(id); }

    // Lista per cliente (Paginata)
	@Transactional(readOnly = true)
	public PageResponse<AlimentoDaEvitareDto> listByCliente(Long clienteId, Pageable pageable) {
		return PageResponse.from(
            repo.findByCliente_Id(clienteId, pageable)
                .map(DtoMapper::toAlimentoDaEvitareDtoLight)
        );
	}

	@Transactional(readOnly = true)
	public AlimentoDaEvitareDto getById(Long id) {
		return repo.findById(id)
            .map(DtoMapper::toAlimentoDaEvitareDtoLight)
            .orElseThrow(()-> new RuntimeException("Elemento non trovato"));
	}
}