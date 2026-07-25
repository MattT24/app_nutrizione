package it.nutrizionista.restnutrizionista.security;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import it.nutrizionista.restnutrizionista.entity.Utente;

/**
 * Costruttore CENTRALIZZATO delle authorities Spring a partire dai PERMESSI dell'Utente in DB.
 *
 * <p>Estratto dalla logica storica di {@code UserDetailsServiceImpl} (login legacy): le authorities
 * sono <b>esclusivamente</b> gli alias dei permessi ({@code Utente → Ruolo → RuoloPermesso → Permesso.getAlias()}),
 * stringhe nude (es. {@code ALIMENTO_READ}, {@code SUPER_ADMIN}), <b>senza</b> prefisso {@code ROLE_}/{@code PERMISSION_}.
 * Il RUOLO non è un'authority (il discriminatore platform-admin resta {@code getRuolo().getAlias()} sull'entità).
 *
 * <p>Riusato dai TRE punti che popolano il {@code SecurityContext}:
 * <ul>
 *   <li>login legacy ({@code UserDetailsServiceImpl});</li>
 *   <li>binario Keycloak — chain browser ({@code OidcUserService} custom);</li>
 *   <li>binario Keycloak — chain bearer ({@code JwtAuthenticationConverter} custom).</li>
 * </ul>
 * Così l'authority {@code SUPER_ADMIN} (che è un PERMESSO, oltre che un ruolo) è preservata in tutti i binari
 * → {@code hasAuthority('SUPER_ADMIN')} su {@code /api/admin/**} continua a funzionare.
 */
@Component
public class AuthorityBuilder {

    /** Authorities = alias dei permessi del ruolo dell'utente. Difensivo su ruolo/permessi null. */
    public List<SimpleGrantedAuthority> build(Utente u) {
        if (u == null || u.getRuolo() == null || u.getRuolo().getRuoloPermessi() == null) {
            return List.of();
        }
        return u.getRuolo().getRuoloPermessi().stream()
                .map(rp -> new SimpleGrantedAuthority(rp.getPermesso().getAlias()))
                .collect(Collectors.toList());
    }
}
