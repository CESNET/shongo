<%--
  -- Wizard page for editing attributes of a new room or new capacity for permanent room.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="app" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<h1><spring:message code="views.wizard.createAttributes.${reservationRequest.specificationType}"/></h1>
<hr/>
<app:reservationRequestForm confirmUrl="${formUrl}" permanentRooms="${permanentRooms}"/>
<hr/>