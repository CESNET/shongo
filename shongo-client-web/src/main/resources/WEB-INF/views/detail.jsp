<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ page import="cz.cesnet.shongo.client.web.models.TechnologyModel" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:if test="${isActive && !isPeriodicEvent}">
    <security:accesscontrollist hasPermission="WRITE" domainObject="${objectId}" var="isWritable"/>
</c:if>

<script type="text/javascript">
    var module = angular.module('jsp:detail', ['ngApplication', 'ngPagination', 'ngTooltip', 'ngCookies', 'ngSanitize', 'ng.deviceDetector', 'jsp:roomParticipantDialog']);
    module.controller("DetailController", function($scope, $timeout){
        var refreshParameters = {};

        // Set refresh timeout
        $scope.setRefreshTimeout = function(callback) {
            if ($scope.activeTabId == null) {
                console.error("setRefreshTimeout failed: No tab is active.");
            }
            if (refreshParameters[$scope.activeTabId] == null) {
                refreshParameters[$scope.activeTabId] = {
                    timeout: 5,
                    count: 0
                };
            }
            var tabRefreshParameters = refreshParameters[$scope.activeTabId];

            // Increase count and timeout
            if (tabRefreshParameters.count > 0 && (tabRefreshParameters.count % 3) == 0) {
                // Double refresh timeout after three refreshes
                tabRefreshParameters.timeout *= 2;

                // Stop refreshing when timeout is longer than 10 minutes
                if (tabRefreshParameters.timeout > (5 * 60)) {
                    return;
                }
            }
            tabRefreshParameters.count++;

            // Cancel old promise
            if (tabRefreshParameters.promise != null) {
                $timeout.cancel(tabRefreshParameters.promise);
            }

            // Setup refresh
            tabRefreshParameters.promiseCallback = callback;
            tabRefreshParameters.promise = $timeout(function(){
                // Cancel promise and callback
                tabRefreshParameters.promise = null;
                tabRefreshParameters.promiseCallback = null;
                // Perform callback (when session is not expired)
                if (!window.isSessionExpired()) {
                    callback();
                }
            }, tabRefreshParameters.timeout * 1000);
        };

        /**
         * Reservation request parameters
         */
        $scope.reservationRequest = {
            id: "${objectId}",
            historyItemId: "${objectId}",
            isPeriodic: ${isPeriodic},
            isPeriodicEvent: ${isPeriodicEvent},
            technology: ${technology != null ? ('"'.concat(technology).concat('"')) : 'null'},
            allocationState: "${allocationState}",
            reservationId : "${reservationId}",
            roomState: "${roomState}",
            roomStateStarted: ${roomState.started == true},
            roomStateAvailable: ${roomState.available == true},
            roomHasRecordingService: ${roomHasRecordingService == true},
            roomHasRecordings: ${roomHasRecordings == true},
            permanentRoomHasRecordings: ${permanentRoomHasRecordings == true},
            recordingsObjectId: ("${allocationState == 'ALLOCATED' ? objectId : permanentRoomId}")
        };

        /**
         * Url to this page with currently active tab.
         */
        $scope.requestUrl = null;

        /**
         * Currently active tab id.
         */
        $scope.activeTabId = null;

        /**
         * Event called when tab is created.
         */
        $scope.onCreateTab = function(tabId, tabScope) {
            if ($scope.firstTabActivation == null) {
                // Watch first tab activation
                $scope.firstTabActivation = tabScope.$watch("active", function(){
                    // Remove the watch
                    $scope.firstTabActivation();
                    // Deactivate first tab
                    tabScope.active = false;
                    // Activate tab after all tabs are initialized
                    $timeout(function(){
                        // If specified tab exists and it isn't disabled, activate it
                        if ($scope.firstTabActivation.tabScope != null && !$scope.firstTabActivation.tabScope.disabled) {
                            $scope.firstTabActivation.tabScope.active = true;
                        }
                        // Otherwise active first tab
                        else {
                            tabScope.active = true;
                        }
                    }, 0);
                });
            }
            else if (tabId == "${tab}") {
                // Store this tab to be activated on the start
                $scope.firstTabActivation.tabScope = tabScope;
            }
        };

        /**
         * Event called when tab is activated
         */
        $scope.onActivateTab = function(tabId, tabScope) {
            if (tabId != $scope.activeTabId) {
                // Stop old tab refresh
                if (refreshParameters[$scope.activeTabId] != null) {
                    var tabRefreshParameters = refreshParameters[$scope.activeTabId];
                    if (tabRefreshParameters.promise != null) {
                        // Cancel promise
                        $timeout.cancel(tabRefreshParameters.promise);
                    }
                }

                // Set new tab
                $scope.activeTabId = tabId;
                $scope.requestUrl = "${requestUrl}".split("?")[0];
                if (tabId != "reservationRequest") {
                    $scope.requestUrl += "?tab=" + tabId;
                }

                // Refresh tab
                if (tabScope.inited) {
                    // Always refresh runtime management
                    if (tabId == "runtimeManagement") {
                        tabScope.refresh();
                    }
                    // Refresh recordings when it is requested
                    else if (tabId == "recordings" && $scope.refreshRecordings) {
                        $scope.refreshRecordings = false;
                        tabScope.refresh();
                    }
                }

                // Start new tab refresh
                if (refreshParameters[$scope.activeTabId] != null) {
                    var tabRefreshParameters = refreshParameters[$scope.activeTabId];
                    if (tabRefreshParameters.promiseCallback != null) {
                        // Schedule promise again
                        tabRefreshParameters.count--;
                        $scope.setRefreshTimeout(tabRefreshParameters.promiseCallback);
                    }
                }
            }
        };

        /**
         * Can be used to refresh tab with given tabId.
         */
        $scope.refreshTab = function(tabId, url) {
            var tabElement = angular.element("#" + tabId);
            var tabScope = tabElement.scope();
            tabScope.refresh(url);
        };
    });
