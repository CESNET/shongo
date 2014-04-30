<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <title>${message("app.name")} - ${title}</title>
    ${head}
    <script type="text/javascript">
        $(function () {
            $('#timezone').qtip({
                position: {
                    my: 'top right',
                    at: 'bottom center'
                },
                style: {
                    classes: 'qtip-app'
                }
            });
        });
    </script>
</head>
<body>

<!-- Content wrapper: begin -->
<div id="wrapper">

    <!-- Header: begin -->
    <div class="navbar navbar-default">

        <!-- Right panel: begin -->
        <ul class="nav navbar-nav pull-right">
            <!-- User -->
            <#if user??>
                <!-- Logged in user -->
                <li class="dropdown">
                    <a class="dropdown-toggle" data-toggle="dropdown" href="#">
                        <b class="icon-cog"></b>
                        <b>${user.name}</b>
                        <#if user.administratorMode>
                            (${message("user.administrator")})
                        </#if>
                        <b class="caret"></b>
                    </a>
                    <ul class="dropdown-menu">
                        <li>
                            <a class="menuitem" href="${url.user.settings}">${message("user.settings")}...</a>
                        </li>
                        <li class="divider"></li>
                        <li>
                            <a class="menuitem" href="${url.user.getSettingsAdvancedMode(!user.advancedMode)}">
                                <#if user.advancedMode><span class="icon-ok"></span></#if>
                                ${message("user.settingsAdvancedMode")}
                            </a>
                        </li>
                        <#if user.administratorModeAvailable>
                            <li>
                                <a class="menuitem" href="${url.user.getSettingsAdministratorMode(!user.administratorMode)}">
                                    <#if user.administratorMode><span class="icon-ok"></span></#if>
                                    ${message("user.settingsAdministratorMode")}
                                </a>
                            </li>
                        </#if>
                        <li class="divider"></li>
                        <li>
                            <a class="menuitem" href="${url.user.logout}">${message("user.logout")}</a>
                        </li>
                    </ul>
                </li>
            <#else>
                <!-- Login button -->
                <li>
                    <a href="${url.user.login}">${message("user.login")}</a>
                </li>
            </#if>

            <!-- Timezone -->
            <li>
                <span id="timezone" class="navbar-text" title="${session.timezone.help}">
                    ${session.timezone.title}
                </span>
            </li>

            <!-- Language selection -->
            <li>
                <span class="navbar-text">
                    <a class="language" href="${url.language.en}"><img src="${url.resources}/img/i18n/en.png" alt="English" title="English"/></a>
                    <a class="language" href="${url.language.cs}"><img src="${url.resources}/img/i18n/cs.png" alt="Česky" title="Česky"/></a>
                </span>
            </li>
        </ul>
        <!-- Right panel: end -->

        <!-- Left panel: begin -->
        <div class="navbar-header">
            <!-- Application name -->
            <a class="navbar-brand" href="${url.home}">${message("app.name")}</a>

            <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
                <div>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                </div>
                <span>&nbsp;${message("menu")}</span>
            </button>
        </div>
        <!-- Left panel: end -->

        <!-- Main links: begin -->
        <div class="navbar-collapse collapse">
            <ul class="nav navbar-nav">
                <#list links as link>
                    <li><a href="${link.url}">${link.title}</a></li>
                </#list>
                <li><a href="#">Test</a></li>
            </ul>
        </div>
        <!-- Main links: end -->

        <!-- Header breadcrumbs: begin -->
        <div>
            <!-- Report problem -->
            <ol class="breadcrumb pull-right">
                <li><a href="${url.report}">${message("report")}</a></li>
            </ol>

            <!-- Breadcrumb items -->
            <ol class="breadcrumb">
            <#list breadcrumbs as breadcrumb>
                <#if breadcrumb_has_next>
                    <li>
                        <a href="${breadcrumb.url}">${breadcrumb.title}</a>
                    </li>
                <#else>
                    <li class="active">${breadcrumb.title}</li>
                </#if>
            </#list>
            </ol>
        </div>
        <!-- Header breadcrumbs: end -->

    </div>
    <!-- Header: end -->

    <!-- Content placeholder: begin -->
    <div class="container">
        ${content}
    </div>
    <!-- Content placeholder: end -->

    <div class="push"></div>
</div>
<!-- Content wrapper: end -->

<!-- Footer: begin -->
<div id="footer">
    ${message("app.powered")} <a href="https://shongo.cesnet.cz/" target="_blank">Shongo</a>
    ${message("app.version")} <a href="${url.changelog}">${app.version}</a>
    &copy; 2012 - 2014&nbsp;&nbsp;&nbsp;
    <a title="CESNET" href="http://www.cesnet.cz/">
        <img src="${url.resources}/img/cesnet.gif" alt="CESNET, z.s.p.o."/>
    </a>
</div>
<!-- Footer: end -->

</body>
</html>