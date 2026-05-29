package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * DTO per riordinare elementi (pasti o alimenti) tramite drag-and-drop.
 * Contiene la lista ordinata di ID nell'ordine desiderato.
 */
public record ReorderDto(
	@NotNull(message = "La lista di ID e' obbligatoria")
	List<Long> ids
) {}
