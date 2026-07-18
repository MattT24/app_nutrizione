package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RuotaPasswordDemoRequest {
    @NotBlank @Size(min = 14, max = 128)
    private String password;
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
