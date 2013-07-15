<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%--
  -- Wizard page.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="app" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<c:if test="${wizardPages != null}">
    <div class="wizard">
        <c:forEach items="${wizardPages}" var="page" varStatus="pageStatus">
            <c:choose>
                <c:when test="${page == navigation.page}">
                    <c:set var="classLink" value="link current"/>
                    <c:set var="classBadge" value="badge badge-inverse"/>
                </c:when>
                <c:otherwise>
                    <c:set var="classLink" value="link"/>
                    <c:set var="classBadge" value="badge"/>
                </c:otherwise>
            </c:choose>
            <c:choose>
                <c:when test="${page.url != null && availablePages.contains(page)}">
                    <a href="${page.url}" class="${classLink}">
                        <span class="${classBadge}">${pageStatus.index + 1}</span>${page.titleCode}
                    </a>
                </c:when>
                <c:otherwise>
                    <span class="${classLink}">
                        <span class="${classBadge}">${pageStatus.index + 1}</span>${page.titleCode}
                    </span>
                </c:otherwise>
            </c:choose>
        </c:forEach>
    </div>
</c:if>

<c:choose>
    <c:when test="${navigation == 'WIZARD_SELECT'}">
        <c:set var="urlReservations">${contextPath}
            <%= ClientWebUrl.WIZARD_RESERVATION_REQUEST_LIST %>
        </c:set>
        <c:set var="urlCreateRoom">${contextPath}
            <%= ClientWebUrl.WIZARD_CREATE_ROOM %>
        </c:set>
        <c:set var="urlCreatePermanentRoomCapacity">
            ${contextPath}<%= ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY %>
        </c:set>
        <div class="actions">
            <span>Co si prejete udelat?</span>
            <ul>
                <li><a href="${urlCreateRoom}">Zarezervovat novou mistnost</a></li>
                <li><a href="${urlCreatePermanentRoomCapacity}">Zarezervovat kapacitu pro existujici mistnost</a></li>
                <li><a href="${urlReservations}">Zobrazit seznam vasich rezervaci</a></li>
            </ul>
        </div>
    </c:when>

    <c:when test="${navigation == 'WIZARD_RESERVATION_REQUEST'}">
        TODO: reservation requests
    </c:when>

    <c:when test="${navigation == 'WIZARD_RESERVATION_REQUEST_DETAIL'}">
        TODO: reservation request detail
    </c:when>

    <c:when test="${navigation == 'WIZARD_CREATE_ROOM'}">
        <c:set var="urlCreateAdhocRoom">${contextPath}<%= ClientWebUrl.WIZARD_CREATE_ADHOC_ROOM %>
        </c:set>
        <c:set var="urlCreatePermanentRoom">${contextPath}<%= ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM %>
        </c:set>
        <div class="actions">
            <span>Jakou mistnost si prejete zarezervovat?</span>
            <ul>
                <li><a href="${urlCreateAdhocRoom}">Jednorazovou mistnost</a></li>
                <li><a href="${urlCreatePermanentRoom}">Permanentni mistnost</a></li>
            </ul>
        </div>
    </c:when>

    <c:when test="${navigation == 'WIZARD_CREATE_ROOM_ATTRIBUTES' || navigation == 'WIZARD_CREATE_PERMANENT_ROOM_CAPACITY'}">
        <c:set var="nextPageUrl">javascript: document.getElementById('reservationRequest').submit();</c:set>
        <c:set var="confirmUrl">${contextPath}<%= ClientWebUrl.WIZARD_CREATE_CONFIRM %></c:set>
        <h1>Create ${reservationRequest.specificationType}</h1>
        <jsp:include page="reservationRequestForm.jsp">
            <jsp:param name="reservationRequestFormId" value="reservationRequest"/>
            <jsp:param name="confirmUrl" value="${confirmUrl}"/>
        </jsp:include>
    </c:when>

    <c:when test="${navigation == 'WIZARD_CREATE_ROOM_ROLES'}">
        <h1>Use roles</h1>
    </c:when>

</c:choose>

<div>
    <c:if test="${nextPageUrl == null && navigation.nextPage != null && navigation.nextPage.url != null && availablePages.contains(navigation.nextPage)}">
        <c:set var="nextPageUrl">${contextPath}${navigation.nextPage.url}</c:set>
    </c:if>
    <c:if test="${nextPageUrl != null}">
        <a class="btn btn-primary pull-right" href="${nextPageUrl}">
            <spring:message code="views.button.continue"/>
        </a>
    </c:if>
    <c:if test="${navigation.previousPage != null}">
        <c:set var="previousPageUrl">${contextPath}${navigation.previousPage.url}</c:set>
        <a class="btn btn-primary" href="${previousPageUrl}">
            <spring:message code="views.button.back"/>
        </a>
    </c:if>
</div>