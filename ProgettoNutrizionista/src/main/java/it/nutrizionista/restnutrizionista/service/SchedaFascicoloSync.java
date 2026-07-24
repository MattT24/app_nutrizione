package it.nutrizionista.restnutrizionista.service;

import it.nutrizionista.restnutrizionista.entity.TipoDocumento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tiene allineato il PDF eventualmente già archiviato nel fascicolo con lo stato attuale di una
 * scheda (lo archivia la prima volta se non ancora presente, altrimenti lo rigenera). Il contenuto
 * di una scheda cambia da molti service diversi (pasti, alimenti nei pasti, alternative, template) —
 * questo componente centralizza la chiamata a {@link FascicoloService#sincronizzaDocumento} così
 * ognuno di quei service aggiunge una sola riga invece di duplicare try/catch e logging.
 *
 * <p>Best-effort per design: un fallimento della sincronizzazione (es. pool di rendering PDF
 * momentaneamente saturo) non deve MAI far fallire l'operazione di dominio che l'ha causata
 * (salvare un alimento, un pasto, ecc.) — viene solo loggato.
 */
@Component
public class SchedaFascicoloSync {

    private static final Logger log = LoggerFactory.getLogger(SchedaFascicoloSync.class);

    private final FascicoloService fascicoloService;

    public SchedaFascicoloSync(FascicoloService fascicoloService) {
        this.fascicoloService = fascicoloService;
    }

    public void sincronizza(Long clienteId, Long schedaId) {
        if (clienteId == null || schedaId == null) return;
        try {
            fascicoloService.sincronizzaDocumento(clienteId, TipoDocumento.SCHEDA, schedaId);
        } catch (RuntimeException e) {
            log.warn("Sincronizzazione automatica del fascicolo fallita per scheda {}: {}", schedaId, e.getMessage(), e);
        }
    }

    /** Elimina (se esiste) il documento fascicolo della scheda, quando la scheda stessa viene eliminata. */
    public void elimina(Long clienteId, Long schedaId) {
        if (clienteId == null || schedaId == null) return;
        fascicoloService.eliminaDocumentoDiOrigine(clienteId, TipoDocumento.SCHEDA, schedaId);
    }
}
