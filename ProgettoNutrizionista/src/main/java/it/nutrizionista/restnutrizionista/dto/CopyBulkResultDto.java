package it.nutrizionista.restnutrizionista.dto;

import java.util.ArrayList;
import java.util.List;

public class CopyBulkResultDto {

    private List<CopyResultItemDto> successi = new ArrayList<>();
    private List<CopyResultItemDto> conflitti = new ArrayList<>();
    private int totaleRichiesti;
    private int totaleEseguiti;

    public void addSuccesso(CopyResultItemDto item) {
        successi.add(item);
        totaleEseguiti++;
    }

    public void addConflitto(CopyResultItemDto item) {
        conflitti.add(item);
    }

    public List<CopyResultItemDto> getSuccessi() {
        return successi;
    }

    public void setSuccessi(List<CopyResultItemDto> successi) {
        this.successi = successi;
    }

    public List<CopyResultItemDto> getConflitti() {
        return conflitti;
    }

    public void setConflitti(List<CopyResultItemDto> conflitti) {
        this.conflitti = conflitti;
    }

    public int getTotaleRichiesti() {
        return totaleRichiesti;
    }

    public void setTotaleRichiesti(int totaleRichiesti) {
        this.totaleRichiesti = totaleRichiesti;
    }

    public int getTotaleEseguiti() {
        return totaleEseguiti;
    }

    public void setTotaleEseguiti(int totaleEseguiti) {
        this.totaleEseguiti = totaleEseguiti;
    }
}
