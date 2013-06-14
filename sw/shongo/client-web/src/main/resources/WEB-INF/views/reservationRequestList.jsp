<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<script type="text/javascript">
    window.onErrorResolved = false;
    window.onError = function(response) {
        // Handle only first error
        if (window.onErrorResolved) {
            return;
        }
        window.onErrorResolved = true;

        // Rewrite document by error page
        document.write(response.data);
    };
</script>

<div ng-app="pagination" ng-controller="ReadyController">

    <div class="spinner" ng-hide="ready"></div>

    <div ng-show="ready">

        <div ng-controller="PaginationController"
             ng-init="init('list_aliases', '${contextPath}/reservation-request/data?start=:start&count=:count&type=ALIAS')"
             on-error="window.onError">
            <pagination-page-size class="pull-right"><spring:message
                    code="views.pagination.records"/></pagination-page-size>
            <h2><spring:message code="views.reservationRequest.type.aliases"/></h2>
            <table class="table table-striped table-hover">
                <thead>
                <tr>
                    <th width="85px"><spring:message code="views.reservationRequest.created"/></th>
                    <th><spring:message code="views.reservationRequest.user"/></th>
                    <th><spring:message code="views.reservationRequest.technology"/></th>
                    <th><spring:message code="views.reservationRequest.specification.alias.roomName"/></th>
                    <th width="180px"><spring:message code="views.reservationRequestList.earliestSlot"/></th>
                    <th><spring:message code="views.reservationRequest.description"/></th>
                    <th width="160px"><spring:message code="views.reservationRequestList.action"/></th>
                </tr>
                </thead>
                <tbody>
                <tr ng-repeat="reservationRequest in items">
                    <td>{{reservationRequest.created}}</td>
                    <td>{{reservationRequest.user}}</td>
                    <td>{{reservationRequest.technology}}</td>
                    <td>{{reservationRequest.roomName}}</td>
                    <td>{{reservationRequest.earliestSlotStart}}<br/>{{reservationRequest.earliestSlotEnd}}</td>
                    <td>{{reservationRequest.description}}</td>
                    <td>
                        <a href="${contextPath}/reservation-request/detail/{{reservationRequest.id}}"><spring:message
                                code="views.reservationRequestList.action.detail"/></a>
                        | <a href="${contextPath}/reservation-request/modify/{{reservationRequest.id}}"><spring:message
                            code="views.reservationRequestList.action.modify"/></a>
                        | <a href="${contextPath}/reservation-request/delete/{{reservationRequest.id}}"><spring:message
                            code="views.reservationRequestList.action.delete"/></a>
                    </td>
                </tr>
                <tr ng-hide="items.length">
                    <td colspan="7" class="empty">- - - None - - -</td>
                </tr>
                </tbody>
            </table>
            <a class="btn btn-primary" href="${contextPath}/reservation-request/create?type=ALIAS"><spring:message
                    code="views.button.create"/></a>
            <pagination-pages class="pull-right"><spring:message code="views.pagination.pages"/></pagination-pages>
            &nbsp;
        </div>

        <hr>

        <div ng-controller="PaginationController"
             ng-init="init('list_rooms', '${contextPath}/reservation-request/data?start=:start&count=:count&type=ROOM')"
             on-error="window.onError">
            <pagination-page-size class="pull-right"><spring:message
                    code="views.pagination.records"/></pagination-page-size>
            <h2><spring:message code="views.reservationRequest.type.rooms"/></h2>
            <table class="table table-striped table-hover">
                <thead>
                <tr>
                    <th width="85px"><spring:message code="views.reservationRequest.created"/></th>
                    <th><spring:message code="views.reservationRequest.user"/></th>
                    <th><spring:message code="views.reservationRequest.technology"/></th>
                    <th><spring:message code="views.reservationRequest.specification.room.participantCount"/></th>
                    <th width="180px"><spring:message code="views.reservationRequestList.earliestSlot"/></th>
                    <th><spring:message code="views.reservationRequest.description"/></th>
                    <th width="160px"><spring:message code="views.reservationRequestList.action"/></th>
                </tr>
                </thead>
                <tbody>
                <tr ng-repeat="reservationRequest in items">
                    <td>{{reservationRequest.created}}</td>
                    <td>{{reservationRequest.user}}</td>
                    <td>{{reservationRequest.technology}}</td>
                    <td>{{reservationRequest.participantCount}}</td>
                    <td>{{reservationRequest.earliestSlotStart}}<br/>{{reservationRequest.earliestSlotEnd}}</td>
                    <td>{{reservationRequest.description}}</td>
                    <td>
                        <a href="${contextPath}/reservation-request/detail/{{reservationRequest.id}}"><spring:message
                                code="views.reservationRequestList.action.detail"/></a>
                        | <a href="${contextPath}/reservation-request/modify/{{reservationRequest.id}}"><spring:message
                            code="views.reservationRequestList.action.modify"/></a>
                        | <a href="${contextPath}/reservation-request/delete/{{reservationRequest.id}}"><spring:message
                            code="views.reservationRequestList.action.delete"/></a>
                    </td>
                </tr>
                <tr ng-hide="items.length">
                    <td colspan="7" class="empty">- - - None - - -</td>
                </tr>
                </tbody>
            </table>
            <a class="btn btn-primary" href="${contextPath}/reservation-request/create?type=ROOM"><spring:message
                    code="views.button.create"/></a>
            <pagination-pages class="pull-right"><spring:message code="views.pagination.pages"/></pagination-pages>
            &nbsp;
        </div>

    </div>
</div>