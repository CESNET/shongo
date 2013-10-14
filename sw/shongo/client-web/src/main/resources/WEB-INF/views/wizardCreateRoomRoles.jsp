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
    angular.module('jsp:wizardCreateRoomRoles', ['ngTooltip']);
</script>

<div ng-app="jsp:wizardCreateRoomRoles">

    <h1><spring:message code="views.wizard.createRoom.roles"/></h1>

    <spring:message code="views.wizard.createRoom.roles.description"/>
    <tag:help>
        <strong><spring:message code="views.aclRecord.role.OWNER"/></strong>
        <p><spring:message code="views.aclRecord.roleHelp.OWNER"/></p>
        <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
            <strong><spring:message code="views.aclRecord.role.RESERVATION_REQUEST_USER"/></strong>
            <p><spring:message code="views.aclRecord.roleHelp.RESERVATION_REQUEST_USER"/></p>
        </c:if>
        <strong><spring:message code="views.aclRecord.role.READER"/></strong>
        <p><spring:message code="views.aclRecord.roleHelp.READER"/></p>
    </tag:help>

    <hr/>

    <tag:userRoleList data="${reservationRequest.userRoles}" createUrl="${createRoleUrl}" deleteUrl="${deleteRoleUrl}"/>

    <hr/>

</div>

