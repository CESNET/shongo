
<%--
  -- Detail of reservation request.
  --%>
<%@ tag body-content="empty" %>
<%@ tag import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="reservationRequest" required="false"
             type="cz.cesnet.shongo.client.web.models.ReservationRequestModel" %>
<%@attribute name="detailUrl" required="false" %>

<tag:url var="userListUrl" value="<%= ClientWebUrl.USER_LIST_DATA %>"/>

<script type="text/javascript">
    angular.provideModule('tag:reservationRequestDetail', ['ngTooltip', 'ngResource', 'ngSanitize']);

    function DynamicStateController($scope, $application, $sce) {
        $scope.formatGroup = function(groupId, event) {
            $.ajax("${userListUrl}?groupId=" + groupId, {
                dataType: "json"
            }).done(function (data) {
                content = "<b><spring:message code="views.userRole.groupMembers"/>:</b><br/>";
                content += $application.formatUsers(data, "<spring:message code="views.userRole.groupMembers.none"/>");
                event.setResult(content);
            }).fail($application.handleAjaxFailure);
            return "<spring:message code="views.loading"/>";
        };
    }

    function MoreDetailController($scope) {
        if (window.reservationRequestDetail == null) {
            window.reservationRequestDetail = {
                show: false
            };
        }
        $scope.$context = window.reservationRequestDetail;
    }
</script>

