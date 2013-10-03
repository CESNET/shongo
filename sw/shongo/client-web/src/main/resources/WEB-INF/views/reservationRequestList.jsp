<%--
  -- Page for listing reservation requests for current user.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="advancedUserInterface" value="${sessionScope.user.advancedUserInterface}"/>

<tag:url var="listDataUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_LIST_DATA %>"/>
<tag:url var="reservationRequestDetailUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DETAIL %>">
    <tag:param name="reservationRequestId" value="{{reservationRequest.id}}" escape="false"/>
</tag:url>
<tag:url var="reservationRequestModifyUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_MODIFY %>">
    <tag:param name="reservationRequestId" value="{{reservationRequest.id}}" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="reservationRequestDuplicateUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_CREATE_DUPLICATE %>">
    <tag:param name="reservationRequestId" value="{{reservationRequest.id}}" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="reservationRequestDeleteUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DELETE %>">
    <tag:param name="reservationRequestId" value="{{reservationRequest.id}}" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="createRoomUrl" value="<%= ClientWebUrl.WIZARD_CREATE_ROOM %>">
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="createPermanentRoomUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_CREATE %>">
    <tag:param name="specificationType" value="PERMANENT_ROOM"/>
</tag:url>
<tag:url var="createAdhocRoomUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_CREATE %>">
    <tag:param name="specificationType" value="ADHOC_ROOM"/>
</tag:url>

<script type="text/javascript">
    angular.module('jsp:reservationRequestList', ['tag:expandableBlock', 'tag:reservationRequestList', 'ngTooltip', 'ngSanitize']);
</script>

<div ng-app="jsp:reservationRequestList" ng-controller="ReadyController">

    <%-- What do you want to do? --%>
    <tag:expandableBlock name="actions" expandable="${advancedUserInterface}" expandCode="views.select.action" cssClass="actions">
        <span><spring:message code="views.select.action"/></span>
        <ul>
            <li>
                <a href="${createRoomUrl}" tabindex="1">
                    <spring:message code="views.index.action.createRoom"/>
                </a>
            </li>
        </ul>
    </tag:expandableBlock>

    <%-- List of reservation requests --%>
    <div class="spinner" ng-hide="ready || errorContent"></div>
    <span ng-controller="HtmlController" ng-show="!ready && errorContent" ng-bind-html="html(errorContent)"></span>

    <div ng-show="ready">

        <tag:reservationRequestList name="permanent" specificationType="PERMANENT_ROOM" detailUrl="${reservationRequestDetailUrl}"
                                    detailed="true" createUrl="${createPermanentRoomUrl}" modifyUrl="${reservationRequestModifyUrl}"
                                    duplicateUrl="${reservationRequestDuplicateUrl}" deleteUrl="${reservationRequestDeleteUrl}">
            <h2>
                <spring:message code="views.reservationRequestList.permanentRooms"/>
                <tag:help>
                    <spring:message code="help.reservationRequest.specification.PERMANENT_ROOM"/>
                </tag:help>
            </h2>
        </tag:reservationRequestList>

        <hr/>

        <tag:reservationRequestList name="adhoc" specificationType="ADHOC_ROOM" detailUrl="${reservationRequestDetailUrl}"
                                    detailed="true" createUrl="${createAdhocRoomUrl}" modifyUrl="${reservationRequestModifyUrl}"
                                    duplicateUrl="${reservationRequestDuplicateUrl}" deleteUrl="${reservationRequestDeleteUrl}">
            <h2>
                <spring:message code="views.reservationRequestList.adhocRooms"/>
                <tag:help>
                    <spring:message code="help.reservationRequest.specification.ADHOC_ROOM"/>
                </tag:help>
            </h2>
        </tag:reservationRequestList>

    </div>
</div>