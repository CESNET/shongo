<%--
  -- Page for creation/modification of a reservation request.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="entityType"><%= cz.cesnet.shongo.controller.EntityType.RESERVATION_REQUEST %></c:set>

<tag:url var="cancelUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DETAIL %>">
    <tag:param name="reservationRequestId" value="${userRole.entityId}"/>
</tag:url>

<tag:userRoleForm entityType="${entityType}" confirmTitle="views.button.add" cancelUrl="${cancelUrl}"/>
