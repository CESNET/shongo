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
                <c:when test="${wizardPage == wizardPageActive}">
                    <c:set var="classLink" value="link active"/>
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
                        <spring:message code="${wizardPage.titleCode}" arguments="${wizardPage.titleArguments}"/>
                    </a>
                </c:when>
                <c:otherwise>
                    <span class="${classLink}">
                        <span class="${classBadge}">${wizardPageStatus.index + 1}</span>
                        <spring:message code="${wizardPage.titleCode}" arguments="${wizardPage.titleArguments}"/>
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

    <c:forEach items="${wizardActions}" var="wizardAction">
        <c:if test="${wizardAction.url != null && wizardAction.position == 'LEFT'}">
            <c:set var="wizardActionClass" value="btn btn-default"/>
            <c:if test="${wizardAction.primary}">
                <c:set var="wizardActionClass" value="${wizardActionClass} btn-primary"/>
            </c:if>
            <a class="${wizardActionClass}" href="${wizardAction.url.startsWith('javascript:') ? '' : contextPath}${wizardAction.url}" tabindex="3">
                <spring:message code="${wizardAction.titleCode}"/>
            </a>
        </c:if>
    </c:forEach>
    <div class="pull-right">
        <c:forEach items="${wizardActions}" var="wizardAction">
            <c:if test="${wizardAction.url != null && wizardAction.position == 'RIGHT'}">
                <c:set var="wizardActionClass" value="btn btn-default"/>
                <c:if test="${wizardAction.primary}">
                    <c:set var="wizardActionClass" value="${wizardActionClass} btn-primary"/>
                </c:if>
                <a class="${wizardActionClass}" href="${wizardAction.url.startsWith('javascript:') ? '' : contextPath}${wizardAction.url}" tabindex="2">
                    <spring:message code="${wizardAction.titleCode}"/>
                </a>
            </c:if>
        </c:forEach>
    </div>
</div>