<%--
  -- Content for user settings dialog.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="tabIndex" value="1"/>
<tag:url var="cancelUrl" value="${requestScope.backUrl}"/>
<tag:url var="webServiceDataUrl" value="<%= ClientWebUrl.USER_SETTINGS_WEB_SERVICE_DATA %>"/>

<%--
Alternative: https://einfra.cesnet.cz/perun-gui/#usr/info?active=1
--%>
<c:url var="webServiceUrl" value="https://einfra.cesnet.cz/a/user-profile/fed/"/>

<script type="text/javascript">
    var module = angular.module('jsp:userSettings', ['ngApplication', 'ngTooltip']);
    module.controller("UserSettingsController", function($scope, $application, $timeout){
        $scope.useWebService = ${userSettings.useWebService};
        $scope.locale = "${userSettings.locale}";
        $scope.homeTimeZone = "${userSettings.homeTimeZone}";
        $scope.currentTimeZone = "${currentTimeZone}";
        $scope.currentTimeZoneEnabled = ${userSettings.currentTimeZoneEnabled};
        $scope.$watch("useWebService", function(){
            if ($scope.useWebService) {
                $.ajax("${webServiceDataUrl}", {
                    dataType: "json"
                }).done(function (data) {
                    $("#homeTimeZone").select2("val", data.homeTimeZone);
                    $timeout(function(){
                        $scope.locale = data.locale;
                        $scope.homeTimeZone = data.homeTimeZone;
                    }, 0);
                }).fail($application.handleAjaxFailure);
            }
        });
    });

    $(function(){
        $("#homeTimeZone,#currentTimeZone").select2();
    });
</script>

<c:set var="currentTimeZone" value="${userSettings.currentTimeZone}"/>
<c:if test="${empty currentTimeZone}">
    <c:set var="currentTimeZone" value="UTC"/>
</c:if>

