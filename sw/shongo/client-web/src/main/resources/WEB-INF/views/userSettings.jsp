<%--
  -- Content for user settings dialog.
  --%>
<%@ page language="java" pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" trimDirectiveWhitespaces="true" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="tabIndex" value="1"/>
<c:set var="confirmUrl">${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.USER_SETTINGS_SAVE %></c:set>
<c:set var="cancelUrl">${contextPath}<%= cz.cesnet.shongo.client.web.ClientWebUrl.HOME %></c:set>

<script type="text/javascript">
    angular.module('jsp:userSettings', ['ngTooltip']);
</script>

<form:form class="form-horizontal"
           commandName="userSettings"
           action="${confirmUrl}"
           method="post"
           ng-app="jsp:userSettings">

    <fieldset>

        <div class="control-group">
            <label class="control-label" for="user">
                <spring:message code="views.userSettings.user"/>:
            </label>
            <div class="controls double-width">
                <security:authentication property="principal" var="principal"/>
                <input id="user" type="text" readonly="true" value="${principal.fullName} (${principal.originalId})"/>
            </div>
        </div>

        <div class="control-group">
            <form:label class="control-label" path="locale">
                <spring:message code="views.userSettings.locale"/>:
            </form:label>
            <div class="controls">
                <form:select path="locale" tabindex="${tabIndex}">
                    <form:option value=""><spring:message code="views.userSettings.default"/></form:option>
                    <form:option value="en">English</form:option>
                    <form:option value="cs">Čeština</form:option>
                </form:select>
            </div>
        </div>

        <div class="control-group">
            <form:label class="control-label" path="dateTimeZone">
                <spring:message code="views.userSettings.dateTimeZone"/>:
            </form:label>
            <div class="controls">
                <form:select path="dateTimeZone" cssStyle="width: 500px;" tabindex="${tabIndex}">
                    <form:option value=""><spring:message code="views.userSettings.default"/></form:option>
                    <c:forEach items="${timeZones}" var="timeZone">
                        <form:option value="${timeZone.key}">${timeZone.value}</form:option>
                    </c:forEach>
                </form:select>
            </div>
        </div>

        <c:if test="${userSettings.adminMode != null}">
            <div class="control-group">
                <form:label class="control-label" path="adminMode">
                    <spring:message code="views.userSettings.adminMode"/>:
                </form:label>
                <div class="controls">
                    <form:checkbox path="adminMode" tabindex="${tabIndex}"/>&nbsp
                    <tag:help><spring:message code="views.userSettings.adminMode.help"/></tag:help>
                </div>
            </div>
        </c:if>

    </fieldset>

    <div class="control-group">
        <div class="controls">
            <spring:message code="views.button.save" var="saveTitle"/>
            <input class="btn btn-primary" type="submit" value="${saveTitle}" tabindex="${tabIndex}"/>
            <a class="btn" href="${cancelUrl}" tabindex="${tabIndex}"><spring:message code="views.button.cancel"/></a>
        </div>
    </div>

</form:form>
