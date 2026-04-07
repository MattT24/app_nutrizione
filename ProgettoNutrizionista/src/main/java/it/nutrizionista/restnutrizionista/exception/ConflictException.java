package it.nutrizionista.restnutrizionista.exception;

import java.util.Collections;
import java.util.List;

public class ConflictException extends RuntimeException {

	private final List<String> conflitti;

	public ConflictException(String message) {
		super(message);
		this.conflitti = Collections.emptyList();
	}

	public ConflictException(String message, List<String> conflitti) {
		super(message);
		this.conflitti = conflitti != null ? conflitti : Collections.emptyList();
	}

	public List<String> getConflitti() {
		return conflitti;
	}
}

