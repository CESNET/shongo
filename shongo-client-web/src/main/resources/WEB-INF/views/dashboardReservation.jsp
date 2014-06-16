<%--
  -- Main welcome page.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="advancedUserInterface" value="${sessionScope.SHONGO_USER.advancedUserInterface}"/>
<tag:url var="reservationRequestListDataUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_LIST_DATA %>">
    <tag:param name="specification-type" value=":type"/>
    <c:if test="${advancedUserInterface}">
        <tag:param name="allocation-state" value=":allocationState" escape="false"/>
    </c:if>
</tag:url>
<tag:url var="permanentRoomCapacitiesUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_LIST_DATA %>">
    <tag:param name="specification-type" value="PERMANENT_ROOM_CAPACITY"/>
    <tag:param name="permanent-room=" value=":permanent-room-id"/>
    <tag:param name="count" value="5"/>
    <tag:param name="sort" value="SLOT_NEAREST"/>
</tag:url>
<tag:url var="detailUrl" value="<%= ClientWebUrl.DETAIL_VIEW %>">
    <tag:param name="objectId" value="{{reservationRequest.id}}" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="detailRuntimeManagementUrl" value="<%= ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_VIEW %>">
    <tag:param name="objectId" value="{{reservationRequest.id}}" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="reservationRequestModifyUrl" value="<%= ClientWebUrl.WIZARD_MODIFY %>">
    <tag:param name="reservationRequestId" value="{{reservationRequest.id}}" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="reservationRequestDuplicateUrl" value="<%= ClientWebUrl.WIZARD_DUPLICATE %>">
    <tag:param name="reservationRequestId" value="{{reservationRequest.id}}" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="reservationRequestDeleteUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DELETE %>">
    <tag:param name="reservationRequestId" value="{{reservationRequest.id}}" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="createPermanentRoomCapacityUrl" value="<%= ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY %>">
    <tag:param name="permanentRoom" value="{{reservationRequest.id}}" escape="false"/>
    <tag:param name="force" value="true"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="permanentRoomCapacityDetailUrl" value="<%= ClientWebUrl.DETAIL_VIEW %>">
    <tag:param name="objectId" value="{{capacity.id}}" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="helpUrl" value="<%= ClientWebUrl.HELP %>"/>

<script type="text/javascript">
    function DashboardReservationListController($scope, $cookieStore) {
        var showNotAllocated = $cookieStore.get("index.showNotAllocated");
        if (showNotAllocated == null) {
            showNotAllocated = true;
        }

        $scope.reservationList = {};

        $scope.setupParameter = function(parameter, defaultValue, refreshList) {
            var value = null;
            <c:if test="${advancedUserInterface}">
                value = $cookieStore.get("index." + parameter);
            </c:if>
            if (value == null) {
                value = defaultValue;
            }
            $scope.reservationList[parameter] = value;
            $scope.$watch("reservationList." + parameter, function(newValue, oldValue){
                if (newValue != oldValue) {
                    $cookieStore.put("index." + parameter, newValue);
                    if (refreshList) {
                        $scope.$$childHead.refresh();
                    }
                }
            });
        };
        $scope.setupParameter("type", "", true);
        $scope.setupParameter("showNotAllocated", true, true);
        $scope.setupParameter("showPermanentRoomCapacities", true, false);
        $scope.getAllocationState = function() {
            return ($scope.reservationList.showNotAllocated ? null : "ALLOCATED");
        };
        $scope.getType = function() {
            return ($scope.reservationList.type != "" ? $scope.reservationList.type : "PERMANENT_ROOM,ADHOC_ROOM");
        };
    }
    function DashboardPermanentRoomCapacitiesController($scope, $resource) {
        $scope.items = null;
        if ($scope.reservationRequest.type != 'PERMANENT_ROOM') {
            return;
        }
        if ($scope.reservationRequest.state == 'FAILED' || $scope.reservationRequest.state == 'ALLOCATED_FINISHED') {
            return;
        }
        var resource = $resource('${permanentRoomCapacitiesUrl}', null, {
            list: {method: 'GET'}
        });
        resource.list({'permanent-room-id': $scope.reservationRequest.id}, function (result) {
            $scope.count = result.count;
            $scope.items = result.items;
        });
    }
</script>

