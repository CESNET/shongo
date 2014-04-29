<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <title></title>
    ${param.head}
</head>
<body>

<!-- Content wrapper: begin -->
<div id="wrapper">

    <!-- Header: begin -->
    <div class="navbar navbar-static-top block">
        <div class="navbar-inner">

            <!-- Left panel - application name and main links -->
            <div class="main">

                <!-- Application name -->
                <a class="brand" href="${url.home}">${name}</a>

                <!-- Button which represents collapsed main links -->
                <div class="pull-left">
                    <ul class="nav" role="navigation">
                        <li>
                            <button type="button" class="btn btn-navbar" data-toggle="collapse"
                                    data-target=".nav-collapse">
                                <div>
                                    <span class="icon-bar"></span>
                                    <span class="icon-bar"></span>
                                    <span class="icon-bar"></span>
                                </div>
                                <span>&nbsp;<spring:message code="design.menu"/></span>
                            </button>
                        </li>
                    </ul>
                </div>

                <!-- Main links -->
                <div class="nav-collapse collapse pull-left">
                    <ul class="nav" role="navigation">
                        <c:forEach items="${links}" var="link">
                            <a href="${link.url}">${link.title}</a>
                        </c:forEach>
                    </ul>
                </div>

            </div>

            <!-- Right panel - user, timezone, language -->
           <%-- <div class="breadcrumbs pull-right">
                <ul class="nav">

                    <!-- Login button -->
                    <c:if test="${empty user.id}">
                        <li>
                            <a href="${url.user.login}"><spring:message code="design.login"/></a>
                        </li>
                    </c:if>

                    <!-- Logged in user -->
                    <c:if test="${not empty user.id}">
                        <li class="dropdown">
                            <a class="dropdown-toggle" data-toggle="dropdown" href="#">
                                <b class="icon-cog"></b>
                                <b>${user.name}</b><c:if test="${user.administrator}">&nbsp;(<spring:message code="design.user.administrator"/>)</c:if>
                                <b class="caret"></b>
                            </a>
                            <ul class="dropdown-menu" role="menu">
                                <li>
                                    <a class="menuitem" href="${url.user.settings}"><spring:message code="design.user.settings"/>...</a>
                                </li>
                                <li class="divider"></li>
                                <li>
                                    <a class="menuitem" href="${url.user.settings.advancedMode}">
                                        <c:if test="${user.settings.advancedMode}"><span class="icon-ok"></span></c:if><spring:message code="design.user.settings.advancedMode"/>
                                    </a>
                                </li>
                                <c:if test="${user.settings.administratorModeAvailable}">
                                    <li>
                                        <a class="menuitem" href="${url.user.settings.administratorMode}">
                                            <c:if test="${user.settings.administratorMode}"><span class="icon-ok"></span></c:if><spring:message code="design.user.settings.administratorMode"/>
                                        </a>
                                    </li>
                                </c:if>
                                <li class="divider"></li>
                                <li>
                                    <a class="menuitem" href="${url.user.logout}"><spring:message code="design.user.logout"/></a>
                                </li>
                            </ul>
                        </li>
                    </c:if>

                    <!-- Timezone -->
                    <li>
                        <span id="timezone" class="navbar-text" title="${userSession.timezone.help}">
                            ${userSession.timezone.title}
                        </span>
                    </li>

                    <!-- Language selection -->
                    <li>
                        <span class="navbar-text">
                            <a id="language-english" href="${url.language.en}">
                                <img class="language" src="${url.resources}/img/i18n/en.png" alt="English" title="English"/>
                            </a>
                            <a id="language-czech" href="${url.language.cs}">
                                <img class="language" src="${url.resources}/img/i18n/cs.png" alt="Česky" title="Česky"/>
                            </a>
                        </span>
                    </li>
                </ul>
            </div>

            <!-- Breadcrumbs -->
            <div class="breadcrumbs">
                <ul>
                    <c:forEach items="${breadcrumbs}" var="breadcrumb" varStatus="breadcrumbStatus">
                        <c:when test="${!breadcrumbStatus.last}">
                            <li>
                                <a href="${breadcrumb.url}">${breadcrumb.title}</a>
                                <span class="divider">/</span>
                            </li>
                        </c:when>
                        <c:otherwise>
                            <li class="active">${breadcrumb.title}</li>
                        </c:otherwise>
                    </c:forEach>
                </ul>
            </div>

            <!-- Report problem -->
            <div class="pull-right">
                <a href="${url.report}"><spring:message code="design.report"/></a>
            </div>  --%>

        </div>
    </div>
    <!-- Header: end -->

    <!-- Content placeholder: begin -->
    <div class="container">
        ${param.content}
    </div>
    <!-- Content placeholder: end -->

    <div class="push"></div>
</div>
<!-- Content wrapper: end -->

<!-- Footer: begin -->
<div id="footer">
    <p class="muted">
        Powered by <a href="https://shongo.cesnet.cz/" target="_blank">Shongo</a>
        version <a href="${url.changelog}">${version}</a>
        &copy; 2012 - 2014&nbsp;&nbsp;&nbsp;
        <a title="CESNET" href="http://www.cesnet.cz/">
            <img src="${url.resources}/img/cesnet.gif" alt="CESNET, z.s.p.o."/>
        </a>
    </p>
</div>
<!-- Footer: end -->

</body>
</html>