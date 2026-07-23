package it.nutrizionista.restnutrizionista.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import it.nutrizionista.restnutrizionista.dto.ShareRequest;
import it.nutrizionista.restnutrizionista.exception.BadRequestException;

/**
 * Copre {@link ShareRecipient#resolve}: senza {@code confermaOverride=true} l'indirizzo digitato
 * dall'utente viene ignorato a favore dell'email registrata del cliente. La UI (modale "Condividi
 * Documento") deve sempre inviare {@code confermaOverride=true}, altrimenti un indirizzo corretto
 * a mano nella modale verrebbe scartato in silenzio e il PDF sanitario finirebbe comunque
 * all'indirizzo registrato.
 */
class ShareRecipientTest {

    @Test
    void senzaOverride_usaEmailRegistrataDelCliente_ancheSeReqNeSpecificaUnaDiversa() {
        ShareRequest req = new ShareRequest();
        req.setEmail("altro@test.it");
        req.setConfermaOverride(false);

        String to = ShareRecipient.resolve("registrata@test.it", req);

        assertEquals("registrata@test.it", to);
    }

    @Test
    void conOverride_usaEmailSpecificataInReq() {
        ShareRequest req = new ShareRequest();
        req.setEmail("override@test.it");
        req.setConfermaOverride(true);

        String to = ShareRecipient.resolve("registrata@test.it", req);

        assertEquals("override@test.it", to);
    }

    @Test
    void conOverride_maEmailVuota_ricadeSullEmailRegistrata() {
        ShareRequest req = new ShareRequest();
        req.setEmail("  ");
        req.setConfermaOverride(true);

        String to = ShareRecipient.resolve("registrata@test.it", req);

        assertEquals("registrata@test.it", to);
    }

    @Test
    void senzaEmailRegistrataEsenzaOverride_lanciaBadRequest() {
        ShareRequest req = new ShareRequest();

        assertThrows(BadRequestException.class, () -> ShareRecipient.resolve(null, req));
    }

    @Test
    void trimmaLoSpazioAiBordiDellIndirizzoRisolto() {
        ShareRequest req = new ShareRequest();
        req.setEmail(" override@test.it ");
        req.setConfermaOverride(true);

        assertEquals("override@test.it", ShareRecipient.resolve("registrata@test.it", req));
    }
}
