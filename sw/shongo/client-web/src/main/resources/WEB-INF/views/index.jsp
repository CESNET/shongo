<%--
  -- Main welcome page.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="loginUrl">${contextPath}<%= ClientWebUrl.LOGIN %></c:set>
<c:set var="createRoomUrl">
    ${contextPath}<%= ClientWebUrl.WIZARD_CREATE_ROOM %>
</c:set>
<c:set var="createPermanentRoomCapacityUrl">
    ${contextPath}<%= ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY %>?force=new
</c:set>
<c:set var="reservationRequestListUrl">${contextPath}<%= ClientWebUrl.RESERVATION_REQUEST_LIST %></c:set>

<h1>${title}</h1>
<p><spring:message code="views.index.welcome"/></p>
<p><spring:message code="views.index.suggestions" arguments="${configuration.contactEmail}"/></p>

<security:authorize access="!isAuthenticated()">
    <p><strong><spring:message code="views.index.login" arguments="${loginUrl}"/></strong></p>
</security:authorize>

<security:authorize access="isAuthenticated()">
    <script type="text/javascript">
        var module = angular.module('jsp:indexDashboard', ['ngPagination', 'ngTooltip', 'ngSanitize']);
    </script>

    <div ng-app="jsp:indexDashboard">

        <div class="actions">
            <span><spring:message code="views.wizard.select"/></span>
            <ul>
                <li>
                    <a href="${createRoomUrl}" tabindex="1">
                        <spring:message code="views.wizard.select.createRoom"/>
                    </a>
                </li>
                <li>
                    <a href="${createPermanentRoomCapacityUrl}" tabindex="1">
                        <spring:message code="views.wizard.select.createPermanentRoomCapacity"/>
                    </a>
                </li>
                <li>
                    <a href="${reservationRequestListUrl}" tabindex="1">
                        <spring:message code="views.index.dashboard.reservationRequestList"/>
                    </a>
                </li>
            </ul>
        </div>

    </div>
</security:authorize>