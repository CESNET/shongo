<%@ page language="java" pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" trimDirectiveWhitespaces="true" %>
<%@ page import="org.springframework.web.util.UriComponentsBuilder" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<%-- Tag Libraries --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>

<%-- Variables --%>
<c:set var="path" value="${pageContext.request.contextPath}"/>
<c:set var="title"><tiles:getAsString name="title"/></c:set>
<spring:message code="${title}" var="title"/>
<%
    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(
            (String) request.getAttribute("javax.servlet.forward.request_uri"));
    uriBuilder.query(request.getQueryString()).replaceQueryParam("lang", ":lang");
    pageContext.setAttribute("urlLanguage", uriBuilder.build().toUriString());
%>

<%-- Header --%>
<head>
    <title>${title}</title>
    <link href="${path}/css/bootstrap.min.css" rel="stylesheet">
    <link href="${path}/css/application.css" rel="stylesheet">
    <link href="${path}/css/layout.css" rel="stylesheet">
</head>

<body>

<%-- Page navigation header --%>
<div class="navbar navbar-fixed-top">
    <div class="navbar-inner">

        <div class="container">
            <a class="brand" href="/"><spring:message code="shongo.name"/>&nbsp;${configuration.titleSuffix}</a>

            <div class="nav-collapse collapse">
                <ul class="nav">
                    <li><a href="${path}/"><spring:message code="views.layout.link.home"/></a></li>
                    <li><a href="${path}/reservation-request"><spring:message code="views.layout.link.reservationRequests"/></a></li>
                </ul>
            </div>
        </div>

        <div style="margin-top: -40px; position: relative;">
            <div class="navbar-text pull-right">
                <a href="${urlLanguage.replaceAll(":lang", "en")}"><img class="language" src="${path}/img/i18n/en.png" alt="English" title="English"/></a>
                <a href="${urlLanguage.replaceAll(":lang", "cs")}"><img class="language" src="${path}/img/i18n/cz.png" alt="Česky" title="Česky"/></a>
            </div>
            <div class="navbar-text pull-right">
                <security:authorize access="!isAuthenticated()">
                    <a href="${path}/login"><spring:message code="views.layout.login"/></a>
                </security:authorize>
                <security:authorize access="isAuthenticated()">
                    <c:set var="userName"><b><security:authentication property="principal"/></b></c:set>
                    <spring:message code="views.layout.logged" arguments="${userName}"/>
                    <a href="${path}/logout"><spring:message code="views.layout.logout"/></a>
                </security:authorize>
            </div>
        </div>

    </div>
</div>

<div class="content">
    <%-- Page content --%>
    <div class="wrapper">
        <div class="proper-content">
            <div class="container">
                <h1>${title}</h1>
                <tiles:insertAttribute name="body"/>
            </div>
        </div>
        <div class="push"></div>
    </div>

    <%-- Page footer --%>
    <div class="footer">
        <p class="muted">
            <a href="${path}/changelog"><spring:message code="shongo.shortname"/> <spring:message
                    code="shongo.version"/></a>
            &copy; 2012 - 2013&nbsp;&nbsp;&nbsp;
            <a title="CESNET" href="http://www.cesnet.cz/">
                <img src="${path}/img/cesnet.gif" alt="CESNET, z.s.p.o."/>
            </a>
        </p>
    </div>
</div>

<script src="${path}/js/jquery.min.js"></script>
<script src="${path}/js/bootstrap.min.js"></script>
</body>

</html>
