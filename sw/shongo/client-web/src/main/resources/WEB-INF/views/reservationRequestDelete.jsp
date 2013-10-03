
<%--
  -- Page for confirmation of deletion of reservation request or for displaying dependencies because of which
  -- the reservation request can't be deleted.
  --%>
<%@ page language="java" pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" trimDirectiveWhitespaces="true" %>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="backUrl"><%= ClientWebUrl.RESERVATION_REQUEST_LIST %></c:set>
<tag:url var="backUrl" value="${requestScope.backUrl.getUrl(backUrl)}"/>

<tag:reservationRequestDelete dependencies="${dependencies}" detailUrl="<%= ClientWebUrl.RESERVATION_REQUEST_DETAIL %>"/>

<c:choose>
    <c:when test="${dependencies.size() > 0}">
        <div>
            <a class="btn btn-primary" href="${backUrl}" tabindex="1"><spring:message code="views.button.back"/></a>
            <form method="post" action="?dependencies=true" class="inline">
                <spring:message code="views.button.yes" var="buttonYes"/>
                <spring:message code="views.button.deleteAll" var="buttonDeleteAll"/>
                <input type="submit" class="btn" tabindex="1" value="${buttonDeleteAll}"/>
            </form>
        </div>
    </c:when>
    <c:otherwise>
        <div>
            <form method="post" class="inline">
                <spring:message code="views.button.yes" var="buttonYes"/>
                <input type="submit" class="btn btn-primary" tabindex="1" value="${buttonYes}"/>
            </form>
            <a class="btn" href="${backUrl}" tabindex="1"><spring:message code="views.button.no"/></a>
        </div>
    </c:otherwise>
</c:choose>

