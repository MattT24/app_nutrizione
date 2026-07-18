package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Seconda autorizzazione per un'operazione critica; nessun valore viene loggato. */
public class ImpersonazioneDemoRequest {
    @NotBlank @Size(max = 128)
    private String adminPassword;
    @NotBlank @Size(max = 256)
    private String masterPassword;
    @NotBlank @Size(min = 10, max = 500)
    private String motivo;
    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
    public String getMasterPassword() { return masterPassword; }
    public void setMasterPassword(String masterPassword) { this.masterPassword = masterPassword; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
