<%--
  -- Wizard page for managing user roles for a new room.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<tag:url var="createRoleUrl" value="<%= ClientWebUrl.WIZARD_ROOM_ROLE_CREATE %>"/>
<tag:url var="deleteRoleUrl" value="<%= ClientWebUrl.WIZARD_ROOM_ROLE_DELETE %>"/>

<script type="text/javascript">
    var module = angular.module('jsp:wizardRoomRoles', ['ngApplication', 'ngTooltip']);
</script>

<div ng-app="jsp:wizardRoomRoles">

    <h1><spring:message code="views.wizard.room.roles"/></h1>

    <spring:message code="views.wizard.room.roles.description"/>
    <c:if test="${reservationRequest.technology == 'ADOBE_CONNECT'}">
        <br/>
        <spring:message code="views.wizard.room.roles.description.participants"/>
    </c:if>

    <hr/>

    <tag:userRoleList data="${reservationRequest.userRoles}" createUrl="${createRoleUrl}" deleteUrl="${deleteRoleUrl}"/>

    <hr/>

</div>

