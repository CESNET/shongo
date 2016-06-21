<%--
  -- Child reservation requests.
  --%>
<%@ tag body-content="empty" %>
<%@ tag import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="detailUrl" required="true" %>

<script type="text/javascript">
    var module = angular.provideModule('tag:reservationRequestChildren', ['ngPagination', 'ngSanitize']);
    function RefreshController($scope, $timeout) {
        // Period for refreshing empty list
        $scope.refreshTimeout = 5;
        // Number of refreshes for empty list
        $scope.refreshCount = 5;
        // After data is set
        $scope.$parent.onSetData = function(data) {
            if (data.length == 0 && $scope.refreshCount-- > 0) {
                if ($scope.timeout != null) {
                    $timeout.cancel($scope.timeout);
                }
                $scope.timeout = $timeout(function() {
                    $scope.timeout = null;
                    $scope.$parent.refresh();
                }, $scope.refreshTimeout * 1000);
            }
            else {
                $scope.$parent.onSetData = null;
            }
        };
    }
</script>

<tag:url var="childListUrl" value="<%= ClientWebUrl.DETAIL_RESERVATION_REQUEST_CHILDREN %>">
    <tag:param name="objectId" value=":id" escape="false"/>
</tag:url>
<tag:url var="childDetailUrl" value="${detailUrl}">
    <tag:param name="objectId" value="{{childReservationRequest.id}}" escape="false"/>
    <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
</tag:url>
<tag:url var="childRoomManagementUrl" value="<%= ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_VIEW %>">
    <tag:param name="objectId" value="{{childReservationRequest.id}}" escape="false"/>
    <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
</tag:url>
<tag:url var="childReservationDelete" value="<%= ClientWebUrl.WIZARD_ROOM_PERIODIC_REMOVE %>">
    <tag:param name="reservationRequestId" value="{{childReservationRequest.parentReservationRequestId}}" escape="false"/>
    <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
    <tag:param name="excludeReservationId" value="{{childReservationRequest.reservationId}}" escape="false"/>
</tag:url>

<div ng-controller="PaginationController"
     ng-init="init('reservationRequestDetail.children', '${childListUrl}', {id: '${reservationRequest.id}'})">
    <div ng-controller="RefreshController"></div>
    <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
    <spring:message code="views.button.refresh" var="paginationRefresh"/>
    <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
        <spring:message code="views.pagination.records"/>
    </pagination-page-size>
    <h2><spring:message code="views.reservationRequestDetail.children"/></h2>
    <div class="spinner" ng-hide="ready || errorContent"></div>
    <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
    <table class="table table-striped table-hover" ng-show="ready">
        <thead>
        <tr>
            <th width="320px"><pagination-sort column="SLOT">
                <spring:message code="views.reservationRequest.slot"/></pagination-sort>
            </th>
            <th><pagination-sort column="STATE">
                <spring:message code="views.reservationRequest.state"/></pagination-sort>
            </th>
            <th><spring:message code="views.room.aliases"/></th>
            <th style="min-width: 85px; width: 85px;">
                <spring:message code="views.list.action"/>
                <pagination-sort-default class="pull-right"><spring:message code="views.pagination.defaultSorting"/></pagination-sort-default>
            </th>
        </tr>
        </thead>
        <tbody>
        <tr ng-repeat="childReservationRequest in items">
            <td>{{childReservationRequest.slot}}</td>
            <td class="reservation-request-state">
                <tag:help label="{{childReservationRequest.stateMessage}}" cssClass="{{childReservationRequest.state}}">
                    <span>{{childReservationRequest.stateHelp}}</span>
                </tag:help>
            </td>
            <td ng-controller="HtmlController">
                <div ng-switch on="isEmpty(childReservationRequest.roomAliasesDescription)" style="display: inline-block;">
                    <div ng-switch-when="false">
                        <c:set var="executableAliases">
                            <span ng-bind-html="html(childReservationRequest.roomAliases)"></span>
                        </c:set>
                        <tag:help label="${executableAliases}" selectable="true">
                            <span ng-bind-html="html(childReservationRequest.roomAliasesDescription)"></span>
                        </tag:help>
                    </div>
                    <span ng-switch-when="true"
                          ng-bind-html="roomAliases(childReservationRequest)"></span>
                </div>
            </td>
            <td>
                <tag:listAction code="show" url="${childDetailUrl}" tabindex="2"/>
                <span ng-show="childReservationRequest.roomStateAvailable">
                    | <tag:listAction code="manage" url="${childRoomManagementUrl}" tabindex="2"/>
                </span>
                <span ng-show="childReservationRequest.reservationId">
                    | <tag:listAction code="delete" url="${childReservationDelete}" tabindex="2" />
                </span>
            </td>
        </tr>
        </tbody>
        <tbody>
        <tr ng-hide="items.length">
            <td colspan="4" class="empty">
                <c:choose>
                    <c:when test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
                        <div class="alert alert-warning">
                            <spring:message code="views.reservationRequestDetail.waitingForAllocation"/>...
                        </div>
                    </c:when>
                    <c:otherwise>
                        <spring:message code="views.list.none"/>
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>
        </tbody>
    </table>
    <pagination-pages ng-show="ready"><spring:message code="views.pagination.pages"/></pagination-pages>
</div>