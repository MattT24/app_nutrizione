<#--
  Statera — pagina di login Keycloak brandizzata (pattern BFF/OIDC).
  Self-contained: rende il proprio <head> e linka resources/css/login.css (niente @layout).
  ⚠️ Contratto form Keycloak PRESERVATO: form→${url.loginAction}; input username/password; hidden credentialId;
     submit name="login"; rememberMe/resetPassword/social condizionali; errori via kcSanitize.
  Layout a due colonne fedele alla login originale (auth-shell.css): pannello brand emerald + pannello form.
-->
<!DOCTYPE html>
<html lang="it" data-theme="light">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="robots" content="noindex, nofollow">
  <title>Statera — Accesso</title>
  <link rel="stylesheet" href="${url.resourcesPath}/css/login.css">
</head>
<body>
  <div class="login-screen">
    <div class="login-card">

      <aside class="login-brand">
        <img class="login-brand__logo" src="${url.resourcesPath}/img/LogoStatera.png" alt="Statera" />
        <p class="login-brand__tag">L'equilibrio tra natura e scienza, al servizio dei tuoi pazienti.</p>
        <span class="login-brand__badge">Area professionista</span>
      </aside>

      <section class="login-form">
        <p class="login-eyebrow">Accesso area professionista</p>
        <h1 class="login-title">Bentornato</h1>
        <p class="login-sub">Accedi per gestire pazienti, agenda e schede.</p>

        <#if displayMessage?? && displayMessage && message?has_content>
          <div class="login-alert login-alert--${(message.type)!'error'}">
            ${kcSanitize(message.summary)?no_esc}
          </div>
        </#if>

        <#-- Errori per-campo: Keycloak 26 espone "credenziali non valide"/lockout come errore di campo con
             displayMessage=false → il blocco globale sopra viene saltato. Mirror dello stock KC. -->
        <#if messagesPerField.existsError('username','password')>
          <div class="login-alert login-alert--error">
            ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
          </div>
        </#if>

        <form id="kc-form-login" action="${url.loginAction}" method="post">
          <div class="login-field">
            <label class="login-field__label" for="username">Email</label>
            <div class="login-input">
              <input id="username" name="username" type="text" autofocus autocomplete="username"
                     value="${(login.username!'')}" placeholder="nome@studio.it" />
            </div>
          </div>

          <div class="login-field">
            <label class="login-field__label" for="password">Password</label>
            <div class="login-input">
              <input id="password" name="password" type="password" autocomplete="current-password"
                     placeholder="&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;" />
            </div>
          </div>

          <div class="login-row">
            <#if realm.rememberMe?? && realm.rememberMe>
              <label class="login-check">
                <input id="rememberMe" name="rememberMe" type="checkbox" <#if login.rememberMe??>checked</#if> />
                <span class="login-check__text">Ricordami</span>
              </label>
            <#else>
              <span></span>
            </#if>
            <#if realm.resetPasswordAllowed?? && realm.resetPasswordAllowed>
              <a class="login-link" href="${url.loginResetCredentialsUrl}">Password dimenticata?</a>
            </#if>
          </div>

          <input type="hidden" id="id-hidden-input" name="credentialId" value="${(auth.selectedCredential!'')}"/>
          <button class="login-submit" name="login" id="kc-login" type="submit">Accedi</button>
        </form>

        <#-- Social login (es. Google): compare automaticamente quando l'IdP è abilitato nel realm (al cutover). -->
        <#if social?? && social.providers?? && (social.providers?size > 0)>
          <div class="login-divider"><span>oppure</span></div>
          <div class="login-social">
            <#list social.providers as p>
              <a class="login-social__btn" id="social-${p.alias}" href="${p.loginUrl}">Accedi con ${p.displayName!p.alias}</a>
            </#list>
          </div>
        </#if>
      </section>

    </div>
  </div>
</body>
</html>
