<%--
  -- Page for listing reservation requests for current user.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib prefix="app" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="urlListData">${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_LIST_DATA %></c:set>
<s:eval var="urlDetail" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetail(contextPath, '{{reservationRequest.id}}')"/>
<s:eval var="urlRoomDetail" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetail(contextPath, '{{reservationRequest.roomReservationRequestId}}')"/>
<c:set var="urlCreate">${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_CREATE %></c:set>
<s:eval var="urlModify" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestModify(contextPath, '{{reservationRequest.id}}')"/>
<s:eval var="urlDelete" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDelete(contextPath, '{{reservationRequest.id}}')"/>

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
             ng-init="init('reservationRequestList.aliases', '${urlListData}?start=:start&count=:count&type=PERMANENT_ROOM')"
             on-error="window.onError">
            <pagination-page-size class="pull-right">
                <s:message code="views.pagination.records"/>
            </pagination-page-size>
            <h2>
                <s:message code="views.reservationRequestList.permanentRooms"/>
                <app:help><s:message code="views.help.reservationRequest.specification.PERMANENT_ROOM"/></app:help>
            </h2>
            <table class="table table-striped table-hover">
                <thead>
                <tr>
                    <th width="85px"><s:message code="views.reservationRequest.dateTime"/></th>
                    <th><s:message code="views.reservationRequest.user"/></th>
                    <th><s:message code="views.reservationRequest.technology"/></th>
                    <th><s:message code="views.reservationRequest.specification.permanentRoomName"/></th>
                    <th width="150px"><s:message code="views.reservationRequestList.earliestSlot"/></th>
                    <th><s:message code="views.reservationRequest.allocationState"/></th>
                    <th><s:message code="views.reservationRequest.description"/></th>
                    <th width="160px"><s:message code="views.list.action"/></th>
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
                        <a href="${urlDetail}"><s:message code="views.list.action.show"/></a>
                        <span ng-show="reservationRequest.writable">
                            | <a href="${urlModify}"><s:message code="views.list.action.modify"/></a>
                            | <a href="${urlDelete}"><s:message code="views.list.action.delete"/></a>
                        </span>
                    </td>
                </tr>
                <tr ng-hide="items.length">
                    <td colspan="7" class="empty"><s:message code="views.list.none"/></td>
                </tr>
                </tbody>
            </table>
            <a class="btn btn-primary" href="${urlCreate}?type=PERMANENT_ROOM">
                <s:message code="views.button.create"/>
            </a>
            <pagination-pages class="pull-right"><s:message code="views.pagination.pages"/></pagination-pages>
            &nbsp;
        </div>

        <hr/>

        <div ng-controller="PaginationController"
             ng-init="init('reservationRequestList.rooms', '${urlListData}?start=:start&count=:count&type=ADHOC_ROOM')"
             on-error="window.onError">
            <pagination-page-size class="pull-right">
                <s:message code="views.pagination.records"/>
            </pagination-page-size>
            <h2>
                <s:message code="views.reservationRequestList.rooms"/>
                <app:help>
                    <s:message code="views.help.reservationRequest.specification.ADHOC_ROOM"/>
                    <br/>
                    <s:message code="views.help.reservationRequest.specification.PERMANENT_ROOM_CAPACITY"/>
                </app:help>
            </h2>
            <table class="table table-striped table-hover">
                <thead>
                <tr>
                    <th width="85px"><s:message code="views.reservationRequest.dateTime"/></th>
                    <th><s:message code="views.reservationRequest.user"/></th>
                    <th><s:message code="views.reservationRequestList.rooms.room"/></th>
                    <th width="100px"><s:message code="views.reservationRequest.specification.roomParticipantCount"/></th>
                    <th width="150px"><s:message code="views.reservationRequestList.earliestSlot"/></th>
                    <th><s:message code="views.reservationRequest.allocationState"/></th>
                    <th><s:message code="views.reservationRequest.description"/></th>
                    <th width="160px"><s:message code="views.list.action"/></th>
                </tr>
                </thead>
                <tbody>
                <tr ng-repeat="reservationRequest in items">
                    <td>{{reservationRequest.dateTime}}</td>
                    <td>{{reservationRequest.user}}</td>
                    <td ng-switch on="isEmpty(reservationRequest.roomReservationRequestId)">
                        <span ng-switch-when="true">{{reservationRequest.room}}</span>
                        <a ng-switch-when="false" href="${urlRoomDetail}">
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
                        <a href="${urlDetail}"><s:message code="views.list.action.show"/></a>
                        | <a href="${urlModify}"><s:message code="views.list.action.modify"/></a>
                        | <a href="${urlDelete}"><s:message code="views.list.action.delete"/></a>
                    </td>
                </tr>
                <tr ng-hide="items.length">
                    <td colspan="7" class="empty"><s:message code="views.list.none"/></td>
                </tr>
                </tbody>
            </table>
            <a class="btn btn-primary" href="${urlCreate}?type=ADHOC_ROOM">
                <s:message code="views.button.create"/>
            </a>
            <pagination-pages class="pull-right"><s:message code="views.pagination.pages"/></pagination-pages>
            &nbsp;
        </div>

    </div>
</div>