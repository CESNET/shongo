<%--
  -- List of reservation requests.
  --%>
<%@ tag trimDirectiveWhitespaces="true" %>
<%@ tag import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="name" required="false" %>
<%@attribute name="specificationType" required="false" %>
<%@attribute name="detailUrl" required="false" %>
<%@attribute name="createUrl" required="false" %>
<%@attribute name="modifyUrl" required="false" %>
<%@attribute name="duplicateUrl" required="false" %>
<%@attribute name="deleteUrl" required="false" %>
<%@attribute name="detailed" required="false" %>

<c:set var="advancedUserInterface" value="${sessionScope.user.advancedUserInterface}"/>

<c:if test="${!advancedUserInterface}">
    <c:set var="modifyUrl" value="${null}"/>
    <c:set var="duplicateUrl" value="${null}"/>
</c:if>

<c:set var="listName" value="reservation-request-list${name != null ? ('-'.concat(name)) : ''}"/>
<c:set var="listUrlQuery" value=""/>
<c:set var="listUrlParameters" value="{'specification-type': ["/>
<c:forEach items="${specificationType}" var="specificationTypeItem" varStatus="specificationTypeStatus">
    <c:set var="listUrlParameters" value="${listUrlParameters}'${specificationTypeItem}'"/>
    <c:if test="${!specificationTypeStatus.last}">
        <c:set var="listUrlParameters" value="${listUrlParameters},"/>
    </c:if>
</c:forEach>
<c:set var="listUrlParameters" value="${listUrlParameters}]}"/>
<tag:url var="listUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_LIST_DATA %>"/>

<tag:url var="reservationRequestDetailUrl" value="${detailUrl}">
    <tag:param name="reservationRequestId" value="{{reservationRequest.id}}" escape="false"/>
</tag:url>
<tag:url var="reservationRequestModifyUrl" value="${modifyUrl}">
    <tag:param name="reservationRequestId" value="{{reservationRequest.id}}" escape="false"/>
</tag:url>
<tag:url var="reservationRequestDuplicateUrl" value="${duplicateUrl}">
    <tag:param name="reservationRequestId" value="{{reservationRequest.id}}" escape="false"/>
</tag:url>
<tag:url var="reservationRequestDeleteUrl" value="${deleteUrl}">
    <tag:param name="reservationRequestId" value="{{reservationRequest.id}}" escape="false"/>
</tag:url>

<script type="text/javascript">
    angular.provideModule('tag:reservationRequestList', ['ngPagination', 'ngTooltip', 'ngSanitize']);
</script>

