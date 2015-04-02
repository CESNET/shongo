<p>${message("main.welcome")}</p>
<p>${message("main.suggestions", configuration["suggestion-email"])}</p>
<#if configuration["maintenance"]?? && configuration["maintenance"] >
    <p><strong>${message("main.maintenance")}</strong></p>
<#else>
    <#if !user??>
        <p><strong>${message("main.login", url.login)}</strong></p>
    </#if>
</#if>