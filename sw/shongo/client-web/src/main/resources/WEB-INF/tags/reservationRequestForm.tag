<%--
  -- Reservation request form.
  --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ tag import="cz.cesnet.shongo.client.web.models.ReservationRequestModel" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="permanentRooms" required="false" type="java.util.Collection<cz.cesnet.shongo.controller.api.ReservationRequestSummary>" %>
<%@attribute name="confirmUrl" required="false" type="java.lang.String" %>
<%@attribute name="confirmTitle" required="false" type="java.lang.String" %>
<%@attribute name="cancelUrl" required="false" type="java.lang.String" %>
<%@attribute name="cancelTitle" required="false" type="java.lang.String" %>

<script type="text/javascript">
    angular.module('tag:reservationRequestForm', ['ngDateTime', 'ngTooltip']);

    function ReservationRequestFormController($scope) {
        // Get value or default value if null
        $scope.value = function (value, defaultValue) {
            return ((value == null || value == '') ? defaultValue : value);
        };

        // Get dynamic reservation request attributes
        $scope.id = $scope.value('${reservationRequest.id}', null);
        $scope.technology = $scope.value('${reservationRequest.technology}', 'H323_SIP');

    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM_CAPACITY'}">
        // Get permanent rooms
        var permanentRooms = {<c:forEach items="${permanentRooms}" var="permanentRoom" varStatus="status"><spring:eval expression="T(cz.cesnet.shongo.client.web.models.ReservationRequestModel$Technology).find(permanentRoom.technologies)" var="technology" />
            "${permanentRoom.id}": {
                id: "${permanentRoom.id}",
                name: "${permanentRoom.specification.value}",
                formattedSlot: "<joda:format value="${permanentRoom.earliestSlot.start}" style="M-"/> - <joda:format value="${permanentRoom.earliestSlot.end}" style="M-"/>",
                slot: "${permanentRoom.earliestSlot}",
                technology: "${technology}"
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
        // Update permanent rooms model when start or duration changes
        $("#start,#durationCount").change(function () {
            $scope.updatePermanentRooms();
        });
        // Set proper technology for selected permanent room
        $scope.$watch("permanentRoom", function () {
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
    }
</script>

<form:form class="form-horizontal"
           commandName="reservationRequest"
           action="${confirmUrl}"
           method="post"
           ng-controller="ReservationRequestFormController">

    <fieldset>

        <c:if test="${not empty reservationRequest.id}">
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
                    <form:option value="SCIENCE">
                        <spring:message code="views.reservationRequest.purpose.SCIENCE"/>
                    </form:option>
                    <form:option value="EDUCATION">
                        <spring:message code="views.reservationRequest.purpose.EDUCATION"/>
                    </form:option>
                </form:select>
            </div>
        </div>

        <c:choose>
            <c:when test="${reservationRequest.specificationType != 'PERMANENT_ROOM_CAPACITY'}">
                <div class="control-group">
                    <form:label class="control-label" path="technology">
                        <spring:message code="views.reservationRequest.technology"/>:
                    </form:label>
                    <div class="controls">
                        <form:select path="technology" ng-model="technology">
                            <form:option value="H323_SIP">
                                <%= ReservationRequestModel.Technology.H323_SIP.getTitle() %>
                            </form:option>
                            <form:option value="ADOBE_CONNECT">
                                <%= ReservationRequestModel.Technology.ADOBE_CONNECT.getTitle() %>
                            </form:option>
                        </form:select>
                        <form:errors path="technology" cssClass="error"/>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
                <input type="hidden" name="technology" value="{{technology}}"/>
            </c:otherwise>
        </c:choose>

        <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM_CAPACITY'}">
            <div class="control-group">
                <form:label class="control-label" path="permanentRoomReservationRequestId"><spring:message
                        code="views.reservationRequest.specification.permanentRoomReservationRequestId"/>:
                </form:label>
                <div class="controls">
                    <form:select path="permanentRoomReservationRequestId" cssErrorClass="error"
                                 ng-model="permanentRoom"
                                 ng-options="option.name for (value, option) in permanentRooms">
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

        <div class="control-group">
            <form:label class="control-label" path="description">
                <spring:message code="views.reservationRequest.description"/>:
            </form:label>
            <div class="controls double-width">
                <form:input path="description" cssErrorClass="error"/>
                <form:errors path="description" cssClass="error"/>
                <tag:help><spring:message code="views.help.reservationRequest.description"/></tag:help>
            </div>
        </div>

        <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
            <div class="control-group">
                <form:label class="control-label" path="permanentRoomName">
                    <spring:message code="views.reservationRequest.specification.permanentRoomName"/>:
                </form:label>
                <div class="controls">
                    <form:input path="permanentRoomName" cssErrorClass="error"/>
                    <form:errors path="permanentRoomName" cssClass="error"/>
                </div>
            </div>
        </c:if>

        <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
            <div class="control-group">
                <form:label class="control-label" path="roomParticipantCount">
                    <spring:message code="views.reservationRequest.specification.roomParticipantCount"/>:
                </form:label>
                <div class="controls">
                    <form:input path="roomParticipantCount" cssErrorClass="error"/>
                    <form:errors path="roomParticipantCount" cssClass="error"/>
                </div>
            </div>
        </c:if>

        <div class="control-group">
            <form:label class="control-label" path="start">
                <spring:message code="views.reservationRequest.start"/>:
            </form:label>
            <div class="controls">
                <c:choose>
                    <c:when test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
                        <form:input path="start" cssErrorClass="error" date-picker="true"/>
                    </c:when>
                    <c:otherwise>
                        <form:input path="start" cssErrorClass="error" date-time-picker="true"/>
                    </c:otherwise>
                </c:choose>
                <form:errors path="start" cssClass="error"/>
            </div>
        </div>

        <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
            <div class="control-group">
                <form:label class="control-label" path="end">
                    <spring:message code="views.reservationRequest.end"/>:
                </form:label>
                <div class="controls">
                    <form:input path="end" cssErrorClass="error" date-time-picker="true" format="date"/>
                    <form:errors path="end" cssClass="error"/>
                </div>
            </div>
        </c:if>

        <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
            <div class="control-group">
                <form:label class="control-label" path="durationCount">
                    <spring:message code="views.reservationRequest.duration"/>:
                </form:label>
                <div class="controls">
                    <form:input path="durationCount" cssErrorClass="error" cssStyle="width: 95px;"/>
                    &nbsp;
                    <form:select path="durationType" cssStyle="width: 100px;">
                        <form:option value="MINUTE"><spring:message
                                code="views.reservationRequest.duration.minutes"/></form:option>
                        <form:option value="HOUR"><spring:message
                                code="views.reservationRequest.duration.hours"/></form:option>
                        <form:option value="DAY"><spring:message
                                code="views.reservationRequest.duration.days"/></form:option>
                    </form:select>
                    <form:errors path="durationCount" cssClass="error"/>
                </div>
            </div>

            <div class="control-group">
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
                            <span class="add-on">
                                <spring:message code="views.reservationRequest.periodicity.until"/>
                            </span>
                        <form:input path="periodicityEnd" cssErrorClass="error" date-picker="true"/>
                    </div>
                    <form:errors path="periodicityEnd" cssClass="error"/>
                </div>
            </div>
        </c:if>

        <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
            <div class="control-group" ng-show="technology == 'H323_SIP'" class="hide">
                <form:label class="control-label" path="roomPin">
                    <spring:message code="views.reservationRequest.specification.roomPin"/>:
                </form:label>
                <div class="controls">
                    <form:input path="roomPin" cssErrorClass="error"/>
                    <form:errors path="roomPin" cssClass="error"/>
                </div>
            </div>
        </c:if>

    </fieldset>

    <c:if test="${not empty confirmTitle || backUrl != null}">
        <div class="control-group">
            <div class="controls">
                <c:if test="${not empty confirmTitle}">
                    <spring:message code="${confirmTitle}" var="confirmTitle"/>
                    <input class="btn btn-primary" type="submit" value="${confirmTitle}"/>
                </c:if>
                <c:if test="${cancelUrl != null}">
                    <c:if test="${empty cancelTitle}">
                        <c:set var="cancelTitle" value="views.button.cancel"/>
                    </c:if>
                    <a class="btn" href="${cancelUrl}"><spring:message code="${cancelTitle}"/></a>
                </c:if>
            </div>
        </div>
    </c:if>

</form:form>
