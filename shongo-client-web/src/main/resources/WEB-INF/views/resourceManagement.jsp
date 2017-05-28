<%--
  --    Page displaying resources with possibility to create new or edit existing resources.
  --%>

<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<tag:url var="resourceCreateUrl" value="<%= ClientWebUrl.RESOURCE_NEW %>"/>
<c:set var="administrationMode" value="${sessionScope.SHONGO_USER.administrationMode}"/>

<c:choose>
    <c:when test="${not empty errorMessage}">
        <div class="alert alert-danger" style="margin: 20px;">
            <spring:message code="${errorMessage}"/>
            <br/>
        </div>
    </c:when>
</c:choose>

    <table class="table table-striped table-hover" ng-show="ready">
        <thead>
        <tr>
            <th width="200px">
                <spring:message code="views.resource.name"/>
            </th>
            <th width="200px">
                <spring:message code="views.resource.id"/>
            </th>
            <th width="200px">
                <spring:message code="views.resource.technology"/>
            </th>
            <th width="200px">
                <spring:message code="views.resource.description"/>
            </th>
            <th style="min-width: 95px; width: 105px;">
                <spring:message code="views.list.action"/>
            </th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${readableResources}" var="resource">
            <tag:url var="resourceShowDetail" value="<%= ClientWebUrl.RESOURCE_DETAIL %>">
                <tag:param name="resourceId" value="${resource.id}" escape="false"/>
            </tag:url>
            <tag:url var="resourceModifyUrl" value="<%= ClientWebUrl.RESOURCE_MODIFY %>">
                <tag:param name="resourceId" value="${resource.id}" escape="false"/>
            </tag:url>
            <tag:url var="resourceSingleDeleteUrl" value="<%= ClientWebUrl.RESOURCE_SINGLE_DELETE %>">
                <tag:param name="resourceId" value="${resource.id}" escape="false"/>
            </tag:url>
            <tr>
                <td>${resource.get("name")}</td>
                <td>${resource.get("id")}</td>
                <td>${resource.get("technology").getTitle()}</td>
                <td>${resource.get("description")}</td>
                <td>
                    <tag:listAction code="show" titleCode="views.resourceManagement.showDetail" url="${resourceShowDetail}" tabindex="1"/>
                    <c:choose>
                        <c:when test="${resource.isWritable || administrationMode}">
                            | <tag:listAction code="modify" url="${resourceModifyUrl}" tabindex="2"/>
                        </c:when>
                        <c:otherwise>
                            | <tag:listAction code="modify" disabled="true" tabindex="2"/>
                        </c:otherwise>
                    </c:choose>
                    <c:choose>
                    <c:when test="${resource.isWritable || administrationMode}">
                        | <tag:listAction code="delete" url="${resourceSingleDeleteUrl}" tabindex="3"/>
                    </c:when>
                    <c:otherwise>
                        | <tag:listAction code="delete" disabled="true" url="${resourceSingleDeleteUrl}" tabindex="3"/>
                    </c:otherwise>
                </c:choose>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${fn:length(readableResources) eq 0}">
            <td colspan="6" class="empty"><spring:message code="views.list.none"/></td>
        </c:if>
        </tbody>
    </table>


    <c:if test="${administrationMode}">
    <a class="btn btn-default pull-right" style="margin-top: 15px;" href="${resourceCreateUrl}">
        <spring:message code="views.resource.add"/>
    </a>
    </c:if>
