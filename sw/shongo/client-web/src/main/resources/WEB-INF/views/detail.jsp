<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<jsp:include page="../templates/roomParticipantDialog.jsp"/>

<script type="text/javascript">
    var module = angular.module('jsp:detail', ['ngApplication', 'ngPagination', 'ngTooltip', 'ngCookies', 'ngSanitize', 'jsp:roomParticipantDialog']);
    module.controller("DetailController", function($scope){
        $scope.requestUrl = null;
        $scope.onCreateTab = function(tabId, tabScope) {
            if (tabId == "${tab}") {
                tabScope.active = true;
            }
        };
        $scope.onActivateTab = function(tabId) {
            $scope.requestUrl = "${requestUrl}".split("?")[0];
            if (tabId != "reservationRequest") {
                $scope.requestUrl += "?tab=" + tabId;
            }
        };
        $scope.refreshTab = function(tabId, element) {
            var tabElement = angular.element("#" + tabId);
            var tabScope = tabElement.scope();
            tabScope.refresh();
        };
    });
</script>

<div ng-app="jsp:detail" ng-controller="DetailController">

    <h1>${titleDescription}</h1>

    <tabset>

        <spring:message var="detailReservationRequestTitle" code="views.detail.tab.reservationRequest"/>
        <tag:url var="detailReservationRequestUrl" value="<%= ClientWebUrl.DETAIL_RESERVATION_REQUEST_TAB %>">
            <tag:param name="objectId" value="${objectId}"/>
        </tag:url>
        <tab id="reservationRequest" ng-controller="TabController"
             heading="${detailReservationRequestTitle}"
             content-url="${detailReservationRequestUrl}">
        </tab>

        <spring:message var="detailUserRolesTitle" code="views.detail.tab.userRoles"/>
        <tag:url var="detailUserRolesUrl" value="<%= ClientWebUrl.DETAIL_USER_ROLES_TAB %>">
            <tag:param name="objectId" value="${objectId}"/>
        </tag:url>
        <tab id="userRoles" ng-controller="TabController"
             heading="${detailUserRolesTitle}"
             content-url="${detailUserRolesUrl}">
        </tab>

        <spring:message var="detailParticipantsTitle" code="views.detail.tab.participants"/>
        <tag:url var="detailParticipantsUrl" value="<%= ClientWebUrl.DETAIL_PARTICIPANTS_TAB %>">
            <tag:param name="objectId" value="${objectId}"/>
        </tag:url>
        <tab id="participants" ng-controller="TabController"
             heading="${detailParticipantsTitle}"
             content-url="${detailParticipantsUrl}">
        </tab>

        <spring:message var="detailRuntimeManagementTitle" code="views.detail.tab.runtimeManagement"/>
        <tag:url var="detailRuntimeManagementUrl" value="<%= ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_TAB %>">
            <tag:param name="objectId" value="${objectId}"/>
        </tag:url>
        <tab id="runtimeManagement" ng-controller="TabController"
             heading="${detailRuntimeManagementTitle}"
             content-url="${detailRuntimeManagementUrl}">
        </tab>

        <spring:message var="detailRecordingsTitle" code="views.detail.tab.recordings"/>
        <tag:url var="detailRecordingsUrl" value="<%= ClientWebUrl.DETAIL_RECORDINGS_TAB %>">
            <tag:param name="objectId" value="${objectId}"/>
        </tag:url>
        <tab id="recordings" ng-controller="TabController"
             heading="${detailRecordingsTitle}"
             content-url="${detailRecordingsUrl}">
        </tab>

    </tabset>

    <div class="table-actions pull-right">
        <tag:url var="backUrl" value="${requestScope.backUrl}"/>
        <a class="btn btn-primary" href="${backUrl}" tabindex="1">
            <spring:message code="views.button.back"/>
        </a>
    </div>

</div>
