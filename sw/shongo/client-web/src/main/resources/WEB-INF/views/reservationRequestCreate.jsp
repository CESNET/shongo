<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<script type="text/javascript">
    var module = angular.module('app', []);
    module.directive('bootstrapDateTimePicker', function() {
        return {
            restrict: 'A',
            link: function postLink(scope, element, attrs, controller) {
                var dtp = element.datetimepicker({
                    format: "yyyy-mm-dd hh:ii",
                    minuteStep: 1,
                    autoclose: true,
                    todayBtn: true,
                    todayHighlight: true
                });
                element.attr("readonly", true);
                element.data("datetimepicker").setValue();
            }
        }
    });
    module.directive('bootstrapDatePicker', function() {
        return {
            restrict: 'A',
            link: function postLink(scope, element, attrs, controller) {
                element.datetimepicker({
                    format: "yyyy-mm-dd",
                    minView: "month",
                    autoclose: true,
                    todayBtn: true,
                    todayHighlight: true
                });
                element.attr("readonly", true);
                element.data("datetimepicker").setValue();
            }
        }
    });



</script>

<div ng-app="app">

<form:form class="form-horizontal"
           commandName="reservationRequest"
           action="${contextPath}/reservation-request/create/confirmed"
           method="post"
           ng-controller="FormController">

    <script type="text/javascript">
        function FormController($scope) {
            $scope.value = function(value, defaultValue) {
                return ((value == null || value == '') ? defaultValue : value);
            };
            $scope.type = $scope.value('${param.type}', 'ALIAS');
            $scope.technology = $scope.value('${param.technology}', 'H323_SIP');
        }
    </script>

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

        <div class="control-group">
            <form:label class="control-label" path="id">
                <spring:message code="views.reservationRequest.identifier"/>:
            </form:label>
            <div class="controls">
                <form:input path="id" readonly="true"/>
            </div>
        </div>

        <div class="control-group">
            <form:label class="control-label" path="technology">
                <spring:message code="views.reservationRequest.technology"/>:
            </form:label>
            <div class="controls">
                <form:select path="technology" ng-model="technology">
                    <form:option value="H323_SIP">H.323/SIP</form:option>
                    <form:option value="ADOBE_CONNECT">Adobe Connect</form:option>
                </form:select>
            </div>
        </div>

        <div class="control-group" ng-show="type == 'ALIAS'">
            <form:label class="control-label" path="alias.roomName">
                <spring:message code="views.reservationRequest.specification.alias.roomName"/>:
            </form:label>
            <div class="controls">
                <form:input path="alias.roomName" cssErrorClass="error"/>
                <form:errors path="alias.roomName" cssClass="error"/>
            </div>
        </div>

        <div class="control-group" ng-show="type == 'ROOM'">
            <form:label class="control-label" path="room.alias">
                <spring:message code="views.reservationRequest.specification.room.alias"/>:
            </form:label>
            <div class="controls">
                <form:select path="room.alias">
                    <form:option value="ADHOC">Ad-Hoc</form:option>
                </form:select>
                <form:errors path="room.alias" cssClass="error"/>
            </div>
        </div>

        <div class="control-group" ng-show="type == 'ROOM'">
            <form:label class="control-label" path="room.participantCount">
                <spring:message code="views.reservationRequest.specification.room.participantCount"/>:
            </form:label>
            <div class="controls">
                <form:input path="room.participantCount" cssErrorClass="error"/>
                <form:errors path="room.participantCount" cssClass="error"/>
            </div>
        </div>

        <div class="control-group">
            <form:label class="control-label" path="start">
                <spring:message code="views.reservationRequest.start"/>:
            </form:label>
            <div class="controls">
                <form:input path="start" cssErrorClass="error" bootstrap-date-time-picker="true"/>
                <form:errors path="start" cssClass="error"/>
            </div>
        </div>

        <div class="control-group" ng-show="type == 'ROOM'">
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

        <div class="control-group" ng-show="type == 'ALIAS'">
            <form:label class="control-label" path="end">
                <spring:message code="views.reservationRequest.end"/>:
            </form:label>
            <div class="controls">
                <form:input path="end" cssErrorClass="error" bootstrap-date-picker="true"/>
                <form:errors path="end" cssClass="error"/>
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

        <div class="control-group" ng-show="type == 'ROOM' && technology == 'H323_SIP'">
            <form:label class="control-label" path="room.pin">
                <spring:message code="views.reservationRequest.specification.room.pin"/>:
            </form:label>
            <div class="controls">
                <form:input path="room.pin" cssErrorClass="error"/>
                <form:errors path="room.pin" cssClass="error"/>
            </div>
        </div>

    </fieldset>

    <div class="control-group">
        <div class="controls">
            <spring:message code="views.button.create" var="create"/>
            <input class="btn btn-primary" type="submit" value="${create}"/>
            <a class="btn" href="${contextPath}/reservation-request"><spring:message code="views.button.cancel"/></a>
        </div>
    </div>

</form:form>

</div>
