<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%--
  -- Page for listing rooms.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="urlRoomsData">${contextPath}<%= ClientWebUrl.ROOM_LIST_DATA %>
</c:set>
<c:set var="urlRoomUsages">${contextPath}<%= ClientWebUrl.ROOM_LIST_DATA %>
</c:set>

<script type="text/javascript">
    var module = angular.module('jsp:roomList', ['ngPagination', 'ngTooltip', 'ngSanitize']);

    function RoomController($scope, $resource) {
        $scope.toggleRoom = function (room) {
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

<div ng-app="jsp:roomList">

    <div ng-controller="PaginationController"
         ng-init="init('dashboard.rooms', '${urlRoomsData}')">
        <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
        <spring:message code="views.button.refresh" var="paginationRefresh"/>
        <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
            <spring:message code="views.pagination.records"/>
        </pagination-page-size>
        <h1><spring:message code="views.roomList.title"/></h1>
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
                    <pagination-sort-default class="pull-right"><spring:message code="views.pagination.defaultSorting"/></pagination-sort-default>
                </th>
            </tr>
            </thead>
            <tbody ng-controller="RoomController">
            <tr ng-repeat-start="room in items" ng-class-odd="'odd'" ng-class-even="'even'"
                ng-class="{'deprecated': room.isDeprecated}">
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
                <td><span ng-bind-html="room.slot"></span></td>
                <td class="room-state">
                    <tag:help label="{{room.stateMessage}}" labelClass="{{room.state}}"
                              tooltipId="roomStateTooltip-{{$index}}">
                        <span>{{room.stateHelp}}</span>
                    </tag:help>
                </td>
            </tr>
            <tr ng-repeat-end class="description" ng-class-odd="'odd'" ng-class-even="'even'">
                <td ng-show="room.usageCount > 0 && room.showUsages" colspan="5" style="padding-left: 30px;">
                    <div class="spinner" ng-hide="room.usages != null"></div>
                    <div ng-show="room.usages != null">
                        <spring:message code="views.roomList.room.usages"/>:
                        <ul ng-show="room.usages.length">
                            <li ng-repeat="usage in room.usages">
                                <strong>
                                    <spring:message code="views.roomList.room.usage.participant"
                                                    arguments="{{usage.licenseCount}}"/>
                                </strong>
                                <spring:message code="views.roomList.room.usage.slot" arguments="{{usage.slot}}"/>
                                    <span class="room-state">(<tag:help label="{{usage.stateMessage}}" labelClass="{{usage.state}}" tooltipId="room-{{$parent.$index}}-usageStateTooltip-{{$index}}"><span>{{usage.stateHelp}}</span></tag:help>)</span>
                            </li>
                        </ul>
                        <span class="empty" ng-hide="room.usages.length">
                            <br/>&nbsp;&nbsp;&nbsp;<spring:message code="views.list.none"/>
                        </span>
                    </div>
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
