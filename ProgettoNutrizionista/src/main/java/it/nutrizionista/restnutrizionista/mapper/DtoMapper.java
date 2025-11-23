package it.nutrizionista.restnutrizionista.mapper;

import java.util.stream.Collectors;

import it.nutrizionista.restnutrizionista.dto.GruppoDto;
import it.nutrizionista.restnutrizionista.dto.PermessoDto;
import it.nutrizionista.restnutrizionista.dto.PermessoRuoloDto;
import it.nutrizionista.restnutrizionista.dto.RuoloDto;
import it.nutrizionista.restnutrizionista.dto.UtenteDto;
import it.nutrizionista.restnutrizionista.entity.Gruppo;
import it.nutrizionista.restnutrizionista.entity.Permesso;
import it.nutrizionista.restnutrizionista.entity.Ruolo;
import it.nutrizionista.restnutrizionista.entity.RuoloPermesso;
import it.nutrizionista.restnutrizionista.entity.Utente;

/**
 * Mapper Entity -> DTO con metodi ESPLICITI (niente overload con booleani),
 * per evitare confusione ed eliminare ricorsioni.
 */
public class DtoMapper {

	   
    /** Gruppo "light": solo campi del gruppo, senza lista dei permessi. */
    public static GruppoDto toGruppoDtoLight(Gruppo g) {
        if (g == null) return null;
        GruppoDto dto = new GruppoDto();
        dto.setId(g.getId());
        dto.setNome(g.getNome());
        dto.setAlias(g.getAlias());
        dto.setCreatedAt(g.getCreatedAt());
        dto.setUpdatedAt(g.getUpdatedAt());
        return dto;
    }

    /** Gruppo "withPermessi": include anche la lista dei PermessoDto (light). */
    public static GruppoDto toGruppoDtoWithPermessi(Gruppo g) {
        if (g == null) return null;
        GruppoDto dto = toGruppoDtoLight(g);
        if (g.getPermessi() != null) {
            dto.setPermessi(
                g.getPermessi().stream()
                  .map(DtoMapper::toPermessoDtoLight)  // permesso light
                  .collect(Collectors.toList())
            );
        }
        return dto;
    }

    
    /** Permesso "light": solo campi + gruppo (light). */
    public static PermessoDto toPermessoDtoLight(Permesso p) {
        if (p == null) return null;
        PermessoDto dto = new PermessoDto();
        dto.setId(p.getId());
        dto.setNome(p.getNome());
        dto.setAlias(p.getAlias());
        dto.setGruppo(toGruppoDtoLight(p.getGruppo())); // gruppo light
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }

    /**
     * Permesso "withAssoc": include anche la lista di associazioni ruolo-permesso.
     * Ogni associazione è mappata in modo "safe" (ruolo/permesso light) per evitare ricorsioni.
     */
    public static PermessoDto toPermessoDtoWithAssoc(Permesso p) {
        if (p == null) return null;
        PermessoDto dto = toPermessoDtoLight(p);
        if (p.getRuoloPermessi() != null) {
            dto.setRuoloPermessi(
                p.getRuoloPermessi().stream()
                  .map(DtoMapper::toRuoloPermessoDtoSafe) // safe: nested light
                  .collect(Collectors.toList())
            );
        }
        return dto;
    }

    
    /** Ruolo "light": solo campi base, senza lista associazioni. */
    public static RuoloDto toRuoloDtoLight(Ruolo r) {
        if (r == null) return null;
        RuoloDto dto = new RuoloDto();
        dto.setId(r.getId());
        dto.setNome(r.getNome());
        dto.setAlias(r.getAlias());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        return dto;
    }

