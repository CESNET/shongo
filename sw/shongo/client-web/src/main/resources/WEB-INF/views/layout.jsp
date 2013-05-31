<%@ page language="java" pageEncoding="UTF-8" contentType="text/html;charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="title"><tiles:getAsString name="title"/></c:set>
<spring:message code="${title}" var="title"/>

<html>
<head>
    <title>${title}</title>
</head>
<body>

<div>
    Menu:
    <ul>
        <li><a href="${contextPath}/"><spring:message code="views.layout.link.home"/></a></li>
        <li><a href="${contextPath}/reservation-requests"><spring:message code="views.layout.link.reservationRequests"/></a></li>
    </ul>
</div>

<div>
    <security:authorize access="!isAuthenticated()">
        <a href="${contextPath}/login"><spring:message code="views.layout.login"/></a>
    </security:authorize>
    <security:authorize access="isAuthenticated()">
        <c:set var="userName"><b><security:authentication property="principal"/></b></c:set>
        <spring:message code="views.layout.logged" arguments="${userName}"/>
        <a href="${contextPath}/logout"><spring:message code="views.layout.logout"/></a>
    </security:authorize>
</div>

<div>
    <spring:message code="views.layout.language"/>: <a href="?lang=en">English</a> | <a href="?lang=cs">Česky</a>
</div>

<h1>${title}</h1>

<tiles:insertAttribute name="body"/>

</body>
</html>
