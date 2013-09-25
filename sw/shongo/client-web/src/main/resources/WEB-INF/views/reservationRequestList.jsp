<%--
  -- Page for listing reservation requests for current user.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="requestUrl"><%= request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) %></c:set>

<c:set var="listDataUrl">${contextPath}<%= ClientWebUrl.RESERVATION_REQUEST_LIST_DATA %></c:set>
<spring:eval var="detailUrl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetail(contextPath, '{{reservationRequest.id}}')"/>
<spring:eval var="modifyUrl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestModify(contextPath, '{{reservationRequest.id}}') + '?back-url=' + requestUrl"/>
<spring:eval var="deleteUrl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDelete(contextPath, '{{reservationRequest.id}}') + '?back-url=' + requestUrl"/>

<script type="text/javascript">
    angular.module('jsp:reservationRequestList', ['ngPagination', 'ngTooltip']);
</script>

<%-- What do you want to do? --%>
<c:if test="${!sessionScope.user.advancedUserInterface}">
    <div class="actions">
        <span><spring:message code="views.select.action"/></span>
        <ul>
            <li>
                <c:set var="createRoomUrl">${contextPath}<%= ClientWebUrl.WIZARD_CREATE_ROOM %>?back-url=${requestUrl}</c:set>
                <a href="${createRoomUrl}" tabindex="1">
                    <spring:message code="views.index.action.createRoom"/>
                </a>
            </li>
        </ul>
    </div>
</c:if>

<%-- List of reservation requests --%>
<div ng-app="jsp:reservationRequestList" ng-controller="ReadyController">

    <div class="spinner" ng-hide="ready"></div>

    <div ng-show="ready">

        <spring:eval var="createUrl"
                     expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestCreate(contextPath, 'PERMANENT_ROOM')"/>

        <tag:reservationRequestList name="permanent" specificationType="PERMANENT_ROOM" detailUrl="${detailUrl}" detailed="true"
                                    createUrl="${createUrl}" modifyUrl="${modifyUrl}" deleteUrl="${deleteUrl}">
            <h2>
                <spring:message code="views.reservationRequestList.permanentRooms"/>
                <tag:help>
                    <spring:message code="help.reservationRequest.specification.PERMANENT_ROOM"/>
                </tag:help>
            </h2>
        </tag:reservationRequestList>

        <hr/>

        <spring:eval var="createUrl"
                     expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestCreate(contextPath, 'ADHOC_ROOM')"/>
        <tag:reservationRequestList name="adhoc" specificationType="ADHOC_ROOM" detailUrl="${detailUrl}" detailed="true"
                                    createUrl="${createUrl}" modifyUrl="${modifyUrl}" deleteUrl="${deleteUrl}">
            <h2>
                <spring:message code="views.reservationRequestList.adhocRooms"/>
                <tag:help>
                    <spring:message code="help.reservationRequest.specification.ADHOC_ROOM"/>
                </tag:help>
            </h2>
        </tag:reservationRequestList>

    </div>
</div>