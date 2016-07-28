<%--
  -- Usages of reservation request.
  --%>
<%@ tag body-content="empty" %>
<%@ tag import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="detailUrl" required="true" %>
<%@attribute name="createUrl" required="false" %>
<%@attribute name="createWhen" required="false" %>

<tag:url var="usageListUrl" value="<%= ClientWebUrl.DETAIL_RESERVATION_REQUEST_USAGES %>">
    <tag:param name="objectId" value=":id"/>
</tag:url>
<tag:url var="usageDetailUrl" value="${detailUrl}">
    <tag:param name="objectId" value="{{usage.id}}" escape="false"/>
</tag:url>
<tag:url var="usageModifyUrl" value="<%= ClientWebUrl.WIZARD_MODIFY %>">
    <tag:param name="reservationRequestId" value="{{usage.id}}" escape="false"/>
    <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
</tag:url>
<tag:url var="usageDuplicateUrl" value="<%= ClientWebUrl.WIZARD_DUPLICATE %>">
    <tag:param name="reservationRequestId" value="{{usage.id}}" escape="false"/>
    <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
</tag:url>
<tag:url var="usageDeleteUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DELETE %>">
    <tag:param name="reservationRequestId" value="{{usage.id}}" escape="false"/>
    <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
</tag:url>
<tag:url var="reservationRequestMultipleDeleteUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DELETE %>" />

<script type="text/javascript">
    angular.provideModule('tag:reservationRequestUsages', ['ngPagination']);
</script>

<c:set var="deleteCheckboxName" value="usageDeleteCheckbox" />

<div ng-controller="PaginationController"
     ng-init="init('reservationRequestDetail.usages', '${usageListUrl}', {id: '${reservationRequest.id}'}, null, '${reservationRequestMultipleDeleteUrl}', '${deleteCheckboxName}')">
    <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
    <spring:message code="views.button.refresh" var="paginationRefresh"/>
    <spring:message code="views.button.remove" var="paginationRemove"/>
    <spring:message code="views.button.removeAll" var="paginationRemoveAll"/>

    <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}" remove="${paginationRemove}" remove-all="${paginationRemoveAll}">
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
            <th style="min-width: 110px; width: 110px;">
                <spring:message code="views.list.action"/>
                <pagination-sort-default class="pull-right"><spring:message code="views.pagination.defaultSorting"/></pagination-sort-default>
            </th>
        </tr>
        </thead>
        <tbody>
        <tr ng-repeat="usage in items">
            <td>
                {{usage.slot}}
                <span ng-show="usage.futureSlotCount">
                    <spring:message code="views.reservationRequestList.slotMore" var="slotMore" arguments="{{usage.futureSlotCount}}"/>
                    <tag:help label="(${slotMore})">
                        <spring:message code="views.reservationRequestList.slotMoreHelp"/>
                    </tag:help>
                </span>
            </td>
            <td>{{usage.roomParticipantCount}}</td>
            <td class="reservation-request-state">
                <tag:help label="{{usage.stateMessage}}" cssClass="{{usage.state}}">
                    <span>{{usage.stateHelp}}</span>
                </tag:help>
            </td>
            <td>
                <tag:listAction code="show" url="${usageDetailUrl}" tabindex="2"/>
                <span ng-show="usage.isWritable">
                    <span ng-hide="usage.state == 'ALLOCATED_FINISHED'">
                        | <tag:listAction code="modify" url="${usageModifyUrl}" tabindex="4"/>
                    </span>
                    <span ng-show="usage.state == 'ALLOCATED_FINISHED'">
                        | <tag:listAction code="duplicate" url="${usageDuplicateUrl}" tabindex="4"/>
                    </span>
                    | <tag:listAction code="delete" url="${usageDeleteUrl}" tabindex="4"/>
                    | <input type="checkbox" name="${deleteCheckboxName}" value="{{usage.id}}"/>
                </span>
            </td>
        </tr>
        </tbody>
        <tbody>
        <tr ng-hide="items.length">
            <td colspan="4" class="empty"><spring:message code="views.list.none"/></td>
        </tr>
        </tbody>
    </table>
    <c:if test="${not empty createUrl}">
        <div class="table-actions">
            <a class="btn btn-primary" href="${createUrl}" tabindex="1" ng-show="${not empty createWhen ? createWhen : 'true'}">
                <spring:message code="views.reservationRequestDetail.action.createPermanentRoomCapacity"/>
            </a>
        </div>
    </c:if>
    <pagination-pages class="${(not empty createUrl) ? 'pull-right' : ''}" ng-show="ready">
        <spring:message code="views.pagination.pages"/>
    </pagination-pages>
    <div class="clearfix"></div>
</div>