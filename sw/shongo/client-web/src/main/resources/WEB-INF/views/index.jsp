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
<c:set var="wizardUrl">${contextPath}<%= ClientWebUrl.WIZARD %></c:set>
<c:set var="urlAdvanced">${contextPath}<%= ClientWebUrl.RESERVATION_REQUEST_LIST %></c:set>
<c:set var="urlRoomsData">${contextPath}<%= ClientWebUrl.ROOMS_DATA %></c:set>
<c:set var="urlRoomUsages">${contextPath}<%= ClientWebUrl.ROOMS_DATA %></c:set>

<h1>${title}</h1>
<p><spring:message code="views.index.welcome"/></p>
<p><spring:message code="views.index.suggestions" arguments="${configuration.contactEmail}"/></p>

<security:authorize access="!isAuthenticated()">
    <p><strong><spring:message code="views.index.login" arguments="${urlLogin}"/></strong></p>
</security:authorize>

<security:authorize access="isAuthenticated()">
    <script type="text/javascript">
        var module = angular.module('jsp:indexDashboard', ['ngPagination', 'ngTooltip']);
        module.directive('roomUsages', function() {
            return {
                restrict: 'A',
                link: function(scope, element, attrs) {
                    element.after("<tr><td>ahoj</td></tr>");
                }
            };
        })

        function RoomController($scope, $resource)
        {
            $scope.toggleRoom = function(room) {
                room.showUsages = !room.showUsages;
                if (room.showUsages && room.usages == null) {
                    var resource = $resource('${urlRoomUsages}', null, {
                        list: {method: 'GET'}
                    });
                    resource.list({'room-id': room.id}, function (result) {
                        room.usages = result.items;
                    });
                }
            }
        }
    </script>

    <div ng-app="jsp:indexDashboard">

        <div class="actions">
            <span><spring:message code="views.wizard.select"/></span>
            <ul>
                <li><a href="${wizardUrl}" tabindex="1"><spring:message code="views.index.dashboard.startWizard"/></a></li>
                <li><a href="${urlAdvanced}" tabindex="1"><spring:message code="views.index.dashboard.startAdvanced"/></a></li>
            </ul>
        </div>

        <div ng-controller="PaginationController"
             ng-init="init('dashboard.rooms', '${urlRoomsData}')">
            <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
            <spring:message code="views.button.refresh" var="paginationRefresh"/>
            <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
                <spring:message code="views.pagination.records"/>
            </pagination-page-size>
            <h2><spring:message code="views.index.dashboard.rooms"/></h2>
            <div class="spinner" ng-hide="ready"></div>
            <table class="table table-striped table-hover" ng-show="ready">
                <thead>
                <tr>
                    <th>
                        <pagination-sort column="ROOM_NAME"><spring:message code="views.room.name"/></pagination-sort>
                    </th>
                    <th>
                        <pagination-sort column="ROOM_TECHNOLOGY">
                            <spring:message code="views.room.technology"/>
                        </pagination-sort>
                    </th>
                    <th style="text-align: right;">
                        <pagination-sort column="ROOM_LICENSE_COUNT">
                            <spring:message code="views.room.licenseCount"/>
                        </pagination-sort>
                    </th>
                    <th>
                        <pagination-sort column="SLOT"><spring:message code="views.room.slot"/></pagination-sort>
                    </th>
                    <th width="200px">
                        <pagination-sort column="STATE"><spring:message code="views.room.state"/></pagination-sort>
                    </th>
                </tr>
                </thead>
                <tbody ng-controller="RoomController">
                    <tr ng-repeat-start="room in items" ng-class-odd="'odd'" ng-class-even="'even'">
                        <spring:eval var="urlRoomManagement"
                                     expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getRoomManagement(contextPath, '{{room.id}}')"/>
                        <td>
                            <span ng-switch="room.usageCount > 0">
                                <a ng-switch-when="true" href="" ng-click="toggleRoom(room)"
                                   ng-class="{'icon-plus': !room.showUsages, 'icon-minus': room.showUsages}"></a>
                                <span ng-switch-default class="icon-none"></span>
                            </span>
                            <a href="${urlRoomManagement}" tabindex="2">{{room.name}}</a>
                            <span ng-show="room.usageCount > 0">({{room.usageCount}})</span>
                        </td>
                        <td>{{room.technology}}</td>
                        <td style="text-align: right; padding-right: 30px;">{{room.licenseCount}}</td>
                        <td>{{room.slotStart}} - {{room.slotEnd}}</td>
                        <td class="executable-state">
                            <tag:help label="{{room.stateMessage}}" labelClass="{{room.state}}"
                                      tooltipId="roomStateTooltip-{{$index}}">
                                <span>{{room.stateHelp}}</span>
                            </tag:help>
                        </td>
                    </tr>
                    <tr ng-repeat-end class="description" ng-class-odd="'odd'" ng-class-even="'even'">
                        <td ng-show="room.usageCount > 0 && room.showUsages" colspan="5" style="padding-left: 30px;">
                            <spring:message code="views.index.dashboard.room.usages"/>:
                            <div class="spinner" ng-hide="room.usages"></div>
                            <ul>
                                <li ng-repeat="usage in room.usages">
                                    <strong>
                                        <spring:message code="views.index.dashboard.room.usage.participant"
                                                        arguments="{{usage.licenseCount}}"/>
                                    </strong>
                                    <spring:message code="views.index.dashboard.room.usage.slot"
                                                    arguments="{{usage.slotStart}},{{usage.slotEnd}}"/>
                                    <span class="executable-state">
                                        (<tag:help label="{{usage.stateMessage}}" labelClass="{{usage.state}}" tooltipId="room-{{$parent.$index}}-usageStateTooltip-{{$index}}"><span>{{usage.stateHelp}}</span></tag:help>)
                                    </span>
                                </li>
                            </ul>

                        </td>
                    </tr>
                    <tr ng-hide="items.length">
                        <td colspan="5" class="empty"><spring:message code="views.list.none"/></td>
                    </tr>
                </tbody>
            </table>
            <pagination-pages><spring:message code="views.pagination.pages"/></pagination-pages>
        </div>

    </div>
</security:authorize>