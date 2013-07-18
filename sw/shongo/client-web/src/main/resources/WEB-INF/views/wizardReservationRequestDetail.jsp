<%--
  -- Wizard page for detail of reservation request.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="detailUrl">
    ${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_DETAIL %>
</c:set>

<h1><spring:message code="views.wizard.reservationRequestDetail"/></h1>

<script type="text/javascript">
    angular.module('jsp:wizardReservationRequestDetail', ['tag:reservationRequestDetail']);
</script>

<div ng-app="jsp:wizardReservationRequestDetail">

    <tag:reservationRequestDetail reservationRequest="${reservationRequest}" detailUrl="${detailUrl}"/>

</div>