<%--
  -- Page for creation/modification of participant for a reservation request.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<script type="text/javascript">
    var module = angular.module('jsp:reservationRequestParticipant', ['ngApplication', 'tag:participantForm']);
</script>

<div ng-app="jsp:reservationRequestParticipant">

    <c:choose>
        <c:when test="${empty participant.id}">
            <c:set var="title" value="views.participant.add"/>
            <c:set var="confirmTitle" value="views.button.add"/>
        </c:when>
        <c:otherwise>
            <c:set var="title" value="views.participant.modify"/>
            <c:set var="confirmTitle" value="views.button.modify"/>
        </c:otherwise>
    </c:choose>

    <h1><spring:message code="${title}"/></h1>

    <tag:participantForm confirmTitle="${confirmTitle}" hideRole="${technology == 'H323_SIP'}"/>

</div>
