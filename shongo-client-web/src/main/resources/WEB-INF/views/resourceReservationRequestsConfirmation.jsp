<%--
  -- Page displaying resource reservation requests requiring confirmation. Shows only resources owned by current user.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<tag:url var="reservationRequestConfirmationDataUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_CONFIRMATION_DATA %>">
    <tag:param name="count" value="10"/>
</tag:url>
<tag:url var="reservationRequestConfirmUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_CONFIRM %>">
</tag:url>
<tag:url var="reservationRequestDenyUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DENY %>">
</tag:url>

<script type="text/javascript">
    var module = angular.module('jsp:resourceReservationRequests', ['ngApplication', 'ngDateTime', 'ngPagination', 'ngTooltip', 'ngCookies', 'ngSanitize', 'ui.select2']);
    module.controller('ResourceReservationRequestConfirmationController', function ($scope, $timeout, $application) {
        var dateTime = moment();

        $scope.resources = {
            <c:forEach items="${resources}" var="resource">
                "${resource.key}" : "${resource.value}",
            </c:forEach>
        };

        $scope.resourceIdOptions = {
            escapeMarkup: function (markup) {
                return markup;
            },
            data: [
                {id: "all", text: "<spring:message code="views.resource.all"/>"}
                <c:forEach items="${resources}" var="resource">
                , {id: "${resource.key}", text: "<b>${resource.value}</b>"}
                </c:forEach>
            ]
        };

        $scope.reservationsFilter = {
            intervalFrom: dateTime.format("YYYY-MM-DD"),
            intervalTo: dateTime.date(+30).format("YYYY-MM-DD"),
            resourceId: $scope.resourceIdOptions.data[0].id
        };

        $scope.$watchCollection('[reservationsFilter.intervalFrom, reservationsFilter.intervalTo]', function (newValues, oldValues, scope) {
            var startClass = document.getElementById("start").className;
            var endClass = document.getElementById("start").className;
            if (new Date(newValues[0]) > new Date(newValues[1])) {
                alert("<spring:message code='validation.field.invalidInterval'/>");
                document.getElementById("start").className += " fa-red";
                document.getElementById("end").className += " fa-red";
                return;
            }
            document.getElementById("start").className = startClass.replace(" fa-red", "");
            document.getElementById("end").className = endClass.replace(" fa-red", "");
            $scope.$$childHead.refresh();
        });
        $("#resourceId").change(function () {
            $scope.$$childHead.refresh();
        });

        $scope.getResourceReservationRequestsListDataUrl = function () {
            var url = "${reservationRequestConfirmationDataUrl}";
            if ($scope.reservationsFilter.intervalFrom != null && $scope.reservationsFilter.intervalFrom != "") {
                url += "&interval-from=" + $scope.reservationsFilter.intervalFrom;
            }
            if ($scope.reservationsFilter.intervalTo != null && $scope.reservationsFilter.intervalTo != "") {
                url += "&interval-to=" + $scope.reservationsFilter.intervalTo;
            }
            if ($("#resourceId") != null && $("#resourceId").val() != null &&
            $("#resourceId").val() != "" && $("#resourceId").val() != "all") {
                url += "&resource-id=" + encodeURIComponent($("#resourceId").val());
            }
            return url;
        };

        $scope.updateRequestConfirmation = function (url) {
            $.ajax({
                url: url,
                success: function(){
                    $scope.$$childHead.refresh();
                },
                error: function(){
                    $scope.$$childHead.refresh();
                    $application.handleAjaxFailure;
                }
            });
        };
    });
</script>

<div ng-app="jsp:resourceReservationRequestsConfirmation">
    <div ng-controller="ResourceReservationRequestConfirmationController">
        <div ng-controller="PaginationController"
             ng-init="init('resourceReservationRequests',getResourceReservationRequestsListDataUrl);">

            <form class="form-inline">
                <label for="resourceId"><spring:message code="views.resource"/>:</label>
                <input id="resourceId" ng-model="reservationsFilter.resourceId" ui-select2="resourceIdOptions" class="min-input"/>
                &nbsp;
                <label for="start"><spring:message code="views.interval"/>:</label>

                <div class="input-group" style="display: inline-table;">
                <span class="input-group-addon">
                    <spring:message code="views.interval.from"/>:
                </span>
                    <input id="start" class="form-control form-picker" type="text" date-picker="true" readonly="true"
                           style="width: 100px;" ng-model="reservationsFilter.intervalFrom"/>
                </div>
                <div class="input-group" style="display: inline-table">
                <span class="input-group-addon">
                    <spring:message code="views.interval.to"/>:
                </span>
                    <input id="end" class="form-control form-picker" type="text" date-picker="true" readonly="true"
                           style="width: 100px;" ng-model="reservationsFilter.intervalTo"/>
                </div>

                <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
                <spring:message code="views.button.refresh" var="paginationRefresh"/>
                <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}"
                                      refresh="${paginationRefresh}">
                    <spring:message code="views.pagination.records"/>
                </pagination-page-size>
            </form>

            <div class="spinner" ng-hide="ready || errorContent"></div>
            <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
            <table class="table table-striped table-hover" ng-show="ready" id="resourceRequests">
                <thead>
                <tr>
                    <th width="200px">
                        <spring:message code="views.resource"/>
                    </th>
                    <th width="200px">
                        <spring:message code="views.resourceReservationRequests.confirmation.requestedBy"/>
                    </th>
                    <th>
                        <%--<pagination-sort column="SLOT"><spring:message code="views.room.slot"/></pagination-sort>--%>
                        <spring:message code="views.room.slot"/>
                    </th>
                    <th>
                        <spring:message code="views.room.description"/>
                    </th>
                    <th style="min-width: 65px; width: 65px;">
                        <spring:message code="views.list.action"/>
                        <%--<pagination-sort-default class="pull-right"><spring:message code="views.pagination.defaultSorting"/></pagination-sort-default>--%>
                    </th>
                </tr>
                </thead>
                <tbody>
                <tr ng-repeat="request in items">
                    <td>
                        <span ng-bind="resources['{{request.resourceId}}']" />
                    </td>
                    <td>
                        <tag:help label="{{request.user}}" selectable="true">
                    <span ng-show="request.userEmail">
                        <strong><spring:message code="views.room.ownerEmail"/></strong>
                        <br/>
                        <a href="mailto: {{request.userEmail}}">{{request.userEmail}}</a>
                    </span>
                        </tag:help>
                    </td>
                    <td>
                        <span ng-bind-html="request.slot"></span>
                    </td>
                    <td>{{request.description}}</td>
                    <td>
                        <a href="" ng-click="updateRequestConfirmation('${reservationRequestConfirmUrl}?reservationRequestId=' + request.id)" title="<spring:message code="views.list.action.confirm.title" />">
                            <i class="fa fa-check"></i></a>
                        &nbsp;|&nbsp;
                        <a href="" ng-click="updateRequestConfirmation('${reservationRequestDenyUrl}?reservationRequestId=' + request.id)" title="<spring:message code="views.list.action.deny.title" />">
                            <i class="fa  fa-times fa-red"></i></a>
                    </td>
                </tr>
                </tbody>
                <tbody>
                <tr ng-hide="items.length">
                    <td colspan="5" class="empty"><spring:message code="views.list.none"/></td>
                </tr>
                </tbody>
            </table>
            <pagination-pages ng-show="ready"><spring:message code="views.pagination.pages"/></pagination-pages>
        </div>
    </div>
</div>
