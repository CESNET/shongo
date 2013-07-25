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
                <tag:help label="reservationState-{{$index}}"
                          tooltipId="reservationState-tooltip-{{$index}}">
                    <span>{{childReservationRequest.allocationStateHelp}}</span>
                    <div ng-switch on="isEmpty(childReservationRequest.allocationStateReport)">
                        <div ng-switch-when="false">
                            <pre>{{childReservationRequest.allocationStateReport}}</pre>
                        </div>
                    </div>
                </tag:help>
            </td>
            <td class="executable-state">
                <div ng-show="childReservationRequest.roomState">
                    <span id="executableState-{{$index}}" class="{{childReservationRequest.roomState}}">{{childReservationRequest.roomStateMessage}}</span>
                    <tag:help label="executableState-{{$index}}"
                              tooltipId="executableState-tooltip-{{$index}}">
                        <span>{{childReservationRequest.roomStateHelp}}</span>
                        <div ng-switch on="isEmpty(childReservationRequest.roomStateReport)">
                            <div ng-switch-when="false">
                                <pre>{{childReservationRequest.roomStateReport}}</pre>
                            </div>
                        </div>
                    </tag:help>
                </div>
            </td>
            <td>
                <span id="executableAliases-{{$index}}"
                      ng-bind-html-unsafe="childReservationRequest.roomAliases"></span>
                <div ng-switch on="isEmpty(childReservationRequest.roomAliasesDescription)"
                     style="display: inline-block;">
                    <div ng-switch-when="false">
                        <tag:help label="executableAliases-{{$index}}"
                                  tooltipId="executableAliases-tooltip-{{$index}}">
                            <span ng-bind-html-unsafe="childReservationRequest.roomAliasesDescription"></span>
                        </tag:help>
                    </div>
                </div>
            </td>
            <td>
                <a href="${childDetailUrl}"><spring:message code="views.list.action.show"/></a>
                <span ng-show="childReservationRequest.roomStateAvailable">
                    | <a href="${childRoomManagementUrl}"><spring:message code="views.list.action.manage"/></a>
                </span>
            </td>
        </tr>
        </tbody>
    </table>
    <pagination-pages><spring:message code="views.pagination.pages"/></pagination-pages>
</div>