package it.nutrizionista.restnutrizionista.entity;

public enum TipoObiettivo {
	DIMAGRIMENTO(0.80, 35, 35, 30),
	MANTENIMENTO(1.00, 25, 50, 25),
	MASSA(1.15, 30, 45, 25),
	RICOMPOSIZIONE(0.95, 40, 30, 30);

	private final double moltiplicatoreTdee;
	private final int pctProteine;
	private final int pctCarboidrati;
	private final int pctGrassi;

	TipoObiettivo(double moltiplicatoreTdee, int pctProteine, int pctCarboidrati, int pctGrassi) {
		this.moltiplicatoreTdee = moltiplicatoreTdee;
		this.pctProteine = pctProteine;
		this.pctCarboidrati = pctCarboidrati;
		this.pctGrassi = pctGrassi;
	}

	public double getMoltiplicatoreTdee() {
		return moltiplicatoreTdee;
	}

	public int getPctProteine() {
		return pctProteine;
	}

	public int getPctCarboidrati() {
		return pctCarboidrati;
	}

	public int getPctGrassi() {
		return pctGrassi;
	}
}
