<%--
  -- Page layout template to which are inserted all other pages into "body" attribute.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>

<tiles:importAttribute name="css"/>
<tiles:importAttribute name="js"/>
<tiles:importAttribute name="i18n"/>

<%-- JS, CSS and i18n files --%>
<c:set var="head" scope="request">
<c:forEach items="${css}" var="file">
    <link rel="stylesheet" type="text/css" href="${contextPath}/css/${file}"/><%--
--%></c:forEach>
    <link rel="stylesheet" type="text/css" href="${contextPath}/design/css/style.css"/><%--
--%><c:forEach items="${js}" var="file">
    <script type="text/javascript" src="${contextPath}/js/${file}"></script><%--
--%></c:forEach>
<c:if test="${requestContext.locale.language != 'en'}">
<c:forEach items="${i18n}" var="file">
    <script src="${contextPath}/js/${file}.${requestContext.locale.language}.js"></script><%--
--%></c:forEach>
</c:if>
</c:set>

<%-- Content --%>
<c:set var="content" scope="request">
    <tiles:insertAttribute name="content"/>
</c:set>

<c:import url="${designLayoutUrl}"/>

<%-- Include design layout
<jsp:include page="file://shongo-client-web/design/cesnet/layout.jsp">
    <jsp:param name="head" value="${head}"/>
    <jsp:param name="content" value="${content}"/>
</jsp:include>--%>
