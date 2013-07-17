<%--
  -- Wizard page for confirmation of a new room or new capacity for permanent room.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<h1><spring:message code="views.wizard.confirmation"/></h1>
<hr/>
<p><spring:message code="views.wizard.confirmation.question"/></p>
<hr/>