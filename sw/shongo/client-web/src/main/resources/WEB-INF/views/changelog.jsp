<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>

<c:forEach var="version" items="${changelog}">
    <strong>${version.name}</strong> (<joda:format value="${version.dateTime}" style="M-" />)
    <ul>
        <c:forEach var="change" items="${version.changes}">
            <li>${change}</li>
        </c:forEach>
    </ul>
</c:forEach>
