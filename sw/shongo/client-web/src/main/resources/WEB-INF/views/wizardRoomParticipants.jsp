<%--
  -- Wizard page for managing participants.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="tabIndex" value="1"/>

<tag:url var="createUrl" value="${createUrl}"/>
<tag:url var="modifyUrl" value="${modifyUrl}"/>
<tag:url var="deleteUrl" value="${deleteUrl}"/>

<script type="text/javascript">
    var module = angular.module('jsp:wizardRoomParticipants', ['ngTooltip']);

    /**
     * Store reservation request form and redirect to given {@code url}.
     *
     * @param url
     */
    window.redirect = function(url) {
        $.post('<%= ClientWebUrl.WIZARD_UPDATE %>',$('#reservationRequest').serialize(), function(){
            window.location.href = url;
        });
    };
</script>

<h1><spring:message code="views.wizard.room.participants"/></h1>

<div ng-app="jsp:wizardRoomParticipants" ng-init="roomParticipantNotificationEnabled = ${reservationRequest.roomParticipantNotificationEnabled}">

    <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
        <form:form class="form-horizontal" commandName="reservationRequest" method="post">
            <fieldset>
                <legend>
                    <spring:message code="views.wizard.room.participants.notification"/>
                    <form:checkbox id="roomParticipantNotificationEnabled" path="roomParticipantNotificationEnabled" tabindex="${tabIndex}" ng-model="roomParticipantNotificationEnabled"/>
                    <tag:help><spring:message code="views.wizard.room.participants.notification.help"/></tag:help>
                </legend>
                <div class="control-group">
                    <form:label class="control-label" path="roomMeetingName">
                        <spring:message code="views.reservationRequest.specification.roomMeetingName"/>:
                    </form:label>
                    <div class="controls">
                        <form:input path="roomMeetingName" cssErrorClass="error" tabindex="${tabIndex}" ng-disabled="!roomParticipantNotificationEnabled"/>
                        <form:errors path="roomMeetingName" cssClass="error"/>
                    </div>
                </div>
            </fieldset>
        </form:form>
    </c:if>

    <legend><spring:message code="views.wizard.room.participants.title"/></legend>

    <p><spring:message code="views.wizard.room.participants.help"/></p>

    <tag:participantList data="${reservationRequest.roomParticipants}"
                         createUrl="javascript: redirect('${createUrl}')"
                         modifyUrl="javascript: redirect${modifyUrl}')"
                         deleteUrl="javascript: redirect${deleteUrl}')"/>

    <hr/>

</div>