package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
//utile per riproporre pasti precedentemente creati nelle schede dello stesso cliente
public class SchedaPastoRequest {
	@NotNull(message = "La scheda è obbligatoria")
    private SchedaDto scheda;
	//può essere vuota se nessuna scheda è già stata creata
    private List<PastoDto> pasti;

	public SchedaDto getScheda() {
		return scheda;
	}

	public void setScheda(SchedaDto scheda) {
		this.scheda = scheda;
	}

	public List<PastoDto> getPasti() {
		return pasti;
	}

	public void setPasti(List<PastoDto> pasti) {
		this.pasti = pasti;
	} 
    
}
