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
            var calendar = uiCalendarConfig.calendars['meetingRoomsReservationsCalendar'];
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
                    var descriptionTitle = "<spring:message code="views.room.description"/>";
                    events.push({
                        title: event.description,
                        description: event.description,
                        createdBy: event.ownerName,
                        ownersEmail: event.ownersEmail,
                        start: event.start,
                        end: event.end
                    });
                });
                callback(events);

            })
        };
        $scope.renderCalender = function(calendar) {
            if(uiCalendarConfig.calendars[calendar]){
                uiCalendarConfig.calendars[calendar].fullCalendar('render');
            }
        };
        $scope.eventRender = function( event, element, view ) {
            var descriptionTitle = "<spring:message code="views.room.description"/>";
            var createdBy = "<spring:message code="views.reservationRequest.createdBy"/>";
            element.qtip({
                content: "<strong>" + descriptionTitle + ":</strong><br /><span>" + event.description + "</span><br />" +
                "<strong>" + createdBy + ":</strong><br /><span>" + event.createdBy + " (<a href=\"mailto:" + event.ownersEmail + "\">" + event.ownersEmail + "</a>)</span>",
                show: {
                    solo: true
                },
                hide: {
                    fixed: true,
                    delay: 600
                },
                style: {
                    classes: 'qtip-app'
                }
            });
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
                    right: 'prev,today,next agendaDay,agendaWeek,month'
                },
                loading: function (bool) {
                    if (bool) {
                        $('#directives-calendar').fadeTo(100,0.5);
                        $('#loadingImg').show();
                    } else {
                        $('#loadingImg').fadeOut();
                        $('#loadingImg').hide();
                        $('#directives-calendar').fadeTo(100,1);
                    }
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
                $scope.refreshCalendar();
            }
        });
        $scope.refreshCalendar = function() {
            var calendar = uiCalendarConfig.calendars['meetingRoomsReservationsCalendar'];
            calendar.fullCalendar('refetchEvents');
            calendar.fullCalendar('rerenderEvents');
        }
        $scope.$on("refresh-meetingRoomsReservationsCalendar", function() {
            $scope.initCalendar();
        });
    });
</script>

<div ng-controller="CalendarController">
    <div class="alert alert-warning"><spring:message code="views.index.meetingRooms.description"/></div>
    <button class="pull-right fa fa-refresh btn btn-default" ng-click="refreshCalendar()"></button>
    <div class="btn-group pull-right">
        <form class="form-inline">
            <label for="meetingRoomResourceId"><spring:message code="views.room"/>:</label>
            <input id="meetingRoomResourceId" ng-model="reservationsFilter.resourceId" ui-select2="resourceIdOptions"/>
        </form>
    </div>
    <div id="directives-calendar" class="calendar">
        <div class="alert-success calAlert" ng-show="alertMessage != undefined && alertMessage != ''">
            <h4>{{alertMessage}}</h4>
        </div>
        <div class="spinner centered-in-element" id="loadingImg"></div>
        <div class="calendar" ng-model="eventSources" calendar="meetingRoomsReservationsCalendar" ui-calendar="uiConfig.calendar"></div>
    </div>
</div>
