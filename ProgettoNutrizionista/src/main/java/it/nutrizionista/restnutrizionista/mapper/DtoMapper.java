package it.nutrizionista.restnutrizionista.mapper;

import java.util.stream.Collectors;

import it.nutrizionista.restnutrizionista.dto.*;
import it.nutrizionista.restnutrizionista.entity.*;

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
        dto.setFilePathLogo(u.getFilePathLogo());
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
        dto.setFilePathLogo(u.getFilePathLogo());
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
        u.setFilePathLogo(dto.getFilePathLogo());
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
        u.setFilePathLogo(dto.getFilePathLogo());
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
	
	//mapper per l'entità Cliente
	//mapper cliente completo
	public static ClienteDto toClienteDto(Cliente c) {
	    if (c == null) {
	        return null;
	    }
	    ClienteDto dto = new ClienteDto();
	    dto.setId(c.getId());
	    dto.setSesso(c.getSesso());
	    dto.setAltezza(c.getAltezza());
	    dto.setAssunzioneFarmaci(c.getAssunzioneFarmaci());
	    dto.setTelefono(c.getTelefono());
	    dto.setBeveAlcol(c.getBeveAlcol());
	    dto.setCodiceFiscale(c.getCodiceFiscale());
	    dto.setCognome(c.getCognome());
	    dto.setEmail(c.getEmail());
	    dto.setDataNascita(c.getDataNascita());
	    dto.setProblematicheSalutari(c.getProblematicheSalutari());
	    dto.setCreatedAt(c.getCreatedAt());
	    dto.setFunzioniIntestinali(c.getFunzioniIntestinali());
	    dto.setIntolleranze(c.getIntolleranze());
	    dto.setMisurazioni(toMisurazioneDto(c.getMisurazioni()));
	    dto.setNome(c.getNome());
	    dto.setNumAllenamentiSett(c.getNumAllenamentiSett());
	    dto.setNutrizionista(toUtenteDto(c.getNutrizionista()));
	    dto.setPeso(c.getPeso());
	    dto.setProblematicheSalutari(c.getProblematicheSalutari());
	    dto.setUpdatedAt(c.getUpdatedAt());
	    
		return dto;
	    
	}
	//mapper cliente con solo le cose essenziali, vedete se aggiungere info
	public static ClienteDto toClienteDtoLight(Cliente c) {
	    if (c == null) {
	        return null;
	    }
	    ClienteDto dto = new ClienteDto();
	    dto.setId(c.getId());
	    dto.setNome(c.getNome());
	    dto.setCognome(c.getCognome());	    
		return dto;
	    
	}
 
	//Mapper per l'entita alimentoBase
	
	public static AlimentoBaseDto toAlimentoBaseDto(AlimentoBase a) {
		if (a == null) {
	        return null;
	    }
		AlimentoBaseDto dto = new AlimentoBaseDto();
		dto.setId(a.getId());
		dto.setNome(a.getNome());
		dto.setMacroNutrienti(toMacroDto(a.getMacronutrienti()));
		dto.setMicroNutrienti(toMicroDto(a.getMicronutrienti()));
		dto.setMisuraInGrammi(a.getMisuraInGrammi());
		dto.setCreatedAt(a.getCreatedAt());
		dto.setUpdatedAt(a.getUpdatedAt());
		return dto;
	}
	
	public static AlimentoBaseDto toAlimentoBaseDtoMacro(AlimentoBase a) {
		if (a == null) {
	        return null;
	    }
		AlimentoBaseDto dto = new AlimentoBaseDto();
		dto.setId(a.getId());
		dto.setNome(a.getNome());
		dto.setMacroNutrienti(toMacroDto(a.getMacronutrienti()));
		dto.setMisuraInGrammi(a.getMisuraInGrammi());
		return dto;
	}
	
	public static AlimentoBaseDto toAlimentoBaseDtoLight(AlimentoBase a) {
		if (a == null) {
	        return null;
	    }
		AlimentoBaseDto dto = new AlimentoBaseDto();
		dto.setId(a.getId());
		dto.setNome(a.getNome());
		dto.setMisuraInGrammi(a.getMisuraInGrammi()); //asseconda del front decideremo cosa lasciare tipo questo <--
		return dto;
	}
	
	//Mapper per l'entità AlimentoDaEvitare
	
	public static AlimentoDaEvitareDto toAlimentoDaEvitareDtoLight(AlimentoDaEvitare a) {
		if (a == null) {
	        return null;
	    }
		AlimentoDaEvitareDto dto = new AlimentoDaEvitareDto();
		dto.setId(a.getId());
		dto.setAlimento(toAlimentoBaseDto(a.getAlimento()));
		return dto;
	}
	
	public static AlimentoDaEvitareDto toAlimentoDaEvitareDto(AlimentoDaEvitare a) {
		if (a == null) {
	        return null;
	    }
		AlimentoDaEvitareDto dto = new AlimentoDaEvitareDto();
		dto.setId(a.getId());
		dto.setAlimento(toAlimentoBaseDto(a.getAlimento()));
		dto.setCliente(toClienteDtoLight(a.getCliente()));
		return dto;
	}
	//Mapper per l'entità Macro
	
	public static MacroDto toMacroDto(Macro m) {
		if (m == null) {
	        return null;
	    }
		MacroDto dto = new MacroDto();
		dto.setId(m.getId());
		dto.setAlimento(toAlimentoBaseDto(m.getAlimento()));
		dto.setCalorie(m.getCalorie());
		dto.setGrassi(m.getGrassi());
		dto.setProteine(m.getProteine());
		dto.setCarboidrati(m.getCarboidrati());
		return dto;
	}
	//Mapper per l'entità Micro DA FINIRE
	
	public static MicroDto toMicroDto(Micro m) {
		if (m == null) {
	        return null;
	    }
		MicroDto dto = new MicroDto();
//		dto.setId(m.getId());

		return dto;
	}
	
	//Mapper per l'entita Pasto

	
	public static PastoDto toPastoDto(Pasto p) {
		if (p == null) {
	        return null;
	    }
		PastoDto dto = new PastoDto();
		dto.setId(p.getId());
		dto.setNome(p.getNome());
		dto.setScheda(toSchedaDto(p.getScheda()));
		dto.setOrarioFine(p.getOrarioFine());
		dto.setOrarioInizio(p.getOrarioInizio());
		dto.setCreatedAt(p.getCreatedAt());
		dto.setUpdatedAt(p.getUpdatedAt());
		return dto;
	}
	public static PastoDto toPastoDtoLight(Pasto p) { //senza la Scheda
		if (p == null) {
	        return null;
	    }
		PastoDto dto = new PastoDto();
		dto.setId(p.getId());
		dto.setNome(p.getNome());
		dto.setOrarioFine(p.getOrarioFine());
		dto.setOrarioInizio(p.getOrarioInizio());
		dto.setCreatedAt(p.getCreatedAt());
		dto.setUpdatedAt(p.getUpdatedAt());
		return dto;
	}
	
    public static PastoDto toPastoDtoWithAssoc(Pasto p) {
        if (p == null) return null;
        PastoDto dto = toPastoDtoLight(p);
        if (p.getAlimentiPasto() != null) {
            dto.setAlimentiPasto(
                p.getAlimentiPasto().stream()
                  .map(DtoMapper::toAlimentoPastoDtoSafe) // safe: nested light
                  .collect(Collectors.toList())
            );
        }
        return dto;
    }

    //Mapper AlimentoPasto
    
    public static AlimentoPastoDto toAlimentoPastoDtoSafe(AlimentoPasto ap) {
        if (ap == null) return null;

        AlimentoPastoDto dto = new AlimentoPastoDto();
        dto.setId(ap.getId());

        // Ruolo light
        Pasto pasto = ap.getPasto();
        if (pasto != null) {
            dto.setPasto(toPastoDtoLight(pasto));
        }

        // Permesso light
        AlimentoBase alim = ap.getAlimento();
        if (alim != null) {
            dto.setAlimento(toAlimentoBaseDtoLight(alim));
        }

        return dto;
    }
	
	
	//Mapper per l'entità scheda
	
	public static SchedaDto toSchedaDto(Scheda s) {
		if (s == null) {
	        return null;
	    }
		SchedaDto dto = new SchedaDto();
		dto.setId(s.getId());
		dto.setAttiva(s.getAttiva());
		dto.setCliente(toClienteDto(s.getCliente()));
		dto.setCreatedAt(s.getCreatedAt());
		dto.setUpdatedAt(s.getUpdatedAt());
		return dto;
	}
	//mapper per l' entità misurazioneAntrometrica

	public static MisurazioneAntropometricaDto toMisurazioneDto(MisurazioneAntropometrica misurazioni) {

		return null;
	}
	
	
	//mapper per l'entità appuntamento

	 public static AppuntamentoDto toAppuntamentoDto(Appuntamento appuntamento) {
	        if (appuntamento == null) {
	            return null;
	        }

	        AppuntamentoDto dto = new AppuntamentoDto();
	        dto.setId(appuntamento.getId());
	        
	        // Dati nutrizionista
	        dto.setNutrizionistaId(appuntamento.getNutrizionista().getId());
	        dto.setNutrizionistaNome(appuntamento.getNutrizionista().getNome());
	        dto.setNutrizionistaCognome(appuntamento.getNutrizionista().getCognome());
	        
	        // Dati cliente - verifica se è registrato o meno
	        if (appuntamento.getCliente() != null) {
	            // Cliente registrato
	            dto.setClienteId(appuntamento.getCliente().getId());
	            dto.setClienteNome(appuntamento.getCliente().getNome());
	            dto.setClienteCognome(appuntamento.getCliente().getCognome());
	            dto.setClienteRegistrato(true);
	        } else {
	            // Cliente non registrato - usa i campi temporanei
	            dto.setClienteId(null);
	            dto.setClienteNome(appuntamento.getClienteNomeTemp());
	            dto.setClienteCognome(appuntamento.getClienteCognomeTemp());
	            dto.setClienteRegistrato(false);
	        }
	        
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

	    /**
	     * Converte da AppuntamentoFormDto a Appuntamento entity
	     * Gestisce sia clienti registrati (cliente != null) che non registrati (cliente == null)
	     */
	    public static Appuntamento toAppuntamento(AppuntamentoFormDto formDTO, Utente nutrizionista, Cliente cliente) {
	        if (formDTO == null) {
	            return null;
	        }

	        Appuntamento appuntamento = new Appuntamento();
	        appuntamento.setNutrizionista(nutrizionista);
	        appuntamento.setCliente(cliente); // Può essere null per clienti non registrati
	        
	        // Se il cliente non è registrato, popola i campi temporanei
	        if (cliente == null) {
	            appuntamento.setClienteNomeTemp(formDTO.getClienteNome());
	            appuntamento.setClienteCognomeTemp(formDTO.getClienteCognome());
	            
	        }
	        
	        appuntamento.setDescrizioneAppuntamento(formDTO.getDescrizioneAppuntamento());
	        appuntamento.setData(formDTO.getData());
	        appuntamento.setOra(formDTO.getOra());
	        appuntamento.setModalita(formDTO.getModalita());
	        appuntamento.setStato(formDTO.getStato() != null ? formDTO.getStato() : Appuntamento.StatoAppuntamento.PROGRAMMATO);
	        appuntamento.setLuogo(formDTO.getLuogo());

	        // Gestione email cliente
	        String emailCliente;
	        if (formDTO.getEmailCliente() != null) {
	            emailCliente = formDTO.getEmailCliente();
	        } else if (cliente != null) {
	            emailCliente = cliente.getEmail();
	        } else {
	            emailCliente = null; // Verrà validato nel service
	        }
	        appuntamento.setEmailCliente(emailCliente);

	        return appuntamento;
	    }



	
}
	    