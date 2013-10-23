<%--
  -- Usages of reservation request.
  --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ tag import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="detailUrl" required="true" %>
<%@attribute name="createUrl" required="false" %>
<%@attribute name="createWhen" required="false" %>

<tag:url var="usageListUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DETAIL_USAGES %>">
    <tag:param name="reservationRequestId" value=":id"/>
</tag:url>
<tag:url var="usageDetailUrl" value="${detailUrl}">
    <tag:param name="reservationRequestId" value="{{permanentRoomCapacity.id}}" escape="false"/>
</tag:url>

<script type="text/javascript">
    angular.provideModule('tag:reservationRequestUsages', ['ngPagination']);
</script>

<div ng-controller="PaginationController"
     ng-init="init('reservationRequestDetail.permanentRoomUsages', '${usageListUrl}', {id: '${reservationRequest.id}'})">
    <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
    <spring:message code="views.button.refresh" var="paginationRefresh"/>
    <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
        <spring:message code="views.pagination.records"/>
    </pagination-page-size>
    <h2><spring:message code="views.reservationRequestDetail.permanentRoomCapacities"/></h2>
    <div class="spinner" ng-hide="ready || errorContent"></div>
    <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
    <table class="table table-striped table-hover" ng-show="ready">
        <thead>
        <tr>
            <th width="400px"><pagination-sort column="SLOT">
                <spring:message code="views.reservationRequestList.slot"/></pagination-sort>
            </th>
            <th><pagination-sort column="ROOM_PARTICIPANT_COUNT">
                <spring:message code="views.reservationRequest.specification.roomParticipantCount"/></pagination-sort>
            </th>
            <th><pagination-sort column="STATE">
                <spring:message code="views.reservationRequest.state"/></pagination-sort>
            </th>
            <th style="min-width: 85px; width: 85px;">
                <spring:message code="views.list.action"/>
                <pagination-sort-default class="pull-right"><spring:message code="views.pagination.defaultSorting"/></pagination-sort-default>
            </th>
        </tr>
        </thead>
        <tbody>
        <tr ng-repeat="permanentRoomCapacity in items">
            <td>
                {{permanentRoomCapacity.slot}}
                <span ng-show="permanentRoomCapacity.futureSlotCount">
                    <spring:message code="views.reservationRequestList.slotMore" var="slotMore" arguments="{{permanentRoomCapacity.futureSlotCount}}"/>
                    <tag:help label="(${slotMore})">
                        <spring:message code="views.reservationRequestList.slotMoreHelp"/>
                    </tag:help>
                </span>
            </td>
            <td>{{permanentRoomCapacity.roomParticipantCount}}</td>
            <td class="reservation-request-state">
                <tag:help label="{{permanentRoomCapacity.stateMessage}}"
                          labelClass="{{permanentRoomCapacity.state}}"
                          tooltipId="reservationState-tooltip-{{$index}}">
                    <span>{{permanentRoomCapacity.stateHelp}}</span>
                </tag:help>
            </td>
            <td>
                <tag:listAction code="show" url="${usageDetailUrl}" tabindex="2"/>
            </td>
        </tr>
        </tbody>
        <tbody>
        <tr ng-hide="items.length">
            <td colspan="4" class="empty"><spring:message code="views.list.none"/></td>
        </tr>
        </tbody>
    </table>
    <pagination-pages class="${(not empty createUrl) ? 'pull-right' : ''}" ng-show="ready">
        <spring:message code="views.pagination.pages"/>
    </pagination-pages>
</div>
<c:if test="${not empty createUrl}">
    <div class="table-actions">
        <a class="btn btn-primary" href="${createUrl}" tabindex="1" ng-show="${not empty createWhen ? createWhen : 'true'}">
            <spring:message code="views.button.create"/>
        </a>
    </div>
</c:if>