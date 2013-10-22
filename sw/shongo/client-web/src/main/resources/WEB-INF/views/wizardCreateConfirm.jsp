<%--
  -- Wizard page for confirmation of a new room or new capacity for permanent room.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<h1><spring:message code="views.wizard.confirmation"/></h1>

<hr/>

<div ng-app="tag:reservationRequestDetail">

    <h2><spring:message code="views.wizard.confirmation.question"/></h2>

    <tag:reservationRequestDetail reservationRequest="${reservationRequest}"
                                  detailUrl="<%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_DETAIL %>"
                                  isActive="false"/>
    &nbsp;
    <p><spring:message code="views.wizard.confirmation.chooseFinish"/></p>

</div>

<hr/>