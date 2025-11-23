package it.nutrizionista.restnutrizionista.dto;

import org.springframework.data.domain.Page;


import java.util.List;

/** Wrapper generico per risposte paginabili. */
public class PageResponse<T> {

    private List<T> contenuto;
    private int numeroPagina;
    private int dimensionePagina;
    private long totaleElementi;
    private int totalePagine;
    private boolean ultima;

    public PageResponse() {}

    public static <T> PageResponse<T> from(Page<T> page) {
        PageResponse<T> dto = new PageResponse<>();
        dto.setContenuto(page.getContent());
        dto.setNumeroPagina(page.getNumber());
        dto.setDimensionePagina(page.getSize());
        dto.setTotaleElementi(page.getTotalElements());
        dto.setTotalePagine(page.getTotalPages());
        dto.setUltima(page.isLast());
        return dto;
    }

    public List<T> getContenuto() { return contenuto; }
    public void setContenuto(List<T> contenuto) { this.contenuto = contenuto; }
    public int getNumeroPagina() { return numeroPagina; }
    public void setNumeroPagina(int numeroPagina) { this.numeroPagina = numeroPagina; }
    public int getDimensionePagina() { return dimensionePagina; }
    public void setDimensionePagina(int dimensionePagina) { this.dimensionePagina = dimensionePagina; }
    public long getTotaleElementi() { return totaleElementi; }
    public void setTotaleElementi(long totaleElementi) { this.totaleElementi = totaleElementi; }
    public int getTotalePagine() { return totalePagine; }
    public void setTotalePagine(int totalePagine) { this.totalePagine = totalePagine; }
    public boolean isUltima() { return ultima; }
    public void setUltima(boolean ultima) { this.ultima = ultima; }
}
