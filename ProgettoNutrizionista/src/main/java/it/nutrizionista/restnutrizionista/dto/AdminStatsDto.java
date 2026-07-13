package it.nutrizionista.restnutrizionista.dto;

/** Statistiche aggregate per la dashboard super admin. */
public class AdminStatsDto {
    private long totaleNutrizionisti;
    private long attivi;
    private long inattivi;
    /** Numero di giorni entro cui un login è considerato "attivo". */
    private int sogliaGiorniAttivita;

    public AdminStatsDto() {}

    public AdminStatsDto(long totaleNutrizionisti, long attivi, long inattivi, int sogliaGiorniAttivita) {
        this.totaleNutrizionisti = totaleNutrizionisti;
        this.attivi = attivi;
        this.inattivi = inattivi;
        this.sogliaGiorniAttivita = sogliaGiorniAttivita;
    }

    public long getTotaleNutrizionisti() { return totaleNutrizionisti; }
    public void setTotaleNutrizionisti(long totaleNutrizionisti) { this.totaleNutrizionisti = totaleNutrizionisti; }
    public long getAttivi() { return attivi; }
    public void setAttivi(long attivi) { this.attivi = attivi; }
    public long getInattivi() { return inattivi; }
    public void setInattivi(long inattivi) { this.inattivi = inattivi; }
    public int getSogliaGiorniAttivita() { return sogliaGiorniAttivita; }
    public void setSogliaGiorniAttivita(int sogliaGiorniAttivita) { this.sogliaGiorniAttivita = sogliaGiorniAttivita; }
}
