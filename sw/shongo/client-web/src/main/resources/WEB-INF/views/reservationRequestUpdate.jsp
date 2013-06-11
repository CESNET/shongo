<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<tiles:importAttribute/>

<script type="text/javascript">
    var module = angular.module('app', []);
    module.directive('dateTimePicker', function() {
        return {
            restrict: 'A',
            link: function postLink(scope, element, attrs, controller) {
                // Create date/time picker
                element.datetimepicker({

                    minuteStep: 2,
                    autoclose: true,
                    todayBtn: true,
                    todayHighlight: true
                });
                //element.attr("readonly", true);
                element.data("datetimepicker").setValue();

                // Create method for initializing "datetime" or "date" format
                var dateTimePicker = element.data("datetimepicker");
                dateTimePicker.setFormatDate = function() {
                    dateTimePicker.minView = $.fn.datetimepicker.DPGlobal.convertViewMode('month');
                    dateTimePicker.viewSelect = element.data("datetimepicker").minView;
                    dateTimePicker.setFormat("yyyy-mm-dd");
                    dateTimePicker.setValue();
                };
                dateTimePicker.setFormatDateTime = function() {
                    dateTimePicker.minView = $.fn.datetimepicker.DPGlobal.convertViewMode('hour');
                    dateTimePicker.setFormat("yyyy-mm-dd hh:ii");
                    dateTimePicker.setValue();
                };

                if ( attrs.format == "date") {
                    dateTimePicker.setFormatDate();
                }
                else {
                    dateTimePicker.setFormatDateTime();
                }
            }
        }
    });

</script>

<div ng-app="app">

<form:form class="form-horizontal"
           commandName="reservationRequest"
           action="${contextPath}${confirmUrl}"
           method="post"
           ng-controller="FormController">

    <script type="text/javascript">
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
                    <form:option value="H323_SIP">H.323/SIP</form:option>
                    <form:option value="ADOBE_CONNECT">Adobe Connect</form:option>
                </form:select>
            </div>
        </div>

        <div class="control-group" ng-show="type == 'ALIAS'" class="hide">
            <form:label class="control-label" path="alias.roomName">
                <spring:message code="views.reservationRequest.specification.alias.roomName"/>:
            </form:label>
            <div class="controls">
                <form:input path="alias.roomName" cssErrorClass="error"/>
                <form:errors path="alias.roomName" cssClass="error"/>
            </div>
        </div>

        <div class="control-group" ng-show="type == 'ROOM'" class="hide">
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

        <div class="control-group" ng-show="type == 'ROOM'" class="hide">
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
                <form:input path="start" cssErrorClass="error" date-time-picker="true"/>
                <form:errors path="start" cssClass="error"/>
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

        <div class="control-group" ng-show="type == 'ALIAS'" class="hide">
            <form:label class="control-label" path="end">
                <spring:message code="views.reservationRequest.end"/>:
            </form:label>
            <div class="controls">
                <form:input path="end" cssErrorClass="error" date-time-picker="true" format="date"/>
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

        <div class="control-group" ng-show="type == 'ROOM' && technology == 'H323_SIP'" class="hide">
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
            <spring:message code="${confirm}" var="confirm"/>
            <input class="btn btn-primary" type="submit" value="${confirm}"/>
            <a class="btn" href="${contextPath}/reservation-request"><spring:message code="views.button.cancel"/></a>
        </div>
    </div>

</form:form>

</div>
