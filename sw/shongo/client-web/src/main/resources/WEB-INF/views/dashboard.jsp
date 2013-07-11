<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%--
  -- Dashboard page.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="app" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="urlWizard">${contextPath}<%= ClientWebUrl.WIZARD %></c:set>
<c:set var="urlAdvanced">${contextPath}<%= ClientWebUrl.RESERVATION_REQUEST_LIST%></c:set>

<ul>
<li><a href="${urlWizard}">Start wizard for beginners</a></li>
<li><a href="${urlAdvanced}">Start advanced user interface</a></li>
</ul>

<h2>List of rooms:</h2>
<table class="table table-striped table-hover">
    <thead>
    <tr>
        <th>Room name</th>
        <th>Technology</th>
        <th>Time slot</th>
        <th>State</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td>{{room.name}}</td>
        <td>{{room.technology}}</td>
        <td>{{room.slot}}</td>
        <td>{{room.state}}</td>
    </tr>
    <tr ng-hide="items.length">
        <td colspan="4" class="empty">- - - None - - -</td>
    </tr>
    </tbody>
</table>
