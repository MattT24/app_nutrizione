package it.nutrizionista.restnutrizionista.enums;

/** I tre premi riscattabili con i punti gamification. Ogni costante porta costo e testi UI. */
public enum TipoPremio {

    SCONTO_20("20% di sconto mensile", "20% di sconto sul tuo prossimo rinnovo mensile", 1500),
    SCONTO_40("40% di sconto mensile", "40% di sconto sul tuo prossimo rinnovo mensile", 3000),
    MESE_GRATIS("Mese gratis", "Un mese di abbonamento completamente gratuito", 5000);

    private final String titolo;
    private final String descrizione;
    private final int costo;

    TipoPremio(String titolo, String descrizione, int costo) {
        this.titolo = titolo;
        this.descrizione = descrizione;
        this.costo = costo;
    }

    public String getTitolo() { return titolo; }
    public String getDescrizione() { return descrizione; }
    public int getCosto() { return costo; }
}
