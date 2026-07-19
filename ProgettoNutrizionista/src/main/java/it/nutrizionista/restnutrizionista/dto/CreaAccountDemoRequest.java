package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreaAccountDemoRequest {
    @NotBlank
    @Size(min = 4, max = 64)
    @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9._-]*$",
            message = "può contenere solo lettere, numeri, punto, trattino e underscore")
    private String username;

    @NotBlank
    @Size(min = 14, max = 128)
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
