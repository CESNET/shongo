<%@ tag trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<c:set var="test" value="${pageContext}"/>

<%
    Integer tagHelpId = (Integer) request.getAttribute("SHONGO_APP_TAG_HELP_ID");
    if (tagHelpId == null) {
        tagHelpId = 0;
    }
    tagHelpId += 1;
    request.setAttribute("SHONGO_APP_TAG_HELP_ID", tagHelpId);
    jspContext.setAttribute("tooltipId", "tooltip-" + tagHelpId);
%>


<div class="tooltip-container">
    <img tooltip="${tooltipId}" class="tooltip-label help-icon" src="${contextPath}/img/help.gif"/>
    <div id="${tooltipId}" class="tooltip-content">
        <jsp:doBody/>
    </div>
</div>
