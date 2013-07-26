<%@ tag trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@attribute name="label" required="false"%>
<%@attribute name="labelElement" required="false"%>
<%@attribute name="labelClass" required="false"%>
<%@attribute name="tooltipId" required="false"%>
<%@attribute name="type" required="false"%>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<%
    // Generate new tooltipId
    if (tooltipId == null) {
        Integer tagHelpId = (Integer) request.getAttribute("SHONGO_APP_TAG_HELP_ID");
        if (tagHelpId == null) {
            tagHelpId = 0;
        }
        tagHelpId += 1;
        request.setAttribute("SHONGO_APP_TAG_HELP_ID", tagHelpId);
        jspContext.setAttribute("tooltipId", "tooltip-" + tagHelpId);
    }

    // Set type
    if (labelElement != null) {
        type = "icon";
    }
    else if (type == null && label != null) {
        type = "text";
    }
    else if (type == null) {
        type = "icon";
    }
    jspContext.setAttribute("type", type);
%>

<c:choose>
    <c:when test="${type == 'text'}">

        <div class="tooltip-container">
            <span tooltip="${tooltipId}" class="tooltip-label dotted ${labelClass}">${label}</span>
            <jsp:doBody var="body"/>
            <c:if test="${not empty body}">
                <div id="${tooltipId}" class="tooltip-content">
                    <span>${body}</span>
                </div>
            </c:if>
        </div>

    </c:when>
    <c:otherwise>

        <c:if test="${not empty label && empty labelElement}">
            <c:set var="labelElement" value="${tooltipId}-label"/>
            <span id="${labelElement}" class="${labelClass}">${label}</span>
        </c:if>
        <div class="tooltip-container">
            <c:choose>
                <c:when test="${not empty labelElement}">
                    <img tooltip="${tooltipId}" label="${labelElement}" class="tooltip-label help-icon" src="${contextPath}/img/help.gif"/>
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

    </c:otherwise>
</c:choose>
