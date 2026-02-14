package it.nutrizionista.restnutrizionista.service;

import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Metodo;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.Sesso;

@Service
public class PlicometriaCalcoliService {

    public record Risultati(
            double sommaPliche,
            double densitaCorporea,
            double percentualeMassaGrassa,
            double massaGrassaKg,
            double massaMagraKg
    ) {}

    public Risultati calcola(Plicometria p, Cliente cliente) {
    	
    	if (p.getMetodo() == null) throw new RuntimeException("Metodo mancante");

        if (p.getDataMisurazione() == null) throw new RuntimeException("Data misurazione mancante");
        if (cliente.getSesso() == null) throw new RuntimeException("Sesso cliente mancante");
        if (cliente.getDataNascita() == null) throw new RuntimeException("Data nascita cliente mancante");
        
        double pesoKg = cliente.getPeso();
        if (pesoKg <= 0) throw new RuntimeException("Peso cliente non valido (<=0)");

        int eta = java.time.Period.between(cliente.getDataNascita(), p.getDataMisurazione()).getYears();
    
        Sesso sesso = cliente.getSesso();

        // Metodi manuali: richiedono %MG già valorizzata (in entity)
        if (p.getMetodo() == Metodo.MISURAZIONE_LIBERA || p.getMetodo() == Metodo.PARILLO) {
            if (p.getPercentualeMassaGrassa() == null) {
                throw new RuntimeException("Per il metodo " + p.getMetodo() + " inserire manualmente la % massa grassa");
            }
            double perc = p.getPercentualeMassaGrassa();
            double mgKg = pesoKg * (perc / 100.0);
            double mmKg = pesoKg - mgKg;
            return new Risultati(0, 0, perc, mgKg, mmKg);
        }

        validaPlicheNecessarie(p, sesso);

        double somma = sommaPliche(p, sesso);
        double bd = densitaCorporea(p.getMetodo(), sesso, eta, somma);

        double perc = (495.0 / bd) - 450.0; // Siri
        double mgKg = pesoKg * (perc / 100.0);
        double mmKg = pesoKg - mgKg;

        return new Risultati(somma, bd, perc, mgKg, mmKg);
    }

    private void validaPlicheNecessarie(Plicometria p, Sesso sesso) {
        switch (p.getMetodo()) {
            case JACKSON_POLLOCK_3 -> {
                if (sesso == Sesso.Maschio) requireAll(p.getPettorale(), p.getAddominale(), p.getCoscia());
                else requireAll(p.getTricipite(), p.getSovrailiaca(), p.getCoscia());
            }
            case JACKSON_POLLOCK_7 -> requireAll(
                    p.getPettorale(), p.getAscellare(), p.getTricipite(),
                    p.getSottoscapolare(), p.getAddominale(), p.getSovrailiaca(), p.getCoscia()
            );
            case DURNIN_WOMERSLEY -> requireAll(p.getTricipite(), p.getBicipite(), p.getSottoscapolare(), p.getSovrailiaca());
            default -> throw new IllegalArgumentException("Metodo non gestito: " + p.getMetodo());
        }
    }

    private double sommaPliche(Plicometria p, Sesso sesso) {
        return switch (p.getMetodo()) {
            case JACKSON_POLLOCK_3 -> (sesso == Sesso.Maschio)
                    ? n(p.getPettorale()) + n(p.getAddominale()) + n(p.getCoscia())
                    : n(p.getTricipite()) + n(p.getSovrailiaca()) + n(p.getCoscia());
            case JACKSON_POLLOCK_7 -> n(p.getPettorale()) + n(p.getAscellare()) + n(p.getTricipite())
                    + n(p.getSottoscapolare()) + n(p.getAddominale()) + n(p.getSovrailiaca()) + n(p.getCoscia());
            case DURNIN_WOMERSLEY -> n(p.getTricipite()) + n(p.getBicipite()) + n(p.getSottoscapolare()) + n(p.getSovrailiaca());
            default -> throw new IllegalArgumentException("Metodo non gestito: " + p.getMetodo());
        };
    }

    private double densitaCorporea(Metodo metodo, Sesso sesso, int eta, double somma) {
        switch (metodo) {

            case JACKSON_POLLOCK_3 -> {
                if (sesso == Sesso.Maschio) {
                    double s = somma;
                    return 1.10938 - (0.0008267 * s) + (0.0000016 * s * s) - (0.0002574 * eta);
                } else {
                    double s = somma;
                    return 1.0994921 - (0.0009929 * s) + (0.0000023 * s * s) - (0.0001392 * eta);
                }
            }

            case JACKSON_POLLOCK_7 -> {
                double s7 = somma;
                if (sesso == Sesso.Maschio) {
                    return 1.112 - (0.00043499 * s7) + (0.00000055 * s7 * s7) - (0.00028826 * eta);
                } else {
                    return 1.097 - (0.00046971 * s7) + (0.00000056 * s7 * s7) - (0.00012828 * eta);
                }
            }

            case DURNIN_WOMERSLEY -> {
                double log = Math.log10(somma);
                CoeffDW c = coeffDW(sesso, eta);
                return c.a - (c.b * log);
            }

            default -> throw new IllegalArgumentException("Metodo non gestito: " + metodo);
        }
    }

    private static class CoeffDW {
        final double a, b;
        CoeffDW(double a, double b) { this.a = a; this.b = b; }
    }

    private CoeffDW coeffDW(Sesso sesso, int eta) {
        if (sesso == Sesso.Maschio) {
            if (eta < 17) return new CoeffDW(1.1533, 0.0643);
            if (eta <= 19) return new CoeffDW(1.1620, 0.0630);
            if (eta <= 29) return new CoeffDW(1.1631, 0.0632);
            if (eta <= 39) return new CoeffDW(1.1422, 0.0544);
            if (eta <= 49) return new CoeffDW(1.1620, 0.0700);
            return new CoeffDW(1.1715, 0.0779);
        } else {
            if (eta < 17) return new CoeffDW(1.1369, 0.0598);
            if (eta <= 19) return new CoeffDW(1.1549, 0.0678);
            if (eta <= 29) return new CoeffDW(1.1599, 0.0717);
            if (eta <= 39) return new CoeffDW(1.1423, 0.0632);
            if (eta <= 49) return new CoeffDW(1.1333, 0.0612);
            return new CoeffDW(1.1339, 0.0645);
        }
    }

    private void requireAll(Double... values) {
        for (Double v : values) {
            if (v == null) throw new RuntimeException("Inserire tutte le pliche richieste dal metodo selezionato");
        }
    }

    private double n(Double v) {
        if (v == null) throw new RuntimeException("Pliche mancanti");
        return v;
    }
}
