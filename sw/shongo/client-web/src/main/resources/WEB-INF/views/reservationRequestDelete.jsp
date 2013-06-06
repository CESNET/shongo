<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>

<c:set var="path" value="${pageContext.request.contextPath}"/>
<c:set var="reservationRequestName"><strong>${reservationRequest.id}</strong></c:set>

<c:choose>
    <c:when test="${reservationRequest.dependencies.size() > 0}">
        <p><spring:message code="views.reservationRequestDelete.referenced" arguments="${reservationRequestName}"/></p>
        <ul>
            <c:forEach var="dependency" items="${reservationRequest.dependencies}">
                <li><a href="${path}/reservation-request/detail?id=${dependency.id}">${dependency.description}</a> (<joda:format value="${dependency.earliestSlot.start}" style="MS"/> - <joda:format value="${dependency.earliestSlot.end}" style="MS"/>)</li>
            </c:forEach>
        </ul>
        <div>
            <a class="btn btn-primary" href="${path}/reservation-request"><spring:message code="views.button.back"/></a>
        </div>
    </c:when>
    <c:otherwise>
        <p><spring:message code="views.reservationRequestDelete.question" arguments="${reservationRequestName}"/></p>
        <div>
            <a class="btn btn-primary" href="${path}/reservation-request/delete/${reservationRequest.id}/confirmed"><spring:message code="views.button.yes"/></a>
            <a class="btn" href="${path}/reservation-request"><spring:message code="views.button.no"/></a>
        </div>
    </c:otherwise>
</c:choose>

