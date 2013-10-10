<%--
  -- Wizard page for setting of user role attributes.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="entityType"><%= cz.cesnet.shongo.controller.EntityType.RESERVATION_REQUEST %></c:set>
<tag:url var="cancelUrl" value="<%= ClientWebUrl.WIZARD_CREATE_ROOM_ROLES %>"/>

<script type="text/javascript">
    angular.module('jsp:wizardCreateRoomRole', ['tag:userRoleForm']);
</script>

<div ng-app="jsp:wizardCreateRoomRole">

    <h1><spring:message code="views.wizard.createRoom.role.add"/></h1>

    <hr/>

    <tag:userRoleForm entityType="${entityType}" confirmTitle="views.button.add" cancelUrl="${cancelUrl}"/>

    <hr/>

</div>