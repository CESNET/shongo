<%--
  -- Page which is shows message.
  --%>
<%@ page language="java" pageEncoding="UTF-8" contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<div>
    <h1><spring:message code="${title}"/></h1>
    <p><spring:message code="${message}"/></p>
    <div>
        <a class="btn btn-primary" href="${contextPath}${backUrl}"><spring:message code="views.button.back"/></a>
    </div>
</div>

