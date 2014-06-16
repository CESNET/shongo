<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="cs" xml:lang="cs">
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${message("app.name")} - ${title}</title>
    ${head}

    <!-- Layout tooltips: start -->
    <script type="text/javascript">
        $(function () {
            $('#timezone').qtip({position: {my: 'top right', at: 'bottom center'}, style: {classes: 'qtip-app'}});
        });
    </script>
    <!-- Layout tooltips: end -->
</head>
<body>

<!-- Content wrapper: begin -->
<div id="wrapper">

    <!-- Header: begin -->
    <nav class="navbar navbar-default" role="navigation">

        <!-- Non-collapsing right-side menu: start -->
        <div class="navbar-header pull-right">
            <!-- Menu items -->
            <ul class="nav pull-left">
                <!-- Language selection -->
                <li class="navbar-text pull-right" style="margin-left: 0px; margin-right: 15px;">
                    <span>
                        <a class="language" href="${url.languageEn}"><img src="${url.resources}/img/i18n/en.png" alt="English" title="English"/></a>
                        <a class="language" href="${url.languageCs}"><img src="${url.resources}/img/i18n/cs.png" alt="Česky" title="Česky"/></a>
                    </span>
                </li>

                <!-- Timezone -->
                <li class="navbar-text pull-right" style="margin-left: 0px; margin-right: 15px;">
                    <span id="timezone" title="${session.timezone.help}">
                        ${session.timezone.title}
                    </span>
                </li>

            <#if user??>
                <!-- User information -->
                <li class="dropdown pull-right">
                    <a class="dropdown-toggle" data-toggle="dropdown" href="#">
                        <i class="fa fa-cog"></i>
                        <b>${user.name}</b>
                        <#if user.administrationMode>
                            <!-- Administration -->
                            (${message("user.administration")})
                        </#if>
                        <b class="caret"></b>
                        <#if !user.reservationAvailable>
                            <!-- Reservation warning -->
                            <script type="text/javascript">
                                $(function () {
                                    $('#warning').qtip({
                                        content: { text: "${escapeJavaScript(message("user.reservationDisabled", url.help + "#loa"))}" },
                                        position: { my: 'top right', at: 'bottom center' },
                                        style: { classes: 'qtip-app' },
                                        hide: { fixed: true, delay: 300 }
                                    });
                                });
                            </script>
                            <b id="warning" class="fa fa-warning" style="color: #f71;"></b>
                        </#if>
                    </a>
                    <ul class="dropdown-menu">
                        <li>
                            <a class="menuitem" href="${url.userSettings}">${message("user.settings")}...</a>
                        </li>
                        <li class="divider"></li>
                        <li>
                            <a class="menuitem" href="${url.userSettingsAdvancedMode(!user.advancedMode)}">
                                <#if user.advancedMode><i class="fa fa-check"></i></#if>${message("user.settingsAdvancedMode")}
                            </a>
                        </li>
                        <#if user.administrationModeAvailable>
                            <li>
                                <a class="menuitem" href="${url.userSettingsAdministrationMode(!user.administrationMode)}">
                                    <#if user.administrationMode><i class="fa fa-check"></i></#if>${message("user.settingsAdministrationMode")}
                                </a>
                            </li>
                        </#if>
                        <li class="divider"></li>
                        <li>
                            <a class="menuitem" href="${url.logout}">${message("user.logout")}</a>
                        </li>
                    </ul>
                </li>
            <#else>
                <!-- Login button -->
                <li class="pull-right">
                    <a href="${url.login}">${message("user.login")}</a>
                </li>
            </#if>
            </ul>
        </div>
        <!-- Non-collapsing right-side menu: end -->

        <!-- Application name -->
        <div class="navbar-header">
            <a class="navbar-brand" href="${url.home}">${message("app.name")}</a>

            <!-- Collapsed menu -->
            <button type="button" data-toggle="collapse" data-target=".navbar-collapse" class="navbar-toggle pull-left" style="padding-top: 4px; padding-bottom: 4px;">
                    <span style="display: inline-block; margin-top: 5px;">
                        <span class="icon-bar" ></span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                    </span>
                    <span style="display: inline-block; padding-top: 2px; vertical-align: top;">
                        &nbsp;${message("menu")}
                    </span>
            </button>
        </div>

        <!-- Collapsing left-side menu: start -->
        <div class="collapse navbar-collapse navbar-left">
            <ul class="nav navbar-nav">
            <#list links as link>
                <#if link.separator>
                    <li class="divider"></li>
                <#elseif link.hasChildLinks()>
                    <li class="dropdown">
                        <a class="dropdown-toggle" data-toggle="dropdown" href="#">
                        ${link.title}&nbsp;<b class="caret"></b>
                        </a>
                        <ul class="dropdown-menu">
                            <#list link.childLinks as childLink>
                                <#if childLink.separator>
                                    <li class="divider"></li>
                                <#else>
                                    <li><a href="${childLink.url}">${childLink.title}</a></li>
                                </#if>
                            </#list>
                        </ul>
                    </li>
                <#else>
                    <li><a href="${link.url}">${link.title}</a></li>
                </#if>
            </#list>
            </ul>
        </div>
        <!-- Collapsing left-side menu: end -->

        <!-- Header breadcrumbs: begin -->
        <div class="breadcrumbs">
            <!-- Report problem -->
            <ol class="breadcrumb pull-right">
                <li><a href="${url.report}">${message("report")}</a></li>
            </ol>

            <!-- Breadcrumb items -->
            <ol class="breadcrumb">
            <#list breadcrumbs as breadcrumb>
                <#if breadcrumb_has_next>
                    <li>
                        <#if breadcrumb.url??>
                            <a href="${breadcrumb.url}">${breadcrumb.title}</a>
                        <#else>
                            ${breadcrumb.title}
                        </#if>
                    </li>
                <#else>
                    <li class="active">${breadcrumb.title}</li>
                </#if>
            </#list>
            </ol>
        </div>
        <!-- Header breadcrumbs: end -->

    </nav>
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
    &copy; 2012 - ${.now?string("yyyy")}&nbsp;&nbsp;&nbsp;
    <a title="CESNET" href="http://www.cesnet.cz/">
        <img src="${url.resources}/img/cesnet.gif" alt="CESNET, z.s.p.o."/>
    </a>
</div>
<!-- Footer: end -->

</body>
</html>