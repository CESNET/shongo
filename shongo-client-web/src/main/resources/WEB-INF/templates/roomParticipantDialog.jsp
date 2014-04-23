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
    module.controller("RoomParticipantDialogController", function($scope, $modalInstance, data, $timeout) {
        $scope.data = data;
        $scope.data.enableMicrophoneLevel = $scope.data.microphoneLevel != null && $scope.data.microphoneLevel != 0;
        if (!$scope.data.enableMicrophoneLevel) {
            $scope.data.microphoneLevel = 5;
        }
        $scope.originalMicrophoneLevel = $scope.data.microphoneLevel;
        $scope.save = function () {
            if ($scope.data.microphoneLevel == null) {
                $scope.data.microphoneLevel = $scope.originalMicrophoneLevel;
            }
            if (!$scope.data.enableMicrophoneLevel) {
                $scope.data.microphoneLevel = 0;
            }
            $modalInstance.close($scope.data);
        };
        $scope.cancel = function () {
            $modalInstance.dismiss();
        };
        $timeout(function () {
            // init room layout
            tagRoomLayout_participantLayoutInit();
        }, 0);
    });
    <tag:roomLayout id="participantLayout" content="javascript"/>
</script>

<script type="text/ng-template" id="/roomParticipantDialog.html">
    <div class="modal-header">
        <h3><spring:message code="views.room.currentParticipant.modify"/></h3>
    </div>
    <div class="modal-body">
        <form class="form-horizontal">
            <div class="control-group">
                <label class="control-label" for="name">
                    <spring:message code="views.room.currentParticipant.name"/>:
                </label>
                <div class="controls">
                    <input id="name" type="text" readonly="true" tabindex="1" ng-model="data.name"/>
                </div>
            </div>
            <c:if test="${technology == 'H323_SIP'}">
                <div class="control-group">
                    <label class="control-label" for="name">
                        <spring:message code="views.room.currentParticipant.alias"/>:
                    </label>
                    <div class="controls">
                        <input id="name" type="text" readonly="true" tabindex="1" ng-model="data.alias"/>
                    </div>
                </div>
            </c:if>
            <div class="control-group" ng-show="data.layout != null">
                <label class="control-label" for="participantLayout">
                    <spring:message code="views.room.currentParticipant.layout"/>:
                </label>
                <div class="controls">
                    <tag:roomLayout id="participantLayout" model="data.layout" content="html"/>
                </div>
            </div>
            <c:if test="${technology == 'H323_SIP'}">
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
            <div class="control-group" ng-show="data.microphoneEnabled != null">
                <div class="controls">
                    <div>
                        <label class="checkbox inline">
                            <input type="checkbox" tabindex="1" ng-model="data.microphoneEnabled"/><spring:message code="views.room.currentParticipant.microphoneEnabled"/>
                        </label>
                    </div>
                </div>
            </div>
            <div class="control-group" ng-show="data.videoEnabled != null">
                <div class="controls">
                    <div>
                        <label class="checkbox inline">
                            <input type="checkbox" tabindex="1" ng-model="data.videoEnabled"/><spring:message code="views.room.currentParticipant.videoEnabled"/>
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