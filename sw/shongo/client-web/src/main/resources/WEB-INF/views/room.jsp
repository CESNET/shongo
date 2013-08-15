<%--
  -- Page for displaying details about a single reservation request.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<spring:eval expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestDetail(contextPath, room.reservationRequestId)" var="urlDetail"/>

<script type="text/javascript">
    angular.module('jsp:room', ['ngTooltip']);
</script>

<div ng-app="jsp:room">

    <dl class="dl-horizontal">

        <dt><spring:message code="views.room.technology"/>:</dt>
        <dd>${room.technology.title}</dd>

        <dt><spring:message code="views.reservationRequest.slot"/>:</dt>
        <dd>
            <joda:format value="${room.slot.start}" style="MM"/>
            <br/>
            <joda:format value="${room.slot.end}" style="MM"/>
        </dd>

        <dt><spring:message code="views.room.state"/>:</dt>
        <dd class="room-state">
            <spring:message code="views.executable.roomState.${room.state}" var="roomStateLabel"/>
            <spring:message code="help.executable.roomState.${room.state}" var="roomStateHelp"/>
            <tag:help label="${roomStateLabel}" labelClass="${room.state}">
                <span>${roomStateHelp}</span>
                <c:if test="${!room.state.available && not empty room.stateReport}">
                    <pre>${room.stateReport}</pre>
                </c:if>
            </tag:help>
        </dd>

        <c:if test="${roomRuntime != null}">
            <dt><spring:message code="views.room.licenseCount"/>:</dt>
            <dd>
                ${roomRuntime.licenseCount}
                <c:if test="${roomRuntime.licenseCount == 0}">
                    <spring:eval var="createPermanentRoomCapacityUrl"
                                 expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).getReservationRequestCreatePermanentRoomCapacity(contextPath, room.reservationRequestId)"/>
                    (<spring:message code="views.room.licenseCount.none" arguments="${createPermanentRoomCapacityUrl}"/>)
                </c:if>
            </dd>
        </c:if>

        <dt><spring:message code="views.room.aliases"/>:</dt>
        <dd>
            <tag:help label="${room.aliases}">
                <c:set value="${room.aliasesDescription}" var="roomAliasesDescription"/>
                <c:if test="${not empty roomAliasesDescription}">
                    ${roomAliasesDescription}
                </c:if>
            </tag:help>
        </dd>

        <dt><spring:message code="views.room.identifier"/>:</dt>
        <dd>${room.id}</dd>

        <dt><spring:message code="views.reservationRequest"/>:</dt>
        <dd><a href="${urlDetail}">${reservationRequestId}</a></dd>

    </dl>

    <c:if test="${roomNotAvailable}">
        <div class="not-available">
            <h2><spring:message code="views.room.notAvailable.heading"/></h2>
            <p><spring:message code="views.room.notAvailable.text" arguments="${configuration.contactEmail}"/></p>
        </div>
    </c:if>

    <c:if test="${roomParticipants != null}">
        <h2><spring:message code="views.room.participants"/></h2>
        <table class="table table-striped table-hover">
            <thead>
            <tr>
                <th><spring:message code="views.room.participant.name"/></th>
                <th><spring:message code="views.room.participant.email"/></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${roomParticipants}" var="participant" varStatus="status">
                <tr>
                    <td>${participant.name}</td>
                    <td>
                        ${participant.user.primaryEmail}
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${roomParticipants.isEmpty()}">
                <tr>
                    <td colspan="2" class="empty"><spring:message code="views.list.none"/></td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </c:if>

    <security:accesscontrollist hasPermission="WRITE" domainObject="${room}" var="isWritable"/>

    <c:if test="${roomRecordings != null}">
        <h2><spring:message code="views.room.recordings"/></h2>
        <table class="table table-striped table-hover">
            <thead>
            <tr>
                <th><spring:message code="views.room.recording.name"/></th>
                <th><spring:message code="views.room.recording.uploaded"/></th>
                <th><spring:message code="views.room.recording.duration"/></th>
                <th>
                    <c:choose>
                        <c:when test="${isWritable}">
                            <spring:message code="views.room.recording.editableUrl"/>
                        </c:when>
                        <c:otherwise>
                            <spring:message code="views.room.recording.url"/>
                        </c:otherwise>
                    </c:choose>
                </th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${roomRecordings}" var="recording" varStatus="status">
                <tr>
                    <td>
                        <c:choose>
                            <c:when test="${not empty recording.description}">
                                <tag:help label="${recording.name}">
                                    <strong><spring:message code="views.room.recording.description"/>:</strong>
                                    ${recording.description}
                                </tag:help>
                            </c:when>
                            <c:otherwise>
                                ${recording.name}
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <joda:format value="${recording.beginDate}" style="MM"/>
                    </td>
                    <td>
                        <joda:format value="${recording.duration}" pattern="HH:mm:ss"/>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${isWritable}">
                                <a href="${recording.editableUrl}" target="_blank">${recording.editableUrl}</a>
                            </c:when>
                            <c:otherwise>
                                <a href="${recording.url}" target="_blank">${recording.url}</a>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${roomRecordings.isEmpty()}">
                <tr>
                    <td colspan="4" class="empty"><spring:message code="views.list.none"/></td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </c:if>
</div>

<div class="pull-right">
    <a class="btn" href="javascript: location.reload();">
        <spring:message code="views.button.refresh"/>
    </a>
</div>