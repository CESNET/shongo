<%--
  -- Reservation request form.
  --%>
<%@ tag body-content="empty" %>
<%@ tag import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="objectType" required="true" type="cz.cesnet.shongo.controller.ObjectType" %>
<%@attribute name="confirmTitle" required="false" type="java.lang.String" %>
<%@attribute name="cancelUrl" required="false" type="java.lang.String" %>
<%@attribute name="cancelTitle" required="false" type="java.lang.String" %>

<c:set var="tabIndex" value="1"/>

<tag:url var="userListUrl" value="<%= ClientWebUrl.USER_LIST_DATA %>"/>
<tag:url var="groupListUrl" value="<%= ClientWebUrl.GROUP_LIST_DATA %>"/>
<c:if test="${empty cancelUrl}">
    <tag:url var="cancelUrl" value="${requestScope.backUrl}"/>
</c:if>

<script type="text/javascript">
    var module = angular.module('tag:userRoleForm', ['ngTooltip']);
    module.controller("UserRoleFormController", function($scope, $application, $timeout) {
        // Get value or default value if null
        $scope.value = function (value, defaultValue) {
            return ((value == null || value == '') ? defaultValue : value);
        };

        // Get dynamic user role attributes
        $scope.identityType = $scope.value('${userRole.identityType}', null);
        $scope.$watch('identityType', function(newValue, oldValue){
            if (newValue != oldValue) {
                $("#identityPrincipalId").val(null);
            }
            $scope.initIdentitySelect();
        });

        /**
         * Refresh description of current group.
         */
        $scope.groupDescription = null;
        $scope.refreshGroupDescription = function() {
            var identityPrincipalId = $(this).val();
            if ($scope.identityType == 'GROUP' && identityPrincipalId != null && identityPrincipalId != $scope.identityPrincipalId) {
                $scope.identityPrincipalId = identityPrincipalId;
                $scope.groupDescription = '';
                $scope.$apply();
                $.ajax("${userListUrl}?groupId=" + identityPrincipalId, {
                    dataType: "json"
                }).done(function (data) {
                    $timeout(function(){
                        $scope.groupDescription = $application.formatUsers(data, "<spring:message code="views.userRole.groupMembers.none"/>", 5);
                    }, 0);
                }).fail($application.handleAjaxFailure);
            }
            else {
                $scope.identityPrincipalId = null;
                $scope.groupDescription = null;
            }
        };

        /**
         * Initialize identity selection box.
         */
        $scope.initIdentitySelect = function() {
            var placeholder;
            var formatIdentity;
            var identityListUrl;
            var identityUrl;
            var identityField;
            var minimumInputLength;
            switch ($scope.identityType) {
                case "USER":
                    placeholder = "<spring:message code="views.select.user"/>";
                    formatIdentity = function(user) {
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
                    identityListUrl = "${userListUrl}";
                    identityUrl = "${userListUrl}?userId=";
                    identityField = "userId";
                    minimumInputLength = 2;
                    break;
                case "GROUP":
                    placeholder = "<spring:message code="views.select.group"/>";
                    formatIdentity = function(group) {
                        var result = "<b>" + group.name + "</b>";
                        if (group.description != null) {
                            result += " (" + group.description + ")";
                        }
                        return result;
                    };
                    identityListUrl = "${groupListUrl}";
                    identityUrl = "${groupListUrl}?groupId=";
                    identityField = "id";
                    minimumInputLength = 0;
                    break;
            }
            $("#identityPrincipalId").select2({
                placeholder: placeholder,
                width: 'resolve',
                minimumInputLength: minimumInputLength,
                ajax: {
                    url: identityListUrl,
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
                            results.push({id: dataItem[identityField], text: formatIdentity(dataItem)});
                        }
                        return {results: results};
                    },
                    transport: function (options) {
                        var control = this.data().select2;
                        if (control.opts.formatNoMatchesDefault == null) {
                            control.opts.formatNoMatchesDefault = control.opts.formatNoMatches;
                        }
                        control.opts.formatNoMatches = control.opts.formatNoMatchesDefault;
                        return $.ajax(options).fail(function(response){
                            if (response.status == 500) {
                                var error = JSON.parse(response.responseText);
                                if (error.error == "type-illegal-value") {
                                    control.opts.formatNoMatches = function(term){
                                        return "<span class='error'><spring:message code="validation.field.invalidValue"/></span>";
                                    };
                                    options.success([]);
                                    return;
                                }
                            }
                            $application.handleAjaxFailure(response);
                        });
                    }
                },
                escapeMarkup: function (markup) { return markup; },
                initSelection: function (element, callback) {
                    var id = $(element).val();
                    callback({id: 0, text: '<spring:message code="views.select.loading"/>'});
                    $.ajax(identityUrl + id, {
                        dataType: "json"
                    }).done(function (data) {
                        callback({id: id, text: formatIdentity(data[0])});
                    }).fail($application.handleAjaxFailure);
                }
            });
            $scope.groupDescription = '';
            $("#identityPrincipalId").off("change", $scope.refreshGroupDescription);
            $("#identityPrincipalId").on("change", $scope.refreshGroupDescription);
        };
    });
