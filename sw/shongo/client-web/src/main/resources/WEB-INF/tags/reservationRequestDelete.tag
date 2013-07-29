<%--
  -- Confirmation for deletion of reservation request delete or display dependencies.
  --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>

<%@attribute name="dependencies" required="true"
             type="java.util.Collection<cz.cesnet.shongo.controller.api.ReservationRequestSummary>" %>
<%@attribute name="detailUrl" required="true" %>

<c:set var="reservationRequestName"><strong>${reservationRequest.id}</strong></c:set>

<c:choose>
    <c:when test="${dependencies.size() > 0}">
        <p><spring:message code="views.reservationRequestDelete.referenced" arguments="${reservationRequestName}"/></p>
        <ul>
            <c:forEach var="dependency" items="${dependencies}">
                <li>
                    <spring:eval var="urlDetail"
                                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, dependency.id)" />
                    <a href="${urlDetail}" tabindex="2">${dependency.description}</a>
                    (<spring:message code="views.reservationRequestDelete.dateTime"/>&nbsp;<joda:format value="${dependency.dateTime}" style="MS"/>)
                </li>
            </c:forEach>
        </ul>
    </c:when>
    <c:otherwise>
        <p><spring:message code="views.reservationRequestDelete.question" arguments="${reservationRequestName}"/></p>
    </c:otherwise>
</c:choose>