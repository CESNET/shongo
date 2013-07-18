<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%--
  -- Main welcome page.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="urlLogin">${contextPath}<%= ClientWebUrl.LOGIN %></c:set>
<c:set var="urlWizard">${contextPath}<%= ClientWebUrl.WIZARD %></c:set>
<c:set var="urlAdvanced">${contextPath}<%= ClientWebUrl.RESERVATION_REQUEST_LIST %></c:set>
<c:set var="urlRoomsData">${contextPath}<%= ClientWebUrl.ROOMS_DATA %></c:set>

<h1>${title}</h1>
<p><spring:message code="views.index.welcome"/></p>
<p><spring:message code="views.index.suggestions" arguments="${configuration.contactEmail}"/></p>

<security:authorize access="!isAuthenticated()">
    <p><spring:message code="views.index.login" arguments="${urlLogin}"/></p>
</security:authorize>

<security:authorize access="isAuthenticated()">
    <script type="text/javascript">
        angular.module('jsp:indexDashboard', ['ngPagination', 'ngTooltip']);
    </script>

    <div ng-app="jsp:indexDashboard">

        <div class="actions">
            <span><spring:message code="views.wizard.select"/></span>
            <ul>
                <li><a href="${urlWizard}"><spring:message code="views.index.dashboard.startWizard"/></a></li>
                <li><a href="${urlAdvanced}"><spring:message code="views.index.dashboard.startAdvanced"/></a></li>
            </ul>
        </div>

        <div ng-controller="PaginationController"
             ng-init="init('dashboard.rooms', '${urlRoomsData}?start=:start&count=:count')">
            <pagination-page-size class="pull-right">
                <spring:message code="views.pagination.records"/>
            </pagination-page-size>
            <h2><spring:message code="views.index.dashboard.rooms"/></h2>
            <div class="spinner" ng-hide="ready"></div>
            <table class="table table-striped table-hover" ng-show="ready">
                <thead>
                <tr>
                    <th><spring:message code="views.room.name"/></th>
                    <th><spring:message code="views.room.technology"/></th>
                    <th><spring:message code="views.room.slot"/></th>
                    <th width="200px"><spring:message code="views.room.state"/></th>
                </tr>
                </thead>
                <tbody>
                <tr ng-repeat="room in items">
                    <spring:eval var="urlRoomManagement" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getRoomManagement(contextPath, '{{room.id}}')"/>
                    <td><a href="${urlRoomManagement}">{{room.name}}</a></td>
                    <td>{{room.technology}}</td>
                    <td>{{room.slotStart}} - {{room.slotEnd}}</td>
                    <td class="executable-state">
                        <span id="roomState-{{$index}}" class="{{room.state}}">{{room.stateMessage}}</span>
                        <tag:help label="roomState-{{$index}}" tooltipId="roomStateTooltip-{{$index}}">
                            <span>{{room.stateHelp}}</span>
                        </tag:help>
                    </td>
                </tr>
                <tr ng-hide="items.length">
                    <td colspan="4" class="empty"><spring:message code="views.list.none"/></td>
                </tr>
                </tbody>
            </table>
            <pagination-pages><spring:message code="views.pagination.pages"/></pagination-pages>
        </div>

    </div>
</security:authorize>