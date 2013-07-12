<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%--
  -- Wizard page.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="app" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<c:if test="${pages != null}">
    <div class="wizard">
        <c:forEach items="${pages}" var="page" varStatus="pageStatus">
            <c:choose>
                <c:when test="${page == currentPage}">
                    <c:set var="classLink" value="link current"/>
                    <c:set var="classBadge" value="badge badge-inverse"/>
                </c:when>
                <c:otherwise>
                    <c:set var="classLink" value="link"/>
                    <c:set var="classBadge" value="badge"/>
                </c:otherwise>
            </c:choose>
            <c:choose>
                <c:when test="${page.url != null}">
                    <a href="${page.url}" class="${classLink}"><span class="${classBadge}">${pageStatus.index + 1}</span>${page.titleCode}</a>
                </c:when>
                <c:otherwise>
                    <span class="${classLink}"><span class="${classBadge}">${pageStatus.index + 1}</span>${page.titleCode}</span>
                </c:otherwise>
            </c:choose>
        </c:forEach>
    </div>
</c:if>

<c:set var="urlBack">${contextPath}<%= ClientWebUrl.HOME %></c:set>
<c:if test="${currentPage.parentPage != null}">
    <c:set var="urlBack">${contextPath}${currentPage.parentPage.url}</c:set>
</c:if>

<c:choose>
    <c:when test="${currentPage == 'SELECT'}">
        <c:set var="urlReservations">${contextPath}<%= ClientWebUrl.WIZARD_RESERVATIONS %></c:set>
        <c:set var="urlCreateRoom">${contextPath}<%= ClientWebUrl.WIZARD_CREATE_ROOM %></c:set>
        <c:set var="urlCreatePermanentRoomCapacity">${contextPath}<%= ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY %></c:set>
        <div class="actions">
            <span>Co si prejete udelat?</span>
            <ul>
                <li><a href="${urlReservations}">Zobrazit seznam vasich rezervaci</a></li>
                <li><a href="${urlCreateRoom}">Zarezervovat novou mistnost</a></li>
                <li><a href="${urlCreatePermanentRoomCapacity}">Zarezervovat kapacitu pro existujici mistnost</a></li>
            </ul>
        </div>
    </c:when>

    <c:when test="${currentPage == 'RESERVATIONS'}">
        TODO: reservations
    </c:when>

    <c:when test="${currentPage == 'CREATE_ROOM'}">
        <c:set var="urlCreateAdhocRoom">${contextPath}<%= ClientWebUrl.WIZARD_CREATE_ADHOC_ROOM %></c:set>
        <c:set var="urlCreatePermanentRoom">${contextPath}<%= ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM %></c:set>
        <div class="actions">
            <span>Jakou mistnost si prejete zarezervovat?</span>
            <ul>
                <li><a href="${urlCreateAdhocRoom}">Jednorazovou mistnost</a></li>
                <li><a href="${urlCreatePermanentRoom}">Permanentni mistnost</a></li>
            </ul>
        </div>
    </c:when>
</c:choose>

<div>
    <a class="btn btn-primary" href="${urlBack}">
        <spring:message code="views.button.back"/>
    </a>
</div>