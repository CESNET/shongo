<%@ page import="cz.cesnet.shongo.client.web.models.TechnologyModel" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>



<%--<c:choose>
    <c:when test="${not empty resource.id}">
        <h1><spring:message code="views.resource.modify"/></h1>
    </c:when>
    <c:otherwise>
        <h1><spring:message code="views.resource.create"/></h1>
    </c:otherwise>
</c:choose>--%>

<script type="text/javascript">
    var module = angular.module('jsp:resourceAttributes', []);
    module.controller("ResourceFormController", ['$scope', '$log', function($scope, $log) {
        // Get value or default value if null
        $scope.value = function (value, defaultValue) {
            return ((value == null || value == '' || value == 0) ? defaultValue : value);
        };
        $scope.id = $scope.value('${resource.id}', null);
        $scope.type = $scope.value('${resource.type}', null);
        $scope.isDeviceResource = ${resource.type == "DEVICE_RESOURCE"};


        $scope.resourceTypeChange = function() {
            if ($scope.type == "DEVICE_RESOURCE") {
                $scope.isDeviceResource = true;
            } else {
                $scope.isDeviceResource = false;
            }
        };

    }])

/*    $(document).ready(function() {
        $('#technologies').multiselect();
    });*/
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
                <form:select cssClass="form-control" ng-model="type" path="type" ng-change="resourceTypeChange()" ng-disabled="id">
                    <form:options items="${ResourceType.values()}"></form:options>
                </form:select>
                <c:if test="${resource.id != null}">
                    <input type="hidden" name="type" value="${resource.type}"/>
                </c:if>

            </div>
        </div>

        <%--Show Id if its set--%>
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
                <spring:message code="views.resource.description"/>:
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
                <form:input cssStyle="display:inline; margin-right: 10px;width: 50%;" cssClass="form-control" path="maximumFuture"/><spring:message code="views.period.monthsN"/>
            </div>
        </div>

        <c:if test="${!(resource.id != null and resource.type == 'RESOURCE') }">
            <div class="form-group" ng-show="isDeviceResource" class="ng-hide">
                <form:label class="col-xs-3 control-label" path="technologies">
                    <spring:message code="views.resource.technology"/>:
                </form:label>
                <div class="col-xs-4">
                    <form:select cssClass="form-control" path="technologies" >
                        <c:forEach items="<%=TechnologyModel.values()%>" var="technology">
                            <form:option value="${technology.title}"></form:option>
                        </c:forEach>
                    </form:select>
                </div>
            </div>
        </c:if>



    </form:form>


    <hr/>
    <div>
        <a class="btn btn-default pull-right" href="javascript: document.getElementById('resource').submit();">
            <spring:message code="views.resource.save"/>
        </a>
    </div>
</div>
