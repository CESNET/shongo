<%--
  -- Page for creation/modification of a reservation request.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.models.ReservationRequestModel" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<tiles:importAttribute/>

<div>

<form:form class="form-horizontal"
           commandName="aclRecord"
           action="${contextPath}${confirmUrl}"
           method="post">

    <fieldset>

        <form:hidden path="id"/>

        <div class="control-group">
            <form:label class="control-label" path="entityId">
                <spring:message code="${entity}"/>:
            </form:label>
            <div class="controls">
                <form:input path="entityId" readonly="true"/>
            </div>
        </div>

        <div class="control-group">
            <form:label class="control-label" path="userId">
                <spring:message code="views.aclRecord.user"/>:
            </form:label>
            <div class="controls">
                <form:input path="userId" cssErrorClass="error"/>
                <form:errors path="userId" cssClass="error"/>
            </div>
        </div>

        <div class="control-group">
            <form:label class="control-label" path="role">
                <spring:message code="views.aclRecord.role"/>:
            </form:label>
            <div class="controls">
                <form:select path="role">
                    <c:forEach items="${roles}" var="role">
                        <form:option value="${role}"><spring:message code="views.aclRecord.role.${role}"/></form:option>
                    </c:forEach>
                </form:select>
                <form:errors path="role" cssClass="error"/>
            </div>
        </div>

    </fieldset>

    <div class="control-group">
        <div class="controls">
            <spring:message code="${confirm}" var="confirm"/>
            <input class="btn btn-primary" type="submit" value="${confirm}"/>
            <a class="btn" href="${contextPath}${backUrl}"><spring:message code="views.button.cancel"/></a>
        </div>
    </div>

</form:form>

</div>
