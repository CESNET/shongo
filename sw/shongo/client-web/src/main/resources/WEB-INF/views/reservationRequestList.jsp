<%--
  -- Page for listing reservation requests for current user.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<script type="text/javascript">
    // Angular application
    angular.module('ngReservationRequestList', ['ngPagination', 'ngTooltip']);

    // On error handler for listing reservation requests
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

<div ng-app="ngReservationRequestList" ng-controller="ReadyController">

    <div class="spinner" ng-hide="ready"></div>

    <div ng-show="ready">

        <div ng-controller="PaginationController"
             ng-init="init('reservationRequestList.aliases', '${contextPath}/reservation-request/data?start=:start&count=:count&type=PERMANENT_ROOM')"
             on-error="window.onError">
            <pagination-page-size class="pull-right">
                <spring:message code="views.pagination.records"/>
            </pagination-page-size>
            <h2>
                <spring:message code="views.reservationRequestList.permanentRooms"/>
                <app:help><spring:message code="views.help.reservationRequest.specification.PERMANENT_ROOM"/></app:help>
            </h2>
            <table class="table table-striped table-hover">
                <thead>
                <tr>
                    <th width="85px"><spring:message code="views.reservationRequest.dateTime"/></th>
                    <th><spring:message code="views.reservationRequest.user"/></th>
                    <th><spring:message code="views.reservationRequest.technology"/></th>
                    <th><spring:message code="views.reservationRequest.specification.permanentRoomName"/></th>
                    <th width="150px"><spring:message code="views.reservationRequestList.earliestSlot"/></th>
                    <th><spring:message code="views.reservationRequest.allocationState"/></th>
                    <th><spring:message code="views.reservationRequest.description"/></th>
                    <th width="160px"><spring:message code="views.list.action"/></th>
                </tr>
                </thead>
                <tbody>
                <tr ng-repeat="reservationRequest in items">
                    <td>{{reservationRequest.dateTime}}</td>
                    <td>{{reservationRequest.user}}</td>
                    <td>{{reservationRequest.technology}}</td>
                    <td>{{reservationRequest.roomName}}</td>
                    <td>{{reservationRequest.earliestSlotStart}}<br/>{{reservationRequest.earliestSlotEnd}}</td>
                    <td class="allocation-state">
                        <span class="{{reservationRequest.allocationState}}">
                            {{reservationRequest.allocationStateMessage}}
                        </span>
                    </td>
                    <td>{{reservationRequest.description}}</td>
                    <td>
                        <a href="${contextPath}/reservation-request/detail/{{reservationRequest.id}}"><spring:message
                                code="views.list.action.show"/></a>
                        <span ng-show="reservationRequest.writable">
                            | <a href="${contextPath}/reservation-request/modify/{{reservationRequest.id}}"><spring:message
                                code="views.list.action.modify"/></a>
                            | <a href="${contextPath}/reservation-request/delete/{{reservationRequest.id}}"><spring:message
                                code="views.list.action.delete"/></a>
                        </span>
                    </td>
                </tr>
                <tr ng-hide="items.length">
                    <td colspan="7" class="empty">- - - None - - -</td>
                </tr>
                </tbody>
            </table>
            <a class="btn btn-primary" href="${contextPath}/reservation-request/create?type=PERMANENT_ROOM">
                <spring:message code="views.button.create"/>
            </a>
            <pagination-pages class="pull-right"><spring:message code="views.pagination.pages"/></pagination-pages>
            &nbsp;
        </div>

        <hr/>

        <div ng-controller="PaginationController"
             ng-init="init('reservationRequestList.rooms', '${contextPath}/reservation-request/data?start=:start&count=:count&type=ADHOC_ROOM')"
             on-error="window.onError">
            <pagination-page-size class="pull-right">
                <spring:message code="views.pagination.records"/>
            </pagination-page-size>
            <h2>
                <spring:message code="views.reservationRequestList.rooms"/>
                <app:help>
                    <spring:message code="views.help.reservationRequest.specification.ADHOC_ROOM"/>
                    <br/>
                    <spring:message code="views.help.reservationRequest.specification.PERMANENT_ROOM_CAPACITY"/>
                </app:help>
            </h2>
            <table class="table table-striped table-hover">
                <thead>
                <tr>
                    <th width="85px"><spring:message code="views.reservationRequest.dateTime"/></th>
                    <th><spring:message code="views.reservationRequest.user"/></th>
                    <th><spring:message code="views.reservationRequestList.rooms.room"/></th>
                    <th width="100px"><spring:message code="views.reservationRequest.specification.roomParticipantCount"/></th>
                    <th width="150px"><spring:message code="views.reservationRequestList.earliestSlot"/></th>
                    <th><spring:message code="views.reservationRequest.allocationState"/></th>
                    <th><spring:message code="views.reservationRequest.description"/></th>
                    <th width="160px"><spring:message code="views.list.action"/></th>
                </tr>
                </thead>
                <tbody>
                <tr ng-repeat="reservationRequest in items">
                    <td>{{reservationRequest.dateTime}}</td>
                    <td>{{reservationRequest.user}}</td>
                    <td ng-switch on="isEmpty(reservationRequest.roomReservationRequestId)">
                        <span ng-switch-when="true">{{reservationRequest.room}}</span>
                        <a ng-switch-when="false" href="${contextPath}/reservation-request/detail/{{reservationRequest.roomReservationRequestId}}">
                            {{reservationRequest.room}}
                        </a>
                    </td>
                    <td>{{reservationRequest.participantCount}}</td>
                    <td>{{reservationRequest.earliestSlotStart}}<br/>{{reservationRequest.earliestSlotEnd}}</td>
                    <td class="allocation-state">
                        <span class="{{reservationRequest.allocationState}}">
                            {{reservationRequest.allocationStateMessage}}
                        </span>
                    </td>
                    <td>{{reservationRequest.description}}</td>
                    <td>
                        <a href="${contextPath}/reservation-request/detail/{{reservationRequest.id}}"><spring:message
                                code="views.list.action.show"/></a>
                        | <a href="${contextPath}/reservation-request/modify/{{reservationRequest.id}}"><spring:message
                            code="views.list.action.modify"/></a>
                        | <a href="${contextPath}/reservation-request/delete/{{reservationRequest.id}}"><spring:message
                            code="views.list.action.delete"/></a>
                    </td>
                </tr>
                <tr ng-hide="items.length">
                    <td colspan="7" class="empty">- - - None - - -</td>
                </tr>
                </tbody>
            </table>
            <a class="btn btn-primary" href="${contextPath}/reservation-request/create?type=ADHOC_ROOM">
                <spring:message code="views.button.create"/>
            </a>
            <pagination-pages class="pull-right"><spring:message code="views.pagination.pages"/></pagination-pages>
            &nbsp;
        </div>

    </div>
</div>