<%--
  -- Table in resource capacity utilization page.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<div style="overflow: auto;">
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
            <td style="min-width: 250px;"><tag:format value="${entry.key}" style="date"/></td>
            <c:forEach items="${resourceCapacitySet}" var="resourceCapacity">
                <c:set var="utilization" value="${entry.value.get(resourceCapacity)}"/>
                <td style="width: 150px;" class="${resourceCapacity.getCssClass(utilization)}">
                    <tag:url var="resourceCapacityUtilizationDescriptionUrl" value="<%= ClientWebUrl.RESOURCE_CAPACITY_UTILIZATION_DESCRIPTION %>">
                        <tag:param name="interval" value="${entry.key}"/>
                        <tag:param name="resourceCapacityClass" value="${resourceCapacity.className}"/>
                        <tag:param name="resourceId" value="${resourceCapacity.resourceId}"/>
                    </tag:url>
                    <spring:message var="valueMaximum" code="views.resourceCapacityUtilization.value.maximum"/>
                    <spring:message var="valueAverage" code="views.resourceCapacityUtilization.value.average"/>
                    <a class="maximum" href="${resourceCapacityUtilizationDescriptionUrl}" target="_blank" title="${valueMaximum}">
                        ${resourceCapacity.formatUtilization(utilization, 'MAXIMUM', style)}<c:if test="${style == 'ABSOLUTE'}">/${resourceCapacity.licenseCount}</c:if>
                    </a>
                    <span title="${valueAverage}">(${resourceCapacity.formatUtilization(utilization, 'AVERAGE', style)})</span>
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
    <%--
    <tr>
        <td></td>
        <c:forEach items="10,20,30,40,50,60,70,80,90,100" var="item">
            <td class="utilized utilized${item}">Test</td>
        </c:forEach>
    </tr>--%>
</table>

</div>