    /**
     * Ruolo "withAssoc": include la lista di associazioni ruolo-permesso.
     * Ogni associazione è mappata in modo "safe" (ruolo/permesso light) per evitare ricorsioni.
     */
    public static RuoloDto toRuoloDtoWithAssoc(Ruolo r) {
        if (r == null) return null;
        RuoloDto dto = toRuoloDtoLight(r);
        if (r.getRuoloPermessi() != null) {
            dto.setRuoloPermessi(
                r.getRuoloPermessi().stream()
                  .map(DtoMapper::toRuoloPermessoDtoSafe) // safe: nested light
                  .collect(Collectors.toList())
            );
        }
        return dto;
    }

    
    /**
     * Associazione "safe": mappa RuoloPermesso con:
     * - Ruolo LIGHT (senza lista ruoloPermessi)
     * - Permesso LIGHT (senza lista ruoloPermessi)
     * per evitare strutture annidate infinite.
     */
    public static PermessoRuoloDto toRuoloPermessoDtoSafe(RuoloPermesso rp) {
        if (rp == null) return null;

        PermessoRuoloDto dto = new PermessoRuoloDto();
        dto.setId(rp.getId());

        // Ruolo light
        Ruolo ruolo = rp.getRuolo();
        if (ruolo != null) {
            dto.setRuolo(toRuoloDtoLight(ruolo));
        }

        // Permesso light
        Permesso perm = rp.getPermesso();
        if (perm != null) {
            dto.setPermesso(toPermessoDtoLight(perm));
        }

        return dto;
    }

    
    /**
     * UtenteDto con Ruolo LIGHT (niente lista di associazioni),
     * sufficiente per la maggior parte degli scenari e senza ricorsioni.
     */
    public static UtenteDto toUtenteDto(Utente u) {
        if (u == null) return null;
        UtenteDto dto = new UtenteDto();
        dto.setId(u.getId());
        dto.setNome(u.getNome());
        dto.setCognome(u.getCognome());
        dto.setCodiceFiscale(u.getCodiceFiscale());
        dto.setEmail(u.getEmail());
        dto.setDataNascita(u.getDataNascita());
        dto.setTelefono(u.getTelefono());
        dto.setIndirizzo(u.getIndirizzo());
        dto.setRuolo(toRuoloDtoLight(u.getRuolo())); // ruolo light
        dto.setCreatedAt(u.getCreatedAt());
        dto.setUpdatedAt(u.getUpdatedAt());
        return dto;
    }
    public static UtenteDto toUtenteDtoLight(Utente u) { //senza ruolo
        if (u == null) return null;
        UtenteDto dto = new UtenteDto();
        dto.setId(u.getId());
        dto.setNome(u.getNome());
        dto.setCognome(u.getCognome());
        dto.setCodiceFiscale(u.getCodiceFiscale());
        dto.setEmail(u.getEmail());
        dto.setDataNascita(u.getDataNascita());
        dto.setTelefono(u.getTelefono());
        dto.setIndirizzo(u.getIndirizzo());
        dto.setCreatedAt(u.getCreatedAt());
        dto.setUpdatedAt(u.getUpdatedAt());
        return dto;
    }
    public static Utente toUtente(UtenteDto dto) {
        if (dto == null) return null;
        Utente u = new Utente();
        u.setId(dto.getId());
        u.setNome(dto.getNome());
        u.setCognome(dto.getCognome());
        u.setCodiceFiscale(dto.getCodiceFiscale());
        u.setEmail(dto.getEmail());
        u.setDataNascita(dto.getDataNascita());
        u.setTelefono(dto.getTelefono());
        u.setIndirizzo(dto.getIndirizzo());
        u.setRuolo(toRuoloLight(dto.getRuolo())); // ruolo light
        u.setCreatedAt(dto.getCreatedAt());
        u.setUpdatedAt(dto.getUpdatedAt());
        return u;
    }
    public static Utente toUtenteLight(UtenteDto dto) { //senza ruolo
        if (dto == null) return null;
        Utente u = new Utente();
        u.setId(dto.getId());
        u.setNome(dto.getNome());
        u.setCognome(dto.getCognome());
        u.setCodiceFiscale(dto.getCodiceFiscale());
        u.setEmail(dto.getEmail());
        u.setDataNascita(dto.getDataNascita());
        u.setTelefono(dto.getTelefono());
        u.setIndirizzo(dto.getIndirizzo());
        u.setCreatedAt(dto.getCreatedAt());
        u.setUpdatedAt(dto.getUpdatedAt());
        return u;
    }

	private static Ruolo toRuoloLight(RuoloDto dto) {
        if (dto == null) return null;
        Ruolo r = new Ruolo();
        r.setId(dto.getId());
        r.setNome(dto.getNome());
        r.setAlias(dto.getAlias());
        r.setCreatedAt(dto.getCreatedAt());
        r.setUpdatedAt(dto.getUpdatedAt());
        return r;
    }
	

	
	
}
