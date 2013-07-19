<%--
  -- Wizard page for managing user roles for a new room.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="entityType"><%= cz.cesnet.shongo.controller.EntityType.RESERVATION_REQUEST %></c:set>
<c:set var="cancelUrl">
    ${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.WIZARD_CREATE_ROOM_ROLES %>
</c:set>

<h1><spring:message code="views.wizard.createRoom.role.add"/></h1>
<hr/>
<tag:userRoleForm entityType="${entityType}" confirmTitle="views.button.add" cancelUrl="${cancelUrl}"/>
<hr/>