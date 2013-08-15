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

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="reservationRequestDetail" value="${reservationRequest.detail}"/>

<script type="text/javascript">
    angular.provideModule('tag:reservationRequestDetail', ['ngTooltip']);

    function MoreDetailController($scope) {
        $scope.show = false;
    }
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
        <c:when test="${reservationRequestDetail.allocationState == 'ALLOCATED' && reservationRequestDetail.room != null}">
            <%-- Allocated slot is shown --%>
            <c:set var="reservationRequestSlot" value="${reservationRequestDetail.room.slot}"/>
            <spring:message var="reservationRequestSlotLabel" code="views.reservationRequest.room.slot"/>

            <%-- Requested slot is shown in more detail --%>
            <c:set var="reservationRequestDetailSlot" value="${reservationRequest.slot}"/>
            <spring:message var="reservationRequestDetailSlotLabel" code="views.reservationRequest.slot"/>
        </c:when>
        <c:otherwise>
            <%-- Requested slot is shown --%>
            <c:set var="reservationRequestSlot" value="${reservationRequest.slot}"/>
            <spring:message code="views.reservationRequest.slot" var="reservationRequestSlotLabel"/>

            <%-- Allocated slot is shown in more detail --%>
            <c:if test="${reservationRequestDetail.room != null}">
                <c:set var="reservationRequestDetailSlot" value="${reservationRequestDetail.room.slot}"/>
                <spring:message var="reservationRequestDetailSlotLabel" code="views.reservationRequest.room.slot"/>
            </c:if>
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
        <dd class="reservation-request-state">
            <spring:message code="views.reservationRequest.state.${reservationRequestDetail.state}" var="state"/>
            <tag:help label="${state}" labelClass="${reservationRequestDetail.state}">
                <spring:message code="help.reservationRequest.state.${reservationRequestDetail.state}"/>
            </tag:help>
            <c:if test="${reservationRequestDetail.room != null && reservationRequestDetail.room.state.available}">
                <spring:eval var="urlRoomManagement"
                             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getRoomManagement(contextPath, reservationRequestDetail.room.id)"/>
                (<a href="${urlRoomManagement}"><spring:message code="views.list.action.manage"/></a>
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

        <div ng-controller="MoreDetailController">

            <div ng-show="show">

                <hr/>

                <c:if test="${reservationRequestDetail.allocationState != null}">
                    <dt><spring:message code="views.reservationRequest.allocationState"/>:</dt>
                    <dd class="allocation-state">
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
                    <dd class="room-state">
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

                <c:if test="${not empty reservationRequestDetailSlot}">
                    <dt>${reservationRequestDetailSlotLabel}:</dt>
                    <dd>
                        <joda:format value="${reservationRequestDetailSlot.start}" style="MM"/>
                        <br/>
                        <joda:format value="${reservationRequestDetailSlot.end}" style="MM"/>
                    </dd>
                </c:if>

                <c:if test="${reservationRequestDetail.room != null}">
                    <c:if test="${not empty reservationRequest.room.name && reservationRequest.specificationType != 'PERMANENT_ROOM_CAPACITY'}">
                        <dt><spring:message code="views.reservationRequest.room.name"/>:</dt>
                        <dd>${reservationRequest.room.name}</dd>
                    </c:if>

                    <c:if test="${not empty reservationRequest.roomParticipantCount}">
                        <dt><spring:message code="views.reservationRequest.room.licenseCount"/>:</dt>
                        <dd>${reservationRequestDetail.room.licenseCount}</dd>
                    </c:if>
                </c:if>

                <c:if test="${not empty reservationRequest.id}">
                    <dt><spring:message code="views.reservationRequest.identifier"/>:</dt>
                    <dd>${reservationRequest.id}</dd>
                </c:if>

            </div>

            <dt></dt>
            <dd>
                <a href="" ng-click="show = true" ng-show="!show"><spring:message code="views.button.showMoreDetail"/></a>
                <a href="" ng-click="show = false" ng-show="show"><spring:message code="views.button.hideMoreDetail"/></a>
            </dd>

        </div>

    </c:if>

</dl>