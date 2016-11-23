<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<security:authentication property="principal.userId" var="userId"/>
<tag:url var="meetingRoomReservationsUrl" value="<%= ClientWebUrl.MEETING_ROOM_RESERVATION_LIST_DATA %>"/>
<tag:url var="meetingRoomIcsUrl" value="<%= ClientWebUrl.MEETING_ROOM_ICS %>">
    <tag:param name="objectUriKey" value="" escape="false" />
</tag:url>
<%-- Has to be without back-url for form action --%>
<tag:url var="meetingRoomBookUrl" value="<%= ClientWebUrl.WIZARD_MEETING_ROOM_BOOK %>"/>
<tag:url var="periodicMeetingRoomModifyUrl" value="<%= ClientWebUrl.WIZARD_MODIFY %>">
    <tag:param name="reservationRequestId" value="\" + event.parentRequestId + \"" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="singleMeetingRoomModifyUrl" value="<%= ClientWebUrl.WIZARD_MODIFY %>">
    <tag:param name="reservationRequestId" value="\" + event.requestId + \"" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="meetingRoomDeleteUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DELETE %>">
    <tag:param name="reservationRequestId" value="\" + event.requestId + \"" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="meetingRoomDeleteAllUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DELETE %>">
    <tag:param name="reservationRequestId" value="\" + event.parentRequestId + \"" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="childReservationDelete" value="<%= ClientWebUrl.WIZARD_ROOM_PERIODIC_REMOVE %>">
    <tag:param name="reservationRequestId" value="\" + event.parentRequestId + \"" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}" escape="false"/>
    <tag:param name="excludeReservationId" value="\" + event.id + \"" escape="false"/>
