<%--
  -- Main welcome page.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<p><spring:message code="views.index.welcome"/></p>
<p><spring:message code="views.index.suggestions" arguments="${configuration.suggestionEmail}"/></p>

<security:authorize access="!isAuthenticated()">
    <tag:url var="loginUrl" value="<%= ClientWebUrl.LOGIN %>"/>
    <p><strong><spring:message code="views.index.login" arguments="${loginUrl}"/></strong></p>
</security:authorize>

<security:authorize access="isAuthenticated()">
    <security:authentication property="principal.userId" var="userId"/>
    <c:set var="advancedUserInterface" value="${sessionScope.SHONGO_USER.advancedUserInterface}"/>
    <tag:url var="createRoomUrl" value="<%= ClientWebUrl.WIZARD_ROOM %>">
        <tag:param name="back-url" value="${requestScope.requestUrl}"/>
    </tag:url>
    <tag:url var="reservationRequestListUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_LIST %>"/>
    <tag:url var="reservationRequestListDataUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_LIST_DATA %>">
        <tag:param name="specification-type" value="PERMANENT_ROOM,ADHOC_ROOM"/>
        <tag:param name="allocation-state" value="ALLOCATED"/>
    </tag:url>
    <tag:url var="permanentRoomCapacitiesUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_LIST_DATA %>">
        <tag:param name="specification-type" value="PERMANENT_ROOM_CAPACITY"/>
        <tag:param name="permanent-room=" value=":permanent-room-id"/>
        <tag:param name="count" value="5"/>
        <tag:param name="sort" value="SLOT_NEAREST"/>
    </tag:url>
    <tag:url var="roomManagementUrl" value="<%= ClientWebUrl.ROOM_MANAGEMENT %>">
        <tag:param name="roomId" value="{{reservationRequest.reservationId}}" escape="false"/>
    </tag:url>
    <tag:url var="reservationRequestDetailUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DETAIL %>">
        <tag:param name="reservationRequestId" value="{{reservationRequest.id}}" escape="false"/>
        <tag:param name="back-url" value="${requestScope.requestUrl}"/>
    </tag:url>
    <tag:url var="reservationRequestModifyUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_MODIFY %>">
        <tag:param name="reservationRequestId" value="{{reservationRequest.id}}" escape="false"/>
        <tag:param name="back-url" value="${requestScope.requestUrl}"/>
    </tag:url>
    <tag:url var="reservationRequestDuplicateUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_CREATE_DUPLICATE %>">
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
    <tag:url var="permanentRoomCapacityDetailUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DETAIL %>">
        <tag:param name="reservationRequestId" value="{{capacity.id}}" escape="false"/>
        <tag:param name="back-url" value="${requestScope.requestUrl}"/>
    </tag:url>

    <tag:url var="participantRoomListUrl" value="<%= ClientWebUrl.ROOM_LIST_DATA %>">
        <tag:param name="participant-user-id" value="${userId}"/>
    </tag:url>
    <tag:url var="participantRoomUrl" value="<%= ClientWebUrl.ROOM_DATA %>">
        <tag:param name="roomId" value=":roomId"/>
    </tag:url>
    <tag:url var="helpUrl" value="<%= ClientWebUrl.HELP %>"/>

    <script type="text/javascript">
        var module = angular.module('jsp:indexDashboard', ['ngApplication', 'ngPagination', 'ngTooltip', 'ngCookies', 'ngSanitize']);
        module.controller("TabController", function($scope, $element) {
            $scope.$watch("active", function(active) {
                if (active) {
                    var refreshEvent = 'refresh-' + $element.attr('id');
                    $scope.$parent.$broadcast(refreshEvent);
                }
            });
        });
        module.controller("PermanentRoomCapacitiesController", function($scope, $resource) {
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
        });
        module.controller("ParticipantRoomController", function($scope, $application) {
            $scope.formatAliases = function(executableId, event){
                $.ajax("${participantRoomUrl}".replace(":roomId", executableId), {
                    dataType: "json"
                }).done(function (data) {
                    event.setResult(data.aliases);
                }).fail($application.handleAjaxFailure);
                return "<spring:message code="views.loading"/>";
            };
        });
    </script>

    <div ng-app="jsp:indexDashboard">

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
            </div>
        </c:if>

        <security:authorize access="hasPermission(RESERVATION)">
            <tag:expandableBlock name="actions" expandable="${advancedUserInterface}" expandCode="views.select.action" cssClass="actions">
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
            </tag:expandableBlock>
        </security:authorize>

        <tabset>

            <spring:message code="views.index.rooms" var="roomsTitle"/>
            <tab id="rooms" heading="${roomsTitle}" ng-controller="TabController">
                <div ng-controller="PaginationController"
                     ng-init="setSortDefault('SLOT_NEAREST'); init('dashboard', '${reservationRequestListDataUrl}', null, 'refresh-rooms');">
                    <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
                    <spring:message code="views.button.refresh" var="paginationRefresh"/>
                    <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
                        <spring:message code="views.pagination.records"/>
                    </pagination-page-size>
                    <div class="alert alert-warning"><spring:message code="views.index.rooms.description"/></div>
                    <div class="spinner" ng-hide="ready || errorContent"></div>
                    <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
                    <table class="table table-striped table-hover" ng-show="ready">
                        <thead>
                        <tr>
                            <th>
                                <pagination-sort column="REUSED_RESERVATION_REQUEST"><spring:message code="views.reservationRequest.type"/></pagination-sort>
                                <tag:help selectable="true" width="800px">
                                    <h1><spring:message code="views.reservationRequest.specification.ADHOC_ROOM"/></h1>
                                    <p><spring:message code="views.help.roomType.ADHOC_ROOM.description"/></p>
                                    <h1><spring:message code="views.reservationRequest.specification.PERMANENT_ROOM"/></h1>
                                    <p><spring:message code="views.help.roomType.PERMANENT_ROOM.description"/></p>
                                    <a class="btn btn-success" href="${helpUrl}#rooms" target="_blank">
                                        <spring:message code="views.help.rooms.display"/>
                                    </a>
                                </tag:help>
                            </th>
                            <th style="min-width: 150px;">
                                <pagination-sort column="ALIAS_ROOM_NAME"><spring:message code="views.reservationRequestList.roomName"/></pagination-sort>
                            </th>
                            <th style="min-width: 110px;">
                                <pagination-sort column="TECHNOLOGY">
                                    <spring:message code="views.reservationRequest.technology"/>
                                </pagination-sort>
                            </th>
                            <th style="min-width: 230px;">
                                <pagination-sort column="SLOT"><spring:message code="views.reservationRequestList.slot"/></pagination-sort>
                            </th>
                            <th width="200px">
                                <pagination-sort column="STATE"><spring:message code="views.reservationRequest.state"/></pagination-sort>
                                <tag:helpReservationRequestState/>
                            </th>
                            <th style="min-width: 85px; width: 85px;">
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
                                <spring:message code="views.index.rooms.manageRoom" var="manageRoom"/>
                                <a ng-show="reservationRequest.reservationId" href="${roomManagementUrl}" title="${manageRoom}" tabindex="2">{{reservationRequest.roomName}}</a>
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
                                <tag:url var="roomEnterUrl" value="<%= ClientWebUrl.ROOM_ENTER %>">
                                    <tag:param name="roomId" value="{{reservationRequest.reservationId}}" escape="false"/>
                                </tag:url>
                                <span ng-show="(reservationRequest.state == 'ALLOCATED_STARTED' || reservationRequest.state == 'ALLOCATED_STARTED_AVAILABLE') && reservationRequest.technology == 'ADOBE_CONNECT'">
                                    <tag:listAction code="enterRoom" url="${roomEnterUrl}" target="_blank" tabindex="4"/> |
                                </span>
                                <tag:listAction code="show" titleCode="views.index.rooms.showDetail" url="${reservationRequestDetailUrl}" tabindex="2"/>
                                <span ng-show="reservationRequest.isWritable">
                                    <c:if test="${advancedUserInterface}">
                                        <span ng-hide="reservationRequest.state == 'ALLOCATED_FINISHED'">
                                            | <tag:listAction code="modify" url="${reservationRequestModifyUrl}" tabindex="4"/>
                                        </span>
                                        <span ng-show="reservationRequest.state == 'ALLOCATED_FINISHED'">
                                            | <tag:listAction code="duplicate" url="${reservationRequestDuplicateUrl}" tabindex="4"/>
                                        </span>
                                    </c:if>
                                    | <tag:listAction code="delete" url="${reservationRequestDeleteUrl}" tabindex="4"/>
                                </span>
                            </td>
                        </tr>
                        <tr ng-repeat-end class="description" ng-class-odd="'odd'" ng-class-even="'even'"
                            ng-class="{'deprecated': reservationRequest.isDeprecated}">
                            <td ng-controller="PermanentRoomCapacitiesController" colspan="6" ng-show="items != null">
                                <div style="position: relative;">
                                    <div style="position: absolute;  right: 0px; bottom: 0px;" ng-show="reservationRequest.isProvidable && reservationRequest.state != 'ALLOCATED_FINISHED'">
                                        <a class="btn" href="${createPermanentRoomCapacityUrl}" tabindex="1">
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
                                            <a href="${reservationRequestDetailUrl}" tabindex="2">
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
            </tab>

            <spring:message code="views.index.participation" var="participationTitle"/>
            <tab id="participation" heading="${participationTitle}" ng-controller="TabController">
                <div ng-controller="PaginationController"
                     ng-init="init('roomList', '${participantRoomListUrl}', null, 'refresh-participation')">
                    <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
                    <spring:message code="views.button.refresh" var="paginationRefresh"/>
                    <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
                        <spring:message code="views.pagination.records"/>
                    </pagination-page-size>
                    <div class="alert alert-warning"><spring:message code="views.index.participation.description"/></div>
                    <div class="spinner" ng-hide="ready || errorContent"></div>
                    <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
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
                            <th>
                                <pagination-sort column="SLOT"><spring:message code="views.room.slot"/></pagination-sort>
                            </th>
                            <th width="200px">
                                <pagination-sort column="STATE"><spring:message code="views.room.state"/></pagination-sort><tag:helpRoomState/>
                            </th>
                            <th>
                                <spring:message code="views.room.description"/>
                            </th>
                            <th style="min-width: 85px; width: 85px;">
                                <spring:message code="views.list.action"/>
                                <pagination-sort-default class="pull-right"><spring:message code="views.pagination.defaultSorting"/></pagination-sort-default>
                            </th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr ng-repeat="room in items" ng-class="{'deprecated': room.isDeprecated}">
                            <td ng-controller="ParticipantRoomController">
                                <tag:help label="{{room.name}}" content="formatAliases(room.id, event)" selectable="true"/>
                            </td>
                            <td>{{room.technology}}</td>
                            <td><span ng-bind-html="room.slot"></span></td>
                            <td class="room-state">
                                <tag:help label="{{room.stateMessage}}" cssClass="{{room.state}}">
                                    <span>{{room.stateHelp}}</span>
                                </tag:help>
                            </td>
                            <td>{{room.description}}</td>
                            <td>
                                <tag:url var="roomEnterUrl" value="<%= ClientWebUrl.ROOM_ENTER %>">
                                    <tag:param name="roomId" value="{{room.id}}" escape="false"/>
                                </tag:url>
                                <span ng-show="room.stateAvailable">
                                    <tag:listAction code="enterRoom" url="${roomEnterUrl}" target="_blank" tabindex="4"/>
                                </span>
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
            </tab>

        </tabset>
    </div>

</security:authorize>