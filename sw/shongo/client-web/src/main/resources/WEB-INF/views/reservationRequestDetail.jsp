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
<tag:url var="reservationRequestDetailUrl" value="<%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_DETAIL %>"/>

<tag:url var="reservationRequestModifyUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_MODIFY %>">
    <tag:param name="reservationRequestId" value="${reservationRequest.id}"/>
</tag:url>
<tag:url var="reservationRequestDuplicateUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_CREATE_DUPLICATE %>">
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

    <%-- What do you want to do? --%>
    <tag:expandableBlock name="actions" expandable="${advancedUserInterface}" expandCode="views.select.action" cssClass="actions">
        <span><spring:message code="views.select.action"/></span>
        <ul>
            <c:if test="${canCreatePermanentRoomCapacity}">
                <tag:url var="createPermanentRoomCapacityUrl" value="<%= ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY %>">
                    <tag:param name="permanentRoom" value="${reservationRequest.id}"/>
                    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
                </tag:url>
                <li ng-switch on="$child.allocationState.code == 'ALLOCATED' && ($child.roomState.started || $child.roomState.code == 'NOT_STARTED')">
                    <a ng-switch-when="true" href="${createPermanentRoomCapacityUrl}" tabindex="1">
                        <spring:message code="views.reservationRequestDetail.action.createPermanentRoomCapacity"/>
                    </a>
                    <span ng-switch-when="false" class="disabled">
                        <spring:message code="views.reservationRequestDetail.action.createPermanentRoomCapacity"/>
                    </span>
                </li>
            </c:if>
            <li>
                <a href="javascript: location.reload();"  tabindex="1">
                    <spring:message code="views.reservationRequestDetail.action.refresh"/>
                </a>
            </li>
            <c:if test="${isWritable}">
                <li>
                    <tag:url var="deleteUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DELETE %>">
                        <tag:param name="reservationRequestId" value="${reservationRequest.id}"/>
                    </tag:url>
                    <a href="${reservationRequestDeleteUrl}" tabindex="1"><spring:message code="views.reservationRequestDetail.action.delete"/></a>
                </li>
            </c:if>
        </ul>
    </tag:expandableBlock>

    <%-- History --%>
    <c:if test="${history != null}">
        <div class="bordered jspReservationRequestDetailHistory">
            <h2><spring:message code="views.reservationRequestDetail.history"/></h2>
            <table class="table table-striped table-hover">
                <thead>
                <tr>
                    <th><spring:message code="views.reservationRequest.dateTime"/></th>
                    <th><spring:message code="views.reservationRequest.user"/></th>
                    <th><spring:message code="views.reservationRequest.type"/></th>
                    <c:if test="${reservationRequest.state != null}">
                        <th><spring:message code="views.reservationRequest.state"/></th>
                    </c:if>
                    <th><spring:message code="views.list.action"/></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${history}" var="historyItem" varStatus="status">
                    <c:set var="rowClass" value=""/>
                    <c:choose>
                        <c:when test="${historyItem.selected}">
                            <tr class="selected">
                        </c:when>
                        <c:otherwise>
                            <tr>
                        </c:otherwise>
                    </c:choose>
                    <td><tag:format value="${historyItem.dateTime}" styleShort="true"/></td>
                    <td>${historyItem.user}</td>
                    <td><spring:message code="views.reservationRequest.type.${historyItem.type}"/></td>
                    <c:if test="${reservationRequest.state != null}">
                        <td class="reservation-request-state">
                            <c:choose>
                                <c:when test="${historyItem.selected}">
                                    <span class="{{$child.state.code}}">{{$child.state.label}}</span>
                                </c:when>
                                <c:otherwise>
                                    <c:if test="${historyItem.state != null}">
                                        <span class="${historyItem.state}"><spring:message code="views.reservationRequest.state.${reservationRequest.specificationType}.${historyItem.state}"/></span>
                                    </c:if>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </c:if>
                    <td>
                        <c:choose>
                            <c:when test="${historyItem.id != reservationRequest.id && historyItem.type != 'DELETED'}">
                                <spring:eval var="historyItemDetailUrl"
                                             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(reservationRequestDetailUrl, historyItem.id)"/>
                                <tag:listAction code="show" url="${historyItemDetailUrl}" tabindex="2"/>
                            </c:when>
                            <c:when test="${historyItem.selected}">(<spring:message code="views.list.selected"/>)</c:when>
                        </c:choose>
                        <c:if test="${historyItem.type == 'MODIFIED' && status.first}">
                            <tag:url var="historyItemRevertUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DETAIL_REVERT %>">
                                <tag:param name="reservationRequestId" value="${historyItem.id}"/>
                            </tag:url>
                            <span ng-show="$child.allocationState.code != 'ALLOCATED'">
                                | <tag:listAction code="revert" url="${historyItemRevertUrl}" tabindex="2"/>
                            </span>
                        </c:if>
                    </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>

    <%-- Detail of request --%>
    <c:if test="${isWritable}">
        <tag:url var="modifyUserRolesUrl" value="<%= ClientWebUrl.USER_ROLE_LIST %>">
            <tag:param name="objectId" value="${reservationRequest.id}"/>
            <tag:param name="back-url" value="${requestUrl}"/>
        </tag:url>
    </c:if>
    <tag:reservationRequestDetail reservationRequest="${reservationRequest}" detailUrl="${reservationRequestDetailUrl}" isActive="${isActive}" modifyUserRolesUrl="${modifyUserRolesUrl}"/>

    <c:if test="${isActive}">

        <%-- Periodic events --%>
        <c:if test="${reservationRequest.periodicityType != 'NONE'}">
            <hr/>
            <tag:reservationRequestChildren detailUrl="${reservationRequestDetailUrl}"/>
        </c:if>

        <%-- Permanent room capacities --%>
        <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
            <hr/>
            <c:if test="${canCreatePermanentRoomCapacity}">
                <tag:url var="createUsageUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_CREATE %>">
                    <tag:param name="specificationType" value="PERMANENT_ROOM_CAPACITY"/>
                    <tag:param name="permanentRoom" value="${reservationRequest.id}"/>
                    <tag:param name="back-url" value="${requestUrl}"/>
                </tag:url>
                <c:set var="createUsageWhen" value="$child.allocationState.code == 'ALLOCATED' && ($child.roomState.started || $child.roomState.code == 'NOT_STARTED')"/>
            </c:if>
            <div class="table-actions-left">
                <tag:reservationRequestUsages detailUrl="${reservationRequestDetailUrl}" createUrl="${createUsageUrl}" createWhen="${createUsageWhen}"/>
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