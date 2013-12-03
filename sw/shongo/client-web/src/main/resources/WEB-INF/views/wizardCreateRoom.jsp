<%--
  -- Wizard page for creating a new room.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<tag:url var="createAdhocRoomUrl" value="<%= ClientWebUrl.WIZARD_ADHOC_ROOM %>"/>
<tag:url var="createPermanentRoomUrl" value="<%= ClientWebUrl.WIZARD_PERMANENT_ROOM %>"/>

<div class="actions">
    <span><spring:message code="views.wizard.createRoom"/></span>
    <ul>
        <li>
            <a href="${createAdhocRoomUrl}" tabindex="1"><spring:message code="views.wizard.createRoom.adhoc"/></a>
            <tag:helpRoomType roomType="ADHOC_ROOM"/>
        </li>
        <li>
            <a href="${createPermanentRoomUrl}" tabindex="1"><spring:message code="views.wizard.createRoom.permanent"/></a>
            <tag:helpRoomType roomType="PERMANENT_ROOM"/>
        </li>
    </ul>
</div>