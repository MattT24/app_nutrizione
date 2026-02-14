package it.nutrizionista.restnutrizionista.service;

import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.AlternativeMode;

@Service
public class AlternativeSuggestionCalculator {

	public Integer suggestQuantity(AlimentoPasto alimentoPasto, AlimentoBase alimentoAlternativo, AlternativeMode mode) {
		if (alimentoPasto == null || alimentoAlternativo == null) return 100;
		if (alimentoPasto.getAlimento() == null) return 100;
		if (alimentoPasto.getAlimento().getMacronutrienti() == null) return 100;
		if (alimentoAlternativo.getMacronutrienti() == null) return 100;

		double mainPerGram = perGram(alimentoPasto.getAlimento(), mode);
		double altPerGram = perGram(alimentoAlternativo, mode);
		if (mainPerGram <= 0 || altPerGram <= 0) return 100;

		double target = mainPerGram * alimentoPasto.getQuantita();
		double suggested = target / altPerGram;
		long rounded = Math.round(suggested);
		if (rounded < 1) return 1;
		if (rounded > 5000) return 5000;
		return (int) rounded;
	}

	private double perGram(AlimentoBase alimento, AlternativeMode mode) {
		if (alimento == null || alimento.getMacronutrienti() == null) return 0;
		if (alimento.getMisuraInGrammi() == null || alimento.getMisuraInGrammi() <= 0) return 0;

		double value = switch (mode != null ? mode : AlternativeMode.CALORIE) {
		case CALORIE -> safe(alimento.getMacronutrienti().getCalorie());
		case PROTEINE -> safe(alimento.getMacronutrienti().getProteine());
		case CARBOIDRATI -> safe(alimento.getMacronutrienti().getCarboidrati());
		case GRASSI -> safe(alimento.getMacronutrienti().getGrassi());
		};

		return value / alimento.getMisuraInGrammi();
	}

	private double safe(Double value) {
		return value != null ? value : 0;
	}
}

