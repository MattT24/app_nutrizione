package it.nutrizionista.restnutrizionista.dto;

/**
 * DTO leggero per navbar e home: solo i dati strettamente necessari
 * alla visualizzazione dell'utente loggato (saluto + logo).
 * Per il profilo completo usare {@link UtenteDto} via /api/utenti/profilo.
 */
public record ProfiloLightDto(
        Long id,
        String nome,
        String cognome,
        String filePathLogo,
        String ruoloNome,
        String email
) {}
