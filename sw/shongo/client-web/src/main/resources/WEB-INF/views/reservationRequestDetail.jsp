<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%--
  -- Page for displaying details about a single reservation request.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="requestUrl"><%= request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) %></c:set>
<c:set var="advancedUserInterface" value="${sessionScope.user.advancedUserInterface}"/>

<c:set var="detailUrl">
    ${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_DETAIL %>
</c:set>
<c:set var="backUrl"><%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_LIST %></c:set>
<c:set var="backUrl">${contextPath}${requestScope.backUrl.getUrl(backUrl)}</c:set>
<spring:eval var="modifyUrl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestModify(contextPath, reservationRequest.id)"/>
<spring:eval var="deleteUrl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDelete(contextPath, reservationRequest.id)"/>

<c:if test="${isActive && empty reservationRequest.parentReservationRequestId}">
    <security:accesscontrollist hasPermission="WRITE" domainObject="${reservationRequest}" var="isWritable"/>
    <security:accesscontrollist hasPermission="PROVIDE_RESERVATION_REQUEST"
                                domainObject="${reservationRequest}" var="isProvidable"/>
</c:if>

<script type="text/javascript">
    angular.module('jsp:reservationRequestDetail', ['tag:reservationRequestDetail', 'ngPagination']);
</script>

<%-- What do you want to do? --%>
<c:if test="${!sessionScope.user.advancedUserInterface}">
    <c:if test="${isProvidable && reservationRequest.slot.containsNow()}">
        <spring:eval var="createPermanentRoomCapacityUrl"
                     expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getWizardCreatePermanentRoomCapacity(contextPath, requestUrl, reservationRequest.id)"/>
    </c:if>
    <div class="actions">
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
                    <spring:eval var="deleteUrl"
                                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDelete(contextPath, reservationRequest.id)"/>
                    <a href="${deleteUrl}" tabindex="1"><spring:message code="views.reservationRequestDetail.action.delete"/></a>
                </li>
            </c:if>
        </ul>
    </div>
</c:if>

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
            <c:forEach items="${history}" var="historyItem">
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
                    <c:if test="${historyItem.state != null}">
                        <span class="${historyItem.state}"><spring:message code="views.reservationRequest.state.${reservationRequest.specificationType}.${historyItem.state}"/></span>
                    </c:if>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${historyItem.id != reservationRequest.id && historyItem.type != 'DELETED'}">
                            <spring:eval var="historyItemDetailUrl"
                                         expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, historyItem.id)"/>
                            <tag:listAction code="show" url="${historyItemDetailUrl}" tabindex="2"/>
                        </c:when>
                        <c:when test="${historyItem.selected}">(<spring:message code="views.list.selected"/>)</c:when>
                    </c:choose>
                    <c:if test="${historyItem.isRevertible}">
                        <spring:eval var="historyItemRevertUrl"
                                     expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetailRevert(contextPath, historyItem.id)"/>
                        | <tag:listAction code="revert" url="${historyItemRevertUrl}" tabindex="2"/>
                    </c:if>
                </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</c:if>

<div ng-app="jsp:reservationRequestDetail">

    <%-- Detail of request --%>
    <tag:reservationRequestDetail reservationRequest="${reservationRequest}" detailUrl="${detailUrl}" isActive="${isActive}"/>

    <%-- User roles --%>
    <hr/>
    <h2><spring:message code="views.reservationRequest.userRoles"/></h2>
    <spring:eval var="aclUrl"
                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestAcl(contextPath, ':id')"/>
    <spring:eval var="aclCreateUrl"
                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestAclCreate(contextPath, reservationRequest.id)"/>
    <spring:eval var="aclDeleteUrl"
                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestAclDelete(contextPath, reservationRequest.id)"/>
    <tag:userRoleList dataUrl="${aclUrl}" dataUrlParameters="id: '${reservationRequest.id}'"
                      isWritable="${isWritable}" createUrl="${aclCreateUrl}" deleteUrl="${aclDeleteUrl}"/>

    <c:if test="${isActive}">

        <%-- Periodic events --%>
        <c:if test="${reservationRequest.periodicityType != 'NONE'}">
            <hr/>
            <tag:reservationRequestChildren detailUrl="${detailUrl}"/>
        </c:if>

        <%-- Permanent room capacities --%>
        <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
            <hr/>
            <c:if test="${isProvidable && reservationRequest.allocationState == 'ALLOCATED' && reservationRequest.slot.containsNow()}">
                <spring:eval var="usageCreateUrl"
                             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestCreatePermanentRoomCapacity(contextPath, reservationRequest.id)"/>
            </c:if>
            <tag:reservationRequestUsages detailUrl="${detailUrl}" createUrl="${usageCreateUrl}"/>
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
            <a class="btn" href="${modifyUrl}" tabindex="1">
                <spring:message code="views.button.modify"/>
            </a>
        </c:if>
        <a class="btn" href="${deleteUrl}" tabindex="1">
            <spring:message code="views.button.delete"/>
        </a>
    </c:if>
</div>