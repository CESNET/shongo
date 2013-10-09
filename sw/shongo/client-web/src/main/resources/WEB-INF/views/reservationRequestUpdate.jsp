<%--
  -- Page for creation/modification of a reservation request.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<tiles:importAttribute />
<c:set var="cancelUrl"><%= ClientWebUrl.RESERVATION_REQUEST_LIST %></c:set>
<tag:url var="cancelUrl" value="${requestScope.backUrl.getUrl(backUrl)}"/>

<script type="text/javascript">
    angular.module('jsp:reservationRequestUpdate', ['tag:reservationRequestForm']);
</script>

<div ng-app="jsp:reservationRequestUpdate">

    <h1>
        <spring:message code="${title}"/>
        <spring:message code="views.reservationRequestUpdate.type.${reservationRequest.specificationType}"/>
    </h1>

    <tag:reservationRequestForm confirmTitle="${confirmTitle}" cancelUrl="${cancelUrl}"
                                permanentRooms="${permanentRooms}"/>

</div>
