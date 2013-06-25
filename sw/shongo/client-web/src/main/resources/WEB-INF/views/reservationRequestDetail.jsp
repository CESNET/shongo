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

        <c:if test="${reservationRequest.specificationType == 'ALIAS'}">
            <dt><spring:message code="views.reservationRequest.specification.aliasRoomName"/>:</dt>
            <dd>${reservationRequest.aliasRoomName}</dd>
        </c:if>

        <c:if test="${reservationRequest.specificationType == 'ROOM'}">
            <dt><spring:message code="views.reservationRequest.specification.roomAlias"/>:</dt>
            <dd>
                <c:choose>
                    <c:when test="${reservationRequest.roomAliasReservationId}">
                        ${reservationRequest.roomAliasReservationId}
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
            <c:when test="${reservationRequest.periodicityType == 'NONE'}">
                <h2><spring:message code="views.reservationRequestDetail.reservations"/></h2>
                <table class="table table-striped table-hover">
                    <thead>
                    <tr>
                        <th width="350px"><spring:message code="views.reservationRequest.slot"/></th>
                        <th><spring:message code="views.reservationRequest.allocationState"/></th>
                        <th width="100px"><spring:message code="views.list.action"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td>
                            <joda:format value="${reservationRequest.start}" style="MS"/> -
                            <joda:format value="${reservationRequest.end}" style="MS"/>
                        </td>
                        <td>
                            <div class="tooltip-container">
                                <span tooltip="reservation-tooltip" class="tooltip-label">${reservationRequest.allocationState}</span>
                                <div id="reservation-tooltip" class="tooltip-content">
                                    <pre>${reservationRequest.allocationStateReport}</pre>
                                </div>
                            </div>
                        </td>
                        <td>TODO: action</td>
                    </tr>
                    </tbody>
                </table>
            </c:when>

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
                            <th width="350px"><spring:message code="views.reservationRequest.slot"/></th>
                            <th><spring:message code="views.reservationRequest.allocationState"/></th>
                            <th width="100px"><spring:message code="views.list.action"/></th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr ng-repeat="childReservationRequest in items">
                            <td>{{childReservationRequest.slot}}</td>
                            <td>
                                <div class="tooltip-container">
                                    <span tooltip="reservation-{{$index}}" class="tooltip-label">{{childReservationRequest.allocationState}}</span>
                                    <div id="reservation-{{$index}}" class="tooltip-content">
                                        <pre>{{childReservationRequest.allocationStateReport}}</pre>
                                    </div>
                                </div>
                            </td>
                            <td>TODO: action</td>
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