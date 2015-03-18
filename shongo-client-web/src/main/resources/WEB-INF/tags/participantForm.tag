<%@ tag import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%--
  -- Reservation request form.
  --%>
<%@ tag body-content="empty" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="confirmTitle" required="false" type="java.lang.String" %>
<%@attribute name="cancelUrl" required="false" type="java.lang.String" %>
<%@attribute name="cancelTitle" required="false" type="java.lang.String" %>
<%@attribute name="hideRole" required="false" type="java.lang.Boolean" %>

<%
    String formUrl = request.getParameter("form-url");
    jspContext.setAttribute("formUrl", formUrl);
%>

<c:if test="${empty formUrl}">
    <c:set var="formUrl" value="${requestUrl}"/>
</c:if>

<c:set var="tabIndex" value="1"/>

<tag:url var="userListUrl" value="<%= ClientWebUrl.USER_LIST_DATA %>"/>
<c:if test="${empty cancelUrl}">
    <tag:url var="cancelUrl" value="${requestScope.backUrl}"/>
</c:if>

<script type="text/javascript">
    var module = angular.module('tag:participantForm', ['ngTooltip']);

    module.controller("ParticipantFormController", function($scope, $application) {
        // Get value or default value if null
        $scope.value = function (value, defaultValue) {
            return ((value == null || value == '') ? defaultValue : value);
        };

        // Get dynamic participant attributes
        $scope.type = $scope.value('${participant.type}', null);

        $scope.init = function() {
            var formatUser = function(user) {
                var text = "<b>" + user.firstName;
                if ( user.lastName != null ) {
                    text += " " + user.lastName;
                }
                text += "</b>";
                if ( user.organization != null ) {
                    text += " (" + user.organization + ")";
                }
                return text;
            };
            $("#userId").select2({
                placeholder: "<spring:message code="views.select.user"/>",
                width: 'resolve',
                minimumInputLength: 2,
                ajax: {
                    url: "${userListUrl}",
                    dataType: 'json',
                    quietMillis: 1000,
                    data: function (term, page) {
                        return {
                            filter: term
                        };
                    },
                    results: function (data, page) {
                        var results = [];
                        for (var index = 0; index < data.length; index++) {
                            var dataItem = data[index];
                            results.push({id: dataItem.userId, text: formatUser(dataItem)});
                        }
                        return {results: results};
                    },
                    transport: function (options) {
                        return $.ajax(options).fail($application.handleAjaxFailure);
                    }
                },
                escapeMarkup: function (markup) { return markup; },
                initSelection: function (element, callback) {
                    var id = $(element).val();
                    callback({id: 0, text: '<spring:message code="views.select.loading"/>'});
                    $.ajax("${userListUrl}?userId=" + id, {
                        dataType: "json"
                    }).done(function (data) {
                        callback({id: id, text: formatUser(data[0])});
                    }).fail($application.handleAjaxFailure);
                }
            });
        };
    });
</script>

