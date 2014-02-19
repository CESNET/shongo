<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%--
  -- Page for displaying details about a single reservation request.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="advancedUserInterface" value="${sessionScope.SHONGO_USER.advancedUserInterface}"/>
<c:set var="backUrl"><%= ClientWebUrl.RESERVATION_REQUEST_LIST %></c:set>

<tag:url var="backUrl" value="${requestScope.backUrl.getUrl(backUrl)}"/>
<tag:url var="detailUrl" value="<%= ClientWebUrl.DETAIL_VIEW %>"/>

<tag:url var="reservationRequestModifyUrl" value="<%= ClientWebUrl.WIZARD_MODIFY %>">
    <tag:param name="reservationRequestId" value="${reservationRequest.id}"/>
</tag:url>
<tag:url var="reservationRequestDuplicateUrl" value="<%= ClientWebUrl.WIZARD_DUPLICATE %>">
    <tag:param name="reservationRequestId" value="${reservationRequest.id}"/>
</tag:url>
<tag:url var="reservationRequestDeleteUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DELETE %>">
    <tag:param name="reservationRequestId" value="${reservationRequest.id}"/>
</tag:url>

<c:if test="${isActive && empty reservationRequest.parentReservationRequestId}">
    <security:accesscontrollist hasPermission="WRITE" domainObject="${reservationRequest}" var="isWritable"/>

    <security:authorize access="hasPermission(RESERVATION)">
        <security:accesscontrollist hasPermission="PROVIDE_RESERVATION_REQUEST"
                                    domainObject="${reservationRequest}" var="canCreatePermanentRoomCapacity"/>
    </security:authorize>
</c:if>

<c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
    <c:set var="canCreatePermanentRoomCapacity" value="${false}"/>
</c:if>

<script type="text/javascript">
    var module = angular.module('jsp:reservationRequestDetail', ['ngApplication', 'tag:expandableBlock', 'tag:reservationRequestDetail', 'ngPagination']);
</script>

<%-- Page title --%>
<h1>
    <c:choose>
        <c:when test="${not empty reservationRequest.parentReservationRequestId}">
            <spring:message code="views.reservationRequestDetail.title.child"/>
        </c:when>
        <c:otherwise>
            <spring:message code="views.reservationRequestDetail.title"/>
        </c:otherwise>
    </c:choose>&nbsp;<%--
    --%><spring:message code="views.reservationRequest.for.${reservationRequest.specificationType}"
                    arguments="${reservationRequest.roomName}"/>
</h1>

<div ng-app="jsp:reservationRequestDetail">





    <%-- Detail of request --%>
    <c:if test="${isWritable}">
        <tag:url var="modifyUserRolesUrl" value="<%= ClientWebUrl.DETAIL_USER_ROLES_VIEW %>">
            <tag:param name="objectId" value="${reservationRequest.id}"/>
            <tag:param name="back-url" value="${requestUrl}"/>
        </tag:url>
    </c:if>
    <tag:reservationRequestDetail reservationRequest="${reservationRequest}" detailUrl="${detailUrl}" isActive="${isActive}" modifyUserRolesUrl="${modifyUserRolesUrl}"/>

    <c:if test="${isActive}">

        <%-- Periodic events --%>
        <c:if test="${reservationRequest.periodicityType != 'NONE'}">
            <hr/>
            <tag:reservationRequestChildren detailUrl="${detailUrl}"/>
        </c:if>

        <%-- Permanent room capacities --%>
        <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
            <hr/>
            <c:if test="${canCreatePermanentRoomCapacity}">
                <tag:url var="createUsageUrl" value="<%= ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY %>">
                    <tag:param name="permanentRoom" value="${reservationRequest.id}"/>
                    <tag:param name="back-url" value="${requestUrl}"/>
                </tag:url>
                <c:set var="createUsageWhen" value="$child.allocationState.code == 'ALLOCATED' && ($child.roomState.started || $child.roomState.code == 'NOT_STARTED')"/>
            </c:if>
            <div class="table-actions-left">
                <tag:reservationRequestUsages detailUrl="${detailUrl}" createUrl="${createUsageUrl}" createWhen="${createUsageWhen}"/>
            </div>
        </c:if>

    </c:if>

    <div class="table-actions pull-right">
        <a class="btn btn-primary" href="${backUrl}" tabindex="1">
            <spring:message code="views.button.back"/>
        </a>
        <a class="btn" href="javascript: location.reload();" tabindex="1">
            <spring:message code="views.button.refresh"/>
        </a>
        <c:if test="${isWritable}">
            <c:if test="${advancedUserInterface}">
                <span ng-switch on="$child.state.code == 'ALLOCATED_FINISHED'">
                    <a ng-switch-when="true" class="btn" href="${reservationRequestDuplicateUrl}" tabindex="1">
                        <spring:message code="views.button.duplicate"/>
                    </a>
                    <a ng-switch-when="false" class="btn" href="${reservationRequestModifyUrl}" tabindex="1">
                        <spring:message code="views.button.modify"/>
                    </a>
                </span>
            </c:if>
            <a class="btn" href="${reservationRequestDeleteUrl}" tabindex="1">
                <spring:message code="views.button.delete"/>
            </a>
        </c:if>
    </div>

</div>