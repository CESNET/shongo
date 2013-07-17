<%--
  -- Wizard page for creating a new room.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="urlCreateAdhocRoom">
    ${contextPath}<%= ClientWebUrl.WIZARD_CREATE_ADHOC_ROOM %>
</c:set>
<c:set var="urlCreatePermanentRoom">
    ${contextPath}<%= ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM %>
</c:set>

<div class="actions">
    <span><spring:message code="views.wizard.createRoom"/></span>
    <ul>
        <li><a href="${urlCreateAdhocRoom}"><spring:message code="views.wizard.createRoom.adhoc"/></a></li>
        <li><a href="${urlCreatePermanentRoom}"><spring:message code="views.wizard.createRoom.permanent"/></a></li>
    </ul>
</div>