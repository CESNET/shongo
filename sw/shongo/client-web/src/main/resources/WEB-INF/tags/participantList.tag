<%--
  -- List of participants.
  --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="isWritable" required="false" %>
<%@attribute name="data" required="false" type="java.util.Collection" %>
<%@attribute name="createUrl" required="false" %>
<%@attribute name="modifyUrl" required="false" %>
<%@attribute name="deleteUrl" required="false" %>

<c:set var="isWritable" value="${isWritable != null ? isWritable : true}"/>
<c:set var="tableHead">
    <thead>
    <tr>
        <th><spring:message code="views.aclRecord.user"/></th>
        <th><spring:message code="views.aclRecord.role"/></th>
        <th><spring:message code="views.aclRecord.email"/></th>
        <c:if test="${isWritable && not empty deleteUrl}">
            <th style="min-width: 85px; width: 85px;">
                <spring:message code="views.list.action"/>
            </th>
        </c:if>
    </tr>
    </thead>
</c:set>
<c:set var="tableEmptyRow">
    <td colspan="4" class="empty"><spring:message code="views.list.none"/></td>
</c:set>

<c:choose>
    <%-- Static list of user roles --%>
    <c:when test="${data != null}">
        <table class="table table-striped table-hover">
                ${tableHead}
            <tbody>
            <c:forEach items="${data}" var="userRole">
                <tag:url var="aclDeleteUrl" value="${deleteUrl}">
                    <tag:param name="aclRecordId" value="${userRole.id}"/>
                </tag:url>
                <tr>
                    <td>${userRole.user.fullName} (${userRole.user.originalId})</td>
                    <td><spring:message code="views.aclRecord.role.${userRole.role}"/></td>
                    <td>${userRole.user.primaryEmail}</td>
                    <c:if test="${isWritable && not empty aclDeleteUrl}">
                        <td>
                            <c:if test="${not empty userRole.id && userRole.deletable}">
                                <tag:listAction code="delete" url="${aclDeleteUrl}" tabindex="2"/>
                            </c:if>
                        </td>
                    </c:if>
                </tr>
            </c:forEach>
            <c:if test="${empty data}">
                <tr>${tableEmptyRow}</tr>
            </c:if>
            </tbody>
        </table>
        <c:if test="${isWritable && createUrl != null}">
            <a class="btn btn-primary" href="${createUrl}">
                <spring:message code="views.button.add"/>
            </a>
        </c:if>
    </c:when>

</c:choose>


