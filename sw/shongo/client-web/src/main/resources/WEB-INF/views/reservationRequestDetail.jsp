<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<dl class="dl-horizontal">

    <dt><spring:message code="views.reservationRequest.identifier"/>:</dt>
    <dd>${reservationRequest.id} </dd>

    <dt><spring:message code="views.reservationRequest.created"/>:</dt>
    <dd><joda:format value="${reservationRequest.created}" style="MM" /> </dd>

    <dt><spring:message code="views.reservationRequest.purpose"/>:</dt>
    <dd>
        <c:choose>
            <c:when test="${reservationRequest.purpose == 'SCIENCE'}">
                <spring:message code="views.reservationRequest.purpose.science"/>
            </c:when>
            <c:when test="${reservationRequest.purpose == 'EDUCATION'}">
                <spring:message code="views.reservationRequest.purpose.education"/>
            </c:when>
            <c:when test="${reservationRequest.purpose == 'MAINTENANCE'}">
                <spring:message code="views.reservationRequest.purpose.maintenance"/>
            </c:when>
            <c:when test="${reservationRequest.purpose == 'OWNER'}">
                <spring:message code="views.reservationRequest.purpose.owner"/>
            </c:when>
        </c:choose>
    </dd>

    <dt><spring:message code="views.reservationRequest.description"/>:</dt>
    <dd>${reservationRequest.description} </dd>

</dl>

<div class="pull-right">
    <a class="btn btn-primary" href="${contextPath}/reservation-request"><spring:message code="views.button.back"/></a>
    <a class="btn" href=""><spring:message code="views.button.refresh"/></a>
    <security:authorize access="hasPermission(#reservationRequest, T(cz.cesnet.shongo.controller.Permission).WRITE)">
        <a class="btn" href="${contextPath}/reservation-request/modify/${reservationRequest.id}"><spring:message code="views.button.modify"/></a>
        <a class="btn" href="${contextPath}/reservation-request/delete/${reservationRequest.id}"><spring:message code="views.button.delete"/></a>
    </security:authorize>
</div>