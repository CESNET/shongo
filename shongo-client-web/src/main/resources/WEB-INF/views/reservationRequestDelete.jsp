<%--
  -- Page for confirmation of deletion of multiple reservation requests or for displaying dependencies because of which
  -- the reservation request can't be deleted.
  --%>

<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="backUrl">
    <%= ClientWebUrl.HOME%>
</c:set>
<tag:url var="backUrl" value="${requestScope.backUrl.getUrl(backUrl)}"/>

<script type="text/javascript">
    var module = angular.module('jsp:reservationRequestDelete', ['ngApplication', 'ngTooltip']);
</script>

<c:choose>
    <c:when test="${(reservationRequestDeleteDetailList.size() >  1)}">
        <p><strong><spring:message code="views.reservationRequestDelete.questionPlural"/></strong></p>
    </c:when>
    <c:otherwise>
        <p><strong><spring:message code="views.reservationRequestDelete.questionSingular"/></strong></p>
    </c:otherwise>
</c:choose>

<c:forEach var="requestDetail" items="${reservationRequestDeleteDetailList}">

    <tag:url var="reservationRequestDetailUrl" value="<%= ClientWebUrl.DETAIL_VIEW %>">
        <tag:param name="objectId" value="${requestDetail.reservationRequestId}" escape="false"/>
    </tag:url>

    <c:set var="specificationTypeMessage">
        <strong><spring:message code="views.specificationType.forWithName.${requestDetail.specificationType}" arguments=" ${requestDetail.roomName}"/></strong>
    </c:set>

    <ul>
        <li>
            <c:choose>
                <c:when test="${requestDetail.dependencies.size() > 0}">
                    <%-- URL link of request with dependecies --%>
                    <c:set var="requestWithDependencyDetailUrl">
                        <a href="${reservationRequestDetailUrl}" target="_blank">${specificationTypeMessage}</a>
                    </c:set>

                    <%-- Formated link of request with dependecies --%>
                    <spring:message code="views.reservationRequestDelete.included" var="included" />
                    <p><spring:message code="views.reservationRequestDelete.questionWithSlot"
                                       arguments="${requestWithDependencyDetailUrl} ;<strong>${requestDetail.slot}</strong> ${included}:" argumentSeparator=";"/></p>
                    <ul>
                        <c:forEach var="dependency" items="${requestDetail.dependencies}">
                            <li>
                                <%-- URL of dependancy--%>
                                <tag:url var="parentReservationRequestDetailUrl"
                                         value="<%= ClientWebUrl.DETAIL_VIEW %>">
                                    <tag:param name="objectId" value="${dependency.id}"/>
                                </tag:url>

                                <%-- Name of dependancy --%>
                                <spring:eval
                                        expression="T(cz.cesnet.shongo.client.web.models.SpecificationType).fromReservationRequestSummary(dependency)"
                                        var="dependencySpecificationType"/>
                                <a href="${parentReservationRequestDetailUrl}" target="_blank" tabindex="2">
                                    <strong>
                                        <spring:message code="views.reservationRequestDelete.included.${dependencySpecificationType}" arguments=" "/>
                                    </strong>
                                </a>

                                <%-- Link and time slot of dependency --%>
                                (<strong><tag:format value="${dependency.earliestSlot}"/><c:if test="${not empty dependency.description}">, ${dependency.description}</c:if></strong>)
                            </li>
                        </c:forEach>
                    </ul>
                </c:when>
                <c:otherwise>
                    <%-- Show all reservation slots for periodic requests --%>
                    <c:set var="timeSlotMessage">
                        <c:if test="${requestDetail.futureSlotCount > 0}">
                            <span ng-app="jsp:reservationRequestDelete">
                            <spring:message code="views.reservationRequestList.slotMore" var="slotMore"
                                            arguments="${requestDetail.futureSlotCount}"/>
                                <tag:help label="(${slotMore})" cssClass="push-top">
                                    <c:choose>
                                        <c:when test="${fn:length(requestDetail.reservationSlots) - requestDetail.futureSlotCount > 0}">
                                            <c:forEach var="nextSlot"
                                                       items="${requestDetail.reservationSlots}"
                                                       begin="${fn:length(requestDetail.reservationSlots) - requestDetail.futureSlotCount}">
                                                <strong>${nextSlot}</strong><br/>
                                            </c:forEach>
                                        </c:when>
                                        <c:otherwise>
                                            <tag:help label="(${slotMore})" cssClass="push-top">
                                                <spring:message
                                                        code="views.reservationRequestList.slotMoreHelp"/>
                                            </tag:help>
                                        </c:otherwise>
                                    </c:choose>
                                </tag:help>
                            </span>
                        </c:if>
                    </c:set>

                    <%-- Link and time slot of request --%>
                    <%--<c:set var="slotDetailUrl">--%>
                        <%--<a href="${reservationRequestDetailUrl}" target="_blank"><strong>${requestDetail.slot}</strong></a>--%>
                    <%--</c:set>--%>

                    <%-- Link and type of request --%>
                    <%--<spring:message code="views.reservationRequestDelete.question.${requestDetail.specificationType}" var="nameOfType" />--%>
                    <c:set var="namedDetailUrl">
                        <a href="${reservationRequestDetailUrl}" target="_blank"><strong>${specificationTypeMessage}</strong></a>
                    </c:set>

                    <%-- Formated link to request without dependencies --%>
                    <p><spring:message
                            code="views.reservationRequestDelete.questionWithSlot"
                            arguments="${namedDetailUrl};<strong>${requestDetail.slot}</strong> ${timeSlotMessage}" argumentSeparator=";"/>
                    </p>
                </c:otherwise>
            </c:choose>
        </li>
    </ul>
</c:forEach>

<div>
    <form method="post" class="form-inline">
        <spring:message code="views.button.yes" var="buttonYes"/>
        <input type="submit" class="btn btn-primary" tabindex="1" value="${buttonYes}"/>
        <a class="btn btn-default" href="${backUrl}" tabindex="1"><spring:message code="views.button.no"/></a>
    </form>
</div>

<%--<c:choose>--%>
    <%--<c:when test="${(requestDetail.dependencies.size() > 0) || (requestDetail.dependencies.size > 0)}">--%>
        <%--<div>--%>
            <%--<form method="post" action="?dependencies=true" class="form-inline">--%>
                <%--<a class="btn btn-primary" href="${backUrl}" tabindex="1"><spring:message code="views.button.back"/></a>--%>
                <%--<spring:message code="views.button.yes" var="buttonYes"/>--%>
                <%--<spring:message code="views.button.deleteAll" var="buttonDeleteAll"/>--%>
                <%--<input type="submit" class="btn btn-default" tabindex="1" value="${buttonDeleteAll}"/>--%>
            <%--</form>--%>
        <%--</div>--%>
    <%--</c:when>--%>
    <%--<c:otherwise>--%>
        <%--<div>--%>
            <%--<form method="post" class="form-inline">--%>
                <%--<spring:message code="views.button.yes" var="buttonYes"/>--%>
                <%--<input type="submit" class="btn btn-primary" tabindex="1" value="${buttonYes}"/>--%>
                <%--<a class="btn btn-default" href="${backUrl}" tabindex="1"><spring:message code="views.button.no"/></a>--%>
            <%--</form>--%>
        <%--</div>--%>
    <%--</c:otherwise>--%>
<%--</c:choose>--%>