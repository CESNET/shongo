<%--
  -- Wizard page for detail of reservation request.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="detailUrl">
    ${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.WIZARD_RESERVATION_REQUEST_DETAIL %>
</c:set>

<c:if test="${empty reservationRequest.parentReservationRequestId}">
    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
        <security:accesscontrollist hasPermission="PROVIDE_RESERVATION_REQUEST"
                                    domainObject="${reservationRequest}" var="isProvidable"/>
    </c:if>
    <security:accesscontrollist hasPermission="WRITE" domainObject="${reservationRequest}" var="isWritable"/>
</c:if>

<script type="text/javascript">
    angular.module('jsp:wizardReservationRequestDetail', ['tag:reservationRequestDetail', 'ngPagination']);
</script>

<c:if test="${isProvidable && reservationRequest.slot.containsNow()}">
    <spring:eval var="createPermanentRoomCapacityUrl"
                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getWizardCreatePermanentRoomCapacity(contextPath, reservationRequest.id)"/>
</c:if>

<div class="actions">
    <span><spring:message code="views.wizard.select"/></span>
    <ul>
        <c:if test="${createPermanentRoomCapacityUrl != null}">
            <li>
                <c:choose>
                    <c:when test="${reservationRequest.allocationState == 'ALLOCATED'}">
                        <a href="${createPermanentRoomCapacityUrl}" tabindex="1">
                            <spring:message code="views.wizard.reservationRequestDetail.createPermanentRoomCapacity"/>
                        </a>
                    </c:when>
                    <c:otherwise>
                        <span class="disabled">
                            <spring:message code="views.wizard.reservationRequestDetail.createPermanentRoomCapacity"/>
                        </span>
                    </c:otherwise>
                </c:choose>
            </li>
        </c:if>
        <li>
            <a href="javascript: location.reload();"  tabindex="1">
                <spring:message code="views.wizard.reservationRequestDetail.refresh"/>
            </a>
        </li>
        <c:if test="${isWritable}">
            <li>
                <spring:eval var="deleteUrl"
                             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getWizardReservationRequestDelete(contextPath, reservationRequest.id)"/>
                <a href="${deleteUrl}" tabindex="1"><spring:message code="views.wizard.reservationRequestDetail.delete"/></a>
            </li>
        </c:if>
    </ul>
</div>

<h1><spring:message code="views.wizard.reservationRequestDetail"/></h1>

<hr/>

<div ng-app="jsp:wizardReservationRequestDetail">

    <%-- Detail of request --%>
    <tag:reservationRequestDetail reservationRequest="${reservationRequest}" detailUrl="${detailUrl}"/>

    <%-- Reservation --%>
    <c:if test="${reservation != null}">
        <h2>
            <spring:message code="views.wizard.reservationRequestDetail.room"/>
            <c:if test="${reservation.roomState.available}">
                <spring:eval var="urlRoomManagement"
                             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getRoomManagement(contextPath, reservation.roomId)"/>
                <a class="btn" href="${urlRoomManagement}">
                    <spring:message code="views.list.action.manage"/>
                </a>
            </c:if>
        </h2>
        <dl class="dl-horizontal">

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

            <dt><spring:message code="views.room.aliases"/>:</dt>
            <dd>
                <c:if test="${not empty reservation.roomAliasesDescription}">
                    <tag:help label="${reservation.roomAliases}">${reservation.roomAliasesDescription}</tag:help>
                </c:if>
            </dd>

        </dl>
    </c:if>

    <%-- User roles --%>
    <h2><spring:message code="views.reservationRequest.userRoles"/></h2>
    <ul>
        <c:forEach items="${userRoles}" var="userRole">
            <li>${userRole.user.fullName} (<spring:message code="views.aclRecord.role.${userRole.role}"/>)</li>
        </c:forEach>
    </ul>

    <%-- Periodic events --%>
    <c:if test="${reservationRequest.periodicityType != 'NONE'}">
        <tag:reservationRequestChildren detailUrl="${detailUrl}"/>
    </c:if>

    <%-- Permanent room capacities --%>
    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
        <tag:reservationRequestUsages detailUrl="${detailUrl}"/>
    </c:if>

    <security:accesscontrollist hasPermission="WRITE" domainObject="${reservationRequest}" var="isWritable"/>

</div>

<hr/>

<div class="pull-right">
    <a class="btn" href="javascript: location.reload();">
        <spring:message code="views.button.refresh"/>
    </a>
</div>