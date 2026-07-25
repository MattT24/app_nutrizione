package it.nutrizionista.restnutrizionista.security;

/**
 * Binari di autenticazione selezionabili via property {@code auth.provider} (feature flag della migrazione Keycloak).
 * <ul>
 *   <li>{@code legacy-jwt} (default): custom-JWT HS256 esistente ({@code LegacySecurityConfig}).</li>
 *   <li>{@code keycloak}: BFF OIDC ({@code KeycloakSecurityConfig}, 2 filter chain).</li>
 * </ul>
 * Le due {@code @Configuration} sono mutuamente esclusive (@ConditionalOnProperty). Un valore ignoto è un errore
 * di configurazione (vedi {@code AuthProviderStartupValidator}, fail-closed all'avvio).
 */
public enum AuthProvider {
    LEGACY_JWT("legacy-jwt"),
    KEYCLOAK("keycloak");

    public final String value;

    AuthProvider(String value) {
        this.value = value;
    }

    /** True se {@code v} è uno dei binari noti. */
    public static boolean isValid(String v) {
        return LEGACY_JWT.value.equals(v) || KEYCLOAK.value.equals(v);
    }
}
