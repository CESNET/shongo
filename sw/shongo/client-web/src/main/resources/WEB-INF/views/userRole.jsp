<%--
  -- Page for creation of a user role for a reservation request.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ page import="cz.cesnet.shongo.controller.ObjectType" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="objectType"><%= ObjectType.RESERVATION_REQUEST %></c:set>

<script type="text/javascript">
    var module = angular.module('jsp:userRole', ['ngApplication', 'tag:userRoleForm']);
</script>

<div ng-app="jsp:userRole">

    <tag:userRoleForm objectType="${objectType}" confirmTitle="views.button.add"/>

</div>
