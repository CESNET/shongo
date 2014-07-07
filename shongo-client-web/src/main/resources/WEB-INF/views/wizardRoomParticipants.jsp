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
    var module = angular.module('jsp:wizardRoomParticipants', ['ngApplication', 'ngTooltip']);

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

<div class="jspWizardRoomParticipants" ng-app="jsp:wizardRoomParticipants"
     ng-init="roomParticipantNotificationEnabled = ${reservationRequest.roomParticipantNotificationEnabled}">

    <c:choose>
        <c:when test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
            <form:form class="form-horizontal" commandName="reservationRequest" method="post">
                <legend>
                    <spring:message code="views.wizard.room.participants.notification"/>&nbsp;<form:checkbox id="roomParticipantNotificationEnabled" path="roomParticipantNotificationEnabled" tabindex="${tabIndex}" ng-model="roomParticipantNotificationEnabled"/>
                    <tag:help><spring:message code="views.reservationRequest.specification.roomParticipantNotificationEnabled.help"/></tag:help>
                </legend>
                <div class="form-group">
                    <form:label class="col-xs-2 control-label" path="roomMeetingName">
                        <spring:message code="views.reservationRequest.specification.roomMeetingName"/>:
                    </form:label>
                    <div class="col-xs-4">
                        <form:input cssClass="form-control" cssErrorClass="form-control error"
                                    path="roomMeetingName" tabindex="${tabIndex}"
                                    ng-disabled="!roomParticipantNotificationEnabled"/>
                        <form:errors path="roomMeetingName" cssClass="error"/>
                    </div>
                </div>
                <div class="form-group">
                    <form:label class="col-xs-2 control-label" path="roomMeetingDescription">
                        <spring:message code="views.reservationRequest.specification.roomMeetingDescription"/>:
                    </form:label>
                    <div class="col-xs-6">
                        <form:textarea cssClass="form-control" cssErrorClass="form-control error"
                                       path="roomMeetingDescription" rows="3"
                                       tabindex="${tabIndex}" ng-disabled="!roomParticipantNotificationEnabled"/>
                        <form:errors path="roomMeetingDescription" cssClass="error"/>
                    </div>
                </div>
            </form:form>
        </c:when>
        <c:otherwise>
            <form:form class="form-horizontal" commandName="reservationRequest" method="post">
            </form:form>
        </c:otherwise>
    </c:choose>

    <legend><spring:message code="views.wizard.room.participants.title"/></legend>

    <c:choose>
        <c:when test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
            <p><spring:message code="views.room.participants.help.${reservationRequest.technology}.permanentRoom"/></p>
        </c:when>
        <c:when test="${not empty reservationRequest.technology}">
            <p><spring:message code="views.room.participants.help.${reservationRequest.technology}"/></p>
        </c:when>
        <c:otherwise>
            <p><spring:message code="views.room.participants.help"/></p>
        </c:otherwise>
    </c:choose>

    <tag:participantList data="${reservationRequest.roomParticipants}"
                         createUrl="javascript: redirect('${createUrl}')"
                         modifyUrl="javascript: redirect('${modifyUrl}')"
                         deleteUrl="javascript: redirect('${deleteUrl}')"
                         hideRole="${reservationRequest.technology == 'H323_SIP'}"/>

    <hr/>

</div>