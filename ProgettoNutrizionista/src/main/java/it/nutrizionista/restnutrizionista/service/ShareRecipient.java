package it.nutrizionista.restnutrizionista.service;

import it.nutrizionista.restnutrizionista.dto.ShareRequest;
import it.nutrizionista.restnutrizionista.exception.BadRequestException;

/**
 * Risolve il destinatario dell'invio via email di un PDF sanitario.
 * Default: l'email REGISTRATA del cliente (risolta server-side). L'override verso un altro
 * indirizzo è possibile solo con conferma esplicita ({@code confermaOverride=true}), così i
 * dati sanitari non vengono inviati a indirizzi arbitrari digitati per errore.
 */
public final class ShareRecipient {

    private ShareRecipient() {
    }

    public static String resolve(String clienteEmail, ShareRequest req) {
        if (req != null && req.isConfermaOverride()
                && req.getEmail() != null && !req.getEmail().isBlank()) {
            return req.getEmail().trim();
        }
        if (clienteEmail == null || clienteEmail.isBlank()) {
            throw new BadRequestException(
                    "Il cliente non ha un'email registrata: specifica un destinatario e conferma l'invio (confermaOverride=true).");
        }
        return clienteEmail.trim();
    }
}
