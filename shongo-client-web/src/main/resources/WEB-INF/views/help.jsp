<%--
  -- Main welcome page.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<tag:url var="reportUrl" value="<%= cz.cesnet.shongo.client.web.ClientWebUrl.REPORT %>"/>

<script>
    $(function() {
        $('body').scrollspy({ target: '.jspHelp > .menu' });
    });
</script>

<div class="jspHelp">

    <div class="menu">
        <ul class="nav nav-list">
            <li class="active"><a href="#top"><i class="fa fa-chevron-right pull-right"></i><spring:message code="views.help.introduction"/></a></li>
            <li><a href="#loa"><i class="fa fa-chevron-right pull-right"></i><spring:message code="views.help.loa"/></a></li>
            <li><a href="#rooms"><i class="fa fa-chevron-right pull-right"></i><spring:message code="views.help.rooms"/></a></li>
            <li><a href="#resources"><i class="fa fa-chevron-right pull-right"></i><spring:message code="views.help.resources"/></a></li>
        </ul>
    </div>

    <div class="content">
        <h1><spring:message code="views.help.title"/></h1>

        <h2><spring:message code="views.help.introduction"/></h2>
        <tag:url var="loginUrl" value="<%= ClientWebUrl.LOGIN %>"/>
        <tag:url var="homeUrl" value="<%= ClientWebUrl.HOME %>"/>
        <tag:url var="reservationRequestUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST %>"/>
        <p><spring:message code="views.help.introduction.text1" arguments="${loginUrl}"/></p>
        <p><spring:message code="views.help.introduction.text"/></p>
        <p><spring:message code="views.help.introduction.text2" arguments="${homeUrl},${reservationRequestUrl}"/></p>

        <h2 id="loa"><spring:message code="views.help.loa"/></h2>
        <p><spring:message code="views.help.loa.text1"/></p>
        <ul>
            <li><spring:message code="views.help.loa.text2"/></li>
            <li><spring:message code="views.help.loa.text3" arguments="${reportUrl}"/></li>
        </ul>

        <h2 id="rooms"><spring:message code="views.help.rooms"/></h2>
        <p><spring:message code="views.help.rooms.text"/></p>
        <h3 id="adhoc-room">1. <spring:message code="views.reservationRequest.specification.ADHOC_ROOM"/></h3>
        <tag:helpRoomType roomType="ADHOC_ROOM"/>
        <h3 id="permanent-room">2. <spring:message code="views.reservationRequest.specification.PERMANENT_ROOM"/></h3>
        <tag:helpRoomType roomType="PERMANENT_ROOM"/>

        <h2 id="resources"><spring:message code="views.help.resources"/></h2>
        <p><spring:message code="views.help.resources.text" arguments="#adhoc-room,#permanent-room"/></p>
        <div class="room-examples">
            <p><spring:message code="views.help.resources.example"/></p>
            <div class="room-example">
                <div class="room-image">
                    <img src="/img/room/resource_licenses.png"/>
                </div>
                <p><spring:message code="views.help.resources.example.label"/></p>
            </div>
        </div>
    </div>

</div>
