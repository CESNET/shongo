<%@ tag trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<c:set var="tooltipId" value="tooltip-0"/>

<div class="tooltip-container">
    <img tooltip="${tooltipId}" class="tooltip-label" src="${contextPath}/img/help.gif"/>
    <div id="${tooltipId}" class="tooltip-content">
        <jsp:doBody/>
    </div>
</div>
