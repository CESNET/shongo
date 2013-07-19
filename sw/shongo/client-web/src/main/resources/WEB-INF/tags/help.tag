<%@ tag trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@attribute name="label" required="false"%>
<%@attribute name="tooltipId" required="false"%>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<c:set var="test" value="${pageContext}"/>

<%
    if (tooltipId == null) {
        // Generate new tooltipId
        Integer tagHelpId = (Integer) request.getAttribute("SHONGO_APP_TAG_HELP_ID");
        if (tagHelpId == null) {
            tagHelpId = 0;
        }
        tagHelpId += 1;
        request.setAttribute("SHONGO_APP_TAG_HELP_ID", tagHelpId);
        jspContext.setAttribute("tooltipId", "tooltip-" + tagHelpId);
    }
%>

<div class="tooltip-container">
    <c:choose>
        <c:when test="${not empty label}">
            <img tooltip="${tooltipId}" label="${label}" class="tooltip-label help-icon" src="${contextPath}/img/help.gif"/>
        </c:when>
        <c:otherwise>
            <img tooltip="${tooltipId}" class="tooltip-label help-icon" src="${contextPath}/img/help.gif"/>
        </c:otherwise>
    </c:choose>
    <jsp:doBody var="body"/>
    <c:if test="${not empty body}">
        <div id="${tooltipId}" class="tooltip-content">
            <span>${body}</span>
        </div>
    </c:if>
</div>
