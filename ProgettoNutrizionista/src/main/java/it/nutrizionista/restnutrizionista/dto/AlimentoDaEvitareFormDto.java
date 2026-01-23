package it.nutrizionista.restnutrizionista.dto;

import it.nutrizionista.restnutrizionista.entity.TipoRestrizione;
import jakarta.validation.constraints.NotBlank;

public class AlimentoDaEvitareFormDto {

    private Long id;
    @NotBlank(message = "L'alimento base è obbligatorio")
	private AlimentoBaseDto alimento;
    @NotBlank(message = "Il cliente è obbligatorio")
	private ClienteDto cliente;
    
    private TipoRestrizione tipo; 
    private String note;

    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public AlimentoBaseDto getAlimento() {
		return alimento;
	}
	public void setAlimento(AlimentoBaseDto alimento) {
		this.alimento = alimento;
	}
	public ClienteDto getCliente() {
		return cliente;
	}
	public void setCliente(ClienteDto cliente) {
		this.cliente = cliente;
	}
	public TipoRestrizione getTipo() {
		return tipo;
	}
	public void setTipo(TipoRestrizione tipo) {
		this.tipo = tipo;
	}
	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}

    
    
}
