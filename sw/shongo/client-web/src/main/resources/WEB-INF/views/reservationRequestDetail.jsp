<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%--
  -- Page for displaying details about a single reservation request.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="advancedUserInterface" value="${sessionScope.user.advancedUserInterface}"/>
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
    <security:accesscontrollist hasPermission="PROVIDE_RESERVATION_REQUEST"
                                domainObject="${reservationRequest}" var="isProvidable"/>
</c:if>

<c:if test="${reservationRequest.allocationState != 'ALLOCATED' || reservationRequest.room == null || (!reservationRequest.room.state.started && reservationRequest.room.state != 'NOT_STARTED')}">
    <c:set var="isProvidable" value="${false}"/>
</c:if>

<script type="text/javascript">
    angular.module('jsp:reservationRequestDetail', ['tag:expandableBlock', 'tag:reservationRequestDetail', 'ngPagination']);
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
    </c:choose>
    <c:choose>
        <c:when test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
            <spring:message code="views.reservationRequestDetail.title.forRoom"
                            arguments="${reservationRequest.roomName}"/>
        </c:when>
        <c:when test="${reservationRequest.specificationType == 'ADHOC_ROOM'}">
            <spring:message code="views.reservationRequestDetail.title.forRoom.adhoc" var="adhocRoomName"/>
            <spring:message code="views.reservationRequestDetail.title.forRoom" arguments="${adhocRoomName}"/>
        </c:when>
    </c:choose>
</h1>

<div ng-app="jsp:reservationRequestDetail">

    <%-- What do you want to do? --%>
    <c:if test="${isProvidable}">
        <tag:url var="createPermanentRoomCapacityUrl" value="<%= ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY %>">
            <tag:param name="permanentRoom" value="${reservationRequest.id}"/>
            <tag:param name="back-url" value="${requestScope.requestUrl}"/>
        </tag:url>
    </c:if>
    <tag:expandableBlock name="actions" expandable="${advancedUserInterface}" expandCode="views.select.action" cssClass="actions">
        <span><spring:message code="views.select.action"/></span>
        <ul>
            <c:if test="${createPermanentRoomCapacityUrl != null}">
                <li>
                    <c:choose >
                        <c:when test="${reservationRequest.allocationState == 'ALLOCATED'}">
                            <a href="${createPermanentRoomCapacityUrl}" tabindex="1">
                                <spring:message code="views.reservationRequestDetail.action.createPermanentRoomCapacity"/>
                            </a>
                        </c:when>
                        <c:otherwise>
                            <span class="disabled">
                                <spring:message code="views.reservationRequestDetail.action.createPermanentRoomCapacity"/>
                            </span>
                        </c:otherwise>
                    </c:choose>
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
                    <th><spring:message code="views.reservationRequest.state"/></th>
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
    <tag:reservationRequestDetail reservationRequest="${reservationRequest}" detailUrl="${reservationRequestDetailUrl}" isActive="${isActive}"/>

    <%-- User roles --%>
    <hr/>
    <h2><spring:message code="views.reservationRequest.userRoles"/></h2>
    <tag:url var="aclUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_ACL %>">
        <tag:param name="reservationRequestId" value=":id"/>
    </tag:url>
    <tag:url var="aclCreateUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_ACL_CREATE %>">
        <tag:param name="reservationRequestId" value="${reservationRequest.id}"/>
    </tag:url>
    <tag:url var="aclDeleteUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_ACL_DELETE %>">
        <tag:param name="reservationRequestId" value="${reservationRequest.id}"/>
    </tag:url>
    <tag:userRoleList dataUrl="${aclUrl}" dataUrlParameters="id: '${reservationRequest.id}'"
                      isWritable="${isWritable}" createUrl="${aclCreateUrl}" deleteUrl="${aclDeleteUrl}"/>

    <c:if test="${isActive}">

        <%-- Periodic events --%>
        <c:if test="${reservationRequest.periodicityType != 'NONE'}">
            <hr/>
            <tag:reservationRequestChildren detailUrl="${reservationRequestDetailUrl}"/>
        </c:if>

        <%-- Permanent room capacities --%>
        <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
            <hr/>
            <c:if test="${isProvidable}">
                <tag:url var="createUsageUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_CREATE %>">
                    <tag:param name="specificationType" value="PERMANENT_ROOM_CAPACITY"/>
                    <tag:param name="permanentRoom" value="${reservationRequest.id}"/>
                </tag:url>
            </c:if>
            <tag:reservationRequestUsages detailUrl="${reservationRequestDetailUrl}" createUrl="${createUsageUrl}"/>
        </c:if>

    </c:if>

</div>

<div class="pull-right">
    <a class="btn btn-primary" href="${backUrl}" tabindex="1">
        <spring:message code="views.button.back"/>
    </a>
    <a class="btn" href="javascript: location.reload();" tabindex="1">
        <spring:message code="views.button.refresh"/>
    </a>
    <c:if test="${isWritable}">
        <c:if test="${advancedUserInterface}">
            <c:choose>
                <c:when test="${reservationRequest.state == 'ALLOCATED_FINISHED'}">
                    <a class="btn" href="${reservationRequestDuplicateUrl}" tabindex="1">
                        <spring:message code="views.button.duplicate"/>
                    </a>
                </c:when>
                <c:otherwise>
                    <a class="btn" href="${reservationRequestModifyUrl}" tabindex="1">
                        <spring:message code="views.button.modify"/>
                    </a>
                </c:otherwise>
            </c:choose>
        </c:if>
        <a class="btn" href="${reservationRequestDeleteUrl}" tabindex="1">
            <spring:message code="views.button.delete"/>
        </a>
    </c:if>
</div>