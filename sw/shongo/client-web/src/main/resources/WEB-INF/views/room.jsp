<%--
  -- Page for displaying details about a single reservation request.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<security:accesscontrollist hasPermission="WRITE" domainObject="${room}" var="isWritable"/>
<c:if test="${room.state == 'STOPPED'}">
    <c:set var="isWritable" value="false"/>
</c:if>

<tag:url var="reservationRequestDetailUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DETAIL %>">
    <tag:param name="reservationRequestId" value="${reservationRequestId}"/>
    <tag:param name="back-url" value="${requestScope.requestUrl}"/>
</tag:url>

<script type="text/javascript">
    var module = angular.module('jsp:room', ['jsp:roomParticipantDialog', 'ngTooltip', 'ngPagination']);
    module.controller('RoomController', function($scope) {
        <c:if test="${roomRuntime != null}">
            $scope.layout = "${roomRuntime.layout != null ? roomRuntime.layout : 'OTHER'}";
            <tag:url var="modifyRoomUrl" value="<%= ClientWebUrl.ROOM_MANAGEMENT_MODIFY %>">
                <tag:param name="roomId" value="${room.id}"/>
                <tag:param name="back-url" value="${requestScope.requestUrl}"/>
                <tag:param name="layout" value=":layout"/>
            </tag:url>
            $scope.$watch("layout", function (newVal, oldVal) {
                if (newVal != oldVal) {
                    var url = "${modifyRoomUrl}";
                    url = url.replace(":layout", newVal);
                    $.post(url);
                }
            });
        </c:if>
    });
    module.controller('RoomDetailController', function($scope) {
        $scope.show = false;
    });
    module.controller('RoomParticipantController', function($scope, $timeout, $roomParticipantDialog) {
        var roomParticipantAttributes = ["layout", "microphoneLevel", "audioMuted", "videoMuted"];

        /**
         * @param url for modifying participant
         */
        $scope.modifyByUrl = function(url) {
            $.post(url, function(){
                $timeout(function(){
                    $scope.$parent.refresh();
                }, 0);
            });
        };

        /**
         * @param roomParticipant
         * @returns {Boolean} whether given {@code roomParticipant} can have some attributes edited
         */
        $scope.isEditable = function(roomParticipant) {
            for (var index in roomParticipantAttributes) {
                if (roomParticipant[roomParticipantAttributes[index]] != null) {
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
                $.post(url);
            });
        };
    });
</script>

<h1>
<c:choose>
    <c:when test="${room.type == 'PERMANENT_ROOM'}">
        <spring:message code="views.room.heading" arguments="${room.name}"/>
    </c:when>
    <c:otherwise>
        <spring:message code="views.room.headingAdhoc"/>
    </c:otherwise>
</c:choose>
</h1>

