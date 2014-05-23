<%--
  -- Page which is shown when the reservation request doesn't exist or user doesn't have proper permissions.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<p><spring:message code="views.errorObjectInaccessible.text" arguments="${objectId};${configuration.suggestionEmail}" argumentSeparator=";"/></p>

