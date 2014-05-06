
<%--
  -- Page for confirmation of deletion of reservation request or for displaying dependencies because of which
  -- the reservation request can't be deleted.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="backUrl"><%= ClientWebUrl.RESERVATION_REQUEST_LIST_VIEW %></c:set>
<tag:url var="backUrl" value="${requestScope.backUrl.getUrl(backUrl)}"/>

<c:set var="specificationType">
    <strong><spring:message code="views.specificationType.forWithName.${specificationType}" arguments=" ${reservationRequest.roomName}"/></strong>
</c:set>

<c:choose>
    <c:when test="${dependencies.size() > 0}">
        <p><spring:message code="views.reservationRequestDelete.referenced" arguments="${specificationType}"/></p>
        <ul>
            <c:forEach var="dependency" items="${dependencies}">
                <li>
                    <tag:url var="parentReservationRequestDetailUrl" value="<%= ClientWebUrl.DETAIL_VIEW %>">
                        <tag:param name="objectId" value="${dependency.id}"/>
                    </tag:url>
                    <a href="${parentReservationRequestDetailUrl}" tabindex="2">
                        <spring:eval expression="T(cz.cesnet.shongo.client.web.models.SpecificationType).fromReservationRequestSummary(dependency)" var="dependencySpecificationType"/>
                        <strong><spring:message code="views.detail.title.${dependencySpecificationType}" arguments=" "/></strong>
                    </a>
                    (<tag:format value="${dependency.earliestSlot}"/><c:if test="${not empty dependency.description}">, ${dependency.description}</c:if>)
                </li>
            </c:forEach>
        </ul>
    </c:when>
    <c:otherwise>
        <p><spring:message code="views.reservationRequestDelete.question" arguments="${specificationType}"/></p>
    </c:otherwise>
</c:choose>

<c:choose>
    <c:when test="${dependencies.size() > 0}">
        <div>
            <a class="btn btn-primary" href="${backUrl}" tabindex="1"><spring:message code="views.button.back"/></a>
            <form method="post" action="?dependencies=true" class="form-inline">
                <spring:message code="views.button.yes" var="buttonYes"/>
                <spring:message code="views.button.deleteAll" var="buttonDeleteAll"/>
                <input type="submit" class="btn btn-default" tabindex="1" value="${buttonDeleteAll}"/>
            </form>
        </div>
    </c:when>
    <c:otherwise>
        <div>
            <form method="post" class="form-inline">
                <spring:message code="views.button.yes" var="buttonYes"/>
                <input type="submit" class="btn btn-primary" tabindex="1" value="${buttonYes}"/>
                <a class="btn btn-default" href="${backUrl}" tabindex="1"><spring:message code="views.button.no"/></a>
            </form>
        </div>
    </c:otherwise>
</c:choose>

