<%--
  -- Wizard page for setting of user role attributes.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ page import="cz.cesnet.shongo.controller.ObjectType" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="objectType"><%= ObjectType.RESERVATION_REQUEST %></c:set>
<tag:url var="cancelUrl" value="<%= ClientWebUrl.WIZARD_ROOM_ROLES %>"/>

<script type="text/javascript">
    var module = angular.module('jsp:wizardRoomRole', ['ngApplication', 'tag:userRoleForm']);
</script>

<div ng-app="jsp:wizardRoomRole">

    <h1><spring:message code="views.wizard.room.role.add"/></h1>

    <hr/>

    <tag:userRoleForm objectType="${objectType}" confirmTitle="views.button.add" cancelUrl="${cancelUrl}"/>

    <hr/>

</div>