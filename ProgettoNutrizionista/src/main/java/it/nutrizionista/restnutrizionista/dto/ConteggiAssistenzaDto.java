package it.nutrizionista.restnutrizionista.dto;

/** Conteggi leggeri per i badge della dashboard super admin. */
public class ConteggiAssistenzaDto {
    private long inAttesa;
    private long accettati;
    public long getInAttesa() { return inAttesa; }
    public void setInAttesa(long inAttesa) { this.inAttesa = inAttesa; }
    public long getAccettati() { return accettati; }
    public void setAccettati(long accettati) { this.accettati = accettati; }
}
