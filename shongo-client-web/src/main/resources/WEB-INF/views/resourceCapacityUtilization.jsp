<%--
  -- Page displaying resource capacity utilization.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="tabIndex" value="1"/>

<script type="text/javascript">
    var module = angular.module('jsp:resourceCapacityUtilization', ['ngApplication', 'ngDateTime', 'ngTooltip', 'ngCookies', 'ngSanitize']);

    module.controller("ConfigurationController", function($scope, $application, $compile){
        $scope.floor = function(number) {
            return Math.floor(number);
        };
        $scope.dateTimeAdd = function(dateTime, period, periodCount) {
            periodCount = (periodCount != null ? periodCount : 1);
            switch (period) {
                case "P1D": {
                    dateTime.add("days", periodCount);
                    break;
                }
                case "P1W": {
                    dateTime.add("weeks", periodCount);
                    break;
                }
                case "P1M": {
                    dateTime.add("months", periodCount);
                    break;
                }
                case "P1Y": {
                    dateTime.add("years", periodCount);
                    break;
                }
            }
            return dateTime;
        };
        $scope.formatFirstUpper = function(string) {
            return string.substring(0, 1).toUpperCase() + string.substring(1);
        };
        $scope.formatPeriod = function(period, plural) {
            switch (period) {
                case "P1D": {
                    if (plural) {
                        return "days";
                    }
                    else {
                        return "day";
                    }
                }
                case "P1W": {
                    if (plural) {
                        return "weeks";
                    }
                    else {
                        return "week";
                    }
                }
                case "P1M": {
                    if (plural) {
                        return "months";
                    }
                    else {
                        return "month";
                    }
                }
                case "P1Y": {
                    if (plural) {
                        return "years";
                    }
                    else {
                        return "year";
                    }
                }
            }
        };
        $scope.moveStart = function(periodCount) {
            var start = moment($scope.start);
            start = $scope.dateTimeAdd(start, $scope.period, periodCount);
            $scope.start = start.format("YYYY-MM-DD");
            $scope.end = $scope.dateTimeAdd(start, $scope.period, $scope.periodCount).format("YYYY-MM-DD");
        };
        $scope.updateContent = function(refresh) {
            var url = "<tag:url value="<%= ClientWebUrl.RESOURCE_CAPACITY_UTILIZATION_TABLE %>"/>";
            url += "?start=" + $scope.start + "T00:00:00";
            url += "&end=" + $scope.end + "T23:59:59";
            url += "&period=" + $scope.period;
            url += "&type=" + $scope.type;
            if (refresh) {
                url += "&refresh=true";
            }
            $("#table").html("<div class='spinner'></div>");
            $.ajax({
                type: "GET",
                url: url,
                cache:false,
                success: function(table) {
                    $("#table").html($compile(table)($scope));
                },
                fail: $application.handleAjaxFailure
            });
        };

        // Initial range
        $scope.period = "P1D";
        $scope.periodCount = 7;
        $scope.start = moment().weekday(0).format("YYYY-MM-DD");
        // Configuration
        $scope.type = "RELATIVE";


        // Update changes in configuration
        $scope.$watch("period", function(){
            // Update end
            $scope.end = $scope.dateTimeAdd(moment($scope.start), $scope.period, $scope.periodCount).format("YYYY-MM-DD");
        });
        $scope.$watch("[start,end]", function(){
            // Update period count
            var start = moment($scope.start);
            var end = moment($scope.end);
            $scope.periodCount = 0;
            while (start < end || $scope.periodCount == 0) {
                $scope.periodCount++;
                $scope.dateTimeAdd(start, $scope.period);
            }
            $scope.updateContent();
        }, true);
        $scope.$watch("type", function(newValue, oldValue){
            if (newValue != oldValue) {
                $scope.updateContent();
            }
        });
    });

    $(document).ready(function(){
        var dateTime = moment();
        //$("#start").val(dateTime.weekday(0).format("YYYY-MM-DD"));

    });
</script>

<div ng-app="jsp:resourceCapacityUtilization" ng-controller="ConfigurationController">
    <form class="form-inline">
        <label for="period">Units:</label>
        &nbsp;
        <select id="period" class="form-control" tabindex="${tabIndex}" ng-model="period">
            <c:forEach var="period" items="P1D,P1W,P1M,P1Y">
                <option value="${period}">{{formatFirstUpper(formatPeriod("${period}", true))}}</option>
            </c:forEach>
        </select>
        &nbsp;
        <label><spring:message code="views.resourceReservations.interval"/>:</label>
        &nbsp;
        <div class="input-group" style="display: inline-table;">
            <span class="input-group-addon">
                From:
            </span>
            <input id="start" class="form-control" type="text" date-picker="true" readonly="true" style="width: 100px; background-color: white;" ng-model="start"/>
        </div>
        &nbsp;
        <div class="input-group" style="display: inline-table">
            <span class="input-group-addon">
                To:
            </span>
            <input id="end" class="form-control" type="text" date-picker="true" readonly="true" style="width: 100px; background-color: white;" ng-model="end"/>
        </div>
        <div class="pull-right">
            <div class="btn-group-divided">
                <a class="btn btn-default" href="" ng-click="moveStart(-periodCount)" title="{{periodCount + ' ' + formatPeriod(period, true)}} backward">&lt;&lt;&lt;</a>
                <a class="btn btn-default" href="" ng-click="moveStart(periodCount)" title="{{periodCount + ' ' + formatPeriod(period, true)}} forward">&gt;&gt;&gt;</a>
            </div>
            <div class="btn-group-divided">
                <a class="btn btn-default" href="" ng-click="moveStart(-floor(periodCount / 2))" title="{{floor(periodCount / 2) + ' ' + formatPeriod(period, true)}} backward">&lt;&lt;</a>
                <a class="btn btn-default" href="" ng-click="moveStart(floor(periodCount / 2))" title="{{floor(periodCount / 2) + ' ' + formatPeriod(period, true)}} forward">&gt;&gt;</a>
            </div>
            <div class="btn-group-divided">
                <a class="btn btn-default" href="" ng-click="moveStart(-1)" title="One {{formatPeriod(period)}} backward">&lt;</a>
                <a class="btn btn-default" href="" ng-click="moveStart(1)" title="One {{formatPeriod(period)}} forward">&gt;</a>
            </div>
        </div>
    </form>
    <form class="form-inline" style="padding-top: 10px;">
        <label for="period">Show:</label>
        &nbsp;
        <label class="radio-inline">
            <input type="radio" name="show" value="ABSOLUTE" tabindex="${tabIndex}" ng-model="type"/>
            <span>absolute values</span>
        </label>
        <label class="radio-inline">
            <input type="radio" name="show" value="RELATIVE" tabindex="${tabIndex}" ng-model="type"/>
            <span>relative values</span>
        </label>
    </form>

    <hr/>

    <div id="table">
    </div>

</div>

