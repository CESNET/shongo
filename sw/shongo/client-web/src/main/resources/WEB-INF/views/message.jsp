<%--
  -- Page which is shows message.
  --%>
<%@ page language="java" pageEncoding="UTF-8" contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="urlBack" value="${contextPath}${urlBack}"/>

<div>
    <h1><spring:message code="${title}"/></h1>
    <p><spring:message code="${message}"/></p>
    <div>
        <a class="btn btn-primary" href="${urlBack}"><spring:message code="views.button.back"/></a>
    </div>
</div>

