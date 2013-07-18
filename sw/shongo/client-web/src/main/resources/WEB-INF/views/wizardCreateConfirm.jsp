<%--
  -- Wizard page for confirmation of a new room or new capacity for permanent room.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<h1><spring:message code="views.wizard.confirmation"/></h1>

<hr/>

<p><spring:message code="views.wizard.confirmation.question"/></p>

<div ng-app="tag:reservationRequestDetail">

    <tag:reservationRequestDetail reservationRequest="${reservationRequest}" detailUrl="${detailUrl}"/>

</div>

<hr/>