<%--
  -- Participation tab in dashboard.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<security:authentication property="principal.userId" var="userId"/>
<tag:url var="participantRoomListUrl" value="<%= ClientWebUrl.ROOM_LIST_DATA %>">
    <tag:param name="participant-user-id" value="${userId}"/>
</tag:url>
<tag:url var="participantRoomUrl" value="<%= ClientWebUrl.ROOM_DATA %>">
    <tag:param name="objectId" value=":objectId"/>
</tag:url>

<script type="text/javascript">
    function ParticipantRoomController($scope, $application) {
        $scope.formatAliases = function(executableId, event){
            $.ajax("${participantRoomUrl}".replace(":objectId", executableId), {
                dataType: "json"
            }).done(function (data) {
                var result = data.aliases;
                if (data.pin != null) {
                    result += "<strong><spring:message code="views.room.pin"/>:</strong> " + data.pin;
                }
                event.setResult(result);
            }).fail($application.handleAjaxFailure);
            return "<spring:message code="views.loading"/>";
        };
    }
</script>

<div ng-controller="PaginationController"
     ng-init="init('roomList', '${participantRoomListUrl}', null, 'refresh-participation')">
    <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
    <spring:message code="views.button.refresh" var="paginationRefresh"/>
    <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
        <spring:message code="views.pagination.records"/>
    </pagination-page-size>
    <div class="alert alert-warning"><spring:message code="views.index.participation.description"/></div>
    <div class="spinner" ng-hide="ready || errorContent"></div>
    <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
    <table class="table table-striped table-hover" ng-show="ready">
        <thead>
        <tr>
            <th>
                <pagination-sort column="ROOM_NAME"><spring:message code="views.reservationRequestList.roomName"/></pagination-sort>
            </th>
            <th>
                <pagination-sort column="ROOM_TECHNOLOGY">
                    <spring:message code="views.room.technology"/>
                </pagination-sort>
            </th>
            <th>
                <pagination-sort column="SLOT"><spring:message code="views.room.slot"/></pagination-sort>
            </th>
            <th width="200px">
                <pagination-sort column="STATE"><spring:message code="views.room.state"/></pagination-sort><tag:helpRoomState/>
            </th>
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
            <td ng-controller="ParticipantRoomController">
                <spring:message code="views.room.name.adhoc" var="roomNameAdhoc"/>
                <tag:help label="{{room.type == 'ROOM' ? '${roomNameAdhoc}' : room.name}}" content="formatAliases(room.id, event)" selectable="true"/>
            </td>
            <td>{{room.technologyTitle}}</td>
            <td><span ng-bind-html="room.slot"></span></td>
            <td class="room-state">
                <tag:help label="{{room.stateMessage}}" cssClass="{{room.state}}">
                    <span>{{room.stateHelp}}</span>
                </tag:help>
            </td>
            <td>{{room.description}}</td>
            <td>
                <tag:url var="detailRuntimeManagementEnterUrl" value="<%= ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_ENTER %>">
                    <tag:param name="objectId" value="{{room.id}}" escape="false"/>
                </tag:url>
                <span ng-show="room.stateAvailable && room.technology == 'ADOBE_CONNECT'">
                    <tag:listAction code="enterRoom" url="${detailRuntimeManagementEnterUrl}" target="_blank" tabindex="4"/>
                </span>
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