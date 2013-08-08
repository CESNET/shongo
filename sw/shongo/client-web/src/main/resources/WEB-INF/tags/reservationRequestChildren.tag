<%--
  -- Child reservation requests.
  --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="detailUrl" required="true" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<script type="text/javascript">
    angular.provideModule('tag:reservationRequestChildren', ['ngPagination']);
</script>

<spring:eval var="childListUrl"
             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetailChildren(contextPath, ':id')"/>
<spring:eval var="childDetailUrl"
             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, '{{childReservationRequest.id}}')"/>
<spring:eval var="childRoomManagementUrl"
             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getRoomManagement(contextPath, '{{childReservationRequest.roomId}}')"/>
<div ng-controller="PaginationController"
     ng-init="init('reservationRequestDetail.children', '${childListUrl}?start=:start&count=:count', {id: '${reservationRequest.id}'})">
    <pagination-page-size class="pull-right">
        <spring:message code="views.pagination.records"/>
    </pagination-page-size>
    <h2><spring:message code="views.reservationRequestDetail.children"/></h2>

    <div class="spinner" ng-hide="ready"></div>
    <table class="table table-striped table-hover" ng-show="ready">
        <thead>
        <tr>
            <th width="320px"><spring:message code="views.reservationRequest.slot"/></th>
            <th><spring:message code="views.reservationRequest.state"/></th>
            <th><spring:message code="views.room.aliases"/></th>
            <th width="120px"><spring:message code="views.list.action"/></th>
        </tr>
        </thead>
        <tbody>
        <tr ng-repeat="childReservationRequest in items">
            <td>{{childReservationRequest.slot}}</td>
            <td class="reservation-request-state">
                <tag:help label="{{childReservationRequest.stateMessage}}"
                          labelClass="{{childReservationRequest.state}}"
                          tooltipId="reservationState-tooltip-{{$index}}">
                    <span>{{childReservationRequest.stateHelp}}</span>
                    <div ng-switch on="isEmpty(childReservationRequest.stateReport)">
                        <div ng-switch-when="false">
                            <pre>{{childReservationRequest.stateReport}}</pre>
                        </div>
                    </div>
                </tag:help>
            </td>
            <td>
                <div ng-switch on="isEmpty(childReservationRequest.roomAliasesDescription)" style="display: inline-block;">
                    <div ng-switch-when="false">
                        <c:set var="executableAliases">
                            <span ng-bind-html-unsafe="childReservationRequest.roomAliases"></span>
                        </c:set>
                        <tag:help label="${executableAliases}"
                                  tooltipId="executableAliases-tooltip-{{$index}}">
                            <span ng-bind-html-unsafe="childReservationRequest.roomAliasesDescription"></span>
                        </tag:help>
                    </div>
                    <span ng-switch-when="true"
                          ng-bind-html-unsafe="childReservationRequest.roomAliases"></span>
                </div>
            </td>
            <td>
                <a href="${childDetailUrl}" tabindex="2"><spring:message code="views.list.action.show"/></a>
                <span ng-show="childReservationRequest.roomStateAvailable">
                    | <a href="${childRoomManagementUrl}" tabindex="2"><spring:message code="views.list.action.manage"/></a>
                </span>
            </td>
        </tr>
        <tr ng-hide="items.length">
            <td colspan="4" class="empty"><spring:message code="views.list.none"/></td>
        </tr>
        </tbody>
    </table>
    <pagination-pages><spring:message code="views.pagination.pages"/></pagination-pages>
</div>