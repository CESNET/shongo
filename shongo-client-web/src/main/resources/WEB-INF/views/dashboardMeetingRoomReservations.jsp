<%--
  -- Meeting rooms tab in dashboard.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<security:authentication property="principal.userId" var="userId"/>
<tag:url var="meetingRoomReservationListUrl" value="<%= ClientWebUrl.MEETING_ROOM_RESERVATION_LIST_DATA %>">
    <tag:param name="specification-type" value="MEETING_ROOM"/>
</tag:url>
<tag:url var="meetingRoomDetailUrl" value="<%= ClientWebUrl.DETAIL_VIEW %>">
    <tag:param name="objectId" value="{{room.id}}" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="meetingRoomModifyUrl" value="<%= ClientWebUrl.WIZARD_MODIFY %>">
    <tag:param name="reservationRequestId" value="{{room.id}}" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>
<tag:url var="meetingRoomDeleteUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DELETE %>">
    <tag:param name="reservationRequestId" value="{{room.id}}" escape="false"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>

<tag:url var="resourceListUrl" value="<%= ClientWebUrl.RESOURCE_LIST_DATA %>"/>

<script type="text/javascript">
    // Controller for filtering
    function DashboardReservationListController($scope, $cookieStore, $application, $timeout) {
        var dateTime = moment();

        $scope.resourceIdOptions = {
            escapeMarkup: function (markup) {
                return markup;
            },
            data: [
                <c:forEach items="${resources}" var="resource">
                {id: "${resource.key}", text: "${resource.value}"},
                </c:forEach>
            ]
        }

        $scope.reservationsFilter = {
            intervalFrom: dateTime.weekday(0).format("YYYY-MM-DD"),
            intervalTo: dateTime.weekday(6).format("YYYY-MM-DD"),
            resourceId: $scope.resourceIdOptions.data[0].id
        };

        $scope.$watchCollection('[reservationsFilter.intervalFrom, reservationsFilter.intervalTo]', function(newValues, oldValues, scope) {
            var startClass = document.getElementById("start").className;
            var endClass = document.getElementById("start").className;
            if (new Date(newValues[0]) > new Date(newValues[1])) {
                alert("<spring:message code='validation.field.invalidInterval'/>");
                document.getElementById("start").className += " fa-red";
                document.getElementById("end").className += " fa-red";
                return;
            }
            if ($scope.$parent.$tab.active) {
                document.getElementById("start").className = startClass.replace(" fa-red","");
                document.getElementById("end").className = endClass.replace(" fa-red","");
                $scope.$$childHead.refresh();
            }
        });
        $scope.$watch('reservationsFilter.resourceId', function(newResourceId, oldResourceId, scope) {
            if ($scope.$parent.$tab.active && typeof newResourceId == "object") {
                $scope.$$childHead.refresh();
            }
        });

        // URL for listing rooms
        $scope.getReservationListDataUrl = function() {
            var url = "${meetingRoomReservationListUrl}";
            if ($scope.reservationsFilter.intervalFrom != null && $scope.reservationsFilter.intervalFrom != "") {
                url += "&interval-from=" + $scope.reservationsFilter.intervalFrom + "T00:00:00";
            }
            if ($scope.reservationsFilter.intervalTo != null && $scope.reservationsFilter.intervalTo != "") {
                url += "&interval-to=" + $scope.reservationsFilter.intervalTo + "T23:59:59";
            }
            if ($scope.reservationsFilter.resourceId != null && $scope.reservationsFilter.resourceId.id != null) {
                url += "&resource-id=" + encodeURIComponent($scope.reservationsFilter.resourceId.id);
            }
            return url;
        };
    }
</script>

