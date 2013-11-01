<%--
  -- Page for configuration of room participants.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<script type="text/javascript">
    angular.module('jsp:roomParticipantList', ['ngTooltip']);
</script>

<h1>
    <spring:message code="views.roomParticipantList.heading"/>
    <spring:message code="views.room.for.${room.type}" arguments="${room.name}"/>
</h1>

<div ng-app="jsp:roomParticipantList">

    <p><spring:message code="views.room.participants.help"/></p>
    <tag:url var="participantModifyUrl" value="<%= ClientWebUrl.ROOM_PARTICIPANT_MODIFY %>">
        <tag:param name="back-url" value="${requestUrl}"/>
    </tag:url>
    <tag:url var="participantDeleteUrl" value="<%= ClientWebUrl.ROOM_PARTICIPANT_DELETE %>">
        <tag:param name="back-url" value="${requestUrl}"/>
    </tag:url>
    <tag:participantList data="${room.participants}"
                         modifyUrl="${participantModifyUrl}" deleteUrl="${participantDeleteUrl}"
                         urlParam="roomId" urlValue="roomId"/>
    <tag:url var="participantCreateUrl" value="<%= ClientWebUrl.ROOM_PARTICIPANT_CREATE %>">
        <tag:param name="roomId" value="${room.id}"/>
        <tag:param name="back-url" value="${requestUrl}"/>
    </tag:url>
    <a class="btn btn-primary" href="${participantCreateUrl}">
        <spring:message code="views.button.add"/>
    </a>

</div>

<div class="pull-right">
    <tag:url var="backUrl" value="${requestScope.backUrl}"/>
    <a class="btn" href="${backUrl}"><spring:message code="views.button.back"/></a>
</div>