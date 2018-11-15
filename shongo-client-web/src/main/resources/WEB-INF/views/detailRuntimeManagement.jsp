<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<security:accesscontrollist hasPermission="WRITE" domainObject="${room}" var="isWritable"/>
<c:if test="${room.state == 'STOPPED'}">
    <c:set var="isWritable" value="false"/>
</c:if>
<c:if test="${isProvidable}">
    <security:accesscontrollist hasPermission="PROVIDE_RESERVATION_REQUEST" domainObject="${reservationRequestId}" var="isProvidable"/>
</c:if>

<tag:url var="userListUrl" value="<%= ClientWebUrl.USER_LIST_DATA %>"/>

<c:if test="${roomRuntime != null && room.available}">
    <c:set var="isRoomAvailable" value="true"/>
</c:if>

<%-- Recording Control Panel --%>
<c:if test="${isRoomAvailable && room.recordingService != null}">
    <script type="text/javascript">
        <tag:url value="<%= ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_RECORDING_START %>" var="startRecordingUrl">
        <tag:param name="objectId" value="${room.id}"/>
        <tag:param name="executableId" value="${room.recordingService.executableId}"/>
        <tag:param name="executableServiceId" value="${room.recordingService.id}"/>
        </tag:url>
        <tag:url value="<%= ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_RECORDING_STOP %>" var="stopRecordingUrl">
        <tag:param name="objectId" value="${room.id}"/>
        <tag:param name="executableId" value="${room.recordingService.executableId}"/>
        <tag:param name="executableServiceId" value="${room.recordingService.id}"/>
        </tag:url>
        function RoomRecordingController($scope, $timeout, $application) {
            $scope.isRecordingActive = ${room.recordingService.active};
            $scope.recordingError = null;
            $scope.recordingRequestActive = false;
            $scope.startRecording = function() {
                if ($scope.recordingRequestActive) {
                    return;
                }
                $scope.recordingRequestActive = true;
                $.post("${startRecordingUrl}", {
                }).done(function(result){
                    $timeout(function(){
                        if (typeof(result) == "object" && result["error"] != null) {
                            $scope.recordingError = result["error"];
                        }
                        else {
                            $scope.isRecordingActive = true;
                            $scope.recordingError = null;
                        }
                        $scope.recordingRequestActive = false;
                    }, 0);
                }).fail($application.handleAjaxFailure);
            };
            $scope.stopRecording = function() {
                if ($scope.recordingRequestActive) {
                    return;
                }
                $scope.recordingRequestActive = true;
                $.post("${stopRecordingUrl}", {
                }).done(function(result){
                    $timeout(function(){
                        if (typeof(result) == "object" && result["error"] != null) {
                            $scope.recordingError = result["error"];
                        }
                        else {
                            $scope.isRecordingActive = false;
                            $scope.recordingError = null;
                            // Refresh recordings tab when it becomes active
                            $scope.$parent.$parent.refreshRecordings = true;
                        }
                        $scope.recordingRequestActive = false;
                    }, 0);
                }).fail($application.handleAjaxFailure);
            };
        }
    </script>
    <div class="pull-right" ng-controller="RoomRecordingController">
        <div style="text-align: right;">
            <spring:message code="views.room.recording.started" var="recordingStarted"/>
            <a class="btn btn-default" href="" ng-click="startRecording()" ng-hide="isRecordingActive" ng-disabled="recordingRequestActive">
                <i class="icon-recording-start"></i>
                <spring:message code="views.room.recording.start"/>
            </a>
            <a class="btn btn-default" href="" ng-click="stopRecording()" title="${recordingStarted}" ng-show="isRecordingActive"  ng-disabled="recordingRequestActive">
                <i class="icon-recording-stop"></i>
                <spring:message code="views.room.recording.stop"/>
            </a>
        </div>
        <div style="max-width: 300px; margin-top: 10px;" class="alert alert-danger" ng-show="recordingError != null">
            {{recordingError}}
        </div>
    </div>
</c:if>

