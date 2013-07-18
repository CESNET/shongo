<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%--
  -- Page for displaying details about a single reservation request.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="detailUrl">
    ${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_DETAIL %>
</c:set>
<c:set var="backUrl">
    ${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.RESERVATION_REQUEST_LIST %>
</c:set>
<spring:eval var="modifyUrl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestModify(contextPath, reservationRequest.id)"/>
<spring:eval var="deleteUrl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDelete(contextPath, reservationRequest.id)"/>

<c:if test="${isActive && parentReservationRequestId == null}">
    <security:accesscontrollist hasPermission="WRITE" domainObject="${reservationRequest}" var="isWritable"/>
    <security:accesscontrollist hasPermission="PROVIDE_RESERVATION_REQUEST" domainObject="${reservationRequest}" var="isProvidable"/>
</c:if>

<script type="text/javascript">
    angular.module('jsp:reservationRequestDetail', ['ngPagination', 'tag:reservationRequestDetail']);
</script>

<%-- History --%>
<c:if test="${history != null}">
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
                <c:set var="rowClass" value=""/>
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
                            <spring:eval var="historyItemDetailUrl"
                                         expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, historyItem.id)"/>
                            <a href="${historyItemDetailUrl}">
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
</c:if>

<%-- Page title --%>
<h1>
    <c:choose>
        <c:when test="${parentReservationRequestId != null}">
            <spring:message code="views.reservationRequestDetail.title.child"/>
        </c:when>
        <c:otherwise>
            <spring:message code="views.reservationRequestDetail.title"/>
        </c:otherwise>
    </c:choose>
</h1>

<div ng-app="jsp:reservationRequestDetail">

    <%-- Detail of request --%>
    <tag:reservationRequestDetail reservationRequest="${reservationRequest}" detailUrl="${detailUrl}"/>

    <%-- List of user roles --%>
    <h2><spring:message code="views.reservationRequestDetail.userRoles"/></h2>
    <spring:eval var="aclUrl"
                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestAcl(contextPath, ':id')"/>
    <spring:eval var="aclCreateUrl"
                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestAclCreate(contextPath, reservationRequest.id)"/>
    <spring:eval var="aclDeleteUrl"
                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestAclDelete(contextPath, reservationRequest.id)"/>
    <tag:userRoleList dataUrl="${aclUrl}" dataUrlParameters="id: '${reservationRequest.id}'"
                      isWritable="${isWritable}" createUrl="${aclCreateUrl}" deleteUrl="${aclDeleteUrl}"/>

    <%-- Active reservation request --%>
    <c:if test="${isActive}">
        <hr/>
        <c:choose>
            <%-- List of reservations --%>
            <c:when test="${reservationRequest.periodicityType == 'NONE'}">
                <h2><spring:message code="views.reservationRequestDetail.reservations"/></h2>
                <table class="table table-striped table-hover">
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
                    <c:forEach items="${reservations}" var="reservation" varStatus="status">
                        <tr>
                            <td>
                                <joda:format value="${reservation.slot.start}" style="MS"/> -
                                <joda:format value="${reservation.slot.end}" style="MS"/>
                            </td>
                            <td class="allocation-state">
                                <c:if test="${reservation.allocationState != null}">
                                    <span id="reservationState-${status.index}" class="${reservation.allocationState}">
                                        <spring:message code="views.reservationRequest.allocationState.${reservation.allocationState}"/>
                                    </span>
                                    <tag:help label="reservationState-${status.index}">
                                        <span>
                                            <spring:message code="views.help.reservationRequest.allocationState.${reservation.allocationState}"/>
                                        </span>
                                        <c:if test="${reservation.allocationStateReport != null}">
                                            <pre>${reservation.allocationStateReport}</pre>
                                        </c:if>
                                    </tag:help>
                                </c:if>
                            </td>
                            <td class="executable-state">
                                <c:if test="${reservation.roomState != null}">
                                    <span id="executableState-${status.index}" class="${reservation.roomState}">
                                        <spring:message code="views.reservationRequest.executableState.${reservation.roomState}"/>
                                    </span>
                                    <tag:help label="executableState-${status.index}">
                                        <span>
                                            <spring:message code="views.help.reservationRequest.executableState.${reservation.roomState}"/>
                                        </span>
                                        <c:if test="${reservation.roomStateReport != null}">
                                            <pre>${reservation.roomStateReport}</pre>
                                        </c:if>
                                    </tag:help>
                                </c:if>
                            </td>
                            <td>
                                <span id="executableAliases-${status.index}">${reservation.roomAliases}</span>
                                <c:if test="${reservation.roomAliasesDescription != null}">
                                    <tag:help label="executableAliases-${status.index}">${reservation.roomAliasesDescription}</tag:help>
                                </c:if>
                            </td>
                            <td>
                                <c:if test="${reservation.roomState.available}">
                                    <spring:eval var="urlRoomManagement" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getRoomManagement(contextPath, reservation.roomId)"/>
                                    <a href="${urlRoomManagement}">
                                        <spring:message code="views.list.action.manage"/>
                                    </a>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:when>

            <%-- Multiple reservation requests dynamically --%>
            <c:otherwise>
                <spring:eval var="childListUrl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetailChildren(contextPath, ':id')"/>
                <spring:eval var="childDetailUrl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, '{{childReservationRequest.id}}')"/>
                <spring:eval var="childRoomManagementUrl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getRoomManagement(contextPath, '{{childReservationRequest.roomId}}')"/>
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
                                <tag:help label="reservationState-{{$index}}" tooltipId="reservationState-tooltip-{{$index}}">
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
                                    <tag:help label="executableState-{{$index}}" tooltipId="executableState-tooltip-{{$index}}">
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
                                <span id="executableAliases-{{$index}}" ng-bind-html-unsafe="childReservationRequest.roomAliases"></span>
                                <div ng-switch on="isEmpty(childReservationRequest.roomAliasesDescription)" style="display: inline-block;">
                                    <div ng-switch-when="false">
                                        <tag:help label="executableAliases-{{$index}}" tooltipId="executableAliases-tooltip-{{$index}}">
                                            <span ng-bind-html-unsafe="childReservationRequest.roomAliasesDescription"></span>
                                        </tag:help>
                                    </div>
                                </div>
                            </td>
                            <td>
                                <a href="${childDetailUrl}"><spring:message code="views.list.action.show"/></a>
                                <span ng-show="childReservationRequest.roomStateAvailable">
                                    | <a href="${childRoomManagementUrl}">
                                        <spring:message code="views.list.action.manage"/>
                                    </a>
                                </span>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                    <pagination-pages><spring:message code="views.pagination.pages"/></pagination-pages>
                </div>
            </c:otherwise>
        </c:choose>
    </c:if>

    <%-- Permanent room capacities --%>
    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
        <spring:eval var="usageListUrl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetailUsages(contextPath, ':id')"/>
        <spring:eval var="usageDetailUrl" expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(detailUrl, '{{permanentRoomCapacity.id}}')"/>
        <hr/>
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
                <c:when test="${isProvidable && reservationRequest.slot.containsNow()}">
                    <c:set var="urlCreate">${contextPath}<%= ClientWebUrl.RESERVATION_REQUEST_CREATE %></c:set>
                    <a class="btn btn-primary" href="${urlCreate}?type=PERMANENT_ROOM_CAPACITY&permanentRoom=${reservationRequest.id}">
                        <spring:message code="views.button.create"/>
                    </a>
                    <pagination-pages class="pull-right"><spring:message code="views.pagination.pages"/></pagination-pages>
                </c:when>
                <c:otherwise>
                    <pagination-pages><spring:message code="views.pagination.pages"/></pagination-pages>
                </c:otherwise>
            </c:choose>
        </div>
    </c:if>

</div>

<div class="pull-right">
    <a class="btn btn-primary" href="${backUrl}">
        <spring:message code="views.button.back"/>
    </a>
    <a class="btn" href="javascript: location.reload();">
        <spring:message code="views.button.refresh"/>
    </a>
    <c:if test="${isWritable}">
        <a class="btn" href="${modifyUrl}">
            <spring:message code="views.button.modify"/>
        </a>
        <a class="btn" href="${deleteUrl}">
            <spring:message code="views.button.delete"/>
        </a>
    </c:if>
</div>