<%--
  -- Reservations tab in dashboard.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<security:authorize access="hasPermission(OPERATOR)" var="isOperator"/>
<c:set var="advancedUserInterface" value="${sessionScope.SHONGO_USER.advancedUserInterface}"/>
<tag:url var="reservationRequestListDataUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_LIST_DATA %>"/>
<tag:url var="permanentRoomCapacitiesUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_LIST_DATA %>">
    <tag:param name="specification-type" value="PERMANENT_ROOM_CAPACITY"/>
    <tag:param name="count" value="10"/>
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
<tag:url var="userListUrl" value="<%= ClientWebUrl.USER_LIST_DATA %>"/>

<script type="text/javascript">
    // Controller for filtering
    function DashboardReservationListController($scope, $cookieStore, $application, $timeout) {
        // Load filter configuration
        $scope.reservationList = {
            showExtendedFilter: false
        };
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
                        $scope.refreshList();
                    }
                }
            });
        };
        $scope.setupParameter("specificationType", "", true);
        $scope.setupParameter("specificationTechnology", "", true);
        $scope.setupParameter("showNotAllocated", true, true);
        <c:choose>
            <c:when test="${isOperator}">
                // We don't want to show capacities for operators (because a lot of permanent rooms can be visible)
                $scope.reservationList.showPermanentRoomCapacities = false;
            </c:when>
            <c:otherwise>
                $scope.setupParameter("showPermanentRoomCapacities", true, false);
            </c:otherwise>
        </c:choose>

        // Watch for filter changes (which are not persisted to cookies)
        for (var parameter in {"intervalFrom": null, "intervalTo": null, "search": null}) {
            var handler = function(newValue, oldValue){
                if (newValue != oldValue) {
                    $scope.refreshList(this.fn.parameter == "search" ? 1000 : 0);
                }
            };
            handler.parameter = parameter;
            $scope.$watch("reservationList." + parameter, handler);
        }
        for (var parameter in {"userId": null, "participantUserId": null}) {
            $scope.$watch("reservationList." + parameter, function(newValue, oldValue){
                if ((newValue != null && typeof newValue == "object" && newValue.id != 0 && (oldValue == null || oldValue.id != newValue.id)) || (newValue == null && oldValue != null)) {
                    $scope.refreshList();
                }
            });
        }
        $scope.$watch("reservationList.showExtendedFilter", function(newValue, oldValue){
            if (newValue != oldValue) {
                $scope.refreshList();
            }
        });

        // Refresh list of rooms
        var refreshListTimer = null;
        $scope.refreshList = function(timeout) {
            if (refreshListTimer != null) {
                $timeout.cancel(refreshListTimer);
            }
            refreshListTimer = $timeout(function(){
                $scope.$$childHead.refresh();
                refreshListTimer = null;
            }, timeout);
        };

        // URL for listing rooms
        $scope.getReservationRequestListDataUrl = function() {
            var specificationType = $scope.reservationList.specificationType;
            if ( specificationType == null || specificationType == "") {
                specificationType = "PERMANENT_ROOM,ADHOC_ROOM";
            }
            var url = "${reservationRequestListDataUrl}?specification-type=" + specificationType;
            if ($scope.reservationList.specificationTechnology != null && $scope.reservationList.specificationTechnology != "") {
                url += "&specification-technology=" + $scope.reservationList.specificationTechnology;
            }
            if (!$scope.reservationList.showNotAllocated) {
                url += "&allocation-state=ALLOCATED"
            }
            if ($scope.reservationList.showExtendedFilter) {
                if ($scope.reservationList.intervalFrom != null && $scope.reservationList.intervalFrom != "") {
                    url += "&interval-from=" + $scope.reservationList.intervalFrom + "T00:00:00";
                }
                if ($scope.reservationList.intervalTo != null && $scope.reservationList.intervalTo != "") {
                    url += "&interval-to=" + $scope.reservationList.intervalTo + "T23:59:59";
                }
                if ($scope.reservationList.userId != null) {
                    url += "&user-id=" + $scope.reservationList.userId.id;
                }
                if ($scope.reservationList.participantUserId != null) {
                    url += "&participant-user-id=" + $scope.reservationList.participantUserId.id;
                }
                if ($scope.reservationList.search != null && $scope.reservationList.search != "") {
                    url += "&search=" + encodeURIComponent($scope.reservationList.search);
                }
            }
            return url;
        };

        // User select options
        $scope.formatUser = function (user) {
            var text = "<b>" + user.firstName;
            if (user.lastName != null) {
                text += " " + user.lastName;
            }
            text += "</b>";
            if (user.organization != null) {
                text += " (" + user.organization + ")";
            }
            return text;
        };
        $scope.userOptions = {
            placeholder: "<spring:message code="views.select.user"/>",
            width: 'resolve',
            minimumInputLength: 2,
            ajax: {
                url: "${userListUrl}",
                dataType: 'json',
                quietMillis: 1000,
                data: function (term, page) {
                    return {
                        filter: term
                    };
                },
                results: function (data, page) {
                    var results = [];
                    for (var index = 0; index < data.length; index++) {
                        var dataItem = data[index];
                        results.push({id: dataItem.userId, text: $scope.formatUser(dataItem)});
                    }
                    return {results: results};
                },
                transport: function (options) {
                    return $.ajax(options).fail($application.handleAjaxFailure);
                }
            },
            escapeMarkup: function (markup) { return markup; },
            initSelection: function (element, callback) {
                var id = $(element).val();
                callback({id: id, text: '<spring:message code="views.select.loading"/>'});
                $.ajax("${userListUrl}?userId=" + id, {
                    dataType: "json"
                }).done(function (data) {
                    callback({id: id, text: $scope.formatUser(data[0])});
                }).fail($application.handleAjaxFailure);
            }
        };
    }

    // Controller for one permanent room
    function DashboardPermanentRoomCapacitiesController($scope, $resource) {
        $scope.items = null;
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

<c:set var="deleteCheckboxName" value="virtualRoomsDeleteCheckbox" />

<div ng-controller="DashboardReservationListController">
    <div ng-controller="PaginationController"
         ng-init="setSortDefault('SLOT_NEAREST'); init('dashboard', getReservationRequestListDataUrl, null, 'refresh-rooms', '${reservationRequestMultipleDeleteUrl}', '${deleteCheckboxName}');">
        <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
        <spring:message code="views.button.refresh" var="paginationRefresh"/>
        <spring:message code="views.button.remove" var="paginationRemove"/>
        <spring:message code="views.button.removeAll" var="paginationRemoveAll"/>

        <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}" remove="${paginationRemove}" remove-all="${paginationRemoveAll}">
            <spring:message code="views.pagination.records"/>
        </pagination-page-size>
        <div>
            <div class="alert alert-warning">
                <spring:message code="views.index.reservations.description"/>
            </div>

            <%-- Filter: start --%>
            <c:if test="${advancedUserInterface}">
                <form class="form-inline filter" ng-class="{'filter-collapsed': !reservationList.showExtendedFilter}">
                    <input type="hidden" ng-model="reservationList.showExtendedFilter"/>
                    <span class="toggle pull-right">
                        <a href="" ng-click="reservationList.showExtendedFilter = true" ng-show="!reservationList.showExtendedFilter"><spring:message code="views.index.reservations.showExtendedFilter"/></a>
                        <a href="" ng-click="reservationList.showExtendedFilter = false" ng-show="reservationList.showExtendedFilter"><spring:message code="views.index.reservations.hideExtendedFilter"/></a>
                    </span>
                    <span class="title"><spring:message code="views.filter"/>:</span>
                    <div class="row">
                        <div class="input-group">
                            <span class="input-group-addon">
                                <spring:message code="views.reservationRequest.type"/>
                            </span>
                            <select class="form-control" ng-model="reservationList.specificationType" style="width: 190px;">
                                <option value=""><spring:message code="views.reservationRequest.specification.all"/></option>
                                <option value="ADHOC_ROOM"><spring:message code="views.reservationRequest.specification.ADHOC_ROOM"/></option>
                                <option value="PERMANENT_ROOM"><spring:message code="views.reservationRequest.specification.PERMANENT_ROOM"/></option>
                            </select>
                        </div>
                        <div class="input-group">
                            <span class="input-group-addon">
                                <spring:message code="views.reservationRequest.technology"/>
                            </span>
                            <select class="form-control" ng-model="reservationList.specificationTechnology" style="width: 150px;">
                                <option value=""><spring:message code="views.reservationRequest.technology.all"/></option>
                                <spring:eval var="technologies" expression="T(cz.cesnet.shongo.client.web.models.TechnologyModel).values()"/>
                                <c:forEach var="technology" items="${technologies}">
                                    <option value="${technology}"><spring:message code="${technology.titleCode}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    <div class="row" ng-show="reservationList.showExtendedFilter">
                        <div class="input-group">
                            <span class="input-group-addon">
                                <spring:message code="views.index.reservations.intervalFrom"/>
                            </span>
                            <input id="intervalFrom" type="text" class="form-control form-picker" date-picker="true" readonly="true" style="width: 190px;" ng-model="reservationList.intervalFrom"/>
                        </div>
                        <div class="input-group">
                            <span class="input-group-addon">
                                <spring:message code="views.index.reservations.intervalTo"/>
                            </span>
                            <input id="intervalTo" type="text" class="form-control form-picker" date-picker="true" readonly="true" style="width: 190px;" ng-model="reservationList.intervalTo"/>
                        </div>
                        <a class="btn btn-default" href="" ng-click="reservationList.intervalFrom = null; reservationList.intervalTo = null;"><i class="fa fa-times"></i></a>
                    </div>
                    <div class="row" ng-show="reservationList.showExtendedFilter">
                        <div class="input-group">
                            <span class="input-group-addon">
                                <spring:message code="views.user"/>
                            </span>
                            <input id="userId" class="form-control" style="width: 300px;" ng-model="reservationList.userId" ui-select2="userOptions"/>
                        </div>
                        <a class="btn btn-default" href="" ng-click="reservationList.userId = null;"><i class="fa fa-times"></i></a>
                    </div>
                    <div class="row" ng-show="reservationList.showExtendedFilter">
                        <div class="input-group">
                            <span class="input-group-addon">
                                <spring:message code="views.index.reservations.participant"/>
                            </span>
                            <input id="participantUserId" class="form-control" style="width: 300px;" ng-model="reservationList.participantUserId" ui-select2="userOptions"/>
                        </div>
                        <a class="btn btn-default" href="" ng-click="reservationList.participantUserId = null;"><i class="fa fa-times"></i></a>
                    </div>
                    <div class="row" ng-show="reservationList.showExtendedFilter">
                        <div class="input-group">
                            <span class="input-group-addon">
                                <spring:message code="views.search"/>
                            </span>
                            <input type="text" class="form-control" style="width: 300px;" ng-model="reservationList.search"/>
                        </div>
                    </div>
                    <div class="row">
                        <div class="checkbox-inline">
                            <input id="showNotAllocated" type="checkbox" ng-model="reservationList.showNotAllocated"/>
                            <label for="showNotAllocated">
                                <spring:message code="views.index.reservations.showNotAllocated"/>
                            </label>
                        </div>
                        <%-- We don't want to show capacities for operators (because a lot of permanent rooms can be visible) --%>
                        <c:if test="${!isOperator}">
                            <div class="checkbox-inline">
                                <input id="showPermanentRoomCapacities" type="checkbox" ng-model="reservationList.showPermanentRoomCapacities"/>
                                <label for="showPermanentRoomCapacities">
                                    <spring:message code="views.index.reservations.showPermanentRoomCapacities"/>
                                </label>
                            </div>
                        </c:if>
                    </div>
                </form>
            </c:if>
            <%-- Filter: end --%>

        </div>
        <div class="spinner" ng-hide="ready || errorContent"></div>
        <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
        <table class="table table-striped table-hover" ng-show="ready">

            <%-- Table head: start --%>
            <thead>
            <tr>
                <c:if test="${advancedUserInterface}">
                    <th>
                        <pagination-sort column="DATETIME"><spring:message code="views.reservationRequest.createdAt"/></pagination-sort>
                    </th>
                    <th>
                        <pagination-sort column="USER"><spring:message code="views.reservationRequest.createdBy"/></pagination-sort>
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
                <th style="min-width: 150px; width: 150px;">
                    <spring:message code="views.list.action"/>
                    <pagination-sort-default class="pull-right"><spring:message code="views.pagination.defaultSorting"/></pagination-sort-default>
                </th>
            </tr>
            </thead>
            <%-- Table head: end --%>

            <tbody>

            <%-- Single reservation: start --%>
            <tr ng-repeat-start="reservationRequest in items" ng-class-odd="'odd'" ng-class-even="'even'"
                ng-class="{'deprecated': reservationRequest.isDeprecated}">
                <c:if test="${advancedUserInterface}">
                    <td>{{reservationRequest.dateTime}}</td>
                    <td>{{reservationRequest.user}}</td>
                </c:if>
                <td>{{reservationRequest.typeMessage}}</td>
                <td>
                    <spring:message code="views.index.reservations.showDetail" var="manageRoom"/>
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
                    <tag:listAction code="show" titleCode="views.index.reservations.showDetail" url="${detailUrl}" tabindex="2"/>
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
                        | <input type="checkbox" name="${deleteCheckboxName}" value="{{reservationRequest.id}}"/>
                    </span>
                </td>
            </tr>
            <%-- Single reservation: end --%>

            <%-- Capacities for single permanent room: start --%>
            <tr ng-repeat-end class="description" ng-class-odd="'odd'" ng-class-even="'even'"
                ng-class="{'deprecated': reservationRequest.isDeprecated}">
                <td ng-if="reservationRequest.type == 'PERMANENT_ROOM' && reservationList.showPermanentRoomCapacities" ng-controller="DashboardPermanentRoomCapacitiesController" colspan="8">
                    <div style="position: relative;">
                        <div style="position: absolute;  right: 0px; bottom: 0px;" ng-show="reservationRequest.isProvidable && reservationRequest.state != 'ALLOCATED_FINISHED'">
                            <a class="btn btn-default" href="${createPermanentRoomCapacityUrl}" tabindex="1">
                                <spring:message code="views.index.reservations.permanentRoomCapacity.create" arguments="{{reservationRequest.roomName}}"/>
                            </a>
                        </div>
                        <span><spring:message code="views.index.reservations.permanentRoomCapacity" arguments="{{reservationRequest.roomName}}"/>:</span>
                        <ul>
                            <li ng-repeat="capacity in items">
                                <a href="${permanentRoomCapacityDetailUrl}">{{capacity.roomParticipantCountMessage}}</a>
                                <spring:message code="views.index.reservations.permanentRoomCapacity.slot" arguments="{{capacity.earliestSlot}}"/>
                                <span ng-show="capacity.futureSlotCount">
                                    (<spring:message code="views.reservationRequestList.slotMore" arguments="{{capacity.futureSlotCount}}"/>)
                                </span>
                                <span class="reservation-request-state">(<tag:help label="{{capacity.stateMessage}}" cssClass="{{capacity.state}}"><span>{{capacity.stateHelp}}</span></tag:help>)</span>
                            </li>
                            <li ng-show="count > items.length">
                                <a href="${detailUrl}" tabindex="2">
                                    <spring:message code="views.index.reservations.permanentRoomCapacity.slotMore" arguments="{{count - items.length}}"/>...
                                </a>
                            </li>
                            <li  ng-hide="items.length">
                                <span class="empty"><spring:message code="views.list.none"/></span>
                            </li>
                        </ul>
                    </div>
                </td>
            </tr>
            <%-- Capacities for single permanent room: end --%>

            </tbody>
            <tbody>

            <%-- Empty row --%>
            <tr ng-hide="items.length">
                <td colspan="8" class="empty"><spring:message code="views.list.none"/></td>
            </tr>

            </tbody>
        </table>
        <pagination-pages ng-show="ready"><spring:message code="views.pagination.pages"/></pagination-pages>
    </div>
</div>