<form:form class="form-horizontal" role="form" method="post" commandName="userSettings"
           ng-app="jsp:userSettings" ng-controller="UserSettingsController">

    <div class="form-group">
        <label class="col-xs-3 control-label" for="user">
            <spring:message code="views.userSettings.user"/>:
        </label>
        <div class="col-xs-4">
            <security:authentication property="principal" var="principal"/>
            <c:set var="user">
                ${principal.fullName}&nbsp;<c:if test="${not empty principal.organization}">(${principal.organization})</c:if>
            </c:set>
            <input class="form-control" id="user" type="text" readonly="true" value="${user}"/>
        </div>
    </div>

    <div class="form-group">
        <form:label class="col-xs-3 control-label" path="useWebService">
            <spring:message code="views.userSettings.useWebService" var="useWebServiceLabel"/>
            <tag:help label="${useWebServiceLabel}:" selectable="true">
                <spring:message code="views.userSettings.useWebService.help" arguments="${webServiceUrl}"/>
            </tag:help>
        </form:label>
        <div class="col-xs-6">
            <span class="checkbox-inline">
                <form:checkbox path="useWebService" tabindex="${tabIndex}" ng-model="useWebService"/>
            </span>
            <a class="btn btn-default" href="${webServiceUrl}" target="_blank"><spring:message code="views.userSettings.useWebService.edit"/></a>
        </div>
    </div>

    <div class="form-group">
        <form:label class="col-xs-3 control-label" path="locale">
            <spring:message code="views.userSettings.locale" var="localeLabel"/>
            <tag:help label="${localeLabel}:">
                <spring:message code="views.userSettings.locale.help"/>
            </tag:help>
        </form:label>
        <div class="col-xs-4">
            <form:select cssClass="form-control" path="locale" tabindex="${tabIndex}" ng-model="locale" ng-disabled="useWebService">
                <form:option value=""><spring:message code="views.userSettings.default"/></form:option>
                <form:option value="en">English</form:option>
                <form:option value="cs">Čeština</form:option>
            </form:select>
        </div>
        <div class="col-xs-offset-3 col-xs-12" ng-show="locale == ''">
            <form:checkbox path="localeDefaultWarning" tabindex="${tabIndex}"/>&nbsp;<spring:message code="views.userSettings.localeDefaultWarning"/>
        </div>
    </div>

    <div class="form-group">
        <form:label class="col-xs-3 control-label" path="homeTimeZone">
            <spring:message code="views.userSettings.homeTimeZone" var="homeTimeZoneLabel"/>
            <tag:help label="${homeTimeZoneLabel}:">
                <spring:message code="views.userSettings.homeTimeZone.help"/>
            </tag:help>
        </form:label>
        <div class="col-xs-4">
            <form:select cssClass="form-control" path="homeTimeZone" tabindex="${tabIndex}" ng-model="homeTimeZone" ng-disabled="useWebService">
                <form:option value=""><spring:message code="views.userSettings.default"/></form:option>
                <spring:eval expression="T(cz.cesnet.shongo.client.web.models.TimeZoneModel).getTimeZones(sessionScope.SHONGO_USER.locale)" var="timeZones"/>
                <c:forEach items="${timeZones}" var="timeZone">
                    <form:option value="${timeZone.key}">${timeZone.value}</form:option>
                </c:forEach>
            </form:select>
        </div>
        <div class="col-xs-offset-3 col-xs-12" ng-show="homeTimeZone == '' && (!currentTimeZoneEnabled || currentTimeZone == '')">
            <form:checkbox path="timeZoneDefaultWarning" tabindex="${tabIndex}"/>&nbsp;<spring:message code="views.userSettings.timeZoneDefaultWarning"/>
        </div>
    </div>

    <div class="form-group">
        <form:label class="col-xs-3 control-label" path="currentTimeZoneEnabled">
            <spring:message code="views.userSettings.currentTimeZone" var="currentTimeZoneLabel"/>
            <tag:help label="${currentTimeZoneLabel}:">
                <spring:message code="views.userSettings.currentTimeZone.help"/>
            </tag:help>
        </form:label>
        <div class="col-xs-4">
            <div class="input-group">
                <span class="input-group-addon checkbox">
                    <form:checkbox id="currentTimeZoneEnabled" path="currentTimeZoneEnabled" tabindex="${tabIndex}" ng-model="currentTimeZoneEnabled"/>
                </span>
                <form:select cssClass="form-control" path="currentTimeZone" tabindex="${tabIndex}"
                             ng-disabled="!currentTimeZoneEnabled" ng-model="currentTimeZone">
                    <c:forEach items="${timeZones}" var="timeZone">
                        <form:option value="${timeZone.key}">${timeZone.value}</form:option>
                    </c:forEach>
                </form:select>
            </div>
        </div>
    </div>

    <div class="form-group">
        <form:label class="col-xs-3 control-label" path="advancedUserInterface" for="advancedUserInterface">
            <spring:message code="views.userSettings.advancedUserInterface" var="advancedUserInterfaceLabel"/>
            <tag:help label="${advancedUserInterfaceLabel}:">
                <spring:message code="views.userSettings.advancedUserInterface.help"/>
            </tag:help>
        </form:label>
        <div class="col-xs-4">
            <div class="checkbox">
                <form:checkbox id="advancedUserInterface" path="advancedUserInterface" tabindex="${tabIndex}"/>&nbsp;
            </div>
        </div>
    </div>

    <security:authorize access="hasPermission(ADMINISTRATION)">
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="administrationMode" for="administrationMode">
                <spring:message code="views.userSettings.administrationMode" var="adminModeLabel"/>
                <tag:help label="${adminModeLabel}:">
                    <spring:message code="views.userSettings.administrationMode.help"/>
                </tag:help>
            </form:label>
            <div class="col-xs-4">
                <div class="checkbox">
                    <form:checkbox id="administrationMode" path="administrationMode" tabindex="${tabIndex}"/>&nbsp;
                </div>
            </div>
        </div>
    </security:authorize>

    <div class="form-group">
        <div class="col-xs-offset-3 col-xs-4">
            <spring:message code="views.button.save" var="saveTitle"/>
            <input class="btn btn-primary" type="submit" value="${saveTitle}" tabindex="${tabIndex}"/>
            <a class="btn btn-default" href="${cancelUrl}" tabindex="${tabIndex}"><spring:message code="views.button.cancel"/></a>
        </div>
    </div>

</form:form>