<%--
  -- Wizard page for managing user roles for a new room.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="app" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="cancelUrl">
    ${contextPath}<%= ClientWebUrl.WIZARD_CREATE_ROOM_ROLES %>
</c:set>

<h1>Create user role</h1>
<app:userRoleForm confirmTitle="views.button.create" cancelUrl="${cancelUrl}"/>