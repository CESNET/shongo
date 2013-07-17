<%--
  -- Wizard user interface.
  --%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<%-- Wizard pages --%>
<c:if test="${wizardPages != null}">
    <div class="wizard">
        <c:forEach items="${wizardPages}" var="wizardPage" varStatus="wizardPageStatus">
            <c:choose>
                <c:when test="${wizardPage == wizardPageCurrent}">
                    <c:set var="classLink" value="link current"/>
                    <c:set var="classBadge" value="badge badge-inverse"/>
                </c:when>
                <c:otherwise>
                    <c:set var="classLink" value="link"/>
                    <c:set var="classBadge" value="badge"/>
                </c:otherwise>
            </c:choose>
            <c:choose>
                <c:when test="${wizardPage.url != null && wizardPage.available}">
                    <a href="${wizardPage.url}" class="${classLink}">
                        <span class="${classBadge}">${wizardPageStatus.index + 1}</span>
                        <spring:message code="${wizardPage.titleCode}"/>
                    </a>
                </c:when>
                <c:otherwise>
                    <span class="${classLink}">
                        <span class="${classBadge}">${wizardPageStatus.index + 1}</span>
                        <spring:message code="${wizardPage.titleCode}"/>
                    </span>
                </c:otherwise>
            </c:choose>
        </c:forEach>
    </div>
</c:if>

<%-- Wizard page content --%>
<tiles:importAttribute name="content"/>
<tiles:insertAttribute name="content"/>

<%-- Wizard navigation --%>
<div>
    <c:set var="primaryClass" value="btn-primary"/>

    <%-- Link to next page --%>
    <c:if test="${wizardPageNextUrl == null && wizardPageNext != null}">
        <c:set var="wizardPageNextUrl" value="${wizardPageNext.url}"/>
    </c:if>
    <c:if test="${wizardPageNextUrl != null && wizardPageNextUrl != ''}">
        <c:if test="${wizardPageNextTitle == null}">
            <c:choose>
                <c:when test="${wizardPageNext != null}">
                    <c:set var="wizardPageNextTitle" value="views.button.continue"/>
                </c:when>
                <c:otherwise>
                    <c:set var="wizardPageNextTitle" value="views.button.finish"/>
                </c:otherwise>
            </c:choose>
        </c:if>
        <a class="btn ${primaryClass} pull-right" href="${contextPath}${wizardPageNextUrl}">
            <spring:message code="${wizardPageNextTitle}"/>
        </a>
        <c:set var="primaryClass"></c:set>
    </c:if>

    <%-- Link to previous page --%>
    <c:if test="${wizardPagePreviousUrl == null && wizardPagePrevious != null}">
        <c:set var="wizardPagePreviousUrl" value="${wizardPagePrevious.url}"/>
    </c:if>
    <c:if test="${wizardPagePreviousUrl != null && wizardPagePreviousUrl != ''}">
        <a class=" btn ${primaryClass}" href="${contextPath}${wizardPagePreviousUrl}">
            <spring:message code="views.button.back"/>
        </a>
    </c:if>
</div>