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
            <p><spring:message code="views.reservationRequest.specificationHelp.ADHOC_ROOM"/></p>
        </li>
        <li>
            <a href="${createPermanentRoomUrl}" tabindex="1"><spring:message code="views.wizard.createRoom.permanent"/></a>
            <p><spring:message code="views.reservationRequest.specificationHelp.PERMANENT_ROOM"/></p>
        </li>
    </ul>
</div>