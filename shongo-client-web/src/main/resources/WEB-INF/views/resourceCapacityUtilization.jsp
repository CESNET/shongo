<%--
  -- Page displaying resource capacity utilization.
  --%>
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
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${resourceCapacityUtilization}" var="entry">
        <tr>
            <td><tag:format value="${entry.key}"/></td>
            <c:forEach items="${resourceCapacitySet}" var="resourceCapacity">
                <c:set var="utilization" value="${entry.value.get(resourceCapacity)}"/>
                <td>${resourceCapacity.formatUtilization(utilization)}</td>
            </c:forEach>
        </tr>
    </c:forEach>
    </tbody>
</table>

