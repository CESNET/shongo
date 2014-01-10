<%--
  -- Page for creation/modification of a reservation request.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<tiles:importAttribute />
<c:set var="cancelUrl"><%= ClientWebUrl.RESERVATION_REQUEST_LIST %></c:set>
<tag:url var="cancelUrl" value="${requestScope.backUrl.getUrl(backUrl)}"/>

<script type="text/javascript">
    var module = angular.module('jsp:reservationRequestUpdate', ['ngApplication', 'tag:reservationRequestForm']);

    /**
     * Store reservation request form and redirect to given {@code url}.
     *
     * @param url
     */
    window.redirect = function(url) {
        $.post('<%= ClientWebUrl.RESERVATION_REQUEST_UPDATE %>',$('#reservationRequest').serialize(), function(){
            window.location.href = url;
        });
    };
</script>

<div ng-app="jsp:reservationRequestUpdate">

    <h1>
        <spring:message code="${title}"/>&nbsp;<%--
        --%><spring:message code="views.reservationRequestUpdate.type.${reservationRequest.specificationType}"/>
    </h1>

    <tag:reservationRequestForm confirmTitle="${confirmTitle}" cancelUrl="${cancelUrl}"
                                permanentRooms="${permanentRooms}">

        <div ng-show="$child.technology == 'ADOBE_CONNECT'">

            <%-- Participants --%>
            <h2><spring:message code="views.reservationRequest.participants"/></h2>
            <tag:url var="participantBackUrl" value="${requestUrl}">
                <tag:param name="reuse" value="true"/>
            </tag:url>
            <tag:url var="participantCreateUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_PARTICIPANT_CREATE %>">
                <tag:param name="back-url" value="${participantBackUrl}"/>
            </tag:url>
            <tag:url var="participantModifyUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_PARTICIPANT_MODIFY %>">
                <tag:param name="back-url" value="${participantBackUrl}"/>
            </tag:url>
            <tag:url var="participantDeleteUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_PARTICIPANT_DELETE %>">
                <tag:param name="back-url" value="${participantBackUrl}"/>
            </tag:url>
            <tag:participantList data="${reservationRequest.roomParticipants}"
                                 createUrl="javascript: redirect('${participantCreateUrl}')"
                                 modifyUrl="javascript: redirect('${participantModifyUrl}')"
                                 deleteUrl="javascript: redirect('${participantDeleteUrl}')"/>

        </div>

    </tag:reservationRequestForm>

</div>
