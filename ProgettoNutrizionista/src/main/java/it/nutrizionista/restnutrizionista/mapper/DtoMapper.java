package it.nutrizionista.restnutrizionista.mapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import it.nutrizionista.restnutrizionista.dto.AlimentoAlternativoDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoBaseDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoBaseFormDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoPastoDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoPastoSchedaTemplateDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoSchedaTemplateAlternativaDto;
import it.nutrizionista.restnutrizionista.dto.AppuntamentoDto;
import it.nutrizionista.restnutrizionista.dto.AppuntamentoFormDto;
import it.nutrizionista.restnutrizionista.dto.AvversionePersonaleDto;
import it.nutrizionista.restnutrizionista.dto.ClienteDto;
import it.nutrizionista.restnutrizionista.dto.ClienteFormDto;
import it.nutrizionista.restnutrizionista.dto.GruppoDto;
import it.nutrizionista.restnutrizionista.dto.MacroDto;
import it.nutrizionista.restnutrizionista.dto.MicroDto;
import it.nutrizionista.restnutrizionista.dto.MisurazioneAntropometricaDto;
import it.nutrizionista.restnutrizionista.dto.MisurazioneAntropometricaFormDto;
import it.nutrizionista.restnutrizionista.dto.ObiettivoNutrizionaleDto;
import it.nutrizionista.restnutrizionista.dto.OrariStudioDto;
import it.nutrizionista.restnutrizionista.dto.OrariStudioFormDto;
import it.nutrizionista.restnutrizionista.dto.PastoDto;
import it.nutrizionista.restnutrizionista.dto.PastoSchedaTemplateDto;
import it.nutrizionista.restnutrizionista.dto.PastoTemplateAlternativaDto;
import it.nutrizionista.restnutrizionista.dto.PastoTemplateDto;
import it.nutrizionista.restnutrizionista.dto.PastoTemplateItemDto;
import it.nutrizionista.restnutrizionista.dto.PermessoDto;
import it.nutrizionista.restnutrizionista.dto.PermessoRuoloDto;
import it.nutrizionista.restnutrizionista.dto.PlicometriaDto;
import it.nutrizionista.restnutrizionista.dto.PlicometriaFormDto;
import it.nutrizionista.restnutrizionista.dto.RuoloDto;
import it.nutrizionista.restnutrizionista.dto.SchedaDto;
import it.nutrizionista.restnutrizionista.dto.SchedaFormDto;
import it.nutrizionista.restnutrizionista.dto.SchedaTemplateDto;
import it.nutrizionista.restnutrizionista.dto.SystemTagDto;
import it.nutrizionista.restnutrizionista.dto.UtenteDto;
import it.nutrizionista.restnutrizionista.dto.ValoreMicroDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoAlternativo;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.AlimentoPastoSchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.AlimentoSchedaTemplateAlternativa;
import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.AvversionePersonale;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Gruppo;
import it.nutrizionista.restnutrizionista.entity.Macro;
import it.nutrizionista.restnutrizionista.entity.Micro;
import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;
import it.nutrizionista.restnutrizionista.entity.ObiettivoNutrizionale;
import it.nutrizionista.restnutrizionista.entity.OrariStudio;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.PastoSchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.PastoTemplate;
import it.nutrizionista.restnutrizionista.entity.PastoTemplateAlimento;
import it.nutrizionista.restnutrizionista.entity.PastoTemplateAlternativo;
import it.nutrizionista.restnutrizionista.entity.Permesso;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.Ruolo;
import it.nutrizionista.restnutrizionista.entity.RuoloPermesso;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.SchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.entity.ValoreMicro;
import it.nutrizionista.restnutrizionista.enums.TagStandard;

/**
 * Mapper Entity -> DTO con metodi ESPLICITI (niente overload con booleani),
 * per evitare confusione ed eliminare ricorsioni.
 */
public class DtoMapper {

	/** Gruppo "light": solo campi del gruppo, senza lista dei permessi. */
	public static GruppoDto toGruppoDtoLight(Gruppo g) {
		if (g == null)
			return null;
		GruppoDto dto = new GruppoDto();
		dto.setId(g.getId());
		dto.setNome(g.getNome());
		dto.setAlias(g.getAlias());

		return dto;
	}

	/** Gruppo "withPermessi": include anche la lista dei PermessoDto (light). */
	public static GruppoDto toGruppoDtoWithPermessi(Gruppo g) {
		if (g == null)
			return null;
		GruppoDto dto = toGruppoDtoLight(g);
		if (g.getPermessi() != null) {
			dto.setPermessi(
					g.getPermessi().stream()
							.map(DtoMapper::toPermessoDtoLight) // permesso light
							.collect(Collectors.toList()));
		}
		return dto;
	}

	/** Permesso "light": solo campi + gruppo (light). */
	public static PermessoDto toPermessoDtoLight(Permesso p) {
		if (p == null)
			return null;
		PermessoDto dto = new PermessoDto();
		dto.setId(p.getId());
		dto.setNome(p.getNome());
		dto.setAlias(p.getAlias());
		dto.setGruppo(toGruppoDtoLight(p.getGruppo())); // gruppo light

		return dto;
	}

	/**
	 * Permesso "withAssoc": include anche la lista di associazioni ruolo-permesso.
	 * Ogni associazione è mappata in modo "safe" (ruolo/permesso light) per evitare
	 * ricorsioni.
	 */
	public static PermessoDto toPermessoDtoWithAssoc(Permesso p) {
		if (p == null)
			return null;
		PermessoDto dto = toPermessoDtoLight(p);
		if (p.getRuoloPermessi() != null) {
			dto.setRuoloPermessi(
					p.getRuoloPermessi().stream()
							.map(DtoMapper::toRuoloPermessoDtoSafe) // safe: nested light
							.collect(Collectors.toList()));
		}
		return dto;
	}

	/** Ruolo "light": solo campi base, senza lista associazioni. */
	public static RuoloDto toRuoloDtoLight(Ruolo r) {
		if (r == null)
			return null;
		RuoloDto dto = new RuoloDto();
		dto.setId(r.getId());
		dto.setNome(r.getNome());
		dto.setAlias(r.getAlias());
		return dto;
	}

