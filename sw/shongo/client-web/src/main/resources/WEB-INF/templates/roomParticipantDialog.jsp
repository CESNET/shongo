<%--
  -- Dialog for modifying room participant.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<script type="text/javascript">
    var module = angular.module('jsp:roomParticipantDialog', ['ui.bootstrap']);
    module.factory("$roomParticipantDialog", function($modal) {
        return {
            modify: function(roomParticipant) {
                return $modal.open({
                    templateUrl: "/roomParticipantDialog.html",
                    controller: "RoomParticipantDialogController",
                    backdrop: "static",
                    windowClass: "center",
                    resolve: {
                        data: function () {
                            return angular.copy(roomParticipant);
                        }
                    }
                });
            }
        };
    });
    module.controller("RoomParticipantDialogController", function($scope, $modalInstance, data) {
        $scope.data = data;
        $scope.data.enableMicrophoneLevel = $scope.data.microphoneLevel != null;
        if (!$scope.data.enableMicrophoneLevel) {
            $scope.data.microphoneLevel = 5;
        }
        $scope.originalMicrophoneLevel = $scope.data.microphoneLevel;
        if ($scope.data.layout != null) {
            if ($scope.data.layout.indexOf("VOICE_SWITCHED") != -1) {
                $scope.data.layout = "VOICE_SWITCHED_SPEAKER_CORNER";
            }
            else {
                $scope.data.layout = "SPEAKER_CORNER";
            }
        }
        $scope.save = function () {
            if ($scope.data.microphoneLevel == null) {
                $scope.data.microphoneLevel = $scope.originalMicrophoneLevel;
            }
            if (!$scope.data.enableMicrophoneLevel) {
                $scope.data.microphoneLevel = null;
            }
            $modalInstance.close($scope.data);
        };
        $scope.cancel = function () {
            $modalInstance.dismiss();
        };
    });
</script>

<script type="text/ng-template" id="/roomParticipantDialog.html">
    <div class="modal-header">
        <h3><spring:message code="views.room.currentParticipant.modify"/></h3>
    </div>
    <div class="modal-body">
        <form class="form-horizontal">
            <div class="control-group">
                <label class="control-label" for="name">
                    <spring:message code="views.room.currentParticipant.name"/>
                </label>
                <div class="controls">
                    <input id="name" type="text" readonly="true" tabindex="1" ng-model="data.name"/>
                </div>
            </div>
            <div class="control-group" ng-show="data.layout != null">
                <label class="control-label" for="layout">
                    <spring:message code="views.room.currentParticipant.layout"/>:
                </label>
                <div class="controls">
                    <select id="layout" ng-model="data.layout" tabindex="1">
                        <option value="SPEAKER_CORNER"><spring:message code="views.room.currentParticipant.layout.NOT_VOICE_SWITCHED"/></option>
                        <option value="VOICE_SWITCHED_SPEAKER_CORNER"><spring:message code="views.room.currentParticipant.layout.VOICE_SWITCHED"/></option>
                    </select>
                </div>
            </div>
            <c:if test="${room.technology == 'H323_SIP'}">
                <div class="control-group" >
                    <label class="control-label">
                        <input type="checkbox" tabindex="1" ng-model="data.enableMicrophoneLevel"/>
                        <spring:message code="views.room.currentParticipant.microphoneLevel"/>:
                    </label>
                    <div class="controls">
                        <input type="number" min="1" max="10" tabindex="1" ng-model="data.microphoneLevel" ng-disabled="!data.enableMicrophoneLevel"/>
                    </div>
                </div>
            </c:if>
            <div class="control-group" ng-show="data.audioMuted != null">
                <div class="controls">
                    <div>
                        <label class="checkbox inline">
                            <input type="checkbox" tabindex="1" ng-model="data.audioMuted"/><spring:message code="views.room.currentParticipant.audioMuted"/>
                        </label>
                    </div>
                </div>
            </div>
            <div class="control-group" ng-show="data.videoMuted != null">
                <div class="controls">
                    <div>
                        <label class="checkbox inline">
                            <input type="checkbox" tabindex="1" ng-model="data.videoMuted"/><spring:message code="views.room.currentParticipant.videoMuted"/>
                        </label>
                    </div>
                </div>
            </div>
        </form>
    </div>
    <div class="modal-footer">
        <button ng-click="save()" class="btn btn-primary" ><spring:message code="views.button.save"/></button>
        <button ng-click="cancel()" class="btn" ><spring:message code="views.button.cancel"/></button>
    </div>
</script>