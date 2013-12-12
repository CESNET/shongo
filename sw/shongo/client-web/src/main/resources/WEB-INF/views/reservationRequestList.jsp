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
<tag:url var="reservationRequestDetailUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DETAIL %>"/>
<tag:url var="reservationRequestModifyUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_MODIFY %>">
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="reservationRequestDuplicateUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_CREATE_DUPLICATE %>">
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="reservationRequestDeleteUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DELETE %>">
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="createRoomUrl" value="<%= ClientWebUrl.WIZARD_ROOM %>">
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<security:authorize access="hasPermission(RESERVATION)">
    <tag:url var="createPermanentRoomUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_CREATE %>">
        <tag:param name="specificationType" value="PERMANENT_ROOM"/>
    </tag:url>
    <tag:url var="createAdhocRoomUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_CREATE %>">
        <tag:param name="specificationType" value="ADHOC_ROOM"/>
    </tag:url>
</security:authorize>
<tag:url var="helpUrl" value="<%= ClientWebUrl.HELP %>"/>

<script type="text/javascript">
    angular.module('jsp:reservationRequestList', ['tag:reservationRequestList', 'ngTooltip', 'ngCookies', 'ngSanitize']);
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

        <tag:reservationRequestList name="permanent" specificationType="PERMANENT_ROOM" detailUrl="${reservationRequestDetailUrl}"
                                    detailed="true" createUrl="${createPermanentRoomUrl}" modifyUrl="${reservationRequestModifyUrl}"
                                    duplicateUrl="${reservationRequestDuplicateUrl}" deleteUrl="${reservationRequestDeleteUrl}">
            <h2>
                <spring:message code="views.reservationRequestList.permanentRooms"/>&nbsp;
                <tag:help selectable="true">
                    <p><spring:message code="views.help.roomType.PERMANENT_ROOM.description"/></p>
                    <a class="btn btn-success" href="${helpUrl}#permanent-room" target="_blank">
                        <spring:message code="views.help.roomType.display"/>
                    </a>
                </tag:help>
            </h2>
        </tag:reservationRequestList>

        <hr/>

        <tag:reservationRequestList name="adhoc" specificationType="ADHOC_ROOM" detailUrl="${reservationRequestDetailUrl}"
                                    detailed="true" createUrl="${createAdhocRoomUrl}" modifyUrl="${reservationRequestModifyUrl}"
                                    duplicateUrl="${reservationRequestDuplicateUrl}" deleteUrl="${reservationRequestDeleteUrl}">
            <h2>
                <spring:message code="views.reservationRequestList.adhocRooms"/>&nbsp;
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