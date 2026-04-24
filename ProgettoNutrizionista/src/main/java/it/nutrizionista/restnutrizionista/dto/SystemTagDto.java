package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;

public record SystemTagDto(
    @NotBlank String id,
    @NotBlank String label
) {}
