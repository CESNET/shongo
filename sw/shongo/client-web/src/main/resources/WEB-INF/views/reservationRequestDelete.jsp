<%--
  -- Page for confirmation of deletion of reservation request or for displaying dependencies because of which
  -- the reservation request can't be deleted.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="reservationRequestName"><strong>${reservationRequest.id}</strong></c:set>

<form action="${contextPath}/reservation-request/delete/confirmed" method="post">

    <input type="hidden" name="id" value="${reservationRequest['id']}"/>

<c:choose>
    <c:when test="${dependencies.size() > 0}">
        <p><spring:message code="views.reservationRequestDelete.referenced" arguments="${reservationRequestName}"/></p>
        <ul>
            <c:forEach var="dependency" items="${dependencies}">
                <li><a href="${contextPath}/reservation-request/detail?id=${dependency.id}">${dependency.description}</a>
                    (<spring:message code="views.reservationRequestDelete.dateTime"/> <joda:format value="${dependency.dateTime}" style="MS"/>)</li>
            </c:forEach>
        </ul>
        <div>
            <a class="btn btn-primary" href="${contextPath}/reservation-request"><spring:message code="views.button.back"/></a>
        </div>
    </c:when>
    <c:otherwise>
        <p><spring:message code="views.reservationRequestDelete.question" arguments="${reservationRequestName}"/></p>
        <div>
            <spring:message code="views.button.yes" var="yes"/>
            <input type="submit" class="btn btn-primary" value="${yes}"/>
            <a class="btn" href="${contextPath}/reservation-request"><spring:message code="views.button.no"/></a>
        </div>
    </c:otherwise>
</c:choose>

</form>
