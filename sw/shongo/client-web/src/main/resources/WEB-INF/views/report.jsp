<%--
  -- Page for reporting problems.
  --%>
<%@ page language="java" pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<c:choose>
    <c:when test="${isSubmitted}">
        <p><spring:message code="views.report.submitted"/></p>
        <a class="btn btn-primary" href="${contextPath}/"><spring:message code="views.button.back"/></a>
    </c:when>
    <c:otherwise>
        <tag:reportForm/>
    </c:otherwise>
</c:choose>
