package it.nutrizionista.restnutrizionista.service;

import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.exception.TrattamentoLimitatoException;

/**
 * Enforcement della <b>limitazione del trattamento</b> (art. 18 GDPR, A5.3), nello stesso spirito di
 * {@link OwnershipValidator}: un unico punto di verifica riusato dai service.
 *
 * <p>Quando un {@link Cliente} è in stato {@code trattamentoLimitato}, il dato resta CONSERVATO e
 * LEGGIBILE dal titolare, ma ogni operazione che <b>scrive / produce / invia</b> dati del cliente
 * deve essere rifiutata con {@link TrattamentoLimitatoException} (HTTP 423 Locked).
 *
 * <p><b>Regola di coverage (non negoziabile — vedi CLAUDE.md):</b> ogni nuovo metodo service che
 * scrive/produce/invia dati di un cliente DEVE chiamare {@link #assertNonLimitato(Cliente)} subito
 * <i>dopo</i> l'ownership check ({@code ownershipValidator.getOwned*}). L'ordine è importante: un
 * tenant estraneo riceve 403 (dall'ownership) <b>prima</b> di 423, così non si rivela lo stato
 * "limitato" di un cliente di un altro nutrizionista.
 *
 * <p>Le <b>letture</b>, l'<b>export/download per il titolare</b> (GET .../pdf, download fascicolo) e
 * la consultazione NON chiamano questo check: l'art. 18 preserva l'accesso del titolare. Anche
 * {@code attivaLimitazione}/{@code revocaLimitazione} NON lo chiamano (devono operare su un cliente
 * limitato).
 */
@Service
public class LimitazioneTrattamentoValidator {

    private static final String MESSAGGIO =
            "Trattamento limitato (art. 18 GDPR): l'operazione modificherebbe, produrrebbe o invierebbe "
            + "dati di questo cliente e non è consentita finché la limitazione è attiva. "
            + "La consultazione dei dati resta possibile; revoca la limitazione per operare di nuovo.";

    /**
     * Rifiuta con 423 Locked se il cliente è in stato di limitazione del trattamento.
     * No-op se {@code cliente} è {@code null} (l'ownership/validazione a monte gestisce il caso).
     */
    public void assertNonLimitato(Cliente cliente) {
        if (cliente != null && cliente.isTrattamentoLimitato()) {
            throw new TrattamentoLimitatoException(MESSAGGIO);
        }
    }
}
