<%--
  -- Page for creation/modification of a reservation request.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.models.ReservationRequestModel" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<tiles:importAttribute/>

<script type="text/javascript">
    // Angular application
    var app = angular.module('ngReservationRequestUpdate', ['ngDateTime', 'ngTooltip']);

    // Form controller
    function FormController($scope) {
        // Get value or default value if null
        $scope.value = function(value, defaultValue) {
            return ((value == null || value == '') ? defaultValue : value);
        };

        // Get dynamic reservation request attributes
        $scope.id = $scope.value('${reservationRequest.id}', null);
        $scope.specificationType = $scope.value('${reservationRequest.specificationType}', 'ADHOC_ROOM');
        $scope.technology = $scope.value('${reservationRequest.technology}', 'H323_SIP');

        // Specifies whether we are modifying an existing reservation request
        $scope.modification = $scope.id != null;

        // Set proper date/time format for start date/time picker
        $scope.$watch("specificationType", function () {
            var dateTimePicker = $('#start').data("datetimepicker");
            if ( $scope.specificationType == 'PERMANENT_ROOM') {
                dateTimePicker.setFormatDate();
            }
            else {
                dateTimePicker.setFormatDateTime();
            }
        });

        // Permanent rooms
        var permanentRooms = {<c:forEach items="${permanentRooms}" var="permanentRoom" varStatus="status">
            "${permanentRoom.id}": {
                id: "${permanentRoom.id}",
                name: "${permanentRoom.specification.value}",
                formattedSlot: "<joda:format value="${permanentRoom.earliestSlot.start}" style="M-"/> - <joda:format value="${permanentRoom.earliestSlot.end}" style="M-"/>",
                slot: "${permanentRoom.earliestSlot}"
            }<c:if test="${!status.last}">,</c:if></c:forEach>
        };
        $scope.permanentRooms = {};
        for (var permanentRoom in permanentRooms) {
            $scope.permanentRooms[permanentRoom] = permanentRooms[permanentRoom];
        }
        $scope.permanentRoom = $scope.permanentRooms["${reservationRequest.permanentRoomCapacityReservationRequestId}"];
        $scope.updatePermanentRooms = function(performApply) {
            // Determine requested slot
            var requestedStart = moment($("#start").val());
            var requestedDuration = parseInt($("#durationCount").val());
            var requestedEnd = (requestedStart != null && requestedDuration > 0) ? requestedStart.add($("#durationType").val().toLowerCase(), requestedDuration) : null;
            requestedStart = requestedStart != null ? requestedStart.unix() : null;
            requestedEnd = requestedEnd != null ? requestedEnd.unix() : null;

            // Delete old permanent rooms
            for (var permanentRoom in $scope.permanentRooms) {
                delete $scope.permanentRooms[permanentRoom];
            }

            // Add matching permanent rooms
            for ( var permanentRoomId in permanentRooms) {
                var permanentRoom = permanentRooms[permanentRoomId];
                var permanentRoomSlot = permanentRoom.slot.split("/");
                if ( requestedStart != null || requestedEnd != null ) {
                    var permanentRoomStart = moment(permanentRoomSlot[0]).unix();
                    var permanentRoomEnd = moment(permanentRoomSlot[1]).unix();
                    if ( (requestedStart != null && (requestedStart < permanentRoomStart || requestedStart >= permanentRoomEnd)) ||
                            (requestedEnd != null && (requestedEnd <= permanentRoomStart || requestedEnd > permanentRoomEnd)) ) {
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
            if ( performApply !== false ) {
                $scope.$apply();
            }
        };
        $("#start,#durationCount").change(function() {
            $scope.updatePermanentRooms();
        });
        $scope.updatePermanentRooms(false);
    }
</script>

<div ng-app="ngReservationRequestUpdate">

<form:form class="form-horizontal"
           commandName="reservationRequest"
           action="${contextPath}${confirmUrl}"
           method="post"
           ng-controller="FormController">

    <fieldset>

        <legend class="select">
            <input type="hidden" name="specificationType" value="{{specificationType}}"/>
            <ul class="nav nav-pills" ng-class="{disabled: modification}">
                <li ng-class="{active: specificationType == 'ADHOC_ROOM'}">
                    <a href="" ng-click="modification || (specificationType = 'ADHOC_ROOM')">
                        <spring:message code="views.reservationRequest.specification.ADHOC_ROOM"/>
                    </a>
                </li>
                <li ng-class="{active: specificationType == 'PERMANENT_ROOM'}">
                    <a href="" ng-click="modification || (specificationType = 'PERMANENT_ROOM')">
                        <spring:message code="views.reservationRequest.specification.PERMANENT_ROOM"/>
                    </a>
                </li>
                <li ng-class="{active: specificationType == 'PERMANENT_ROOM_CAPACITY'}">
                    <a href="" ng-click="modification || (specificationType = 'PERMANENT_ROOM_CAPACITY')">
                        <spring:message code="views.reservationRequest.specification.PERMANENT_ROOM_CAPACITY"/>
                    </a>
                </li>
            </ul>
            <app:help>
                <strong><spring:message code="views.reservationRequest.specification.ADHOC_ROOM"/></strong>
                <p><spring:message code="views.help.reservationRequest.specification.ADHOC_ROOM"/></p>
                <strong><spring:message code="views.reservationRequest.specification.PERMANENT_ROOM"/></strong>
                <p><spring:message code="views.help.reservationRequest.specification.PERMANENT_ROOM"/></p>
                <strong><spring:message code="views.reservationRequest.specification.PERMANENT_ROOM_CAPACITY"/></strong>
                <p><spring:message code="views.help.reservationRequest.specification.PERMANENT_ROOM_CAPACITY"/></p>
            </app:help>
        </legend>

        <c:if test="${reservationRequest.id != null}">
            <div class="control-group">
                <form:label class="control-label" path="id">
                    <spring:message code="views.reservationRequest.identifier"/>:
                </form:label>
                <div class="controls double-width">
                    <form:input path="id" readonly="true"/>
                </div>
            </div>
        </c:if>

        <div class="control-group">
            <form:label class="control-label" path="purpose">
                <spring:message code="views.reservationRequest.purpose"/>:
            </form:label>
            <div class="controls">
                <form:select path="purpose">
                    <form:option value="SCIENCE"><spring:message code="views.reservationRequest.purpose.SCIENCE"/></form:option>
                    <form:option value="EDUCATION"><spring:message code="views.reservationRequest.purpose.EDUCATION"/></form:option>
                </form:select>
            </div>
        </div>

        <div class="control-group" ng-show="specificationType == 'PERMANENT_ROOM' || specificationType == 'ADHOC_ROOM'" class="hide">
            <form:label class="control-label" path="technology">
                <spring:message code="views.reservationRequest.technology"/>:
            </form:label>
            <div class="controls">
                <form:select path="technology" ng-model="technology">
                    <form:option value="H323_SIP"><%= ReservationRequestModel.Technology.H323_SIP.getTitle() %></form:option>
                    <form:option value="ADOBE_CONNECT"><%= ReservationRequestModel.Technology.ADOBE_CONNECT.getTitle() %></form:option>
                </form:select>
            </div>
        </div>

        <div class="control-group" ng-show="specificationType == 'PERMANENT_ROOM_CAPACITY'" class="hide">
            <form:label class="control-label" path="permanentRoomCapacityReservationRequestId">
                <spring:message code="views.reservationRequest.specification.permanentRoomCapacityReservationRequestId"/>:
            </form:label>
            <div class="controls">
                <form:select path="permanentRoomCapacityReservationRequestId" cssErrorClass="error"
                             ng-model="permanentRoom" ng-options="option.name for (value, option) in permanentRooms">
                    <form:option value="">-- <spring:message code="views.select.choose"/> --</form:option>
                    {{option}}
                </form:select>
                <form:errors path="permanentRoomCapacityReservationRequestId" cssClass="error"/>
                <div ng-show="permanentRoom" class="description"><b><spring:message code="views.reservationRequest.validity"/>: </b>{{permanentRoom.formattedSlot}}</div>
            </div>
        </div>

        <div class="control-group">
            <form:label class="control-label" path="description">
                <spring:message code="views.reservationRequest.description"/>:
            </form:label>
            <div class="controls double-width">
                <form:input path="description" cssErrorClass="error"/>
                <form:errors path="description" cssClass="error"/>
                <app:help><spring:message code="views.help.reservationRequest.description"/></app:help>
            </div>
        </div>

        <div class="control-group" ng-show="specificationType == 'PERMANENT_ROOM'" class="hide">
            <form:label class="control-label" path="permanentRoomName">
                <spring:message code="views.reservationRequest.specification.permanentRoomName"/>:
            </form:label>
            <div class="controls">
                <form:input path="permanentRoomName" cssErrorClass="error"/>
                <form:errors path="permanentRoomName" cssClass="error"/>
            </div>
        </div>

        <div class="control-group" ng-show="specificationType == 'ADHOC_ROOM' || specificationType == 'PERMANENT_ROOM_CAPACITY'" class="hide">
            <form:label class="control-label" path="roomParticipantCount">
                <spring:message code="views.reservationRequest.specification.roomParticipantCount"/>:
            </form:label>
            <div class="controls">
                <form:input path="roomParticipantCount" cssErrorClass="error"/>
                <form:errors path="roomParticipantCount" cssClass="error"/>
            </div>
        </div>

        <div class="control-group">
            <form:label class="control-label" path="start">
                <spring:message code="views.reservationRequest.start"/>:
            </form:label>
            <div class="controls">
                <form:input path="start" cssErrorClass="error" date-time-picker="true"/>
                <form:errors path="start" cssClass="error"/>
            </div>
        </div>

        <div class="control-group" ng-show="specificationType == 'PERMANENT_ROOM'" class="hide">
            <form:label class="control-label" path="end">
                <spring:message code="views.reservationRequest.end"/>:
            </form:label>
            <div class="controls">
                <form:input path="end" cssErrorClass="error" date-time-picker="true" format="date"/>
                <form:errors path="end" cssClass="error"/>
            </div>
        </div>

        <div class="control-group" ng-show="specificationType == 'ADHOC_ROOM' || specificationType == 'PERMANENT_ROOM_CAPACITY'" class="hide">
            <form:label class="control-label" path="durationCount">
                <spring:message code="views.reservationRequest.duration"/>:
            </form:label>
            <div class="controls">
                <form:input path="durationCount" cssErrorClass="error" cssStyle="width: 100px;"/>
                <form:select path="durationType" cssStyle="width: 100px;">
                    <form:option value="MINUTE"><spring:message code="views.reservationRequest.duration.minutes"/></form:option>
                    <form:option value="HOUR"><spring:message code="views.reservationRequest.duration.hours"/></form:option>
                    <form:option value="DAY"><spring:message code="views.reservationRequest.duration.days"/></form:option>
                </form:select>
                <form:errors path="durationCount" cssClass="error"/>
            </div>
        </div>

        <div class="control-group" ng-show="specificationType == 'ADHOC_ROOM' || specificationType == 'PERMANENT_ROOM_CAPACITY'" class="hide">
            <form:label class="control-label" path="periodicityType">
                <spring:message code="views.reservationRequest.periodicity"/>:
            </form:label>
            <div class="controls">
                <label class="radio inline" for="periodicity-none">
                    <form:radiobutton id="periodicity-none" path="periodicityType" value="NONE"/>
                    <spring:message code="views.reservationRequest.periodicity.NONE"/>
                </label>
                <label class="radio inline" for="periodicity-daily">
                    <form:radiobutton id="periodicity-daily" path="periodicityType" value="DAILY"/>
                    <spring:message code="views.reservationRequest.periodicity.DAILY"/>
                </label>
                <label class="radio inline" for="periodicity-weekly">
                    <form:radiobutton id="periodicity-weekly" path="periodicityType" value="WEEKLY"/>
                    <spring:message code="views.reservationRequest.periodicity.WEEKLY"/>
                </label>
                &nbsp;
                <div class="input-prepend">
                    <span class="add-on"><spring:message code="views.reservationRequest.periodicity.until"/></span>
                    <form:input path="periodicityEnd" cssErrorClass="error" date-time-picker="true" format="date"/>
                </div>
                <form:errors path="periodicityEnd" cssClass="error"/>
            </div>
        </div>

        <div class="control-group" ng-show="(specificationType == 'ADHOC_ROOM' || specificationType == 'PERMANENT_ROOM_CAPACITY') && technology == 'H323_SIP'" class="hide">
            <form:label class="control-label" path="roomPin">
                <spring:message code="views.reservationRequest.specification.roomPin"/>:
            </form:label>
            <div class="controls">
                <form:input path="roomPin" cssErrorClass="error"/>
                <form:errors path="roomPin" cssClass="error"/>
            </div>
        </div>

    </fieldset>

    <div class="control-group">
        <div class="controls">
            <spring:message code="${confirm}" var="confirm"/>
            <input class="btn btn-primary" type="submit" value="${confirm}"/>
            <a class="btn" href="${contextPath}/reservation-request"><spring:message code="views.button.cancel"/></a>
        </div>
    </div>

</form:form>

</div>
