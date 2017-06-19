<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%----%>

<%@ attribute name="value" required="true" type="java.lang.Boolean" %>
<%----%>
<c:choose>
    <c:when test="${value}">
        <i class="fa fa-check" aria-hidden="true"></i>
    </c:when>
    <c:otherwise>
        <i class="fa fa-times" aria-hidden="true"></i>
    </c:otherwise>

</c:choose>
