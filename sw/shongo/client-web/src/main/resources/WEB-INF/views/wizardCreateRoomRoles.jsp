<%--
  -- Wizard page for managing user roles for a new room.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="app" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="createRoleUrl">
    ${contextPath}<%= ClientWebUrl.WIZARD_CREATE_ROOM_ROLE_CREATE %>
</c:set>
<c:set var="deleteRoleUrl">
    ${contextPath}<%= ClientWebUrl.WIZARD_CREATE_ROOM_ROLE_DELETE %>
</c:set>

<h1><spring:message code="views.wizard.createRoom.roles"/></h1>
<hr/>
<app:userRoleList data="${reservationRequest.userRoles}" createUrl="${createRoleUrl}" deleteUrl="${deleteRoleUrl}"/>
<hr/>