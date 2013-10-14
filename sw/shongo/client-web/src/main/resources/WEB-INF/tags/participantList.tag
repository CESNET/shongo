<%--
  -- List of participants.
  --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="isWritable" required="false" %>
<%@attribute name="data" required="false" type="java.util.Collection" %>
<%@attribute name="description" required="false" type="java.lang.Boolean" %>
<%@attribute name="createUrl" required="false" %>
<%@attribute name="modifyUrl" required="false" %>
<%@attribute name="deleteUrl" required="false" %>
<%@attribute name="urlParam" required="false" %>
<%@attribute name="urlValue" required="false" %>

<c:set var="isWritable" value="${isWritable != null ? isWritable : true}"/>
<c:set var="tableHead">
    <thead>
    <tr>
        <th><spring:message code="views.participant.userId"/></th>
        <th><spring:message code="views.participant.role"/></th>
        <th><spring:message code="views.participant.email"/></th>
        <c:if test="${description}">
            <th><spring:message code="views.participant.description"/></th>
        </c:if>
        <c:if test="${isWritable && (not empty modifyUrl || not empty deleteUrl)}">
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
    <c:when test="${data != null}">
        <table class="table table-striped table-hover">
                ${tableHead}
            <tbody>
            <c:forEach items="${data}" var="participant">
                <tr>
                    <td>${participant.name}</td>
                    <td><spring:message code="views.participant.role.${participant.role}"/></td>
                    <td>${participant.email}</td>
                    <c:if test="${description}">
                        <td>${participant.description}</td>
                    </c:if>
                    <c:if test="${isWritable && (not empty modifyUrl || not empty deleteUrl)}">
                        <td>
                            <c:if test="${not empty participant.id && not empty modifyUrl}">
                                <tag:url var="participantModifyUrl" value="${modifyUrl}">
                                    <c:if test="${not empty urlParam}">
                                        <tag:param name="${urlParam}" value="${participant[urlValue]}"/>
                                    </c:if>
                                    <tag:param name="participantId" value="${participant.id}"/>
                                </tag:url>
                                <tag:listAction code="modify" url="${participantModifyUrl}" tabindex="2"/>
                            </c:if>
                            <c:if test="${not empty participant.id && not empty deleteUrl}">
                                <tag:url var="participantDeleteUrl" value="${deleteUrl}">
                                    <c:if test="${not empty urlParam}">
                                        <tag:param name="${urlParam}" value="${participant[urlValue]}"/>
                                    </c:if>
                                    <tag:param name="participantId" value="${participant.id}"/>
                                </tag:url>
                                <tag:listAction code="delete" url="${participantDeleteUrl}" tabindex="2"/>
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


