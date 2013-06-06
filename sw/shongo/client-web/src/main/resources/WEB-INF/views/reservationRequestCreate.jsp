<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<c:set var="path" value="${pageContext.request.contextPath}"/>

<form:form class="form-horizontal"
           commandName="reservationRequest"
           action="${path}/reservation-request/create/confirmed"
           method="post">

    <div class="control-group">
        <form:label class="control-label" path="id"><spring:message
                code="views.reservationRequest.identifier"/>:</form:label>
        <div class="controls">
            <form:input path="id" readonly="true"/>
        </div>
    </div>

    <div class="control-group">
        <form:label class="control-label" path="description"><spring:message
                code="views.reservationRequest.description"/>:</form:label>
        <div class="controls">
            <form:input path="description" cssErrorClass="error"/>
            <form:errors path="description" cssClass="error"/>
        </div>
    </div>

    <div class="control-group">
        <div class="controls">
            <spring:message code="views.button.create" var="create"/>
            <input class="btn btn-primary" type="submit" value="${create}"/>
            <a class="btn" href="${path}/reservation-request"><spring:message code="views.button.cancel"/></a>
        </div>
    </div>

</form:form>