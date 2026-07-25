<#--
  Statera — pagina Keycloak "aggiorna password" brandizzata (required-action UPDATE_PASSWORD).
  Self-contained: rende il proprio <head> e riusa resources/css/login.css (nessuna nuova regola CSS; niente @layout).
  ⚠️ Contratto form Keycloak update-password PRESERVATO: form→${url.loginAction}; hidden username(readonly)+password
     (per i password-manager); campi visibili password-new/password-confirm (nomi ESATTI, critici); checkbox
     logout-sessions; submit; cancel-aia solo sotto isAppInitiatedAction. Errori via kcSanitize (unico ?no_esc).
  Layout a due colonne identico a login.ftl → continuità visiva col login.
-->
<!DOCTYPE html>
<html lang="it" data-theme="light">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="robots" content="noindex, nofollow">
  <title>Statera — Aggiorna password</title>
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
        <p class="login-eyebrow">Sicurezza account</p>
        <h1 class="login-title">Imposta una nuova password</h1>
        <p class="login-sub">Scegli una password sicura per continuare ad accedere a Statera.</p>
        <#-- Requisiti dalla password-policy del realm (length12/upperCase1/lowerCase1/digits1/notUsername). -->
        <p class="login-hint">Minimo 12 caratteri, con almeno 1 maiuscola, 1 minuscola e 1 cifra; diversa dall'email.</p>

        <#if displayMessage?? && displayMessage && message?has_content>
          <div class="login-alert login-alert--${(message.type)!'error'}">
            ${kcSanitize(message.summary)?no_esc}
          </div>
        </#if>

        <#-- Errori per-campo: violazioni di policy password rese da Keycloak come errore di campo con
             displayMessage=false → altrimenti una nuova password non conforme fallirebbe in silenzio. -->
        <#if messagesPerField.existsError('password-new','password-confirm')>
          <div class="login-alert login-alert--error">
            ${kcSanitize(messagesPerField.getFirstError('password-new','password-confirm'))?no_esc}
          </div>
        </#if>

        <form id="kc-passwd-update-form" action="${url.loginAction}" method="post">
          <#-- Campi nascosti per i password-manager (contratto Keycloak). -->
          <input type="text" id="username" name="username" value="${(username!'')}"
                 autocomplete="username" readonly="readonly" style="display:none;" />
          <input type="password" id="password" name="password"
                 autocomplete="current-password" style="display:none;" />

          <div class="login-field">
            <label class="login-field__label" for="password-new">Nuova password</label>
            <div class="login-input">
              <input id="password-new" name="password-new" type="password" autofocus
                     autocomplete="new-password"
                     placeholder="&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;" />
            </div>
          </div>

          <div class="login-field">
            <label class="login-field__label" for="password-confirm">Conferma password</label>
            <div class="login-input">
              <input id="password-confirm" name="password-confirm" type="password"
                     autocomplete="new-password"
                     placeholder="&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;" />
            </div>
          </div>

          <div class="login-row">
            <label class="login-check">
              <input id="logout-sessions" name="logout-sessions" type="checkbox" value="on" checked />
              <span class="login-check__text">Esci dalle altre sessioni</span>
            </label>
          </div>

          <button class="login-submit" type="submit">Aggiorna password</button>

          <#if isAppInitiatedAction??>
            <button class="login-link" type="submit" name="cancel-aia" value="true"
                    style="border:none;background:none;cursor:pointer;margin-top:14px;align-self:center;">
              Annulla
            </button>
          </#if>
        </form>
      </section>

    </div>
  </div>
</body>
</html>
