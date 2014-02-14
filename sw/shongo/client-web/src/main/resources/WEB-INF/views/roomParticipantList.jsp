<%--
  -- Page for configuration of room participants.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<script type="text/javascript">
    var module = angular.module('jsp:roomParticipantList', ['ngApplication', 'ngTooltip']);
</script>

<h1>
    <spring:message code="views.roomParticipantList.heading"/>&nbsp;<spring:message code="views.room.for.${room.type}" arguments="${room.name}"/>
</h1>

<div ng-app="jsp:roomParticipantList" class="table-actions-left">

    <c:choose>
        <c:when test="${room.type == 'PERMANENT_ROOM'}">
            <p><spring:message code="views.room.participants.help.${room.technology}.permanentRoom"/></p>
        </c:when>
        <c:otherwise>
            <p><spring:message code="views.room.participants.help.${room.technology}"/></p>
        </c:otherwise>
    </c:choose>
    <tag:url var="participantCreateUrl" value="<%= ClientWebUrl.ROOM_PARTICIPANT_CREATE %>">
        <tag:param name="roomId" value="${room.id}"/>
        <tag:param name="back-url" value="${requestUrl}"/>
    </tag:url>
    <tag:url var="participantModifyUrl" value="<%= ClientWebUrl.ROOM_PARTICIPANT_MODIFY %>">
        <tag:param name="back-url" value="${requestUrl}"/>
    </tag:url>
    <tag:url var="participantDeleteUrl" value="<%= ClientWebUrl.ROOM_PARTICIPANT_DELETE %>">
        <tag:param name="back-url" value="${requestUrl}"/>
    </tag:url>
    <tag:participantList data="${room.participants}"
                         createUrl="${participantCreateUrl}" modifyUrl="${participantModifyUrl}" deleteUrl="${participantDeleteUrl}"
                         urlParam="roomId" urlValue="roomId"  hideRole="${room.technology == 'H323_SIP'}"/>

</div>

<div class="table-actions pull-right">
    <tag:url var="backUrl" value="${requestScope.backUrl}"/>
    <a class="btn" href="${backUrl}"><spring:message code="views.button.back"/></a>
</div>