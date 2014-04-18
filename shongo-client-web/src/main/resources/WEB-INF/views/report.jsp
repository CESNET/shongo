<%--
  -- Page for reporting problems.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<tag:url var="backUrl" value="${requestScope.backUrl}"/>

<c:choose>
    <c:when test="${isSent != null}">
        <c:choose>
            <c:when test="${isSent}">
                <p><spring:message code="views.report.sendingSucceeded"/></p>
            </c:when>
            <c:otherwise>
                <p><spring:message code="views.report.sendingFailed"/></p>
                <c:if test="${not empty configuration.hotlines}">
                    <p><spring:message code="views.report.sendingFailed.hotline"/>:</p>
                    <ul>
                        <c:forEach items="${configuration.hotlines}" var="hotline">
                            <li>${hotline}</li>
                        </c:forEach>
                    </ul>
                </c:if>
                <p><spring:message code="views.report.sendingFailed.context"/>:</p>
                <pre>${report.getEmailContent(pageContext.request)}</pre>
            </c:otherwise>
        </c:choose>
        <a class="btn btn-primary" href="${backUrl}"><spring:message code="views.button.back"/></a>

    </c:when>
    <c:otherwise>
        <tag:reportForm/>
    </c:otherwise>
</c:choose>
