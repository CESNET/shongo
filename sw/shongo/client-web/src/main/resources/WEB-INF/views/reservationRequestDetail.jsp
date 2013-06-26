<%--
  -- Page for displaying details about a single reservation request.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<c:if test="${isActive}">
    <security:accesscontrollist hasPermission="WRITE" domainObject="${reservationRequest}" var="isWritable"/>
</c:if>

<script type="text/javascript">
    // Angular application
    angular.module('ngReservationRequestDetail', ['ngPagination', 'ngTooltip']);
</script>

<%-- History --%>
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
                        <a href="${contextPath}/reservation-request/detail/${historyItem.id}">
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

<%-- Page title --%>
<h1><spring:message code="views.reservationRequestDetail.title"/></h1>

<div ng-app="ngReservationRequestDetail">

    <%-- Detail of request --%>
    <dl class="dl-horizontal">

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

        <dt><spring:message code="views.reservationRequest.periodicity"/>:</dt>
        <dd>
            <spring:message code="views.reservationRequest.periodicity.${reservationRequest.periodicityType}"/>
            <c:if test="${reservationRequest.periodicityType != 'NONE' && reservationRequest.periodicityEnd != null}">
                (<spring:message code="views.reservationRequest.periodicity.until"/> <joda:format
                    value="${reservationRequest.periodicityEnd}" style="M-"/>)
            </c:if>
        </dd>

        <dt><spring:message code="views.reservationRequest.type"/>:</dt>
        <dd>
            <spring:message code="views.reservationRequest.specification.${reservationRequest.specificationType}"/>
            <app:help><spring:message
                    code="views.help.reservationRequest.specification.${reservationRequest.specificationType}"/></app:help>
        </dd>

        <dt><spring:message code="views.reservationRequest.technology"/>:</dt>
        <dd>${reservationRequest.technology.title}</dd>

        <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
            <dt><spring:message code="views.reservationRequest.specification.permanentRoomName"/>:</dt>
            <dd>${reservationRequest.permanentRoomName}</dd>
        </c:if>

        <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM_CAPACITY'}">
            <dt><spring:message code="views.reservationRequest.specification.permanentRoomCapacityReservationId"/>:</dt>
            <dd>
                <c:choose>
                    <c:when test="${reservationRequest.permanentRoomCapacityReservationId}">
                        ${reservationRequest.permanentRoomCapacityReservationId}
                    </c:when>
                    <c:otherwise><spring:message
                            code="views.reservationRequest.specification.roomAlias.adhoc"/></c:otherwise>
                </c:choose>
            </dd>

            <dt><spring:message code="views.reservationRequest.specification.roomParticipantCount"/>:</dt>
            <dd>${reservationRequest.roomParticipantCount}</dd>

            <dt><spring:message code="views.reservationRequest.specification.roomPin"/>:</dt>
            <dd>${reservationRequest.roomPin}</dd>
        </c:if>

    </dl>

    <%-- List of user roles --%>
    <div ng-controller="PaginationController"
         ng-init="init('reservationRequestDetail.acl', '${contextPath}/reservation-request/:id/acl?start=:start&count=:count', {id: '${reservationRequest.id}'})">
        <pagination-page-size class="pull-right">
            <spring:message code="views.pagination.records"/>
        </pagination-page-size>
        <h2><spring:message code="views.reservationRequestDetail.userRoles"/></h2>

        <div class="spinner" ng-hide="ready"></div>
        <table class="table table-striped table-hover" ng-show="ready">
            <thead>
            <tr>
                <th><spring:message code="views.aclRecord.user"/></th>
                <th><spring:message code="views.aclRecord.role"/></th>
                <th><spring:message code="views.aclRecord.email"/></th>
                <c:if test="${isWritable}">
                    <th width="100px"><spring:message code="views.list.action"/></th>
                </c:if>
            </tr>
            </thead>
            <tbody>
            <tr ng-repeat="userRole in items">
                <td>{{userRole.user.fullName}}</td>
                <td>{{userRole.role}}</td>
                <td>{{userRole.user.primaryEmail}}</td>
                <c:if test="${isWritable}">
                    <td>
                        <a href="${contextPath}/reservation-request/${reservationRequest.id}/acl/delete/{{userRole.id}}">
                            <spring:message code="views.list.action.delete"/>
                        </a>
                    </td>
                </c:if>
            </tr>
            <tr ng-hide="items.length">
                <td colspan="7" class="empty">- - - None - - -</td>
            </tr>
            </tbody>
        </table>
        <c:choose>
            <c:when test="${isWritable}">
                <a class="btn btn-primary" href="${contextPath}/reservation-request/${reservationRequest.id}/acl/create">
                    <spring:message code="views.button.create"/>
                </a>
                <pagination-pages class="pull-right"><spring:message code="views.pagination.pages"/></pagination-pages>
            </c:when>
            <c:otherwise>
                <pagination-pages><spring:message code="views.pagination.pages"/></pagination-pages>
            </c:otherwise>
        </c:choose>
    </div>

    <%-- List of reservations --%>
    <c:if test="${isActive}">
        <hr/>
        <c:choose>
            <%-- Single reservation --%>
            <c:when test="${reservationRequest.periodicityType == 'NONE'}">
                <h2><spring:message code="views.reservationRequestDetail.reservations"/></h2>
                <table class="table table-striped table-hover">
                    <thead>
                    <tr>
                        <th width="320px"><spring:message code="views.reservationRequest.slot"/></th>
                        <th><spring:message code="views.reservationRequest.allocationState"/></th>
                        <th><spring:message code="views.reservationRequestDetail.reservations.aliases"/></th>
                        <th width="120px"><spring:message code="views.list.action"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td>
                            <joda:format value="${childReservationRequest.slot.start}" style="MS"/> -
                            <joda:format value="${childReservationRequest.slot.end}" style="MS"/>
                        </td>
                        <td class="allocation-state">
                            <span id="reservationState" class="${childReservationRequest.allocationState}"><spring:message code="views.reservationRequest.allocationState.${childReservationRequest.allocationState}"/></span>
                            <app:help label="reservationState">
                                <span><spring:message code="views.help.reservationRequest.allocationState.${childReservationRequest.allocationState}"/></span>
                                <c:if test="${childReservationRequest.allocationStateReport != null}">
                                    <pre>${childReservationRequest.allocationStateReport}</pre>
                                </c:if>
                            </app:help>
                        </td>
                        <td>
                            <span id="reservationAliases">${childReservationRequest.aliases}</span>
                            <c:if test="${childReservationRequest.aliasesDescription != null}">
                                <app:help label="reservationAliases">${childReservationRequest.aliasesDescription}</app:help>
                            </c:if>
                        </td>
                        <td><span><spring:message code="views.reservationRequestDetail.reservations.action.room"/></span></td>
                    </tr>
                    </tbody>
                </table>
            </c:when>

            <%-- Multiple reservations dynamically --%>
            <c:otherwise>
                <div ng-controller="PaginationController"
                     ng-init="init('reservationRequestDetail.children', '${contextPath}/reservation-request/:id/children?start=:start&count=:count', {id: '${reservationRequest.id}'})">
                    <pagination-page-size class="pull-right">
                        <spring:message code="views.pagination.records"/>
                    </pagination-page-size>
                    <h2><spring:message code="views.reservationRequestDetail.reservations"/></h2>
                    <div class="spinner" ng-hide="ready"></div>
                    <table class="table table-striped table-hover" ng-show="ready">
                        <thead>
                        <tr>
                            <th width="320px"><spring:message code="views.reservationRequest.slot"/></th>
                            <th><spring:message code="views.reservationRequest.allocationState"/></th>
                            <th><spring:message code="views.reservationRequestDetail.reservations.aliases"/></th>
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
                            <td>
                                <span id="reservationAliases-{{$index}}">{{childReservationRequest.aliases}}</span>
                                <div ng-switch on="isEmpty(childReservationRequest.allocationStateReport)">
                                    <div ng-switch-when="false">
                                        <app:help label="reservationAliases-{{$index}}">{{childReservationRequest.aliasesDescription}}</app:help>
                                    </div>
                                </div>
                            </td>
                            <td><span><spring:message code="views.reservationRequestDetail.reservations.action.room"/></span></td>
                        </tr>
                        </tbody>
                    </table>
                    <pagination-pages><spring:message code="views.pagination.pages"/></pagination-pages>
                </div>
            </c:otherwise>
        </c:choose>
    </c:if>

</div>

<div class="pull-right">
    <a class="btn btn-primary" href="${contextPath}/reservation-request">
        <spring:message code="views.button.back"/>
    </a>
    <a class="btn" href="">
        <spring:message code="views.button.refresh"/>
    </a>
    <c:if test="${isWritable}">
        <a class="btn" href="${contextPath}/reservation-request/modify/${reservationRequest.id}">
            <spring:message code="views.button.modify"/>
        </a>
        <a class="btn" href="${contextPath}/reservation-request/delete/${reservationRequest.id}">
            <spring:message code="views.button.delete"/>
        </a>
    </c:if>
</div>