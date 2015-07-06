<%--
  -- Reservation request form.
  --%>
<%@ tag import="cz.cesnet.shongo.client.web.models.TechnologyModel" %>
<%@ tag import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ tag import="cz.cesnet.shongo.client.web.models.ReservationRequestModel" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="permanentRooms" required="false" type="java.util.Collection<cz.cesnet.shongo.controller.api.ReservationRequestSummary>" %>
<%@attribute name="confirmTitle" required="false" type="java.lang.String" %>
<%@attribute name="cancelUrl" required="false" type="java.lang.String" %>
<%@attribute name="cancelTitle" required="false" type="java.lang.String" %>

<c:set var="administrationMode" value="${sessionScope.SHONGO_USER.administrationMode}"/>
<c:set var="reservationRequestModification" value="${reservationRequest.modification}"/>
<c:set var="tabIndex" value="1"/>
<spring:eval var="weekDays" expression="T(cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot$DayOfWeek).values()"/>

<tag:url var="resourceListUrl" value="<%= ClientWebUrl.RESOURCE_LIST_DATA %>"/>

<script type="text/javascript">
    var module = angular.module('tag:reservationRequestForm', ['ngDateTime', 'ngTooltip']);
    module.controller("ReservationRequestFormController", function($scope, $application) {
        // Get value or default value if null
        $scope.value = function (value, defaultValue) {
            return ((value == null || value == '' || value == 0) ? defaultValue : value);
        };

        // Get dynamic reservation request attributes
        $scope.id = $scope.value('${reservationRequest.id}', null);
        $scope.technology = $scope.value('${reservationRequest.technology}', 'H323_SIP');
        $scope.periodicityType = $scope.value('${reservationRequest.periodicityType}', 'NONE');
        $scope.periodicityCycle = $scope.value('${reservationRequest.periodicityCycle}', 1);
        $scope.monthPeriodicityType = $scope.value('${reservationRequest.monthPeriodicityType}', 'STANDARD');
        $scope.roomRecorded = $scope.value(${reservationRequest.roomRecorded == true}, false);
        <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
            $("#start").timepicker('setTime', $scope.value(new Date('${reservationRequest.requestStart}'), new Date()));
        </c:if>

        // Update end when start is changed
        $("#startDate").change(function () {
            var startPicker = $("#startDate");
            var endPicker = $("#end");
            if ( endPicker.length == 0 ) {
                return;
            }
            var start = moment(startPicker.val());
            var end = moment(endPicker.val());
            if ( end == null || end < start) {
                endPicker.val(start.format("YYYY-MM-DD"));
            }
        });

        $scope.getTimeZone = function() {
            var timeZone = $("#timeZone").val();
            if (timeZone != null && timeZone != "") {
                return timeZone;
            }
            else {
                return null;
            }
        };

        $scope.getStart = function() {
            <c:choose>
                <c:when test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
                    var startTimePicker = $("#start").data("timepicker");
                    var start = $("#startDate").val();
                    var hours = (startTimePicker.meridian == "PM" ? startTimePicker.hour + 12 : startTimePicker.hour);
                    start = moment(start).minutes(startTimePicker.minute).hours(hours);
                </c:when>
                <c:otherwise>
                    var start = $("#startDate").val();
                </c:otherwise>
            </c:choose>
            if (start.isValid()) {
                return start;
            }
            else {
                return null;
            }
        };

        $scope.getEnd = function() {
            var start = $scope.getStart();
            if (start == null) {
                return null;
            }
            var durationCount = parseInt($("#durationCount").val());
            if (isNaN(durationCount)) {
                durationCount = 0;
            }
            var durationType = $("#durationType").val().toLowerCase() + "s";
            var end = start;
            if (durationCount > 0) {
                end = start.clone().add(durationType, durationCount);
            }
            return end;
        };

        $scope.getPeriodicityEnd = function() {
            var periodicityEnd = $("#periodicityEnd").val();
            periodicityEnd = moment(periodicityEnd);
            if (periodicityEnd.isValid()) {
                return periodicityEnd;
            }
            else {
                return null;
            }
        };

        // Periodicity full options init
        <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
            $scope.days = [];
            <c:forEach items="${weekDays}" var="day">
                $scope.days[${day.dayIndex} - 1] = '${day}';
            </c:forEach>

            $scope.getLastDateOfMonth = function(date) {
                if (isNaN(date)) {
                    return null;
                }
                var year = date.getFullYear(), month = date.getMonth();
                return new Date(year, month + 1, 0);
            };

            $scope.getStartDay = function() {
                var start = $scope.getStart();
                if (start == null) {
                    return null;
                }
                var startDayIndex = new Date(start).getDay();
                return $scope.days[startDayIndex];
            };

            $scope.getStartDayOrderNo = function() {
                var start = $scope.getStart();
                if (start == null) {
                    return null;
                }
                var startDate = new Date(start);
                var dayOrderNo = startDate.getDate()/7;
                if (startDate.getDate() % 7 != 0) {
                    dayOrderNo =  Math.floor(dayOrderNo) + 1;
                } else {
                    dayOrderNo = Math.floor(dayOrderNo);
                }
                if (startDate.getDate() + 7 > $scope.getLastDateOfMonth(startDate).getDate()) {
                    dayOrderNo = -1;
                }
                return dayOrderNo;
            };

//            $scope.getDayInMonthDate = function(date, dayOrder, dayInMonth) {
//                if (isNaN(date) || (dayOrder != -1 && (dayOrder < 0 || dayOrder > 4)) || dayInMonth == -1) {
//                    return null;
//                }
//                if (0 < dayOrder < 10) {
//                    date.setDate(1);
//
//                    while (date.getDay() != dayInMonth) {
//                        date.setDate(date.getDate()+1);
//                    }
//                    for (i = 1; i < dayOrder; i++) {
//                        if (date.getDate()+7 < $scope.getLastDateOfMonth(date).getDate() + 1) {
//                            date.setDate(date.getDate() + 7);
//                        }
//                    }
//
//                } else if (dayOrder == -1 && (-1 < dayInMonth < 7)) {
//                    date = $scope.getLastDateOfMonth(date);
//                    while (date.getDay() != dayInMonth) {
//                        date.setDate(date.getDate()-1);
//                    }
//                }
//                else {
//                    return null
//                }
//                return date;
//            };

            // Set init monthly (specific-day) periodicity
            $scope.periodicityDayOrder = $scope.value('${reservationRequest.periodicityDayOrder}', $scope.getStartDayOrderNo());
            $scope.periodicityDayInMonth = $scope.value('${reservationRequest.periodicityDayInMonth}', $scope.getStartDay());
            // Set init weekly periodicity
            var selectedDays = [];
            var noDaySelected = true;
            <c:forEach items="${reservationRequest.periodicDaysInWeek}" var="day">
                selectedDays[${day.ordinal()}] = '${day}';
                if (selectedDays[${day.ordinal()}]) {
                    noDaySelected = false;
                }
            </c:forEach>
            $scope.periodicDayMONDAY = ($.inArray("MONDAY", selectedDays) != -1 ? true : (noDaySelected ? $scope.getStartDay() == "MONDAY" : false));
            $scope.periodicDayTUESDAY = ($.inArray("TUESDAY", selectedDays) != -1 ? true : (noDaySelected ? $scope.getStartDay() == "TUESDAY" : false));
            $scope.periodicDayWEDNESDAY = ($.inArray("WEDNESDAY", selectedDays) != -1 ? true : (noDaySelected ? $scope.getStartDay() == "WEDNESDAY" : false));
            $scope.periodicDayTHURSDAY = ($.inArray("THURSDAY", selectedDays) != -1 ? true : (noDaySelected ? $scope.getStartDay() == "THURSDAY" : false));
            $scope.periodicDayFRIDAY = ($.inArray("FRIDAY", selectedDays) != -1 ? true : (noDaySelected ? $scope.getStartDay() == "FRIDAY" : false));
            $scope.periodicDaySATURDAY = ($.inArray("SATURDAY", selectedDays) != -1 ? true : (noDaySelected ? $scope.getStartDay() == "SATURDAY" : false));
            $scope.periodicDaySUNDAY = ($.inArray("SUNDAY", selectedDays) != -1 ? true : (noDaySelected ? $scope.getStartDay() == "SUNDAY" : false));

            // Update start date when month periodicity has changed
//            $scope.$watchCollection('[periodicityDayOrder, periodicityDayInMonth]', function(newValues) {
//                var startDate = $("#start").data("datetimepicker").getDate();
//                var dateTime = new Date(startDate.getFullYear(),startDate.getMonth(), startDate.getDate(), 0, 0, 0, 0);
//
//                var date = $scope.getDayInMonthDate(new Date(dateTime.valueOf()), newValues[0], $.inArray(newValues[1],$scope.days));
//                if (date < dateTime) {
//                    var nextMonthDate = new Date(dateTime.valueOf()).setMonth(dateTime.getMonth() + 1);
//                    date = $scope.getDayInMonthDate(new Date(nextMonthDate.valueOf()), newValues[0], $.inArray(newValues[1],$scope.days));
//                }
//                var y = date.getFullYear();
//                var m = date.getMonth();
//                var d = date.getDate();
//                var h = startDate.getHours();
//                var min = startDate.getMinutes();
//                $("#start").data("timepicker").setDate(new Date(y, m, d, h, min, 0, 0));
//            });
        </c:if>

    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM_CAPACITY'}">
        // Get permanent rooms
        var permanentRooms = {<c:forEach items="${permanentRooms}" var="permanentRoom" varStatus="status"><spring:eval expression="T(cz.cesnet.shongo.client.web.models.TechnologyModel).find(permanentRoom.specificationTechnologies)" var="technology" />
            "${permanentRoom.id}": {
                id: "${permanentRoom.id}",
                name: "${permanentRoom.roomName} (${technology.title})",
                formattedSlot: "<tag:format value="${permanentRoom.earliestSlot}" style="date"/>",
                slot: "${permanentRoom.earliestSlot}",
                technology: "${technology}",
                reservationId: "${permanentRoom.lastReservationId}"
            }<c:if test="${!status.last}">, </c:if></c:forEach>
        };
        // Add all permanent rooms to the model
        $scope.permanentRooms = {};
        for (var permanentRoom in permanentRooms) {
            $scope.permanentRooms[permanentRoom] = permanentRooms[permanentRoom];
        }
        // Set current permanent rooms
        $scope.permanentRoom = $scope.permanentRooms["${reservationRequest.permanentRoomReservationRequestId}"];
        $scope.updatePermanentRooms = function (performApply) {
            // Determine requested slot
            var requestedStart = $scope.getStart();
            var requestedDuration = parseInt($("#durationCount").val());
            var durationType = $("#durationType").val().toLowerCase() + "s";
            var requestedEnd = (requestedStart != null && requestedDuration > 0) ? requestedStart.clone().add(durationType, requestedDuration) : null;
            var slotBeforeMinutesPicker = $("#slotBeforeMinutes");
            if (requestedStart != null && slotBeforeMinutesPicker != null) {
                requestedStart.add("minutes", -slotBeforeMinutesPicker.val());
            }
            var slotAfterMinutesPicker = $("#slotAfterMinutes");
            if (requestedEnd != null && slotAfterMinutesPicker != null) {
                requestedEnd.add("minutes", slotAfterMinutesPicker.val());
            }
            requestedStart = requestedStart != null ? requestedStart.unix() : null;
            requestedEnd = requestedEnd != null ? requestedEnd.unix() : null;

            // Delete old permanent rooms
            for (var permanentRoom in $scope.permanentRooms) {
                delete $scope.permanentRooms[permanentRoom];
            }

            // Add matching permanent rooms
            for (var permanentRoomId in permanentRooms) {
                var permanentRoom = permanentRooms[permanentRoomId];
                var permanentRoomSlot = permanentRoom.slot.split("/");
                if (requestedStart != null || requestedEnd != null) {
                    var permanentRoomStart = moment(permanentRoomSlot[0]).unix();
                    var permanentRoomEnd = moment(permanentRoomSlot[1]).unix();
                    var realStart = requestedStart;
                    if (requestedStart != null && requestedStart < (Date.now()/1000|0)) {
                        realStart = Date.now()/1000|0;
                    }
                    if ((realStart != null && (realStart < permanentRoomStart || realStart >= permanentRoomEnd)) ||
                            (requestedEnd != null && (requestedEnd <= permanentRoomStart || requestedEnd > permanentRoomEnd))) {
                        // Remove current permanent room
                        if (permanentRoom == $scope.permanentRoom) {
                            $scope.permanentRoom = null;
                        }
                        // Skip this permanent room because it doesn't match the requested slot
                        continue;
                    }
                }
                $scope.permanentRooms[permanentRoomId] = permanentRoom;
            }

            // Update
            if (performApply !== false) {
                $scope.$apply();
            }
        };
        // Watch whether PIN was modified
        $scope.pinModified = false;
        $("#roomPin").change(function () {
            var pin = $("#roomPin").val();
            if (pin != "") {
                $scope.pinModified = true;
            }
            else {
                $scope.pinModified = false;
            }
        });
        // Update permanent rooms model when start or duration changes
        $("#startDate,#start,#durationCount,#slotBeforeMinutes,#slotAfterMinutes").change(function () {
            $scope.updatePermanentRooms();
        });
        // Set proper technology for selected permanent room
        $scope.$watch("permanentRoom", function () {
            if ($scope.permanentRoom != null) {
                var reservationId = $scope.permanentRoom.reservationId;
                if (reservationId != null) {
                    <tag:url var="roomDataUrl" value="<%= ClientWebUrl.ROOM_DATA %>">
                    <tag:param name="objectId" value=":objectId"/>
                    </tag:url>
                    $.ajax("${roomDataUrl}".replace(":objectId", reservationId), {
                        dataType: "json"
                    }).done(function (data) {
                        if (!$scope.pinModified) {
                            $("#roomPin").val(data.pin);
                        }
                    }).fail($application.handleAjaxFailure);
                }

                $scope.technology = $scope.permanentRoom.technology;
            }
            else {
                $scope.technology = null;
            }
        });
        // Initially update permanent rooms
        $scope.updatePermanentRooms(false);
    </c:if>

        <tag:url var="periodicEventsUrl" value="<%= ClientWebUrl.WIZARD_PERIODIC_EVENTS %>"/>
        $scope.formatPeriodicEvents = function(event) {
            var start = $scope.getStart();
            if (start == null) {
                return "Vyplnte datum.";
            }
            var periodicityEnd = $scope.getPeriodicityEnd();
            var timeZone = $scope.getTimeZone();
            var request = {
                timeZone: timeZone,
                maxCount: 10,
                start: start.format('YYYY-MM-DDTHH:mm'),
                periodicityType: $scope.periodicityType,
                periodicityEnd: (periodicityEnd != null ? periodicityEnd.format('YYYY-MM-DD') : null)
            };
            $.ajax("${periodicEventsUrl}", {
                type: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                },
                dataType: 'json',
                data: JSON.stringify(request)
            }).done(function (data) {
                var content = "<spring:message code="views.reservationRequest.periodicity.events"/>:";
                for ( var index = 0; index < data.length; index++) {
                    var dateTime = data[index];
                    content += "<br/>";
                    if (dateTime == null) {
                        content += "<spring:message code="views.reservationRequest.periodicity.events.more"/>...";
                        break;
                    }
                    var periodicEvent = moment.parseZone(dateTime).lang("${requestContext.locale.language}");
                    content += "<strong>" + (index + 1) + ")</strong> ";
                    content += periodicEvent.format("dddd ");
                    content += "<strong>" + periodicEvent.format("LLL") + "</strong>";
                    content += " (" + periodicEvent.format("Z") + ")";
                }
                if (data.length == 0) {
                    content += "<br><spring:message code="views.list.none"/>";
                }
                event.setResult(content, false);
            }).fail($application.handleAjaxFailure);
            return "<spring:message code="views.loading"/>";
        };
    });

    /**
     * Get list of resources.
     *
     * @param capabilityClass
     * @param callback
     */
    window.getResources = function(capabilityClass, callback) {
        var technology = $("#technology").val();
        $.ajax("${resourceListUrl}?capabilityClass=" + capabilityClass + "&technology=" + technology, {
            dataType: "json"
        }).done(function (data) {
            var resources = [{id: "", text: "<spring:message code="views.reservationRequest.specification.resourceId.none"/>"}];
            for (var index = 0; index < data.length; index++) {
                var resource = data[index];
                resources.push({
                    id: resource.id,
                    text: "<strong>" + resource.name + "</strong> (" + resource.id + ")"
                });
            }
            callback(resources);
        })
    };

    /**
     * Get list of resources by assigned tag.
     *
     * @param capabilityClass
     * @param callback
     */
    window.getResourcesByTag = function(tagName, callback) {
        $.ajax("${resourceListUrl}?&tag=" + tagName, {
            dataType: "json"
        }).done(function (data) {
            var resources = [{id: "", text: "<spring:message code="views.reservationRequest.specification.MEETING_ROOM.choose"/>"}];

            for (var index = 0; index < data.length; index++) {
                var resource = data[index];
                var description = "";
                if (resource.description) {
                    description = " (" + resource.description + ")";
                }
                var domain = "";
                if (resource.domainName) {
                    domain = " (" + resource.domainName + ")";
                }
                resources.push({
                    id: resource.id,
                    text: "<strong>" + resource.name + "</strong>" + description + domain
                });
            }
            callback(resources);
        })
    };

    $(function(){
        $("#timeZone").select2();
    });
