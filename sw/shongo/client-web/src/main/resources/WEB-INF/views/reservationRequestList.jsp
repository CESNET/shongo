<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:set var="path" value="${pageContext.request.contextPath}"/>

<script src="${path}/js/angular.min.js"></script>
<script src="${path}/js/angular-resource.min.js"></script>
<script src="${path}/js/pagination.js"></script>

<div ng-app="pagination">

<div ng-controller="PaginationController" ng-init="init('${path}/reservation-request/data?start=:start&count=:count')">
    <pagination-page-size class="pull-right"></pagination-page-size>
    <h2>Aliases</h2>
    <table class="table table-striped table-hover">
        <thead>
        <tr>
            <th><spring:message code="views.reservationRequestList.identifier"/></th>
            <th><spring:message code="views.reservationRequestList.description"/></th>
            <th width="180px"><spring:message code="views.reservationRequestList.action"/></th>
        </tr>
        </thead>
        <tbody>
        <tr ng-repeat="reservationRequest in items">
            <td>{{reservationRequest.id}}</td>
            <td>{{reservationRequest.description}}</td>
            <td>
                <a href="${path}/reservation-request/detail?id={{reservationRequest.id}}"><spring:message code="views.reservationRequestList.detail"/></a>
                | <a href="${path}/reservation-request/delete?id={{reservationRequest.id}}"><spring:message code="views.reservationRequestList.delete"/></a>
            </td>
        </tr>
        <tr ng-hide="items.length">
            <td colspan="6" class="empty">- - - None - - -</td>
        </tr>
        </tbody>
    </table>
    <pagination-pages class="pull-right"></pagination-pages>&nbsp;
</div>

<hr>


<div ng-controller="PaginationController" ng-init="init('${path}/reservation-request/data?start=:start&count=:count')">
    <pagination-page-size class="pull-right"></pagination-page-size>
    <h2>Room capacity</h2>
    <table class="table table-striped table-hover">
        <thead>
        <tr>
            <th><spring:message code="views.reservationRequestList.identifier"/></th>
            <th><spring:message code="views.reservationRequestList.description"/></th>
            <th width="180px"><spring:message code="views.reservationRequestList.action"/></th>
        </tr>
        </thead>
        <tbody>
        <tr ng-repeat="reservationRequest in items">
            <td>{{reservationRequest.id}}</td>
            <td>{{reservationRequest.description}}</td>
            <td>
                <a href="${path}/reservation-request/detail?id={{reservationRequest.id}}"><spring:message code="views.reservationRequestList.detail"/></a>
                | <a href="${path}/reservation-request/delete?id={{reservationRequest.id}}"><spring:message code="views.reservationRequestList.delete"/></a>
            </td>
        </tr>
        <tr ng-hide="items.length">
            <td colspan="6" class="empty">- - - None - - -</td>
        </tr>
        </tbody>
    </table>
    <pagination-pages class="pull-right"></pagination-pages>&nbsp;
</div>
</div>