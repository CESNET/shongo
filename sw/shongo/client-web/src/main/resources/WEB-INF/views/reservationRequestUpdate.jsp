<%--
  -- Page for creation/modification of a reservation request.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.models.ReservationRequestModel" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<tiles:importAttribute/>

<script type="text/javascript">
    // Angular application
    angular.module('ngReservationRequestUpdate', ['ngDateTime']);

    // Form controller
    function FormController($scope) {
        $scope.value = function(value, defaultValue) {
            return ((value == null || value == '') ? defaultValue : value);
        };
        $scope.id = $scope.value('${reservationRequest.id}', null);
        $scope.type = $scope.value('${reservationRequest.type}', 'ALIAS');
        $scope.technology = $scope.value('${reservationRequest.technology}', 'H323_SIP');
        $scope.$watch("type", function () {
            var dateTimePicker = $('#start').data("datetimepicker");
            if ( $scope.type == 'ALIAS') {
                dateTimePicker.setFormatDate();
            }
            else {
                dateTimePicker.setFormatDateTime();
            }
        });
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
            <input type="hidden" name="type" value="{{type}}"/>
            <ul class="nav nav-pills" >
                <li ng-class="{active: type == 'ALIAS'}">
                    <a href="" ng-click="type = 'ALIAS'"><spring:message code="views.reservationRequest.specification.alias"/></a>
                </li>
                <li ng-class="{active: type == 'ROOM'}">
                    <a href="" ng-click="type = 'ROOM'"><spring:message code="views.reservationRequest.specification.room"/></a>
                </li>
            </ul>
        </legend>

        <c:if test="${reservationRequest.id != null}">
            <div class="control-group">
                <form:label class="control-label" path="id">
                    <spring:message code="views.reservationRequest.identifier"/>:
                </form:label>
                <div class="controls">
                    <form:input path="id" readonly="true"/>
                </div>
            </div>
        </c:if>

        <div class="control-group">
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

        <div class="control-group" ng-show="type == 'ALIAS'" class="hide">
            <form:label class="control-label" path="aliasRoomName">
                <spring:message code="views.reservationRequest.specification.alias.roomName"/>:
            </form:label>
            <div class="controls">
                <form:input path="aliasRoomName" cssErrorClass="error"/>
                <form:errors path="aliasRoomName" cssClass="error"/>
            </div>
        </div>

        <div class="control-group" ng-show="type == 'ROOM'" class="hide">
            <form:label class="control-label" path="roomAliasReservationId">
                <spring:message code="views.reservationRequest.specification.room.alias"/>:
            </form:label>
            <div class="controls">
                <form:select path="roomAliasReservationId">
                    <form:option value="ADHOC">Ad-Hoc</form:option>
                </form:select>
                <form:errors path="roomAliasReservationId" cssClass="error"/>
            </div>
        </div>

        <div class="control-group" ng-show="type == 'ROOM'" class="hide">
            <form:label class="control-label" path="roomParticipantCount">
                <spring:message code="views.reservationRequest.specification.room.participantCount"/>:
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

        <div class="control-group" ng-show="type == 'ALIAS'" class="hide">
            <form:label class="control-label" path="end">
                <spring:message code="views.reservationRequest.end"/>:
            </form:label>
            <div class="controls">
                <form:input path="end" cssErrorClass="error" date-time-picker="true" format="date"/>
                <form:errors path="end" cssClass="error"/>
            </div>
        </div>

        <div class="control-group" ng-show="type == 'ROOM'" class="hide">
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

        <div class="control-group" ng-show="type == 'ROOM'" class="hide">
            <form:label class="control-label" path="periodicityType">
                <spring:message code="views.reservationRequest.periodicity"/>:
            </form:label>
            <div class="controls">
                <label class="radio inline" for="periodicity-none">
                    <form:radiobutton id="periodicity-none" path="periodicityType" value="NONE"/>
                    <spring:message code="views.reservationRequest.periodicity.none"/>
                </label>
                <label class="radio inline" for="periodicity-daily">
                    <form:radiobutton id="periodicity-daily" path="periodicityType" value="DAILY"/>
                    <spring:message code="views.reservationRequest.periodicity.daily"/>
                </label>
                <label class="radio inline" for="periodicity-weekly">
                    <form:radiobutton id="periodicity-weekly" path="periodicityType" value="WEEKLY"/>
                    <spring:message code="views.reservationRequest.periodicity.weekly"/>
                </label>
                &nbsp;
                <div class="input-prepend">
                    <span class="add-on"><spring:message code="views.reservationRequest.periodicity.until"/></span>
                    <form:input path="periodicityEnd" cssErrorClass="error" date-time-picker="true" format="date"/>
                </div>
                <form:errors path="periodicityEnd" cssClass="error"/>
            </div>
        </div>

        <div class="control-group">
            <form:label class="control-label" path="purpose">
                <spring:message code="views.reservationRequest.purpose"/>:
            </form:label>
            <div class="controls">
                <form:select path="purpose">
                    <form:option value="SCIENCE"><spring:message code="views.reservationRequest.purpose.science"/></form:option>
                    <form:option value="EDUCATION"><spring:message code="views.reservationRequest.purpose.education"/></form:option>
                </form:select>
            </div>
        </div>

        <div class="control-group">
            <form:label class="control-label" path="description">
                <spring:message code="views.reservationRequest.description"/>:
            </form:label>
            <div class="controls">
                <form:input path="description" cssErrorClass="error"/>
                <form:errors path="description" cssClass="error"/>
            </div>
        </div>

        <div class="control-group" ng-show="type == 'ROOM' && technology == 'H323_SIP'" class="hide">
            <form:label class="control-label" path="roomPin">
                <spring:message code="views.reservationRequest.specification.room.pin"/>:
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
