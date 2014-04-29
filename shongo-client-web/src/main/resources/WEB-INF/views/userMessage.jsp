<%--
  -- Page which is shows message.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<tag:url var="backUrl" value="${requestScope.backUrl}"/>

<div>
    <h1><spring:message code="${titleCode}"/></h1>
    <p><spring:message code="${messageCode}"/></p>
    <div>
        <a class="btn btn-primary" href="${backUrl}"><spring:message code="views.button.back"/></a>
    </div>
</div>

