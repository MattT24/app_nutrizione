<#--
  Statera — pagina Keycloak "link scaduto" brandizzata (action token / invito scaduto).
  Self-contained: proprio <head> + login.css. Due vie: ricomincia il flusso oppure continua l'ultimo.
-->
<!DOCTYPE html>
<html lang="it" data-theme="light">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="robots" content="noindex, nofollow">
  <title>Statera — Link scaduto</title>
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
        <p class="login-eyebrow">Sessione</p>
        <h1 class="login-title">Link scaduto</h1>
        <p class="login-sub">Il link non è più valido. Ricomincia l'accesso o continua da dove eri.</p>

        <a class="login-submit" href="${url.loginRestartFlowUrl}">Ricomincia l'accesso</a>
        <div class="login-row" style="justify-content:center;margin-top:14px;">
          <a class="login-link" href="${url.loginAction}">Continua</a>
        </div>
      </section>

    </div>
  </div>
</body>
</html>
