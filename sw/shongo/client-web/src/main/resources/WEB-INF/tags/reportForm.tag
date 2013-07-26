<%@ tag trimDirectiveWhitespaces="true" %>
<%@ tag import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<%@attribute name="label" required="false"%>
<%@attribute name="tooltipId" required="false"%>
<%@attribute name="submitUrl" required="false"%>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:if test="${empty submitUrl}">
    <c:set var="submitUrl">${contextPath}<%= ClientWebUrl.REPORT_SUBMIT %></c:set>
</c:if>

<div class="report">
    <form:form class="form-horizontal" commandName="report" action="${submitUrl}" method="post">
        <fieldset>

            <div class="control-group">
                <form:label class="control-label" path="email">
                    <spring:message code="views.report.email"/>:
                </form:label>
                <div class="controls double-width">
                    <form:input path="email" value="${email}" readonly="${emailReadOnly}" cssErrorClass="error"/>
                    <form:errors path="email" cssClass="error"/>
                </div>
            </div>

            <div class="control-group">
                <form:label class="control-label" path="message">
                    <spring:message code="views.report.message"/>:
                </form:label>
                <div class="controls double-width">
                    <form:textarea path="message" cssErrorClass="error"/>
                    <form:errors path="message" cssClass="error"/>
                </div>
            </div>

            <div class="control-group">
                <div class="controls">
                    <spring:message code="views.button.send" var="submitTitle"/>
                    <input class="btn btn-primary" type="submit" value="${submitTitle}"/>
                    <a  class="btn" href="${contextPath}/"><spring:message code="views.button.cancel"/></a>
                </div>
            </div>

        </fieldset>
    </form:form>
</div>