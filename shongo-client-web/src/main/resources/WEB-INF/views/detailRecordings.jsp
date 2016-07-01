<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ page import="cz.cesnet.shongo.controller.ObjectType" %>
<%@ page import="cz.cesnet.shongo.api.AdobeConnectPermissions" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<security:accesscontrollist hasPermission="WRITE" domainObject="${objectId}" var="hasWritePermission"/>

<tag:url value="<%= ClientWebUrl.DETAIL_RECORDINGS_DATA %>" var="roomRecordingsUrl">
    <tag:param name="objectId" value=":id"/>
</tag:url>


<script type="text/javascript">
    function RecordingFolderController($scope, deviceDetector, $timeout) {
        $scope.deviceDetector = deviceDetector;
        $scope.changePermissions = function(url) {
            if ($scope.recordingFolderId == '') {
                return;
            };
            $.post(url, function(data){
                $timeout(function(){
                    $scope.isRecordingFolderPublic = data;
                    $scope.$$childHead.refresh();
                }, 0);
            });
        };

        $scope.resourceId = "${resourceId}";
        $scope.recordingFolderId = "${recordingFolderId}";
        $scope.isRecordingFolderPublic ="${isRecordingFolderPublic}";
    }

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

<div ng-controller="RecordingFolderController">
    <%-- Runtime management - Recordings --%>
    <div ng-controller="PaginationController"
         ng-init="init('recordings', '${roomRecordingsUrl}', {id: reservationRequest.recordingsObjectId})">
        <spring:eval var="permissionPublic" expression="T(cz.cesnet.shongo.api.jade.RecordingPermissionType).PUBLIC"/>
        <spring:eval var="permissionPrivate" expression="T(cz.cesnet.shongo.api.jade.RecordingPermissionType).PRIVATE"/>
        <spring:eval var="typeRecording" expression="T(cz.cesnet.shongo.api.jade.RecordingObjectType).RECORDING"/>
        <spring:eval var="typeRecordingFolder" expression="T(cz.cesnet.shongo.api.jade.RecordingObjectType).FOLDER"/>

        <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
        <spring:message code="views.button.refresh" var="paginationRefresh"/>
        <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
            <spring:message code="views.pagination.records"/>
        </pagination-page-size>
        <div class="pull-right pagination-page-size" ng-show="reservationRequest.technology == 'ADOBE_CONNECT'">
            <tag:url value="<%= ClientWebUrl.DETAIL_RECORDINGS_CHANGE_PERMISSIONS %>" var="recordingFolderMakePublicUrl">
                <tag:param name="objectId" value="' + reservationRequest.recordingsObjectId + '" escape="false"/>
                <tag:param name="resourceId" value="' + resourceId + '" escape="false"/>
                <tag:param name="recordingObjectType" value="${typeRecordingFolder}" escape="false"/>
                <tag:param name="recordingFolderId" value="' + recordingFolderId + '" escape="false"/>
                <tag:param name="recordingObjectPermissions" value="${permissionPublic}" escape="false"/>
            </tag:url>
            <tag:url value="<%= ClientWebUrl.DETAIL_RECORDINGS_CHANGE_PERMISSIONS %>" var="recordingMakeFolderPrivateUrl">
                <tag:param name="objectId" value="' + reservationRequest.recordingsObjectId + '" escape="false"/>
                <tag:param name="resourceId" value="' + resourceId + '" escape="false"/>
                <tag:param name="recordingObjectType" value="${typeRecordingFolder}" escape="false"/>
                <tag:param name="recordingFolderId" value="' + recordingFolderId + '" escape="false"/>
                <tag:param name="recordingObjectPermissions" value="${permissionPrivate}" escape="false"/>
            </tag:url>
            <spring:message var="recordingMakeFolderPublicTitle" code="views.list.action.makeFolderPublic.title"/>
            <spring:message var="recordingMakeFolderPrivateTitle" code="views.list.action.makeFolderPrivate.title"/>
            <span ng-show="{{recordingFolderId}}">
                <a class="btn btn-default" href="" ng-click="changePermissions('${recordingFolderMakePublicUrl}')" title="${recordingMakeFolderPublicTitle}" ng-hide="isRecordingFolderPublic"><i class="fa fa-lock"></i></a>
                <a class="btn btn-default" href="" ng-click="changePermissions('${recordingMakeFolderPrivateUrl}')" title="${recordingMakeFolderPrivateTitle}" ng-show="isRecordingFolderPublic"><i class="fa fa-unlock"></i></a>
            </span>
            <a class="btn btn-default disabled" ng-hide="{{recordingFolderId}}"><i class="fa fa-lock"></i></a>
        </div>
        <h2><spring:message code="views.room.recordings"/></h2>
        <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
        <div class="alert alert-warning" ng-show="({{deviceDetector.raw.os.windows}} || {{deviceDetector.raw.os.mac}}) && reservationRequest.technology == 'ADOBE_CONNECT'">
            <spring:message code="views.room.recording.ADOBE_CONNECT.makeOffline.title.win"/>
        </div>
        <div class="alert alert-danger" ng-show="!{{deviceDetector.raw.os.windows}} && !{{deviceDetector.raw.os.mac}} && reservationRequest.technology == 'ADOBE_CONNECT'">
            <spring:message code="views.room.recording.ADOBE_CONNECT.makeOffline.title.linux"/>
        </div>

        <div class="spinner" ng-hide="ready || errorContent"></div>


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
                    <%-- Show download URL if available, for Adobe Connect show view URL always --%>
                    <span ng-show="roomRecording.downloadUrl && reservationRequest.technology != 'ADOBE_CONNECT'"><a href="{{roomRecording.downloadUrl}}" target="_blank">{{roomRecording.filename}}</a></span>
                    <span ng-show="roomRecording.viewUrl && (roomRecording.downloadUrl == null || reservationRequest.technology == 'ADOBE_CONNECT')"><a href="{{roomRecording.viewUrl}}" target="_blank">{{roomRecording.filename}}</a></span>
                    <span ng-hide="roomRecording.downloadUrl || roomRecording.viewUrl"><spring:message code="views.room.recording.pending"/></span>
                </td>
                <td ng-controller="RoomRecordingActionController">
                    <%-- Show download link when available. --%>
                    <spring:message var="recordingDownloadTitle" code="views.list.action.download.title"/>
                    <span ng-hide="reservationRequest.technology == 'ADOBE_CONNECT' || !roomRecording.downloadUrl">
                        <a href="{{roomRecording.downloadUrl}}" title="${recordingDownloadTitle}" target="_blank"><i class="fa fa-download"></i></a>
                    </span>
                    <%-- For Adobe Connect show only in OS Windows or Mac (due to AC add-in) --%>
                    <span ng-show="roomRecording.downloadUrl && reservationRequest.technology == 'ADOBE_CONNECT' && ({{deviceDetector.raw.os.windows}} || {{deviceDetector.raw.os.mac}})">
                        <span data-hasqtip="1" tooltip="" selectable="false" position="" tooltip-width="" class="ng-scope">
                            <a href="{{roomRecording.downloadUrl}}" target="_blank"><i class="fa fa-download"></i></a>
                        </span>
                        <span class="hidden"><spring:message code="views.room.recording.ADOBE_CONNECT.makeOffline.description"/></span>
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
                            <tag:url value="<%= ClientWebUrl.DETAIL_RECORDINGS_CHANGE_PERMISSIONS %>" var="recordingMakePublicUrl">
                                <tag:param name="objectId" value="' + reservationRequest.recordingsObjectId + '" escape="false"/>
                                <tag:param name="resourceId" value="' + roomRecording.resourceId + '" escape="false"/>
                                <tag:param name="recordingObjectType" value="${typeRecording}" escape="false"/>
                                <tag:param name="recordingFolderId" value="' + recordingFolderId + '" escape="false"/>
                                <tag:param name="recordingId" value="' + roomRecording.id + '" escape="false"/>
                                <tag:param name="recordingObjectPermissions" value="${permissionPublic}" escape="false"/>
                            </tag:url>
                            <tag:url value="<%= ClientWebUrl.DETAIL_RECORDINGS_CHANGE_PERMISSIONS %>" var="recordingMakePrivateUrl">
                                <tag:param name="objectId" value="' + reservationRequest.recordingsObjectId + '" escape="false"/>
                                <tag:param name="resourceId" value="' + roomRecording.resourceId + '" escape="false"/>
                                <tag:param name="recordingObjectType" value="${typeRecording}" escape="false"/>
                                <tag:param name="recordingFolderId" value="' + recordingFolderId + '" escape="false"/>
                                <tag:param name="recordingId" value="' + roomRecording.id + '" escape="false"/>
                                <tag:param name="recordingObjectPermissions" value="${permissionPrivate}" escape="false"/>
                            </tag:url>
                            <spring:message var="recordingMakePublicTitle" code="views.list.action.makePublic.title"/>
                            <spring:message var="recordingMakePrivateTitle" code="views.list.action.makePrivate.title"/>
                            <span ng-hide="reservationRequest.technology != 'ADOBE_CONNECT'">
                                <span ng-hide="isRecordingFolderPublic">
                                    <a href="" ng-click="changePermissions('${recordingMakePublicUrl}')" title="${recordingMakePublicTitle}" ng-hide="roomRecording.isPublic"><i class="fa fa-lock"></i></a>
                                    <a href="" ng-click="changePermissions('${recordingMakePrivateUrl}')" title="${recordingMakePrivateTitle}" ng-show="roomRecording.isPublic"><i class="fa fa-unlock"></i></a>
                                </span>

                                <spring:message var="recordingFolderIsPublicTitle" code="views.list.action.recordingFolderIsPublic.title"/>
                                <i class="fa fa-unlock" title="${recordingFolderIsPublicTitle}" ng-show="isRecordingFolderPublic"></i>
                            </span>
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
</div>


