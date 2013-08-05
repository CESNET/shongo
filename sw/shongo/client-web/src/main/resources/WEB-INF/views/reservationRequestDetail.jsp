<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%--
  -- Page for displaying details about a single reservation request.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="detailUrl">
    ${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_DETAIL %>
</c:set>
<c:set var="backUrl">
    ${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_LIST %>
</c:set>
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

<%-- History --%>
<c:if test="${history != null}">
    <div class="pull-right bordered">
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
                <td><joda:format value="${historyItem.dateTime}" style="MM"/></td>
                <td>${historyItem.user}</td>
                <td><spring:message code="views.reservationRequest.type.${historyItem.type}"/></td>
                <td class="reservation-request-state">
                    <c:if test="${historyItem.state != null}">
                        <span class="${historyItem.state}">
                            <spring:message code="views.reservationRequest.state.${historyItem.state}"/>
                        </span>
                    </c:if>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${historyItem.id != reservationRequest.id && historyItem.type != 'DELETED'}">
                            <spring:eval var="historyItemDetailUrl"
                                         expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, historyItem.id)"/>
                            <a href="${historyItemDetailUrl}"  tabindex="1">
                                <spring:message code="views.list.action.show"/>
                            </a>
                        </c:when>
                        <c:otherwise>(<spring:message code="views.list.selected"/>)</c:otherwise>
                    </c:choose>
                    <c:if test="${historyItem.isRevertible}">
                        <spring:eval var="historyItemRevertUrl"
                                     expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetailRevert(contextPath, historyItem.id)"/>
                        | <a href="${historyItemRevertUrl}" tabindex="2"><spring:message code="views.list.action.revert"/></a>
                    </c:if>
                </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
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
</h1>

<div ng-app="jsp:reservationRequestDetail">

    <%-- Detail of request --%>
    <tag:reservationRequestDetail reservationRequest="${reservationRequest}" detailUrl="${detailUrl}"/>

    <%-- Reservation --%>
    <c:if test="${reservation != null}">
        <h2>
            <c:choose>
                <c:when test="${reservationRequest.allocationState == 'ALLOCATED'}">
                    <spring:message code="views.reservationRequestDetail.reservation"/>
                </c:when>
                <c:otherwise>
                    <spring:message code="views.reservationRequestDetail.oldReservation"/>
                </c:otherwise>
            </c:choose>
            <c:if test="${reservation.roomState.available}">
                <spring:eval var="urlRoomManagement"
                             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getRoomManagement(contextPath, reservation.roomId)"/>
                <a class="btn" href="${urlRoomManagement}">
                    <spring:message code="views.list.action.manage"/>
                </a>
            </c:if>
        </h2>
        <dl class="dl-horizontal">

            <dt><spring:message code="views.reservationRequest.slot"/>:</dt>
            <dd>
                <joda:format value="${reservation.slot.start}" style="MS"/> -
                <joda:format value="${reservation.slot.end}" style="MS"/>
            </dd>

            <dt><spring:message code="views.room.state"/>:</dt>
            <dd class="executable-state">
                <c:if test="${reservation.roomState != null}">
                    <spring:message code="views.reservationRequest.executableState.${reservation.roomState}" var="roomState"/>
                    <tag:help label="${roomState}" labelClass="${reservation.roomState}">
                        <span>
                            <spring:message code="views.help.reservationRequest.executableState.${reservation.roomState}"/>
                        </span>
                        <c:if test="${not empty reservation.roomStateReport}">
                            <pre>${reservation.roomStateReport}</pre>
                        </c:if>
                    </tag:help>
                </c:if>
            </dd>

            <dt><spring:message code="views.room.licenseCount"/>:</dt>
            <dd>${reservation.roomLicenseCount}</dd>

            <dt><spring:message code="views.room.aliases"/>:</dt>
            <dd>
                <tag:help label="${reservation.roomAliases}">
                    <c:if test="${not empty reservation.roomAliasesDescription}">
                        ${reservation.roomAliasesDescription}
                    </c:if>
                </tag:help>
            </dd>

        </dl>
    </c:if>

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
            <c:if test="${isProvidable && reservationRequest.slot.containsNow()}">
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
        <a class="btn" href="${modifyUrl}" tabindex="1">
            <spring:message code="views.button.modify"/>
        </a>
        <a class="btn" href="${deleteUrl}" tabindex="1">
            <spring:message code="views.button.delete"/>
        </a>
    </c:if>
</div>