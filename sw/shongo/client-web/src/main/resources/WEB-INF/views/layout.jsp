<%--
  -- Page layout template to which are inserted all other pages into "body" attribute.
  --%>
<%@ page language="java" pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" trimDirectiveWhitespaces="true" %>
<%@ page import="org.springframework.web.util.UriComponentsBuilder" %>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="cs" xml:lang="cs">

<%-- Tag Libraries --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>

<%-- Variables --%>
<tiles:importAttribute/>
<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="urlDashboard">${contextPath}<%= ClientWebUrl.DASHBOARD %></c:set>
<c:set var="urlWizard">${contextPath}<%= ClientWebUrl.WIZARD %></c:set>
<c:set var="urlReservationRequestList">${contextPath}<%= ClientWebUrl.RESERVATION_REQUEST_LIST %></c:set>
<c:set var="urlChangelog">${contextPath}<%= ClientWebUrl.CHANGELOG %></c:set>
<spring:message code="${title}" var="title"/>
<%
    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(
            (String) request.getAttribute("javax.servlet.forward.request_uri"));
    uriBuilder.query(request.getQueryString()).replaceQueryParam("lang", ":lang");
    pageContext.setAttribute("urlLanguage", uriBuilder.build().toUriString());
%>

<%-- Header --%>
<head>
    <title>Shongo - ${title}</title>

    <c:forEach items="${css}" var="file">
        <link rel="stylesheet" href="${contextPath}/css/${file}" />
    </c:forEach>
    <c:forEach items="${js}" var="file">
        <script src="${contextPath}/js/${file}"></script>
    </c:forEach>
    <c:if test="${requestContext.locale.language != 'en'}">
        <c:forEach items="${i18n}" var="file">
            <script src="${contextPath}/js/i18n/${file}.${requestContext.locale.language}.js"></script>
        </c:forEach>
    </c:if>
</head>

<body>

<div class="content">

<%-- Page navigation header --%>
<div class="navbar navbar-static-top block">
    <div class="navbar-inner">

        <div class="main">
            <a class="brand" href="/"><spring:message code="shongo.name"/>&nbsp;${configuration.titleSuffix}</a>
            <div class="nav-collapse collapse pull-left">
                <ul class="nav" role="navigation">
                    <li><a href="${urlDashboard}"><spring:message code="views.layout.link.dashboard"/></a></li>
                    <li><a href="${urlWizard}"><spring:message code="views.layout.link.wizard"/></a></li>
                    <li><a href="${urlReservationRequestList}"><spring:message code="views.layout.link.reservationRequests"/></a></li>
                </ul>
            </div>
        </div>

        <ul class="nav pull-right">
            <li>
                <button type="button" class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                </button>
            </li>
            <security:authorize access="!isAuthenticated()">
                <li>
                    <c:set var="urlLogin">${contextPath}<%= ClientWebUrl.LOGIN %></c:set>
                    <a href="${urlLogin}"><spring:message code="views.layout.login"/></a>
                </li>
            </security:authorize>
            <security:authorize access="isAuthenticated()">
                <li class="dropdown">
                    <a class="dropdown-toggle" data-toggle="dropdown" href="#">
                        <b><security:authentication property="principal"/></b>
                        <b class="caret"></b>
                    </a>
                    <ul class="dropdown-menu" role="menu">
                        <li>
                            <c:set var="urlLogout">${contextPath}<%= ClientWebUrl.LOGOUT %></c:set>
                            <a class="menuitem" href="${urlLogout}"><spring:message code="views.layout.logout"/></a>
                        </li>
                    </ul>
                </li>
            </security:authorize>
            <li>
                <spring:message code="views.layout.timezone" var="timezone"/>
                <span class="navbar-text" id="timezone" title="${timezone}">${sessionScope.dateTimeZone}</span>
            </li>
            <li>
                <span class="navbar-text">
                    <a id="language-english" href="${urlLanguage.replaceAll(":lang", "en")}"><img class="language" src="${contextPath}/img/i18n/en.png" alt="English" title="English"/></a>
                    <a id="language-czech" href="${urlLanguage.replaceAll(":lang", "cs")}"><img class="language" src="${contextPath}/img/i18n/cz.png" alt="Česky" title="Česky"/></a>
                </span>
            </li>


        </ul>

    </div>
</div>

<%-- Page content --%>
<div class="block push">
    <div class="container">
    <c:choose>
        <c:when test="${heading == 'title'}">
            <h1>${title}</h1>
        </c:when>
        <c:when test="${heading != ''}">
            <h1>${heading}</h1>
        </c:when>
    </c:choose>
    <tiles:insertAttribute name="body"/>
        </div>
</div>

<%-- Page footer --%>
<div class="footer block">
    <p class="muted">
        <a href="${urlChangelog}"><spring:message code="shongo.shortname"/>&nbsp;<spring:message
                code="shongo.version"/></a>
        &copy; 2012 - 2013&nbsp;&nbsp;&nbsp;
        <a title="CESNET" href="http://www.cesnet.cz/">
            <img src="${contextPath}/img/cesnet.gif" alt="CESNET, z.s.p.o."/>
        </a>
    </p>
</div>

</div>

</body>

</html>
