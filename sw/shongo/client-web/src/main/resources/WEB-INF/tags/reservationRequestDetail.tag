<%--
  -- Reservation request form.
  --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
        <spring:message code="views.reservationRequest.specification.${reservationRequest.specificationType}"/>
        <tag:help>
            <spring:message
                    code="views.help.reservationRequest.specification.${reservationRequest.specificationType}"/>
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
        <dt><spring:message code="views.reservationRequest.specification.permanentRoomCapacityReservationRequestId"/>:
        </dt>
        <dd>
            <c:choose>
                <c:when test="${reservationRequest.permanentRoomCapacityReservationRequestId != null}">
                    <spring:eval var="urlDetail"
                                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, reservationRequest.permanentRoomCapacityReservationRequestId)"/>
                    <a href="${urlDetail}">${permanentRoomReservationRequest.specification.value}</a>
                </c:when>
                <c:otherwise>
                    <spring:message code="views.reservationRequest.specification.roomAlias.adhoc"/>
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
        <joda:format value="${reservationRequest.start}" style="MM"/>
        <br/>
        <joda:format value="${reservationRequest.end}" style="MM"/>
    </dd>

    <c:if test="${parentReservationRequestId == null}">
        <dt><spring:message code="views.reservationRequest.periodicity"/>:</dt>
        <dd>
            <spring:message code="views.reservationRequest.periodicity.${reservationRequest.periodicityType}"/>
            <c:if test="${reservationRequest.periodicityType != 'NONE' && reservationRequest.periodicityEnd != null}">
                (<spring:message code="views.reservationRequest.periodicity.until"/> <joda:format
                    value="${reservationRequest.periodicityEnd}" style="M-"/>)
            </c:if>
        </dd>
    </c:if>

    <c:if test="${reservationRequest.roomPin != null}">
        <dt><spring:message code="views.reservationRequest.specification.roomPin"/>:</dt>
        <dd>${reservationRequest.roomPin}</dd>
    </c:if>

    <dt><spring:message code="views.reservationRequest.description"/>:</dt>
    <dd>${reservationRequest.description}</dd>

    <dt><spring:message code="views.reservationRequest.purpose"/>:</dt>
    <dd>
        <spring:message code="views.reservationRequest.purpose.${reservationRequest.purpose}"/>
    </dd>

    <c:if test="${reservationRequest.dateTime != null}">
        <dt><spring:message code="views.reservationRequest.dateTime"/>:</dt>
        <dd><joda:format value="${reservationRequest.dateTime}" style="MM"/></dd>
    </c:if>

    <c:if test="${reservationRequest.id != null}">
        <dt><spring:message code="views.reservationRequest.identifier"/>:</dt>
        <dd>${reservationRequest.id}</dd>
    </c:if>

    <c:if test="${parentReservationRequestId != null}">
        <dt><spring:message code="views.reservationRequest.parentIdentifier"/>:</dt>
        <dd>
            <spring:eval var="urlDetail"
                         expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, parentReservationRequestId)"/>
            <a href="${urlDetail}">${parentReservationRequestId}</a>
        </dd>
    </c:if>

</dl>