<form:form class="form-horizontal"
           commandName="participant"
           method="post"
           ng-controller="ParticipantFormController"
           ng-init="init()">

    <form:hidden path="id"/>

    <div class="form-group">
        <form:label class="col-xs-2 control-label" path="type">
            <spring:message code="views.participant.type"/>:
        </form:label>
        <div class="col-xs-5">
            <label class="radio inline" for="typeUser">
                <form:radiobutton id="typeUser" path="type" value="USER" ng-model="type"/>
                <span><spring:message code="views.participant.type.USER"/></span>
            </label>
            <c:choose>
                <c:when test="${reservationRequest.technology == 'ADOBE_CONNECT' && reservationRequest.roomAccessMode == 'PRIVATE'}">
                    <div class="radio inline text-muted">
                        <form:radiobutton id="typeAnonymous" path="type" disabled="true"/>
                        <span><spring:message code="views.participant.type.ANONYMOUS" /></span>
                        <tag:help><spring:message code="views.participant.roleHelp.PARTICIPANT.disabled"/></tag:help>
                    </div>
                </c:when>
                <c:otherwise>
                    <label class="radio inline" for="typeAnonymous">
                        <form:radiobutton id="typeAnonymous" path="type" value="ANONYMOUS" ng-model="type"/>
                        <span><spring:message code="views.participant.type.ANONYMOUS"/></span>
                    </label>
                </c:otherwise>
            </c:choose>
            <br />
            <div class="alert alert-warning" ng-show="type == 'ANONYMOUS' && ${reservationRequest.technology == 'ADOBE_CONNECT'}"><spring:message code="views.participant.roleHelp.PARTICIPANT.enabled"/></div>
            <form:errors path="type" cssClass="error"/>
        </div>
    </div>

    <div class="form-group" ng-show="type == 'USER'">
        <form:label class="col-xs-2 control-label" path="userId">
            <spring:message code="views.participant.userId"/>:
        </form:label>
        <div class="col-xs-4">
            <form:input cssClass="form-control" cssErrorClass="form-control error" path="userId" tabindex="${tabIndex}"/>
            <form:errors path="userId" cssClass="error"/>
        </div>
    </div>

    <div class="form-group" ng-show="type == 'ANONYMOUS'">
        <form:label class="col-xs-2 control-label" path="name">
            <spring:message code="views.participant.name"/>:
        </form:label>
        <div class="col-xs-4">
            <form:input cssClass="form-control" cssErrorClass="form-control error" path="name" tabindex="${tabIndex}"/>
            <form:errors path="name" cssClass="error"/>
        </div>
    </div>

    <div class="form-group" ng-show="type == 'ANONYMOUS'">
        <form:label class="col-xs-2 control-label" path="email">
            <spring:message code="views.participant.email"/>:
        </form:label>
        <div class="col-xs-4">
            <form:input cssClass="form-control" cssErrorClass="form-control error" path="email" tabindex="${tabIndex}"/>
            <form:errors path="email" cssClass="error"/>
        </div>
    </div>

    <c:choose>
        <c:when test="${hideRole}">
            <form:hidden path="role" value="PARTICIPANT"/>
        </c:when>
        <c:otherwise>
            <div class="form-group">
                <form:label class="col-xs-2 control-label" path="role">
                    <spring:message code="views.participant.role" var="roleLabel"/>
                    <tag:help label="${roleLabel}:">
                        <spring:eval var="roles" expression="T(cz.cesnet.shongo.ParticipantRole).values()"/>
                        <c:forEach items="${roles}" var="role">
                            <strong><spring:message code="views.participant.role.${role}"/></strong>
                            <p><spring:message code="views.participant.roleHelp.${role}"/></p>
                        </c:forEach>
                    </tag:help>
                </form:label>
                <div class="col-xs-4" ng-hide="type == 'ANONYMOUS'">
                    <form:select cssClass="form-control" path="role" tabindex="${tabIndex}">
                        <c:forEach items="${roles}" var="role">
                            <form:option value="${role}"><spring:message
                                    code="views.participant.role.${role}"/></form:option>
                        </c:forEach>
                    </form:select>
                    <form:errors path="role" cssClass="error"/>
                </div>
                <div class="col-xs-4" ng-show="type == 'ANONYMOUS'">
                    <form:select cssClass="form-control" path="role" readonly="true" tabindex="${tabIndex}">
                        <spring:eval var="participant" expression="T(cz.cesnet.shongo.ParticipantRole).PARTICIPANT"/>
                        <form:option value="${participant}"><spring:message
                                code="views.participant.role.GUEST"/></form:option>
                    </form:select>
                    <form:errors path="role" cssClass="error"/>
                </div>
            </div>
        </c:otherwise>
    </c:choose>

    <div class="form-group">
        <div class="col-xs-offset-2 col-xs-4">
            <c:if test="${not empty confirmTitle}">
                <spring:message code="${confirmTitle}" var="confirmTitle"/>
                <input class="btn btn-primary" type="submit" value="${confirmTitle}" tabindex="${tabIndex}"/>
            </c:if>
            <c:if test="${empty cancelTitle}">
                <c:set var="cancelTitle" value="views.button.cancel"/>
            </c:if>
            <a class="btn btn-default" href="${cancelUrl}" tabindex="${tabIndex}"><spring:message code="${cancelTitle}"/></a>
        </div>
    </div>

</form:form>
