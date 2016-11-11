<%--
  --    Page displaying resources with possibility to create new or edit existing resources.
  --%>

<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<html>
<head>
    <title>Title</title>
</head>
<body>

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
                <tag:url var="resourceModifyUrl" value="<%= ClientWebUrl.RESOURCE_MODIFY %>">
                    <tag:param name="resourceId" value="${resource.id}" escape="false"/>
                </tag:url>
                <tr>
                    <td>${resource.get("name")}</td>
                    <td>${resource.get("id")}</td>
                    <td>${resource.get("technology")}</td>
                    <td>${resource.get("description")}</td>
                    <td>
                        <tag:listAction code="show" titleCode="views.resourceManagement.showDetail" url="" tabindex="1"/>
                        <c:choose>
                            <c:when test="${resource.isWritable}">
                                | <tag:listAction code="modify" url="${resourceModifyUrl}" tabindex="2"/>
                            </c:when>
                            <c:otherwise>
                                | <tag:listAction code="modify" disabled="true" tabindex="2"/>
                            </c:otherwise>
                        </c:choose>

                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>


</body>
</html>
