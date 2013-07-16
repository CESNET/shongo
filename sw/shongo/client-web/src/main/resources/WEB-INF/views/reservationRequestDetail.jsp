<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%--
  -- Page for displaying details about a single reservation request.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib prefix="app" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="urlBack">${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_LIST %></c:set>
<spring:eval var="urlModify" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestModify(contextPath, reservationRequest.id)"/>
<spring:eval var="urlDelete" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDelete(contextPath, reservationRequest.id)"/>

<c:if test="${isActive && parentReservationRequestId == null}">
    <security:accesscontrollist hasPermission="WRITE" domainObject="${reservationRequest}" var="isWritable"/>
    <security:accesscontrollist hasPermission="PROVIDE_RESERVATION_REQUEST" domainObject="${reservationRequest}" var="isProvidable"/>
</c:if>

<script type="text/javascript">
    // Angular application
    angular.module('ngReservationRequestDetail', ['ngPagination', 'ngTooltip']);
</script>

<%-- History --%>
<c:if test="${history != null}">
    <div class="pull-right bordered">
        <h2><spring:message code="views.reservationRequestDetail.history"/></h2>
        <table class="table table-striped table-hover">
            <thead>
            <tr>
                <th><spring:message code="views.reservationRequest.dateTime"/></th>
                <th><spring:message code="views.reservationRequest.user"/></th>
                <th><spring:message code="views.reservationRequest.type"/></th>
                <th><spring:message code="views.list.action"/></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${history}" var="historyItem">
                <c:set var="rowClass" value=""></c:set>
                <c:choose>
                    <c:when test="${historyItem.selected}">
                        <tr class="selected">
                    </c:when>
                    <c:otherwise>
                        <tr>
                    </c:otherwise>
                </c:choose>
                <td><joda:format value="${historyItem.dateTime}" style="MM"/></td>
                <td>${historyItem.user}</td>
                <td><spring:message code="views.reservationRequest.type.${historyItem.type}"/></td>
                <td>
                    <c:choose>
                        <c:when test="${historyItem.id != reservationRequest.id && historyItem.type != 'DELETED'}">
                            <spring:eval var="urlDetail" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetail(contextPath, historyItem.id)"/>
                            <a href="${urlDetail}">
                                <spring:message code="views.list.action.show"/>
                            </a>
                        </c:when>
                        <c:otherwise>(<spring:message code="views.list.selected"/>)</c:otherwise>
                    </c:choose>
                </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</c:if>

<%-- Page title --%>
<h1>
    <c:choose>
        <c:when test="${parentReservationRequestId != null}">
            <spring:message code="views.reservationRequestDetail.title.child"/>
        </c:when>
        <c:otherwise>
            <spring:message code="views.reservationRequestDetail.title"/>
        </c:otherwise>
    </c:choose>
</h1>

<div ng-app="ngReservationRequestDetail">

    <%-- Detail of request --%>
    <dl class="dl-horizontal">

        <c:if test="${parentReservationRequestId != null}">
            <dt><spring:message code="views.reservationRequest.parentIdentifier"/>:</dt>
            <dd>
                <spring:eval var="urlDetail" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetail(contextPath, parentReservationRequestId)"/>
                <a href="${urlDetail}">${parentReservationRequestId}</a>
            </dd>
        </c:if>

        <dt><spring:message code="views.reservationRequest.identifier"/>:</dt>
        <dd>${reservationRequest.id}</dd>

        <dt><spring:message code="views.reservationRequest.dateTime"/>:</dt>
        <dd><joda:format value="${reservationRequest.dateTime}" style="MM"/></dd>

        <dt><spring:message code="views.reservationRequest.purpose"/>:</dt>
        <dd>
            <spring:message code="views.reservationRequest.purpose.${reservationRequest.purpose}"/>
        </dd>

        <dt><spring:message code="views.reservationRequest.description"/>:</dt>
        <dd>${reservationRequest.description}</dd>

        <dt><spring:message code="views.reservationRequest.slot"/>:</dt>
        <dd>
            <joda:format value="${reservationRequest.start}" style="MM"/>
            <br/>
            <joda:format value="${reservationRequest.end}" style="MM"/>
        </dd>

        <c:if test="${parentReservationRequestId == null}">
            <dt><spring:message code="views.reservationRequest.periodicity"/>:</dt>
            <dd>
                <spring:message code="views.reservationRequest.periodicity.${reservationRequest.periodicityType}"/>
                <c:if test="${reservationRequest.periodicityType != 'NONE' && reservationRequest.periodicityEnd != null}">
                    (<spring:message code="views.reservationRequest.periodicity.until"/> <joda:format
                        value="${reservationRequest.periodicityEnd}" style="M-"/>)
                </c:if>
            </dd>
        </c:if>

        <dt><spring:message code="views.reservationRequest.type"/>:</dt>
        <dd>
            <spring:message code="views.reservationRequest.specification.${reservationRequest.specificationType}"/>
            <app:help><spring:message
                    code="views.help.reservationRequest.specification.${reservationRequest.specificationType}"/></app:help>
        </dd>

        <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM' || reservationRequest.specificationType == 'ADHOC_ROOM'}">
            <dt><spring:message code="views.reservationRequest.technology"/>:</dt>
            <dd>${reservationRequest.technology.title}</dd>
        </c:if>

        <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
            <dt><spring:message code="views.reservationRequest.specification.permanentRoomName"/>:</dt>
            <dd>${reservationRequest.permanentRoomName}</dd>
        </c:if>

        <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM_CAPACITY'}">
            <dt><spring:message code="views.reservationRequest.specification.permanentRoomCapacityReservationRequestId"/>:</dt>
            <dd>
                <c:choose>
                    <c:when test="${reservationRequest.permanentRoomCapacityReservationRequestId != null}">
                        <spring:eval var="urlDetail" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetail(contextPath, reservationRequest.permanentRoomCapacityReservationRequestId)"/>
                        <a href="${urlDetail}">${permanentRoomReservationRequest.specification.value}</a>
                    </c:when>
                    <c:otherwise>
                        <spring:message code="views.reservationRequest.specification.roomAlias.adhoc"/>
                    </c:otherwise>
                </c:choose>
            </dd>
        </c:if>

        <c:if test="${reservationRequest.specificationType == 'ADHOC_ROOM' || reservationRequest.specificationType == 'PERMANENT_ROOM_CAPACITY'}">
            <dt><spring:message code="views.reservationRequest.specification.roomParticipantCount"/>:</dt>
            <dd>${reservationRequest.roomParticipantCount}</dd>

            <c:if test="${reservationRequest.roomPin != null}">
                <dt><spring:message code="views.reservationRequest.specification.roomPin"/>:</dt>
                <dd>${reservationRequest.roomPin}</dd>
            </c:if>
        </c:if>

    </dl>

    <%-- List of user roles --%>
    <spring:eval var="urlAcl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestAcl(contextPath, ':id')"/>
    <spring:eval var="urlAclCreate" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestAclCreate(contextPath, reservationRequest.id)"/>
    <spring:eval var="urlAclDelete" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestAclDelete(contextPath, reservationRequest.id)"/>

    <h2><spring:message code="views.reservationRequestDetail.userRoles"/></h2>
    <app:userRoleList dataUrl="${urlAcl}"
                   dataUrlParameters="id: '${reservationRequest.id}'"
                   createUrl="${urlAclCreate}"
                   deleteUrl="${urlAclDelete}"
                   isWritable="${isWritable}"/>

    <%-- Active reservation request --%>
    <c:if test="${isActive}">
        <hr/>
        <c:choose>
            <%-- List of reservations --%>
            <c:when test="${reservationRequest.periodicityType == 'NONE'}">
                <h2><spring:message code="views.reservationRequestDetail.reservations"/></h2>
                <table class="table table-striped table-hover">
                    <thead>
                    <tr>
                        <th width="320px"><spring:message code="views.reservationRequest.slot"/></th>
                        <th><spring:message code="views.reservationRequest.allocationState"/></th>
                        <th><spring:message code="views.room.state"/></th>
                        <th><spring:message code="views.room.aliases"/></th>
                        <th width="120px"><spring:message code="views.list.action"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${reservations}" var="reservation" varStatus="status">
                        <tr>
                            <td>
                                <joda:format value="${reservation.slot.start}" style="MS"/> -
                                <joda:format value="${reservation.slot.end}" style="MS"/>
                            </td>
                            <td class="allocation-state">
                                <c:if test="${reservation.allocationState != null}">
                                    <span id="reservationState-${status.index}" class="${reservation.allocationState}">
                                        <spring:message code="views.reservationRequest.allocationState.${reservation.allocationState}"/>
                                    </span>
                                    <app:help label="reservationState-${status.index}">
                                        <span>
                                            <spring:message code="views.help.reservationRequest.allocationState.${reservation.allocationState}"/>
                                        </span>
                                        <c:if test="${reservation.allocationStateReport != null}">
                                            <pre>${reservation.allocationStateReport}</pre>
                                        </c:if>
                                    </app:help>
                                </c:if>
                            </td>
                            <td class="executable-state">
                                <c:if test="${reservation.roomState != null}">
                                    <span id="executableState-${status.index}" class="${reservation.roomState}">
                                        <spring:message code="views.reservationRequest.executableState.${reservation.roomState}"/>
                                    </span>
                                    <app:help label="executableState-${status.index}">
                                        <span>
                                            <spring:message code="views.help.reservationRequest.executableState.${reservation.roomState}"/>
                                        </span>
                                        <c:if test="${reservation.roomStateReport != null}">
                                            <pre>${reservation.roomStateReport}</pre>
                                        </c:if>
                                    </app:help>
                                </c:if>
                            </td>
                            <td>
                                <span id="executableAliases-${status.index}">${reservation.roomAliases}</span>
                                <c:if test="${reservation.roomAliasesDescription != null}">
                                    <app:help label="executableAliases-${status.index}">${reservation.roomAliasesDescription}</app:help>
                                </c:if>
                            </td>
                            <td>
                                <c:if test="${reservation.roomState.available}">
                                    <spring:eval var="urlRoomManagement" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getRoomManagement(contextPath, reservation.roomId)"/>
                                    <a href="${urlRoomManagement}">
                                        <spring:message code="views.list.action.manage"/>
                                    </a>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:when>

            <%-- Multiple reservation requests dynamically --%>
            <c:otherwise>
                <spring:eval var="urlChildList" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetailChildren(contextPath, ':id')"/>
                <spring:eval var="urlChildDetail" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetail(contextPath, '{{childReservationRequest.id}}')"/>
                <spring:eval var="urlChildRoomManagement" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getRoomManagement(contextPath, '{{childReservationRequest.roomId}}')"/>
                <div ng-controller="PaginationController"
                     ng-init="init('reservationRequestDetail.children', '${urlChildList}?start=:start&count=:count', {id: '${reservationRequest.id}'})">
                    <pagination-page-size class="pull-right">
                        <spring:message code="views.pagination.records"/>
                    </pagination-page-size>
                    <h2><spring:message code="views.reservationRequestDetail.children"/></h2>
                    <div class="spinner" ng-hide="ready"></div>
                    <table class="table table-striped table-hover" ng-show="ready">
                        <thead>
                        <tr>
                            <th width="320px"><spring:message code="views.reservationRequest.slot"/></th>
                            <th><spring:message code="views.reservationRequest.allocationState"/></th>
                            <th><spring:message code="views.room.state"/></th>
                            <th><spring:message code="views.room.aliases"/></th>
                            <th width="120px"><spring:message code="views.list.action"/></th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr ng-repeat="childReservationRequest in items">
                            <td>{{childReservationRequest.slot}}</td>
                            <td class="allocation-state">
                                <span id="reservationState-{{$index}}" class="{{childReservationRequest.allocationState}}">{{childReservationRequest.allocationStateMessage}}</span>
                                <app:help label="reservationState-{{$index}}" tooltipId="reservationState-tooltip-{{$index}}">
                                    <span>{{childReservationRequest.allocationStateHelp}}</span>
                                    <div ng-switch on="isEmpty(childReservationRequest.allocationStateReport)">
                                        <div ng-switch-when="false">
                                            <pre>{{childReservationRequest.allocationStateReport}}</pre>
                                        </div>
                                    </div>
                                </app:help>
                            </td>
                            <td class="executable-state">
                                <div ng-show="childReservationRequest.roomState">
                                    <span id="executableState-{{$index}}" class="{{childReservationRequest.roomState}}">{{childReservationRequest.roomStateMessage}}</span>
                                    <app:help label="executableState-{{$index}}" tooltipId="executableState-tooltip-{{$index}}">
                                        <span>{{childReservationRequest.roomStateHelp}}</span>
                                        <div ng-switch on="isEmpty(childReservationRequest.roomStateReport)">
                                            <div ng-switch-when="false">
                                                <pre>{{childReservationRequest.roomStateReport}}</pre>
                                            </div>
                                        </div>
                                    </app:help>
                                </div>
                            </td>
                            <td>
                                <span id="executableAliases-{{$index}}" ng-bind-html-unsafe="childReservationRequest.roomAliases"></span>
                                <div ng-switch on="isEmpty(childReservationRequest.roomAliasesDescription)" style="display: inline-block;">
                                    <div ng-switch-when="false">
                                        <app:help label="executableAliases-{{$index}}" tooltipId="executableAliases-tooltip-{{$index}}">
                                            <span ng-bind-html-unsafe="childReservationRequest.roomAliasesDescription"></span>
                                        </app:help>
                                    </div>
                                </div>
                            </td>
                            <td>
                                <a href="${urlChildDetail}"><spring:message code="views.list.action.show"/></a>
                                <span ng-show="childReservationRequest.roomStateAvailable">
                                    | <a href="${urlChildRoomManagement}">
                                        <spring:message code="views.list.action.manage"/>
                                    </a>
                                </span>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                    <pagination-pages><spring:message code="views.pagination.pages"/></pagination-pages>
                </div>
            </c:otherwise>
        </c:choose>
    </c:if>

    <%-- Permanent room capacities --%>
    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
        <spring:eval var="urlUsageList" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetailUsages(contextPath, ':id')"/>
        <spring:eval var="urlUsageDetail" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetail(contextPath, '{{permanentRoomCapacity.id}}')"/>
        <hr/>
        <div ng-controller="PaginationController"
             ng-init="init('reservationRequestDetail.permanentRoomCapacities', '${urlUsageList}?start=:start&count=:count', {id: '${reservationRequest.id}'})">
            <pagination-page-size class="pull-right">
                <spring:message code="views.pagination.records"/>
            </pagination-page-size>
            <h2><spring:message code="views.reservationRequestDetail.permanentRoomCapacities"/></h2>
            <div class="spinner" ng-hide="ready"></div>
            <table class="table table-striped table-hover" ng-show="ready">
                <thead>
                <tr>
                    <th width="320px"><spring:message code="views.reservationRequest.slot"/></th>
                    <th><spring:message code="views.reservationRequest.specification.roomParticipantCount"/></th>
                    <th><spring:message code="views.reservationRequest.allocationState"/></th>
                    <th width="120px"><spring:message code="views.list.action"/></th>
                </tr>
                </thead>
                <tbody>
                <tr ng-repeat="permanentRoomCapacity in items">
                    <td>{{permanentRoomCapacity.slot}}</td>
                    <td>{{permanentRoomCapacity.roomParticipantCount}}</td>
                    <td class="allocation-state">
                        <span id="permanentRoomCapacityState-{{$index}}" class="{{permanentRoomCapacity.allocationState}}">{{permanentRoomCapacity.allocationStateMessage}}</span>
                        <app:help label="permanentRoomCapacityState-{{$index}}" tooltipId="reservationState-tooltip-{{$index}}">
                            <span>{{permanentRoomCapacity.allocationStateHelp}}</span>
                        </app:help>
                    </td>
                    <td>
                        <a href="${urlUsageDetail}"><spring:message code="views.list.action.show"/></a>
                    </td>
                </tr>
                <tr ng-hide="items.length">
                    <td colspan="3" class="empty"><spring:message code="views.list.none"/></td>
                </tr>
                </tbody>
            </table>
            <c:choose>
                <c:when test="${isProvidable && reservationRequest.slot.containsNow()}">
                    <c:set var="urlCreate">${contextPath}<%= ClientWebUrl.RESERVATION_REQUEST_CREATE %></c:set>
                    <a class="btn btn-primary" href="${urlCreate}?type=PERMANENT_ROOM_CAPACITY&permanentRoom=${reservationRequest.id}">
                        <spring:message code="views.button.create"/>
                    </a>
                    <pagination-pages class="pull-right"><spring:message code="views.pagination.pages"/></pagination-pages>
                </c:when>
                <c:otherwise>
                    <pagination-pages><spring:message code="views.pagination.pages"/></pagination-pages>
                </c:otherwise>
            </c:choose>
        </div>
    </c:if>

</div>

<div class="pull-right">
    <a class="btn btn-primary" href="${urlBack}">
        <spring:message code="views.button.back"/>
    </a>
    <a class="btn" href="javascript: location.reload();">
        <spring:message code="views.button.refresh"/>
    </a>
    <c:if test="${isWritable}">
        <a class="btn" href="${urlModify}">
            <spring:message code="views.button.modify"/>
        </a>
        <a class="btn" href="${urlDelete}">
            <spring:message code="views.button.delete"/>
        </a>
    </c:if>
</div>