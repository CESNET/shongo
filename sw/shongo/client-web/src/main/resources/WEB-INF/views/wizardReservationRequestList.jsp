<%--
  -- Wizard page for listing reservation requests.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="detailUrl">
    ${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.WIZARD_RESERVATION_REQUEST_DETAIL %>
</c:set>
<c:set var="createUrl">
    ${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.WIZARD_CREATE_ROOM %>
</c:set>
<c:set var="deleteUrl">
    ${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.WIZARD_RESERVATION_REQUEST_DELETE %>
</c:set>

<div class="actions">
    <span><spring:message code="views.wizard.select"/></span>
    <ul>
        <li>
            <a href="${createUrl}" tabindex="1">
                <spring:message code="views.wizard.select.createRoom"/>
            </a>
        </li>
    </ul>
</div>

<div ng-app="tag:reservationRequestList">

    <tag:reservationRequestList specificationType="ADHOC_ROOM,PERMANENT_ROOM"
                                detailUrl="${detailUrl}" deleteUrl="${deleteUrl}">
        <h1><spring:message code="views.wizard.reservationRequestList"/></h1>
    </tag:reservationRequestList>

</div>