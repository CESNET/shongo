<%--
  -- Table in resource capacity utilization page.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<table class="table table-striped table-hover">
    <thead>
    <tr>
        <th></th>
        <c:forEach items="${resourceCapacitySet}" var="resourceCapacity">
            <th>${resourceCapacity.resourceName}</th>
        </c:forEach>
        <c:if test="${empty resourceCapacitySet}">
            <th><spring:message code="views.resource.none"/></th>
        </c:if>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${resourceCapacityUtilization}" var="entry">
        <tr>
            <td style="width: 250px;"><tag:format value="${entry.key}" style="date"/></td>
            <c:forEach items="${resourceCapacitySet}" var="resourceCapacity">
                <c:set var="utilization" value="${entry.value.get(resourceCapacity)}"/>
                <td style="width: 100px;">
                    <tag:url var="resourceCapacityUtilizationDescriptionUrl" value="<%= ClientWebUrl.RESOURCE_CAPACITY_UTILIZATION_DESCRIPTION %>">
                        <tag:param name="interval" value="${entry.key}"/>
                        <tag:param name="resourceCapacityClass" value="${resourceCapacity.className}"/>
                        <tag:param name="resourceId" value="${resourceCapacity.resourceId}"/>
                    </tag:url>
                    <a href="${resourceCapacityUtilizationDescriptionUrl}" target="_blank">
                        ${resourceCapacity.formatUtilization(utilization, type)}
                    </a>
                </td>
            </c:forEach>
            <td></td>
        </tr>
    </c:forEach>
    </tbody>
    <tfoot>
    <tr>
        <th></th>
        <c:forEach items="${resourceCapacitySet}" var="resourceCapacity">
            <th>${resourceCapacity.resourceName}</th>
        </c:forEach>
        <c:if test="${empty resourceCapacitySet}">
            <th><spring:message code="views.resource.none"/></th>
        </c:if>
    </tr>
    </tfoot>
</table>