	/**
	 * Ruolo "withAssoc": include la lista di associazioni ruolo-permesso.
	 * Ogni associazione è mappata in modo "safe" (ruolo/permesso light) per evitare
	 * ricorsioni.
	 */
	public static RuoloDto toRuoloDtoWithAssoc(Ruolo r) {
		if (r == null)
			return null;
		RuoloDto dto = toRuoloDtoLight(r);
		if (r.getRuoloPermessi() != null) {
			dto.setRuoloPermessi(
					r.getRuoloPermessi().stream()
							.map(DtoMapper::toRuoloPermessoDtoSafe) // safe: nested light
							.collect(Collectors.toList()));
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
		if (rp == null)
			return null;

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
		if (u == null)
			return null;
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

		return dto;
	}

	public static UtenteDto toUtenteDtoLight(Utente u) { // senza ruolo
		if (u == null)
			return null;
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
		return dto;
	}

	public static Utente toUtente(UtenteDto dto) {
		if (dto == null)
			return null;
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
		return u;
	}

	public static Utente toUtenteLight(UtenteDto dto) { // senza ruolo
		if (dto == null)
			return null;
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

		return u;
	}

	private static Ruolo toRuoloLight(RuoloDto dto) {
		if (dto == null)
			return null;
		Ruolo r = new Ruolo();
		r.setId(dto.getId());
		r.setNome(dto.getNome());
		r.setAlias(dto.getAlias());
		return r;
	}

	// mapper per l'entità Cliente
	// mapper cliente completo
	public static ClienteDto toClienteDto(Cliente c) {
		if (c == null)
			return null;

		ClienteDto dto = new ClienteDto();
		dto.setId(c.getId());
		dto.setSesso(c.getSesso());
		dto.setAltezza(c.getAltezza());
		dto.setAssunzioneFarmaci(c.getAssunzioneFarmaci());
		dto.setTelefono(c.getTelefono());
		dto.setBeveAlcol(c.getBeveAlcol());
		dto.setFuma(c.getFuma());
		dto.setCodiceFiscale(c.getCodiceFiscale());
		dto.setCognome(c.getCognome());
		dto.setEmail(c.getEmail());
		dto.setDataNascita(c.getDataNascita());
		dto.setProblematicheSalutari(c.getProblematicheSalutari());
		dto.setFunzioniIntestinali(c.getFunzioniIntestinali());
		dto.setIntolleranze(c.getIntolleranze());
		dto.setNome(c.getNome());
		dto.setLivelloDiAttivita(c.getLivelloDiAttivita());
		dto.setNutrizionista(toUtenteDto(c.getNutrizionista()));
		dto.setPeso(c.getPeso());
		dto.setPesoTarget(c.getPesoTarget());
		dto.setAltezzaTarget(c.getAltezzaTarget());
		dto.setQuantitaEQualitaDelSonno(c.getQuantitaEQualitaDelSonno());

		if (c.getMisurazioni() != null) {
			dto.setMisurazioni(c.getMisurazioni().stream()
					.map(DtoMapper::toMisurazioneDtoLight)
					.collect(Collectors.toList()));
		} else {
			dto.setMisurazioni(new ArrayList<>());
		}
		if (c.getPlicometrie() != null) {
			dto.setPlicometrie(c.getPlicometrie().stream()
					.map(DtoMapper::toPlicometriaDtoLight)
					.collect(Collectors.toList()));
		} else {
			dto.setPlicometrie(new ArrayList<>());
		}

		// ── Tags clinici MDSS (EAGER — già in RAM, zero query extra) ──
		dto.setTagStandard(c.getTagStandard() != null
				? new HashSet<>(c.getTagStandard())
				: new HashSet<>());

		// Blacklist personale (LAZY — esporre solo se già inizializzato nella transazione)
		if (c.getBlacklistManuale() != null) {
			dto.setBlacklistManuale(
					c.getBlacklistManuale().stream()
							.map(DtoMapper::toAvversionePersonaleDto)
							.collect(Collectors.toSet())
			);
		}

		return dto;
	}

	public static Cliente toCliente(ClienteFormDto form) {
		if (form == null)
			return null;

		Cliente c = new Cliente();
		c.setSesso(form.getSesso());
		c.setNome(form.getNome());
		c.setCognome(form.getCognome());
		c.setCodiceFiscale(form.getCodiceFiscale());
		c.setEmail(form.getEmail());
		c.setTelefono(form.getTelefono());
		c.setDataNascita(form.getDataNascita());
		c.setPeso(form.getPeso());
		c.setAltezza(form.getAltezza());
		c.setPesoTarget(form.getPesoTarget());
		c.setAltezzaTarget(form.getAltezzaTarget());
		c.setLivelloDiAttivita(form.getLivelloDiAttivita());
		c.setIntolleranze(form.getIntolleranze());
		c.setFunzioniIntestinali(form.getFunzioniIntestinali());
		c.setProblematicheSalutari(form.getProblematicheSalutari());
		c.setQuantitaEQualitaDelSonno(form.getQuantitaEQualitaDelSonno());
		c.setAssunzioneFarmaci(form.getAssunzioneFarmaci());
		c.setBeveAlcol(form.getBeveAlcol() != null ? form.getBeveAlcol() : false);
		c.setFuma(form.getFuma() != null ? form.getFuma() : false);

		// ── Tags clinici MDSS ──
		if (form.getTagStandard() != null) {
			c.setTagStandard(new HashSet<>(form.getTagStandard()));
		}

		return c;
	}

	public static void updateClienteFromForm(Cliente c, ClienteFormDto form) {
		if (c == null || form == null)
			return;

		c.setSesso(form.getSesso());
		c.setNome(form.getNome() != null ? form.getNome() : "");
		c.setCognome(form.getCognome() != null ? form.getCognome() : "");
		c.setCodiceFiscale(form.getCodiceFiscale() != null ? form.getCodiceFiscale() : c.getCodiceFiscale());
		c.setEmail(form.getEmail() != null ? form.getEmail() : c.getEmail());
		c.setTelefono(form.getTelefono());
		c.setDataNascita(form.getDataNascita());
		c.setPeso(form.getPeso());
		c.setAltezza(form.getAltezza());
		c.setPesoTarget(form.getPesoTarget());
		c.setAltezzaTarget(form.getAltezzaTarget());
		c.setLivelloDiAttivita(form.getLivelloDiAttivita());
		c.setIntolleranze(form.getIntolleranze() != null ? form.getIntolleranze() : "");
		c.setFunzioniIntestinali(form.getFunzioniIntestinali() != null ? form.getFunzioniIntestinali() : "");
		c.setProblematicheSalutari(form.getProblematicheSalutari() != null ? form.getProblematicheSalutari() : "");
		c.setQuantitaEQualitaDelSonno(
				form.getQuantitaEQualitaDelSonno() != null ? form.getQuantitaEQualitaDelSonno() : "");
		c.setAssunzioneFarmaci(form.getAssunzioneFarmaci() != null ? form.getAssunzioneFarmaci() : "");
		c.setBeveAlcol(form.getBeveAlcol() != null ? form.getBeveAlcol() : false);
		c.setFuma(form.getFuma() != null ? form.getFuma() : false);

		// ── Tags clinici MDSS: overwrite completo (clean & merge) ──
		if (form.getTagStandard() != null) {
			c.getTagStandard().clear();
			c.getTagStandard().addAll(form.getTagStandard());
		}
	}

	// mapper cliente con solo le cose essenziali, vedete se aggiungere info
	public static ClienteDto toClienteDtoLight(Cliente c) {
		if (c == null) {
			return null;
		}
		ClienteDto dto = new ClienteDto();
		dto.setId(c.getId());
		dto.setNome(c.getNome());
		dto.setCognome(c.getCognome());
		dto.setDataNascita(c.getDataNascita());
		dto.setSesso(c.getSesso());
		dto.setEmail(c.getEmail());
		return dto;
	}

	// ── AvversionePersonale (Record piatto — Fase 3) ──────────────────────

	/**
	 * Mappa AvversionePersonale → AvversionePersonaleDto (struttura piatta).
	 * Usa alimentoId + alimentoNome per minimizzare il payload JSON.
	 * Il frontend Angular necessita solo del nome per badge di visualizzazione.
	 */
	public static AvversionePersonaleDto toAvversionePersonaleDto(AvversionePersonale ap) {
		if (ap == null) return null;
		return new AvversionePersonaleDto(
				ap.getId(),
				ap.getAlimento() != null ? ap.getAlimento().getId() : null,
				ap.getAlimento() != null ? ap.getAlimento().getNome() : null,
				ap.getGravita(),
				ap.getNote()
		);
	}

	// ─── ObiettivoNutrizionale ──────────────────────────────────────────

	public static ObiettivoNutrizionaleDto toObiettivoNutrizionaleDto(ObiettivoNutrizionale ob) {
		if (ob == null)
			return null;

		ObiettivoNutrizionaleDto dto = new ObiettivoNutrizionaleDto();
		dto.setId(ob.getId());
		dto.setClienteId(ob.getCliente() != null ? ob.getCliente().getId() : null);
		dto.setObiettivo(ob.getObiettivo());
		dto.setBmr(ob.getBmr());
		dto.setTdee(ob.getTdee());
		dto.setLaf(ob.getLaf());
		dto.setTargetCalorie(ob.getTargetCalorie());
		dto.setTargetProteine(ob.getTargetProteine());
		dto.setTargetCarboidrati(ob.getTargetCarboidrati());
		dto.setTargetGrassi(ob.getTargetGrassi());
		dto.setTargetFibre(ob.getTargetFibre());
		dto.setPctProteine(ob.getPctProteine());
		dto.setPctCarboidrati(ob.getPctCarboidrati());
		dto.setPctGrassi(ob.getPctGrassi());
		dto.setNote(ob.getNote());
		dto.setLockedPctProteine(ob.getLockedPctProteine());
		dto.setLockedPctCarboidrati(ob.getLockedPctCarboidrati());
		dto.setLockedPctGrassi(ob.getLockedPctGrassi());
		dto.setLockedGProteine(ob.getLockedGProteine());
		dto.setLockedGCarboidrati(ob.getLockedGCarboidrati());
		dto.setLockedGGrassi(ob.getLockedGGrassi());
		dto.setAttivo(ob.getAttivo());
		dto.setDataCreazione(ob.getDataCreazione());
		dto.setCreatedAt(ob.getCreatedAt());
		dto.setUpdatedAt(ob.getUpdatedAt());
		return dto;
	}

	// Mapper per l'entita alimentoBase

	public static AlimentoBase toAlimentoBase(AlimentoBaseFormDto dto) {
		if (dto == null)
			return null;

		AlimentoBase a = new AlimentoBase();
		a.setId(dto.getId());
		a.setNome(dto.getNome());
		a.setMisuraInGrammi(dto.getMisuraInGrammi());
		a.setCategoria(dto.getCategoria());
		a.setUrlImmagine(dto.getUrlImmagine());
		a.setTracce(dto.getTracce());

		// Macro
		Macro macro = toMacro(dto.getMacroNutrienti());
		macro.setAlimento(a);
		a.setMacronutrienti(macro);

		// Tag booleani (D5)
		a.setSenzaGlutine(dto.getSenzaGlutine());
		a.setSenzaLattosio(dto.getSenzaLattosio());
		a.setVegano(dto.getVegano());

		// Micronutrienti → gestiti nel Service (find-or-create)

		return a;
	}

	public static void updateAlimentoBaseFromForm(
			AlimentoBase a,
			AlimentoBaseFormDto form) {
		if (a == null || form == null)
			return;

		a.setNome(form.getNome());
		a.setMisuraInGrammi(form.getMisuraInGrammi());
		a.setCategoria(form.getCategoria());
		a.setUrlImmagine(form.getUrlImmagine());
		a.setTracce(form.getTracce());

		// Tag booleani (D5)
		a.setSenzaGlutine(form.getSenzaGlutine());
		a.setSenzaLattosio(form.getSenzaLattosio());
		a.setVegano(form.getVegano());

		// Macro
		Macro macro = toMacro(form.getMacroNutrienti());
		macro.setAlimento(a);
		a.setMacronutrienti(macro);

		// Micronutrienti → gestiti nel Service (find-or-create)
	}


	public static AlimentoBaseDto toAlimentoBaseDto(AlimentoBase a) {
		if (a == null) return null;
		AlimentoBaseDto dto = new AlimentoBaseDto();
		dto.setId(a.getId());
		dto.setNome(a.getNome());
		dto.setMacroNutrienti(toMacroDtoLight(a.getMacronutrienti()));
		dto.setMicronutrienti(toValoreMicroDto(a.getMicronutrienti()));
		dto.setMisuraInGrammi(a.getMisuraInGrammi());
		dto.setCategoria(a.getCategoria());
		dto.setUrlImmagine(a.getUrlImmagine());
		// Materializziamo la collection per evitare LazyInitializationException durante la serializzazione JSON
		dto.setTracce(a.getTracce() != null ? new java.util.HashSet<>(a.getTracce()) : null);
		// Tag booleani (D5)
		dto.setSenzaGlutine(a.getSenzaGlutine());
		dto.setSenzaLattosio(a.getSenzaLattosio());
		dto.setVegano(a.getVegano());
		return dto;
	}

	public static AlimentoBaseDto toAlimentoBaseDtoMacro(AlimentoBase a) {
		if (a == null) {
			return null;
		}
		AlimentoBaseDto dto = new AlimentoBaseDto();
		dto.setId(a.getId());
		dto.setNome(a.getNome());
		dto.setMacroNutrienti(toMacroDtoLight(a.getMacronutrienti()));
		dto.setMisuraInGrammi(a.getMisuraInGrammi());
		dto.setCategoria(a.getCategoria());
		return dto;
	}

	public static AlimentoBaseDto toAlimentoBaseDtoLight(AlimentoBase a) {
		if (a == null) return null;
		AlimentoBaseDto dto = new AlimentoBaseDto();
		dto.setId(a.getId());
		dto.setNome(a.getNome());
		dto.setMisuraInGrammi(a.getMisuraInGrammi());
		dto.setMacroNutrienti(toMacroDtoLight(a.getMacronutrienti()));
		dto.setCategoria(a.getCategoria());
		// Materializziamo la collection per evitare LazyInitializationException durante la serializzazione JSON
		dto.setTracce(a.getTracce() != null ? new java.util.HashSet<>(a.getTracce()) : null);
		dto.setPersonale(a.getCreatedBy() != null);
		// Tag booleani (D5)
		dto.setSenzaGlutine(a.getSenzaGlutine());
		dto.setSenzaLattosio(a.getSenzaLattosio());
		dto.setVegano(a.getVegano());
		return dto;
	}

	// Mapper Minimal: Solo info base, NIENTE macro/micro per evitare loop
	public static AlimentoBaseDto toAlimentoBaseDtoMinimal(AlimentoBase a) {
		if (a == null) {
			return null;
		}
		AlimentoBaseDto dto = new AlimentoBaseDto();
		dto.setId(a.getId());
		dto.setNome(a.getNome());
		dto.setMisuraInGrammi(a.getMisuraInGrammi());
		dto.setCategoria(a.getCategoria());
		// NON settiamo Macro né Micro qui!
		return dto;
	}


	// Mapper per l'entità Macro

	public static MacroDto toMacroDto(Macro m) {
		if (m == null) {
			return null;
		}
		MacroDto dto = new MacroDto();
		dto.setId(m.getId());
		dto.setAlimento(toAlimentoBaseDtoMinimal(m.getAlimento()));
		dto.setCalorie(m.getCalorie());
		dto.setGrassi(m.getGrassi());
		dto.setProteine(m.getProteine());
		dto.setCarboidrati(m.getCarboidrati());
		dto.setFibre(m.getFibre());
		dto.setZuccheri(m.getZuccheri());
		dto.setGrassiSaturi(m.getGrassiSaturi());
		dto.setSodio(m.getSodio());
		dto.setAlcol(m.getAlcol());
		dto.setAcqua(m.getAcqua());
		dto.setSale(m.getSale());
		return dto;
	}

	public static MacroDto toMacroDtoLight(Macro m) {
		if (m == null) {
			return null;
		}
		MacroDto dto = new MacroDto();
		dto.setId(m.getId());
		dto.setCalorie(m.getCalorie());
		dto.setGrassi(m.getGrassi());
		dto.setProteine(m.getProteine());
		dto.setCarboidrati(m.getCarboidrati());
		dto.setFibre(m.getFibre());
		dto.setZuccheri(m.getZuccheri());
		dto.setGrassiSaturi(m.getGrassiSaturi());
		dto.setSodio(m.getSodio());
		dto.setAlcol(m.getAlcol());
		dto.setAcqua(m.getAcqua());
		dto.setSale(m.getSale());
		return dto;
	}

	public static Macro toMacro(MacroDto dto) {
		if (dto == null) {
			return null;
		}

		Macro m = new Macro();
		m.setId(dto.getId());
		m.setCalorie(dto.getCalorie());
		m.setGrassi(dto.getGrassi());
		m.setProteine(dto.getProteine());
		m.setCarboidrati(dto.getCarboidrati());
		m.setFibre(dto.getFibre());
		m.setZuccheri(dto.getZuccheri());
		m.setGrassiSaturi(dto.getGrassiSaturi());
		m.setSodio(dto.getSodio());
		m.setAlcol(dto.getAlcol());
		m.setAcqua(dto.getAcqua());

		return m;
	}

	// Mapper per l'entità Micro DA FINIRE

	public static MicroDto toMicroDto(Micro m) {
		if (m == null)
			return null;
		MicroDto dto = new MicroDto();
		dto.setId(m.getId());
		dto.setNome(m.getNome());
		dto.setUnita(m.getUnita());
		dto.setCategoria(m.getCategoria());
		return dto;
	}

	public static Micro toMicro(MicroDto dto) {
		if (dto == null) {
			return null;
		}
		Micro m = new Micro();
		m.setId(dto.getId());
		m.setNome(dto.getNome());
		m.setUnita(dto.getUnita());
		m.setCategoria(dto.getCategoria());
		return m;
	}

	// ValoreMicro
	public static List<ValoreMicroDto> toValoreMicroDto(Set<ValoreMicro> valori) {
		return valori.stream()
				.map(vm -> {
					ValoreMicroDto dto = new ValoreMicroDto();
					dto.setValore(vm.getValore());
					dto.setMicronutriente(toMicroDto(vm.getMicronutriente()));
					return dto;
				})
				.toList();
	}
	// Mapper per l'entita Pasto

	public static PastoDto toPastoDto(Pasto p) {
		if (p == null) {
			return null;
		}
		PastoDto dto = new PastoDto();
		dto.setId(p.getId());
		dto.setNome(p.getNome());
		dto.setDefaultCode(p.getDefaultCode());
		dto.setDescrizione(p.getDescrizione());
		dto.setOrdineVisualizzazione(p.getOrdineVisualizzazione());
		dto.setEliminabile(p.getEliminabile());
		dto.setScheda(toSchedaDto(p.getScheda()));
		dto.setOrarioFine(p.getOrarioFine());
		dto.setOrarioInizio(p.getOrarioInizio());
		dto.setGiorno(p.getGiorno() != null ? p.getGiorno().name() : null);
		return dto;
	}

	public static PastoDto toPastoDtoLight(Pasto p) { // senza la Scheda
		if (p == null) {
			return null;
		}
		PastoDto dto = new PastoDto();
		dto.setId(p.getId());
		dto.setNome(p.getNome());
		dto.setDefaultCode(p.getDefaultCode());
		dto.setDescrizione(p.getDescrizione());
		dto.setOrdineVisualizzazione(p.getOrdineVisualizzazione());
		dto.setEliminabile(p.getEliminabile());
		dto.setOrarioFine(p.getOrarioFine());
		dto.setOrarioInizio(p.getOrarioInizio());
		dto.setGiorno(p.getGiorno() != null ? p.getGiorno().name() : null);
		return dto;
	}

	public static PastoDto toPastoDtoWithAssoc(Pasto p) {
		if (p == null)
			return null;
		PastoDto dto = toPastoDtoLight(p);
		if (p.getAlimentiPasto() != null) {
			dto.setAlimentiPasto(
					p.getAlimentiPasto().stream()
							.map(DtoMapper::toAlimentoPastoDtoChild) // safe: nested light
							.collect(Collectors.toList()));
		}
		return dto;
	}

	// Mapper AlimentoPasto

	// Mapper AlimentoPasto

	// Usa questo quando mostri l'alimento dentro la lista del pasto
	public static AlimentoPastoDto toAlimentoPastoDtoChild(AlimentoPasto ap) {
		if (ap == null)
			return null;

		AlimentoPastoDto dto = new AlimentoPastoDto();
		dto.setId(ap.getId());
		dto.setQuantita(ap.getQuantita());

		// Mappiamo l'alimento (fondamentale)
		AlimentoBase alim = ap.getAlimento();
		if (alim != null) {
			dto.setAlimento(toAlimentoBaseDtoMacro(alim));
		}

		String nomeCustom = ap.getNomeOverride() != null ? ap.getNomeOverride().getNomeCustom() : null;
		dto.setNomeCustom(nomeCustom);
		dto.setNomeVisualizzato(nomeCustom != null ? nomeCustom : (alim != null ? alim.getNome() : null));

		// Mappo le alternative (già caricate dal JOIN FETCH)
		if (ap.getAlternative() != null && !ap.getAlternative().isEmpty()) {
			dto.setAlternative(
					ap.getAlternative().stream()
							.map(DtoMapper::toAlimentoAlternativoDtoLight)
							.collect(Collectors.toList()));
		} else {
			dto.setAlternative(new ArrayList<>());
		}

		// IMPORTANTE: NON mappiamo dto.setPasto(...) qui!
		// Evitiamo la ridondanza perché siamo già dentro l'oggetto Pasto.

		return dto;
	}

	// Mantieni il "Safe" o "Full" solo se ti serve mappare un AlimentoPasto preso
	// singolarmente
	public static AlimentoPastoDto toAlimentoPastoDtoFull(AlimentoPasto ap) {
		AlimentoPastoDto dto = toAlimentoPastoDtoChild(ap); // Riutilizza la logica base
		if (ap != null && ap.getPasto() != null) {
			dto.setPasto(toPastoDtoLight(ap.getPasto())); // Qui aggiungi il padre se serve
		}
		return dto;
	}

	// Mapper per l'entità scheda

	public static SchedaDto toSchedaDtoLight(Scheda s) {
		if (s == null) {
			return null;
		}
		SchedaDto dto = new SchedaDto();
		dto.setId(s.getId());
		dto.setAttiva(s.getAttiva());
		dto.setCliente(toClienteDtoLight(s.getCliente()));
		dto.setDataCreazione(s.getDataCreazione());
		dto.setNome(s.getNome());
		dto.setTipo(s.getTipo() != null ? s.getTipo().name() : "GIORNALIERA");
		return dto;
	}

	public static void updateSchedaFromForm(Scheda s, SchedaFormDto form) {
		if (s == null || form == null)
			return;

		s.setAttiva(form.getAttiva());
		s.setNome(form.getNome());

		// Aggiorna tipo solo se il form lo specifica
		if (form.getTipo() != null) {
			try {
				s.setTipo(it.nutrizionista.restnutrizionista.entity.TipoScheda.valueOf(form.getTipo()));
			} catch (IllegalArgumentException e) {
				// Ignora valori non validi, mantieni quello esistente
			}
		}
	}

	public static SchedaDto toSchedaDto(Scheda s) {
		if (s == null) {
			return null;
		}

		SchedaDto dto = new SchedaDto();
		dto.setId(s.getId());
		dto.setAttiva(s.getAttiva());
		dto.setCliente(toClienteDtoLight(s.getCliente()));
		dto.setDataCreazione(s.getDataCreazione());
		dto.setNome(s.getNome());
		dto.setTipo(s.getTipo() != null ? s.getTipo().name() : "GIORNALIERA");
		dto.setPasti(
				s.getPasti().stream()
						.map(DtoMapper::toPastoDtoWithAssoc)
						.collect(Collectors.toList()));
		return dto;
	}

	public static SchedaDto toSchedaDtoForSave(Scheda s) {
		if (s == null) {
			return null;
		}
		SchedaDto dto = new SchedaDto();
		dto.setAttiva(s.getAttiva());
		return dto;
	}

	public static SchedaDto toSchedaDtoLista(Scheda s) {
		if (s == null) {
			return null;

		}

		SchedaDto dto = new SchedaDto();
		dto.setId(s.getId());
		dto.setAttiva(s.getAttiva());
		dto.setCliente(toClienteDtoLight(s.getCliente()));
		dto.setDataCreazione(s.getDataCreazione());
		dto.setNome(s.getNome());

		// 1. Passiamo il conteggio calcolato dal database
		dto.setNumeroPasti(s.getNumeroPasti());
		dto.setTipo(s.getTipo() != null ? s.getTipo().name() : "GIORNALIERA");

		return dto;
	}

	// mapper per l' entità misurazioneAntrometrica

	public static MisurazioneAntropometricaDto toMisurazioneDto(MisurazioneAntropometrica m) {
		if (m == null) {
			return null;
		}
		MisurazioneAntropometricaDto dto = new MisurazioneAntropometricaDto();
		dto.setId(m.getId());
		dto.setBicipiteD(m.getBicipiteD());
		dto.setBicipiteS(m.getBicipiteS());
		dto.setCliente(toClienteDtoLight(m.getCliente()));
		dto.setDataMisurazione(m.getDataMisurazione());
		dto.setPeso(m.getPeso());
		dto.setFianchi(m.getFianchi());
		dto.setGambaD(m.getGambaD());
		dto.setGambaS(m.getGambaS());
		dto.setSpalle(m.getSpalle());
		dto.setTorace(m.getTorace());
		dto.setVita(m.getVita());
		return dto;
	}

	public static MisurazioneAntropometricaDto toMisurazioneDtoLight(MisurazioneAntropometrica m) { // senza cliente
		if (m == null) {
			return null;
		}
		MisurazioneAntropometricaDto dto = new MisurazioneAntropometricaDto();
		dto.setId(m.getId());
		dto.setBicipiteD(m.getBicipiteD());
		dto.setBicipiteS(m.getBicipiteS());
		dto.setDataMisurazione(m.getDataMisurazione());
		dto.setPeso(m.getPeso());
		dto.setFianchi(m.getFianchi());
		dto.setGambaD(m.getGambaD());
		dto.setGambaS(m.getGambaS());
		dto.setSpalle(m.getSpalle());
		dto.setTorace(m.getTorace());
		dto.setVita(m.getVita());
		return dto;
	}

	public static MisurazioneAntropometrica toMisurazione(MisurazioneAntropometricaFormDto form) {
		if (form == null)
			return null;

		MisurazioneAntropometrica m = new MisurazioneAntropometrica();
		m.setId(form.getId());
		m.setBicipiteD(form.getBicipiteD());
		m.setBicipiteS(form.getBicipiteS());
		m.setDataMisurazione(form.getDataMisurazione());
		m.setPeso(form.getPeso());
		m.setFianchi(form.getFianchi());
		m.setGambaD(form.getGambaD());
		m.setGambaS(form.getGambaS());
		m.setSpalle(form.getSpalle());
		m.setTorace(form.getTorace());
		m.setVita(form.getVita());

		return m;
	}

	public static void updateMisurazioneFromForm(
			MisurazioneAntropometrica m,
			MisurazioneAntropometricaFormDto form) {

		if (m == null || form == null)
			return;

		m.setBicipiteD(form.getBicipiteD());
		m.setBicipiteS(form.getBicipiteS());
		m.setDataMisurazione(form.getDataMisurazione());
		m.setPeso(form.getPeso());
		m.setFianchi(form.getFianchi());
		m.setGambaD(form.getGambaD());
		m.setGambaS(form.getGambaS());
		m.setSpalle(form.getSpalle());
		m.setTorace(form.getTorace());
		m.setVita(form.getVita());
	}

	// mapper per le plicometrie

	// --- MAPPER PLICOMETRIA ---

	public static PlicometriaDto toPlicometriaDto(Plicometria p) {
		if (p == null) {
			return null;
		}
		PlicometriaDto dto = new PlicometriaDto();
		dto.setId(p.getId());
		dto.setCliente(toClienteDtoLight(p.getCliente())); // Include il cliente light
		dto.setDataMisurazione(p.getDataMisurazione());
		dto.setMetodo(p.getMetodo());

		// Pliche
		dto.setTricipite(p.getTricipite());
		dto.setBicipite(p.getBicipite());
		dto.setSottoscapolare(p.getSottoscapolare());
		dto.setSovrailiaca(p.getSovrailiaca());
		dto.setAddominale(p.getAddominale());
		dto.setCoscia(p.getCoscia());
		dto.setPettorale(p.getPettorale());
		dto.setAscellare(p.getAscellare());
		dto.setPolpaccio(p.getPolpaccio());

		// Risultati e Note
		dto.setPesoKgRiferimento(p.getPesoKgRiferimento());
		dto.setSommaPliche(p.getSommaPliche());
		dto.setDensitaCorporea(p.getDensitaCorporea());
		dto.setPercentualeMassaGrassa(p.getPercentualeMassaGrassa());
		dto.setMassaGrassaKg(p.getMassaGrassaKg());
		dto.setMassaMagraKg(p.getMassaMagraKg());
		dto.setNote(p.getNote());

		return dto;
	}

	public static PlicometriaDto toPlicometriaDtoLight(Plicometria p) { // Senza cliente (utile per le liste dentro
																		// ClienteDto)
		if (p == null) {
			return null;
		}
		PlicometriaDto dto = new PlicometriaDto();
		dto.setId(p.getId());
		// dto.setCliente(...) -> ESCLUSO
		dto.setDataMisurazione(p.getDataMisurazione());
		dto.setMetodo(p.getMetodo());

		// Pliche
		dto.setTricipite(p.getTricipite());
		dto.setBicipite(p.getBicipite());
		dto.setSottoscapolare(p.getSottoscapolare());
		dto.setSovrailiaca(p.getSovrailiaca());
		dto.setAddominale(p.getAddominale());
		dto.setCoscia(p.getCoscia());
		dto.setPettorale(p.getPettorale());
		dto.setAscellare(p.getAscellare());
		dto.setPolpaccio(p.getPolpaccio());

		// Risultati e Note
		dto.setPercentualeMassaGrassa(p.getPercentualeMassaGrassa());
		dto.setMassaGrassaKg(p.getMassaGrassaKg());
		dto.setMassaMagraKg(p.getMassaMagraKg());
		dto.setNote(p.getNote());

		return dto;
	}

	public static Plicometria toPlicometria(PlicometriaFormDto form) {
		if (form == null)
			return null;

		Plicometria p = new Plicometria();
		p.setId(form.getId());
		p.setDataMisurazione(form.getDataMisurazione());
		p.setMetodo(form.getMetodo());

		// Pliche
		p.setTricipite(form.getTricipite());
		p.setBicipite(form.getBicipite());
		p.setSottoscapolare(form.getSottoscapolare());
		p.setSovrailiaca(form.getSovrailiaca());
		p.setAddominale(form.getAddominale());
		p.setCoscia(form.getCoscia());
		p.setPettorale(form.getPettorale());
		p.setAscellare(form.getAscellare());
		p.setPolpaccio(form.getPolpaccio());

		p.setNote(form.getNote());

		// La percentuale di massa grassa viene calcolata solitamente nel Service,
		// ma se arriva dal form (es. calcolata dal frontend), puoi settarla qui:
		// p.setPercentualeMassaGrassa(form.getPercentualeMassaGrassa());

		return p;
	}

	public static void updatePlicometriaFromForm(Plicometria p, PlicometriaFormDto form) {
		if (p == null || form == null)
			return;

		p.setDataMisurazione(form.getDataMisurazione());
		p.setMetodo(form.getMetodo());

		// Aggiorna Pliche
		p.setTricipite(form.getTricipite());
		p.setBicipite(form.getBicipite());
		p.setSottoscapolare(form.getSottoscapolare());
		p.setSovrailiaca(form.getSovrailiaca());
		p.setAddominale(form.getAddominale());
		p.setCoscia(form.getCoscia());
		p.setPettorale(form.getPettorale());
		p.setAscellare(form.getAscellare());
		p.setPolpaccio(form.getPolpaccio());

		p.setNote(form.getNote());
	}

	// ==========================================
		// MAPPER PER APPUNTAMENTO 
		// ==========================================

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
				dto.setClienteNome(appuntamento.getClienteNome());
				dto.setClienteCognome(appuntamento.getClienteCognome());
				dto.setClienteRegistrato(false);
			}

