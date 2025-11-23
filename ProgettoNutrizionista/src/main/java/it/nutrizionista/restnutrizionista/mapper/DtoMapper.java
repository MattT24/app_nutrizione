package it.nutrizionista.restnutrizionista.mapper;

import java.util.stream.Collectors;

import it.nutrizionista.restnutrizionista.dto.AppuntamentoDto;
import it.nutrizionista.restnutrizionista.dto.AppuntamentoFormDto;
import it.nutrizionista.restnutrizionista.dto.GruppoDto;
import it.nutrizionista.restnutrizionista.dto.PermessoDto;
import it.nutrizionista.restnutrizionista.dto.PermessoRuoloDto;
import it.nutrizionista.restnutrizionista.dto.RuoloDto;
import it.nutrizionista.restnutrizionista.dto.UtenteDto;
import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.Cliente;
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
	
	
	
	//mapper per l'entità appuntamento
	

	public static AppuntamentoDto toAppuntamentoDto(Appuntamento appuntamento) {
	    if (appuntamento == null) {
	        return null;
	    }

	    AppuntamentoDto dto = new AppuntamentoDto();
	    dto.setId(appuntamento.getId());
	    dto.setNutrizionistaId(appuntamento.getNutrizionista().getId());
	    dto.setNutrizionistaNome(appuntamento.getNutrizionista().getNome());
	    dto.setNutrizionistaCognome(appuntamento.getNutrizionista().getCognome());
	    dto.setClienteId(appuntamento.getCliente().getId());
	    dto.setClienteNome(appuntamento.getCliente().getNome());
	    dto.setClienteCognome(appuntamento.getCliente().getCognome());
	    dto.setDescrizioneAppuntamento(appuntamento.getDescrizioneAppuntamento());
	    dto.setData(appuntamento.getData());
	    dto.setOra(appuntamento.getOra());
	    dto.setModalita(appuntamento.getModalita());
	    dto.setStato(appuntamento.getStato());
	    dto.setLuogo(appuntamento.getLuogo());
	    dto.setEmailCliente(appuntamento.getEmailCliente());
	    dto.setCreatedAt(appuntamento.getCreatedAt());
	    dto.setUpdatedAt(appuntamento.getUpdatedAt());

	    return dto;
	}

	
	public static Appuntamento toAppuntamento(AppuntamentoFormDto formDTO, Utente nutrizionista, Cliente cliente) {
	    if (formDTO == null) {
	        return null;
	    }

	    Appuntamento appuntamento = new Appuntamento();
	    appuntamento.setNutrizionista(nutrizionista);
	    appuntamento.setCliente(cliente);
	    appuntamento.setDescrizioneAppuntamento(formDTO.getDescrizioneAppuntamento());
	    appuntamento.setData(formDTO.getData());
	    appuntamento.setOra(formDTO.getOra());
	    appuntamento.setModalita(formDTO.getModalita());
	    appuntamento.setStato(formDTO.getStato() != null ? formDTO.getStato() : Appuntamento.StatoAppuntamento.PROGRAMMATO);
	    appuntamento.setLuogo(formDTO.getLuogo());
	    
	    
	    String emailCliente = formDTO.getEmailCliente() != null ? 
	            formDTO.getEmailCliente() : cliente.getEmail();
	    appuntamento.setEmailCliente(emailCliente);

	    return appuntamento;
	}

	
	public static void updateAppuntamentoFromFormDto(Appuntamento appuntamento, AppuntamentoFormDto formDTO) {
	    if (appuntamento == null || formDTO == null) {
	        return;
	    }

	    if (formDTO.getDescrizioneAppuntamento() != null) {
	        appuntamento.setDescrizioneAppuntamento(formDTO.getDescrizioneAppuntamento());
	    }
	    if (formDTO.getData() != null) {
	        appuntamento.setData(formDTO.getData());
	    }
	    if (formDTO.getOra() != null) {
	        appuntamento.setOra(formDTO.getOra());
	    }
	    if (formDTO.getModalita() != null) {
	        appuntamento.setModalita(formDTO.getModalita());
	    }
	    if (formDTO.getStato() != null) {
	        appuntamento.setStato(formDTO.getStato());
	    }
	    if (formDTO.getLuogo() != null) {
	        appuntamento.setLuogo(formDTO.getLuogo());
	    }
	    if (formDTO.getEmailCliente() != null) {
	        appuntamento.setEmailCliente(formDTO.getEmailCliente());
	    }
	}
	
}
