<%--
  -- Wizard page for enlarging room capacity.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

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
        $(".participantCount").addClass("btn-info");
        if (diff > 0) {
            $(".participantCount" + diff).removeClass("btn-info");
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
        <div class="alert alert-error"><spring:message code="views.wizard.error.failed"/></div>
    </spring:hasBindErrors>

    <div class="control-group">
        <form:label class="control-label" path="id">
            <spring:message code="views.reservationRequest.identifier"/>:
        </form:label>
        <div class="controls double-width">
            <form:input path="id" readonly="true" tabindex="${tabIndex}"/>
        </div>
    </div>

    <div class="control-group">
        <form:label class="control-label" path="original.roomParticipantCount">
            <spring:message code="views.reservationRequest.specification.roomParticipantCount.old"/>:
        </form:label>
        <div class="controls">
            <form:input path="original.roomParticipantCount" cssErrorClass="error" tabindex="${tabIndex}" readonly="true"/>
        </div>
    </div>

    <div class="control-group">
        <div class="controls">
            <c:forEach var="participantCount" items="1,2,3,4,5,6,7,8,9,10">
                <a class="btn btn-info participantCount participantCount${participantCount}" href="javascript: enlargeReservationRequest(${participantCount});">+${participantCount}</a>
            </c:forEach>
        </div>
    </div>

    <div class="control-group">
        <form:label class="control-label" path="roomParticipantCount">
            <spring:message code="views.reservationRequest.specification.roomParticipantCount.new"/>:
        </form:label>
        <div class="controls">
            <form:input path="roomParticipantCount" cssErrorClass="error" tabindex="${tabIndex}"/>
            <form:errors path="roomParticipantCount" cssClass="error"/>
        </div>
    </div>

</fieldset>

</form:form>

<hr/>