<%-- Mangement Detail --%>
<script type="text/javascript">
    function RoomController($scope, $application) {
    <c:if test="${roomRuntime != null}">
        $scope.layout = "${roomRuntime.layout != null ? roomRuntime.layout : 'OTHER'}";
        $scope.contentImportant = "${contentImportant != null ? contentImportant : 'false'}";

        <tag:url var="modifyRoomUrl" value="<%= ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_MODIFY %>">
            <tag:param name="objectId" value="${room.id}"/>
            <tag:param name="layout" value=":layout"/>
        </tag:url>
        <tag:url var="makeContentImportantUrl" value="<%= ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_MODIFY %>">
            <tag:param name="objectId" value="${room.id}"/>
            <tag:param name="contentImportant" value="true"/>
        </tag:url>
        <tag:url var="makeContentUnimportantUrl" value="<%= ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_MODIFY %>">
            <tag:param name="objectId" value="${room.id}"/>
            <tag:param name="contentImportant" value="false"/>
        </tag:url>
        $scope.$watch("layout", function (newVal, oldVal) {
            if (newVal != oldVal) {
                var url = "${modifyRoomUrl}";
                url = url.replace(":layout", newVal);
                $.post(url).fail($application.handleAjaxFailure);
            }
        });
        $scope.makeContentImportant = function () {
            var url = "${makeContentImportantUrl}";
            $.get(url, function (data){
                $scope.contentImportant = 'true';
                $scope.refreshTab('runtimeManagement');
            }).fail($application.handleAjaxFailure);
        };
        $scope.makeContentUnimportant = function () {
            var url = "${makeContentUnimportantUrl}";
            $.get(url, function (data){
                $scope.contentImportant = 'false';
                $scope.refreshTab('runtimeManagement');
            }).fail($application.handleAjaxFailure);
        };
    </c:if>

        $scope.formatGroup = function(groupId, event) {
            $.ajax("${userListUrl}?groupId=" + groupId, {
                dataType: "json"
            }).done(function (data) {
                content = "<b><spring:message code="views.userRole.groupMembers"/>:</b><br/>";
                content += $application.formatUsers(data, "<spring:message code="views.userRole.groupMembers.none"/>");
                event.setResult(content);
            }).fail($application.handleAjaxFailure);
            return "<spring:message code="views.loading"/>";
        };
    }
    function RoomDetailController($scope) {
        $scope.show = false;
    }
