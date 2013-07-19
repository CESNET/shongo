<%--
  -- Page for creation/modification of a reservation request.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="entityType"><%= cz.cesnet.shongo.controller.EntityType.RESERVATION_REQUEST %></c:set>
<spring:eval var="confirmUrl"
             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestAclCreateConfirm(contextPath, userRole.entityId)"/>
<spring:eval var="cancelUrl"
             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetail(contextPath, userRole.entityId)"/>

<tag:userRoleForm entityType="${entityType}"
                  confirmTitle="views.button.add" confirmUrl="${confirmUrl}" cancelUrl="${cancelUrl}"/>
