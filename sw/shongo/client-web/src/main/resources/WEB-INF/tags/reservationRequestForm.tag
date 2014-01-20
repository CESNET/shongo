<%--
  -- Reservation request form.
  --%>
<%@ tag import="cz.cesnet.shongo.client.web.models.TechnologyModel" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="permanentRooms" required="false" type="java.util.Collection<cz.cesnet.shongo.controller.api.ReservationRequestSummary>" %>
<%@attribute name="confirmTitle" required="false" type="java.lang.String" %>
<%@attribute name="cancelUrl" required="false" type="java.lang.String" %>
<%@attribute name="cancelTitle" required="false" type="java.lang.String" %>

<c:set var="reservationRequestModification" value="${reservationRequest.modification}"/>
<c:set var="tabIndex" value="1"/>

<script type="text/javascript">
    var module = angular.module('tag:reservationRequestForm', ['ngDateTime', 'ngTooltip']);
    module.controller("ReservationRequestFormController", function($scope) {
        // Setup child scope
        if ( $scope.$parent != null ) {
            $scope.$parent.$child = $scope;
        }

        // Get value or default value if null
        $scope.value = function (value, defaultValue) {
            return ((value == null || value == '') ? defaultValue : value);
        };

        // Get dynamic reservation request attributes
        $scope.id = $scope.value('${reservationRequest.id}', null);
        $scope.technology = $scope.value('${reservationRequest.technology}', 'H323_SIP');

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

    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM_CAPACITY'}">
        // Get permanent rooms
        var permanentRooms = {<c:forEach items="${permanentRooms}" var="permanentRoom" varStatus="status"><spring:eval expression="T(cz.cesnet.shongo.client.web.models.TechnologyModel).find(permanentRoom.specificationTechnologies)" var="technology" />
            "${permanentRoom.id}": {
                id: "${permanentRoom.id}",
                name: "${permanentRoom.roomName} (${technology.title})",
                formattedSlot: "<tag:format value="${permanentRoom.earliestSlot}" style="date"/>",
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
    });
</script>

<form:form class="form-horizontal"
           commandName="reservationRequest"
           method="post"
           ng-controller="ReservationRequestFormController">

    <fieldset>

        <c:if test="${not empty reservationRequest.id}">
            <div class="control-group">
                <form:label class="control-label" path="id">
                    <spring:message code="views.reservationRequest.identifier"/>:
                </form:label>
                <div class="controls double-width">
                    <form:input path="id" readonly="true" tabindex="${tabIndex}"/>
                </div>
            </div>
        </c:if>

        <c:choose>
            <c:when test="${reservationRequest.specificationType != 'PERMANENT_ROOM_CAPACITY'}">
                <div class="control-group">
                    <form:label class="control-label" path="technology">
                        <spring:message code="views.reservationRequest.technology"/>:
                    </form:label>
                    <div class="controls">
                        <form:select path="technology" ng-model="technology" tabindex="${tabIndex}">
                            <form:option value="H323_SIP">
                                <%= TechnologyModel.H323_SIP.getTitle() %>
                            </form:option>
                            <form:option value="ADOBE_CONNECT">
                                <%= TechnologyModel.ADOBE_CONNECT.getTitle() %>
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
                <div class="controls double-width">
                    <form:select path="permanentRoomReservationRequestId" cssErrorClass="error"
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

        <div class="control-group">
            <form:label class="control-label" path="description">
                <spring:message code="views.reservationRequest.description"/>:
            </form:label>
            <div class="controls double-width">
                <form:input path="description" cssErrorClass="error" tabindex="${tabIndex}"/>
                <tag:help><spring:message code="views.reservationRequest.descriptionHelp"/></tag:help>
                <form:errors path="description" cssClass="error"/>
            </div>
        </div>

        <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
            <div class="control-group">
                <form:label class="control-label" path="roomName">
                    <spring:message code="views.reservationRequest.specification.roomName"/>:
                </form:label>
                <div class="controls">
                    <form:input path="roomName" cssErrorClass="error" tabindex="${tabIndex}"/>
                    <form:errors path="roomName" cssClass="error"/>
                </div>
            </div>
        </c:if>

        <c:if test="${reservationRequestModification != null && reservationRequest.specificationType == 'ADHOC_ROOM' && not empty reservationRequest.roomName}">
            <div class="control-group">
                <form:label class="control-label" path="roomName">
                    <spring:message code="views.reservationRequest.specification.roomName"/>:
                </form:label>
                <div class="controls">
                    <label class="radio inline" for="room-name-new">
                        <form:radiobutton id="room-name-new" path="adhocRoomRetainRoomName" value="false" tabindex="${tabIndex}"/>
                        <span><spring:message code="views.reservationRequest.specification.roomName.new"/></span>
                    </label>
                    <label class="radio inline" for="room-name-retain">
                        <form:radiobutton id="room-name-retain" path="adhocRoomRetainRoomName" value="true" tabindex="${tabIndex}"/>
                        <span><spring:message code="views.reservationRequest.specification.roomName.retain"/></span>
                    </label>
                    &nbsp;
                    <form:input path="roomName" tabindex="${tabIndex}" readonly="true"/>
                    <tag:help><spring:message code="views.reservationRequest.specification.roomName.retainHelp"/></tag:help>
                </div>
            </div>
        </c:if>

        <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
            <div class="control-group">
                <form:label class="control-label" path="roomParticipantCount">
                    <spring:message code="views.reservationRequest.specification.roomParticipantCount"/>:
                </form:label>
                <div class="controls">
                    <form:input path="roomParticipantCount" cssErrorClass="error" tabindex="${tabIndex}"/>
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
                        <form:input path="start" cssErrorClass="error" date-picker="true" tabindex="${tabIndex}"/>
                    </c:when>
                    <c:otherwise>
                        <form:input path="start" cssErrorClass="error" date-time-picker="true" tabindex="${tabIndex}"/>
                    </c:otherwise>
                </c:choose>
                <form:errors path="start" cssClass="error"/>
                <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
                    &nbsp;
                    <div class="input-append">
                        <form:select path="slotBeforeMinutes" cssStyle="width: 75px;" tabindex="${tabIndex}">
                            <form:option value="0"><spring:message code="views.reservationRequest.slotMinutesNone"/></form:option>
                            <form:option value="5">5</form:option>
                            <form:option value="10">10</form:option>
                            <form:option value="15">15</form:option>
                            <form:option value="20">20</form:option>
                            <form:option value="30">30</form:option>
                            <form:option value="45">45</form:option>
                        </form:select>
                        <span class="add-on">
                            <spring:message code="views.reservationRequest.slotBeforeMinutes"/>
                            <tag:help><spring:message code="views.reservationRequest.slotBeforeMinutes.help"/></tag:help>
                        </span>
                    </div>
                    <form:errors path="slotBeforeMinutes" cssClass="error"/>
                </c:if>
            </div>
        </div>

        <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
            <div class="control-group">
                <form:label class="control-label" path="end">
                    <spring:message code="views.reservationRequest.end"/>:
                </form:label>
                <div class="controls">
                    <form:input path="end" cssErrorClass="error" date-time-picker="true" format="date" tabindex="${tabIndex}"/>
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
                    <form:input path="durationCount" cssErrorClass="error" cssStyle="width: 95px;" tabindex="${tabIndex}"/>
                    &nbsp;
                    <form:select path="durationType" cssStyle="width: 100px;" tabindex="${tabIndex}">
                        <form:option value="MINUTE"><spring:message
                                code="views.reservationRequest.duration.minutes"/></form:option>
                        <form:option value="HOUR"><spring:message
                                code="views.reservationRequest.duration.hours"/></form:option>
                        <form:option value="DAY"><spring:message
                                code="views.reservationRequest.duration.days"/></form:option>
                    </form:select>
                    <form:errors path="durationCount" cssClass="error"/>
                    &nbsp;
                    <div class="input-append">
                        <form:select path="slotAfterMinutes" cssStyle="width: 75px;" tabindex="${tabIndex}">
                            <form:option value="0"><spring:message code="views.reservationRequest.slotMinutesNone"/></form:option>
                            <form:option value="5">5</form:option>
                            <form:option value="10">10</form:option>
                            <form:option value="15">15</form:option>
                            <form:option value="20">20</form:option>
                            <form:option value="30">30</form:option>
                            <form:option value="45">45</form:option>
                        </form:select>
                        <span class="add-on">
                            <spring:message code="views.reservationRequest.slotAfterMinutes"/>
                            <tag:help><spring:message code="views.reservationRequest.slotAfterMinutes.help"/></tag:help>
                        </span>
                    </div>
                    <form:errors path="slotAfterMinutes" cssClass="error"/>
                </div>
            </div>

            <div class="control-group">
                <form:label class="control-label" path="periodicityType">
                    <spring:message code="views.reservationRequest.periodicity"/>:
                </form:label>
                <div class="controls">
                    <label class="radio inline" for="periodicity-none">
                        <form:radiobutton id="periodicity-none" path="periodicityType" value="NONE" tabindex="${tabIndex}"/>
                        <spring:message code="views.reservationRequest.periodicity.NONE"/>
                    </label>
                    <label class="radio inline" for="periodicity-daily">
                        <form:radiobutton id="periodicity-daily" path="periodicityType" value="DAILY" tabindex="${tabIndex}"/>
                        <spring:message code="views.reservationRequest.periodicity.DAILY"/>
                    </label>
                    <label class="radio inline" for="periodicity-weekly">
                        <form:radiobutton id="periodicity-weekly" path="periodicityType" value="WEEKLY" tabindex="${tabIndex}"/>
                        <spring:message code="views.reservationRequest.periodicity.WEEKLY"/>
                    </label>
                    &nbsp;
                    <div class="input-prepend">
                            <span class="add-on">
                                <spring:message code="views.reservationRequest.periodicity.until"/>
                            </span>
                        <form:input path="periodicityEnd" cssErrorClass="error" date-picker="true" tabindex="${tabIndex}"/>
                    </div>
                    <form:errors path="periodicityEnd" cssClass="error"/>
                </div>
            </div>
        </c:if>

        <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
            <div class="control-group" ng-show="technology == 'ADOBE_CONNECT'" class="hide">
                <form:label class="control-label" path="roomAccessMode">
                    <spring:message code="views.reservationRequest.specification.roomAccessMode"/>:
                </form:label>
                <div class="controls">
                    <spring:eval var="enumAdobeConnectAccessMode" expression="T(cz.cesnet.shongo.api.AdobeConnectAccessMode).values()"/>
                    <c:forEach var="accessMode" items="${enumAdobeConnectAccessMode}">
                        <label class="radio inline" for="${accessMode}">
                            <c:choose>
                                <c:when test="${accessMode == 'PROTECTED' && reservationRequest.roomAccessMode == null}">
                                    <form:radiobutton id="${accessMode}" path="roomAccessMode" value="${accessMode}" tabindex="${tabIndex}" checked="checked"/>
                                </c:when>
                                <c:otherwise>
                                    <form:radiobutton id="${accessMode}" path="roomAccessMode" value="${accessMode}" tabindex="${tabIndex}"/>
                                </c:otherwise>
                            </c:choose>
                            <spring:message code="views.reservationRequest.specification.roomAccessMode.${accessMode}"/>
                        </label>
                    </c:forEach>
                    <tag:help><spring:message code="views.reservationRequest.specification.roomAccessMode.help"/></tag:help>
                    <form:errors path="roomAccessMode" cssClass="error"/>
                </div>
            </div>
        </c:if>

        <div class="control-group" ng-show="technology == 'H323_SIP' || technology == 'ADOBE_CONNECT'" class="hide">
            <form:label class="control-label" path="roomPin">
                <spring:message code="views.reservationRequest.specification.roomPin"/>:
            </form:label>
            <div class="controls">
                <form:input path="roomPin" cssErrorClass="error" tabindex="${tabIndex}"/>
                <form:errors path="roomPin" cssClass="error"/>
            </div>
        </div>

        <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
            <div class="control-group" ng-hide="technology == 'ADOBE_CONNECT'">
                <form:label class="control-label" path="roomRecorded">
                    <spring:message code="views.reservationRequest.specification.roomRecorded"/>:
                </form:label>
                <div class="controls">
                    <form:checkbox path="roomRecorded" cssErrorClass="error" tabindex="${tabIndex}" value="xx"/>
                    <tag:help><spring:message code="views.reservationRequest.specification.roomRecordedHelp"/></tag:help>
                    <form:errors path="roomRecorded" cssClass="error"/>
                </div>
            </div>
        </c:if>

    </fieldset>

    <jsp:doBody var="body"/>

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
                <a class="btn" href="${cancelUrl}" tabindex="${tabIndex}"><spring:message code="${cancelTitle}"/></a>
            </c:if>
        </c:set>
    </c:if>

    <c:choose>
        <c:when test="${not empty body}">
            <div class="table-actions-left">${body}</div>
            <div class="table-actions pull-right">${buttons}</div>
        </c:when>
        <c:otherwise>
            <div class="control-group">
                <div class="controls">${buttons}</div>
            </div>
        </c:otherwise>
    </c:choose>

</form:form>
