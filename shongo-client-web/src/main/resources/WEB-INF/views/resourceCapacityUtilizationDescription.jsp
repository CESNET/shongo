<%--
  -- Description of single resource capacity utilization.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<dl class="dl-horizontal" ng-controller="DynamicStateController">

    <dt><spring:message code="views.resourceReservations.resource"/>:</dt>
    <dd>
        ${resourceCapacity.resourceName} (${resourceCapacity.resourceId})
    </dd>

    <dt><spring:message code="views.resourceReservations.interval"/>:</dt>
    <dd>
        <tag:format value="${interval}" style="date"/>
    </dd>

    <c:set var="peakBucket" value="${resourceCapacityUtilization.peakBucket}"/>
    <c:if test="${peakBucket != null}">
        <dt>Peak utilization:</dt>
        <dd>
            ${resourceCapacity.formatUtilization(resourceCapacityUtilization, 'ABSOLUTE')}
            (${resourceCapacity.formatUtilization(resourceCapacityUtilization, 'RELATIVE')})
        </dd>

        <dt>Peak date/time:</dt>
        <dd>
            <tag:format value="${peakBucket.dateTime}"/>
        </dd>

    </c:if>

</dl>

<h2>Reservations:</h2>
<table class="table table-striped table-hover">
    <thead>
    <tr>
        <th>id</th>
        <th>slot</th>
        <th>licenses</th>
        <th>reservation request</th>
        <th>requested by</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${resourceCapacityUtilization.reservations}" var="reservation">
        <tr>
            <td>
                <c:choose>
                    <c:when test="${reservation.slot.contains(peakBucket.dateTime.millis)}">
                        <strong>[peak] ${reservation.id}</strong>
                    </c:when>
                    <c:otherwise>
                        ${reservation.id}
                    </c:otherwise>
                </c:choose>
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
            <td>${users.get(reservation.userId).fullName} (${reservation.userId})</td>
        </tr>
    </c:forEach>
    </tbody>
</table>

