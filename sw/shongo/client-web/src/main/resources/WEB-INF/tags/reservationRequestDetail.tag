<%--
  -- Detail of reservation request.
  --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="reservationRequest" required="false"
             type="cz.cesnet.shongo.client.web.models.ReservationRequestModel" %>
<%@attribute name="isActive" required="true" type="java.lang.Boolean" %>
<%@attribute name="detailUrl" required="false" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="reservationRequestDetail" value="${reservationRequest.detail}"/>
<spring:eval var="detailStateUrl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetailState(contextPath, ':reservationRequestId')"/>

<script type="text/javascript">
    angular.provideModule('tag:reservationRequestDetail', ['ngTooltip', 'ngResource', 'ngSanitize']);

    function DynamicStateController($scope, $resource, $sce, $timeout) {
        // Default requested slot
        $scope.requestedSlot = '<tag:format value="${reservationRequest.slot}" multiline="true"/>';
        <c:if test="${reservationRequestDetail != null && reservationRequestDetail.state != null}">
            // Default ReservationRequestState
            $scope.state = {
                code: "${reservationRequestDetail.state}",
                label: "<spring:message code="views.reservationRequest.state.${reservationRequestDetail.state}"/>",
                help: "<spring:message code="help.reservationRequest.state.${reservationRequestDetail.state}"/>"
            };
        </c:if>
        <c:if test="${reservationRequestDetail != null && reservationRequestDetail.allocationState != null}">
            <spring:eval expression="reservationRequestDetail.allocationStateReport != null ? reservationRequestDetail.allocationStateReport.replaceAll('\n','\\\\\\n') : null" var="allocationStateReport"/>
            // Default AllocationState
            $scope.allocationState = {
                code: "${reservationRequestDetail.allocationState}",
                report: "${allocationStateReport}",
                label: "<spring:message code="views.reservationRequest.allocationState.${reservationRequestDetail.allocationState}"/>",
                help: "<spring:message code="help.reservationRequest.allocationState.${reservationRequestDetail.allocationState}"/>"
            };
        </c:if>
        <c:if test="${reservationRequestDetail != null && reservationRequestDetail.room != null}">
            // Default room id and slot
            $scope.roomId = "${reservationRequestDetail.room.id}";
            $scope.roomSlot = '<tag:format value="${reservationRequestDetail.room.slot}" multiline="true"/>';
            $scope.roomName = "${reservationRequestDetail.room.name}";
            $scope.roomLicenseCount = "${reservationRequestDetail.room.licenseCount}";
            // Default  RoomState
            <spring:eval expression="reservationRequestDetail.room.stateReport != null ? reservationRequestDetail.room.stateReport.replaceAll('\n','\\\\\\n') : null" var="roomStateReport"/>
            $scope.roomState = {
                code: "${reservationRequestDetail.room.state}",
                started: ${reservationRequestDetail.room.state.started},
                report: "${roomStateReport}",
                label: "<spring:message code="views.executable.roomState.${reservationRequestDetail.room.state}"/>",
                help: "<%--
                --%><c:choose><%--
                    --%><c:when test="${reservationRequest.specificationType == 'PERMANENT_ROOM_CAPACITY'}"><%--
                        --%><spring:message code="help.executable.roomState.USED_ROOM.${reservationRequestDetail.room.state}"/><%--
                    --%></c:when><%--
                    --%><c:otherwise><%--
                        --%><spring:message code="help.executable.roomState.${reservationRequestDetail.room.state}"/><%--
                    --%></c:otherwise><%--
                --%></c:choose>"
            };
        </c:if>
        <c:if test="${isActive}">
            // Refreshing resource
            $scope.refreshResource = $resource('${detailStateUrl}', {reservationRequestId: '${reservationRequest.id}'}, {
                get: {method: 'GET'}
            });
            // Initial refresh timeout in seconds
            $scope.refreshTimeout = 5;
            // Number of performed automatic refreshes
            $scope.refreshCount = 0;
            // Specifies whether refreshing is in progress
            $scope.refreshing = false;

            /**
             * Perform refresh of reservation request state
             *
             * @param callback to be called after refresh is finished
             */
            $scope.refresh = function(callback){
                $scope.refreshing = true;
                $scope.refreshResource.get(null, function (result) {
                    $scope.state = result.state;
                    $scope.allocationState = result.allocationState;
                    $scope.roomId = result.roomId;
                    $scope.roomSlot = result.roomSlot;
                    $scope.roomName = result.roomName;
                    $scope.roomLicenseCount = result.roomLicenseCount;
                    $scope.roomState = result.roomState;
                    $scope.allocatedSlot = result.allocatedSlot;
                    if (callback != null) {
                        callback();
                    }
                    $scope.refreshing = false;
                });
            };

            /**
             * Perform automatic refresh.
             */
            $scope.autoRefresh = function() {

                $scope.refresh(function(){
                    $scope.refreshCount++;
                    if (($scope.refreshCount % 3) == 0) {
                        // Double refresh timeout after three refreshes
                        $scope.refreshTimeout *= 2;
                    }
                    $scope.setupRefresh();
                });
            };
            // Schedule first automatic refresh
            $scope.setupRefresh = function() {
                if ($scope.allocationState != null && $scope.allocationState.code == 'NOT_ALLOCATED' ||
                        ($scope.roomState != null && $scope.roomState.code != 'STOPPED' && $scope.roomState.code != 'FAILED')) {
                    $timeout($scope.autoRefresh, $scope.refreshTimeout * 1000);
                }
            };
            $scope.setupRefresh();
        </c:if>

        $scope.html = function(html) {
            return $sce.trustAsHtml(html);
        };
    }

    function MoreDetailController($scope) {
        $scope.show = false;
    }