<div ng-controller="DashboardReservationListController">
    <div ng-controller="PaginationController"
         ng-init="setSortDefault('SLOT_NEAREST'); init('dashboard', '${reservationRequestListDataUrl}', {allocationState: getAllocationState, type: getType}, 'refresh-rooms');">
        <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
        <spring:message code="views.button.refresh" var="paginationRefresh"/>

        <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
            <spring:message code="views.pagination.records"/>
        </pagination-page-size>
        <div>
            <div class="alert alert-warning">
                <spring:message code="views.index.rooms.description"/>
            </div>

            <%-- Filtering --%>
            <c:if test="${advancedUserInterface}">
                <form class="form-inline filter">
                    <span class="title"><spring:message code="views.filter"/>:</span>
                    <div class="input-group">
                        <span class="input-group-addon">
                            <spring:message code="views.reservationRequest.type"/>
                        </span>
                        <select id="type" class="form-control" ng-model="reservationList.type" style="width: 190px;">
                            <option value=""><spring:message code="views.reservationRequest.specification.all"/></option>
                            <option value="ADHOC_ROOM"><spring:message code="views.reservationRequest.specification.ADHOC_ROOM"/></option>
                            <option value="PERMANENT_ROOM"><spring:message code="views.reservationRequest.specification.PERMANENT_ROOM"/></option>
                        </select>
                    </div>
                    <div class="row">
                        <div class="checkbox-inline">
                            <input id="showNotAllocated" type="checkbox" ng-model="reservationList.showNotAllocated"/>
                            <label for="showNotAllocated">
                                <spring:message code="views.index.rooms.showNotAllocated"/>
                            </label>
                        </div>
                        <div class="checkbox-inline">
                            <input id="showPermanentRoomCapacities" type="checkbox" ng-model="reservationList.showPermanentRoomCapacities"/>
                            <label for="showPermanentRoomCapacities">
                                <spring:message code="views.index.rooms.showPermanentRoomCapacities"/>
                            </label>
                        </div>
                    </div>
                </form>
            </c:if>

        </div>
        <div class="spinner" ng-hide="ready || errorContent"></div>
        <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
        <table class="table table-striped table-hover" ng-show="ready">
            <thead>
            <tr>
                <c:if test="${advancedUserInterface}">
                    <th>
                        <pagination-sort column="DATETIME"><spring:message code="views.reservationRequest.dateTime"/></pagination-sort>
                    </th>
                    <th>
                        <pagination-sort column="USER"><spring:message code="views.reservationRequest.user"/></pagination-sort>
                    </th>
                </c:if>
                <th>
                    <pagination-sort column="REUSED_RESERVATION_REQUEST"><spring:message code="views.reservationRequest.type"/></pagination-sort><%--
                    --%><tag:help selectable="true" width="800px">
                    <h1><spring:message code="views.reservationRequest.specification.ADHOC_ROOM"/></h1>
                    <p><spring:message code="views.help.roomType.ADHOC_ROOM.description"/></p>
                    <h1><spring:message code="views.reservationRequest.specification.PERMANENT_ROOM"/></h1>
                    <p><spring:message code="views.help.roomType.PERMANENT_ROOM.description"/></p>
                    <a class="btn btn-success" href="${helpUrl}#rooms" target="_blank">
                        <spring:message code="views.help.rooms.display"/>
                    </a>
                    </tag:help>
                </th>
                <th>
                    <pagination-sort column="ALIAS_ROOM_NAME"><spring:message code="views.reservationRequestList.roomName"/></pagination-sort>
                </th>
                <th>
                    <pagination-sort column="TECHNOLOGY">
                        <spring:message code="views.reservationRequest.technology"/>
                    </pagination-sort>
                </th>
                <th>
                    <pagination-sort column="SLOT"><spring:message code="views.reservationRequestList.slot"/></pagination-sort>
                </th>
                <th>
                    <pagination-sort column="STATE"><spring:message code="views.reservationRequest.state"/></pagination-sort><tag:helpReservationRequestState/>
                </th>
                <th style="min-width: 135px; width: 135px;">
                    <spring:message code="views.list.action"/>
                    <pagination-sort-default class="pull-right"><spring:message code="views.pagination.defaultSorting"/></pagination-sort-default>
                </th>
            </tr>
            </thead>
            <tbody>
            <tr ng-repeat-start="reservationRequest in items" ng-class-odd="'odd'" ng-class-even="'even'"
                ng-class="{'deprecated': reservationRequest.isDeprecated}">
                <c:if test="${advancedUserInterface}">
                    <td>{{reservationRequest.dateTime}}</td>
                    <td>{{reservationRequest.user}}</td>
                </c:if>
                <td>{{reservationRequest.typeMessage}}</td>
                <td>
                    <spring:message code="views.index.rooms.showDetail" var="manageRoom"/>
                    <a ng-show="reservationRequest.reservationId" href="${detailUrl}" title="${manageRoom}" tabindex="2">{{reservationRequest.roomName}}</a>
                    <span ng-hide="reservationRequest.reservationId">{{reservationRequest.roomName}}</span>
                    <span ng-show="reservationRequest.roomParticipantCountMessage">({{reservationRequest.roomParticipantCountMessage}})</span>
                </td>
                <td>{{reservationRequest.technologyTitle}}</td>
                <td>
                    <span ng-bind-html="reservationRequest.earliestSlot"></span>
                    <span ng-show="reservationRequest.futureSlotCount">
                        <spring:message code="views.reservationRequestList.slotMore" var="slotMore" arguments="{{reservationRequest.futureSlotCount}}"/>
                        <tag:help label="(${slotMore})" cssClass="push-top">
                            <spring:message code="views.reservationRequestList.slotMoreHelp"/>
                        </tag:help>
                    </span>
                </td>
                <td class="reservation-request-state">
                    <tag:help label="{{reservationRequest.stateMessage}}" cssClass="{{reservationRequest.state}}">
                        <span>{{reservationRequest.stateHelp}}</span>
                    </tag:help>
                </td>
                <td>
                    <tag:url var="detailRuntimeManagementEnterUrl" value="<%= ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_ENTER %>">
                        <tag:param name="objectId" value="{{reservationRequest.reservationId}}" escape="false"/>
                    </tag:url>
                    <span ng-show="(reservationRequest.state == 'ALLOCATED_STARTED' || reservationRequest.state == 'ALLOCATED_STARTED_AVAILABLE') && reservationRequest.technology == 'ADOBE_CONNECT'">
                        <tag:listAction code="enterRoom" url="${detailRuntimeManagementEnterUrl}" target="_blank" tabindex="4"/> |
                    </span>
                    <tag:listAction code="show" titleCode="views.index.rooms.showDetail" url="${detailUrl}" tabindex="2"/>
                    <span ng-show="(reservationRequest.state == 'ALLOCATED_STARTED' || reservationRequest.state == 'ALLOCATED_STARTED_AVAILABLE')">
                        | <tag:listAction code="manageRoom" url="${detailRuntimeManagementUrl}" target="_blank" tabindex="4"/>
                    </span>
                    <span ng-show="reservationRequest.isWritable">
                        <span ng-hide="reservationRequest.state == 'ALLOCATED_FINISHED'">
                            | <tag:listAction code="modify" url="${reservationRequestModifyUrl}" tabindex="4"/>
                        </span>
                        <span ng-show="reservationRequest.state == 'ALLOCATED_FINISHED'">
                            | <tag:listAction code="duplicate" url="${reservationRequestDuplicateUrl}" tabindex="4"/>
                        </span>
                        | <tag:listAction code="delete" url="${reservationRequestDeleteUrl}" tabindex="4"/>
                    </span>
                </td>
            </tr>
            <tr ng-repeat-end class="description" ng-class-odd="'odd'" ng-class-even="'even'"
                ng-class="{'deprecated': reservationRequest.isDeprecated}">
                <td ng-if="reservationRequest.type == 'PERMANENT_ROOM' && reservationList.showPermanentRoomCapacities" ng-controller="DashboardPermanentRoomCapacitiesController" colspan="6">
                    <div style="position: relative;">
                        <div style="position: absolute;  right: 0px; bottom: 0px;" ng-show="reservationRequest.isProvidable && reservationRequest.state != 'ALLOCATED_FINISHED'">
                            <a class="btn btn-default" href="${createPermanentRoomCapacityUrl}" tabindex="1">
                                <spring:message code="views.index.rooms.permanentRoomCapacity.create" arguments="{{reservationRequest.roomName}}"/>
                            </a>
                        </div>
                        <span><spring:message code="views.index.rooms.permanentRoomCapacity" arguments="{{reservationRequest.roomName}}"/>:</span>
                        <ul>
                            <li ng-repeat="capacity in items">
                                <a href="${permanentRoomCapacityDetailUrl}">{{capacity.roomParticipantCountMessage}}</a>
                                <spring:message code="views.index.rooms.permanentRoomCapacity.slot" arguments="{{capacity.earliestSlot}}"/>
                                <span ng-show="capacity.futureSlotCount">
                                    (<spring:message code="views.reservationRequestList.slotMore" arguments="{{capacity.futureSlotCount}}"/>)
                                </span>
                                <span class="reservation-request-state">(<tag:help label="{{capacity.stateMessage}}" cssClass="{{capacity.state}}"><span>{{capacity.stateHelp}}</span></tag:help>)</span>
                            </li>
                            <li ng-show="count > items.length">
                                <a href="${detailUrl}" tabindex="2">
                                    <spring:message code="views.index.rooms.permanentRoomCapacity.slotMore" arguments="{{count - items.length}}"/>...
                                </a>
                            </li>
                            <li  ng-hide="items.length">
                                <span class="empty"><spring:message code="views.list.none"/></span>
                            </li>
                        </ul>
                    </div>
                </td>
            </tr>
            </tbody>
            <tbody>
            <tr ng-hide="items.length">
                <td colspan="6" class="empty"><spring:message code="views.list.none"/></td>
            </tr>
            </tbody>
        </table>
        <pagination-pages ng-show="ready"><spring:message code="views.pagination.pages"/></pagination-pages>
    </div>
</div>
