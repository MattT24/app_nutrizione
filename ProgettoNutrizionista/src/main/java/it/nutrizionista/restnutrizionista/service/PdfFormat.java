package it.nutrizionista.restnutrizionista.service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import it.nutrizionista.restnutrizionista.entity.GiornoSettimana;
import it.nutrizionista.restnutrizionista.entity.Metodo;
import it.nutrizionista.restnutrizionista.entity.Sesso;

/**
 * Formattazione centralizzata per i template PDF (Thymeleaf): numeri in stile italiano,
 * label leggibili di giorni/metodi/pliche, calcolo delta ed età. Sostituisce lo SpEL fragile
 * (String.format, ternari enum) precedentemente presente nei template.
 */
public final class PdfFormat {

    private static final Locale IT = Locale.ITALIAN;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_BREVE = DateTimeFormatter.ofPattern("dd/MM/yy");
    /** Segno meno tipografico (U+2212), non hyphen, coerente col design. */
    public static final String MENO = "−";

    private PdfFormat() {}

    /** Numero "pulito": senza decimali se intero, altrimenti fino a 2 decimali con virgola. */
    public static String num(Double v) {
        if (v == null) return "";
        if (v == Math.rint(v) && !Double.isInfinite(v)) {
            return String.format(IT, "%d", Math.round(v));
        }
        return trimZeri(String.format(IT, "%.2f", v));
    }

    /** Numero con esattamente n decimali (virgola). */
    public static String dec(Double v, int n) {
        if (v == null) return "";
        return String.format(IT, "%." + n + "f", v);
    }

    private static String trimZeri(String s) {
        if (s.contains(",")) {
            s = s.replaceAll("0+$", "").replaceAll(",$", "");
        }
        return s;
    }

    public static String data(LocalDate d) { return d == null ? "" : d.format(DATA); }
    public static String dataBreve(LocalDate d) { return d == null ? "" : d.format(DATA_BREVE); }

    public static Integer eta(LocalDate nascita, LocalDate riferimento) {
        if (nascita == null || riferimento == null) return null;
        return Period.between(nascita, riferimento).getYears();
    }

    /** Direzione del delta corrente-vs-precedente: "up" | "down" | "flat". */
    public static String deltaDir(Double corrente, Double precedente) {
        if (corrente == null || precedente == null) return "flat";
        double d = corrente - precedente;
        if (Math.abs(d) < 1e-9) return "flat";
        return d < 0 ? "down" : "up";
    }

    /** Testo del delta con segno tipografico e unità, es. "−2 kg" / "+3 cm" / "0 mm". */
    public static String delta(Double corrente, Double precedente, String unita) {
        if (corrente == null || precedente == null) return "";
        double d = corrente - precedente;
        String u = (unita == null || unita.isBlank()) ? "" : " " + unita;
        if (Math.abs(d) < 1e-9) return "0" + u;
        String segno = d < 0 ? MENO : "+";
        return segno + num(Math.abs(d)) + u;
    }

    public static String giorno(GiornoSettimana g) {
        if (g == null) return "";
        return switch (g) {
            case LUNEDI -> "Lunedì";
            case MARTEDI -> "Martedì";
            case MERCOLEDI -> "Mercoledì";
            case GIOVEDI -> "Giovedì";
            case VENERDI -> "Venerdì";
            case SABATO -> "Sabato";
            case DOMENICA -> "Domenica";
        };
    }

    public static String sesso(Sesso s) {
        if (s == null) return "";
        return s == Sesso.Maschio ? "Maschio" : "Femmina";
    }

    /** Etichetta leggibile del metodo plicometrico (allineata al frontend). */
    public static String metodoLabel(Metodo m) {
        if (m == null) return "";
        return switch (m) {
            case JACKSON_POLLOCK_3 -> "Jackson-Pollock 3 pliche";
            case JACKSON_POLLOCK_7 -> "Jackson-Pollock 7 pliche";
            case DURNIN_WOMERSLEY -> "Durnin-Womersley";
            case PARILLO -> "Parillo (manuale)";
            case MISURAZIONE_LIBERA -> "Misurazione libera (manuale)";
        };
    }

    /** Numero di pliche del metodo (per la sottolabel del referto), 0 per i metodi manuali. */
    public static int numeroPliche(Metodo m) {
        if (m == null) return 0;
        return switch (m) {
            case JACKSON_POLLOCK_3 -> 3;
            case JACKSON_POLLOCK_7 -> 7;
            case DURNIN_WOMERSLEY -> 4;
            case PARILLO, MISURAZIONE_LIBERA -> 0;
        };
    }

    /** Etichetta leggibile di una plica (chiave campo → nome mostrato). */
    public static String plicaLabel(String key) {
        if (key == null) return "";
        return switch (key) {
            case "tricipite" -> "Tricipite";
            case "bicipite" -> "Bicipite";
            case "sottoscapolare" -> "Sottoscapolare";
            case "sovrailiaca" -> "Soprailiaca";
            case "addominale" -> "Addome";
            case "coscia" -> "Coscia";
            case "pettorale" -> "Pettorale";
            case "ascellare" -> "Ascellare";
            case "polpaccio" -> "Polpaccio";
            default -> key;
        };
    }
}
