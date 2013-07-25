<%@ tag trimDirectiveWhitespaces="true" %>
<%@ tag import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>

<%@attribute name="label" required="false"%>
<%@attribute name="tooltipId" required="false"%>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="submitUrl">${contextPath}<%= ClientWebUrl.REPORT_SUBMIT %></c:set>

<div class="report">
    <form class="form-horizontal" action="${submitUrl}" method="post">
        <fieldset>

            <div class="control-group">
                <label class="control-label" for="email">
                    <spring:message code="views.report.email"/>:
                </label>
                <div class="controls double-width">
                    <security:authorize access="isAuthenticated()">
                        <security:authentication var="email" property="principal.primaryEmail"/>
                        <c:set var="emailReadOnly" value="readonly"/>
                    </security:authorize>
                    <input id="email" type="text" name="email" value="${email}" ${emailReadOnly}/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="message">
                    <spring:message code="views.report.message"/>:
                </label>
                <div class="controls double-width">
                    <textarea id="message" name="message"></textarea>
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
    </form>
</div>