</script>

<form:form class="form-horizontal"
           commandName="userRole"
           method="post"
           ng-controller="UserRoleFormController">


    <form:hidden path="id"/>

    <c:if test="${not empty userRole.objectId}">
        <div class="form-group">
            <form:label class="col-xs-3 control-label" path="objectId">
                <spring:message code="views.userRole.objectType.${objectType}"/>:
            </form:label>
            <div class="col-xs-4">
                <form:input cssClass="form-control" path="objectId" readonly="true" tabindex="${tabIndex}"/>
            </div>
        </div>
    </c:if>

    <div class="form-group">
        <form:label class="col-xs-3 control-label" path="identityType">
            <spring:message code="views.userRole.identityType"/>:
        </form:label>
        <div class="col-xs-4">
            <label class="radio inline" for="identityTypeUser">
                <form:radiobutton id="identityTypeUser" path="identityType" value="USER" ng-model="identityType"/>
                <span><spring:message code="views.userRole.identityType.USER"/></span>
            </label>
            <label class="radio inline" for="identityTypeGroup">
                <form:radiobutton id="identityTypeGroup" path="identityType" value="GROUP" ng-model="identityType"/>
                <span><spring:message code="views.userRole.identityType.GROUP"/></span>
            </label>
        </div>
    </div>

    <div class="form-group">
        <form:label class="col-xs-3 control-label" path="identityPrincipalId">
            <span ng-show="identityType == 'USER'"><spring:message code="views.userRole.user"/>:</span>
            <span ng-show="identityType == 'GROUP'"><spring:message code="views.userRole.group"/>:</span>
        </form:label>
        <div class="col-xs-4">
            <form:input cssClass="form-control" cssErrorClass="form-control error" path="identityPrincipalId" tabindex="${tabIndex}"/>
            <form:errors path="identityPrincipalId" cssClass="error"/>
            <div ng-show="groupDescription" style="margin-top: 5px;">
                <i><b><spring:message code="views.userRole.groupMembers"/>:</b> {{groupDescription}}</i>
            </div>
        </div>
    </div>

    <div class="form-group">
        <spring:eval var="roles" expression="objectType.getOrderedRoles()"/>
        <form:label class="col-xs-3 control-label" path="role">
            <spring:message code="views.userRole.objectRole" var="roleLabel"/>
            <tag:help label="${roleLabel}:">
                <c:forEach items="${roles}" var="role">
                    <strong><spring:message code="views.userRole.objectRole.${role}"/></strong>
                    <p><spring:message code="views.userRole.objectRoleHelp.${role}"/></p>
                </c:forEach>
            </tag:help>
        </form:label>
        <div class="col-xs-4">
            <form:select cssClass="form-control" cssErrorClass="form-control error" path="role" tabindex="${tabIndex}">
                <c:forEach items="${roles}" var="role">
                    <form:option value="${role}"><spring:message code="views.userRole.objectRole.${role}"/></form:option>
                </c:forEach>
            </form:select>
            <form:errors path="role" cssClass="error"/>
        </div>
    </div>

    <div class="form-group">
        <div class="col-xs-offset-3 col-xs-4">
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
