<%@ tag import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%--
  -- Reservation request form.
  --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="confirmTitle" required="false" type="java.lang.String" %>
<%@attribute name="cancelTitle" required="false" type="java.lang.String" %>

<%
    String formUrl = request.getParameter("form-url");
    jspContext.setAttribute("formUrl", formUrl);
%>

<c:if test="${empty formUrl}">
    <c:set var="formUrl" value="${requestUrl}"/>
</c:if>

<c:set var="tabIndex" value="1"/>

<tag:url var="userListUrl" value="<%= ClientWebUrl.USER_LIST_DATA %>"/>
<tag:url var="userUrl" value="<%= ClientWebUrl.USER_DATA %>">
    <tag:param name="userId" value=":userId"/>
</tag:url>
<tag:url var="cancelUrl" value="${requestScope.backUrl}"/>

<script type="text/javascript">
    angular.module('tag:participantForm', ['ngTooltip']);

    function ParticipantFormController($scope) {
        // Get value or default value if null
        $scope.value = function (value, defaultValue) {
            return ((value == null || value == '') ? defaultValue : value);
        };

        // Get dynamic participant attributes
        $scope.type = $scope.value('${participant.type}', null);
    }

    window.formatUser = function(user) {
        var text = user.firstName;
        if ( user.lastName != null ) {
            text += " " + user.lastName;
        }
        if ( user.originalId != null ) {
            text += " (" + user.originalId + ")";
        }
        return text;
    };
    $(function () {
        $("#userId").select2({
            placeholder: "<spring:message code="views.select.user"/>",
            width: 'resolve',
            minimumInputLength: 2,
            ajax: {
                url: "${userListUrl}",
                dataType: 'json',
                data: function (term, page) {
                    return {
                        filter: term
                    };
                },
                results: function (data, page) {
                    var results = [];
                    for (var index = 0; index < data.length; index++) {
                        var dataItem = data[index];
                        results.push({id: dataItem.userId, text: window.formatUser(dataItem)});
                    }
                    return {results: results};
                }
            },
            initSelection: function (element, callback) {
                var id = $(element).val();
                callback({id: 0, text: '<spring:message code="views.select.loading"/>'});
                $.ajax("${userUrl}".replace(':userId', id), {
                    dataType: "json"
                }).done(function (data) {
                            callback({id: id, text: window.formatUser(data)});
                        });
            }
        });
    });
</script>

<form:form class="form-horizontal"
           commandName="participant"
           method="post"
           ng-controller="ParticipantFormController">

    <fieldset>

        <form:hidden path="id"/>

        <div class="control-group">
            <form:label class="control-label" path="type">
                <spring:message code="views.participant.type"/>:
            </form:label>
            <div class="controls">
                <form:radiobutton path="type" value="USER" ng-model="type"/>&nbsp;
                <span><spring:message code="views.participant.type.USER"/></span>
                &nbsp;&nbsp;
                <form:radiobutton path="type" value="ANONYMOUS" ng-model="type"/>&nbsp;
                <spring:message code="views.participant.type.ANONYMOUS"/>
                <form:errors path="type" cssClass="error"/>
            </div>
        </div>

        <div class="control-group" ng-show="type == 'USER'">
            <form:label class="control-label" path="userId">
                <spring:message code="views.participant.userId"/>:
            </form:label>
            <div class="controls double-width">
                <form:input path="userId" cssErrorClass="error" tabindex="${tabIndex}"/>
                <form:errors path="userId" cssClass="error"/>
            </div>
        </div>

        <div class="control-group" ng-show="type == 'ANONYMOUS'">
            <form:label class="control-label" path="name">
                <spring:message code="views.participant.name"/>:
            </form:label>
            <div class="controls">
                <form:input path="name" cssErrorClass="error" tabindex="${tabIndex}"/>
                <form:errors path="name" cssClass="error"/>
            </div>
        </div>

        <div class="control-group" ng-show="type == 'ANONYMOUS'">
            <form:label class="control-label" path="email">
                <spring:message code="views.participant.email"/>:
            </form:label>
            <div class="controls double-width">
                <form:input path="email" cssErrorClass="error" tabindex="${tabIndex}"/>
                <form:errors path="email" cssClass="error"/>
            </div>
        </div>

        <div class="control-group">
            <form:label class="control-label" path="role">
                <spring:message code="views.participant.role"/>:
            </form:label>
            <div class="controls">
                <spring:eval var="roles" expression="T(cz.cesnet.shongo.ParticipantRole).values()"/>
                <form:select path="role" tabindex="${tabIndex}">
                    <c:forEach items="${roles}" var="role">
                        <form:option value="${role}"><spring:message code="views.participant.role.${role}"/></form:option>
                    </c:forEach>
                </form:select>
                <form:errors path="role" cssClass="error"/>
                <tag:help>
                    <c:forEach items="${roles}" var="role">
                        <strong><spring:message code="views.participant.role.${role}"/></strong>
                        <p><spring:message code="views.participant.roleHelp.${role}"/></p>
                    </c:forEach>
                </tag:help>
            </div>
        </div>

    </fieldset>

    <div class="control-group">
        <div class="controls">
            <c:if test="${not empty confirmTitle}">
                <spring:message code="${confirmTitle}" var="confirmTitle"/>
                <input class="btn btn-primary" type="submit" value="${confirmTitle}" tabindex="${tabIndex}"/>
            </c:if>
            <c:if test="${empty cancelTitle}">
                <c:set var="cancelTitle" value="views.button.cancel"/>
            </c:if>
            <a class="btn" href="${cancelUrl}" tabindex="${tabIndex}"><spring:message code="${cancelTitle}"/></a>
        </div>
    </div>

</form:form>
