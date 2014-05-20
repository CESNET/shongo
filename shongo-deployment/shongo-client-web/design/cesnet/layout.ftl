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

    <!-- CESNET's linker: start -->
    <div id="cesnet_linker_placeholder"
         <#if !user??>data-login-href="/login"</#if>
         data-lang="${session.locale.language}"
         data-lang-cs-href="?lang=cs"
         data-lang-en-href="?lang=en">
    </div>
    <!-- CESNET's linker: end -->

    <div id="page_wrapper" data-snap-ignore="true">

        <!-- Header: begin -->
        <nav class="navbar navbar-default" role="navigation">

            <!-- Non-collapsing right-side menu: start -->
            <div class="navbar-header pull-right">
                <!-- Menu items -->
                <ul class="nav pull-left">
                    <!-- Timezone -->
                    <li class="navbar-text pull-right" style="margin-left: 0px; margin-right: 15px;">
                        <span id="timezone" title="${session.timezone.help}">
                            ${session.timezone.title}
                        </span>
                    </li>

                <#if user??>
                    <!-- User information -->
                    <li class="dropdown pull-right">
                        <a style="margin-top: 5px;" class="dropdown-toggle" data-toggle="dropdown" href="#">
                            <i class="fa fa-cog"></i>
                            <b>${user.name}</b>
                            <#if user.administratorMode>
                                <!-- Administrator -->
                                (${message("user.administrator")})
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
                            <#if user.administratorModeAvailable>
                                <li>
                                    <a class="menuitem" href="${url.userSettingsAdministratorMode(!user.administratorMode)}">
                                        <#if user.administratorMode><i class="fa fa-check"></i></#if>${message("user.settingsAdministratorMode")}
                                    </a>
                                </li>
                            </#if>
                            <li class="divider"></li>
                            <li>
                                <a class="menuitem" href="${url.logout}">${message("user.logout")}</a>
                            </li>
                        </ul>
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
                    <li><a href="${link.url}">${link.title}</a></li>
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
                            <a href="${breadcrumb.url}">${breadcrumb.title}</a>
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

    </div>

    <div class="push"></div>
</div>
<!-- Content wrapper: end -->

<!-- CESNET's footer: start -->
<div id="footer" data-snap-ignore="true">
    <footer>
        <div class="container">
            <div class="row">
                <div class="col col-xs-3">
                    <div class="logo-wrapper">
                        <img src="${url.resources}/img/logo-cesnet.png" class="img-responsive" alt="cesnet logo">
                    </div>
                </div>
                <div class="col-lg-7 col-lg-push-2 col-xs-push-1 col-xs-8">
                    <div class="row">
                        <div class="col col-xs-4">
                            <h2>${message("footer.links")}</h2>
                            <ul>
                                <li><a href="http://pki.cesnet.cz/cs/ch-intro.html">CESNET PKI</a></li>
                                <li><a href="http://www.eduid.cz/">eduID.cz</a></li>
                                <li><a href="http://www.eduroam.cz/">eduroam</a></li>
                                <li><a href="http://www.metacentrum.cz/">MetaCentrum</a></li>
                                <li><a href="http://perun.cesnet.cz/web/">PERUN</a></li>
                            </ul>
                        </div>
                        <div class="col col-xs-4">
                            <h2>${message("footer.contact")}</h2>
                            ${message("footer.contact.name")}<br/>
                            ${message("footer.contact.address")}<br/>
                            ${message("footer.tel")}: ${message("footer.contact.tel")}<br/>
                            ${message("footer.fax")}: ${message("footer.contact.fax")}<br/>
                            <a href="mailto:${message("footer.contact.email")}">${message("footer.contact.email")}</a>
                        </div>
                        <div class="col col-xs-4">
                            <h2>${message("footer.serviceDesk")}</h2>
                            ${message("footer.tel")}: ${message("footer.serviceDesk.tel")}<br/>
                            ${message("footer.gsm")}: ${message("footer.serviceDesk.gsm")}<br/>
                            ${message("footer.fax")}: ${message("footer.serviceDesk.fax")}<br/>
                            <a href="mailto:${message("footer.serviceDesk.email")}">${message("footer.serviceDesk.email")}</a>
                        </div>
                    </div>
                </div>
            </div>
            <div class="row">
                <div class="col-xs-5">
                    ${message("app.powered")} <a href="https://shongo.cesnet.cz/" target="_blank">Shongo</a>
                    ${message("app.version")} <a href="${url.changelog}">${app.version}</a>
                </div>
                <div class="col-xs-7 text-right">
                    © 1991–${.now?string("yyyy")} CESNET, z. s. p. o
                </div>
            </div>
        </div>
    </footer>
</div>
<!-- CESNET's footer: end -->

<!-- CESNET's linker (JS): start -->
<script type="text/javascript" async src="${url.resources}/js/linker.js"></script>
<!--<script type="text/javascript" async src="https://linker.cesnet.cz/linker.js"></script>-->
<!-- CESNET's linker (JS): end -->

</body>
</html>