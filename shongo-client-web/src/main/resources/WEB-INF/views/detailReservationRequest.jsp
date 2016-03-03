<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%--<security:authorize access="hasPermission(RESERVATION)">--%>
    <security:accesscontrollist hasPermission="PROVIDE_RESERVATION_REQUEST"
                                domainObject="${reservationRequest}" var="canCreatePermanentRoomCapacity"/>
<%--</security:authorize>--%>
<c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
    <c:set var="canCreatePermanentRoomCapacity" value="${false}"/>
</c:if>

<tag:url var="detailUrl" value="<%= ClientWebUrl.DETAIL_VIEW %>"/>

<script type="text/javascript">
    function DetailReservationRequestController($scope) {
        /**
         * Reservation request state url.
         */
        <tag:url var="detailReservationRequestStateUrl" value="<%= ClientWebUrl.DETAIL_RESERVATION_REQUEST_STATE %>">
            <tag:param name="objectId" value=":reservationRequestId"/>
            <tag:param name="isLatestAllocated" value=":isLatestAllocated"/>
        </tag:url>
        var RESERVATION_REQUEST_STATE_URL = "${detailReservationRequestStateUrl}";

        /**
         * Specifies whether current reservation request is last version which can be modified.
         */
        $scope.reservationRequest.isActive = ${isActive};

        /**
         * Set version of reservation request for the state.
         */
        $scope.setReservationRequest = function(reservationRequestId, isActive, isLatestAllocated) {
            var url = null;
            if (reservationRequestId != null) {
                $scope.reservationRequest.historyItemId = reservationRequestId;
                $scope.reservationRequest.isActive = isActive;
                if (isLatestAllocated) {
                    // Check whether reservation request is still latest allocated (doesn't exist any newer version which become allocated)
                    if (reservationRequestId != $scope.reservationRequest.id && $scope.reservationRequest.allocationState == 'ALLOCATED') {
                        isLatestAllocated = false;
                    }
                }
                url = RESERVATION_REQUEST_STATE_URL;
                url = url.replace(":reservationRequestId", reservationRequestId);
                url = url.replace(":isLatestAllocated", (isLatestAllocated ? true : false));
            }
            $scope.refreshReservationRequestState(url);
        };

        /**
         * Refresh reservation request state.
         */
        $scope.refreshReservationRequestState = function(url) {
            var reservationRequestStateScope = angular.element("#reservationRequestState").scope();
            reservationRequestStateScope.refresh(url);
            $scope.setupAutoRefresh();
        };

        /**
         * Check if auto refresh is needed.
         */
        $scope.isAutoRefreshNeeded = function(){
            return true;
            if ($scope.reservationRequest.isPeriodic) {
                // Periodic events don't need to refresh (their state isn't changed)
                return false;
            }
            if (!$scope.reservationRequest.isActive) {
                // Old version of reservation request, so we don't need to refresh
                return false;
            }
            if ($scope.reservationRequest.allocationState == 'NOT_ALLOCATED') {
                // Not allocated, so we need to refresh
                return true;
            }
            if (($scope.reservationRequest.roomState != 'STOPPED' && $scope.reservationRequest.roomState != 'FAILED')) {
                // Not finished, so we need to refresh
                return true;
            }
            return false;
        }

        /**
         * Setup automatic refresh of reservation request state.
         */
        $scope.setupAutoRefresh = function(){
            if ($scope.isAutoRefreshNeeded()) {
                $scope.setRefreshTimeout(function(){
                    if ($scope.isAutoRefreshNeeded()) {
                        $scope.refreshReservationRequestState();
                    }
                });
            }
        };
        $scope.setupAutoRefresh();
    }
</script>

<div ng-controller="DetailReservationRequestController">

