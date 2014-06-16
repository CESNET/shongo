<%--
  -- Main welcome page.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="advancedUserInterface" value="${sessionScope.SHONGO_USER.advancedUserInterface}"/>

<script type="text/javascript">
    var module = angular.module('jsp:indexDashboard', ['ngApplication', 'ngPagination', 'ngTooltip', 'ngCookies', 'ngSanitize']);
    module.controller("TabController", function($scope, $element) {
        $scope.$tab = $scope.$$childHead;
        $scope.$tab.$watch("active", function(active, test) {
            if (active) {
                var refreshEvent = 'refresh-' + $element.attr('id');
                $scope.$parent.$broadcast(refreshEvent);
            }
        });
    });
</script>

<div ng-app="jsp:indexDashboard" class="jspIndex">

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

    <security:authorize access="hasPermission(RESERVATION)">
        <tag:expandableBlock name="actions" expandable="false" expandCode="views.select.action" cssClass="actions">
            <span><spring:message code="views.select.action"/></span>
            <ul>
                <li>
                    <tag:url var="createRoomUrl" value="<%= ClientWebUrl.WIZARD_ROOM %>">
                        <tag:param name="back-url" value="${requestScope.requestUrl}"/>
                    </tag:url>
                    <a href="${createRoomUrl}" tabindex="1">
                        <spring:message code="views.index.action.createRoom"/>
                    </a>
                </li>
            </ul>
        </tag:expandableBlock>
    </security:authorize>

    <tabset>

        <spring:message code="views.index.rooms" var="roomsTitle"/>
        <tab id="rooms" heading="${roomsTitle}" ng-controller="TabController">
            <%@ include file="dashboardReservation.jsp" %>
        </tab>

        <spring:message code="views.index.participation" var="participationTitle"/>
        <tab id="participation" heading="${participationTitle}" ng-controller="TabController">
            <%@ include file="dashboardParticipation.jsp" %>
        </tab>

    </tabset>

</div>
