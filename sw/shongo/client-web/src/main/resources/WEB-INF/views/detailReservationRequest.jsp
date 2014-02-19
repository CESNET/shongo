<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="advancedUserInterface" value="${sessionScope.SHONGO_USER.advancedUserInterface}"/>

<c:if test="${isActive && empty reservationRequest.parentReservationRequestId}">
    <security:accesscontrollist hasPermission="WRITE" domainObject="${reservationRequest}" var="isWritable"/>
</c:if>
<security:authorize access="hasPermission(RESERVATION)">
    <security:accesscontrollist hasPermission="PROVIDE_RESERVATION_REQUEST"
                                domainObject="${reservationRequest}" var="canCreatePermanentRoomCapacity"/>
</security:authorize>
<c:if test="${reservationRequest.specificationType != 'PERMANENT_ROOM'}">
    <c:set var="canCreatePermanentRoomCapacity" value="${false}"/>
</c:if>

<tag:url var="detailUrl" value="<%= ClientWebUrl.DETAIL_VIEW %>"/>
<tag:url var="reservationRequestModifyUrl" value="<%= ClientWebUrl.WIZARD_MODIFY %>">
    <tag:param name="reservationRequestId" value="${reservationRequest.id}"/>
</tag:url>
<tag:url var="reservationRequestDuplicateUrl" value="<%= ClientWebUrl.WIZARD_DUPLICATE %>">
    <tag:param name="reservationRequestId" value="${reservationRequest.id}"/>
</tag:url>
<tag:url var="reservationRequestDeleteUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DELETE %>">
    <tag:param name="reservationRequestId" value="${reservationRequest.id}"/>
</tag:url>

<script type="text/javascript">
    function DetailReservationRequestController($scope) {
    <c:if test="${isActive && (reservationRequest.allocationState == 'NOT_ALLOCATED' || (reservationRequest.room != null && reservationRequest.room.state != 'STOPPED' && reservationRequest.room.state != 'FAILED'))}">
        // Schedule automatic refresh
        $scope.setRefreshTimeout(function(){
            $scope.refreshTab('reservationRequest');
        });
    </c:if>
    }
</script>

<div ng-controller="DetailReservationRequestController">

<%-- History --%>
<c:if test="${history != null}">
    <div class="bordered jspReservationRequestDetailHistory">
        <h2><spring:message code="views.reservationRequestDetail.history"/></h2>
        <table class="table table-striped table-hover">
            <thead>
            <tr>
                <th><spring:message code="views.reservationRequest.dateTime"/></th>
                <th><spring:message code="views.reservationRequest.user"/></th>
                <th><spring:message code="views.reservationRequest.type"/></th>
                <c:if test="${reservationRequest.state != null}">
                    <th><spring:message code="views.reservationRequest.state"/></th>
                </c:if>
                <th><spring:message code="views.list.action"/></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${history}" var="historyItem" varStatus="status">
                <c:set var="rowClass" value=""/>
                <c:choose>
                    <c:when test="${historyItem.selected}">
                        <tr class="selected">
                    </c:when>
                    <c:otherwise>
                        <tr>
                    </c:otherwise>
                </c:choose>
                <td><tag:format value="${historyItem.dateTime}" styleShort="true"/></td>
                <td>${historyItem.user}</td>
                <td><spring:message code="views.reservationRequest.type.${historyItem.type}"/></td>
                <c:if test="${reservationRequest.state != null}">
                    <td class="reservation-request-state">
                        <c:choose>
                            <c:when test="${historyItem.selected}">
                                <span class="{{$child.state.code}}">{{$child.state.label}}</span>
                            </c:when>
                            <c:otherwise>
                                <c:if test="${historyItem.state != null}">
                                    <span class="${historyItem.state}"><spring:message code="views.reservationRequest.state.${reservationRequest.specificationType}.${historyItem.state}"/></span>
                                </c:if>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </c:if>
                <td>
                    <c:choose>
                        <c:when test="${historyItem.id != reservationRequest.id && historyItem.type != 'DELETED'}">
                            <tag:url var="detailReservationRequestTabUrl" value="<%= ClientWebUrl.DETAIL_RESERVATION_REQUEST_TAB %>">
                                <tag:param name="objectId" value="${historyItem.id}"/>
                            </tag:url>
                            <tag:listAction code="show" ngClick="refreshTab('reservationRequest', '${detailReservationRequestTabUrl}')" tabindex="2"/>
                        </c:when>
                        <c:when test="${historyItem.selected}">(<spring:message code="views.list.selected"/>)</c:when>
                    </c:choose>
                    <c:if test="${historyItem.type == 'MODIFIED' && status.first}">
                        <tag:url var="historyItemRevertUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_REVERT %>">
                            <tag:param name="reservationRequestId" value="${historyItem.id}"/>
                        </tag:url>
                            <span ng-show="$child.allocationState.code != 'ALLOCATED'">
                                | <tag:listAction code="revert" url="${historyItemRevertUrl}" tabindex="2"/>
                            </span>
                    </c:if>
                </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</c:if>

<%-- TODO: Refactorize detail to dynamic panel
<tag:url var="reservationRequestStateUrl" value="<%= ClientWebUrl.DETAIL_RESERVATION_REQUEST_STATE %>">
    <tag:param name="objectId" value="${objectId}"/>
</tag:url>
<div id="reservationRequestState" ng-controller="DynamicContentController" content-url="${reservationRequestStateUrl}">
</div>--%>

<%-- Detail of request --%>
<tag:reservationRequestDetail reservationRequest="${reservationRequest}" detailUrl="${detailUrl}" isActive="${isActive}"/>

<c:if test="${isActive}">

    <%-- Periodic events --%>
    <c:if test="${reservationRequest.periodicityType != 'NONE'}">
        <hr/>
        <tag:reservationRequestChildren detailUrl="${detailUrl}"/>
    </c:if>

    <%-- Permanent room capacities --%>
    <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
        <hr/>
        <c:if test="${canCreatePermanentRoomCapacity}">
            <tag:url var="createUsageUrl" value="<%= ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY %>">
                <tag:param name="permanentRoom" value="${reservationRequest.id}"/>
                <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
            </tag:url>
            <c:set var="createUsageWhen" value="$child.allocationState.code == 'ALLOCATED' && ($child.roomState.started || $child.roomState.code == 'NOT_STARTED')"/>
        </c:if>
        <div class="table-actions-left">
            <tag:reservationRequestUsages detailUrl="${detailUrl}" createUrl="${createUsageUrl}" createWhen="${createUsageWhen}"/>
        </div>
    </c:if>

</c:if>

</div>

<div class="table-actions pull-right">
    <a class="btn" href="#" ng-click="refreshTab('reservationRequest')" tabindex="1">
        <spring:message code="views.button.refresh"/>
    </a>
    <c:if test="${isWritable}">
        <c:if test="${advancedUserInterface}">
                <span ng-switch on="$child.state.code == 'ALLOCATED_FINISHED'">
                    <a ng-switch-when="true" class="btn" href="${reservationRequestDuplicateUrl}" tabindex="1">
                        <spring:message code="views.button.duplicate"/>
                    </a>
                    <a ng-switch-when="false" class="btn" href="${reservationRequestModifyUrl}" tabindex="1">
                        <spring:message code="views.button.modify"/>
                    </a>
                </span>
        </c:if>
        <a class="btn" href="${reservationRequestDeleteUrl}" tabindex="1">
            <spring:message code="views.button.delete"/>
        </a>
    </c:if>
</div>