</script>
<dl class="dl-horizontal" ng-controller="RoomController">

    <dt><spring:message code="views.room.technology"/>:</dt>
    <dd><spring:message code="${room.technology.titleCode}"/></dd>

    <dt><spring:message code="views.room.name"/>:</dt>
    <dd>${room.name}</dd>

    <dt><spring:message code="views.room.slot"/>:</dt>
    <dd>
        <tag:format value="${room.slot}" multiline="true" pre="${room.slotBefore}" post="${room.slotAfter}"/>
    </dd>

    <dt><spring:message code="views.room.state"/>:</dt>
    <dd class="room-state">
        <spring:message code="views.executable.roomState.${room.type}.${room.state}" var="roomStateLabel"/>
        <spring:message code="views.executable.roomStateHelp.${room.type}.${room.state}" var="roomStateHelp"/>
        <tag:help label="${roomStateLabel}" cssClass="${room.state}">
            <span>${roomStateHelp}</span>
            <c:if test="${not empty room.stateReport}">
                <pre>${room.stateReport}</pre>
            </c:if>
        </tag:help>
    </dd>

    <c:if test="${room.state.available}">
        <dt><spring:message code="views.room.licenseCount"/>:</dt>
        <dd>
                ${room.licenseCount}
            <c:if test="${room.licenseCountUntil != null}">
                (<spring:message code="views.room.licenseCountUntil"/>&nbsp;<tag:format value="${room.licenseCountUntil}"/>)
            </c:if>
        </dd>
    </c:if>

    <dt><spring:message code="views.room.aliases"/>:</dt>
    <dd>
        <tag:help label="${room.aliases}" selectable="true">
            <c:set value="${room.aliasesDescription}" var="roomAliasesDescription"/>
            <c:if test="${not empty roomAliasesDescription}">
                ${roomAliasesDescription}
            </c:if>
        </tag:help>
    </dd>

    <c:if test="${not empty room.pin}">
        <dt><spring:message code="views.room.pin"/>:</dt>
        <dd>${room.pin}</dd>
    </c:if>

    <c:if test="${roomRuntime != null}">
        <c:if test="${room.technology == 'H323_SIP'}">
            <dt class="control-label"><spring:message code="views.room.layout"/>:</dt>
            <dd>
                <tag:roomLayout id="roomLayout" model="layout" width="220px"/>
            </dd>
            <dt class="control-label"><spring:message code="views.room.content.title"/>:</dt>
            <dd>
                <a class="btn btn-default" href="" ng-click="makeContentImportant()" ng-hide="contentImportant" ng-disabled="${!room.state.available}">
                    <i class="fa fa-picture-o fa-green" /> <spring:message code="views.room.content.makeImportant"/>
                </a>
                <a class="btn btn-default" href="" ng-click="makeContentUnimportant()" ng-show="contentImportant" ng-disabled="${!room.state.available}">
                    <i class="fa fa-picture-o fa-red" /> <spring:message code="views.room.content.makeUnimportant"/>
                </a>
            </dd>
        </c:if>
    </c:if>

    <div ng-controller="RoomDetailController">

        <div ng-show="show">

            <hr/>

            <dt><spring:message code="views.room.identifier"/>:</dt>
            <dd>${room.id}</dd>

            <dt><spring:message code="views.reservationRequest"/>:</dt>
            <dd>${reservationRequestId}</dd>
        </div>

        <dt></dt>
        <dd>
            <a href="" ng-click="show = true" ng-show="!show"><spring:message code="views.button.showMoreDetail"/></a>
            <a href="" ng-click="show = false" ng-show="show"><spring:message code="views.button.hideMoreDetail"/></a>
        </dd>

    </div>

</dl>

<%-- Room state isn't available --%>
<c:if test="${room.available && roomRuntime == null}">
    <jsp:include page="errorRoomNotAvailable.jsp"/>
</c:if>