			dto.setDescrizioneAppuntamento(appuntamento.getDescrizioneAppuntamento());
			
			// Inizio Appuntamento
			dto.setData(appuntamento.getData());
			dto.setOra(appuntamento.getOra());

			// Fine Appuntamento e Modificatori (Novità per Angular Calendar)
			dto.setEndData(appuntamento.getEndData());
			dto.setEndOra(appuntamento.getEndOra());
			dto.setAllDay(appuntamento.isAllDay());
			dto.setTimezone(appuntamento.getTimezone());

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
				appuntamento.setClienteNome(formDTO.getClienteNome());
				appuntamento.setClienteCognome(formDTO.getClienteCognome());
			}

			appuntamento.setDescrizioneAppuntamento(formDTO.getDescrizioneAppuntamento());
			
			// Inizio Appuntamento
			appuntamento.setData(formDTO.getData());
			appuntamento.setOra(formDTO.getOra());

			// Fine Appuntamento e Modificatori (Novità per Angular Calendar)
			appuntamento.setEndData(formDTO.getEndData());
			appuntamento.setEndOra(formDTO.getEndOra());
			appuntamento.setAllDay(formDTO.isAllDay());
			appuntamento.setTimezone(formDTO.getTimezone());

			appuntamento.setModalita(formDTO.getModalita());
			appuntamento.setStato(formDTO.getStato() != null ? formDTO.getStato() : Appuntamento.StatoAppuntamento.PRENOTATO);
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

		public static void updateAppuntamentoFromFormDto(Appuntamento appuntamento, AppuntamentoFormDto formDto) {

			// Aggiorna data e ora inizio
			if (formDto.getData() != null) {
				appuntamento.setData(formDto.getData());
			}
			if (formDto.getOra() != null) {
				appuntamento.setOra(formDto.getOra());
			}

			// Aggiorna data e ora fine (Novità per Angular Calendar)
			if (formDto.getEndData() != null) {
				appuntamento.setEndData(formDto.getEndData());
			}
			if (formDto.getEndOra() != null) {
				appuntamento.setEndOra(formDto.getEndOra());
			}

			// Aggiorna timezone e flag intera giornata
			appuntamento.setAllDay(formDto.isAllDay());
			if (formDto.getTimezone() != null) {
				appuntamento.setTimezone(formDto.getTimezone());
			}

			// Aggiorna descrizione appuntamento
			if (formDto.getDescrizioneAppuntamento() != null) {
				appuntamento.setDescrizioneAppuntamento(formDto.getDescrizioneAppuntamento());
			}

			// Aggiorna modalità (ONLINE/IN_PRESENZA)
			if (formDto.getModalita() != null) {
				appuntamento.setModalita(formDto.getModalita());
			}

			// Aggiorna stato (PROGRAMMATO/CONFERMATO/ANNULLATO)
			if (formDto.getStato() != null) {
				appuntamento.setStato(formDto.getStato());
			}

			// Aggiorna luogo
			if (formDto.getLuogo() != null) {
				appuntamento.setLuogo(formDto.getLuogo());
			}

			// Aggiorna email cliente
			if (formDto.getEmailCliente() != null) {
				appuntamento.setEmailCliente(formDto.getEmailCliente());
			}

			// Aggiorna dati cliente temporaneo (se non è un cliente registrato)
			if (formDto.getClienteId() == null) {
				appuntamento.setClienteNome(formDto.getClienteNome());
				appuntamento.setClienteCognome(formDto.getClienteCognome());
			}
		}

	// ===================== ALIMENTO PASTO =====================

	/**
	 * Mappa AlimentoPasto -> AlimentoPastoDto in modo "safe" (senza Pasto nested
	 * per evitare loop)
	 */
	public static AlimentoPastoDto toAlimentoPastoDtoSafe(AlimentoPasto ap) {
		if (ap == null)
			return null;

		AlimentoPastoDto dto = new AlimentoPastoDto();
		dto.setId(ap.getId());
		dto.setAlimento(toAlimentoBaseDtoLight(ap.getAlimento()));
		dto.setQuantita(ap.getQuantita());
		// NON settiamo il Pasto per evitare riferimenti circolari
		dto.setCreatedAt(ap.getCreatedAt());
		dto.setUpdatedAt(ap.getUpdatedAt());
		return dto;
	}

	// ===================== ALIMENTO ALTERNATIVO =====================

	/**
	 * Mappa AlimentoAlternativo -> AlimentoAlternativoDto
	 * Include alimentoPasto (light senza scheda) e alimentoAlternativo (light)
	 */
	public static AlimentoAlternativoDto toAlimentoAlternativoDto(AlimentoAlternativo aa) {
		if (aa == null)
			return null;

		AlimentoAlternativoDto dto = new AlimentoAlternativoDto();
		dto.setId(aa.getId());
		dto.setAlimentoPasto(toAlimentoPastoDtoSafe(aa.getAlimentoPasto()));
		dto.setPastoId(aa.getPasto() != null ? aa.getPasto().getId() : null);
		dto.setAlimentoAlternativo(toAlimentoBaseDtoLight(aa.getAlimentoAlternativo()));
		dto.setQuantita(aa.getQuantita());
		dto.setPriorita(aa.getPriorita());
		dto.setMode(aa.getMode());
		dto.setManual(aa.getManual());
		dto.setNote(aa.getNote());
		dto.setNomeCustom(aa.getNomeCustom());
		String nomeAlt = aa.getAlimentoAlternativo() != null ? aa.getAlimentoAlternativo().getNome() : null;
		dto.setNomeVisualizzato(aa.getNomeCustom() != null ? aa.getNomeCustom() : nomeAlt);
		dto.setCreatedAt(aa.getCreatedAt());
		dto.setUpdatedAt(aa.getUpdatedAt());
		return dto;
	}

	/**
	 * Versione light senza alimentoPasto nested (solo alimento alternativo)
	 */
	public static AlimentoAlternativoDto toAlimentoAlternativoDtoLight(AlimentoAlternativo aa) {
		if (aa == null)
			return null;

		AlimentoAlternativoDto dto = new AlimentoAlternativoDto();
		dto.setId(aa.getId());
		dto.setAlimentoAlternativo(toAlimentoBaseDtoLight(aa.getAlimentoAlternativo()));
		dto.setQuantita(aa.getQuantita());
		dto.setPriorita(aa.getPriorita());
		dto.setMode(aa.getMode());
		dto.setManual(aa.getManual());
		dto.setNote(aa.getNote());
		dto.setNomeCustom(aa.getNomeCustom());
		String nomeAlt = aa.getAlimentoAlternativo() != null ? aa.getAlimentoAlternativo().getNome() : null;
		dto.setNomeVisualizzato(aa.getNomeCustom() != null ? aa.getNomeCustom() : nomeAlt);
		return dto;
	}
	
	// ==========================================
	// MAPPER PER ORARIO STUDIO
	// ==========================================

	public static OrariStudioDto toOrariStudioDto(OrariStudio orari) {
		if (orari == null) {
			return null;
		}

		OrariStudioDto dto = new OrariStudioDto();
		dto.setId(orari.getId());

		if (orari.getNutrizionista() != null) {
			dto.setNutrizionistaId(orari.getNutrizionista().getId());
		}

		dto.setGiornoSettimana(orari.getGiornoSettimana());
		dto.setGiornoLavorativo(orari.isGiornoLavorativo());
		dto.setOraApertura(orari.getOraApertura());
		dto.setOraChiusura(orari.getOraChiusura());
		dto.setInizioPausaPranzo(orari.getInizioPausaPranzo());
		dto.setFinePausaPranzo(orari.getFinePausaPranzo());

		dto.setCreatedAt(orari.getCreatedAt());
		dto.setUpdatedAt(orari.getUpdatedAt());

		return dto;
	}

	public static OrariStudio toOrariStudio(OrariStudioFormDto formDto, Utente nutrizionista) {
		if (formDto == null) {
			return null;
		}

		OrariStudio orari = new OrariStudio();
		orari.setNutrizionista(nutrizionista);
		orari.setGiornoSettimana(formDto.getGiornoSettimana());
		orari.setGiornoLavorativo(formDto.isGiornoLavorativo());
		orari.setOraApertura(formDto.getOraApertura());
		orari.setOraChiusura(formDto.getOraChiusura());
		orari.setInizioPausaPranzo(formDto.getInizioPausaPranzo());
		orari.setFinePausaPranzo(formDto.getFinePausaPranzo());

		return orari;
	}

	public static void updateOrariStudioFromFormDto(OrariStudio orari, OrariStudioFormDto formDto) {
		if (orari == null || formDto == null) {
			return;
		}

		if (formDto.getGiornoSettimana() != null) {
			orari.setGiornoSettimana(formDto.getGiornoSettimana());
		}
		orari.setGiornoLavorativo(formDto.isGiornoLavorativo());

		if (formDto.getOraApertura() != null) {
			orari.setOraApertura(formDto.getOraApertura());
		}
		if (formDto.getOraChiusura() != null) {
			orari.setOraChiusura(formDto.getOraChiusura());
		}

		orari.setInizioPausaPranzo(formDto.getInizioPausaPranzo());
		orari.setFinePausaPranzo(formDto.getFinePausaPranzo());
	}

	public static PastoTemplateDto toPastoTemplateDto(PastoTemplate t) {
		if (t == null)
			return null;
		PastoTemplateDto dto = new PastoTemplateDto();
		dto.setId(t.getId());
		dto.setNome(t.getNome());
		dto.setDescrizione(t.getDescrizione());
		if (t.getAlimenti() != null) {
			dto.setAlimenti(t.getAlimenti().stream()
					.map(DtoMapper::toPastoTemplateItemDto)
					.collect(Collectors.toList()));
		}
		dto.setCreatedAt(t.getCreatedAt());
		dto.setUpdatedAt(t.getUpdatedAt());
		return dto;
	}

	public static PastoTemplateItemDto toPastoTemplateItemDto(PastoTemplateAlimento a) {
		if (a == null)
			return null;
		PastoTemplateItemDto dto = new PastoTemplateItemDto();
		AlimentoBase alim = a.getAlimento();
		if (alim != null) {
			dto.setAlimento(toAlimentoBaseDtoMacro(alim));
		}
		dto.setQuantita(a.getQuantita());
		dto.setNomeCustom(a.getNomeCustom());
		dto.setNomeVisualizzato(a.getNomeCustom() != null && !a.getNomeCustom().isBlank()
				? a.getNomeCustom()
				: (alim != null ? alim.getNome() : null));
		if (a.getAlternative() != null && !a.getAlternative().isEmpty()) {
			dto.setAlternative(a.getAlternative().stream()
					.map(DtoMapper::toPastoTemplateAlternativaDto)
					.collect(Collectors.toList()));
		} else {
			dto.setAlternative(new ArrayList<>());
		}
		return dto;
	}

	public static PastoTemplateAlternativaDto toPastoTemplateAlternativaDto(PastoTemplateAlternativo a) {
		if (a == null)
			return null;
		PastoTemplateAlternativaDto dto = new PastoTemplateAlternativaDto();
		dto.setId(a.getId());
		if (a.getAlimentoAlternativo() != null) {
			dto.setAlimentoAlternativo(toAlimentoBaseDtoMacro(a.getAlimentoAlternativo()));
		}
		dto.setQuantita(a.getQuantita());
		dto.setPriorita(a.getPriorita());
		dto.setMode(a.getMode());
		dto.setManual(a.getManual());
		dto.setNote(a.getNote());
		dto.setNomeCustom(a.getNomeCustom());
		String base = a.getAlimentoAlternativo() != null ? a.getAlimentoAlternativo().getNome() : null;
		dto.setNomeVisualizzato(a.getNomeCustom() != null && !a.getNomeCustom().isBlank() ? a.getNomeCustom() : base);
		return dto;
	}

	// ── Ricette ────────────────────────────────────────────────────────────────

	public static it.nutrizionista.restnutrizionista.dto.RicettaDto toRicettaDto(
			it.nutrizionista.restnutrizionista.entity.Ricetta r) {
		if (r == null)
			return null;
		var dto = new it.nutrizionista.restnutrizionista.dto.RicettaDto();
		dto.setId(r.getId());
		dto.setTitolo(r.getTitolo());
		dto.setDescrizione(r.getDescrizione());
		dto.setCategoria(r.getCategoria());
		dto.setUrlImmagine(r.getUrlImmagine());
		dto.setFonte(r.getFonte());
		dto.setPubblica(r.getPubblica());

		// Mappa ingredienti e calcola macro totali
		double totKcal = 0, totP = 0, totC = 0, totG = 0;
		java.util.List<it.nutrizionista.restnutrizionista.dto.RicettaIngredienteDto> ingDtos = new ArrayList<>();
		if (r.getIngredienti() != null) {
			for (var ing : r.getIngredienti()) {
				ingDtos.add(toRicettaIngredienteDto(ing));
				// Contributo macro proporzionale alla quantita
				var alim = ing.getAlimento();
				var macro = alim != null ? alim.getMacroNutrienti() : null;
				double qty = ing.getQuantita() != null ? ing.getQuantita() : 0;
				double ref = (alim != null && alim.getMisuraInGrammi() != null && alim.getMisuraInGrammi() > 0)
						? alim.getMisuraInGrammi() : 100.0;
				if (macro != null) {
					totKcal += safeValue(macro.getCalorie()) * qty / ref;
					totP    += safeValue(macro.getProteine()) * qty / ref;
					totC    += safeValue(macro.getCarboidrati()) * qty / ref;
					totG    += safeValue(macro.getGrassi()) * qty / ref;
				}
			}
		}
		dto.setIngredienti(ingDtos);
		dto.setMacroTotali(new it.nutrizionista.restnutrizionista.dto.RicettaDto.MacroRicettaDto(
				round1(totKcal), round1(totP), round1(totC), round1(totG)));
		return dto;
	}

	public static it.nutrizionista.restnutrizionista.dto.RicettaIngredienteDto toRicettaIngredienteDto(
			it.nutrizionista.restnutrizionista.entity.RicettaIngrediente ing) {
		if (ing == null)
			return null;
		var dto = new it.nutrizionista.restnutrizionista.dto.RicettaIngredienteDto();
		dto.setId(ing.getId());
		if (ing.getAlimento() != null)
			dto.setAlimento(toAlimentoBaseDtoMacro(ing.getAlimento()));
		dto.setQuantita(ing.getQuantita());
		dto.setNomeCustom(ing.getNomeCustom());
		String base = ing.getAlimento() != null ? ing.getAlimento().getNome() : null;
		dto.setNomeVisualizzato(ing.getNomeCustom() != null && !ing.getNomeCustom().isBlank()
				? ing.getNomeCustom() : base);
		return dto;
	}

	private static double safeValue(Double v) { return v != null ? v : 0.0; }
	private static double round1(double v)    { return Math.round(v * 10.0) / 10.0; }

	// ── SchedaTemplate ──────────────────────────────────────────

	public static SchedaTemplateDto toSchedaTemplateDtoLight(SchedaTemplate st) {
		if (st == null) return null;
		SchedaTemplateDto dto = new SchedaTemplateDto();
		dto.setId(st.getId());
		dto.setNome(st.getNome());
		dto.setDescrizione(st.getDescrizione());
		dto.setTipo(st.getTipo() != null ? st.getTipo().name() : "GIORNALIERA");
		dto.setNumeroPasti(st.getPasti() != null ? st.getPasti().size() : 0);
		dto.setCreatedAt(st.getCreatedAt());
		dto.setUpdatedAt(st.getUpdatedAt());
		return dto;
	}

	public static SchedaTemplateDto toSchedaTemplateDto(SchedaTemplate st) {
		if (st == null) return null;
		SchedaTemplateDto dto = new SchedaTemplateDto();
		dto.setId(st.getId());
		dto.setNome(st.getNome());
		dto.setDescrizione(st.getDescrizione());
		dto.setTipo(st.getTipo() != null ? st.getTipo().name() : "GIORNALIERA");
		dto.setNumeroPasti(st.getPasti() != null ? st.getPasti().size() : 0);
		dto.setCreatedAt(st.getCreatedAt());
		dto.setUpdatedAt(st.getUpdatedAt());
		if (st.getPasti() != null) {
			dto.setPasti(st.getPasti().stream()
					.map(DtoMapper::toPastoSchedaTemplateDto)
					.collect(Collectors.toList()));
		}
		return dto;
	}

	public static PastoSchedaTemplateDto toPastoSchedaTemplateDto(PastoSchedaTemplate p) {
		if (p == null) return null;
		PastoSchedaTemplateDto dto = new PastoSchedaTemplateDto();
		dto.setId(p.getId());
		dto.setNome(p.getNome());
		dto.setDescrizione(p.getDescrizione());
		dto.setGiorno(p.getGiorno() != null ? p.getGiorno().name() : null);
		dto.setOrdineVisualizzazione(p.getOrdineVisualizzazione());
		dto.setOrarioInizio(p.getOrarioInizio() != null ? p.getOrarioInizio().toString() : null);
		dto.setOrarioFine(p.getOrarioFine() != null ? p.getOrarioFine().toString() : null);
		if (p.getAlimenti() != null) {
			dto.setAlimentiPasto(p.getAlimenti().stream()
					.map(DtoMapper::toAlimentoPastoSchedaTemplateDto)
					.collect(Collectors.toList()));
		}
		return dto;
	}

	public static AlimentoPastoSchedaTemplateDto toAlimentoPastoSchedaTemplateDto(AlimentoPastoSchedaTemplate a) {
		if (a == null) return null;
		AlimentoPastoSchedaTemplateDto dto = new AlimentoPastoSchedaTemplateDto();
		dto.setId(a.getId());
		dto.setQuantita(a.getQuantita());
		AlimentoBase alim = a.getAlimento();
		if (alim != null) {
			dto.setAlimento(toAlimentoBaseDtoMacro(alim));
		}
		dto.setNomeCustom(a.getNomeCustom());
		dto.setNomeVisualizzato(a.getNomeCustom() != null && !a.getNomeCustom().isBlank()
				? a.getNomeCustom()
				: (alim != null ? alim.getNome() : null));
		if (a.getAlternative() != null) {
			dto.setAlternative(a.getAlternative().stream()
					.sorted((x, y) -> {
						int px = x.getPriorita() != null ? x.getPriorita() : Integer.MAX_VALUE;
						int py = y.getPriorita() != null ? y.getPriorita() : Integer.MAX_VALUE;
						return Integer.compare(px, py);
					})
					.map(DtoMapper::toAlimentoSchedaTemplateAlternativaDto)
					.collect(Collectors.toList()));
		}
		return dto;
	}

	// ── AlimentoSchedaTemplateAlternativa ────────────────────────

	public static AlimentoSchedaTemplateAlternativaDto toAlimentoSchedaTemplateAlternativaDto(
			AlimentoSchedaTemplateAlternativa alt) {
		if (alt == null) return null;
		AlimentoBase alim = alt.getAlimentoAlternativo();
		String nomeVis = (alt.getNomeCustom() != null && !alt.getNomeCustom().isBlank())
				? alt.getNomeCustom()
				: (alim != null ? alim.getNome() : null);
		return new AlimentoSchedaTemplateAlternativaDto(
				alt.getId(),
				alt.getAlimentoPastoSchedaTemplate() != null
						? alt.getAlimentoPastoSchedaTemplate().getId() : null,
				alim != null ? toAlimentoBaseDtoMacro(alim) : null,
				alt.getQuantita(),
				alt.getPriorita(),
				alt.getMode() != null ? alt.getMode().name() : "CALORIE",
				alt.getManual(),
				alt.getNomeCustom(),
				nomeVis
		);
	}

	/**
	 * Trasforma un valore dell'Enum TagStandard in un SystemTagDto leggibile.
	 * Rimuove il prefisso tecnico (ALL_, PAT_, FISIO_, STILE_) e aggiunge
	 * un suffisso descrittivo per la visualizzazione UI del nutrizionista.
	 * Esempio: ALL_GLUTINE -> "Glutine (Allergia)"
	 */
	public static SystemTagDto toTagDto(TagStandard tag) {
		if (tag == null) return null;

		String id = tag.name();
		String rawLabel = tag.name().replace("_", " ");

		if (id.startsWith("ALL_")) {
			rawLabel = rawLabel.replace("ALL ", "") + " (Allergia)";
		} else if (id.startsWith("PAT_")) {
			rawLabel = rawLabel.replace("PAT ", "") + " (Patologia)";
		} else if (id.startsWith("FISIO_")) {
			rawLabel = rawLabel.replace("FISIO ", "") + " (Stato Fisiologico)";
		} else if (id.startsWith("STILE_")) {
			rawLabel = rawLabel.replace("STILE ", "");
		} else if (id.startsWith("REL_")) {
			rawLabel = rawLabel.replace("REL ", "") + " (Religioso)";
		} else if (id.startsWith("FARM_")) {
			rawLabel = rawLabel.replace("FARM ", "") + " (Farmacologico)";
		} else if (id.startsWith("INT_")) {
			rawLabel = rawLabel.replace("INT ", "") + " (Intolleranza)";
		}

		// Capitalizza solo la prima lettera, resto in minuscolo
		String formattedLabel = rawLabel.substring(0, 1).toUpperCase() + rawLabel.substring(1).toLowerCase();

		// Preserva acronimi clinici noti
		formattedLabel = formattedLabel.replace("Ncgs", "NCGS");

		return new SystemTagDto(id, formattedLabel);
	}

}

