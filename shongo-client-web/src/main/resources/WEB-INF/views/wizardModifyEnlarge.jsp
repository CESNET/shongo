<%--
  -- Wizard page for enlarging room capacity.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="tabIndex" value="1"/>

<script type="text/javascript">
    var module = angular.module('jsp:wizardModifyExtend', ['ngApplication', 'ngDateTime']);

    window.enlargeReservationRequest = function(participantCount) {
        var originalParticipantCount = parseInt($("#original\\.roomParticipantCount").val());
        var participantCountEdit = $("#roomParticipantCount");
        participantCountEdit.val(originalParticipantCount + participantCount);
        window.update();
    };

    window.update = function() {
        var originalParticipantCount = $("#original\\.roomParticipantCount").val();
        var participantCount = $("#roomParticipantCount").val();
        var diff = participantCount - originalParticipantCount;
        console.debug(diff);
        $(".participantCount").removeClass("btn-success");
        $(".participantCount").addClass("btn-default");
        if (diff > 0) {
            $(".participantCount" + diff).removeClass("btn-default");
            $(".participantCount" + diff).addClass("btn-success btn-active");
        }
    };

    $(function(){
        $("#roomParticipantCount").change(function(){
            window.update();
        });
        window.update();
    });
</script>

<spring:message code="views.specificationType.for.${reservationRequest.specificationType}" var="specificationType"/>
<h1><spring:message code="views.wizard.modifyEnlarge" arguments="${specificationType}"/></h1>

<hr/>

<form:form class="form-horizontal"
           commandName="reservationRequest"
           method="post"
           ng-controller="ReservationRequestFormController">

<fieldset>

    <spring:hasBindErrors name="reservationRequest">
        <div class="alert alert-danger"><spring:message code="views.wizard.error.failed"/></div>
    </spring:hasBindErrors>

    <div class="form-group">
        <form:label class="col-xs-3 control-label" path="id">
            <spring:message code="views.reservationRequest.identifier"/>:
        </form:label>
        <div class="col-xs-4">
            <form:input cssClass="form-control" path="id" readonly="true"/>
        </div>
    </div>

    <div class="form-group">
        <form:label class="col-xs-3 control-label" path="original.roomParticipantCount">
            <spring:message code="views.reservationRequest.specification.roomParticipantCount.old"/>:
        </form:label>
        <div class="col-xs-2">
            <form:input path="original.roomParticipantCount" cssClass="form-control" cssErrorClass="form-control error" readonly="true"/>
        </div>
    </div>

    <div class="form-group">
        <div class="col-xs-offset-3 col-xs-9">
            <c:forEach var="participantCount" items="1,2,3,4,5,6,7,8,9,10">
                <a class="btn btn-default participantCount participantCount${participantCount}" href="javascript: enlargeReservationRequest(${participantCount});" tabindex="${tabIndex}">+${participantCount}</a>
            </c:forEach>
        </div>
    </div>

    <div class="form-group">
        <form:label class="col-xs-3 control-label" path="roomParticipantCount">
            <spring:message code="views.reservationRequest.specification.roomParticipantCount.new"/>:
        </form:label>
        <div class="col-xs-2">
            <form:input path="roomParticipantCount" cssClass="form-control" cssErrorClass="form-control error" tabindex="${tabIndex}"/>
        </div>
        <div class="col-xs-offset-3 col-xs-9">
            <form:errors path="roomParticipantCount" cssClass="error"/>
        </div>
    </div>

</fieldset>

</form:form>

<hr/>