<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<div ng-app="pagination">

<div ng-controller="PaginationController" ng-init="init('${contextPath}/reservation-request/data?start=:start&count=:count')">
    <pagination-page-size class="pull-right"><spring:message code="views.pagination.records"/></pagination-page-size>
    <h2><spring:message code="views.reservationRequest.type.aliases"/></h2>
    <table class="table table-striped table-hover">
        <thead>
        <tr>
            <th><spring:message code="views.reservationRequest.identifier"/></th>
            <th><spring:message code="views.reservationRequest.description"/></th>
            <th width="180px"><spring:message code="views.reservationRequestList.action"/></th>
        </tr>
        </thead>
        <tbody>
        <tr ng-repeat="reservationRequest in items">
            <td>{{reservationRequest.id}}</td>
            <td>{{reservationRequest.description}}</td>
            <td>
                <a href="${contextPath}/reservation-request/detail/{{reservationRequest.id}}"><spring:message code="views.reservationRequestList.action.detail"/></a>
                | <a href="${contextPath}/reservation-request/modify/{{reservationRequest.id}}"><spring:message code="views.reservationRequestList.action.modify"/></a>
                | <a href="${contextPath}/reservation-request/delete/{{reservationRequest.id}}"><spring:message code="views.reservationRequestList.action.delete"/></a>
            </td>
        </tr>
        <tr ng-hide="items.length">
            <td colspan="6" class="empty">- - - None - - -</td>
        </tr>
        </tbody>
    </table>
    <a class="btn btn-primary" href="${contextPath}/reservation-request/create?type=ALIAS"><spring:message code="views.button.create"/></a>
    <pagination-pages class="pull-right"><spring:message code="views.pagination.pages"/></pagination-pages>&nbsp;
</div>

<hr>


<div ng-controller="PaginationController" ng-init="init('${contextPath}/reservation-request/data?start=:start&count=:count')">
    <pagination-page-size class="pull-right"><spring:message code="views.pagination.records"/></pagination-page-size>
    <h2><spring:message code="views.reservationRequest.type.rooms"/></h2>
    <table class="table table-striped table-hover">
        <thead>
        <tr>
            <th><spring:message code="views.reservationRequest.identifier"/></th>
            <th><spring:message code="views.reservationRequest.description"/></th>
            <th width="180px"><spring:message code="views.reservationRequestList.action"/></th>
        </tr>
        </thead>
        <tbody>
        <tr ng-repeat="reservationRequest in items">
            <td>{{reservationRequest.id}}</td>
            <td>{{reservationRequest.description}}</td>
            <td>
                <a href="${contextPath}/reservation-request/detail/{{reservationRequest.id}}"><spring:message code="views.reservationRequestList.action.detail"/></a>
                | <a href="${contextPath}/reservation-request/modify/{{reservationRequest.id}}"><spring:message code="views.reservationRequestList.action.modify"/></a>
                | <a href="${contextPath}/reservation-request/delete/{{reservationRequest.id}}"><spring:message code="views.reservationRequestList.action.delete"/></a>
            </td>
        </tr>
        <tr ng-hide="items.length">
            <td colspan="6" class="empty">- - - None - - -</td>
        </tr>
        </tbody>
    </table>
    <a class="btn btn-primary" href="${contextPath}/reservation-request/create?type=ROOM"><spring:message code="views.button.create"/></a>
    <pagination-pages class="pull-right"><spring:message code="views.pagination.pages"/></pagination-pages>&nbsp;
</div>
</div>