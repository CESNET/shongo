<%--
  -- Page displaying resource reservations.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<div style="overflow: auto;">
<table class="table table-bordered">
<c:forEach var="rowLabel" items="1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20">
    <tr>
        <td>${rowLabel}</td>
        <c:forEach var="rowLabel" items="1,2,3,4,5,6,7,8,9,10,11,12,13,14,15">
            <td>value</td>
        </c:forEach>
    </tr>
</c:forEach>
</table>
</div>
