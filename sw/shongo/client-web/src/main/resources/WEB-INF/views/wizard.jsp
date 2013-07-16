<%--
  -- Wizard user interface.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="app" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<%-- List of wizard pages --%>
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
                        <span class="${classBadge}">${pageStatus.index + 1}</span>
                        <spring:message code="${page.titleCode}"/>
                    </a>
                </c:when>
                <c:otherwise>
                    <span class="${classLink}">
                        <span class="${classBadge}">${pageStatus.index + 1}</span>
                        <spring:message code="${page.titleCode}"/>
                    </span>
                </c:otherwise>
            </c:choose>
        </c:forEach>
    </div>
</c:if>

<%-- Content for current wizard page --%>
<c:choose>
    <%-- Select page --%>
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
            <span><spring:message code="views.wizard.select"/></span>
            <ul>
                <li><a href="${urlCreateRoom}">
                    <spring:message code="views.wizard.select.createRoom"/>
                </a></li>
                <li><a href="${urlCreatePermanentRoomCapacity}">
                    <spring:message code="views.wizard.select.createPermanentRoomCapacity"/>
                </a></li>
                <li><a href="${urlReservations}">
                    <spring:message code="views.wizard.select.reservationRequestList"/>
                </a></li>
            </ul>
        </div>
    </c:when>

    <%-- List of reservation requests --%>
    <c:when test="${navigation == 'WIZARD_RESERVATION_REQUEST'}">
        TODO: reservation requests
    </c:when>

    <%-- Detail of reservation request --%>
    <c:when test="${navigation == 'WIZARD_RESERVATION_REQUEST_DETAIL'}">
        TODO: reservation request detail
    </c:when>

    <%-- Select room type to be created --%>
    <c:when test="${navigation == 'WIZARD_CREATE_ROOM'}">
        <c:set var="urlCreateAdhocRoom">${contextPath}<%= ClientWebUrl.WIZARD_CREATE_ADHOC_ROOM %>
        </c:set>
        <c:set var="urlCreatePermanentRoom">${contextPath}<%= ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM %>
        </c:set>
        <div class="actions">
            <span><spring:message code="views.wizard.createRoom"/></span>
            <ul>
                <li><a href="${urlCreateAdhocRoom}"><spring:message code="views.wizard.createRoom.adhoc"/></a></li>
                <li><a href="${urlCreatePermanentRoom}"><spring:message code="views.wizard.createRoom.permanent"/></a></li>
            </ul>
        </div>
    </c:when>

    <%-- Create room capacity or set attributes to room of selected type --%>
    <c:when test="${navigation == 'WIZARD_CREATE_ROOM_ATTRIBUTES' || navigation == 'WIZARD_CREATE_PERMANENT_ROOM_CAPACITY'}">
        <c:set var="nextPageUrl">javascript: document.getElementById('reservationRequest').submit();</c:set>
        <c:set var="confirmUrl">${contextPath}<%= ClientWebUrl.WIZARD_CREATE_CONFIRM %></c:set>
        <h1>Create ${reservationRequest.specificationType}</h1>
        <app:reservationRequestForm confirmUrl="${confirmUrl}"
                                    permanentRooms="${permanentRooms}"/>
    </c:when>

    <%-- Set user roles for room --%>
    <c:when test="${navigation == 'WIZARD_CREATE_ROOM_ROLES'}">
        <c:choose>
            <c:when test="${userRole != null && userRole.id == null}">
                <h1>Create user role</h1>
                <app:userRoleForm confirmTitle="views.button.create"/>
            </c:when>
            <c:otherwise>
                <h1>Use roles</h1>
                <c:set var="nextPageUrl">${contextPath}<%= ClientWebUrl.WIZARD_CREATE_FINISH %></c:set>
                <c:set var="createRoleUrl">${contextPath}<%= ClientWebUrl.WIZARD_CREATE_ROOM_ROLE_CREATE %></c:set>
                <c:set var="deleteRoleUrl">${contextPath}<%= ClientWebUrl.WIZARD_CREATE_ROOM_ROLE_DELETE %></c:set>
                <app:userRoleList data="${reservationRequest.userRoles}"
                                  createUrl="${createRoleUrl}"
                                  deleteUrl="${deleteRoleUrl}"/>
            </c:otherwise>
        </c:choose>
    </c:when>

    <%-- Finish room or room capacity --%>
    <c:when test="${navigation == 'WIZARD_CREATE_ROOM_FINISH' || navigation == 'WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_FINISH'}">
        <h1>Finish ${reservationRequest.specificationType} ${reservationRequest.id}</h1>
    </c:when>

</c:choose>

<%-- Wizard navigation --%>
<div>
    <c:set var="primaryClass" value="btn-primary"/>

    <%-- Link to next page --%>
    <c:if test="${nextPageUrl == null && navigation.nextPage != null && navigation.nextPage.url != null && availablePages.contains(navigation.nextPage)}">
        <c:set var="nextPageUrl">${contextPath}${navigation.nextPage.url}</c:set>
    </c:if>
    <c:if test="${nextPageUrl != null}">
        <a class="btn ${primaryClass} pull-right" href="${nextPageUrl}">
            <spring:message code="views.button.continue"/>
        </a>
        <c:set var="primaryClass"></c:set>
    </c:if>

    <%-- Link to previous page --%>
    <c:if test="${navigation.previousPage != null}">
        <c:set var="previousPageUrl">${contextPath}${navigation.previousPage.url}</c:set>
        <a class=" btn ${primaryClass}" href="${previousPageUrl}">
            <spring:message code="views.button.back"/>
        </a>
    </c:if>
</div>