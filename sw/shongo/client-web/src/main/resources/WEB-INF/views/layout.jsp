<%--
  -- Page layout template to which are inserted all other pages into "body" attribute.
  --%>
<%@ page language="java" pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" trimDirectiveWhitespaces="true" %>
<%@ page import="org.springframework.web.util.UriComponentsBuilder" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="cs" xml:lang="cs">

<%-- Tag Libraries --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>

<%-- Variables --%>
<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<tiles:importAttribute/>
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

<%-- Page navigation header --%>
<div class="navbar navbar-fixed-top">
    <div class="navbar-inner">

        <div class="container">
            <a class="brand" href="/"><spring:message code="shongo.name"/>&nbsp;${configuration.titleSuffix}</a>

            <div class="nav-collapse collapse">
                <ul class="nav">
                    <li><a href="${contextPath}/"><spring:message code="views.layout.link.home"/></a></li>
                    <li><a href="${contextPath}/reservation-request"><spring:message code="views.layout.link.reservationRequests"/></a></li>
                </ul>
            </div>
        </div>

        <div style="margin-top: -40px; position: relative;">
            <div class="navbar-text pull-right">
                <spring:message code="views.layout.timezone" var="timezone"/>
                <span id="timezone" title="${timezone}">${sessionScope.dateTimeZone}</span>
                &nbsp;
                <a id="language-english" href="${urlLanguage.replaceAll(":lang", "en")}"><img class="language" src="${contextPath}/img/i18n/en.png" alt="English" title="English"/></a>
                <a id="language-czech" href="${urlLanguage.replaceAll(":lang", "cs")}"><img class="language" src="${contextPath}/img/i18n/cz.png" alt="Česky" title="Česky"/></a>
            </div>
            <div class="navbar-text pull-right">
                <security:authorize access="!isAuthenticated()">
                    <a href="${contextPath}/login"><spring:message code="views.layout.login"/></a>
                </security:authorize>
                <security:authorize access="isAuthenticated()">
                    <c:set var="userName"><b><security:authentication property="principal"/></b></c:set>
                    <spring:message code="views.layout.logged" arguments="${userName}"/>
                    <a href="${contextPath}/logout"><spring:message code="views.layout.logout"/></a>
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
        <div class="push"></div>
    </div>

    <%-- Page footer --%>
    <div class="footer">
        <p class="muted">
            <a href="${contextPath}/changelog"><spring:message code="shongo.shortname"/>&nbsp;<spring:message
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