<%-- Participants --%>
<c:if test="${isRoomAvailable}">
    <script type="text/javascript">
        function RoomParticipantsController($scope, $timeout, $application) {
            /**
             * @param url for modifying participants
             */
            $scope.modifyByUrl = function(url) {
                if (!$scope.items.length) {
                    return;
                }
                $.post(url, function(){
                    $timeout(function(){
                        $scope.$parent.refresh();
                    }, 0);
                }).fail($application.handleAjaxFailure);
            };
        }
        function RoomParticipantController($scope, $timeout, $application, $roomParticipantDialog) {
            var roomParticipantAttributes = ["name", "microphoneEnabled", "microphoneLevel", "videoEnabled"];

            /**
             * @param url for modifying participant
             */
            $scope.modifyByUrl = function(url) {
                $.post(url, function(){
                    $timeout(function(){
                        $scope.$parent.refresh();
                    }, 0);
                }).fail($application.handleAjaxFailure);
            };

            /**
             * @param roomParticipant
             * @returns {Boolean} whether given {@code roomParticipant} can have some attributes edited
             */
            $scope.isEditable = function(roomParticipant) {
                for (var index in roomParticipantAttributes) {
                    var roomParticipantAttribute = roomParticipantAttributes[index];
                    if (roomParticipantAttribute != 'name' && roomParticipant[roomParticipantAttribute] != null) {
                        return true;
                    }
                }
                return ${room.technology == 'H323_SIP'};
            };

            /**
             * Show modify dialog.
             */
            $scope.modify = function(roomParticipant, url) {
                var roomParticipantDialog = $roomParticipantDialog.modify(roomParticipant);
                roomParticipantDialog.result.then(function(result) {
                    angular.copy(result, roomParticipant);
                    var urlQuery = {};
                    for (var attribute in roomParticipant) {
                        if (roomParticipantAttributes.indexOf(attribute) != -1) {
                            var value = roomParticipant[attribute];
                            if (value != null) {
                                urlQuery[attribute] = value;
                            }
                        }
                    }
                    url += "?" + $.param(urlQuery);
                    $.post(url).fail($application.handleAjaxFailure);
                });
            };
        }
    </script>
    <tag:url value="<%= ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_PARTICIPANTS_DATA%>" var="roomParticipantsUrl">
        <tag:param name="objectId" value=":id"/>
    </tag:url>
    <div id = "roomParticipants" ng-controller="PaginationController"
         ng-init="init('room.participants', '${roomParticipantsUrl}', {id: '${room.id}'})">
        <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
        <spring:message code="views.button.refresh" var="paginationRefresh"/>
        <c:if test="${room.technology == 'H323_SIP'}">
            <div ng-controller="RoomParticipantsController" class="pull-right">
                <tag:url var="participantsModifyUrl" value="<%= ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_PARTICIPANT_MODIFY %>">
                    <tag:param name="objectId" value="${room.id}"/>
                    <tag:param name="participantId" value="*" escape="false"/>
                </tag:url>
                <spring:message code="views.room.currentParticipants.microphoneEnable.help" var="microphoneEnableHelp"/>
                <a class="btn btn-default" href="" ng-click="modifyByUrl('${participantsModifyUrl}?microphoneEnabled=true')" title="${microphoneEnableHelp}" ng-disabled="!items.length">
                    <i class="fa fa-microphone fa-green"></i>
                    <spring:message code="views.room.currentParticipants.microphoneEnable"/>
                </a>
                <spring:message code="views.room.currentParticipants.microphoneDisable.help" var="microphoneDisableHelp"/>
                <a class="btn btn-default" href="" ng-click="modifyByUrl('${participantsModifyUrl}?microphoneEnabled=false')" title="${microphoneDisableHelp}" ng-disabled="!items.length">
                    <i class="fa fa-microphone-slash fa-red"></i>
                    <spring:message code="views.room.currentParticipants.microphoneDisable"/>
                </a>
            </div>
        </c:if>
        <h2><spring:message code="views.room.currentParticipants"/></h2>
        <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
            <spring:message code="views.pagination.records"/>
        </pagination-page-size>
        <p><spring:message code="views.room.currentParticipants.help"/></p>
        <div class="spinner" ng-hide="ready || errorContent"></div>
        <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
        <table class="table table-striped table-hover" ng-show="ready">
            <thead>
            <tr>
                <th><spring:message code="views.room.currentParticipant.name"/></th>
                <c:if test="${room.technology == 'H323_SIP'}">
                    <th><spring:message code="views.room.currentParticipant.alias"/></th>
                    <th style="min-width: 150px; width: 150px;">
                        <spring:message code="views.room.currentParticipant.preview"/>
                    </th>
                </c:if>
                <c:if test="${room.technology == 'ADOBE_CONNECT'}">
                    <th><spring:message code="views.room.currentParticipant.role"/></th>
                    <th><spring:message code="views.room.currentParticipant.email"/></th>
                </c:if>
                <c:if test="${room.technology == 'PEXIP'}">
                    <th><spring:message code="views.room.currentParticipant.role"/></th>
                    <th><spring:message code="views.room.currentParticipant.protocol"/></th>
                </c:if>
                <c:if test="${isWritable}">
                    <th style="min-width: 95px; width: 95px;"><spring:message code="views.list.action"/></th>
                </c:if>
            </tr>
            </thead>
            <tbody>
            <tr ng-repeat="roomParticipant in items">
                <td>
                    {{roomParticipant.name}}
                </td>
                <c:if test="${room.technology == 'H323_SIP'}">
                    <td>
                        {{roomParticipant.alias}}
                    </td>
                    <td>
                        <span ng-show="roomParticipant.videoEnabled && roomParticipant.videoSnapshot">
                            <tag:url var="participantVideoSnapshotUrl" value="<%= ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_PARTICIPANT_VIDEO_SNAPSHOT %>">
                                <tag:param name="objectId" value="${room.id}"/>
                                <tag:param name="participantId" value="{{roomParticipant.id}}" escape="false"/>
                            </tag:url>
                            <img ng-src="${participantVideoSnapshotUrl}" style="height: 40px;"/>
                        </span>
                    </td>
                </c:if>
                <c:if test="${room.technology == 'ADOBE_CONNECT'}">
                    <td>
                        {{roomParticipant.role}}
                    </td>
                    <td>
                        {{roomParticipant.email}}
                    </td>
                </c:if>
                <c:if test="${room.technology == 'PEXIP'}">
                    <td>
                        {{roomParticipant.role}}
                    </td>
                    <td>
                        {{roomParticipant.protocol}}
                    </td>
                </c:if>
                    <%-- Actions --%>
                <c:if test="${isWritable}">
                    <td ng-controller="RoomParticipantController">
                        <tag:url var="participantModifyUrl" value="<%= ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_PARTICIPANT_MODIFY %>">
                            <tag:param name="objectId" value="${room.id}"/>
                            <tag:param name="participantId" value="' + roomParticipant.id + '" escape="false"/>
                        </tag:url>
                        <%-- Mute audio --%>
                        <span ng-show="roomParticipant.microphoneEnabled != null">
                            <spring:message var="participantMicrophoneEnableTitle" code="views.room.currentParticipant.microphoneEnabled.enable"/>
                            <spring:message var="participantMicrophoneDisableTitle" code="views.room.currentParticipant.microphoneEnabled.disable"/>
                            <a href="" ng-click="modifyByUrl('${participantModifyUrl}?microphoneEnabled=' + !roomParticipant.microphoneEnabled)" title="{{roomParticipant.microphoneEnabled ? '${participantMicrophoneDisableTitle}' : '${participantMicrophoneEnableTitle}'}}"><i class="fa fa-microphone{{roomParticipant.microphoneEnabled ? '' : '-slash'}} fa-{{roomParticipant.microphoneEnabled ? 'green' : 'red'}}"></i></a>&nbsp;
                        </span>
                        <%-- Mute video --%>
                        <span ng-show="roomParticipant.videoEnabled != null">
                            <spring:message var="participantVideoDisableTitle" code="views.room.currentParticipant.videoEnabled.disable"/>
                            <spring:message var="participantVideoEnableTitle" code="views.room.currentParticipant.videoEnabled.enable"/>
                            <a href="" ng-click="modifyByUrl('${participantModifyUrl}?videoEnabled=' + !roomParticipant.videoEnabled)" title="{{roomParticipant.videoEnabled ? '${participantVideoDisableTitle}' : '${participantVideoEnableTitle}'}}"><i class="fa fa-{{roomParticipant.videoEnabled ? 'video-camera' : 'minus-square'}} fa-{{roomParticipant.videoEnabled ? 'green' : 'red'}}"></i></a>&nbsp;
                        </span>
                        <%-- Modify dialog --%>
                        <span ng-show="isEditable(roomParticipant)">
                            <spring:message var="participantModifyTitle" code="views.button.modify"/>
                            <a href="" ng-click="modify(roomParticipant, '${participantModifyUrl}')" title="${participantModifyTitle}"><i class="fa fa-pencil"></i></a>&nbsp;
                        </span>
                        <%-- Disconnect --%>
                        <tag:url var="disconnectParticipantUrl" value="<%= ClientWebUrl.DETAIL_RUNTIME_MANAGEMENT_PARTICIPANT_DISCONNECT %>">
                            <tag:param name="objectId" value="${room.id}"/>
                            <tag:param name="participantId" value="' + roomParticipant.id + '" escape="false"/>
                        </tag:url>
                        <spring:message var="participantDisconnectTitle" code="views.room.currentParticipant.disconnect"/>
                        <a href="" ng-click="modifyByUrl('${disconnectParticipantUrl}')" title="${participantDisconnectTitle}"><i class="fa fa-times"></i></a>
                    </td>
                </c:if>
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
</c:if>

<%-- Actions --%>
<div class="table-actions pull-right">
    <spring:message code="views.detail.tab.refreshHelp" var="refreshTabHelp"/>
    <a class="btn btn-default" href="#" title="${refreshTabHelp}" ng-click="refreshTab('runtimeManagement')">
        <spring:message code="views.button.refresh"/>
    </a>
</div>
<div class="clearfix"></div>
