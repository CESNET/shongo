<%--
  -- Wizard page for managing participants.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<tag:url var="createUrl" value="${createUrl}"/>
<tag:url var="modifyUrl" value="${modifyUrl}"/>
<tag:url var="deleteUrl" value="${deleteUrl}"/>

<h1><spring:message code="views.wizard.createParticipants"/></h1>

<p><spring:message code="views.wizard.createParticipants.help"/></p>

<hr/>

<tag:participantList data="${reservationRequest.roomParticipants}" createUrl="${createUrl}" modifyUrl="${modifyUrl}" deleteUrl="${deleteUrl}"/>

<hr/>