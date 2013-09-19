<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%--
  -- Main welcome page.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="urlLogin">${contextPath}<%= ClientWebUrl.LOGIN %></c:set>
<c:set var="wizardUrl">${contextPath}<%= ClientWebUrl.WIZARD %></c:set>
<c:set var="urlAdvanced">${contextPath}<%= ClientWebUrl.RESERVATION_REQUEST_LIST %></c:set>
<c:set var="urlRoomsData">${contextPath}<%= ClientWebUrl.ROOM_LIST_DATA %></c:set>
<c:set var="urlRoomUsages">${contextPath}<%= ClientWebUrl.ROOM_LIST_DATA %></c:set>

<h1>${title}</h1>
<p><spring:message code="views.index.welcome"/></p>
<p><spring:message code="views.index.suggestions" arguments="${configuration.contactEmail}"/></p>

<security:authorize access="!isAuthenticated()">
    <p><strong><spring:message code="views.index.login" arguments="${urlLogin}"/></strong></p>
</security:authorize>

<security:authorize access="isAuthenticated()">
    <script type="text/javascript">
        var module = angular.module('jsp:indexDashboard', ['ngPagination', 'ngTooltip', 'ngSanitize']);
    </script>

    <div ng-app="jsp:indexDashboard">

        <div class="actions">
            <span><spring:message code="views.wizard.select"/></span>
            <ul>
                <li><a href="${wizardUrl}" tabindex="1"><spring:message code="views.index.dashboard.startWizard"/></a></li>
                <li><a href="${urlAdvanced}" tabindex="1"><spring:message code="views.index.dashboard.startAdvanced"/></a></li>
            </ul>
        </div>

    </div>
</security:authorize>