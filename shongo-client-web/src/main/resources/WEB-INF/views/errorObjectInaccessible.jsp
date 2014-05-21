<%--
  -- Page which is shown when the reservation request doesn't exist or user doesn't have proper permissions.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="administratorEmails">
    <c:forEach items="${configuration.administratorEmails}" var="administratorEmail" varStatus="status">
        <c:if test="${!status.first}">,</c:if>${administratorEmail}
    </c:forEach>
</c:set>
<p><spring:message code="views.errorObjectInaccessible.text" arguments="${objectId};${administratorEmails}" argumentSeparator=";"/></p>

