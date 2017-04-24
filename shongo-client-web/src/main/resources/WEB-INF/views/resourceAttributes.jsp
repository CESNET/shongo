<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ page import="cz.cesnet.shongo.client.web.models.TechnologyModel" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--<%@attribute name="resource" required="true" type="cz.cesnet.shongo.client.web.models.ResourceModel" %>
<%@ tag import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ tag import="cz.cesnet.shongo.client.web.models.TechnologyModel" %>--%>
<tag:url var="resourceCapabilities" value="<%= ClientWebUrl.RESOURCE_CAPABILITIES %>"/>


<c:set var="administrationMode" value="${sessionScope.SHONGO_USER.administrationMode}"/>
<%--<c:choose>
    <c:when test="${not empty resource.id}">
        <h1><spring:message code="views.resource.modify"/></h1>
    </c:when>
    <c:otherwise>
        <h1><spring:message code="views.resource.create"/></h1>
    </c:otherwise>
</c:choose>--%>

<script type="text/javascript">


    var module = angular.module('jsp:resourceAttributes', ['ngTooltip']);
    module.controller("ResourceFormController", ['$scope', '$log', function ($scope, $log) {
        // Get value or default value if null
        $scope.value = function (value, defaultValue) {
            return ((value == null || value == '' || value == 0) ? defaultValue : value);
        };
        $scope.id = $scope.value('${resource.id}', null);
        $scope.type = $scope.value('${resource.type}', null);
        $scope.isDeviceResource = ${resource.type == "DEVICE_RESOURCE"};


        $scope.resourceTypeChange = function () {
            if ($scope.type == "DEVICE_RESOURCE") {
                $scope.isDeviceResource = true;
            } else {
                $scope.isDeviceResource = false;
            }
        };

        $("#technologies").select2({
            placeholder: "<spring:message code="views.resource.technologies.placeholder"/>",
        });

    }])

    function validateEmail(email) {
        var re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        return re.test(email);
    }

    $(document).ready(function () {

        $("#emailAddresses").select2({
            tags: [],
            tokenSeparators: [",", " "],
            selectOnBlur: true,
            formatNoMatches: function () {
                return '';
            },
            dropdownCssClass: 'select2-hidden',
            createSearchChoice: function (term, data) {
                if ($(data).filter(function () {
                        return this.text.localeCompare(term) === 0;
                    }).length === 0) {
                    if (validateEmail(term)) {
                        return {
                            id: term,
                            text: term
                        };
                    } else {
                        return null;
                    }
                }
            },
        });

    });

</script>

<div ng-app="jsp:resourceAttributes">
    <hr/>

    <form:form class="form-horizontal"
               commandName="resource"
               method="post"
               ng-controller="ResourceFormController">

        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="type">
                <spring:message code="views.resource.type"/>:
            </form:label>
            <div class="col-xs-4">
                <form:select cssClass="form-control" ng-model="type" path="type" ng-change="resourceTypeChange()"
                             ng-disabled="id">
                    <c:forEach items="${resourceTypes}" var="resourceType">
                        <form:option value="${resourceType}"><spring:message
                                code="${resourceType.getCode()}"/></form:option>
                    </c:forEach>
                </form:select>
                <c:if test="${resource.id != null}">
                    <input type="hidden" name="type" value="${resource.type}"/>
                </c:if>

            </div>
        </div>

        <%--Show Id if it is set--%>
        <c:if test="${not empty resource.id}">
            <div class="form-group">
                <form:label class="col-xs-3 control-label" path="id">
                    <spring:message code="views.resource.identifier"/>:
                </form:label>
                <div class="col-xs-4">
                    <form:input cssClass="form-control" path="id" readonly="true" tabindex="${tabIndex}"/>
                </div>
            </div>
        </c:if>

        <%--Name input--%>
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="name">
                <spring:message code="views.resource.name"/>:
            </form:label>
            <div class="col-xs-4">
                <form:input path="name" cssClass="form-control" cssErrorClass="form-control error"
                            tabindex="${tabIndex}"/>
            </div>
            <div class="col-xs-offset-3 col-xs-9">
                <form:errors path="name" cssClass="error"/>
            </div>
        </div>

        <%--Description input--%>
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="description">
                <spring:message code="views.resource.description" var="descriptionLabel"/>
                <tag:help label="${descriptionLabel}:"><spring:message code="views.resourceAttributes.descriptionHelp"/></tag:help>
            </form:label>
            <div class="col-xs-4">
                <form:input path="description" cssClass="form-control" cssErrorClass="form-control error"
                            tabindex="${tabIndex}"/>
            </div>
            <div class="col-xs-offset-3 col-xs-9">
                <form:errors path="description" cssClass="error"/>
            </div>
        </div>

        <%--Allocatable checkbox--%>
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="allocatable">
                <spring:message code="views.resource.allocatable"/>:
            </form:label>
            <div class="col-xs-4 checkbox">
                <form:checkbox path="allocatable"/>
            </div>
        </div>

        <%--Calendar public checkbox--%>
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="calendarPublic">
                <spring:message code="views.resource.calendarPublic"/>:
            </form:label>
            <div class="col-xs-4 checkbox">
                <form:checkbox path="calendarPublic"/>
            </div>
        </div>

        <%--Confirm by owner checkbox--%>
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="confirmByOwner">
                <spring:message code="views.resource.confirmByOwner"/>:
            </form:label>
            <div class="col-xs-4 checkbox">
                <form:checkbox path="confirmByOwner"/>
            </div>
        </div>

        <%--Maximum future period--%>
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="maximumFuture">
                <spring:message code="views.resource.maximumFuture"/>:
            </form:label>
            <div class="col-xs-3">
                <form:input cssStyle="display:inline; margin-right: 10px;width: 50%;" cssClass="form-control"
                            path="maximumFuture"/><spring:message code="views.period.monthsN"/>
            </div>
        </div>

        <%--Administrator emails--%>
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="administratorEmails">
                <spring:message code="views.resource.administratorEmails"/>:
            </form:label>
            <div class="col-xs-4">
                <form:input placeholder="example@domain.com" id="emailAddresses" path="administratorEmails" cssStyle="width: 100%;"/>
            </div>
        </div>

        <%--Technologies--%>
        <c:if test="${!(resource.id != null and resource.type == 'RESOURCE') }">
            <div class="form-group" ng-show="isDeviceResource" class="ng-hide">
                <form:label class="col-xs-3 control-label" path="technologies">
                    <spring:message code="views.resource.technology"/>:
                </form:label>
                <div class="col-xs-4">
                    <form:select cssClass="form-control" path="technologies">

                        <spring:eval var="technologies" expression="T(cz.cesnet.shongo.client.web.models.TechnologyModel).values()"/>
                        <c:forEach items="<%= TechnologyModel.values() %>" var="technology">
                            <form:option value="${technology.title}"></form:option>
                        </c:forEach>
                    </form:select>
                </div>
                <div class="col-xs-offset-3 col-xs-9">
                    <form:errors path="technologies" cssClass="error"/>
                </div>
            </div>
        </c:if>
    </form:form>

    <hr/>

    <div>
        <c:if test="${administrationMode}">
            <a class="btn btn-default pull-right" style="margin-left: 5px;" href="${resourceCapabilities}">
                Spravovat schopnosti
            </a>
        </c:if>
        <a class="btn btn-default pull-right" href="javascript: document.getElementById('resource').submit();">
            <spring:message code="views.resource.save"/>
        </a>
    </div>
</div>