</script>

<dl class="dl-horizontal" ng-controller="DynamicStateController">

    <dt><spring:message code="views.reservationRequest.type"/>:</dt>
    <dd>
        <spring:message code="views.reservationRequest.specification.${reservationRequest.specificationType}" var="specificationType"/>
        <tag:help label="${specificationType}">
            <spring:message code="help.reservationRequest.specification.${reservationRequest.specificationType}"/>
        </tag:help>
    </dd>

    <c:if test="${not empty reservationRequest.parentReservationRequestId}">
        <dt><spring:message code="views.reservationRequest.parentIdentifier"/>:</dt>
        <dd>
            <spring:eval var="urlDetail"
                         expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, reservationRequest.parentReservationRequestId)"/>
            <a href="${urlDetail}">${reservationRequest.parentReservationRequestId}</a>
        </dd>
    </c:if>

    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM' || reservationRequest.specificationType == 'ADHOC_ROOM'}">
        <dt><spring:message code="views.reservationRequest.technology"/>:</dt>
        <dd>${reservationRequest.technology.title}</dd>
    </c:if>

    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM' || (reservationRequest.specificationType == 'ADHOC_ROOM' && not empty reservationRequest.roomName)}">
        <dt><spring:message code="views.reservationRequest.specification.roomName"/>:</dt>
        <dd>${reservationRequest.roomName}</dd>
    </c:if>

    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM_CAPACITY'}">
        <dt><spring:message code="views.reservationRequest.specification.permanentRoomReservationRequestId"/>:</dt>
        <dd>
            <c:choose>
                <c:when test="${not empty detailUrl}">
                    <spring:eval var="permanentRoomDetailUrl"
                                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, reservationRequest.permanentRoomReservationRequestId)"/>
                    <a href="${permanentRoomDetailUrl}" tabindex="2">${reservationRequest.permanentRoomReservationRequest.specification.value}</a>
                </c:when>
                <c:otherwise>
                    ${reservationRequest.permanentRoomReservationRequest.specification.value}
                </c:otherwise>
            </c:choose>
        </dd>
    </c:if>

    <c:if test="${reservationRequest.specificationType == 'ADHOC_ROOM' || reservationRequest.specificationType == 'PERMANENT_ROOM_CAPACITY'}">
        <dt><spring:message code="views.reservationRequest.specification.roomParticipantCount"/>:</dt>
        <dd>${reservationRequest.roomParticipantCount}</dd>
    </c:if>

    <div ng-show="allocationState.code == 'ALLOCATED' && roomId">
        <dt><spring:message code="views.reservationRequest.room.slot"/>:</dt>
        <dd><span ng-bind-html="html(roomSlot)"></span></dd>
    </div>
    <div ng-show="allocationState.code != 'ALLOCATED' || !roomId">
        <dt><spring:message code="views.reservationRequest.slot"/>:</dt>
        <dd><span ng-bind-html="html(requestedSlot)"></span></dd>
    </div>

    <c:if test="${empty reservationRequest.parentReservationRequestId && reservationRequest.specificationType != 'PERMANENT_ROOM'}">
        <dt><spring:message code="views.reservationRequest.periodicity"/>:</dt>
        <dd>
            <spring:message code="views.reservationRequest.periodicity.${reservationRequest.periodicityType}"/>
            <c:if test="${reservationRequest.periodicityType != 'NONE' && reservationRequest.periodicityEnd != null}">
                (<spring:message code="views.reservationRequest.periodicity.until"/>&nbsp;<tag:format value="${reservationRequest.periodicityEnd}" style="date"/>)
            </c:if>
        </dd>
    </c:if>

    <c:if test="${not empty reservationRequest.roomPin}">
        <dt><spring:message code="views.reservationRequest.specification.roomPin"/>:</dt>
        <dd>${reservationRequest.roomPin}</dd>
    </c:if>

    <dt><spring:message code="views.reservationRequest.description"/>:</dt>
    <dd>${reservationRequest.description}</dd>

    <dt><spring:message code="views.reservationRequest.purpose"/>:</dt>
    <dd>
        <spring:message code="views.reservationRequest.purpose.${reservationRequest.purpose}"/>
    </dd>

    <div ng-show="state">
        <dt><spring:message code="views.reservationRequest.state"/>:</dt>
        <dd class="reservation-request-state">
            <tag:help label="{{state.label}}" labelClass="{{state.code}}">
                {{state.help}}
            </tag:help>
            <spring:message code="views.button.refresh" var="buttonRefresh"/>
            <span ng-show="roomId != null && roomState.started">
                <spring:eval var="urlRoomManagement"
                             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getRoomManagement(contextPath, '{{roomId}}')"/>
                (<a href="${urlRoomManagement}"><spring:message code="views.list.action.manage"/></a>)
            </span>
            <c:if test="${isActive}">
                <a ng-click="refresh()" class="btn" href="" title="${buttonRefresh}" ng-disabled="refreshing">
                    <span ng-class="{'icon-refresh': !refreshing, 'icon-repeat': refreshing}"></span>
                </a>
            </c:if>
        </dd>
    </div>

    <c:if test="${reservationRequestDetail != null && reservationRequestDetail.room != null}">
        <dt><spring:message code="views.room.aliases"/>:</dt>
        <dd>
            <tag:help label="${reservationRequestDetail.room.aliases}">
                <c:if test="${not empty reservationRequestDetail.room.aliasesDescription}">
                    ${reservationRequestDetail.room.aliasesDescription}
                </c:if>
            </tag:help>
        </dd>
    </c:if>

    <c:if test="${not empty reservationRequest.dateTime}">
        <dt><spring:message code="views.reservationRequest.dateTime"/>:</dt>
        <dd><tag:format value="${reservationRequest.dateTime}"/></dd>
    </c:if>

    <c:if test="${reservationRequestDetail != null}">

        <div ng-controller="MoreDetailController">

            <div ng-show="show">

                <hr/>

                <div ng-show="allocationState.code">
                    <dt><spring:message code="views.reservationRequest.allocationState"/>:</dt>
                    <dd class="allocation-state">
                        <tag:help label="{{allocationState.label}}" labelClass="{{allocationState.code}}">
                            <span>{{allocationState.help}}</span>
                            <pre ng-show="allocationState.code == 'ALLOCATION_FAILED' && allocationState.report">{{allocationState.report}}</pre>
                        </tag:help>
                    </dd>
                </div>

                <div ng-show="roomState.code">
                    <dt><spring:message code="views.room.state"/>:</dt>
                    <dd class="room-state">
                        <tag:help label="{{roomState.label}}" labelClass="{{roomState.code}}">
                            <span>{{roomState.help}}</span>
                            <pre ng-show="roomState.report">{{roomState.report}}</pre>
                        </tag:help>
                    </dd>
                </div>

                <div ng-show="allocationState.code != 'ALLOCATED' && roomId">
                    <dt><spring:message code="views.reservationRequest.room.slot"/>:</dt>
                    <dd><span ng-bind-html="html(roomSlot)"></span></dd>
                </div>
                <div ng-show="allocationState.code == 'ALLOCATED' && roomId">
                    <dt><spring:message code="views.reservationRequest.slot"/>:</dt>
                    <dd><span ng-bind-html="html(requestedSlot)"></span></dd>
                </div>

                <c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM_CAPACITY'}">
                    <div ng-show="roomName">
                        <dt><spring:message code="views.reservationRequest.room.name"/>:</dt>
                        <dd>{{roomName}}</dd>
                    </div>
                </c:if>

                <div ng-show="roomLicenseCount">
                    <dt><spring:message code="views.reservationRequest.room.licenseCount"/>:</dt>
                    <dd>{{roomLicenseCount}}</dd>
                </div>

                <c:if test="${not empty reservationRequest.id}">
                    <dt><spring:message code="views.reservationRequest.identifier"/>:</dt>
                    <dd>${reservationRequest.id}</dd>
                </c:if>

            </div>

            <dt></dt>
            <dd>
                <a href="" ng-click="show = true" ng-show="!show"><spring:message code="views.button.showMoreDetail"/></a>
                <a href="" ng-click="show = false" ng-show="show"><spring:message code="views.button.hideMoreDetail"/></a>
            </dd>

        </div>

    </c:if>

</dl>