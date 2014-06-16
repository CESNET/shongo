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

    module.controller("ConfigurationController", function($scope, $application, $cookieStore, $compile){
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
        $scope.formatPeriod = function(period, count) {
            var text = "";
            if (typeof(count) == "number") {
                text += count + " ";
            }
            switch (period) {
                case "P1D": {
                    if (typeof(count) == "boolean" && count == true) { return text + "<spring:message code="views.period.days"/>"; }
                    else if (count > 1 && count <= 4)                { return text + "<spring:message code="views.period.days4"/>"; }
                    else if (count > 4)                              { return text + "<spring:message code="views.period.daysN"/>"; }
                    else                                             { return text + "<spring:message code="views.period.day"/>"; }
                }
                case "P1W": {
                    if (typeof(count) == "boolean" && count == true) { return text + "<spring:message code="views.period.weeks"/>"; }
                    else if (count > 1 && count <= 4)                { return text + "<spring:message code="views.period.weeks4"/>"; }
                    else if (count > 4)                              { return text + "<spring:message code="views.period.weeksN"/>"; }
                    else                                             { return text + "<spring:message code="views.period.week"/>"; }
                }
                case "P1M": {
                    if (typeof(count) == "boolean" && count == true) { return text + "<spring:message code="views.period.months"/>"; }
                    else if (count > 1 && count <= 4)                { return text + "<spring:message code="views.period.months4"/>"; }
                    else if (count > 4)                              { return text + "<spring:message code="views.period.monthsN"/>"; }
                    else                                             { return text + "<spring:message code="views.period.month"/>"; }
                }
                case "P1Y": {
                    if (typeof(count) == "boolean" && count == true) { return text + "<spring:message code="views.period.years"/>"; }
                    else if (count > 1 && count <= 4)                { return text + "<spring:message code="views.period.years4"/>"; }
                    else if (count > 4)                              { return text + "<spring:message code="views.period.yearsN"/>"; }
                    else                                             { return text + "<spring:message code="views.period.year"/>"; }
                }
            }
        };
        $scope.moveStart = function(periodCount) {
            var start = moment($scope.start);
            start = $scope.dateTimeAdd(start, $scope.period, periodCount);
            $scope.start = start.format("YYYY-MM-DD");
            var end = $scope.dateTimeAdd(start, $scope.period, $scope.periodCount);
            $scope.end = $scope.dateTimeAdd(end, "P1D", - 1).format("YYYY-MM-DD");
        };
        $scope.updateContent = function(refresh) {
            // Store settings to cookie
            var settings = {
                period: $scope.period,
                periodCount: $scope.periodCount,
                start: moment($scope.start).diff(moment().weekday(0), "days"),
                style: $scope.style
            };
            $cookieStore.put("resourceCapacityUtilization", settings);

            var url = "<tag:url value="<%= ClientWebUrl.RESOURCE_CAPACITY_UTILIZATION_TABLE %>"/>";
            url += "?start=" + $scope.start + "T00:00:00";
            url += "&end=" + $scope.end + "T23:59:59";
            url += "&period=" + $scope.period;
            url += "&style=" + $scope.style;
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

        // Initial configuration
        $scope.period = "P1D";
        $scope.periodCount = 7;
        $scope.start = moment().weekday(0).format("YYYY-MM-DD");
        $scope.style = "RELATIVE";

        // Load settings from cookie
        var settings = $cookieStore.get("resourceCapacityUtilization");
        if (settings != null) {
            $scope.period = settings.period;
            $scope.periodCount = settings.periodCount;
            $scope.start = moment($scope.start).add('days', settings.start + 1).format("YYYY-MM-DD");
            $scope.style = settings.style;
        }

        // Update changes in configuration
        $scope.$watch("period", function(){
            // Update end
            var end = $scope.dateTimeAdd(moment($scope.start), $scope.period, $scope.periodCount);
            $scope.end = $scope.dateTimeAdd(end, "P1D", - 1).format("YYYY-MM-DD");
        });
        $scope.$watch("[start,end]", function(){
            // Update period count
            var start = moment($scope.start);
            var end = moment($scope.end);
            if (start > end) {
                $scope.end = $scope.dateTimeAdd(start, $scope.period, $scope.periodCount).format("YYYY-MM-DD")
            }
            else {
                $scope.periodCount = 0;
                if ($scope.period == "P1D") {
                    $scope.periodCount++;
                }
                while (start < end || $scope.periodCount == 0) {
                    $scope.periodCount++;
                    $scope.dateTimeAdd(start, $scope.period);
                }
            }
            $scope.updateContent();
        }, true);
        $scope.$watch("style", function(newValue, oldValue){
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

<div ng-app="jsp:resourceCapacityUtilization" ng-controller="ConfigurationController" class="jspResourceCapacityUtilization">
    <form class="form-inline">
        <label for="period"><spring:message code="views.resourceCapacityUtilization.units"/>:</label>
        &nbsp;
        <select id="period" class="form-control" tabindex="${tabIndex}" ng-model="period">
            <c:forEach var="period" items="P1D,P1W,P1M,P1Y">
                <option value="${period}">{{formatFirstUpper(formatPeriod("${period}", true))}}</option>
            </c:forEach>
        </select>
        &nbsp;
        <label><spring:message code="views.interval"/>:</label>
        &nbsp;
        <div class="input-group" style="display: inline-table;">
            <span class="input-group-addon">
                <spring:message code="views.interval.from"/>:
            </span>
            <input id="start" class="form-control form-picker" type="text" date-picker="true" readonly="true" style="width: 100px;" ng-model="start"/>
        </div>
        &nbsp;
        <div class="input-group" style="display: inline-table">
            <span class="input-group-addon">
                <spring:message code="views.interval.to"/>:
            </span>
            <input id="end" class="form-control form-picker" type="text" date-picker="true" readonly="true" style="width: 100px;" ng-model="end"/>
        </div>
        <div class="pull-right">
            <spring:message var="forward" code="views.resourceCapacityUtilization.forward"/>
            <spring:message var="backward" code="views.resourceCapacityUtilization.backward"/>
            <div class="btn-group-divided">
                <a href="" ng-click="updateContent(true)" class="btn btn-default" title="<spring:message code="views.button.refresh"/>"><span class="fa fa-refresh"></span></a>
            </div>
            <div class="btn-group-divided">
                <a class="btn btn-default" href="" ng-click="moveStart(-periodCount)" title="{{formatPeriod(period, periodCount)}} ${backward}">&lt;&lt;&lt;</a>
                <a class="btn btn-default" href="" ng-click="moveStart(periodCount)" title="{{formatPeriod(period, periodCount)}} ${forward}">&gt;&gt;&gt;</a>
            </div>
            <div class="btn-group-divided">
                <a class="btn btn-default" href="" ng-click="moveStart(-floor(periodCount / 2))" title="{{formatPeriod(period, floor(periodCount / 2))}} ${backward}">&lt;&lt;</a>
                <a class="btn btn-default" href="" ng-click="moveStart(floor(periodCount / 2))" title="{{formatPeriod(period, floor(periodCount / 2))}} ${forward}">&gt;&gt;</a>
            </div>
            <div class="btn-group-divided">
                <a class="btn btn-default" href="" ng-click="moveStart(-1)" title="{{formatPeriod(period, 1)}} ${backward}">&lt;</a>
                <a class="btn btn-default" href="" ng-click="moveStart(1)" title="{{formatPeriod(period, 1)}} ${forward}">&gt;</a>
            </div>
        </div>
    </form>
    <form class="form-inline" style="padding-top: 10px;">
        <label for="period"><spring:message code="views.resourceCapacityUtilization.show"/>:</label>
        &nbsp;
        <label class="radio-inline">
            <input type="radio" name="show" value="ABSOLUTE" tabindex="${tabIndex}" ng-model="style"/>
            <span><spring:message code="views.resourceCapacityUtilization.show.absolute"/></span>
        </label>
        <label class="radio-inline">
            <input type="radio" name="show" value="RELATIVE" tabindex="${tabIndex}" ng-model="style"/>
            <span><spring:message code="views.resourceCapacityUtilization.show.relative"/></span>
        </label>
    </form>

    <hr/>

    <div id="table">
    </div>

</div>

