<%--
  -- Page which is displayed when uncaught exception is thrown or when other error happens.
  --%>
<%@ page language="java" pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<div class="jspError">

    <spring:message code="views.error.processing"/>
    <pre>${error.requestUri}</pre>

    <spring:message code="views.error.message"/>:
    <pre>${error.description}</pre>

    <spring:message code="views.error.notification"/>
    <tag:reportForm submitUrl="${contextPath}/error"/>

</div>
