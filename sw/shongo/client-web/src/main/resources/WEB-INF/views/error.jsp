<%--
  -- Page which is displayed when uncaught exception is thrown or when other error happens.
  --%>
<%@ page language="java" pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<tag:url var="submitUrl" value="/error"/>

<div class="jspError">

    <p><spring:message code="views.error.processing"/></p>
    <pre>${error.requestUri}</pre>

    <p><spring:message code="views.error.message"/>:</p>
    <pre>${error.description}</pre>

    <p><spring:message code="views.error.notification"/></p>
    <hr/>
    <tag:reportForm submitUrl="${submitUrl}"/>

</div>
