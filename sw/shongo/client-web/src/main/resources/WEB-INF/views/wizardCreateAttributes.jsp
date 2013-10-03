<%--
  -- Wizard page for editing attributes of a new room or new capacity for permanent room.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<h1><spring:message code="views.wizard.createAttributes.${reservationRequest.specificationType}"/></h1>

<hr/>

<div ng-app="tag:reservationRequestForm">

    <tag:reservationRequestForm confirmUrl="" permanentRooms="${permanentRooms}"/>

</div>

<hr/>