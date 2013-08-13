<%--
  -- Page for confirmation of deletion of reservation request or for displaying dependencies because of which
  -- the reservation request can't be deleted.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="detailUrl">
    ${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_DETAIL %>
</c:set>
<c:set var="backUrl">
    ${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_LIST %>
</c:set>
<spring:eval var="confirmUrl"
             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDeleteConfirm(contextPath, reservationRequest.id)"/>

<tag:reservationRequestDelete dependencies="${dependencies}" detailUrl="${detailUrl}"/>

<c:choose>
    <c:when test="${dependencies.size() > 0}">
        <div>
            <a class="btn btn-primary" href="${backUrl}" tabindex="1"><spring:message code="views.button.back"/></a>
            <a class="btn" href="${confirmUrl}?dependencies=true" tabindex="1">
                <spring:message code="views.button.deleteAll"/>
            </a>
        </div>
    </c:when>
    <c:otherwise>
        <div>
            <a class="btn btn-primary" href="${confirmUrl}" tabindex="1"><spring:message code="views.button.yes"/></a>
            <a class="btn" href="${backUrl}" tabindex="1"><spring:message code="views.button.no"/></a>
        </div>
    </c:otherwise>
</c:choose>

