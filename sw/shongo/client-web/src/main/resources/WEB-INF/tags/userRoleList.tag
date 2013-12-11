<%--
  -- List of user roles
  --%>
<%@ tag body-content="empty" %>
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
        <th><spring:message code="views.userRole.user"/></th>
        <th><spring:message code="views.userRole.role"/></th>
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

<c:choose>
    <%-- Static list of user roles --%>
    <c:when test="${data != null}">
        <table class="table table-striped table-hover">
                ${tableHead}
            <tbody>
            <c:forEach items="${data}" var="userRole">
                <tag:url var="userRoleDeleteUrl" value="${deleteUrl}">
                    <tag:param name="roleId" value="${userRole.id}"/>
                </tag:url>
                <tr>
                    <td>${userRole.user.fullName} (${userRole.user.organization})</td>
                    <td><spring:message code="views.userRole.role.${userRole.role}"/></td>
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
            <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}">
                <spring:message code="views.pagination.records"/>
            </pagination-page-size>

            <div class="spinner" ng-hide="ready || errorContent"></div>
            <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
            <table class="table table-striped table-hover" ng-show="ready">
                    ${tableHead}
                <tbody>
                <tr ng-repeat="userRole in items">
                    <td>{{userRole.user.fullName}} ({{userRole.user.organization}})</td>
                    <td>{{userRole.role}}</td>
                    <td>{{userRole.user.primaryEmail}}</td>
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
            <pagination-pages class="${(isWritable && createUrl != null) ? 'pull-right' : ''}" ng-show="ready">
                <spring:message code="views.pagination.pages"/>
            </pagination-pages>
        </div>
        <c:if test="${isWritable && createUrl != null}">
            <div class="table-actions">
                <a class="btn btn-primary" href="${createUrl}" tabindex="1">
                    <spring:message code="views.button.add"/>
                </a>
            </div>
        </c:if>
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


