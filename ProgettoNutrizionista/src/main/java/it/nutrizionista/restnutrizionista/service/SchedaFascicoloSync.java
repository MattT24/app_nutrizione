package it.nutrizionista.restnutrizionista.service;

import it.nutrizionista.restnutrizionista.entity.TipoDocumento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Tiene allineato il PDF eventualmente già archiviato nel fascicolo con lo stato ATTUALE della
 * fonte (scheda, misurazione o plicometria): lo archivia la prima volta se non ancora presente,
 * altrimenti lo rigenera. È il punto UNICO da cui i service di dominio (pasti, alimenti, alternative,
 * template, misurazioni, plicometrie) innescano la sincronizzazione, così ognuno aggiunge una sola
 * riga invece di duplicare try/catch e logging.
 *
 * <p><b>Fuori dalla transazione di scrittura (PDF-01):</b> la sincronizzazione — che esegue il
 * rendering Chromium, potenzialmente lento e fallibile — viene registrata su
 * {@link TransactionSynchronization#afterCommit()} (stesso pattern di
 * {@code FascicoloService.unlinkFilesDopoCommit}) e gira quindi <b>dopo</b> il commit della tx di
 * dominio, in una tx propria aperta dal proxy di {@link FascicoloService}. Conseguenze:
 * <ul>
 *   <li>un fallimento del render <b>non</b> marca rollback-only la tx clinica (che era il difetto:
 *       il {@code catch} "best-effort" non salvava perché la tx partecipante era già avvelenata);</li>
 *   <li>la connessione DB della scrittura clinica non resta impegnata durante il rendering.</li>
 * </ul>
 * Best-effort per design: un fallimento della sincronizzazione (es. pool di rendering PDF saturo) non
 * deve MAI far fallire l'operazione di dominio — viene solo loggato. Il {@code catch} resta volutamente
 * LARGO: post-commit non c'è alcun chiamante a cui propagare un 403/423, quindi anche un'eventuale
 * eccezione di autorizzazione (ridondante: la scrittura clinica ha già superato ownership+limitazione)
 * va inghiottita e non trasformata in un errore. La semantica stretta 403/423 vale solo sul path
 * <i>manuale</i> e sincrono {@code FascicoloService.salvaDocumento}.
 */
@Component
public class SchedaFascicoloSync {

    private static final Logger log = LoggerFactory.getLogger(SchedaFascicoloSync.class);

    private final FascicoloService fascicoloService;

    public SchedaFascicoloSync(FascicoloService fascicoloService) {
        this.fascicoloService = fascicoloService;
    }

    /** Sincronizza il PDF di una SCHEDA (retro-compat per i service scheda). */
    public void sincronizza(Long clienteId, Long schedaId) {
        sincronizza(clienteId, TipoDocumento.SCHEDA, schedaId);
    }

    /** Sincronizza il PDF della fonte indicata DOPO il commit della tx di dominio (best-effort). */
    public void sincronizza(Long clienteId, TipoDocumento tipoDocumento, Long riferimentoId) {
        if (clienteId == null || riferimentoId == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    eseguiSync(clienteId, tipoDocumento, riferimentoId);
                }
            });
        } else {
            // Nessuna tx attiva (es. chiamata fuori da un metodo transazionale): esegui subito.
            eseguiSync(clienteId, tipoDocumento, riferimentoId);
        }
    }

    private void eseguiSync(Long clienteId, TipoDocumento tipoDocumento, Long riferimentoId) {
        try {
            fascicoloService.sincronizzaDocumento(clienteId, tipoDocumento, riferimentoId);
        } catch (RuntimeException e) {
            log.warn("Sincronizzazione automatica del fascicolo fallita ({} id={}): {}",
                    tipoDocumento, riferimentoId, e.getMessage(), e);
        }
    }

    /** Elimina (se esiste) il documento fascicolo di una SCHEDA (retro-compat per i service scheda). */
    public void elimina(Long clienteId, Long schedaId) {
        elimina(clienteId, TipoDocumento.SCHEDA, schedaId);
    }

    /**
     * Elimina (se esiste) il documento fascicolo della fonte indicata quando l'entità sorgente viene
     * eliminata. Resta <b>nella transazione</b> del chiamante (è una DELETE su DB atomica con la
     * cancellazione dell'entità sorgente; nessun rendering → nessun rischio rollback-only).
     */
    public void elimina(Long clienteId, TipoDocumento tipoDocumento, Long riferimentoId) {
        if (clienteId == null || riferimentoId == null) return;
        fascicoloService.eliminaDocumentoDiOrigine(clienteId, tipoDocumento, riferimentoId);
    }
}
