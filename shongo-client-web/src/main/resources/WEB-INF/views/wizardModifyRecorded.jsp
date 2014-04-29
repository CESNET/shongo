<%--
  -- Wizard page for modifying whether room is recorded.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="tabIndex" value="1"/>

<script type="text/javascript">
    var module = angular.module('jsp:wizardModifyRecorded', ['ngApplication', 'ngTooltip']);
</script>

<spring:message code="views.specificationType.for.${reservationRequest.specificationType}" var="specificationType"/>
<h1><spring:message code="views.wizard.modifyRecorded" arguments="${specificationType}"/></h1>

<hr/>

<div ng-app="jsp:wizardModifyRecorded">

<form:form class="form-horizontal"
           commandName="reservationRequest"
           method="post">

    <fieldset>

        <c:if test="${errors != null}">
            <div class="alert alert-error"><spring:message code="views.wizard.error.failed"/></div>
        </c:if>
        <c:set var="roomRecordedError" value="${errors.hasFieldErrors('roomRecorded') ? errors.getFieldErrors('roomRecorded')[0].defaultMessage : null}"/>

        <div class="control-group">
            <form:label class="control-label" path="id">
                <spring:message code="views.reservationRequest.identifier"/>:
            </form:label>
            <div class="controls double-width">
                <form:input path="id" readonly="true" tabindex="${tabIndex}"/>
            </div>
        </div>

        <div class="control-group" ng-hide="technology == 'ADOBE_CONNECT'">
            <form:label class="control-label" path="roomRecorded">
                <spring:message code="views.reservationRequest.specification.roomRecorded" var="roomRecordedLabel"/>
                <tag:help label="${roomRecordedLabel}:"><spring:message code="views.reservationRequest.specification.roomRecordedHelp"/></tag:help>
            </form:label>
            <div class="controls">
                <form:checkbox path="roomRecorded" cssErrorClass="error" tabindex="${tabIndex}" disabled="true"/>
                <c:if test="${not empty roomRecordedError}">
                    <span class="error">
                        ${roomRecordedError}
                    </span>
                </c:if>
            </div>
        </div>

    </fieldset>

</form:form>

</div>

<hr/>