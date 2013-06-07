<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<dl class="dl-horizontal">

    <dt><spring:message code="views.reservationRequest.identifier"/>:</dt>
    <dd>${reservationRequest.id} </dd>

    <dt><spring:message code="views.reservationRequest.description"/>:</dt>
    <dd>${reservationRequest.description} </dd>

</dl>

<div class="pull-right">
    <a class="btn btn-primary" href="${contextPath}/reservation-request"><spring:message code="views.button.back"/></a>
    <a class="btn" href=""><spring:message code="views.button.refresh"/></a>
    <a class="btn" href="${contextPath}/reservation-request/delete/${reservationRequest.id}"><spring:message code="views.button.delete"/></a>
</div>