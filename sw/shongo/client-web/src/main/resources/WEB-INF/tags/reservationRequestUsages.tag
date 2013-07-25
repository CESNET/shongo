<%--
  -- Usages of reservation request.
  --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="detailUrl" required="true" %>
<%@attribute name="createUrl" required="false" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<script type="text/javascript">
    angular.provideModule('tag:reservationRequestUsages', ['ngPagination']);
</script>

<spring:eval var="usageListUrl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetailUsages(contextPath, ':id')"/>
<spring:eval var="usageDetailUrl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, '{{permanentRoomCapacity.id}}')"/>
<div ng-controller="PaginationController"
     ng-init="init('reservationRequestDetail.permanentRoomCapacities', '${usageListUrl}?start=:start&count=:count', {id: '${reservationRequest.id}'})">
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
                <tag:help label="permanentRoomCapacityState-{{$index}}" tooltipId="reservationState-tooltip-{{$index}}">
                    <span>{{permanentRoomCapacity.allocationStateHelp}}</span>
                </tag:help>
            </td>
            <td>
                <a href="${usageDetailUrl}"><spring:message code="views.list.action.show"/></a>
            </td>
        </tr>
        <tr ng-hide="items.length">
            <td colspan="4" class="empty"><spring:message code="views.list.none"/></td>
        </tr>
        </tbody>
    </table>
    <c:choose>
        <c:when test="${createUrl != null}">
            <a class="btn btn-primary" href="${createUrl}">
                <spring:message code="views.button.create"/>
            </a>
            <pagination-pages class="pull-right"><spring:message code="views.pagination.pages"/></pagination-pages>
        </c:when>
        <c:otherwise>
            <pagination-pages><spring:message code="views.pagination.pages"/></pagination-pages>
        </c:otherwise>
    </c:choose>
</div>