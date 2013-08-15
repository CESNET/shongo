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

<c:set var="reservationRequestDetail" value="${reservationRequest.detail}"/>

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

    <c:if test="${not empty reservationRequest.parentReservationRequestId}">
        <dt><spring:message code="views.reservationRequest.parentIdentifier"/>:</dt>
        <dd>
            <spring:eval var="urlDetail"
                         expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, reservationRequest.parentReservationRequestId)"/>
            <a href="${urlDetail}">${reservationRequest.parentReservationRequestId}</a>
        </dd>
    </c:if>

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

    <c:choose>
        <c:when test="${reservationRequestDetail.allocationState == 'ALLOCATED'}">
            <c:set var="reservationRequestSlot" value="${reservationRequestDetail.reservationSlot}"/>
            <c:set var="reservationRequestSlotLabel" value="allocatedSlot"/>
        </c:when>
        <c:otherwise>
            <c:set var="reservationRequestSlot" value="${reservationRequest.slot}"/>
            <c:set var="reservationRequestSlotLabel" value="requestedSlot"/>
        </c:otherwise>
    </c:choose>

    <dt>${reservationRequestSlotLabel}:</dt>
    <dd>
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

    <c:if test="${reservationRequestDetail != null}">
        <dt><spring:message code="views.reservationRequest.state"/>:</dt>
        <dd class="reservation-request-allocation-state">
                ${reservationRequestDetail.state}
            <c:if test="${reservationRequestDetail.room != null && reservationRequestDetail.room.state.available}">
                <spring:eval var="urlRoomManagement"
                             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getRoomManagement(contextPath, reservationRequestDetail.room.id)"/>
                <a class="btn" href="${urlRoomManagement}">
                    <spring:message code="views.list.action.manage"/>
                </a>
            </c:if>
        </dd>

        <c:if test="${reservationRequestDetail.room != null}">
            <dt><spring:message code="views.room.aliases"/>:</dt>
            <dd>
                <tag:help label="${reservationRequestDetail.room.aliases}">
                    <c:if test="${not empty reservationRequestDetail.room.aliasesDescription}">
                        ${reservationRequestDetail.room.aliasesDescription}
                    </c:if>
                </tag:help>
            </dd>
        </c:if>
    </c:if>

    <c:if test="${not empty reservationRequest.dateTime}">
        <dt><spring:message code="views.reservationRequest.dateTime"/>:</dt>
        <dd><joda:format value="${reservationRequest.dateTime}" style="MM"/></dd>
    </c:if>

    <c:if test="${reservationRequestDetail != null}">

        <div style="border: 1px solid">

            More details:<br>

            <c:if test="${reservationRequestDetail.allocationState != null}">
                <dt><spring:message code="views.reservationRequest.allocationState"/>:</dt>
                <dd class="reservation-request-allocation-state">
                    <spring:message code="views.reservationRequest.allocationState.${reservationRequestDetail.allocationState}" var="allocationState"/>
                    <tag:help label="${allocationState}" labelClass="${reservationRequestDetail.allocationState}">
                        <span>
                            <spring:message code="help.reservationRequest.allocationState.${reservationRequestDetail.allocationState}"/>
                        </span>
                        <c:if test="${reservationRequestDetail.allocationState == 'ALLOCATION_FAILED' && not empty reservationRequestDetail.allocationStateReport}">
                            <pre>${reservationRequestDetail.allocationStateReport}</pre>
                        </c:if>
                    </tag:help>
                </dd>
            </c:if>

            <c:if test="${reservationRequestDetail.room != null}">
                <dt><spring:message code="views.room.state"/>:</dt>
                <dd class="executable-state">
                    <c:if test="${reservationRequestDetail.room.state != null}">
                        <spring:message code="views.executable.roomState.${reservationRequestDetail.room.state}" var="roomState"/>
                        <tag:help label="${roomState}" labelClass="${reservationRequestDetail.room.state}">
                                <span>
                                    <spring:message code="help.executable.roomState.${reservationRequestDetail.room.state}"/>
                                </span>
                            <c:if test="${not empty reservationRequestDetail.room.stateReport}">
                                <pre>${reservationRequestDetail.room.stateReport}</pre>
                            </c:if>
                        </tag:help>
                    </c:if>
                </dd>
            </c:if>

            <c:if test="${reservationRequestDetail.allocationState == 'ALLOCATED'}">
                <dt>requestedSlot:</dt>
                <dd class="reservation-request-allocation-state">
                    <joda:format value="${reservationRequest.slot.start}" style="MM"/>
                    <br/>
                    <joda:format value="${reservationRequest.slot.end}" style="MM"/>
                </dd>

                <c:if test="${not empty reservationRequest.permanentRoomName}">
                    <dt>allocatedRoomName:</dt>
                    <dd>${reservationRequest.permanentRoomName}</dd>
                </c:if>

                <c:if test="${not empty reservationRequest.roomParticipantCount}">
                    <dt>allocatedParticipants:</dt>
                    <dd>${reservationRequest.roomParticipantCount}</dd>
                </c:if>
            </c:if>

            <c:if test="${not empty reservationRequest.id}">
                <dt><spring:message code="views.reservationRequest.identifier"/>:</dt>
                <dd>${reservationRequest.id}</dd>
            </c:if>

        </div>
    </c:if>

</dl>