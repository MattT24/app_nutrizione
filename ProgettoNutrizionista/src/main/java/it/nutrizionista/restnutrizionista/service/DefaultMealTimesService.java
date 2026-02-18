package it.nutrizionista.restnutrizionista.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.entity.Pasto;
import jakarta.annotation.PostConstruct;

@Service
public class DefaultMealTimesService {
	private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

	@Value("${meal.defaults.colazione.start:07:00}") private String colazioneStart;
	@Value("${meal.defaults.colazione.end:09:00}") private String colazioneEnd;
	@Value("${meal.defaults.pranzo.start:12:30}") private String pranzoStart;
	@Value("${meal.defaults.pranzo.end:14:30}") private String pranzoEnd;
	@Value("${meal.defaults.merenda.start:16:30}") private String merendaStart;
	@Value("${meal.defaults.merenda.end:17:30}") private String merendaEnd;
	@Value("${meal.defaults.cena.start:19:30}") private String cenaStart;
	@Value("${meal.defaults.cena.end:21:30}") private String cenaEnd;

	private LocalTime colazioneStartTime;
	private LocalTime colazioneEndTime;
	private LocalTime pranzoStartTime;
	private LocalTime pranzoEndTime;
	private LocalTime merendaStartTime;
	private LocalTime merendaEndTime;
	private LocalTime cenaStartTime;
	private LocalTime cenaEndTime;

	@PostConstruct
	void init() {
		colazioneStartTime = parseOrFail("meal.defaults.colazione.start", colazioneStart);
		colazioneEndTime = parseOrFail("meal.defaults.colazione.end", colazioneEnd);
		pranzoStartTime = parseOrFail("meal.defaults.pranzo.start", pranzoStart);
		pranzoEndTime = parseOrFail("meal.defaults.pranzo.end", pranzoEnd);
		merendaStartTime = parseOrFail("meal.defaults.merenda.start", merendaStart);
		merendaEndTime = parseOrFail("meal.defaults.merenda.end", merendaEnd);
		cenaStartTime = parseOrFail("meal.defaults.cena.start", cenaStart);
		cenaEndTime = parseOrFail("meal.defaults.cena.end", cenaEnd);

		assertRange("Colazione", colazioneStartTime, colazioneEndTime);
		assertRange("Pranzo", pranzoStartTime, pranzoEndTime);
		assertRange("Merenda", merendaStartTime, merendaEndTime);
		assertRange("Cena", cenaStartTime, cenaEndTime);
	}

	public void applyDefaultTimesIfMissing(Pasto p) {
		if (p == null) return;
		if (p.getOrarioInizio() != null || p.getOrarioFine() != null) return;

		String code = p.getDefaultCode() != null ? p.getDefaultCode() : p.getNome();
		if (code == null) return;
		String normalized = code.trim().toLowerCase(Locale.ROOT);

		if ("colazione".equals(normalized)) {
			p.setOrarioInizio(colazioneStartTime);
			p.setOrarioFine(colazioneEndTime);
		} else if ("pranzo".equals(normalized)) {
			p.setOrarioInizio(pranzoStartTime);
			p.setOrarioFine(pranzoEndTime);
		} else if ("merenda".equals(normalized)) {
			p.setOrarioInizio(merendaStartTime);
			p.setOrarioFine(merendaEndTime);
		} else if ("cena".equals(normalized)) {
			p.setOrarioInizio(cenaStartTime);
			p.setOrarioFine(cenaEndTime);
		}
	}

	private static LocalTime parseOrFail(String key, String value) {
		try {
			return LocalTime.parse(value, HH_MM);
		} catch (DateTimeParseException ex) {
			throw new IllegalStateException("Configurazione orario non valida: " + key + "=" + value + " (atteso HH:mm)");
		}
	}

	private static void assertRange(String label, LocalTime start, LocalTime end) {
		if (start == null || end == null) throw new IllegalStateException("Range orario non valido per " + label);
		if (!start.isBefore(end)) throw new IllegalStateException("Range orario non valido per " + label + ": inizio deve essere < fine");
	}
}

