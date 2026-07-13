package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.Email;

public class ShareRequest {

    // Destinatario di OVERRIDE: usato solo se confermaOverride=true.
    // Se assente/non confermato, il PDF va all'email registrata del cliente (risolta server-side).
    @Email(message = "Formato email non valido")
    private String email;

    // Conferma esplicita per inviare a un indirizzo diverso da quello registrato del cliente
    // (dati sanitari: evita invii ad address arbitrari digitati per errore).
    private boolean confermaOverride;

    public ShareRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isConfermaOverride() {
        return confermaOverride;
    }

    public void setConfermaOverride(boolean confermaOverride) {
        this.confermaOverride = confermaOverride;
    }
}
