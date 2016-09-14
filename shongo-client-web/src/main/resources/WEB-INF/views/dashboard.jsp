<%--
  -- Dashboard in main page.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="advancedUserInterface" value="${sessionScope.SHONGO_USER.advancedUserInterface}"/>

<tag:url var="createMeetingRoomUrl" value="<%= ClientWebUrl.WIZARD_MEETING_ROOM_BOOK %>" />

<tag:url var="reservationRequestMultipleDeleteUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DELETE %>" />

<script type="text/javascript">
    var module = angular.module('jsp:indexDashboard', ['ngApplication', 'ngDateTime', 'ngPagination', 'ngTooltip', 'ngCookies', 'ngSanitize', 'ui.select2', 'ui.calendar']);
    module.controller("TabController",function($scope, $element) {
        $scope.$tab = $scope.$$childHead;
        // Broadcast "refresh-<tabId>" event when a tab with <tabId> is activated
        // event will be caught in PaginationController (see PaginationController.init method)
        $scope.$tab.$watch("active", function(active) {
            if (active) {
                var refreshEvent = 'refresh-' + $element.attr('id');
                $scope.$parent.$broadcast(refreshEvent);
            }
        });
    });
</script>


<div ng-app="jsp:indexDashboard" class="jspIndex">
    <%-- Warnings about user settings --%>
    <c:if test="${sessionScope.SHONGO_USER.localeDefaultWarning}">
        <tag:url var="userSettingsUrl" value="<%= ClientWebUrl.USER_SETTINGS %>">
            <tag:param name="back-url" value="${requestScope.requestUrl}"/>
        </tag:url>
        <tag:url var="ignoreUrl" value="<%= ClientWebUrl.USER_SETTINGS_ATTRIBUTE %>">
            <tag:param name="name" value="localeDefaultWarning"/>
            <tag:param name="value" value="false"/>
            <tag:param name="back-url" value="${requestScope.requestUrl}"/>
        </tag:url>
            <span class="warning">
                <spring:message code="views.index.localeDefaultWarning" arguments="${userSettingsUrl}"/>
                <a class="btn btn-info pull-right" href="${ignoreUrl}"><spring:message code="views.index.ignore"/></a>
                <div class="clearfix"></div>
            </span>
    </c:if>
    <c:if test="${sessionScope.SHONGO_USER.timeZoneDefaultWarning}">
        <tag:url var="userSettingsUrl" value="<%= ClientWebUrl.USER_SETTINGS %>">
            <tag:param name="back-url" value="${requestScope.requestUrl}"/>
        </tag:url>
        <tag:url var="ignoreUrl" value="<%= ClientWebUrl.USER_SETTINGS_ATTRIBUTE %>">
            <tag:param name="name" value="timeZoneDefaultWarning"/>
            <tag:param name="value" value="false"/>
            <tag:param name="back-url" value="${requestScope.requestUrl}"/>
        </tag:url>
            <span class="warning">
                <spring:message code="views.index.timeZoneDefaultWarning" arguments="${userSettingsUrl}"/>
                <a class="btn btn-info pull-right" href="${ignoreUrl}"><spring:message code="views.index.ignore"/></a>
                <div class="clearfix"></div>
            </span>
    </c:if>
    <c:if test="${!sessionScope.SHONGO_USER.userInterfaceSelected}">
        <div class="warning">
            <tag:url var="beginnerUserInterfaceUrl" value="<%= ClientWebUrl.USER_SETTINGS_ATTRIBUTE %>">
                <tag:param name="name" value="userInterface"/>
                <tag:param name="value" value="BEGINNER"/>
                <tag:param name="back-url" value="${requestScope.requestUrl}"/>
            </tag:url>
            <tag:url var="advanceUserInterfaceUrl" value="<%= ClientWebUrl.USER_SETTINGS_ATTRIBUTE %>">
                <tag:param name="name" value="userInterface"/>
                <tag:param name="value" value="ADVANCED"/>
                <tag:param name="back-url" value="${requestScope.requestUrl}"/>
            </tag:url>
            <div class="pull-right">
                <a class="btn btn-success" href="${advanceUserInterfaceUrl}"><spring:message code="views.button.yes"/></a>
                <a class="btn btn-danger" href="${beginnerUserInterfaceUrl}"><spring:message code="views.button.no"/></a>
            </div>
            <div>
                <spring:message code="views.index.advancedUserInterface"/>
                <p><spring:message code="views.userSettings.advancedUserInterface.help"/></p>
                <p><spring:message code="views.index.advancedUserInterface.later"/></p>
            </div>
            <div class="clearfix"></div>
        </div>
    </c:if>

    <%-- Actions --%>
    <security:authorize access="hasPermission(RESERVATION)">
        <tag:expandableBlock name="actions" expandable="false" expandCode="views.select.action" cssClass="actions">
            <span><spring:message code="views.select.action"/></span>
            <ul>
                <c:if test="${!showOnlyMeetingRooms}">
                    <li>
                        <tag:url var="createRoomUrl" value="<%= ClientWebUrl.WIZARD_ROOM %>">
                            <tag:param name="back-url" value="${requestScope.requestUrl}"/>
                        </tag:url>
                        <a href="${createRoomUrl}" tabindex="1">
                            <spring:message code="views.index.action.createRoom"/>
                        </a>
                    </li>
                </c:if>
                <c:if test="${!meetingRoomResources.isEmpty()}">
                    <li>
                        <a href="${createMeetingRoomUrl}" tabindex="1">
                            <spring:message code="views.index.action.bookMeetingRoom"/>
                        </a>
                    </li>
                </c:if>
            </ul>
        </tag:expandableBlock>
    </security:authorize>

    <tabset>
        <c:if test="${!showOnlyMeetingRooms}">
            <%-- Reservations tab --%>
            <spring:message code="views.index.reservations" var="roomsTitle"/>
            <tab id="rooms" heading="${roomsTitle}" ng-controller="TabController">
                <%@ include file="dashboardReservation.jsp" %>
            </tab>

            <%-- Participation tab --%>
            <spring:message code="views.index.participation" var="participationTitle"/>
            <tab id="participation" heading="${participationTitle}" ng-controller="TabController">
                <%@ include file="dashboardParticipation.jsp" %>
            </tab>
        </c:if>

        <c:if test="${!meetingRoomResources.isEmpty()}">
            <%-- Meeting rooms tab --%>
            <spring:message code="views.index.myMeetingRooms" var="myMeetingRoomTitle"/>
            <tab id="meetingRooms" heading="${myMeetingRoomTitle}" ng-controller="TabController">
                <%@ include file="dashboardMeetingRooms.jsp" %>
            </tab>

            <%-- Your meeting rooms reservation request tab TODO: only for CEITEC --%>
            <%--<spring:message code="views.index.meetingRooms" var="meetingRoomTitle"/>--%>
            <%--<tab id="meetingRoomsReservations" heading="${meetingRoomTitle}" ng-controller="TabController">--%>
                <%--<%@ include file="dashboardMeetingRoomReservations.jsp" %>--%>
            <%--</tab>--%>

            <%-- Your meeting rooms reservation request tab (calendar view) --%>
            <spring:message code="views.index.meetingRoomsCalendar" var="meetingRoomTitle"/>
            <tab id="meetingRoomsReservationsCalendar" heading="${meetingRoomTitle}" ng-controller="TabController">
                <%@ include file="dashboardMeetingRoomCalendar.jsp" %>
            </tab>
        </c:if>
    </tabset>
</div>
