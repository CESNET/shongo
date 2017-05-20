<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<tag:url var="resourceDetail" value="<%= ClientWebUrl.RESOURCE_DETAIL %>">
    <tag:param name="resourceId" value="${resourceId}"/>
</tag:url>

<tag:url var="resourceManagement" value="<%= ClientWebUrl.RESOURCE_RESOURCES %>"/>



<p><strong><spring:message code="views.resourceDelete.question"/></strong></p>




<ul>
    <li>
        <strong><a href="${resourceDetail}" target="_blank"><c:out value="${resourceName}"/></a></strong>
        <br/>
        <p><c:out value="${resourceDescription}"/></p>
    </li>
</ul>

<div>
    <form method="post" class="form-inline">
        <spring:message code="views.button.yes" var="buttonYes"/>
        <input type="submit" class="btn btn-primary" tabindex="1" value="${buttonYes}"/>
        <a class="btn btn-default" href="${resourceManagement}" tabindex="1"><spring:message code="views.button.no"/></a>
    </form>
</div>