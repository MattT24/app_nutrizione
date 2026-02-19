package it.nutrizionista.restnutrizionista.entity;

public enum LivelloDiAttivita {
	SEDENTARIO(1.2),              // 0 allenamenti/sett
	LEGGERMENTE_ATTIVO(1.375),    // 1-2 allenamenti/sett
	MODERATAMENTE_ATTIVO(1.55),   // 3-4 allenamenti/sett
	MOLTO_ATTIVO(1.725),          // 5-6 allenamenti/sett
	ESTREMAMENTE_ATTIVO(1.9);     // 7+ allenamenti/sett

	private final double laf;

	LivelloDiAttivita(double laf) {
		this.laf = laf;
	}

	public double getLaf() {
		return laf;
	}
}
