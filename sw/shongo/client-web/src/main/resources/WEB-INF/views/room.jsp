<%--
  -- Page for displaying details about a single reservation request.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<script type="text/javascript">
    // Angular application
    angular.module('ngRoom', ['ngTooltip']);
</script>

<div ng-app="ngRoom">

    <dl class="dl-horizontal">

        <dt><spring:message code="views.room.identifier"/>:</dt>
        <dd>${executable.id}</dd>

        <dt><spring:message code="views.reservationRequest.slot"/>:</dt>
        <dd>
            <joda:format value="${executable.slot.start}" style="MM"/>
            <br/>
            <joda:format value="${executable.slot.end}" style="MM"/>
        </dd>

        <dt><spring:message code="views.room.state"/>:</dt>
        <dd class="executable-state">
            <span class="${executable.state}" id="executableState">
                <spring:message code="views.reservationRequest.executableState.${executable.state}"/>
            </span>
            <app:help label="executableState">
                <span>
                    <spring:message code="views.help.reservationRequest.executableState.${executable.state}"/>
                </span>
                <c:if test="${!executable.state.available && executable.stateReport != null}">
                    <pre>${executable.stateReport}</pre>
                </c:if>
            </app:help>
        </dd>

    </dl>

    <c:if test="${participants != null}">
        <h2><spring:message code="views.room.participants"/></h2>
        <table class="table table-striped table-hover">
            <thead>
            <tr>
                <th><spring:message code="views.room.participant.name"/></th>
                <th><spring:message code="views.room.participant.email"/></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${participants}" var="participant" varStatus="status">
                <tr>
                    <td>${participant.fullName}</td>
                    <td>${participant.primaryEmail}</td>
                </tr>
            </c:forEach>
            <c:if test="${participants.isEmpty()}">
                <tr>
                    <td colspan="2" class="empty">- - - None - - -</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </c:if>

    <c:if test="${recordings != null}">
        <h2><spring:message code="views.room.recordings"/></h2>
        <table class="table table-striped table-hover">
            <thead>
            <tr>
                <th><spring:message code="views.room.recording.url"/></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${recordings}" var="recording" varStatus="status">
                <tr>
                    <td>
                        <a href="${recording}" target="_blank">${recording}</a>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${recordings.isEmpty()}">
                <tr>
                    <td colspan="2" class="empty">- - - None - - -</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </c:if>
</div>