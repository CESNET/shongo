<%--
  -- Page for confirmation of deletion of reservation request or for displaying dependencies because of which
  -- the reservation request can't be deleted.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<s:eval var="confirmUrl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDeleteConfirm(contextPath, reservationRequest.id)"/>
<c:set var="backUrl">${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_LIST %></c:set>
<c:set var="reservationRequestName"><strong>${reservationRequest.id}</strong></c:set>

<c:choose>
    <c:when test="${dependencies.size() > 0}">
        <p><s:message code="views.reservationRequestDelete.referenced" arguments="${reservationRequestName}"/></p>
        <ul>
            <c:forEach var="dependency" items="${dependencies}">
                <li>
                    <s:eval expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetail(contextPath, dependency.id)" var="urlDetail"/>
                    <a href="${urlDetail}">${dependency.description}</a>
                    (<s:message code="views.reservationRequestDelete.dateTime"/> <joda:format value="${dependency.dateTime}" style="MS"/>)
                </li>
            </c:forEach>
        </ul>
        <div>
            <a class="btn btn-primary" href="${backUrl}"><s:message code="views.button.back"/></a>
        </div>
    </c:when>
    <c:otherwise>
        <p><s:message code="views.reservationRequestDelete.question" arguments="${reservationRequestName}"/></p>
        <div>
            <a class="btn btn-primary" href="${confirmUrl}"><s:message code="views.button.yes"/></a>
            <a class="btn" href="${backUrl}"><s:message code="views.button.no"/></a>
        </div>
    </c:otherwise>
</c:choose>

