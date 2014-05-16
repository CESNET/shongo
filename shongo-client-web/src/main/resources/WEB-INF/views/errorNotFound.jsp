<%--
  -- Page which is displayed when uncaught exception is thrown or when other error happens.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<div>
    <p><spring:message code="views.errorNotFound.page"/></p>
    <pre><%= request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) %></pre>
    <p><spring:message code="views.errorNotFound.notFound"/></p>
</div>
