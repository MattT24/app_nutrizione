<#--
  Statera — pagina Keycloak "info" brandizzata (esito azioni: email verificata, password aggiornata, ecc.).
  Self-contained: proprio <head> + login.css (niente @layout). Mostra il messaggio + link "Torna all'app".
-->
<!DOCTYPE html>
<html lang="it" data-theme="light">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="robots" content="noindex, nofollow">
  <title>Statera</title>
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
        <p class="login-eyebrow">Statera</p>
        <h1 class="login-title">
          <#if messageHeader??>${kcSanitize(messageHeader)?no_esc}<#elseif message?? && message.summary??>${kcSanitize(message.summary)?no_esc}<#else>Operazione completata</#if>
        </h1>
        <#if messageHeader?? && message?? && message.summary?? && messageHeader != message.summary>
          <p class="login-sub">${kcSanitize(message.summary)?no_esc}</p>
        </#if>

        <#if !(skipLink??) || !skipLink>
          <#if pageRedirectUri?has_content>
            <a class="login-submit" href="${pageRedirectUri}">Torna all'app</a>
          <#elseif actionUri?has_content>
            <a class="login-submit" href="${actionUri}">Continua</a>
          <#elseif client?? && client.baseUrl?has_content>
            <a class="login-submit" href="${client.baseUrl}">Torna all'app</a>
          </#if>
        </#if>
      </section>

    </div>
  </div>
</body>
</html>
