<%--
  -- Development page.
  --%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<style type="text/css">
    td {
        padding: 0px 5px;
    }
    td:first-child {
        white-space: nowrap;
    }
    td:last-child {
        color: gray;
    }
    tr {
        border: 1px solid lightgray;
    }
</style>
<h2>AllocationState</h2>
<div class="allocation-state">
    <table>
        <c:forEach items="NOT_ALLOCATED,ALLOCATED,ALLOCATION_FAILED" var="state">
            <tr>
                <td class="${state}"><spring:message code="views.reservationRequest.allocationState.${state}"/></td>
                <td><spring:message code="views.reservationRequest.allocationStateHelp.${state}"/></td>
                <td>${state}</td>
            </tr>
        </c:forEach>
    </table>
</div>

<h2>ReservationRequestState</h2>
<c:forEach items="ADHOC_ROOM,PERMANENT_ROOM,PERMANENT_ROOM_CAPACITY" var="specificationType">
    <strong>SpecificationType.${specificationType}</strong>
    <div class="reservation-request-state">
        <table>
            <c:forEach items="NOT_ALLOCATED,ALLOCATED,ALLOCATED_STARTED,ALLOCATED_STARTED_NOT_AVAILABLE,ALLOCATED_STARTED_AVAILABLE,ALLOCATED_FINISHED,FAILED,MODIFICATION_FAILED" var="state">
                <c:if test="${(!state.startsWith('ALLOCATED_STARTED_') || specificationType == 'PERMANENT_ROOM') && (state != 'ALLOCATED_STARTED' || specificationType != 'PERMANENT_ROOM')}">
                    <tr>
                        <td class="${state}"><spring:message code="views.reservationRequest.state.${specificationType}.${state}"/></td>
                        <td><spring:message code="views.reservationRequest.stateHelp.${specificationType}.${state}"/></td>
                        <td>${state}</td>
                    </tr>
                </c:if>
            </c:forEach>
        </table>
    </div>
    <br/>
</c:forEach>

<h2>RoomState</h2>
<c:forEach items="ADHOC_ROOM,PERMANENT_ROOM,USED_ROOM" var="roomType">
    <strong>RoomType.${roomType}</strong>
    <div class="room-state">
        <table>
            <c:forEach items="NOT_STARTED,STARTED,STARTED_NOT_AVAILABLE,STARTED_AVAILABLE,STOPPED,FAILED" var="state">
                <c:if test="${(!state.startsWith('STARTED_') || roomType == 'PERMANENT_ROOM') && (state != 'STARTED' || roomType != 'PERMANENT_ROOM')}">
                    <tr>
                        <td class="${state}"><spring:message code="views.executable.roomState.${roomType}.${state}"/></td>
                        <td><spring:message code="views.executable.roomStateHelp.${roomType}.${state}"/></td>
                        <td>${state}</td>
                    </tr>
                </c:if>
            </c:forEach>
        </table>
    </div>
    <br/>
</c:forEach>