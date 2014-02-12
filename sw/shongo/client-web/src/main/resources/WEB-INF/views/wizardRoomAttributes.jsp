<%--
  -- Wizard page for editing attributes of a new room or new capacity for permanent room.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:choose>
    <c:when test="${not empty reservationRequest.id}">
        <h1><spring:message code="views.wizard.room.attributes.modify.${reservationRequest.specificationType}"/></h1>
    </c:when>
    <c:otherwise>
        <h1><spring:message code="views.wizard.room.attributes.create.${reservationRequest.specificationType}"/></h1>
    </c:otherwise>
</c:choose>

<hr/>

<script type="text/javascript">
    var module = angular.module('jsp:wizardRoomAttributes', ['ngApplication', 'tag:reservationRequestForm']);
</script>

<div ng-app="jsp:wizardRoomAttributes">

    <tag:reservationRequestForm permanentRooms="${permanentRooms}"/>

</div>

<hr/>