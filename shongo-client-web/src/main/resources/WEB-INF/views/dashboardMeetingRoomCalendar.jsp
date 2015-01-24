<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<security:authentication property="principal.userId" var="userId"/>
<tag:url var="meetingRoomReservationsUrl" value="<%= ClientWebUrl.MEETING_ROOM_RESERVATION_LIST_DATA %>">
    <tag:param name="specification-type" value="MEETING_ROOM"/>
</tag:url>
<c:set var="contextPath" value="${pageContext.request.contextPath}"/>


<script type="text/javascript">
    var calendarModule = angular.module('mrReservationsCalendar', ['ui.calendar', 'ui.bootstrap']);

    calendarModule.controller("CalendarController", function($scope,$compile,uiCalendarConfig) {
        var date = new Date();
        var d = date.getDate();
        var m = date.getMonth();
        var y = date.getFullYear();

        $scope.initCalendar = function() {
            var calendar = uiCalendarConfig.calendars['myCalendar2'];
            if (calendar) {
                calendar.fullCalendar('render');
            }
        };

        $scope.eventsF = function(start, end, timezone, callback) {
            var resourceId = $scope.resourceIdOptions.data[0].id;
            if (typeof $scope.reservationsFilter.resourceId == 'object') {
                resourceId = $scope.reservationsFilter.resourceId.id;
            }
            $.ajax("${meetingRoomReservationsUrl}&interval-from="+start.format()+"&interval-to="+end.format()+"&resource-id="+resourceId, {
                dataType: "json"
            }).done(function (data) {
                var events = [];
                data.items.forEach(function(event) {
                    events.push({
                        title: event.title,
                        start: event.start,
                        end: event.end
                    });
                });
                callback(events);

            })
        };


        $scope.changeView = function(view,calendar) {
            uiCalendarConfig.calendars[calendar].fullCalendar('changeView',view);
        };
        $scope.renderCalender = function(calendar) {
            if(uiCalendarConfig.calendars[calendar]){
                uiCalendarConfig.calendars[calendar].fullCalendar('render');
            }
        };
        $scope.eventRender = function( event, element, view ) {
            element.attr('popover', event.title);
            element.attr('popover-title', 'Hello world');
            element.attr('popover-trigger', 'mouseenter');
            $compile(element)($scope);
        };


        $scope.uiConfig = {
            calendar:{
                lang: '${requestContext.locale.language}',
                defaultView: 'agendaWeek',
                height: 'auto',
                editable: false,
                lazyFetching: false,
                header:{
                    left: 'title',
                    center: '',
                    right: 'today prev,next'
                },
                eventRender: $scope.eventRender
            }
        };

        $scope.eventSources = [$scope.eventsF];

        $scope.resourceIdOptions = {
            escapeMarkup: function (markup) {
                return markup;
            },
            data: [
                <c:forEach items="${meetingRoomResources}" var="meetingRoomResource">
                {id: "${meetingRoomResource.key}", text: "${meetingRoomResource.value}"},
                </c:forEach>
            ]
        }

        $scope.reservationsFilter = {
            resourceId: $scope.resourceIdOptions.data[0].id
        };

        $scope.$watch('reservationsFilter.resourceId', function(newResourceId, oldResourceId, scope) {
            if ($scope.$parent.$tab.active && typeof newResourceId == "object") {
                var calendar = uiCalendarConfig.calendars['myCalendar2'];

                calendar.fullCalendar('refetchEvents');
                calendar.fullCalendar('rerenderEvents');
            }
        });
    });
</script>

<div ng-controller="CalendarController">
    <div ng-controller="PaginationController"
         ng-init="init('meetingRoomListA', initCalendar, null, 'refresh-meetingRoomsReservationsCalendar')">
        <form class="form-inline">
            <label for="meetingRoomResourceId"><spring:message code="views.room"/>:</label>
            <input id="meetingRoomResourceId" ng-model="reservationsFilter.resourceId" ui-select2="resourceIdOptions"/>
        </form>

        <section id="directives-calendar">
            <div class="alert-success calAlert" ng-show="alertMessage != undefined && alertMessage != ''">
                <h4>{{alertMessage}}</h4>
            </div>
            <div class="btn-toolbar">
                <p class="pull-right lead">Calendar Two View Options</p>
                <div class="btn-group">
                    <button class="btn btn-success" ng-click="changeView('agendaDay', 'myCalendar2')">AgendaDay</button>
                    <button class="btn btn-success" ng-click="changeView('agendaWeek', 'myCalendar2')">AgendaWeek</button>
                    <button class="btn btn-success" ng-click="changeView('month', 'myCalendar2')">Month</button>
                </div>
            </div>

            <div class="calendar" ng-model="eventSources" calendar="myCalendar2" ui-calendar="uiConfig.calendar"></div>
        </section>
    </div>
</div>