<div ng-controller="DashboardReservationListController">
<div ng-controller="PaginationController"
     ng-init="init('meetingRoomReservationList', getReservationListDataUrl, null, 'refresh-meetingRoomsReservations')">
    <form class="form-inline">
        <label for="meetingRoomResourceId"><spring:message code="views.room"/>:</label>
        <input id="meetingRoomResourceId" ng-model="reservationsFilter.resourceId" ui-select2="resourceIdOptions"/>

        &nbsp;
        <label for="start"><spring:message code="views.interval"/>:</label>
        <div class="input-group" style="display: inline-table;">
            <span class="input-group-addon">
                <spring:message code="views.interval.from"/>:
            </span>
            <input id="start" class="form-control form-picker" type="text" date-picker="true" readonly="true" style="width: 100px;" ng-model="reservationsFilter.intervalFrom"/>
        </div>
        <div class="input-group" style="display: inline-table">
            <span class="input-group-addon">
                <spring:message code="views.interval.to"/>:
            </span>
            <input id="end" class="form-control form-picker" type="text" date-picker="true" readonly="true" style="width: 100px;" ng-model="reservationsFilter.intervalTo"/>
        </div>

    </form>


    <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
    <spring:message code="views.button.refresh" var="paginationRefresh"/>
    <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
        <spring:message code="views.pagination.records"/>
    </pagination-page-size>
    <div class="alert alert-warning"><spring:message code="views.index.meetingRooms.description"/></div>
    <div class="spinner" ng-hide="ready || errorContent"></div>
    <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
    <table class="table table-striped table-hover" ng-show="ready">
        <thead>
        <tr>
            <th>
                <pagination-sort column="ROOM_NAME"><spring:message code="views.reservationRequestList.resourceName"/></pagination-sort>
            </th>
            <th width="200px">
                <pagination-sort column="OWNER"><spring:message code="views.room.bookedBy"/></pagination-sort>
            </th>
            <th>
                <pagination-sort column="SLOT"><spring:message code="views.room.slot"/></pagination-sort>
            </th>
            <%--th width="200px">
                <pagination-sort column="STATE"><spring:message code="views.room.state"/></pagination-sort>
            </th--%>
            <th>
                <spring:message code="views.room.description"/>
            </th>
            <th style="min-width: 95px; width: 95px;">
                <spring:message code="views.list.action"/>
                <pagination-sort-default class="pull-right"><spring:message code="views.pagination.defaultSorting"/></pagination-sort-default>
            </th>
        </tr>
        </thead>
        <tbody>
        <tr ng-repeat="room in items" ng-class="{'deprecated': room.isDeprecated}">
            <td>
                <tag:help label="{{room.resourceName}}" selectable="true">
                    <span>
                        <strong><spring:message code="views.room.roomDescription"/></strong>
                        <br />
                        {{room.resourceDescription}}
                    </span>
                </tag:help>
            </td>
            <td>
                <tag:help label="{{room.ownerName}}" selectable="true">
                    <span>
                        <strong><spring:message code="views.room.ownerEmail"/></strong>
                        <br />
                        <a href="mailto: {{room.ownerEmail}}">{{room.ownerEmail}}</a>
                    </span>
                </tag:help>
            </td>
            <td>
                <span ng-bind-html="room.slot"></span>
                <%--span ng-show="room.futureSlotCount">
                    <spring:message code="views.reservationRequestList.slotMore" var="slotMore" arguments="{{room.futureSlotCount}}"/>
                    <tag:help label="(${slotMore})" cssClass="push-top">
                        <spring:message code="views.reservationRequestList.slotMoreHelp"/>
                    </tag:help>
                </span--%>
            </td>
            <%--td class="reservation-request-state">
                <tag:help label="{{room.stateMessage}}" cssClass="{{room.state}}">
                    <span>{{room.stateHelp}}</span>
                </tag:help>
            </td--%>
            <td>{{room.description}}</td>
            <td>
                <%--tag:listAction code="show" titleCode="views.index.reservations.showDetail" url="${meetingRoomDetailUrl}" tabindex="1"/>
                <span ng-show="room.isWritable">
                    <span ng-hide="room.state == 'ALLOCATED_FINISHED'">
                        | <tag:listAction code="modify" url="${meetingRoomModifyUrl}" tabindex="2"/>
                    </span>
                    | <tag:listAction code="delete" url="${meetingRoomDeleteUrl}" tabindex="3"/>
                </span--%>
            </td>
        </tr>
        </tbody>
        <tbody>
        <tr ng-hide="items.length">
            <td colspan="6" class="empty"><spring:message code="views.list.none"/></td>
        </tr>
        </tbody>
    </table>
    <pagination-pages ng-show="ready"><spring:message code="views.pagination.pages"/></pagination-pages>
</div>
</div>