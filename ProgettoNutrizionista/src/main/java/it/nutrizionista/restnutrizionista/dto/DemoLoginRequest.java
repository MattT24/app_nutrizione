package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Login separato per account demo: non modifica il contratto email/password esistente. */
public class DemoLoginRequest {
    @NotBlank @Size(min = 4, max = 64)
    private String username;
    @NotBlank @Size(max = 128)
    private String password;
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
