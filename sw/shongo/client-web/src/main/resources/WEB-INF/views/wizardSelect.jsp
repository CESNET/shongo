<%--
  -- Wizard page for selecting action to perform.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="urlReservations">
    ${contextPath}<%= ClientWebUrl.WIZARD_RESERVATION_REQUEST_LIST %>
</c:set>
<c:set var="urlCreateRoom">
    ${contextPath}<%= ClientWebUrl.WIZARD_CREATE_ROOM %>
</c:set>
<c:set var="urlCreatePermanentRoomCapacity">
    ${contextPath}<%= ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY %>
</c:set>

<div class="actions">
    <span><spring:message code="views.wizard.select"/></span>
    <ul>
        <li><a href="${urlCreateRoom}">
            <spring:message code="views.wizard.select.createRoom"/>
        </a></li>
        <li><a href="${urlCreatePermanentRoomCapacity}">
            <spring:message code="views.wizard.select.createPermanentRoomCapacity"/>
        </a></li>
        <li><a href="${urlReservations}">
            <spring:message code="views.wizard.select.reservationRequestList"/>
        </a></li>
    </ul>
</div>