<div ng-controller="PaginationController"
     ng-init="init('${listName}', '${listUrl}', ${listUrlParameters})">
    <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
    <spring:message code="views.button.refresh" var="paginationRefresh"/>
    <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
        <spring:message code="views.pagination.records"/>
    </pagination-page-size>
    <jsp:doBody/>
    <div class="spinner" ng-hide="ready || errorContent"></div>
    <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
    <table class="table table-striped table-hover" ng-show="ready">
        <thead>
        <tr>
            <c:if test="${empty specificationType || specificationType.contains(',')}">
                <th><pagination-sort column="TYPE">
                    <spring:message code="views.reservationRequest.type"/></pagination-sort>
                </th>
            </c:if>
            <c:if test="${specificationType != 'ADHOC_ROOM'}">
                <th><pagination-sort column="ALIAS_ROOM_NAME">
                    <spring:message code="views.reservationRequest.specification.roomName"/></pagination-sort>
                </th>
            </c:if>
            <th style="min-width: 110px;">
                <pagination-sort column="TECHNOLOGY">
                <spring:message code="views.reservationRequest.technology"/></pagination-sort>
            </th>
            <c:if test="${specificationType == 'ADHOC_ROOM'}">
                <th><pagination-sort column="ROOM_PARTICIPANT_COUNT">
                    <spring:message code="views.reservationRequest.specification.roomParticipantCount"/></pagination-sort>
                </th>
            </c:if>
            <th>
                <pagination-sort column="SLOT">
                <spring:message code="views.reservationRequestList.slot"/></pagination-sort>
            </th>
            <th><pagination-sort column="STATE">
                <spring:message code="views.reservationRequest.state"/></pagination-sort><tag:helpReservationRequestState/>
            </th>
            <c:if test="${detailed}">
                <th style="min-width: 75px;">
                    <pagination-sort column="USER">
                    <spring:message code="views.reservationRequest.user"/></pagination-sort>
                <th style="min-width: 90px;">
                    <pagination-sort column="DATETIME">
                    <spring:message code="views.reservationRequest.dateTime"/></pagination-sort>
                </th>
            </c:if>
            <th style="min-width: 85px; width: 85px;">
                <spring:message code="views.list.action"/>
                <pagination-sort-default class="pull-right"><spring:message code="views.pagination.defaultSorting"/></pagination-sort-default>
            </th>
        </tr>
        </thead>
        <tbody ng-controller="HtmlController">
        <tr ng-repeat="reservationRequest in items"  ng-class="{'deprecated': reservationRequest.isDeprecated}" >
            <c:if test="${empty specificationType || specificationType.contains(',')}">
                <td>{{reservationRequest.typeMessage}}</td>
            </c:if>
            <c:if test="${specificationType != 'ADHOC_ROOM'}">
                <td>{{reservationRequest.roomName}}</td>
            </c:if>
            <td>{{reservationRequest.technologyTitle}}</td>
            <c:if test="${specificationType == 'ADHOC_ROOM'}">
                <td>{{reservationRequest.roomParticipantCount}}</td>
            </c:if>
            <td>
                <span ng-bind-html="html(reservationRequest.earliestSlotMultiLine)"></span>
                <span ng-show="reservationRequest.futureSlotCount">
                    <spring:message code="views.reservationRequestList.slotMore" var="slotMore" arguments="{{reservationRequest.futureSlotCount}}"/>
                    <tag:help label="(${slotMore})"
                              style="vertical-align: top;"
                              tooltipId="${listName}-slot-tooltip-{{$index}}">
                        <spring:message code="views.reservationRequestList.slotMoreHelp"/>
                    </tag:help>
                </span>
            </td>
            <td class="reservation-request-state">
                <tag:help label="{{reservationRequest.stateMessage}}"
                          labelClass="{{reservationRequest.state}}"
                          tooltipId="${listName}-state-tooltip-{{$index}}">
                    <span>{{reservationRequest.stateHelp}}</span>
                </tag:help>
            </td>
            <c:if test="${detailed}">
                <td>{{reservationRequest.user}}</td>
                <td>{{reservationRequest.dateTime}}</td>
            </c:if>
            <td>
                <c:if test="${not empty reservationRequestDetailUrl}">
                    <tag:listAction code="show" url="${reservationRequestDetailUrl }" tabindex="4"/>
                </c:if>
                <span ng-show="reservationRequest.isWritable">
                    <c:if test="${not empty reservationRequestModifyUrl}">
                        <span ng-hide="reservationRequest.state == 'ALLOCATED_FINISHED'">
                            <c:if test="${not empty reservationRequestDetailUrl}">| </c:if>
                            <tag:listAction code="modify" url="${reservationRequestModifyUrl}" tabindex="4"/>
                        </span>
                    </c:if>
                    <c:if test="${not empty reservationRequestDuplicateUrl}">
                        <span ng-show="reservationRequest.state == 'ALLOCATED_FINISHED'">
                            <c:if test="${not empty reservationRequestDuplicateUrl}">| </c:if>
                            <tag:listAction code="duplicate" url="${reservationRequestDuplicateUrl}" tabindex="4"/>
                        </span>
                    </c:if>
                    <c:if test="${not empty reservationRequestDetailUrl || (not empty reservationRequestModifyUrl && not empty reservationRequestDuplicateUrl)}"> | </c:if>
                    <c:if test="${not empty reservationRequestDeleteUrl}">
                        <tag:listAction code="delete" url="${reservationRequestDeleteUrl}" tabindex="4"/>
                    </c:if>
                </span>
            </td>
        </tr>
        </tbody>
        <tbody>
        <tr ng-hide="items.length">
            <td colspan="9" class="empty"><spring:message code="views.list.none"/></td>
        </tr>
        </tbody>
    </table>
    <pagination-pages class="pull-right"><spring:message code="views.pagination.pages"/></pagination-pages>
    <c:if test="${not empty createUrl}">
        <a class="btn btn-primary" href="${createUrl}" tabindex="1">
            <spring:message code="views.button.create"/>
        </a>
        &nbsp;
    </c:if>
</div>

