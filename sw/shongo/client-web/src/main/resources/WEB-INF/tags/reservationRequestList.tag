<%--
  -- List of reservation requests.
  --%>
<%@ tag trimDirectiveWhitespaces="true" %>

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

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="advancedUserInterface" value="${sessionScope.user.advancedUserInterface}"/>

<c:if test="${!advancedUserInterface}">
    <c:set var="modifyUrl" value="${null}"/>
</c:if>

<c:set var="listName" value="reservation-request-list${name != null ? ('-'.concat(name)) : ''}"/>
<c:set var="listUrl">
    ${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_LIST_DATA %>
</c:set>
<c:set var="listUrlQuery" value=""/>
<c:set var="listUrlParameters" value="{'specificationType': ["/>
<c:forEach items="${specificationType}" var="specificationTypeItem" varStatus="specificationTypeStatus">
    <c:set var="listUrlParameters" value="${listUrlParameters}'${specificationTypeItem}'"/>
    <c:if test="${!specificationTypeStatus.last}">
        <c:set var="listUrlParameters" value="${listUrlParameters},"/>
    </c:if>
</c:forEach>
<c:set var="listUrlParameters" value="${listUrlParameters}]}"/>
<c:if test="${detailUrl != null}">
    <spring:eval var="detailUrl"
                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, '{{reservationRequest.id}}')"/>
</c:if>
<c:if test="${modifyUrl != null}">
    <spring:eval var="modifyUrl"
                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(modifyUrl, '{{reservationRequest.id}}')"/>
</c:if>
<c:if test="${duplicateUrl != null}">
    <spring:eval var="duplicateUrl"
                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(duplicateUrl, '{{reservationRequest.id}}')"/>
</c:if>
<c:if test="${deleteUrl != null}">
    <spring:eval var="deleteUrl"
                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(deleteUrl, '{{reservationRequest.id}}')"/>
</c:if>

<script type="text/javascript">
    angular.provideModule('tag:reservationRequestList', ['ngPagination', 'ngTooltip', 'ngSanitize']);

    function HtmlController($scope, $sce) {
        $scope.html = function(html) {
            return $sce.trustAsHtml(html);
        };
    }
</script>

<div ng-controller="PaginationController"
     ng-init="init('${listName}', '${listUrl}', ${listUrlParameters})">
    <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
    <spring:message code="views.button.refresh" var="paginationRefresh"/>
    <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
        <spring:message code="views.pagination.records"/>
    </pagination-page-size>
    <jsp:doBody/>
    <div class="spinner" ng-hide="ready"></div>
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
            <th><pagination-sort column="TECHNOLOGY">
                <spring:message code="views.reservationRequest.technology"/></pagination-sort>
            </th>
            <c:if test="${specificationType == 'ADHOC_ROOM'}">
                <th><pagination-sort column="ROOM_PARTICIPANT_COUNT">
                    <spring:message code="views.reservationRequest.specification.roomParticipantCount"/></pagination-sort>
                </th>
            </c:if>
            <th><pagination-sort column="SLOT">
                <spring:message code="views.reservationRequestList.slot"/></pagination-sort>
            </th>
            <th><pagination-sort column="STATE">
                <spring:message code="views.reservationRequest.state"/></pagination-sort><tag:helpReservationRequestState/>
            </th>
            <c:if test="${detailed}">
                <th><pagination-sort column="USER">
                    <spring:message code="views.reservationRequest.user"/></pagination-sort>
                </th>
                <th><pagination-sort column="DATETIME">
                    <spring:message code="views.reservationRequest.dateTime"/></pagination-sort>
                </th>
            </c:if>
            <th style="width: 85px;">
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
            <td>{{reservationRequest.technology}}</td>
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
                <c:if test="${detailUrl != null}">
                    <tag:listAction code="show" url="${detailUrl}" tabindex="4"/>
                </c:if>
                <span ng-show="reservationRequest.isWritable">
                    <c:if test="${modifyUrl != null}">
                        <span ng-hide="reservationRequest.state == 'ALLOCATED_FINISHED'">
                            <c:if test="${detailUrl != null}">| </c:if>
                            <tag:listAction code="modify" url="${modifyUrl}" tabindex="4"/>
                        </span>
                    </c:if>
                    <c:if test="${duplicateUrl != null}">
                        <span ng-show="reservationRequest.state == 'ALLOCATED_FINISHED'">
                            <c:if test="${duplicateUrl != null}">| </c:if>
                            <tag:listAction code="duplicate" url="${duplicateUrl}" tabindex="4"/>
                        </span>
                    </c:if>
                    <c:if test="${detailUrl != null || (modifyUrl != null && duplicateUrl != null)}"> | </c:if>
                    <c:if test="${deleteUrl != null}">
                        <tag:listAction code="delete" url="${deleteUrl}" tabindex="4"/>
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
    <c:if test="${createUrl != null}">
        <a class="btn btn-primary" href="${createUrl}" tabindex="1">
            <spring:message code="views.button.create"/>
        </a>
        &nbsp;
    </c:if>
</div>

