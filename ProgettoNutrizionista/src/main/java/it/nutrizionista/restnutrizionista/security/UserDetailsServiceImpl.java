package it.nutrizionista.restnutrizionista.security;

import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired private UtenteRepository utenteRepository;

    // Sottoclasse per trasportare l'entità Utente
    public static class CustomUserDetails extends org.springframework.security.core.userdetails.User {
        private final Utente utente;

        public CustomUserDetails(Utente utente, java.util.List<SimpleGrantedAuthority> authorities) {
            super(utente.getEmail(), utente.getPassword(), authorities);
            this.utente = utente;
        }

        public Utente getUtente() { return utente; }
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utente u = utenteRepository.findWithAuthoritiesByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + email));

        var authorities = u.getRuolo().getRuoloPermessi().stream()
                .map(rp -> new SimpleGrantedAuthority(rp.getPermesso().getAlias()))
                .collect(Collectors.toList());

        return new CustomUserDetails(u, authorities);
    }
}