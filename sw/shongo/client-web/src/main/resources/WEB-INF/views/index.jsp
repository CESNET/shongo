<%--
  -- Main welcome page.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="advancedUserInterface" value="${sessionScope.user.advancedUserInterface}"/>

<c:set var="loginUrl">${contextPath}<%= ClientWebUrl.LOGIN %></c:set>
<c:set var="createRoomUrl">
    ${contextPath}<%= ClientWebUrl.WIZARD_CREATE_ROOM %>
</c:set>
<c:set var="reservationRequestListUrl">
    ${contextPath}<%= ClientWebUrl.RESERVATION_REQUEST_LIST %>
</c:set>
<c:set var="reservationRequestListDataUrl">
    ${contextPath}<%= ClientWebUrl.RESERVATION_REQUEST_LIST_DATA %>?type=PERMANENT_ROOM,ADHOC_ROOM
</c:set>
<c:set var="permanentRoomCapacitiesUrl">
    ${contextPath}<%= ClientWebUrl.RESERVATION_REQUEST_LIST_DATA %>?type=PERMANENT_ROOM_CAPACITY&permanent-room=:permanent-room-id&count=5
</c:set>

<h1>${title}</h1>
<p><spring:message code="views.index.welcome"/></p>
<p><spring:message code="views.index.suggestions" arguments="${configuration.contactEmail}"/></p>

<security:authorize access="!isAuthenticated()">
    <p><strong><spring:message code="views.index.login" arguments="${loginUrl}"/></strong></p>
</security:authorize>

