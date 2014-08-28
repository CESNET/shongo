
<%--
  -- Page for confirmation of deletion of reservation request or for displaying dependencies because of which
  -- the reservation request can't be deleted.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="backUrl"><%= ClientWebUrl.HOME%></c:set>
<tag:url var="backUrl" value="${requestScope.backUrl.getUrl(backUrl)}"/>

<c:set var="specificationTypeMessage">
    <strong><spring:message code="views.specificationType.forWithName.${specificationType}" arguments=" ${reservationRequest.roomName}"/></strong>
</c:set>

<script type="text/javascript">
    var module = angular.module('jsp:reservationRequestDelete', ['ngApplication', 'ngTooltip']);


</script>

<c:choose>
    <c:when test="${dependencies.size() > 0}">
        <p><spring:message code="views.reservationRequestDelete.referenced" arguments="${specificationTypeMessage}"/></p>
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
        <c:choose>
            <c:when test="${specificationType == 'MEETING_ROOM'}">
                <c:set var="timeSlotMessage">
                    <c:if test="${reservationRequest.futureSlotCount > 0}">
                        <span ng-app="jsp:reservationRequestDelete">
                        <spring:message code="views.reservationRequestList.slotMore" var="slotMore" arguments="${reservationRequest.futureSlotCount}"/>
                            <tag:help label="(${slotMore})" cssClass="push-top">
                                <c:choose>
                                    <c:when test="${fn:length(reservationSlots) - reservationRequest.futureSlotCount > 0}">
                                        <c:forEach var="nextSlot" items="${reservationSlots}" begin="${fn:length(reservationSlots) - reservationRequest.futureSlotCount}">
                                            <strong>${nextSlot}</strong><br />
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        <tag:help label="(${slotMore})" cssClass="push-top">
                                            <spring:message code="views.reservationRequestList.slotMoreHelp"/>
                                        </tag:help>
                                    </c:otherwise>
                                </c:choose>
                            </tag:help>
                        </span>
                    </c:if>
                </c:set>
                <span><spring:message code="views.reservationRequestDelete.question.${specificationType}" arguments="${slot} ${timeSlotMessage}" argumentSeparator=";"/></span>
            </c:when>
            <c:otherwise>
                <p><spring:message code="views.reservationRequestDelete.question" arguments="${specificationTypeMessage}"/></p>
            </c:otherwise>
        </c:choose>
    </c:otherwise>
</c:choose>

<c:choose>
    <c:when test="${dependencies.size() > 0}">
        <div>
            <form method="post" action="?dependencies=true" class="form-inline">
                <a class="btn btn-primary" href="${backUrl}" tabindex="1"><spring:message code="views.button.back"/></a>
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