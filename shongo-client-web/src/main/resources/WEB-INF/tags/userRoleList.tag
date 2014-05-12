<%--
  -- List of user roles
  --%>
<%@ tag import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="isWritable" required="false" %>
<%@attribute name="data" required="false" type="java.util.Collection" %>
<%@attribute name="dataUrl" required="false" %>
<%@attribute name="dataUrlParameters" required="false" %>
<%@attribute name="createUrl" required="false" %>
<%@attribute name="deleteUrl" required="false" %>

<c:set var="isWritable" value="${isWritable != null ? isWritable : true}"/>
<c:set var="tableHead">
    <thead>
    <tr>
        <th><spring:message code="views.userRoleList.for"/></th>
        <th>
            <spring:message code="views.userRole.objectRole"/>
            <tag:help>
                <strong><spring:message code="views.userRole.objectRole.OWNER"/></strong>

                <p><spring:message code="views.userRole.objectRoleHelp.OWNER"/></p>
                <c:if test="${reservationRequest.specificationType == 'PERMANENT_ROOM'}">
                    <strong><spring:message code="views.userRole.objectRole.RESERVATION_REQUEST_USER"/></strong>

                    <p><spring:message code="views.userRole.objectRoleHelp.RESERVATION_REQUEST_USER"/></p>
                </c:if>
                <strong><spring:message code="views.userRole.objectRole.READER"/></strong>

                <p><spring:message code="views.userRole.objectRoleHelp.READER"/></p>
            </tag:help>
        </th>
        <th><spring:message code="views.userRole.email"/></th>
        <c:if test="${isWritable && not empty deleteUrl}">
            <th style="min-width: 85px; width: 85px;">
                <spring:message code="views.list.action"/>
            </th>
        </c:if>
    </tr>
    </thead>
</c:set>
<c:set var="tableEmptyRow">
    <td colspan="4" class="empty"><spring:message code="views.list.none"/></td>
</c:set>
<spring:message code="views.userRoleList.group" var="groupTitle"/>
<spring:message code="views.userRoleList.user" var="userTitle"/>

<tag:url var="userListUrl" value="<%= ClientWebUrl.USER_LIST_DATA %>"/>

<script type="text/javascript">
   function UserRoleController($scope, $application){
       $scope.formatGroup = function(groupId, event) {
           $.ajax("${userListUrl}?groupId=" + groupId, {
               dataType: "json"
           }).done(function (data) {
                content = "<b><spring:message code="views.userRole.groupMembers"/>:</b><br/>";
                content += $application.formatUsers(data, "<spring:message code="views.userRole.groupMembers.none"/>");
                event.setResult(content);
            }).fail($application.handleAjaxFailure);
           return "<spring:message code="views.loading"/>";
       };
   }
</script>

<div class="tagUserRoleList">
<c:choose>
    <%-- Static list of user roles --%>
    <c:when test="${data != null}">
        <jsp:doBody/>
        <table class="table table-striped table-hover">
                ${tableHead}
            <tbody>
            <c:forEach items="${data}" var="userRole">
                <tag:url var="userRoleDeleteUrl" value="${deleteUrl}">
                    <tag:param name="roleId" value="${userRole.id}"/>
                </tag:url>
                <tr ng-controller="UserRoleController">
                    <td>
                        <c:choose>
                            <c:when test="${userRole.identityType == 'GROUP'}">
                                <b class="fa fa-group" title="${groupTitle}"></b>
                                <tag:help label="${userRole.identityName}" content="formatGroup('${userRole.identityPrincipalId}', event)"/>
                            </c:when>
                            <c:otherwise>
                                <b class="fa fa-user" title="${userTitle}"></b>
                                ${userRole.identityName}
                            </c:otherwise>
                        </c:choose>
                        <c:choose>
                            <c:when test="${userRole.identityType == 'USER' && not empty userRole.user.organization}">
                                (${userRole.user.organization})
                            </c:when>
                            <c:when test="${userRole.identityType == 'GROUP' && not empty userRole.group.description}">
                                (${userRole.group.description})
                            </c:when>
                        </c:choose>
                    </td>
                    <td><spring:message code="views.userRole.objectRole.${userRole.role}"/></td>
                    <td>${userRole.user.primaryEmail}</td>
                    <c:if test="${isWritable && not empty userRoleDeleteUrl}">
                        <td>
                            <c:if test="${not empty userRole.id && userRole.deletable}">
                                <tag:listAction code="delete" url="${userRoleDeleteUrl}" tabindex="2"/>
                            </c:if>
                        </td>
                    </c:if>
                </tr>
            </c:forEach>
            <c:if test="${empty data}">
                <tr>${tableEmptyRow}</tr>
            </c:if>
            </tbody>
        </table>
        <c:if test="${isWritable && createUrl != null}">
            <div class="table-actions">
                <a class="btn btn-primary" href="${createUrl}">
                    <spring:message code="views.button.add"/>
                </a>
            </div>
        </c:if>
    </c:when>

    <%-- Dynamic list of user roles --%>
    <c:when test="${dataUrl != null}">
        <tag:url var="userRoleDeleteUrl" value="${deleteUrl}">
            <tag:param name="roleId" value="{{userRole.id}}" escape="false"/>
        </tag:url>
        <div ng-controller="PaginationController"
             ng-init="init('userRoles', '${dataUrl}', {${dataUrlParameters}})">
            <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
            <spring:message code="views.button.refresh" var="paginationRefresh"/>
            <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
                <spring:message code="views.pagination.records"/>
            </pagination-page-size>
            <jsp:doBody/>
            <div class="spinner" ng-hide="ready || errorContent"></div>
            <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
            <table class="table table-striped table-hover" ng-show="ready">
                    ${tableHead}
                <tbody>
                <tr ng-repeat="userRole in items" class="user-role" ng-controller="UserRoleController">
                    <td>
                        <span ng-show="userRole.identityType == 'GROUP'">
                            <b class="fa fa-group" title="${groupTitle}"></b>
                            <tag:help label="{{userRole.identityName}}" content="formatGroup(userRole.identityPrincipalId, event)"/>
                        </span>
                        <span ng-hide="userRole.identityType == 'GROUP'">
                            <b class="fa fa-user" title="${userTitle}"></b>
                            {{userRole.identityName}}
                        </span>
                        <span ng-show="userRole.identityDescription">
                            ({{userRole.identityDescription}})
                        </span>
                    </td>
                    <td>{{userRole.role}}</td>
                    <td>{{userRole.email}}</td>
                    <c:if test="${isWritable && not empty userRoleDeleteUrl}">
                        <td>
                            <span ng-show="userRole.deletable">
                                <tag:listAction code="delete" url="${userRoleDeleteUrl}" tabindex="2"/>
                            </span>
                        </td>
                    </c:if>
                </tr>
                <tr ng-hide="items.length">${tableEmptyRow}</tr>
                </tbody>
            </table>
            <c:if test="${isWritable && createUrl != null}">
                <div class="table-actions">
                    <a class="btn btn-primary" href="${createUrl}" tabindex="1">
                        <spring:message code="views.button.add"/>
                    </a>
                </div>
            </c:if>
            <pagination-pages class="${(isWritable && createUrl != null) ? 'pull-right' : ''}" ng-show="ready">
                <spring:message code="views.pagination.pages"/>
            </pagination-pages>
        </div>

    </c:when>

    <%-- Error --%>
    <c:otherwise>
        Neither attribute
        <pre>data</pre>
        or
        <pre>dataUrl</pre>
        has been specified.
    </c:otherwise>
</c:choose>

</div>


