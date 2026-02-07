package it.nutrizionista.restnutrizionista.dto;

import java.time.LocalDate;

import it.nutrizionista.restnutrizionista.entity.Metodo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

public class PlicometriaFormDto {

    private Long id;

    @NotNull(message = "L'ID del cliente è obbligatorio")
    private ClienteDto cliente;

    @NotNull(message = "Il metodo da utilizzare è obbligatorio")
    private Metodo metodo; 

    @NotNull(message = "La data è obbligatoria")
    @PastOrPresent(message = "La data non può essere nel futuro")
    private LocalDate dataMisurazione;

    // --- MISURAZIONI (mm) ---
    // Nota: Non usiamo @NotNull qui perché alcuni campi rimarranno vuoti
    // a seconda del metodo scelto (es. JP3 non usa il bicipite).
    
    @Positive(message = "Il valore deve essere maggiore di 0")
    @Max(value = 100, message = "Valore anomalo (>100mm)")
    private Double tricipite;

    @Positive(message = "Il valore deve essere maggiore di 0")
    @Max(value = 100, message = "Valore anomalo (>100mm)")
    private Double bicipite; // Essenziale per Durnin-Womersley (4 pliche)

    @Positive(message = "Il valore deve essere maggiore di 0")
    @Max(value = 100, message = "Valore anomalo (>100mm)")
    private Double sottoscapolare;

    @Positive(message = "Il valore deve essere maggiore di 0")
    @Max(value = 100, message = "Valore anomalo (>100mm)")
    private Double pettorale;

    @Positive(message = "Il valore deve essere maggiore di 0")
    @Max(value = 100, message = "Valore anomalo (>100mm)")
    private Double ascellare; // o ascellareMedia

    @Positive(message = "Il valore deve essere maggiore di 0")
    @Max(value = 100, message = "Valore anomalo (>100mm)")
    private Double sovrailiaca;

    @Positive(message = "Il valore deve essere maggiore di 0")
    @Max(value = 100, message = "Valore anomalo (>100mm)")
    private Double addominale;

    @Positive(message = "Il valore deve essere maggiore di 0")
    @Max(value = 100, message = "Valore anomalo (>100mm)")
    private Double coscia;
    
    @Positive(message = "Il valore deve essere maggiore di 0")
    @Max(value = 100, message = "Valore anomalo (>100mm)")
    private Double polpaccio; // Opzionale, ma utile in alcuni metodi

    private String note;

    // --- GETTERS & SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ClienteDto getCliente() { return cliente; }
    public void setCliente(ClienteDto cliente) { this.cliente = cliente; }

    public Metodo getMetodo() { return metodo; }
    public void setMetodo(Metodo metodo) { this.metodo = metodo; }

    public LocalDate getDataMisurazione() { return dataMisurazione; }
    public void setDataMisurazione(LocalDate dataMisurazione) { this.dataMisurazione = dataMisurazione; }

    public Double getTricipite() { return tricipite; }
    public void setTricipite(Double tricipite) { this.tricipite = tricipite; }

    public Double getBicipite() { return bicipite; }
    public void setBicipite(Double bicipite) { this.bicipite = bicipite; }

    public Double getSottoscapolare() { return sottoscapolare; }
    public void setSottoscapolare(Double sottoscapolare) { this.sottoscapolare = sottoscapolare; }

    public Double getPettorale() { return pettorale; }
    public void setPettorale(Double pettorale) { this.pettorale = pettorale; }

    public Double getAscellare() { return ascellare; }
    public void setAscellare(Double ascellare) { this.ascellare = ascellare; }

    public Double getSovrailiaca() { return sovrailiaca; }
    public void setSovrailiaca(Double sovrailiaca) { this.sovrailiaca = sovrailiaca; }

    public Double getAddominale() { return addominale; }
    public void setAddominale(Double addominale) { this.addominale = addominale; }

    public Double getCoscia() { return coscia; }
    public void setCoscia(Double coscia) { this.coscia = coscia; }
    
    public Double getPolpaccio() { return polpaccio; }
    public void setPolpaccio(Double polpaccio) { this.polpaccio = polpaccio; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}