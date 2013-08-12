<%--
  -- Detail of reservation request.
  --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="reservationRequest" required="false"
             type="cz.cesnet.shongo.client.web.models.ReservationRequestModel" %>
<%@attribute name="detailUrl" required="false" %>

<script type="text/javascript">
    angular.provideModule('tag:reservationRequestDetail', ['ngTooltip']);
</script>

<dl class="dl-horizontal">

    <dt><spring:message code="views.reservationRequest.type"/>:</dt>
    <dd>
        <spring:message code="views.reservationRequest.specification.${reservationRequest.specificationType}" var="specificationType"/>
        <tag:help label="${specificationType}">
            <spring:message code="help.reservationRequest.specification.${reservationRequest.specificationType}"/>
        </tag:help>
    </dd>

    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM' || reservationRequest.specificationType == 'ADHOC_ROOM'}">
        <dt><spring:message code="views.reservationRequest.technology"/>:</dt>
        <dd>${reservationRequest.technology.title}</dd>
    </c:if>

    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
        <dt><spring:message code="views.reservationRequest.specification.permanentRoomName"/>:</dt>
        <dd>${reservationRequest.permanentRoomName}</dd>
    </c:if>

    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM_CAPACITY'}">
        <dt><spring:message code="views.reservationRequest.specification.permanentRoomReservationRequestId"/>:</dt>
        <dd>
            <c:choose>
                <c:when test="${not empty detailUrl}">
                    <spring:eval var="permanentRoomDetailUrl"
                                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, reservationRequest.permanentRoomReservationRequestId)"/>
                    <a href="${permanentRoomDetailUrl}" tabindex="2">${reservationRequest.permanentRoomReservationRequest.specification.value}</a>
                </c:when>
                <c:otherwise>
                    ${reservationRequest.permanentRoomReservationRequest.specification.value}
                </c:otherwise>
            </c:choose>
        </dd>
    </c:if>

    <c:if test="${reservationRequest.specificationType == 'ADHOC_ROOM' || reservationRequest.specificationType == 'PERMANENT_ROOM_CAPACITY'}">
        <dt><spring:message code="views.reservationRequest.specification.roomParticipantCount"/>:</dt>
        <dd>${reservationRequest.roomParticipantCount}</dd>
    </c:if>

    <dt><spring:message code="views.reservationRequest.slot"/>:</dt>
    <dd>
        <c:set var="reservationRequestSlot" value="${reservationRequest.slot}"/>
        <joda:format value="${reservationRequestSlot.start}" style="MM"/>
        <br/>
        <joda:format value="${reservationRequestSlot.end}" style="MM"/>
    </dd>

    <c:if test="${empty reservationRequest.parentReservationRequestId && reservationRequest.specificationType != 'PERMANENT_ROOM'}">
        <dt><spring:message code="views.reservationRequest.periodicity"/>:</dt>
        <dd>
            <spring:message code="views.reservationRequest.periodicity.${reservationRequest.periodicityType}"/>
            <c:if test="${reservationRequest.periodicityType != 'NONE' && reservationRequest.periodicityEnd != null}">
                (<spring:message code="views.reservationRequest.periodicity.until"/>&nbsp;<joda:format value="${reservationRequest.periodicityEnd}" style="M-"/>)
            </c:if>
        </dd>
    </c:if>

    <c:if test="${not empty reservationRequest.roomPin}">
        <dt><spring:message code="views.reservationRequest.specification.roomPin"/>:</dt>
        <dd>${reservationRequest.roomPin}</dd>
    </c:if>

    <dt><spring:message code="views.reservationRequest.description"/>:</dt>
    <dd>${reservationRequest.description}</dd>

    <dt><spring:message code="views.reservationRequest.purpose"/>:</dt>
    <dd>
        <spring:message code="views.reservationRequest.purpose.${reservationRequest.purpose}"/>
    </dd>

    <c:if test="${reservationRequest.allocationState != null}">
        <dt><spring:message code="views.reservationRequest.allocationState"/>:</dt>
        <dd class="reservation-request-allocation-state">
            <spring:message code="views.reservationRequest.allocationState.${reservationRequest.allocationState}" var="allocationState"/>
            <tag:help label="${allocationState}" labelClass="${reservationRequest.allocationState}">
                <span>
                    <spring:message code="help.reservationRequest.allocationState.${reservationRequest.allocationState}"/>
                </span>
                <c:if test="${reservationRequest.allocationState == 'ALLOCATION_FAILED' && not empty reservationRequest.allocationStateReport}">
                    <pre>${reservationRequest.allocationStateReport}</pre>
                </c:if>
            </tag:help>
        </dd>
    </c:if>

    <c:if test="${not empty reservationRequest.dateTime}">
        <dt><spring:message code="views.reservationRequest.dateTime"/>:</dt>
        <dd><joda:format value="${reservationRequest.dateTime}" style="MM"/></dd>
    </c:if>

    <c:if test="${not empty reservationRequest.id}">
        <dt><spring:message code="views.reservationRequest.identifier"/>:</dt>
        <dd>${reservationRequest.id}</dd>
    </c:if>

    <c:if test="${not empty reservationRequest.parentReservationRequestId}">
        <dt><spring:message code="views.reservationRequest.parentIdentifier"/>:</dt>
        <dd>
            <spring:eval var="urlDetail"
                         expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, reservationRequest.parentReservationRequestId)"/>
            <a href="${urlDetail}">${reservationRequest.parentReservationRequestId}</a>
        </dd>
    </c:if>

</dl>