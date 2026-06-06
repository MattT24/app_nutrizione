package it.nutrizionista.restnutrizionista.dto;

import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;
import java.util.List;

public record ValutazioneClinicaDto(
    LivelloAllerta stato,
    List<MotivoValutazioneDto> motivi // Strutturato ad hoc per UI iterabile
) {}
