<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<%@ page language="java" pageEncoding="UTF-8" contentType="text/html;charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>

<c:set var="path" value="${pageContext.request.contextPath}"/>
<c:set var="title"><tiles:getAsString name="title"/></c:set>
<spring:message code="${title}" var="title"/>

<head>
    <title>${title}</title>
    <link href="${path}/css/bootstrap.min.css" rel="stylesheet">
    <link href="${path}/css/application.css" rel="stylesheet">
    <link href="${path}/css/layout.css" rel="stylesheet">
</head>

<body>

<div class="navbar  navbar-fixed-top">
    <div class="navbar-inner">
        <div class="navbar-text pull-right">
            <spring:message code="views.layout.language"/>: <a href="?lang=en">English</a> | <a href="?lang=cs">Česky</a>
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

        <div class="container">
            <a class="brand" href="/"><spring:message code="shongo.name"/></a>
            <div class="nav-collapse collapse">
                <ul class="nav">
                    <li><a href="${path}/"><spring:message code="views.layout.link.home"/></a></li>
                    <li><a href="${path}/reservation-requests"><spring:message code="views.layout.link.reservationRequests"/></a></li>
                </ul>
            </div>
        </div>
    </div>
</div>

<div class="content">
    <div class="wrapper">
        <div class="proper-content">
            <div class="container">
                <h1>${title}</h1>
                <tiles:insertAttribute name="body"/>
            </div>
        </div>
        <div class="push"></div>
    </div>
    <div class="footer" >
        <p class="muted">
            <a href="${path}/changelog"><spring:message code="shongo.shortname"/> <spring:message code="shongo.version"/></a>
            &copy; 2012 - 2013&nbsp;&nbsp;&nbsp;
            <a title="CESNET" href="http://www.cesnet.cz/"><img src="${path}/img/cesnet.gif" alt="CESNET, z.s.p.o."/></a>
        </p>
    </div>
</div>

<script src="${path}/js/jquery.min.js"></script>
<script src="${path}/js/bootstrap.min.js"></script>
</body>

</html>
