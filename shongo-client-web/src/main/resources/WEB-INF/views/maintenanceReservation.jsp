<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>


<script type="text/javascript">

    var module = angular.module('jsp:maintenanceReservation', ['ngTooltip', 'ngDateTime']);
    module.controller("MaintenanceReservationController", ['$scope', '$log', function ($scope, $log) {

    }])

</script>


<div ng-app="jsp:maintenanceReservation" ng-controller="MaintenanceReservationController">
    <form:form class="form-horizontal"
               id="maintenanceReservation"
               commandName="maintenanceReservation"
               method="post">

        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="resourceId">
                Pro zdroj:
            </form:label>
            <div class="col-xs-6">
                <div class="form-control-static">
                    <strong>${resourceName}</strong><c:out value=" (${resourceId})"></c:out>
                </div>
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

        <%--Description--%>
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

        <%--Start date and time--%>
        <div class="form-group">
            <label class="col-xs-3 control-label" path="start">
                <spring:message code="views.reservationRequest.start"/>:
            </label>
            <div class="col-xs-9 space-padding">
                <div class="col-xs-2">
                    <form:input cssClass="form-control" cssErrorClass="form-control error" path="startDate" date-picker="true" tabindex="${tabIndex}"/>
                </div>
                <div class="col-xs-2">
                    <form:input cssClass="form-control" cssErrorClass="form-control error" path="start" time-picker="true" data-show-inputs="false" data-minute-step="5" data-second-step="60" tabindex="${tabIndex}"/>
                </div>
            </div>
            <div class="col-xs-offset-3 col-xs-9">
                <form:errors path="startDate" cssClass="error"/>
                <form:errors path="start" cssClass="error"/>
            </div>
        </div>

        <%--End date and time--%>
        <div class="form-group">
            <label class="col-xs-3 control-label" path="start">
                <spring:message code="views.reservationRequest.end"/>:
            </label>
            <div class="col-xs-9 space-padding">
                <div class="col-xs-2">
                    <form:input cssClass="form-control" cssErrorClass="form-control error" path="endDate" date-picker="true" tabindex="${tabIndex}"/>
                </div>
                <div class="col-xs-2">
                    <form:input cssClass="form-control" cssErrorClass="form-control error" path="end" time-picker="true" data-show-inputs="false" data-minute-step="5" data-second-step="60" tabindex="${tabIndex}"/>
                </div>
            </div>
            <div class="col-xs-offset-3 col-xs-9">
                <form:errors path="endDate" cssClass="error"/>
                <form:errors path="end" cssClass="error"/>
            </div>
        </div>

<%--
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="durationCount">
                <spring:message code="views.reservationRequest.duration"/>:
            </form:label>
            <div class="col-xs-9 space-padding">
                <div class="col-xs-2">
                    <form:input cssClass="form-control" cssErrorClass="form-control error" path="durationCount" tabindex="${tabIndex}"/>
                </div>
                <div class="col-xs-2">
                    <form:select path="durationType" cssClass="form-control" tabindex="${tabIndex}">
                        <form:option value="MINUTE"><spring:message code="views.reservationRequest.duration.minutes"/></form:option>
                        <form:option value="HOUR"><spring:message code="views.reservationRequest.duration.hours"/></form:option>
                        <form:option value="DAY"><spring:message code="views.reservationRequest.duration.days"/></form:option>
                    </form:select>
                </div>
            </div>
            <div class="col-xs-offset-3 col-xs-9">
                <form:errors path="durationCount" cssClass="error"/>
            </div>
        </div>--%>



        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="priority">
                Priorita:
            </form:label>
            <div class="col-xs-9 space-padding">
                <div class="col-xs-2" >
                    <form:input cssClass="form-control" cssErrorClass="form-control error" path="priority" tabindex="${tabIndex}"/>
                </div>
            </div>
            <div class="col-xs-offset-3 col-xs-9">
                <p><span style="font-size:0.9em; color:#9b9b9b;">POZOR! Rezervace s vyšší prioritou přepíšou rezervace s nižší prioritou! (1 pro normálni rezervaci)</span></p>

                <form:errors path="priority" cssClass="error"/>
            </div>
        </div>
















    </form:form>

    <a class="btn btn-default pull-right" href="javascript: document.getElementById('maintenanceReservation').submit();">
        <spring:message code="views.button.create"/>
    </a>
<%--
    <button type="submit" form="maintenanceReservation" value="Submit">Submit</button>
--%>
</div>
