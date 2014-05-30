<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<security:accesscontrollist hasPermission="WRITE" domainObject="${objectId}" var="hasWritePermission"/>

<tag:url value="<%= ClientWebUrl.DETAIL_RECORDINGS_DATA %>" var="roomRecordingsUrl">
    <tag:param name="objectId" value=":id"/>
</tag:url>


<script type="text/javascript">
    function RoomRecordingActionController($scope, $timeout) {
        $scope.deleteRecording = function(filename, url) {
            var question = "<spring:message code="views.room.recording.deleteQuestion" arguments=":filename"/>";
            question = question.replace(":filename", filename);
            if (confirm(question)) {
                $.post(url, function(){
                    $timeout(function(){
                        $scope.$parent.refresh();
                    }, 0);
                });
            }

        };
    }
</script>

<%-- Runtime management - Recordings --%>
<div ng-controller="PaginationController"
     ng-init="init('recordings', '${roomRecordingsUrl}', {id: reservationRequest.recordingsObjectId})">
    <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
    <spring:message code="views.button.refresh" var="paginationRefresh"/>
    <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
        <spring:message code="views.pagination.records"/>
    </pagination-page-size>
    <h2><spring:message code="views.room.recordings"/></h2>
    <div class="spinner" ng-hide="ready || errorContent"></div>
    <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
    <table class="table table-striped table-hover" ng-show="ready">
        <thead>
        <tr>
            <%--
            <th><pagination-sort column="ID">ID</pagination-sort></th>
            <th><pagination-sort column="NAME">
                <spring:message code="views.room.recording.name"/></pagination-sort></th>--%>
            <th><pagination-sort column="START">
                <spring:message code="views.room.recording.date"/></pagination-sort></th>
            <th><pagination-sort column="DURATION">
                <spring:message code="views.room.recording.duration"/></pagination-sort></th>
            <th>
                <spring:message code="views.room.recording.url"/>
            </th>
            <th>
                <spring:message code="views.list.action"/>
                <pagination-sort-default class="pull-right"><spring:message code="views.pagination.defaultSorting"/></pagination-sort-default>
            </th>
        </tr>
        </thead>
        <tbody>
        <tr ng-repeat="roomRecording in items">
            <%--
            <td>
                {{roomRecording.id}}
            </td>
            <td>
                <span ng-show="roomRecording.description">
                    <tag:help label="{{roomRecording.name}}">
                        <strong><spring:message code="views.room.recording.description"/>:</strong>
                        {{roomRecording.description}}
                    </tag:help>
                </span>
                <span ng-hide="roomRecording.description">
                    {{roomRecording.name}}
                </span>
            </td>--%>
            <td>
                {{roomRecording.beginDate}}
            </td>
            <td>
                {{roomRecording.duration}}
            </td>
            <td>
                <span ng-show="roomRecording.downloadUrl"><a href="{{roomRecording.downloadUrl}}" target="_blank" download>{{roomRecording.filename}}</a></span>
                <span ng-show="roomRecording.viewUrl && roomRecording.downloadUrl == null"><a href="{{roomRecording.viewUrl}}" target="_blank">{{roomRecording.filename}}</a></span>
                <span ng-hide="roomRecording.downloadUrl || roomRecording.viewUrl"><spring:message code="views.room.recording.pending"/></span>
            </td>
            <td ng-controller="RoomRecordingActionController">
                <span ng-show="roomRecording.downloadUrl">
                    <spring:message var="recordingDownloadTitle" code="views.list.action.download.title"/>
                    <a href="{{roomRecording.downloadUrl}}" title="${recordingDownloadTitle}" target="_blank"><i class="fa fa-download"></i></a>
                </span>
                <span ng-show="roomRecording.viewUrl">
                    <spring:message var="recordingViewTitle" code="views.list.action.view.title"/>
                    <a href="{{roomRecording.viewUrl}}" title="${recordingViewTitle}" target="_blank"><i class="fa fa-eye"></i></a>
                </span>
                <c:if test="${hasWritePermission}">
                    <span ng-show="roomRecording.editUrl">
                        <spring:message var="recordingEditTitle" code="views.list.action.edit.title"/>
                        <a href="{{roomRecording.editUrl}}" title="${recordingEditTitle} ${roomRecording.viewUrl}" target="_blank"><i class="fa fa-pencil"></i></a>
                    </span>
                    <span ng-show="roomRecording.downloadUrl || roomRecording.viewUrl">
                        <tag:url value="<%= ClientWebUrl.DETAIL_RECORDING_DELETE %>" var="roomRecordingDeleteUrl">
                            <tag:param name="objectId" value="' + reservationRequest.recordingsObjectId + '" escape="false"/>
                            <tag:param name="resourceId" value="' + roomRecording.resourceId + '" escape="false"/>
                            <tag:param name="recordingId" value="' + roomRecording.id + '" escape="false"/>
                        </tag:url>
                        <spring:message var="recordingDeleteTitle" code="views.list.action.delete.title"/>
                        <a href="" ng-click="deleteRecording(roomRecording.filename, '${roomRecordingDeleteUrl}')" title="${recordingDeleteTitle}"><i class="fa fa-trash-o"></i></a>
                    </span>
                </c:if>
            </td>
        </tr>
        </tbody>
        <tbody>
        <tr ng-hide="items.length">
            <td colspan="4" class="empty"><spring:message code="views.list.none"/></td>
        </tr>
        </tbody>
    </table>
    <pagination-pages ng-show="ready"><spring:message code="views.pagination.pages"/></pagination-pages>
</div>

