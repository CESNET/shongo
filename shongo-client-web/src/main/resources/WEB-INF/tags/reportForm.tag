<%@ tag import="net.tanesha.recaptcha.ReCaptcha" %>
<%@ tag import="java.util.Properties" %>
<%@ tag import="cz.cesnet.shongo.client.web.models.ReportModel" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="submitUrl" required="false"%>

<c:set var="tabIndex" value="1"/>

<script type="text/javascript">
    var module = angular.module('tag:reportForm', ['ngApplication', 'tag:expandableBlock']);
</script>

<p><spring:message code="views.report.help"/></p>
<div class="tagReportForm" ng-app="tag:reportForm">
    <form:form class="form-horizontal" commandName="report" action="${submitUrl}" method="post">
        <fieldset>

            <div class="control-group">
                <form:label class="control-label" path="email">
                    <spring:message code="views.report.email"/>:
                </form:label>
                <div class="controls double-width">
                    <form:input path="email" value="${email}" readonly="${report.emailReadOnly}" cssErrorClass="error" tabindex="${tabIndex}"/>
                    <form:errors path="email" cssClass="error"/>
                </div>
            </div>

            <div class="control-group">
                <form:label class="control-label" path="message">
                    <spring:message code="views.report.message"/>:
                </form:label>
                <div class="controls double-width">
                    <form:textarea path="message" cssErrorClass="error" tabindex="${tabIndex}"/>
                    <form:errors path="message" cssClass="error"/>
                </div>
            </div>

            <c:if test="${report.reCaptcha != null}">
                <div class="control-group">
                    <div class="controls">
                        <spring:hasBindErrors htmlEscape="true" name="report">
                            <c:if test="${errors.hasFieldErrors('reCaptcha')}">
                                <c:set var="reCaptchaClass" value="error"/>
                            </c:if>
                        </spring:hasBindErrors>
                        <div class="recaptcha ${reCaptchaClass}">
                            <%
                                ReportModel reportModel = (ReportModel) request.getAttribute("report");
                                ReCaptcha reCaptcha = reportModel.getReCaptcha();
                                Properties properties = new Properties();
                                properties.setProperty("theme", "clean");
                                out.print(reCaptcha.createRecaptchaHtml(null, properties));
                            %>
                        </div>
                        <form:errors path="reCaptcha" cssClass="error"/>
                    </div>
                </div>
            </c:if>

            <div class="control-group">
                <div class="controls">
                    <spring:message code="views.button.send" var="submitTitle"/>
                    <input class="btn btn-primary" type="submit" value="${submitTitle}" tabindex="${tabIndex}"/>
                    <a  class="btn" href="${backUrl}" tabindex="${tabIndex}"><spring:message code="views.button.cancel"/></a>
                </div>
            </div>

            <div class="control-group">
                <div class="controls">
                    <tag:expandableBlock name="report" cssClass="context" expandCode="views.report.showContext" collapseCode="views.report.hideContext">
                        <pre>${report.context.toString(pageContext.request)}</pre>
                    </tag:expandableBlock>
                </div>
            </div>

        </fieldset>
    </form:form>
</div>