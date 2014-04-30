<%--
  -- Page layout template to which are inserted all other pages into "body" attribute.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<tiles:importAttribute name="css"/>
<tiles:importAttribute name="js"/>
<tiles:importAttribute name="i18n"/>
<tiles:importAttribute name="title"/>
<tiles:importAttribute name="heading"/>

<%-- Context path --%>
<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<%-- JS, CSS and i18n files --%>
<c:set var="head">
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

<%-- Title --%>
<c:choose>
    <c:when test="${title.getClass().name == 'java.lang.String' && not empty title}">
        <spring:message code="${title}" var="title"/>
    </c:when>
    <c:otherwise>
        <c:set var="title">
            <c:forEach items="${title}" var="titleItem" varStatus="titleStatus">
                <c:if test="${titleItem != null}">
                    <c:set var="titleItem"><tiles:insertAttribute value="${titleItem}"/></c:set>
                    <c:choose>
                        <c:when test="${empty titleItem}"/>
                        <c:when test="${titleItem.startsWith('T(')}">
                            <c:if test="${titleItem.length() > 3}">
                                <c:if test="${!titleStatus.first}"> - </c:if>
                                ${titleItem.substring(2, titleItem.length() - 1)}
                            </c:if>
                        </c:when>
                        <c:when test="${titleItem.startsWith('M(')}">
                            <c:if test="${!titleStatus.first}"> - </c:if>
                            <c:forEach items="${titleItem.substring(2, titleItem.length() - 1).split(',')}"
                                       var="titleItemItem" varStatus="titleItemStatus">
                                <c:set var="titleItemMessageArguments" value=""/>
                                <c:choose>
                                    <c:when test="${titleItemStatus.first}">
                                        <c:set var="titleItemMessage" value="${titleItemItem}"/>
                                    </c:when>
                                    <c:otherwise>
                                        <c:set var="titleItemMessageArguments"
                                               value="${titleItemMessageArguments}${titleItemItem},"/>
                                    </c:otherwise>
                                </c:choose>
                            </c:forEach>
                            <spring:message code="${titleItemMessage}" arguments="${titleItemMessageArguments}"/>
                        </c:when>
                        <c:otherwise>
                            <c:if test="${!titleStatus.first}"> - </c:if>
                            <spring:message code="${titleItem}"/>
                        </c:otherwise>
                    </c:choose>
                </c:if>
            </c:forEach>
        </c:set>
    </c:otherwise>
</c:choose>




<%-- Content --%>
<c:set var="content">
    <c:choose>
        <c:when test="${heading == 'title'}">
            <h1>${title}</h1>
        </c:when>
        <c:when test="${heading != ''}">
            <h1>${heading}</h1>
        </c:when>
    </c:choose>
    <tiles:insertAttribute name="content"/>
</c:set>

<%-- Render layout --%>
${designLayout.render(head, title, content)}