<dl class="dl-horizontal" ng-controller="DynamicStateController">

    <%-- Type --%>
    <dt><spring:message code="views.reservationRequest.type"/>:</dt>
    <dd>
        <spring:message code="views.reservationRequest.specification.${reservationRequest.specificationType}" var="specificationType"/>
        <tag:help label="${specificationType}">
            <spring:message code="views.reservationRequest.specificationHelp.${reservationRequest.specificationType}"/>
        </tag:help>
    </dd>

    <%-- Event from request --%>
    <c:if test="${not empty reservationRequest.parentReservationRequestId}">
        <dt><spring:message code="views.reservationRequest.parentIdentifier"/>:</dt>
        <dd>
            <c:choose>
            <c:when test="${not empty detailUrl}">
                <tag:url var="parentReservationRequestDetailUrl" value="${detailUrl}">
                    <tag:param name="objectId" value="${reservationRequest.parentReservationRequestId}"/>
                </tag:url>
                <a href="${parentReservationRequestDetailUrl}">${reservationRequest.parentReservationRequestId}</a>
            </c:when>
                <c:otherwise>
                    ${reservationRequest.parentReservationRequestId}
                </c:otherwise>
            </c:choose>
        </dd>
    </c:if>

    <%-- Technology --%>
    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM' || reservationRequest.specificationType == 'ADHOC_ROOM'}">
        <dt><spring:message code="views.reservationRequest.technology"/>:</dt>
        <dd>${reservationRequest.technology.title}</dd>
    </c:if>

    <%-- Room name --%>
    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM' || (reservationRequest.specificationType == 'ADHOC_ROOM' && not empty reservationRequest.roomName)}">
        <dt><spring:message code="views.reservationRequest.specification.roomName"/>:</dt>
        <dd>${reservationRequest.roomName}</dd>
    </c:if>

    <%-- For permanent room --%>
    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM_CAPACITY'}">
        <dt><spring:message code="views.reservationRequest.specification.permanentRoomReservationRequestId"/>:</dt>
        <dd>
            <c:choose>
                <c:when test="${not empty detailUrl}">
                    <tag:url var="permanentRoomDetailUrl" value="${detailUrl}">
                        <tag:param name="objectId" value="${reservationRequest.permanentRoomReservationRequestId}"/>
                    </tag:url>
                    <a href="${permanentRoomDetailUrl}" tabindex="2">${reservationRequest.permanentRoomReservationRequest.roomName}</a>
                </c:when>
                <c:otherwise>
                    ${reservationRequest.permanentRoomReservationRequest.roomName}
                </c:otherwise>
            </c:choose>
        </dd>
    </c:if>

    <%-- Number of participants --%>
    <c:if test="${reservationRequest.specificationType == 'ADHOC_ROOM' || reservationRequest.specificationType == 'PERMANENT_ROOM_CAPACITY'}">
        <dt><spring:message code="views.reservationRequest.specification.roomParticipantCount"/>:</dt>
        <dd>${reservationRequest.roomParticipantCount}</dd>
    </c:if>

    <%-- Requested/allocated time slot --%>
    <c:choose>
        <c:when test="${reservationRequest.detail != null && reservationRequest.detail.allocationState == 'ALLOCATED' && reservationRequest.detail.room != null}">
            <dt><spring:message code="views.reservationRequest.room.slot"/>:</dt>
            <dd><tag:format value="${reservationRequest.detail.room.slot}" multiline="true" pre="${reservationRequest.detail.room.slotBefore}" post="${reservationRequest.detail.room.slotAfter}"/></dd>
        </c:when>
        <c:otherwise>
            <dt><spring:message code="views.reservationRequest.slot"/>:</dt>
            <dd><tag:format value="${reservationRequest.slot}" multiline="true"  pre="${reservationRequest.slotBefore}" post="${reservationRequest.slotAfter}"/></dd>
        </c:otherwise>
    </c:choose>

    <%-- Periodicity --%>
    <c:if test="${empty reservationRequest.parentReservationRequestId && reservationRequest.specificationType != 'PERMANENT_ROOM'}">
        <dt><spring:message code="views.reservationRequest.periodicity"/>:</dt>
        <dd>
            <spring:message code="views.reservationRequest.periodicity.${reservationRequest.periodicityType}"/>
            <c:if test="${reservationRequest.periodicityType != 'NONE' && reservationRequest.periodicityEnd != null}">
                (<spring:message code="views.reservationRequest.periodicity.until"/>&nbsp;<tag:format value="${reservationRequest.periodicityEnd}" style="date"/>)
            </c:if>
        </dd>
    </c:if>

    <%-- PIN --%>
    <c:if test="${not empty reservationRequest.roomPin}">
        <dt><spring:message code="views.reservationRequest.specification.roomPin"/>:</dt>
        <dd>${reservationRequest.roomPin}</dd>
    </c:if>

    <%-- Recorded --%>
    <c:if test="${reservationRequest.technology != 'ADOBE_CONNECT' && (reservationRequest.specificationType == 'ADHOC_ROOM' || reservationRequest.specificationType == 'PERMANENT_ROOM_CAPACITY')}">
        <dt><spring:message code="views.reservationRequest.specification.roomRecorded"/>:</dt>
        <dd><spring:message code="views.button.${reservationRequest.roomRecorded ? 'yes' : 'no'}"/></dd>
    </c:if>

    <%-- Description --%>
    <c:if test="${not empty reservationRequest.description}">
        <dt><spring:message code="views.reservationRequest.description"/>:</dt>
        <dd>${reservationRequest.description}</dd>
    </c:if>

    <%-- State --%>
    <c:if test="${reservationRequest.detail != null && reservationRequest.detail.state != null}">
        <dt><spring:message code="views.reservationRequest.state"/>:</dt>
        <dd class="reservation-request-state">
            <spring:message code="views.reservationRequest.state.${reservationRequest.specificationType}.${reservationRequest.detail.state}" var="stateLabel"/>
            <tag:help label="${stateLabel}" cssClass="${reservationRequest.detail.state}">
                ${reservationRequest.detail.stateHelp}
            </tag:help>
        </dd>
    </c:if>

    <%-- How to reach --%>
    <c:if test="${reservationRequest.detail != null && reservationRequest.detail.room != null && not empty reservationRequest.detail.room.aliases}">
        <dt><spring:message code="views.room.aliases"/>:</dt>
        <dd>
            <tag:help label="${reservationRequest.detail.room.aliases}" selectable="true">
                ${reservationRequest.detail.room.aliasesDescription}
            </tag:help>
        </dd>
    </c:if>

    <%-- User roles --%>
    <c:if test="${not empty reservationRequest.userRoles}">
        <dt><spring:message code="views.reservationRequest.userRoles"/>:</dt>
        <dd>
            <spring:message code="views.userRoleList.group" var="groupTitle"/>
            <spring:message code="views.userRoleList.user" var="userTitle"/>
            <c:forEach items="${reservationRequest.userRoles}" var="userRole" varStatus="status">
                <c:choose>
                    <c:when test="${userRole.identityType == 'GROUP'}">
                        <b class="icon-group" title="${groupTitle}"></b>
                        <tag:help label="${userRole.identityName}" content="formatGroup('${userRole.identityPrincipalId}', event)"/>
                    </c:when>
                    <c:otherwise>
                        <b class="icon-user" title="${userTitle}"></b>
                        ${userRole.identityName}
                    </c:otherwise>
                </c:choose>
                (<spring:message code="views.userRole.objectRole.${userRole.role}"/>)<c:if test="${!status.last}">, </c:if>
            </c:forEach>
        </dd>
    </c:if>

    <%-- Participants --%>
    <dt><spring:message code="views.reservationRequest.participants"/>:</dt>
    <dd>
        <c:forEach items="${reservationRequest.roomParticipants}" var="participant" varStatus="status">
            ${participant.name} (<spring:message code="views.participant.role.${participant.role}"/>)<c:if test="${!status.last}">, </c:if>
        </c:forEach>
        <c:if test="${empty reservationRequest.roomParticipants}">
            <spring:message code="views.reservationRequest.participants.none"/>
        </c:if>
    </dd>

    <c:if test="${reservationRequest.roomParticipantNotificationEnabled}">
        <dt><spring:message code="views.reservationRequest.specification.roomParticipantNotificationEnabled"/>:</dt>
        <dd>
            <spring:message code="views.button.yes"/>
        </dd>
        <dt><spring:message code="views.reservationRequest.specification.roomMeetingName"/>:</dt>
        <dd>
            ${reservationRequest.roomMeetingName}
        </dd>
    </c:if>

    <%-- Created --%>
    <c:if test="${not empty reservationRequest.dateTime}">
        <dt><spring:message code="views.reservationRequest.dateTime"/>:</dt>
        <dd><tag:format value="${reservationRequest.dateTime}"/></dd>
    </c:if>

    <%-- Show more detail --%>
    <c:if test="${reservationRequest.detail != null}">

        <div ng-controller="MoreDetailController">

            <div ng-show="$context.show">

                <hr/>

                <c:if test="${reservationRequest.detail.allocationState != null}">
                    <dt><spring:message code="views.reservationRequest.allocationState"/>:</dt>
                    <dd class="allocation-state">
                        <spring:message code="views.reservationRequest.allocationState.${reservationRequest.detail.allocationState}" var="allocationStateLabel"/>
                        <tag:help label="${allocationStateLabel}" cssClass="${reservationRequest.detail.allocationState}">
                            ${reservationRequest.detail.allocationStateHelp}
                        </tag:help>
                    </dd>
                </c:if>

                <c:if test="${reservationRequest.detail.room != null}">
                    <dt><spring:message code="views.room.state"/>:</dt>
                    <dd class="room-state">
                        <spring:message code="views.executable.roomState.${reservationRequest.detail.room.type}.${reservationRequest.detail.room.state}" var="roomStateLabel"/>
                        <tag:help label="${roomStateLabel}" cssClass="${reservationRequest.detail.room.state}">
                            <span><spring:message code="views.executable.roomStateHelp.${reservationRequest.detail.room.type}.${reservationRequest.detail.room.state}"/></span>
                            <c:if test="${not empty reservationRequest.detail.room.stateReport}">
                                <pre>
                                    ${reservationRequest.detail.room.stateReport}
                                </pre>
                            </c:if>
                        </tag:help>
                    </dd>
                </c:if>

                <c:choose>
                    <c:when test="${reservationRequest.detail.allocationState != 'ALLOCATED' && reservationRequest.detail.room != null}">
                        <dt><spring:message code="views.reservationRequest.room.slot"/>:</dt>
                        <dd><tag:format value="${reservationRequest.detail.room.slot}" multiline="true" pre="${reservationRequest.detail.room.slotBefore}" post="${reservationRequest.detail.room.slotAfter}"/></dd>
                    </c:when>
                    <c:when test="${reservationRequest.detail.allocationState == 'ALLOCATED' && reservationRequest.detail.room != null}">
                        <dt><spring:message code="views.reservationRequest.slot"/>:</dt>
                        <dd><tag:format value="${reservationRequest.slot}" multiline="true"  pre="${reservationRequest.slotBefore}" post="${reservationRequest.slotAfter}"/></dd>
                    </c:when>

                </c:choose>

                <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM_CAPACITY' && reservationRequest.detail.room != null}">
                    <dt><spring:message code="views.reservationRequest.room.name"/>:</dt>
                    <dd>${reservationRequest.detail.room.name}</dd>
                </c:if>

                <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM' && reservationRequest.detail.room != null}">
                    <dt><spring:message code="views.reservationRequest.room.licenseCount"/>:</dt>
                    <dd>${reservationRequest.detail.room.licenseCount}</dd>
                </c:if>

                <c:if test="${not empty reservationRequest.id}">
                    <dt><spring:message code="views.reservationRequest.identifier"/>:</dt>
                    <dd>${reservationRequest.id}</dd>
                </c:if>

            </div>

            <dt></dt>
            <dd>
                <a href="" ng-click="$context.show = true" ng-show="!$context.show"><spring:message code="views.button.showMoreDetail"/></a>
                <a href="" ng-click="$context.show = false" ng-show="$context.show"><spring:message code="views.button.hideMoreDetail"/></a>
            </dd>

        </div>

    </c:if>

</dl>