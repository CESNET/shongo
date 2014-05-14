<%--
  -- Wizard page for extending room duration.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="tabIndex" value="1"/>

<script type="text/javascript">
    var module = angular.module('jsp:wizardModifyExtend', ['ngApplication', 'ngDateTime']);

    window.extendReservationRequest = function(minutes) {
        var originalEnd = moment($("#original\\.end").val());
        var endPicker = $("#end");
        var end = originalEnd.add('minutes', minutes);
        endPicker.val(end.format("YYYY-MM-DD HH:mm"));
        window.update();
    };

    window.update = function() {
        var originalEnd = moment($("#original\\.end").val());
        var end = moment($("#end").val());
        var minutes = end.diff(originalEnd) / (1000 * 60);
        $(".minutes").removeClass("btn-success");
        $(".minutes").addClass("btn-default");
        if (minutes > 0) {
            $(".minutes" + minutes).removeClass("btn-default");
            $(".minutes" + minutes).addClass("btn-success btn-active");
        }
    };

    $(function(){
        $("#end").change(function(){
            window.update();
        });
        window.update();
    });
</script>

<spring:message code="views.specificationType.for.${reservationRequest.specificationType}" var="specificationType"/>
<h1><spring:message code="views.wizard.modifyExtend" arguments="${specificationType}"/></h1>

<hr/>

<div ng-app="jsp:wizardModifyExtend">

<form:form class="form-horizontal"
           commandName="reservationRequest"
           method="post">

    <fieldset>

        <spring:hasBindErrors name="reservationRequest">
            <div class="alert alert-danger"><spring:message code="views.wizard.error.failed"/></div>
        </spring:hasBindErrors>

        <div class="form-group">
            <form:label class="col-xs-2 control-label" path="id">
                <spring:message code="views.reservationRequest.identifier"/>:
            </form:label>
            <div class="col-xs-4">
                <form:input cssClass="form-control" path="id" readonly="true"/>
            </div>
        </div>

        <div class="form-group">
            <form:label class="col-xs-2 control-label" path="start">
                <spring:message code="views.reservationRequest.start"/>:
            </form:label>
            <div class="col-xs-4">
                <form:input path="start" cssClass="form-control" readonly="true"/>
            </div>
        </div>

        <div class="form-group">
            <form:label class="col-xs-2 control-label" path="original.end">
                <spring:message code="views.reservationRequest.end.old"/>:
            </form:label>
            <div class="col-xs-4">
                <form:input path="original.end" cssClass="form-control" readonly="true"/>
            </div>
        </div>

        <div class="form-group">
            <div class="col-xs-offset-2 col-xs-10">
                <c:forEach var="minutes" items="15,30,45,60,120,180">
                    <spring:eval expression="T(org.joda.time.Period).parse('PT' + minutes + 'M').normalizedStandard()" var="duration"/>
                    <a class="btn btn-default minutes minutes${minutes}" href="javascript: extendReservationRequest(${minutes});" tabindex="${tabIndex}">+<tag:format value="${duration}"/></a>
                </c:forEach>
            </div>
        </div>

        <div class="form-group">
            <form:label class="col-xs-2 control-label" path="end">
                <spring:message code="views.reservationRequest.end.new"/>:
            </form:label>
            <div class="col-xs-4">
                <form:input path="end" cssClass="form-control" cssErrorClass="form-control error" date-time-picker="true" tabindex="${tabIndex}"/>
            </div>
            <div class="col-xs-offset-2 col-xs-10">
                <form:errors path="start" cssClass="error"/>
                <form:errors path="end" cssClass="error"/>
            </div>
        </div>

    </fieldset>

</form:form>

</div>

<hr/>