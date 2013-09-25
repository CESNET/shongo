<%--
  -- List of user roles
  --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="isWritable" required="false" %>
<%@attribute name="data" required="false" type="java.util.Collection" %>
<%@attribute name="dataUrl" required="false" %>
<%@attribute name="dataUrlParameters" required="false" %>
<%@attribute name="createUrl" required="false" %>
<%@attribute name="deleteUrl" required="false" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="isWritable" value="${isWritable != null ? isWritable : true}"/>
<c:set var="tableHead">
    <thead>
    <tr>
        <th><spring:message code="views.aclRecord.user"/></th>
        <th><spring:message code="views.aclRecord.role"/></th>
        <th><spring:message code="views.aclRecord.email"/></th>
        <c:if test="${isWritable && deleteUrl != null}">
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
                <tr>
                    <td>${userRole.user.fullName} (${userRole.user.originalId})</td>
                    <td><spring:message code="views.aclRecord.role.${userRole.role}"/></td>
                    <td>${userRole.user.primaryEmail}</td>
                    <c:if test="${isWritable && deleteUrl != null}">
                        <td>
                            <c:if test="${not empty userRole.id && userRole.deletable}">
                                <spring:eval var="aclDeleteUrl"
                                             expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(contextPath + deleteUrl, userRole.id)"/>
                                <tag:listAction code="delete" url="${aclDeleteUrl}" tabindex="2"/>
                            </c:if>
                        </td>
                    </c:if>
                </tr>
            </c:forEach>
            <c:if test="data.isEmpty()">
                <tr>${tableEmptyRow}</tr>
            </c:if>
            </tbody>
        </table>
        <c:if test="${isWritable && createUrl != null}">
            <a class="btn btn-primary" href="${createUrl}">
                <spring:message code="views.button.add"/>
            </a>
        </c:if>
    </c:when>

    <%-- Dynamic list of user roles --%>
    <c:when test="${dataUrl != null}">
        <div ng-controller="PaginationController"
             ng-init="init('userRoles', '${dataUrl}', {${dataUrlParameters}})">
            <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
            <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}">
                <spring:message code="views.pagination.records"/>
            </pagination-page-size>

            <div class="spinner" ng-hide="ready"></div>
            <table class="table table-striped table-hover" ng-show="ready">
                    ${tableHead}
                <tbody>
                <tr ng-repeat="userRole in items">
                    <td>{{userRole.user.fullName}} ({{userRole.user.originalId}})</td>
                    <td>{{userRole.role}}</td>
                    <td>{{userRole.user.primaryEmail}}</td>
                    <c:if test="${isWritable}">
                        <td>
                            <spring:eval var="aclDeleteUrl"
                                         expression="T(cz.cesnet.shongo.client.web.ClientWebUrl).format(contextPath + deleteUrl, '{{userRole.id}}')"/>
                            <span ng-show="userRole.deletable">
                                <tag:listAction code="delete" url="${aclDeleteUrl}" tabindex="2"/>
                            </span>
                        </td>
                    </c:if>
                </tr>
                <tr ng-hide="items.length">${tableEmptyRow}</tr>
                </tbody>
            </table>
            <c:choose>
                <c:when test="${isWritable && createUrl != null}">
                    <a class="btn btn-primary" href="${createUrl}" tabindex="1">
                        <spring:message code="views.button.add"/>
                    </a>
                    <pagination-pages class="pull-right">
                        <spring:message code="views.pagination.pages"/>
                    </pagination-pages>
                </c:when>
                <c:otherwise>
                    <pagination-pages><spring:message code="views.pagination.pages"/></pagination-pages>
                </c:otherwise>
            </c:choose>
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