</tag:url>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<script type="text/javascript">
    //var calendarModule = angular.module('mrReservationsCalendar', ['ui.calendar', 'ui.bootstrap']);
    var http = location.protocol;
    var slashes = http.concat("//");
    var host = slashes.concat(window.location.hostname);
    var hostUrl = host.concat(":" + location.port);
    var calendarUrlBase = hostUrl + "${meetingRoomIcsUrl}";
    var exportCalendarMessage = "<strong><spring:message code='views.index.meetingRooms.calendarExport.message'/></strong><br />";

    module.controller("CalendarController", function ($scope, $compile, $application, uiCalendarConfig) {
        var date = new Date();
        var d = date.getDate();
        var m = date.getMonth();
        var y = date.getFullYear();

        // default values
        $scope.highlightOwnedReservations = false;
        var newReservationRequestId = "new_reservation_request_id";

        $scope.formatInterval = function(start, end) {
            var locale = '${requestContext.locale.language}';
            var interval = "";
            if (moment(start).isBefore(end, "day")) {
                interval += moment(start).lang(locale).format("lll");
                interval += "<br />";
                interval += moment(end).lang(locale).format("lll");
            } else {
                interval = moment(start).lang(locale).format("ll");
                interval += " ";
                interval += moment(start).lang(locale).format("LT");
                interval += " - ";
                interval += moment(end).lang(locale).format("LT");
            }
            return interval;
        }

        // Show centered tooltip dialog, @see qTip modal tooltips
        $scope.dialogue = function (content, title, calendar) {
            $('<div />').qtip({
                content: {
                    text: content,
                    button: '<spring:message code="views.button.cancel" />',
                    title: title
                },
                position: {
                    my: 'center', at: 'center',
                    target: $(window)
                },
                show: {
                    ready: true,
                    solo: true,
                    modal: {
                        on: true,
                        blur: false
                    }
                },
                hide: false,
                style: {
                    classes: 'dialogue qtip-light qtipForm',
                    width: 400,
                },
                events: {
                    render: function(event, api) {
                        $('button', api.elements.content).click(function(e) {
                            api.hide(e);
                        });
                    },
                    hide: function(event, api) {
                        api.destroy();
                        calendar.fullCalendar('removeEvents', newReservationRequestId);
                        calendar.fullCalendar('unselect');
                    }
                },
            });
        }

        // Prompt dialog for confirmation of reservation request
        $scope.reservationRequestPrompt = function(start, end, calendar) {
            var bookReservationParamUrl = "${meetingRoomBookUrl}";
            bookReservationParamUrl += "?start=";
            bookReservationParamUrl += start.toISOString();
            bookReservationParamUrl += "&end=";
            bookReservationParamUrl += end.toISOString();
            bookReservationParamUrl += "&resourceId=";
            bookReservationParamUrl += $scope.reservationsFilter.resourceId.id;

            var form = $('<form />', {
                        action: '${meetingRoomBookUrl}',
                        method: 'POST'
                    });
            form.append(
                    $('<input />', {
                                type: 'hidden',
                                name: 'resourceId',
                                value: $scope.reservationsFilter.resourceId.id
                    })
            );
            form.append(
                    $('<input />', {
                                type: 'hidden',
                                name: 'start',
                                value: start.toISOString()
                    })
            );
            form.append(
                    $('<input />', {
                                type: 'hidden',
                                name: 'end',
                                value: end.toISOString()
                    })
            );
            form.append(
                    $('<label />', {
                                html: '<spring:message code="views.reservationRequest.slot" />:'
                    })
            ).append($('<br />'));
            form.append(
                    $('<div />', {
                                html: $scope.formatInterval(start, end)
                    })
            ).append($('<br />'));
            form.append(
                    $('<label />', {
                                text: '<spring:message code="views.room.description" />:',
                                for: 'description',
                                style: 'padding-right: 5px'
                    })
            ).append($('<br />'));
            form.append(
                    $('<input />', {
                                type: 'text',
                                id: 'description',
                                name: 'description',
                                style: 'width:100%'
                    })
            ).append($('<br />')).append($('<br />'));

            var formConfirm = $("<div>", {
                style: 'text-align: right;'
            });
            formConfirm.append($('<a>', {
                href: bookReservationParamUrl,
                text: '<spring:message code="views.button.editMore" />',
                style: 'margin-right:5px',
                class: 'btn btn-default btn-xs'
            }));
            formConfirm.append($('<input>', {
                        type: 'submit',
                        value: '<spring:message code="views.button.finish" />',
                        class: 'btn btn-default btn-primary btn-xs'
                    })
            );
            form.append(formConfirm);
            $scope.dialogue(form, '<spring:message code='views.index.action.bookMeetingRoom'/>', calendar);
        }

        // Resource input select
        $scope.resourceIdOptions = {
            escapeMarkup: function (markup) {
                return markup;
            },
            data: [
                <c:forEach items="${meetingRoomResources}" var="meetingRoomResource">
                    <c:choose>
                    <c:when test="${meetingRoomResource.domainName != null}">
                        {id: "${meetingRoomResource.id}", text: "<b>${meetingRoomResource.name}</b><br />(${meetingRoomResource.domainName})"},
                    </c:when>
                    <c:otherwise>
                        {id: "${meetingRoomResource.id}", text: "<b>${meetingRoomResource.name}</b>"},
                    </c:otherwise>
                    </c:choose>
                </c:forEach>
            ]
        };

        // URI keys for public calendar of resources
        $scope.resourceUriKeys = {
            escapeMarkup: function (markup) {
                return markup;
            },
            data: {
                <c:forEach items="${meetingRoomResources}" var="meetingRoomResource">
                <c:if test="${meetingRoomResource.isCalendarPublic}">
                "${meetingRoomResource.id}": "${meetingRoomResource.calendarUriKey}",
                </c:if>
                </c:forEach>
            }
        };

        $scope.reservableResources = {
            escapeMarkup: function (markup) {
                return markup;
            },
            data: {
                <c:forEach items="${meetingRoomResources}" var="meetingRoomResource">
                "${meetingRoomResource.id}": "${meetingRoomResource.isReservable}",
                </c:forEach>
            }
        };

        $scope.reservationsFilter = {
            resourceId: $scope.resourceIdOptions.data[0].id
        };

        $scope.eventsF = function(start, end, timezone, callback) {
            var resourceId = $scope.resourceIdOptions.data[0].id;
            if (typeof $scope.reservationsFilter.resourceId == 'object') {
                resourceId = $scope.reservationsFilter.resourceId.id;
            }
            $.ajax("${meetingRoomReservationsUrl}?interval-from=" + start.format() + "&interval-to=" + end.format() + "&resource-id=" + resourceId, {
                dataType: "json"
            }).done(function (data) {
                var events = [];
                data.items.forEach(function (event) {
                    var descriptionTitle = "<spring:message code="views.room.description"/>";
                    var none = "<spring:message code="views.reservationRequest.description.none"/>";
                    events.push({
                        id: event.id,
                        title: event.description,
                        description: event.description ? event.description : none,
                        bookedBy: event.ownerName,
                        ownersEmail: event.ownerEmail,
                        foreignDomain: event.foreignDomain,
                        start: event.start,
                        end: event.end,
                        isWritable: event.isWritable,
                        requestId: event.requestId,
                        parentRequestId: event.parentRequestId,
                        isPeriodic: event.isPeriodic
                    });
                });
                callback(events);

            }).fail($application.handleAjaxFailure);
        };
        $scope.eventRender = function(event, element, view) {
            var descriptionTitle = "<spring:message code="views.room.description"/>";
            var bookedByTitle = "<spring:message code="views.room.bookedBy"/>";
            var bookedBy = event.bookedBy ? (event.bookedBy + " (<a href=\"mailto:" + event.ownersEmail + "\">" + event.ownersEmail + "</a>)") : event.foreignDomain;

            // Show actions for owned reservations (copied from listAction.tag)
            var actions = "";
            if (event.isWritable) {
                actions = "<span class='btn-group pull-right'>";
                if (!event.isPeriodic) {
                    if (event.end.isAfter(moment())) {
                        actions += "<a href='${singleMeetingRoomModifyUrl}' ><b class='fa fa-pencil' title='<spring:message code="views.list.action.modify.title"/>'></b></a> | ";
                    }
                    actions += "<a href='${meetingRoomDeleteUrl}' ><b class='fa fa-trash-o' title='<spring:message code="views.list.action.delete.title"/>'></b></a>";
                } else {
                    if (event.end.isAfter(moment())) {
                        actions += "<a href='${periodicMeetingRoomModifyUrl}' ><b class='fa fa-pencil' title='<spring:message code="views.list.action.modify.title"/>'></b></a> | ";
                        actions += "<a href='${childReservationDelete}' ><b class='fa fa-trash-o' title='<spring:message code="views.list.action.delete.single.title"/>'></b></a> | ";
                    }
                    actions += "<a href='${meetingRoomDeleteAllUrl}' ><b class='fa fa-trash-o fa-red' title='<spring:message code="views.list.action.delete.all.title"/>'></b></a>";
                }
                actions += "</span>";
            } else {
                // Change collor for not-owned reservations
                if ($scope.highlightOwnedReservations) {
                    element.css('background-color', '#88b5dd');
                }
            }

            if (newReservationRequestId != event.id) {
                element.qtip({
                    content: actions + "<strong>" + descriptionTitle + ":</strong><br /><span>" + event.description + "</span><br />" +
                    "<strong>" + bookedByTitle + ":</strong><br /><span>" + bookedBy + "</span>",
                    position: {
                        my: 'left top',
                        at: 'top right'
                    },
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
            }
        };

        var selectable = ($scope.reservableResources.data[$scope.resourceIdOptions.data[0].id] === 'true');
        $scope.uiConfig = {
            calendar: {
                lang: '${requestContext.locale.language}',
                <%--timezone: '${sessionScope.SHONGO_USER.timeZone}',--%>
                defaultView: 'agendaWeek',
                editable: false,
                nowIndicator: true,
                selectable: selectable,
                lazyFetching: false,
                selectOverlap: false,
                header: {
                    left: 'title',
                    center: '',
                    right: 'prev,today,next agendaDay,agendaWeek,month'
                },
                loading: function (bool) {
                    if (bool) {
                        $('#directives-calendar').fadeTo(100, 0.5);
                        $('#loadingImg').fadeTo(100, 1);
                        $('#loadingImg').show();
                    } else {
                        $('#loadingImg').fadeOut();
                        $('#loadingImg').hide();
                        $('#directives-calendar').fadeTo(100, 1);
                    }
                },

                // Create new reservation requests by select function
                select: function (start, end) {
                    var calendar = uiCalendarConfig.calendars['meetingRoomsReservationsCalendar'];
                    if (end.isBefore(moment())) {
                        calendar.fullCalendar('unselect');
                        alert("<spring:message code="views.index.action.bookMeetingRoom.invalidPastSlot"/>");
                    } else {
                        //rendering the reservation
                        calendar.fullCalendar('renderEvent',
                                {
                                    start: start,
                                    end: end,
                                    color: '#ff4d4d',
                                    id: newReservationRequestId,
                                }
                        );

                        // confirm request
                        $scope.reservationRequestPrompt(start, end, calendar);
                    }
                },
                eventRender: $scope.eventRender
            }
        };

        $scope.eventSources = [$scope.eventsF];

        $scope.$watch('reservationsFilter.resourceId', function (newResourceId, oldResourceId, scope) {
            if ($scope.$parent.$tab.active && typeof newResourceId == "object") {
                // Enable creating reservation request in the calendar, when resource is reservable
                $scope.uiConfig.calendar.selectable = ($scope.reservableResources.data[newResourceId.id] === 'true');

                $scope.forceReloadCalendar();
                if ($scope.resourceUriKeys.data[newResourceId.id]) {
                    // Show export .ics URL for public resource
                    var resourceCalendarUrl = calendarUrlBase + $scope.resourceUriKeys.data[newResourceId.id]
                    $('[qtip-init]').show();
                    $('[qtip-init]').each(function () {
                        $(this).qtip('option', 'content.text', exportCalendarMessage + "<input id='input' onClick='this.setSelectionRange(0, this.value.length)' readonly value='" + resourceCalendarUrl + "' />");
                    });
                }
                else {
                    $('[qtip-init]').hide();
                }
            }
        });
        $scope.forceReloadCalendar = function () {
            var calendar = uiCalendarConfig.calendars['meetingRoomsReservationsCalendar'];

            // Force reload of config is needed for select (creating reservation requests in calendar)
            calendar.fullCalendar($scope.uiConfig.calendar);

            // Render events
            calendar.fullCalendar('refetchEvents');
            calendar.fullCalendar('rerenderEvents');
        }
        $scope.refreshCalendar = function () {
            var calendar = uiCalendarConfig.calendars['meetingRoomsReservationsCalendar'];

            // Render events
            if (calendar) {
                calendar.fullCalendar('refetchEvents');
                calendar.fullCalendar('rerenderEvents');
            }
        }
        $scope.$on("refresh-meetingRoomsReservationsCalendar", function () {
            var calendar = uiCalendarConfig.calendars['meetingRoomsReservationsCalendar'];
            if (calendar) {
                calendar.fullCalendar('render');
            }
            if (!$scope.resourceUriKeys.data[$scope.resourceIdOptions.data[0].id]) {
                $('[qtip-init]').hide();
            }
        });
        $scope.$watch('highlightOwnedReservations', function(newValue, oldValue, scope) {
            if (typeof newValue === "boolean") {
                $scope.refreshCalendar();
            }
        });
    });

    // Init qtip for ics export
    module.directive('qtipInit', function () {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                var resourceCalendarUrl = calendarUrlBase + scope.resourceUriKeys.data[scope.resourceIdOptions.data[0].id]
                $(element).qtip({
                    content: {
                        text: exportCalendarMessage + "<input id='input' onClick='this.setSelectionRange(0, this.value.length)' readonly value='" + resourceCalendarUrl + "' />"
                    },
                    position: {
                        my: 'left top',
                        at: 'top right'
                    },
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
            }
        }
    });
</script>
<div ng-controller="CalendarController">
    <div class="alert alert-warning"><spring:message code="views.index.meetingRooms.description"/></div>

    <button class="pull-right fa fa-refresh btn btn-default" ng-click="refreshCalendar()"></button>
    <div class="btn-group pull-right">
        <form class="form-inline">
            <label for="meetingRoomResourceId"><spring:message code="views.room"/>:</label>
            <input id="meetingRoomResourceId" ng-model="reservationsFilter.resourceId" ui-select2="resourceIdOptions" class="min-input"/>
        </form>

        <input id="highlightOwnedReservations" type="checkbox" ng-model="highlightOwnedReservations"/>
        <label for="highlightOwnedReservations">
            <spring:message code="views.index.meetingRooms.highlightOwnedReservations"/>
        </label>
    </div>

    <div class="top-margin"><span class="fa fa-calendar" qtip-init/> <spring:message
            code="views.index.meetingRooms.calendarExport"/></div>

    <div id="directives-calendar" class="top-margin">
        <div class="alert-success calAlert" ng-show="alertMessage != undefined && alertMessage != ''">
            <h4>{{alertMessage}}</h4>
        </div>
        <div class="spinner centered-in-element" id="loadingImg"></div>
        <div class="top-margin" ng-model="eventSources" calendar="meetingRoomsReservationsCalendar"
             ui-calendar="uiConfig.calendar"></div>
    </div>
</div>