</script>

<form:form class="form-horizontal"
           commandName="reservationRequest"
           method="post"
           ng-controller="ReservationRequestFormController">

    <c:if test="${not empty reservationRequest.id}">
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="id">
                <spring:message code="views.reservationRequest.identifier"/>:
            </form:label>
            <div class="col-xs-4">
                <form:input cssClass="form-control" path="id" readonly="true" tabindex="${tabIndex}"/>
            </div>
        </div>
    </c:if>

    <c:choose>
        <c:when test="${reservationRequest.specificationType != 'MEETING_ROOM' && reservationRequest.specificationType != 'PERMANENT_ROOM_CAPACITY'}">
            <div class="form-group">
                <form:label class="col-xs-3 control-label" path="technology">
                    <spring:message code="views.reservationRequest.technology"/>:
                </form:label>
                <div class="col-xs-4">
                    <form:select cssClass="form-control" path="technology" ng-model="technology" tabindex="${tabIndex}">
                        <spring:eval var="technologies" expression="T(cz.cesnet.shongo.client.web.models.TechnologyModel).values()"/>
                        <c:forEach var="technology" items="${technologies}">
                            <form:option value="${technology}">${technology.title}</form:option>
                        </c:forEach>
                    </form:select>
                    <form:errors path="technology" cssClass="error"/>
                </div>
            </div>
        </c:when>
        <c:when test="${reservationRequest.specificationType != 'MEETING_ROOM'}">
            <input type="hidden" name="technology" value="{{technology}}"/>
        </c:when>
    </c:choose>

    <c:if test="${administrationMode && reservationRequest.specificationType != 'PERMANENT_ROOM_CAPACITY' && reservationRequest.specificationType != 'MEETING_ROOM'}">
        <script type="text/javascript">
            $(function(){
                var updateResources = function() {
                    window.getResources("RoomProviderCapability", function(resources) {
                        $("#roomResourceId").select2({
                            data: resources,
                            escapeMarkup: function (markup) {
                                return markup;
                            },
                            initSelection: function(element, callback) {
                                var id = $(element).val();
                                for (var index = 0; index < resources.length; index++) {
                                    if (resources[index].id == id) {
                                        callback(resources[index]);
                                        return;
                                    }
                                }
                                // Id wasn't found and thus set default value
                                callback(resources[0]);
                                $("#roomResourceId").val(resources[0].id);
                            }
                        });
                    });
                };
                $("#technology").change(updateResources);
                updateResources();
            });
        </script>
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="roomResourceId">
                <spring:message code="views.reservationRequest.specification.resourceId"/>:
            </form:label>
            <div class="col-xs-4">
                <form:input cssClass="form-control" cssErrorClass="form-control error" path="roomResourceId" tabindex="${tabIndex}"/>
            </div>
            <div class="col-xs-offset-3 col-xs-9">
                <form:errors path="roomResourceId" cssClass="error"/>
            </div>
        </div>

        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="purpose">
                <spring:message code="views.reservationRequest.purpose"/>:
            </form:label>
            <div class="col-xs-4">
                <form:select cssClass="form-control" path="purpose" tabindex="${tabIndex}">
                    <spring:eval var="purposes" expression="T(cz.cesnet.shongo.controller.ReservationRequestPurpose).values()"/>
                    <c:forEach var="purpose" items="${purposes}">
                        <form:option value="${purpose}">${purpose}</form:option>
                    </c:forEach>
                </form:select>
            </div>
            <div class="col-xs-offset-3 col-xs-9">
                <form:errors path="purpose" cssClass="error"/>
            </div>
        </div>
    </c:if>

    <c:if test="${reservationRequest.specificationType == 'MEETING_ROOM'}">
        <script type="text/javascript">
            $(function(){
                var updateResources = function() {
                    window.getResourcesByTag("${configuration.getMeetingRoomTagName()}", function(resources) {
                        $("#meetingRoomResourceId").select2({
                            data: resources,
                            escapeMarkup: function (markup) {
                                return markup;
                            },
                            initSelection: function(element, callback) {
                                var id = $(element).val();
                                for (var index = 0; index < resources.length; index++) {
                                    if (resources[index].id == id) {
                                        callback(resources[index]);
                                        return;
                                    }
                                }
                                // Id wasn't found and thus set default value
                                callback(resources[0]);
                                $("#meetingRoomResourceId").val(resources[0].id);
                            }
                        });
                    });
                };
                updateResources();
            });
        </script>
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="meetingRoomResourceId">
                <spring:message code="views.reservationRequest.specification.MEETING_ROOM"/>:
            </form:label>
            <div class="col-xs-4">
                <form:input cssClass="form-control" cssErrorClass="form-control error" path="meetingRoomResourceId" tabindex="${tabIndex}"/>
            </div>
            <div class="col-xs-offset-3 col-xs-9">
                <form:errors path="meetingRoomResourceId" cssClass="error"/>
            </div>
        </div>
    </c:if>

    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM_CAPACITY'}">
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="permanentRoomReservationRequestId">
                <spring:message code="views.reservationRequest.specification.permanentRoomReservationRequestId"/>:
            </form:label>
            <div class="col-xs-4">
                <form:select cssClass="form-control" cssErrorClass="form-control error"
                             path="permanentRoomReservationRequestId"
                             ng-model="permanentRoom"
                             ng-options="option.name for (value, option) in permanentRooms" tabindex="${tabIndex}">
                    <form:option value="">-- <spring:message code="views.select.choose"/> --</form:option>
                    {{option}}
                </form:select>
                <form:errors path="permanentRoomReservationRequestId" cssClass="error"/>
                <div ng-show="permanentRoom" class="description">
                    <b><spring:message code="views.reservationRequest.validity"/>:</b>
                    {{permanentRoom.formattedSlot}}
                </div>
            </div>
        </div>
    </c:if>

    <div class="form-group">
        <form:label class="col-xs-3 control-label" path="description">
            <spring:message code="views.reservationRequest.description" var="descriptionLabel"/>
            <tag:help label="${descriptionLabel}:"><spring:message code="views.reservationRequest.descriptionHelp"/></tag:help>
        </form:label>
        <div class="col-xs-4">
            <form:input path="description" cssClass="form-control" cssErrorClass="form-control error" tabindex="${tabIndex}"/>
        </div>
        <div class="col-xs-offset-3 col-xs-9">
            <form:errors path="description" cssClass="error"/>
        </div>
    </div>

    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="roomName">
                <spring:message code="views.reservationRequest.specification.roomName"/>:
            </form:label>
            <div class="col-xs-4">
                <form:input path="roomName" cssClass="form-control" cssErrorClass="form-control error" tabindex="${tabIndex}"/>
            </div>
            <div class="col-xs-offset-3 col-xs-9">
                <form:errors path="roomName" cssClass="error"/>
            </div>
        </div>
    </c:if>

    <c:if test="${reservationRequestModification != null && reservationRequest.specificationType == 'ADHOC_ROOM' && not empty reservationRequest.roomName}">
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="roomName">
                <spring:message code="views.reservationRequest.specification.roomName" var="roomNameLabel"/>
                <tag:help label="${roomNameLabel}:">
                    <spring:message code="views.reservationRequest.specification.roomName.retainHelp"/>
                </tag:help>
            </form:label>
            <div class="col-xs-3">
                <label class="radio-inline" for="room-name-new">
                    <form:radiobutton id="room-name-new" path="adhocRoomRetainRoomName" value="false" tabindex="${tabIndex}"/>
                    <span><spring:message code="views.reservationRequest.specification.roomName.new"/></span>
                </label>
                <label class="radio-inline" for="room-name-retain">
                    <form:radiobutton id="room-name-retain" path="adhocRoomRetainRoomName" value="true" tabindex="${tabIndex}"/>
                    <span><spring:message code="views.reservationRequest.specification.roomName.retain"/></span>
                </label>
            </div>
            <div class="col-xs-3">
                <form:input cssClass="form-control" path="roomName" tabindex="${tabIndex}" readonly="true"/>
            </div>
        </div>
    </c:if>

    <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
        <c:if test="${reservationRequest.specificationType != 'MEETING_ROOM'}">
            <div class="form-group">
                <form:label class="col-xs-3 control-label" path="roomParticipantCount">
                    <spring:message code="views.reservationRequest.specification.roomParticipantCount"/>:
                </form:label>
                <div class="col-xs-9 space-padding">
                    <div class="col-xs-2">
                        <form:input path="roomParticipantCount" cssClass="form-control" cssErrorClass="form-control error" tabindex="${tabIndex}"/>
                    </div>
                </div>
                <div class="col-xs-offset-3 col-xs-9">
                    <form:errors path="roomParticipantCount" cssClass="error"/>
                </div>
            </div>
        </c:if>

        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="timeZone">
                <spring:message code="views.reservationRequest.timeZone" var="timeZoneLabel"/>
                <tag:help label="${timeZoneLabel}:">
                    <spring:message code="views.reservationRequest.timeZone.help"/>
                </tag:help>
            </form:label>
            <div class="col-xs-5">
                <c:set var="timeZone" value="${sessionScope.SHONGO_USER.timeZone}"/>
                <c:set var="locale" value="${sessionScope.SHONGO_USER.locale}"/>
                <spring:eval expression="T(cz.cesnet.shongo.client.web.models.TimeZoneModel).getTimeZones(locale)" var="timeZones"/>
                <form:select cssClass="form-control" path="timeZone" tabindex="${tabIndex}">
                    <form:option value="">
                        <spring:message code="views.reservationRequest.timeZone.default"/>
                        <spring:eval expression="T(cz.cesnet.shongo.client.web.models.TimeZoneModel).formatTimeZoneName(timeZone, locale)" var="timeZoneName"/>
                        <c:if test="${not empty timeZoneName}">
                            - ${timeZoneName}
                        </c:if>
                        (<spring:eval expression="T(cz.cesnet.shongo.client.web.models.TimeZoneModel).formatTimeZone(timeZone)"/>)
                    </form:option>
                    <c:forEach items="${timeZones}" var="timeZone">
                        <form:option value="${timeZone.key}">${timeZone.value}</form:option>
                    </c:forEach>
                </form:select>
            </div>
        </div>
    </c:if>

    <div class="form-group">
        <label class="col-xs-3 control-label" path="start">
            <spring:message code="views.reservationRequest.start"/>:
        </label>
        <div class="col-xs-9 space-padding">
            <div class="col-xs-2">
                <form:input cssClass="form-control" cssErrorClass="form-control error" path="startDate" date-picker="true" tabindex="${tabIndex}"/>
            </div>
            <div class="col-xs-2">
                <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
                    <form:input cssClass="form-control" cssErrorClass="form-control error" path="start" time-picker="true" data-show-inputs="false" data-minute-step="5" data-second-step="60" tabindex="${tabIndex}"/>
                </c:if>
            </div>
            <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM' && reservationRequest.specificationType != 'MEETING_ROOM'}">
                <div class="col-xs-4">
                    <div class="input-group" style="width: 100%;">
                        <form:select path="slotBeforeMinutes" cssClass="form-control" tabindex="${tabIndex}">
                            <form:option value="0"><spring:message code="views.reservationRequest.slotMinutesNone"/></form:option>
                            <form:option value="5">5</form:option>
                            <form:option value="10">10</form:option>
                            <form:option value="15">15</form:option>
                            <form:option value="20">20</form:option>
                            <form:option value="30">30</form:option>
                            <form:option value="45">45</form:option>
                        </form:select>
                        <span class="input-group-addon" style="width: 120px;">
                            <spring:message code="views.reservationRequest.slotBeforeMinutes" var="slotBeforeMinutesLabel"/>
                            <tag:help label="${slotBeforeMinutesLabel}"><spring:message code="views.reservationRequest.slotBeforeMinutes.help"/></tag:help>
                        </span>
                    </div>
                </div>
            </c:if>
        </div>
        <div class="col-xs-offset-3 col-xs-9">
            <form:errors path="startDate" cssClass="error"/>
            <form:errors path="start" cssClass="error"/>
            <form:errors path="slotBeforeMinutes" cssClass="error"/>
        </div>
    </div>

    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="end">
                <spring:message code="views.reservationRequest.end"/>:
            </form:label>
            <div class="col-xs-9 space-padding">
                    <div class="col-xs-2">
                        <form:input cssClass="form-control" cssErrorClass="form-control error" path="end" date-time-picker="true" format="date" tabindex="${tabIndex}"/>
                    </div>
                </div>
            <div class="col-xs-offset-3 col-xs-9">
                <form:errors path="end" cssClass="error"/>
            </div>
        </div>
    </c:if>

    <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="durationCount">
                <spring:message code="views.reservationRequest.duration"/>:
            </form:label>
            <div class="col-xs-9 space-padding">
                <div class="col-xs-2">
                    <input type="hidden" name="end" value=""/>
                    <form:input cssClass="form-control" cssErrorClass="form-control error" path="durationCount" tabindex="${tabIndex}"/>
                </div>
                <div class="col-xs-2">
                    <form:select path="durationType" cssClass="form-control" tabindex="${tabIndex}">
                        <form:option value="MINUTE"><spring:message code="views.reservationRequest.duration.minutes"/></form:option>
                        <form:option value="HOUR"><spring:message code="views.reservationRequest.duration.hours"/></form:option>
                        <form:option value="DAY"><spring:message code="views.reservationRequest.duration.days"/></form:option>
                    </form:select>
                </div>
                <c:if test="${reservationRequest.specificationType != 'MEETING_ROOM'}">
                    <div class="col-xs-4">
                        <div class="input-group" style="width: 100%;">
                            <form:select cssClass="form-control" path="slotAfterMinutes" tabindex="${tabIndex}">
                                <form:option value="0"><spring:message code="views.reservationRequest.slotMinutesNone"/></form:option>
                                <form:option value="5">5</form:option>
                                <form:option value="10">10</form:option>
                                <form:option value="15">15</form:option>
                                <form:option value="20">20</form:option>
                                <form:option value="30">30</form:option>
                                <form:option value="45">45</form:option>
                            </form:select>
                            <span class="input-group-addon" style="width: 120px;">
                                <spring:message code="views.reservationRequest.slotAfterMinutes" var="slotAfterMinutesLabel"/>
                                <tag:help label="${slotAfterMinutesLabel}"><spring:message code="views.reservationRequest.slotAfterMinutes.help"/></tag:help>
                            </span>
                        </div>
                    </div>
                </c:if>
            </div>
            <div class="col-xs-offset-3 col-xs-9">
                <form:errors path="durationCount" cssClass="error"/>
                <form:errors path="slotAfterMinutes" cssClass="error"/>
            </div>
        </div>

        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="periodicityType">
                <spring:message code="views.reservationRequest.periodicity"/>:
            </form:label>
            <div class="col-xs-9 row">
                <div class="col-xs-1">
                    <div class="radio">
                        <label for="periodicity-none">
                            <form:radiobutton id="periodicity-none" path="periodicityType" value="NONE" tabindex="${tabIndex}" ng-model="periodicityType"/>
                            <spring:message code="views.reservationRequest.periodicity.NONE"/>
                        </label>
                    </div>
                    <div class="radio">
                        <label for="periodicity-daily">
                            <form:radiobutton id="periodicity-daily" path="periodicityType" value="DAILY" tabindex="${tabIndex}" ng-model="periodicityType"/>
                            <spring:message code="views.reservationRequest.periodicity.DAILY"/>
                        </label>
                    </div>
                    <div class="radio">
                        <label for="periodicity-weekly">
                            <form:radiobutton id="periodicity-weekly" path="periodicityType" value="WEEKLY" tabindex="${tabIndex}" ng-model="periodicityType"/>
                            <spring:message code="views.reservationRequest.periodicity.WEEKLY"/>
                        </label>
                    </div>
                    <div class="radio">
                        <label for="periodicity-monthly">
                            <form:radiobutton id="periodicity-monthly" path="periodicityType" value="MONTHLY" tabindex="${tabIndex}" ng-model="periodicityType"/>
                            <span><spring:message code="views.reservationRequest.periodicity.MONTHLY"/></span>
                        </label>
                    </div>
                </div>

                <div class="vdivider" ></div>

                <div class="col-xs-7" ng-show="periodicityType == 'WEEKLY'">
                    <div class="row">
                        <span><spring:message code="views.reservationRequest.periodicity.recureEvery"/></span>
                        <form:select path="periodicityCycle" cssErrorClass="error" size="1" tabindex="${tabIndex}" ng-disabled="periodicityType != 'WEEKLY'">
                            <form:option value="1" />
                            <form:option value="2" />
                            <form:option value="3" />
                            <form:option value="4" />
                            <form:option value="5" />
                        </form:select>
                        <span>. <spring:message code="views.reservationRequest.periodicity.recureEvery.weeks"/>:</span>
                    </div>
                    <div class="row">
                        <c:forEach items="${weekDays}" var="day">
                            <span class="checkbox col-xs-3">
                                <label for="periodic-day-${day}">
                                    <form:checkbox cssErrorClass="error" path="periodicDaysInWeek" id="periodic-day-${day}" ng-model="periodicDay${day}" tabindex="${tabIndex}" value="${day}"/>
                                    <spring:message code="views.reservationRequest.periodicity.day.${day}"/>
                                </label>
                            </span>
                        </c:forEach>
                    </div>
                    <form:errors path="periodicDaysInWeek" cssClass="error row"/>
                </div>

                <div class="col-xs-7" ng-show="periodicityType == 'MONTHLY'">
                    <div class="radio">
                        <label for="month-periodicity-standard">
                            <form:radiobutton id="month-periodicity-standard" cssErrorClass="form-control error" path="monthPeriodicityType" value="STANDARD" tabindex="${tabIndex}" ng-model="monthPeriodicityType" />
                            <span><spring:message code="views.reservationRequest.periodicity.recureEvery"/></span>
                        </label>
                        <form:select path="periodicityCycle" cssErrorClass="error" size="1" tabindex="${tabIndex}" ng-disabled="monthPeriodicityType != 'STANDARD'">
                            <form:option value="1" />
                            <form:option value="2" />
                            <form:option value="3" />
                            <form:option value="4" />
                            <form:option value="5" />
                        </form:select>
                        <span>. <spring:message code="views.reservationRequest.periodicity.recureEvery.months"/>.</span>
                    </div>
                    <div class="radio form-inline">
                        <label for="month-periodicity-specific-day">
                            <form:radiobutton id="month-periodicity-specific-day" path="monthPeriodicityType" value="SPECIFIC_DAY" tabindex="${tabIndex}" ng-model="monthPeriodicityType"/>
                            <span><spring:message code="views.reservationRequest.periodicity.recureEvery"/></span>
                        </label>
                        <form:select id="periodicityDayOrder" path="periodicityDayOrder" cssErrorClass="form-control error" size="1" ng-model="periodicityDayOrder" tabindex="${tabIndex}" ng-disabled="monthPeriodicityType != 'SPECIFIC_DAY'" >
                            <form:option value="1">1.</form:option>
                            <form:option value="2">2.</form:option>
                            <form:option value="3">3.</form:option>
                            <form:option value="4">4.</form:option>
                            <form:option value="-1"><spring:message code="views.reservationRequest.periodicity.recureEvery.last"/></form:option>
                        </form:select>
                        <form:select id="periodicityDayInMonth" path="periodicityDayInMonth" cssErrorClass="error" ng-model="periodicityDayInMonth" tabindex="${tabIndex}" ng-disabled="monthPeriodicityType != 'SPECIFIC_DAY'" >
                            <c:forEach items="${weekDays}" var="day">
                                <form:option cssErrorClass="error" value="${day}"><spring:message code="views.reservationRequest.periodicity.day.${day}" /></form:option>
                            </c:forEach>
                        </form:select>
                        <span><spring:message code="views.reservationRequest.periodicity.recureEvery.inEvery"/></span>
                        <form:select path="periodicityCycle" cssErrorClass="error" size="1" tabindex="${tabIndex}" ng-disabled="monthPeriodicityType != 'SPECIFIC_DAY'">
                            <form:option value="1" />
                            <form:option value="2" />
                            <form:option value="3" />
                            <form:option value="4" />
                            <form:option value="5" />
                        </form:select>
                        <span>. <spring:message code="views.reservationRequest.periodicity.recureEvery.months"/>.</span>
                    </div>
                </div>

                <%--<div class="col-xs-3">--%>
                    <%--<div class="top-margin" ng-show="periodicityType != 'NONE'">--%>
                        <%--<span class="input-group">--%>
                            <%--<span class="input-group-addon">--%>
                                <%--<spring:message code="views.reservationRequest.periodicity.until"/>--%>
                            <%--</span>--%>
                            <%--<form:input cssClass="form-control" cssErrorClass="form-control error" path="periodicityEnd" date-picker="true" tabindex="${tabIndex}" ng-disabled="periodicityType == 'NONE'"/>--%>
                        <%--</span>--%>
                    <%--</div>--%>
                    <%--<div ng-show="periodicityType != 'NONE'">--%>
                        <%--<label class="control-label">--%>
                            <%--<c:set var="periodicEvents"><b class='fa fa-search'></b>&nbsp;<spring:message code="views.reservationRequest.periodicity.showEvents"/></c:set>--%>
                            <%--<tag:help label="${periodicEvents}" content="formatPeriodicEvents(event)" selectable="true" position="bottom-left"/>--%>
                        <%--</label>--%>
                    <%--</div>--%>
                    <%--<div class="col-xs-12" >--%>
                        <%--<form:errors path="periodicityEnd" cssClass="error"/>--%>
                    <%--</div>--%>
                <%--</div>--%>

            </div>
        </div>

         <div class="form-group">
            <label class="col-xs-3 control-label" path="periodicityEnd">
                <spring:message code="views.reservationRequest.periodicity.end"/>:
            </label>
            <div class="col-xs-9 space-padding">
                <div class="col-xs-2 bg-danger">
                        <form:input cssClass="form-control" cssErrorClass="form-control error" path="periodicityEnd" date-picker="true" tabindex="${tabIndex}" ng-disabled="periodicityType == 'NONE'"/>
                </div>
            </div>
            <div class="col-xs-offset-3 col-xs-9">
                <form:errors path="periodicityEnd" cssClass="error"/>
            </div>
        </div>
    </c:if>

    <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
        <div class="form-group" ng-show="technology == 'ADOBE_CONNECT'" class="hide">
            <form:label class="col-xs-3 control-label" path="roomAccessMode">
                <spring:message code="views.reservationRequest.specification.roomAccessMode" var="roomAccessModeLabel"/>
                <tag:help label="${roomAccessModeLabel}:">
                    <spring:message code="views.reservationRequest.specification.roomAccessMode.help"/>
                </tag:help>
            </form:label>
            <div class="col-xs-4">
                <spring:eval var="enumAdobeConnectAccessMode" expression="T(cz.cesnet.shongo.api.AdobeConnectPermissions).values()"/>
                <c:forEach var="accessMode" items="${enumAdobeConnectAccessMode}">
                    <c:choose>
                        <c:when test="${accessMode == 'PROTECTED' && reservationRequest.roomAccessMode == null}">
                            <label class="radio-inline" for="${accessMode}">
                            <form:radiobutton id="${accessMode}" path="roomAccessMode" value="${accessMode}" tabindex="${tabIndex}" checked="checked"/>
                            <spring:message code="views.reservationRequest.specification.roomAccessMode.${accessMode}"/>
                        </c:when>
                        <c:when test="${accessMode.isUsableByMeetings() == 'true'}">
                            <label class="radio-inline" for="${accessMode}">
                            <form:radiobutton id="${accessMode}" path="roomAccessMode" value="${accessMode}" tabindex="${tabIndex}"/>
                            <spring:message code="views.reservationRequest.specification.roomAccessMode.${accessMode}"/>
                        </c:when>
                    </c:choose>
                    </label>
                </c:forEach>
                <form:errors path="roomAccessMode" cssClass="error"/>
            </div>
        </div>
    </c:if>

    <c:if test="${reservationRequest.specificationType != 'MEETING_ROOM'}">
        <div class="form-group" ng-show="technology == 'H323_SIP' || technology == 'ADOBE_CONNECT'" class="hide">
            <form:label class="col-xs-3 control-label" path="roomPin">
                <spring:message code="views.reservationRequest.specification.roomPin" var="pinLabel"/>
                <tag:help label="${pinLabel}:">
                    <spring:message code="views.reservationRequest.specification.roomPin.help"/>
                </tag:help>
            </form:label>
            <div class="col-xs-9 space-padding">
                <div class="col-xs-2">
                    <form:input cssClass="form-control col-xs-3" cssErrorClass="form-control error" path="roomPin"  tabindex="${tabIndex}"/>
                </div>
            </div>
            <div class="col-xs-offset-3 col-xs-9">
                <form:errors path="roomPin" cssClass="error"/>
            </div>
        </div>
    </c:if>

    <%-- TODO: Check if resource has recording capability --%>
    <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM' && reservationRequest.specificationType != 'MEETING_ROOM'}">
        <div class="form-group" ng-hide="technology == 'ADOBE_CONNECT'">
            <form:label class="col-xs-3 control-label" path="roomRecorded">
                <spring:message code="views.reservationRequest.specification.roomRecorded" var="roomRecordedLabel"/>
                <tag:help label="${roomRecordedLabel}:"><spring:message code="views.reservationRequest.specification.roomRecordedHelp"/></tag:help>
            </form:label>
            <div class="col-xs-4">
                <div class="checkbox">
                    <form:checkbox cssErrorClass="error" path="roomRecorded" tabindex="${tabIndex}" ng-model="roomRecorded"/>
                </div>
                <form:errors path="roomRecorded" cssClass="error"/>
            </div>
        </div>
        <c:if test="${administrationMode}">
            <script type="text/javascript">
                $(function(){
                    var updateResources = function() {
                        window.getResources("RecordingCapability", function(resources) {
                            $("#roomRecordingResourceId").select2({
                                data: resources,
                                escapeMarkup: function (markup) {
                                    return markup;
                                },
                                initSelection: function(element, callback) {
                                    var id = $(element).val();
                                    for (var index = 0; index < resources.length; index++) {
                                        if (resources[index].id == id) {
                                            callback(resources[index]);
                                            return;
                                        }
                                    }
                                    // Id wasn't found and thus set default value
                                    callback(resources[0]);
                                    $("#roomRecordingResourceId").val(resources[0].id);
                                }
                            });
                        });
                    };
                    $("#technology").change(updateResources);
                    updateResources();
                });
            </script>
            <div class="form-group" ng-show="roomRecorded && technology != 'ADOBE_CONNECT'">
                <form:label class="col-xs-3 control-label" path="roomRecordingResourceId">
                    <spring:message code="views.reservationRequest.specification.roomRecordingResourceId"/>:
                </form:label>
                <div class="col-xs-4">
                    <form:input cssClass="form-control" cssErrorClass="form-control error" path="roomRecordingResourceId" tabindex="${tabIndex}"/>
                </div>
                <div class="col-xs-offset-3 col-xs-9">
                    <form:errors path="roomRecordingResourceId" cssClass="error"/>
                </div>
            </div>
        </c:if>
    </c:if>

    <jsp:doBody var="content"/>

</form:form>