<%-- History --%>
<c:if test="${history != null}">
    <div class="bordered jspReservationRequestDetailHistory">
        <h2><spring:message code="views.reservationRequestDetail.history"/></h2>
        <table class="table table-striped table-hover">
            <thead>
            <tr>
                <th><spring:message code="views.reservationRequest.createdAt"/></th>
                <th><spring:message code="views.reservationRequest.createdBy"/></th>
                <th><spring:message code="views.reservationRequest.type"/></th>
                <c:if test="${reservationRequest.state != null}">
                    <th><spring:message code="views.reservationRequest.state"/></th>
                </c:if>
                <th><spring:message code="views.list.action"/></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${history}" var="historyItem" varStatus="status">
                <tr ng-class="{active: ${historyItem.type != 'DELETED'} && reservationRequest.historyItemId == '${historyItem.id}'}">
                <td><tag:format value="${historyItem.dateTime}" styleShort="true"/></td>
                <td>${historyItem.user}</td>
                <td><spring:message code="views.reservationRequest.type.${historyItem.type}"/></td>
                <c:if test="${reservationRequest.state != null}">
                    <td class="reservation-request-state">
                        <c:if test="${historyItem.state != null}">
                            <span ng-show="reservationRequest.id == '${historyItem.id}' && reservationRequest.state">
                                <span class="{{reservationRequest.state}}">{{reservationRequest.stateLabel}}</span>
                            </span>
                            <span ng-show="reservationRequest.id != '${historyItem.id}' || !reservationRequest.state">
                                <span class="${historyItem.state}">
                                    <spring:message code="views.reservationRequest.state.${reservationRequest.specificationType}.${historyItem.state}"/>
                                </span>
                            </span>
                        </c:if>
                    </td>
                </c:if>
                <td>
                    <c:if test="${historyItem.type != 'DELETED'}">
                        <span ng-show="reservationRequest.historyItemId != '${historyItem.id}'">
                            <tag:listAction code="show" ngClick="setReservationRequest('${historyItem.id}', ${historyItem.isActive}, ${historyItem.isLatestAllocated})" tabindex="2"/>
                        </span>
                        <span ng-show="reservationRequest.historyItemId == '${historyItem.id}'">(<spring:message code="views.list.selected"/>)</span>
                    </c:if>
                    <c:if test="${historyItem.type == 'MODIFIED' && status.first}">
                        <tag:url var="historyItemRevertUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_REVERT %>">
                            <tag:param name="reservationRequestId" value="${historyItem.id}"/>
                        </tag:url>
                        <span ng-show="reservationRequest.allocationState != 'ALLOCATED'">
                            | <tag:listAction code="revert" url="${historyItemRevertUrl}" tabindex="2"/>
                        </span>
                    </c:if>
                </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</c:if>

<%-- Reservation request state (refreshable by dynamic content controller) --%>
<tag:url var="reservationRequestStateUrl" value="<%= ClientWebUrl.DETAIL_RESERVATION_REQUEST_STATE %>">
    <tag:param name="objectId" value="${objectId}"/>
    <tag:param name="isLatestAllocated" value="${isLatestAllocated}"/>
</tag:url>
<div id="reservationRequestState" ng-controller="DynamicContentController" content-url="${reservationRequestStateUrl}" content-loaded="true">
    <c:import url="detailReservationRequestState.jsp"/>
</div>

<c:if test="${isActive}">

    <%-- Periodic events --%>
    <c:if test="${reservationRequest.periodicityType != 'NONE'}">
        <hr/>
        <tag:reservationRequestChildren detailUrl="${detailUrl}"/>
    </c:if>

    <%-- Permanent room capacities --%>
    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
        <hr/>
        <c:if test="${canCreatePermanentRoomCapacity}">
            <tag:url var="createUsageUrl" value="<%= ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY %>">
                <tag:param name="permanentRoom" value="${reservationRequest.id}"/>
                <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
            </tag:url>
            <c:set var="createUsageWhen" value="reservationRequest.allocationState == 'ALLOCATED' && (reservationRequest.roomStateStarted || reservationRequest.roomState == 'NOT_STARTED')"/>
        </c:if>
        <div class="table-actions-left">
            <tag:reservationRequestUsages detailUrl="${detailUrl}" createUrl="${createUsageUrl}" createWhen="${createUsageWhen}"/>
        </div>
    </c:if>

</c:if>

</div>
