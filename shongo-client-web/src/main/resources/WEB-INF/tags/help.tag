<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%----%>
<%@attribute name="label" required="false" type="java.lang.String"%>
<%@attribute name="cssClass" required="false" type="java.lang.String"%>
<%@attribute name="position" required="false" type="java.lang.String"%>
<%@attribute name="selectable" required="false" type="java.lang.Boolean"%>
<%@attribute name="width" required="false" type="java.lang.String"%>
<%@attribute name="content" required="false" type="java.lang.String"%>
<%----%><%--
--%><c:set var="contextPath" value="${pageContext.request.contextPath}"/><%--
--%><c:set var="tooltipAttributes"><%--
--%>tooltip selectable="${selectable != null && selectable}" position="${position}" tooltip-width="${width}"<%--
--%><c:if test="${not empty content}"> content="${content}"</c:if><%--
--%></c:set><%--
--%><c:choose><%--
    --%><c:when test="${not empty label}"><%--
        --%><span ${tooltipAttributes} class="dotted ${cssClass}">${label}</span><%--
        --%><c:if test="${empty content}"><span class="hidden"><jsp:doBody/></span></c:if><%--
    --%></c:when><%--
    --%><c:otherwise><%--
        --%><img ${tooltipAttributes} class="help-icon ${cssClass}" src="${contextPath}/img/help.gif"/><%--
        --%><c:if test="${empty content}"><span class="hidden"><jsp:doBody/></span></c:if><%--
    --%></c:otherwise><%--
--%></c:choose>