<div ng-app="jsp:room">

    <jsp:include page="../templates/roomParticipantDialog.jsp"/>

    <%-- Detail of room --%>
    <dl class="dl-horizontal" ng-controller="RoomController">

        <dt><spring:message code="views.room.technology"/>:</dt>
        <dd>${room.technology.title}</dd>

        <dt><spring:message code="views.room.name"/>:</dt>
        <dd>${room.name}</dd>

        <dt><spring:message code="views.room.slot"/>:</dt>
        <dd>
            <tag:format value="${room.slot}" multiline="true"/>
        </dd>

        <dt><spring:message code="views.room.state"/>:</dt>
        <dd class="room-state">
            <spring:message code="views.executable.roomState.${room.type}.${room.state}" var="roomStateLabel"/>
            <spring:message code="views.executable.roomStateHelp.${room.type}.${room.state}" var="roomStateHelp"/>
            <tag:help label="${roomStateLabel}" labelClass="${room.state}">
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
            <tag:help label="${room.aliases}">
                <c:set value="${room.aliasesDescription}" var="roomAliasesDescription"/>
                <c:if test="${not empty roomAliasesDescription}">
                    ${roomAliasesDescription}
                </c:if>
            </tag:help>
        </dd>

        <c:if test="${not empty userRoles}">
            <dt><spring:message code="views.reservationRequest.userRoles"/>:</dt>
            <dd>
                <c:forEach items="${userRoles}" var="userRole" varStatus="status">
                    ${userRole.user.fullName} (<spring:message code="views.userRole.role.${userRole.role}"/>)<c:if test="${!status.last}">, </c:if>
                </c:forEach>
                <c:if test="${isWritable}">
                    <tag:url var="modifyUserRolesUrl" value="<%= ClientWebUrl.USER_ROLE_LIST %>">
                        <tag:param name="entityId" value="${reservationRequestId}"/>
                        <tag:param name="back-url" value="${requestUrl}"/>
                    </tag:url>
                    (<a href="${modifyUserRolesUrl}"><spring:message code="views.reservationRequest.userRoles.modify"/></a>)
                </c:if>
            </dd>
        </c:if>

        <dd>
            <a href="${reservationRequestDetailUrl}"><spring:message code="views.room.showReservationRequest"/></a>
        </dd>

        <c:if test="${roomRuntime != null}">
            <c:if test="${room.technology == 'H323_SIP'}">
                <dt class="control-label"><spring:message code="views.room.layout"/>:</dt>
                <dd>
                    <tag:roomLayout id="roomLayout" model="layout" width="200px"/>
                </dd>
            </c:if>
        </c:if>

        <div ng-controller="RoomDetailController">

            <div ng-show="show">

                <hr/>

                <dt><spring:message code="views.room.identifier"/>:</dt>
                <dd>${room.id}</dd>

                <dt><spring:message code="views.reservationRequest"/>:</dt>
                <dd><a href="${reservationRequestDetailUrl}">${reservationRequestId}</a></dd>
            </div>

            <dt></dt>
            <dd>
                <a href="" ng-click="show = true" ng-show="!show"><spring:message code="views.button.showMoreDetail"/></a>
                <a href="" ng-click="show = false" ng-show="show"><spring:message code="views.button.hideMoreDetail"/></a>
            </dd>

        </div>

    </dl>

    <%-- Allowed Participants --%>
    <c:if test="${room.technology == 'ADOBE_CONNECT'}">
        <h2><spring:message code="views.room.participants"/></h2>
        <p><spring:message code="views.room.participants.help"/></p>
        <tag:url var="participantModifyUrl" value="<%= ClientWebUrl.ROOM_PARTICIPANT_MODIFY %>">
            <tag:param name="back-url" value="${requestUrl}"/>
        </tag:url>
        <tag:url var="participantDeleteUrl" value="<%= ClientWebUrl.ROOM_PARTICIPANT_DELETE %>">
            <tag:param name="back-url" value="${requestUrl}"/>
        </tag:url>
        <tag:participantList isWritable="${isWritable}" data="${room.participants}" description="${not empty room.usageId}"
                             modifyUrl="${participantModifyUrl}" deleteUrl="${participantDeleteUrl}"
                             urlParam="roomId" urlValue="roomId"/>
        <c:if test="${isWritable}">
            <tag:url var="participantCreateUrl" value="<%= ClientWebUrl.ROOM_PARTICIPANT_CREATE %>">
                <tag:param name="roomId" value="${room.id}"/>
                <tag:param name="back-url" value="${requestUrl}"/>
            </tag:url>
            <a class="btn btn-primary" href="${participantCreateUrl}">
                <spring:message code="views.button.add"/>
                <c:if test="${not empty room.usageId}">
                    (<spring:message code="views.room.participants.addRoom"/>)
                </c:if>
            </a>
            <c:if test="${not empty room.usageId}">
                <tag:url var="participantCreateUrl" value="<%= ClientWebUrl.ROOM_PARTICIPANT_CREATE %>">
                    <tag:param name="roomId" value="${room.usageId}"/>
                    <tag:param name="back-url" value="${requestUrl}"/>
                </tag:url>
                <a class="btn btn-primary" href="${participantCreateUrl}">
                    <spring:message code="views.button.add"/>
                    (<spring:message code="views.room.participants.addUsage"/>)
                </a>
            </c:if>
        </c:if>
    </c:if>

    <%-- Runtime management - Not-Available --%>
    <c:if test="${roomNotAvailable}">
        <tag:url value="<%= ClientWebUrl.REPORT %>" var="reportUrl">
            <tag:param name="back-url" value="${requestScope.requestUrl}"/>
        </tag:url>

        <div class="not-available">
            <h2><spring:message code="views.room.notAvailable.heading"/></h2>
            <p><spring:message code="views.room.notAvailable.text" arguments="${reportUrl}"/></p>
        </div>
    </c:if>

    <%-- Runtime management - Current Participants --%>
    <c:if test="${!roomNotAvailable && room.available}">
        <tag:url value="<%= ClientWebUrl.ROOM_MANAGEMENT_PARTICIPANTS_DATA%>" var="roomParticipantsUrl">
            <tag:param name="roomId" value=":id"/>
        </tag:url>
        <div id = "roomParticipants" ng-controller="PaginationController"
             ng-init="init('room.participants', '${roomParticipantsUrl}', {id: '${room.id}'})">
            <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
            <spring:message code="views.button.refresh" var="paginationRefresh"/>
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
                        <th style="min-width: 150px; width: 150px;">
                            <spring:message code="views.room.currentParticipant.preview"/>
                        </th>
                    </c:if>
                    <c:if test="${room.technology == 'ADOBE_CONNECT'}">
                        <th><spring:message code="views.room.currentParticipant.role"/></th>
                        <th><spring:message code="views.room.currentParticipant.email"/></th>
                    </c:if>
                    <c:if test="${isWritable}">
                        <th style="min-width: 85px; width: 85px;"><spring:message code="views.list.action"/></th>
                    </c:if>
                </tr>
                </thead>
                <tbody>
                <tr ng-repeat="roomParticipant in items">
                    <td>{{roomParticipant.name}}
                    </td>
                    <c:if test="${room.technology == 'H323_SIP'}">
                        <td>
                            <span ng-show="roomParticipant.videoSnapshot">
                                <tag:url var="participantVideoSnapshotUrl" value="<%= ClientWebUrl.ROOM_MANAGEMENT_PARTICIPANT_VIDEO_SNAPSHOT %>">
                                    <tag:param name="roomId" value="${room.id}"/>
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
                    <%-- Actions --%>
                    <c:if test="${isWritable}">
                        <td ng-controller="RoomParticipantController">
                            <tag:url var="participantModifyUrl" value="<%= ClientWebUrl.ROOM_MANAGEMENT_PARTICIPANT_MODIFY %>">
                                <tag:param name="roomId" value="${room.id}"/>
                                <tag:param name="participantId" value="{{roomParticipant.id}}" escape="false"/>
                            </tag:url>
                            <%-- Mute audio --%>
                            <span ng-show="roomParticipant.audioMuted != null">
                                <spring:message var="participantAudioMuteTitle" code="views.room.currentParticipant.audioMuted.disable"/>
                                <spring:message var="participantAudioUnMuteTitle" code="views.room.currentParticipant.audioMuted.enable"/>
                                <a href="" ng-click="modifyByUrl('${participantModifyUrl}?audioMuted=' + !roomParticipant.audioMuted)" title="{{roomParticipant.audioMuted ? '${participantAudioUnMuteTitle}' : '${participantAudioMuteTitle}'}}"><i class="icon-volume-{{roomParticipant.audioMuted ? 'off' : 'up'}}"></i></a>&nbsp;
                            </span>
                            <%-- Mute video --%>
                            <span ng-show="roomParticipant.videoMuted != null">
                                <spring:message var="participantVideoMuteTitle" code="views.room.currentParticipant.videoMuted.disable"/>
                                <spring:message var="participantVideoUnMuteTitle" code="views.room.currentParticipant.videoMuted.enable"/>
                                <a href="" ng-click="modifyByUrl('${participantModifyUrl}?videoMuted=' + !roomParticipant.videoMuted)" title="{{roomParticipant.videoMuted ? '${participantVideoUnMuteTitle}' : '${participantVideoMuteTitle}'}}"><i class="icon-{{roomParticipant.videoMuted ? 'minus-sign-alt' : 'facetime-video'}}"></i></a>&nbsp;
                            </span>
                            <%-- Modify dialog --%>
                            <span ng-show="isEditable(roomParticipant)">
                                <spring:message var="participantModifyTitle" code="views.button.modify"/>
                                <a href="" ng-click="modify(roomParticipant, '${participantModifyUrl}')" title="${participantModifyTitle}"><i class="icon-pencil"></i></a>&nbsp;
                            </span>
                            <%-- Disconnect --%>
                            <tag:url var="disconnectParticipantUrl" value="<%= ClientWebUrl.ROOM_MANAGEMENT_PARTICIPANT_DISCONNECT %>">
                                <tag:param name="roomId" value="${room.id}"/>
                                <tag:param name="participantId" value="{{roomParticipant.id}}" escape="false"/>
                            </tag:url>
                            <spring:message var="participantDisconnectTitle" code="views.room.currentParticipant.disconnect"/>
                            <a href="" ng-click="modifyByUrl('${disconnectParticipantUrl}')" title="${participantDisconnectTitle}"><i class="icon-remove"></i></a>
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

    <%-- Runtime management - Recordings --%>
    <c:if test="${room.recordable}">
        <tag:url value="<%= ClientWebUrl.ROOM_MANAGEMENT_RECORDINGS_DATA %>" var="roomRecordingsUrl">
            <tag:param name="roomId" value=":id"/>
        </tag:url>
        <div ng-controller="PaginationController"
             ng-init="init('room.recordings', '${roomRecordingsUrl}', {id: '${room.id}'})">
            <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
            <spring:message code="views.button.refresh" var="paginationRefresh"/>
            <h2><spring:message code="views.room.recordings"/></h2>
            <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
                <spring:message code="views.pagination.records"/>
            </pagination-page-size>
            <div>
                <c:choose>
                    <c:when test="${room.started}">
                        <c:choose>
                            <c:when test="${room.recordingService.active}">
                                <a class="btn" href="">
                                    <spring:message code="views.room.recording.stop"/>
                                </a>
                            </c:when>
                            <c:otherwise>
                                <a class="btn" href="">
                                    <spring:message code="views.room.recording.start"/>
                                </a>
                            </c:otherwise>
                        </c:choose>
                        <a class="btn btn-primary" href="">
                            <spring:message code="views.room.recording.start"/>
                        </a>

                    </c:when>
                    <c:otherwise>
                    <span class="btn disabled">
                        <spring:message code="views.room.recording.start"/>
                    </span>
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="spinner" ng-hide="ready || errorContent"></div>
            <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
            <table class="table table-striped table-hover" ng-show="ready">
                <thead>
                <tr>
                    <th><spring:message code="views.room.recording.name"/></th>
                    <th><spring:message code="views.room.recording.uploaded"/></th>
                    <th><spring:message code="views.room.recording.duration"/></th>
                    <th>
                        <c:choose>
                            <c:when test="${isWritable}">
                                <spring:message code="views.room.recording.editableUrl"/>
                            </c:when>
                            <c:otherwise>
                                <spring:message code="views.room.recording.url"/>
                            </c:otherwise>
                        </c:choose>
                    </th>
                </tr>
                </thead>
                <tbody>
                <tr ng-repeat="roomRecording in items">
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
                    </td>
                    <td>
                        {{roomRecording.beginDate}}
                    </td>
                    <td>
                        {{roomRecording.duration}}
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${isWritable}">
                                <a href="{{roomRecording.editableUrl}}" target="_blank">{{roomRecording.editableUrl}}</a>
                            </c:when>
                            <c:otherwise>
                                <a href="{{roomRecording.url}}" target="_blank">{{roomRecording.url}}</a>
                            </c:otherwise>
                        </c:choose>
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
    </c:if>

    <div class="table-actions" style="text-align: right;">
        <c:if test="${!room.recordable && !room.deprecated}">
            <span class="btn disabled">
                <spring:message code="views.room.recording.book"/>
            </span>
        </c:if>
        <c:if test="${room.state.started && room.licenseCount == 0 && reservationRequestProvidable}">
            <tag:url var="createPermanentRoomCapacityUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_CREATE %>">
                <tag:param name="specificationType" value="PERMANENT_ROOM_CAPACITY"/>
                <tag:param name="permanentRoom" value="${room.id}"/>
                <tag:param name="back-url" value="${requestScope.requestUrl}"/>
            </tag:url>
            <a class="btn btn-primary" href="${createPermanentRoomCapacityUrl}">
                <spring:message code="views.room.requestCapacity"/>
            </a>
        </c:if>
        <a class="btn" href="javascript: location.reload();">
            <spring:message code="views.button.refresh"/>
        </a>
    </div>

</div>