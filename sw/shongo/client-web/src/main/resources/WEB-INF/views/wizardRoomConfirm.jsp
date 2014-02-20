<%--
  -- Wizard page for confirmation of a new room or new capacity for permanent room.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<h1><spring:message code="views.wizard.confirmation"/></h1>

<hr/>

<script type="text/javascript">
    var module = angular.module('jsp:wizardRoomConfirm', ['ngApplication', 'tag:reservationRequestDetail']);
</script>

<div ng-app="jsp:wizardRoomConfirm">

    <c:choose>
        <c:when test="${not empty reservationRequest.id}">
            <h2><spring:message code="views.wizard.confirmation.question.modify"/></h2>
        </c:when>
        <c:otherwise>
            <h2><spring:message code="views.wizard.confirmation.question.create"/></h2>
        </c:otherwise>
    </c:choose>

    <tag:reservationRequestDetail reservationRequest="${reservationRequest}"
                                  detailUrl="<%= cz.cesnet.shongo.client.web.ClientWebUrl.DETAIL_VIEW %>"/>
    &nbsp;
    <p><spring:message code="views.wizard.confirmation.chooseFinish"/></p>

</div>

<hr/>