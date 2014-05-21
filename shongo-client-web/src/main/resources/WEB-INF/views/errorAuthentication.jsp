<%--
  -- Page which is shown when authentication has failed.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<p>${message}</p>
<c:if test="${not empty reason}">
    <pre>${reason}</pre>
</c:if>

<tag:url var="loginUrl" value="<%= ClientWebUrl.LOGIN %>"/>
<a class="btn btn-primary" href="${loginUrl}"><spring:message code="views.errorAuthentication.login"/></a>

