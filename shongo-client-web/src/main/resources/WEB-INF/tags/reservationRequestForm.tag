<%--
  -- Reservation request form.
  --%>
<%@ tag import="cz.cesnet.shongo.client.web.models.TechnologyModel" %>
<%@ tag import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

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

<tag:url var="resourceListUrl" value="<%= ClientWebUrl.RESOURCE_LIST_DATA %>"/>

<script type="text/javascript">
    var module = angular.module('tag:reservationRequestForm', ['ngDateTime', 'ngTooltip']);
    module.controller("ReservationRequestFormController", function($scope, $application) {
        // Get value or default value if null
        $scope.value = function (value, defaultValue) {
            return ((value == null || value == '') ? defaultValue : value);
        };

        // Get dynamic reservation request attributes
        $scope.id = $scope.value('${reservationRequest.id}', null);
        $scope.technology = $scope.value('${reservationRequest.technology}', 'H323_SIP');
        $scope.periodicityType = $scope.value('${reservationRequest.periodicityType}', 'NONE');
        $scope.roomRecorded = $scope.value(${reservationRequest.roomRecorded == true}, false);

        // Update end when start is changed
        $("#start").change(function () {
            var startPicker = $("#start");
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
            var start = $("#start").val();
            start = moment(start);
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
                    if ((requestedStart != null && (requestedStart < permanentRoomStart || requestedStart >= permanentRoomEnd)) ||
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
        $("#start,#durationCount,#slotBeforeMinutes,#slotAfterMinutes").change(function () {
            $scope.updatePermanentRooms();
        });
        // Set proper technology for selected permanent room
        $scope.$watch("permanentRoom", function () {
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

            if ($scope.permanentRoom != null) {
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

    <c:if test="${administrationMode && reservationRequest.specificationType != 'PERMANENT_ROOM_CAPACITY'}">
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
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="roomParticipantCount">
                <spring:message code="views.reservationRequest.specification.roomParticipantCount"/>:
            </form:label>
            <div class="col-xs-2">
                <form:input path="roomParticipantCount" cssClass="form-control" cssErrorClass="form-control error" tabindex="${tabIndex}"/>
            </div>
            <div class="col-xs-offset-3 col-xs-9">
                <form:errors path="roomParticipantCount" cssClass="error"/>
            </div>
        </div>

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
        <form:label class="col-xs-3 control-label" path="start">
            <spring:message code="views.reservationRequest.start"/>:
        </form:label>
        <div class="col-xs-9 space-padding">
            <div class="col-xs-4">
                <c:choose>
                    <c:when test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
                        <form:input cssClass="form-control" cssErrorClass="form-control error" path="start" date-picker="true" tabindex="${tabIndex}"/>
                    </c:when>
                    <c:otherwise>
                        <form:input cssClass="form-control" cssErrorClass="form-control error" path="start" date-time-picker="true" tabindex="${tabIndex}"/>
                    </c:otherwise>
                </c:choose>
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
            <form:errors path="start" cssClass="error"/>
            <form:errors path="slotBeforeMinutes" cssClass="error"/>
        </div>
    </div>

    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="end">
                <spring:message code="views.reservationRequest.end"/>:
            </form:label>
            <div class="col-xs-4">
                <form:input cssClass="form-control" cssErrorClass="form-control error" path="end" date-time-picker="true" format="date" tabindex="${tabIndex}"/>
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
            <div class="col-xs-9 space-padding">
                <div class="col-xs-4">
                    <label class="radio-inline" for="periodicity-none">
                        <form:radiobutton id="periodicity-none" path="periodicityType" value="NONE" tabindex="${tabIndex}" ng-model="periodicityType"/>
                        <spring:message code="views.reservationRequest.periodicity.NONE"/>
                    </label>
                    <label class="radio-inline" for="periodicity-daily">
                        <form:radiobutton id="periodicity-daily" path="periodicityType" value="DAILY" tabindex="${tabIndex}" ng-model="periodicityType"/>
                        <spring:message code="views.reservationRequest.periodicity.DAILY"/>
                    </label>
                    <label class="radio-inline" for="periodicity-weekly">
                        <form:radiobutton id="periodicity-weekly" path="periodicityType" value="WEEKLY" tabindex="${tabIndex}" ng-model="periodicityType"/>
                        <spring:message code="views.reservationRequest.periodicity.WEEKLY"/>
                    </label>
                </div>
                <div class="col-xs-8 space-padding">
                    <div class="col-xs-6">
                        <span class="input-group">
                            <span class="input-group-addon">
                                <spring:message code="views.reservationRequest.periodicity.until"/>
                            </span>
                            <form:input cssClass="form-control" cssErrorClass="form-control error" path="periodicityEnd" date-picker="true" tabindex="${tabIndex}" ng-disabled="periodicityType == 'NONE'"/>
                        </span>
                    </div>
                    <div class="col-xs-6" ng-show="periodicityType != 'NONE'">
                        <label class="control-label">
                            <c:set var="periodicEvents"><b class='fa fa-search'></b>&nbsp;<spring:message code="views.reservationRequest.periodicity.showEvents"/></c:set>
                            <tag:help label="${periodicEvents}" content="formatPeriodicEvents(event)" selectable="true" position="bottom-left"/>
                        </label>
                    </div>
                    <div class="col-xs-12" >
                        <form:errors path="periodicityEnd" cssClass="error"/>
                    </div>
                </div>
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
            <div class="col-xs-4">
                <form:input cssClass="form-control" cssErrorClass="form-control error" path="roomPin"  tabindex="${tabIndex}"/>
            </div>
            <div class="col-xs-offset-3 col-xs-9">
                <form:errors path="roomPin" cssClass="error"/>
            </div>
        </div>
    </c:if>


    <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'} && ${reservationRequest.specificationType != 'MEETING_ROOM'}">
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

    <c:if test="${not empty confirmTitle || cancelUrl != null}">
        <c:set var="buttons">
            <c:if test="${not empty confirmTitle}">
                <spring:message code="${confirmTitle}" var="confirmTitle"/>
                <input class="btn btn-primary" type="submit" value="${confirmTitle}" tabindex="${tabIndex}"/>
            </c:if>
            <c:if test="${cancelUrl != null}">
                <c:if test="${empty cancelTitle}">
                    <c:set var="cancelTitle" value="views.button.cancel"/>
                </c:if>
                <a class="btn btn-default" href="${cancelUrl}" tabindex="${tabIndex}"><spring:message code="${cancelTitle}"/></a>
            </c:if>
        </c:set>
    </c:if>

    <c:choose>
        <c:when test="${not empty content}">
            <div class="table-actions-left">${content}</div>
            <div class="table-actions pull-right">${buttons}</div>
        </c:when>
        <c:otherwise>
            <div class="form-group">
                <div class="col-xs-offset-3 col-xs-4">${buttons}</div>
            </div>
        </c:otherwise>
    </c:choose>

</form:form>
