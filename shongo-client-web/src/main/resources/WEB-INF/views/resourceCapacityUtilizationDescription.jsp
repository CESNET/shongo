<%--
  -- Description of single resource capacity utilization.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<div class="jspResourceCapacityUtilizationDescription">
    <dl class="dl-horizontal">

        <dt><spring:message code="views.resource"/>:</dt>
        <dd>
            ${resourceCapacity.resourceName} (${resourceCapacity.resourceId})
        </dd>

        <dt><spring:message code="views.interval"/>:</dt>
        <dd>
            <tag:format value="${interval}" style="date"/>
        </dd>

        <c:set var="peakBucket" value="${resourceCapacityUtilization.peakBucket}"/>
        <c:if test="${peakBucket != null}">

            <dt><spring:message code="views.resourceCapacityUtilizationDescription.maximumUtilization"/>:</dt>
            <dd class="${resourceCapacity.getCssClass(resourceCapacityUtilization)}">
                ${resourceCapacity.formatUtilization(resourceCapacityUtilization, 'MAXIMUM', 'ABSOLUTE')}/${resourceCapacity.licenseCount}
                (<span>${resourceCapacity.formatUtilization(resourceCapacityUtilization, 'MAXIMUM', 'RELATIVE')}</span>,
                <spring:message code="views.resourceCapacityUtilizationDescription.maximumUtilization.dateTime"/>: <tag:format value="${peakBucket.dateTime}"/>)
            </dd>

            <dt><spring:message code="views.resourceCapacityUtilizationDescription.averageUtilization"/>:</dt>
            <dd>
                    ${resourceCapacity.formatUtilization(resourceCapacityUtilization, 'AVERAGE', 'ABSOLUTE')}/${resourceCapacity.licenseCount}
                (<span>${resourceCapacity.formatUtilization(resourceCapacityUtilization, 'AVERAGE', 'RELATIVE')}</span>)
            </dd>

        </c:if>

    </dl>

    <h2><spring:message code="views.resourceCapacityUtilizationDescription.reservations"/>:</h2>
    <table class="table table-striped table-hover">
        <thead>
        <tr>
            <th><spring:message code="views.reservation.id"/></th>
            <th><spring:message code="views.reservation.slot"/></th>
            <th><spring:message code="views.reservation.licenseCount"/></th>
            <th><spring:message code="views.reservation.reservationRequest"/></th>
            <th><spring:message code="views.reservation.userId"/></th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${resourceCapacityUtilization.reservations}" var="reservation">
            <c:set var="user" value="${users.get(reservation.userId)}"/>
            <c:set var="cssClass" value=""/>
            <c:if test="${reservation.slot.contains(peakBucket.dateTime.millis)}">
                <c:set var="cssClass" value="${resourceCapacity.getCssClass(resourceCapacityUtilization)}"/>
            </c:if>
            <tr class="${cssClass}">
                <td>
                    ${reservation.id}
                </td>
                <td><tag:format value="${reservation.slot}"/></td>
                <td>${not empty reservation.roomLicenseCount ? reservation.roomLicenseCount : 1} licenses</td>
                <td>
                    <tag:url var="reservationRequestDetailUrl" value="<%= ClientWebUrl.DETAIL_VIEW %>">
                        <tag:param name="objectId" value="${reservation.reservationRequestId}"/>
                        <tag:param name="back-url" value="${requestScope.requestUrl}"/>
                    </tag:url>
                    <a href="${reservationRequestDetailUrl}">${reservation.reservationRequestId}</a>
                </td>
                <td>${user.fullName} (${reservation.userId}, <a href="mailto: ${user.email}">${user.email}</a>)</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>