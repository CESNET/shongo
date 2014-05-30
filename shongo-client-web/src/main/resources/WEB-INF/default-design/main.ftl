<p>${message("main.welcome")}</p>
<p>${message("main.suggestions", configuration["suggestion-email"])}</p>
<#if !user??>
    <p><strong>${message("main.login", url.login)}</strong></p>
</#if>