<%--
  -- Page for listing reservation requests for current user.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="advancedUserInterface" value="${sessionScope.SHONGO_USER.advancedUserInterface}"/>

<tag:url var="listDataUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_LIST_DATA %>"/>
<tag:url var="detailUrl" value="<%= ClientWebUrl.DETAIL_VIEW %>">
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="reservationRequestModifyUrl" value="<%= ClientWebUrl.WIZARD_MODIFY %>">
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="reservationRequestDuplicateUrl" value="<%= ClientWebUrl.WIZARD_DUPLICATE %>">
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="reservationRequestDeleteUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DELETE %>">
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="createRoomUrl" value="<%= ClientWebUrl.WIZARD_ROOM %>">
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<security:authorize access="hasPermission(RESERVATION)">
    <tag:url var="createPermanentRoomUrl" value="<%= ClientWebUrl.WIZARD_ROOM_PERMANENT %>"/>
    <tag:url var="createAdhocRoomUrl" value="<%= ClientWebUrl.WIZARD_ROOM_ADHOC %>"/>
</security:authorize>
<tag:url var="helpUrl" value="<%= ClientWebUrl.HELP %>"/>

<script type="text/javascript">
    var module = angular.module('jsp:reservationRequestList', ['ngApplication', 'tag:reservationRequestList', 'ngTooltip', 'ngCookies', 'ngSanitize']);
</script>

<div ng-app="jsp:reservationRequestList" ng-controller="ReadyController">

    <%-- What do you want to do? --%>
    <security:authorize access="hasPermission(RESERVATION)">
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
    </security:authorize>

    <%-- List of reservation requests --%>
    <div class="spinner" ng-hide="ready || errorContent"></div>
    <span ng-controller="HtmlController" ng-show="!ready && errorContent" ng-bind-html="html(errorContent)"></span>

    <div ng-show="ready">

        <tag:reservationRequestList name="permanent" specificationType="PERMANENT_ROOM" detailUrl="${detailUrl}"
                                    detailed="true" createUrl="${createPermanentRoomUrl}" modifyUrl="${reservationRequestModifyUrl}"
                                    duplicateUrl="${reservationRequestDuplicateUrl}" deleteUrl="${reservationRequestDeleteUrl}">
            <h2>
                <spring:message code="views.reservationRequestList.permanentRooms"/>
                <tag:help selectable="true">
                    <p><spring:message code="views.help.roomType.PERMANENT_ROOM.description"/></p>
                    <a class="btn btn-success" href="${helpUrl}#permanent-room" target="_blank">
                        <spring:message code="views.help.roomType.display"/>
                    </a>
                </tag:help>
            </h2>
        </tag:reservationRequestList>

        <hr/>

        <tag:reservationRequestList name="adhoc" specificationType="ADHOC_ROOM" detailUrl="${detailUrl}"
                                    detailed="true" createUrl="${createAdhocRoomUrl}" modifyUrl="${reservationRequestModifyUrl}"
                                    duplicateUrl="${reservationRequestDuplicateUrl}" deleteUrl="${reservationRequestDeleteUrl}">
            <h2>
                <spring:message code="views.reservationRequestList.adhocRooms"/>
                <tag:help selectable="true">
                    <p><spring:message code="views.help.roomType.ADHOC_ROOM.description"/></p>
                    <a class="btn btn-success" href="${helpUrl}#adhoc-room" target="_blank">
                        <spring:message code="views.help.roomType.display"/>
                    </a>
                </tag:help>
            </h2>
        </tag:reservationRequestList>

    </div>
</div>