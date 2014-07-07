<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<security:accesscontrollist hasPermission="WRITE" domainObject="${objectId}" var="isWritable"/>

<h2><spring:message code="views.roomParticipantList.title"/></h2>

<tag:url var="participantDataUrl" value="<%= ClientWebUrl.DETAIL_PARTICIPANTS_DATA %>">
    <tag:param name="objectId" value=":id"/>
</tag:url>
<tag:url var="participantCreateUrl" value="<%= ClientWebUrl.DETAIL_PARTICIPANT_CREATE %>">
    <tag:param name="objectId" value="${objectId}"/>
    <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
</tag:url>
<tag:url var="participantModifyUrl" value="<%= ClientWebUrl.DETAIL_PARTICIPANT_MODIFY %>">
    <tag:param name="objectId" value="${objectId}"/>
    <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
</tag:url>
<tag:url var="participantDeleteUrl" value="<%= ClientWebUrl.DETAIL_PARTICIPANT_DELETE %>">
    <tag:param name="objectId" value="${objectId}"/>
    <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
</tag:url>
<tag:participantList dataUrl="${participantDataUrl}" dataUrlParameters="id: '${objectId}'"
                     createUrl="${participantCreateUrl}" modifyUrl="${participantModifyUrl}"
                     deleteUrl="${participantDeleteUrl}"
                     hideRole="${technology == 'H323_SIP'}" isWritable="${isWritable}">

    <c:choose>
        <c:when test="${type == 'PERMANENT_ROOM'}">
            <p><spring:message code="views.room.participants.help.${technology}.permanentRoom"/></p>
        </c:when>
        <c:when test="${not empty technology}">
            <p><spring:message code="views.room.participants.help.${technology}"/></p>
        </c:when>
        <c:otherwise>
            <p><spring:message code="views.room.participants.help"/></p>
        </c:otherwise>
    </c:choose>

</tag:participantList>