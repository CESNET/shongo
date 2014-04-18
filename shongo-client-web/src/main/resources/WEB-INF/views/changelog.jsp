<%--
  -- Page displaying changes for each released Shongo version.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:forEach var="version" items="${changelog}">
    <strong>${version.name}</strong> (<tag:format value="${version.dateTime}" style="date"/>)
    <ul>
        <c:forEach var="change" items="${version.changes}">
            <li>${change}</li>
        </c:forEach>
    </ul>
</c:forEach>
