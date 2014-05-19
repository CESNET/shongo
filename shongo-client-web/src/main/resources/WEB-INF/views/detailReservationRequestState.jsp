<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<script type="text/javascript">
    function ReservationRequestStateController($scope) {
    <c:if test="${reservationRequest.detail.state != null}">
        // Update only reservation request latest version
        if ($scope.reservationRequest.id == "${reservationRequest.id}") {
            $scope.reservationRequest.allocationState = "${reservationRequest.detail.allocationState}";
            $scope.reservationRequest.reservationId = "${reservationRequest.detail.reservationId}";
            <c:if test="${reservationRequest.detail.room != null}">
            $scope.reservationRequest.roomState = "${reservationRequest.detail.room.state}";
            $scope.reservationRequest.roomStateStarted = ${reservationRequest.detail.room.state.started == true};
            $scope.reservationRequest.roomStateAvailable = ${reservationRequest.detail.room.state.available == true};
            $scope.reservationRequest.roomHasRecordingService = ${reservationRequest.detail.room.hasRecordingService()};
            $scope.reservationRequest.roomHasRecordings = ${reservationRequest.detail.room.hasRecordings()};
            </c:if>
            $scope.reservationRequest.state = "${reservationRequest.detail.state}"
            $scope.reservationRequest.stateLabel = "<spring:message code="views.reservationRequest.state.${reservationRequest.specificationType}.${reservationRequest.detail.state}"/>"
            if ($scope.reservationRequest.allocationState == 'ALLOCATED') {
                $scope.reservationRequest.recordingsObjectId = $scope.reservationRequest.id;
            }
        }
    </c:if>
    }
</script>

<div ng-controller="ReservationRequestStateController">
    <tag:url var="detailUrl" value="<%= ClientWebUrl.DETAIL_VIEW %>"/>
    <tag:reservationRequestDetail reservationRequest="${reservationRequest}" detailUrl="${detailUrl}"/>
</div>
