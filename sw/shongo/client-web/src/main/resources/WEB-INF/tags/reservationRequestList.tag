<%--
  -- List of reservation requests.
  --%>
<%@ tag trimDirectiveWhitespaces="true" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>

<%@attribute name="name" required="false" %>
<%@attribute name="specificationType" required="false" %>
<%@attribute name="detailUrl" required="false" %>
<%@attribute name="createUrl" required="false" %>
<%@attribute name="modifyUrl" required="false" %>
<%@attribute name="deleteUrl" required="false" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="listName" value="${name != null ? ('reservationRequestList.' + name) : 'reservationRequestList'}"/>
<c:set var="listUrl">
    ${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_LIST_DATA %>
</c:set>
<c:set var="listUrlQuery" value=""/>
<c:forEach items="${specificationType}" var="specificationType">
    <c:set var="listUrlQuery" value="${listUrlQuery}&type=${specificationType}"/>
</c:forEach>
<c:if test="${detailUrl != null}">
    <spring:eval var="detailUrl"
                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, '{{reservationRequest.id}}')"/>
</c:if>
<c:if test="${modifyUrl != null}">
    <spring:eval var="modifyUrl"
                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(modifyUrl, '{{reservationRequest.id}}')"/>
</c:if>
<c:if test="${deleteUrl != null}">
    <spring:eval var="deleteUrl"
                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(deleteUrl, '{{reservationRequest.id}}')"/>
</c:if>

<script type="text/javascript">
    angular.provideModule('tag:reservationRequestList', ['ngPagination', 'ngTooltip']);
</script>

<div ng-controller="PaginationController"
     ng-init="init('${listName}', '${listUrl}?start=:start&count=:count${listUrlQuery}')">
    <pagination-page-size class="pull-right">
        <spring:message code="views.pagination.records"/>
    </pagination-page-size>
    <jsp:doBody/>
    <div class="spinner" ng-hide="ready"></div>
    <table class="table table-striped table-hover" ng-show="ready">
        <thead>
        <tr>
            <th><spring:message code="views.reservationRequest.type"/></th>
            <th><spring:message code="views.reservationRequest.specification.permanentRoomName"/></th>
            <th><spring:message code="views.reservationRequest.technology"/></th>
            <th><spring:message code="views.reservationRequest.slot"/></th>
            <th><spring:message code="views.reservationRequest.state"/></th>
            <th><spring:message code="views.list.action"/></th>
        </tr>
        </thead>
        <tbody>
        <tr ng-repeat="reservationRequest in items">
            <td>{{reservationRequest.type}}</td>
            <td>{{reservationRequest.roomName}}</td>
            <td>{{reservationRequest.technology}}</td>
            <td>{{reservationRequest.earliestSlotStart}}<br/>{{reservationRequest.earliestSlotEnd}}</td>
            <td class="reservation-request-state">
                <span class="{{reservationRequest.state}}">
                    {{reservationRequest.stateMessage}}
                </span>
            </td>
            <td>
                <c:if test="${detailUrl != null}">
                    <a href="${detailUrl}" tabindex="4"><spring:message code="views.list.action.show"/></a>
                </c:if>
                    <span ng-show="reservationRequest.writable">
                        <c:if test="${modifyUrl != null}">
                            <c:if test="${detailUrl != null}">| </c:if>
                            <a href="${modifyUrl}" tabindex="4"><spring:message code="views.list.action.modify"/></a>
                        </c:if>
                        <c:if test="${deleteUrl != null}">
                            <c:if test="${detailUrl != null || modifyUrl != null}">| </c:if>
                            <a href="${deleteUrl}" tabindex="4"><spring:message code="views.list.action.delete"/></a>
                        </c:if>
                    </span>
            </td>
        </tr>
        <tr ng-hide="items.length">
            <td colspan="6" class="empty"><spring:message code="views.list.none"/></td>
        </tr>
        </tbody>
    </table>
    <pagination-pages class="pull-right"><spring:message code="views.pagination.pages"/></pagination-pages>
    <c:if test="${createUrl != null}">
        <a class="btn btn-primary" href="${createUrl}?type=PERMANENT_ROOM" tabindex="1">
            <spring:message code="views.button.create"/>
        </a>
        &nbsp;
    </c:if>
</div>