<security:authorize access="isAuthenticated()">
    <script type="text/javascript">
        var module = angular.module('jsp:indexDashboard', ['ngPagination', 'ngTooltip', 'ngSanitize']);

        function PermanentRoomCapacitiesController($scope, $resource) {
            $scope.items = null;
            if ($scope.reservationRequest.type != 'PERMANENT_ROOM') {
                return;
            }
            if ($scope.reservationRequest.state == 'FAILED') {
                return;
            }
            var resource = $resource('${permanentRoomCapacitiesUrl}', null, {
                list: {method: 'GET'}
            });
            resource.list({'permanent-room-id': $scope.reservationRequest.id}, function (result) {
                $scope.count = result.count;
                $scope.items = result.items;
                console.debug("controller", $scope.reservationRequest.id, $scope.count, $scope.items);
            });
        }
    </script>

    <div ng-app="jsp:indexDashboard">

        <div class="actions">
            <span><spring:message code="views.select.action"/></span>
            <ul>
                <li>
                    <a href="${createRoomUrl}" tabindex="1">
                        <spring:message code="views.index.action.createRoom"/>
                    </a>
                </li>
                <li>
                    <a href="${reservationRequestListUrl}" tabindex="1">
                        <spring:message code="views.index.action.reservationRequestList"/>
                    </a>
                </li>
            </ul>
        </div>

        <div ng-controller="PaginationController"
             ng-init="init('dashboard', '${reservationRequestListDataUrl}')">
            <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
            <spring:message code="views.button.refresh" var="paginationRefresh"/>
            <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
                <spring:message code="views.pagination.records"/>
            </pagination-page-size>
            <h2><spring:message code="views.index.dashboard"/></h2>
            <div class="spinner" ng-hide="ready"></div>
            <table class="table table-striped table-hover" ng-show="ready">
                <thead>
                <tr>
                    <th>
                        <pagination-sort column="REUSED_RESERVATION_REQUEST"><spring:message code="views.reservationRequest.type"/></pagination-sort>
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
                    <th width="200px">
                        <pagination-sort column="STATE"><spring:message code="views.reservationRequest.state"/></pagination-sort>
                    </th>
                    <th style="width: 85px;">
                        <spring:message code="views.list.action"/>
                        <pagination-sort-default class="pull-right"><spring:message code="views.pagination.defaultSorting"/></pagination-sort-default>
                    </th>
                </tr>
                </thead>
                <tbody>
                <tr ng-repeat-start="reservationRequest in items" ng-class-odd="'odd'" ng-class-even="'even'"
                    ng-class="{'deprecated': reservationRequest.isDeprecated}">
                    <td>{{reservationRequest.typeMessage}}</td>
                    <td>
                        <spring:eval var="roomManagementUrl"
                                     expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getRoomManagement(contextPath, '{{reservationRequest.reservationId}}')"/>
                        <a ng-show="reservationRequest.reservationId" href="${roomManagementUrl}" tabindex="2">{{reservationRequest.roomName}}</a>
                        <span ng-hide="reservationRequest.reservationId">{{reservationRequest.roomName}}</span>
                        <span ng-show="reservationRequest.roomParticipantCountMessage">({{reservationRequest.roomParticipantCountMessage}})</span>
                    </td>
                    <td>{{reservationRequest.technology}}</td>
                    <td>
                        <span ng-bind-html="reservationRequest.earliestSlot"></span>
                        <span ng-show="reservationRequest.futureSlotCount">
                            <spring:message code="views.reservationRequestList.slotMore" var="slotMore" arguments="{{reservationRequest.futureSlotCount}}"/>
                            <tag:help label="(${slotMore})"
                                      style="vertical-align: top;"
                                      tooltipId="reservationRequest-slot-tooltip-{{$index}}">
                                <spring:message code="views.reservationRequestList.slotMoreHelp"/>
                            </tag:help>
                        </span>
                    </td>
                    <td class="reservation-request-state">
                        <tag:help label="{{reservationRequest.stateMessage}}" labelClass="{{reservationRequest.state}}"
                                  tooltipId="reservationRequest-state-tooltip-{{$index}}">
                            <span>{{reservationRequest.stateHelp}}</span>
                        </tag:help>
                    </td>
                    <td>
                        <spring:eval var="detailUrl"
                                     expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetail(contextPath, '{{reservationRequest.id}}')"/>
                        <tag:listAction code="show" url="${detailUrl}" tabindex="2"/>
                        <span ng-show="reservationRequest.isWritable">
                            <c:if test="${advancedUserInterface}">
                                <spring:eval var="modifyUrl"
                                             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestModify(contextPath, '{{reservationRequest.id}}')"/>
                                | <tag:listAction code="modify" url="${modifyUrl}" tabindex="4"/>
                            </c:if>
                            <spring:eval var="deleteUrl"
                                         expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDelete(contextPath, '{{reservationRequest.id}}')"/>
                            | <tag:listAction code="delete" url="${deleteUrl}" tabindex="4"/>
                        </span>
                    </td>
                </tr>
                <tr ng-repeat-end class="description" ng-class-odd="'odd'" ng-class-even="'even'">
                    <td ng-controller="PermanentRoomCapacitiesController" colspan="6" ng-show="items != null">
                        <div style="position: relative;">
                            <div style="position: absolute;  right: 0px; bottom: 0px;" ng-show="reservationRequest.state != 'ALLOCATED_FINISHED'">
                                <spring:eval var="createCapacityUrl"
                                             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getWizardCreatePermanentRoomCapacity(contextPath, '{{reservationRequest.id}}')"/>
                                <a class="btn" href="${createCapacityUrl}" tabindex="1">
                                    <spring:message code="views.index.dashboard.permanentRoomCapacity.create" arguments="{{reservationRequest.roomName}}"/>
                                </a>
                            </div>
                            <strong><spring:message code="views.index.dashboard.permanentRoomCapacity" arguments="{{reservationRequest.roomName}}"/>:</strong>
                            <ul>
                                <li ng-repeat="capacity in items">
                                    <spring:eval var="capacityDetailUrl"
                                                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetail(contextPath, '{{capacity.id}}')"/>
                                    <a href="${capacityDetailUrl}">{{capacity.roomParticipantCountMessage}}</a>
                                    <spring:message code="views.index.dashboard.permanentRoomCapacity.slot" arguments="{{capacity.earliestSlot}}"/>
                                    <span ng-show="capacity.futureSlotCount">
                                        (<spring:message code="views.reservationRequestList.slotMore" arguments="{{capacity.futureSlotCount}}"/>)
                                    </span>
                                    <span class="reservation-request-state">(<tag:help label="{{capacity.stateMessage}}" labelClass="{{capacity.state}}" tooltipId="capacity-{{$parent.$index}}-usageStateTooltip-{{$index}}"><span>{{capacity.stateHelp}}</span></tag:help>)</span>
                                </li>
                                <li ng-show="count > items.length">
                                    <a href="${detailUrl}" tabindex="2">
                                        <spring:message code="views.index.dashboard.permanentRoomCapacity.slotMore" arguments="{{count - items.length}}"/>...
                                    </a>
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
            <pagination-pages><spring:message code="views.pagination.pages"/></pagination-pages>
        </div>

    </div>

</security:authorize>