</script>

<div ng-app="jsp:detail" ng-controller="DetailController">

    <jsp:include page="../templates/roomParticipantDialog.jsp"/>

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

        <c:if test="${!isPeriodicEvent}">
            <spring:message var="detailUserRolesTitle" code="views.detail.tab.userRoles"/>
            <tag:url var="detailUserRolesUrl" value="<%= ClientWebUrl.DETAIL_USER_ROLES_TAB %>">
                <tag:param name="objectId" value="${objectId}"/>
            </tag:url>
            <tab id="userRoles" ng-controller="TabController"
                 heading="${detailUserRolesTitle}"
                 content-url="${detailUserRolesUrl}">
            </tab>
        </c:if>

        <c:if test="${!isPeriodic && specificationType != 'MEETING_ROOM'}">
            <spring:message var="detailParticipantsTitle" code="views.detail.tab.participants"/>
            <tag:url var="detailParticipantsUrl" value="<%= ClientWebUrl.DETAIL_PARTICIPANTS_TAB %>">
                <tag:param name="objectId" value="${objectId}"/>
            </tag:url>
            <tab id="participants" ng-controller="TabController" disabled="reservationRequest.allocationState != 'ALLOCATED'"
                 heading="${detailParticipantsTitle}"
                 content-url="${detailParticipantsUrl}">
            </tab>

            <spring:message var="detailRuntimeManagementTitle" code="views.detail.tab.runtimeManagement"/>
            <tag:url var="detailRuntimeManagementUrl" value="<%= ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_TAB %>">
                <tag:param name="objectId" value="${objectId}"/>
            </tag:url>
            <tab id="runtimeManagement" ng-controller="TabController" disabled="reservationRequest.allocationState != 'ALLOCATED' || !reservationRequest.roomStateAvailable || reservationRequest.technology == 'FREEPBX'"
                 heading="${detailRuntimeManagementTitle}"
                 content-url="${detailRuntimeManagementUrl}">
            </tab>

            <spring:message var="detailRecordingsTitle" code="views.detail.tab.recordings"/>
            <tag:url var="detailRecordingsUrl" value="<%= ClientWebUrl.DETAIL_RECORDINGS_TAB %>">
                <tag:param name="objectId" value="${objectId}"/>
            </tag:url>
            <tab id="recordings" ng-controller="TabController" disabled="!(reservationRequest.allocationState == 'ALLOCATED' && reservationRequest.roomHasRecordings) && !reservationRequest.permanentRoomHasRecordings"
                 heading="${detailRecordingsTitle}"
                 content-url="${detailRecordingsUrl}">
            </tab>

        </c:if>

    </tabset>

    <c:if test="${isWritable}">
        <div class="table-actions pull-right">
            <c:if test="${specificationType == 'ADHOC_ROOM' || specificationType == 'PERMANENT_ROOM_CAPACITY'}">
                <tag:url var="reservationRequestModifyExtendUrl" value="<%= ClientWebUrl.WIZARD_MODIFY_EXTEND %>">
                    <tag:param name="reservationRequestId" value="${objectId}"/>
                    <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
                </tag:url>
                <tag:url var="reservationRequestModifyEnlargeUrl" value="<%= ClientWebUrl.WIZARD_MODIFY_ENLARGE %>">
                    <tag:param name="reservationRequestId" value="${objectId}"/>
                    <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
                </tag:url>
                <%-- TODO: Check if resource has recording capability --%>
                <c:if test="${!(technology == 'FREEPBX')}">
                    <tag:url var="reservationRequestModifyRecordedUrl" value="<%= ClientWebUrl.WIZARD_MODIFY_RECORDED %>">
                        <tag:param name="reservationRequestId" value="${objectId}"/>
                        <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
                    </tag:url>
                    <div class="btn-group-divided" ng-show="reservationRequest.allocationState == 'ALLOCATED' && reservationRequest.roomStateStarted && !reservationRequest.roomHasRecordingService">
                        <spring:message code="views.detail.action.modifyRecorded.help" var="modifyRecordedHelp"/>
                        <a class="btn btn-default" href="${reservationRequestModifyRecordedUrl}" title="${modifyRecordedHelp}" tabindex="1">
                            <spring:message code="views.detail.action.modifyRecorded"/>
                        </a>
                    </div>
                </c:if>


                <div class="btn-group-divided" ng-show="reservationRequest.allocationState == 'ALLOCATED' && reservationRequest.roomStateStarted">
                    <spring:message code="views.detail.action.modifyExtend.help" var="modifyExtendHelp"/>
                    <a class="btn btn-default" href="${reservationRequestModifyExtendUrl}" title="${modifyExtendHelp}" tabindex="1">
                        <spring:message code="views.detail.action.modifyExtend"/>
                    </a>
                <c:if test="${!(technology == 'FREEPBX' || technology == 'PEXIP')}">
                    <spring:message code="views.detail.action.modifyEnlarge.help" var="modifyEnlargeHelp"/>
                    <a  class="btn btn-default" href="${reservationRequestModifyEnlargeUrl}" title="${modifyEnlargeHelp}" tabindex="1">
                        <spring:message code="views.detail.action.modifyEnlarge"/>
                    </a>
                </c:if>
                </div>

            </c:if>

            <div class="btn-group-divided">
                <tag:url var="reservationRequestModifyUrl" value="<%= ClientWebUrl.WIZARD_MODIFY %>">
                    <tag:param name="reservationRequestId" value="${objectId}"/>
                    <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
                </tag:url>
                <tag:url var="reservationRequestDuplicateUrl" value="<%= ClientWebUrl.WIZARD_DUPLICATE %>">
                    <tag:param name="reservationRequestId" value="${objectId}"/>
                    <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
                </tag:url>
                <span ng-switch on="reservationRequest.state == 'ALLOCATED_FINISHED'">
                    <a ng-switch-when="true" class="btn btn-default" href="${reservationRequestDuplicateUrl}" tabindex="1">
                        <spring:message code="views.button.duplicate"/>
                    </a>
                    <a ng-switch-when="false" class="btn btn-default" href="${reservationRequestModifyUrl}" tabindex="1">
                        <spring:message code="views.button.modify"/>
                    </a>
                </span>

                <tag:url var="reservationRequestDeleteUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DELETE %>">
                    <tag:param name="reservationRequestId" value="${objectId}"/>
                    <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
                </tag:url>
                <a class="btn btn-default" href="${reservationRequestDeleteUrl}" tabindex="1">
                    <spring:message code="views.button.delete"/>
                </a>
            </div>
        </div>
    </c:if>

    <div class="table-actions">
        <tag:url var="backUrl" value="${requestScope.backUrl}"/>
        <a class="btn btn-primary" href="${backUrl}" tabindex="1">
            <spring:message code="views.button.back"/>
        </a>
    </div>

</div>
