<%--
  -- Page for configuration of user roles.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<script type="text/javascript">
    angular.module('jsp:userRoleList', ['ngTooltip', 'ngPagination']);
</script>

<h1>
    <spring:message code="views.userRoleList.heading"/>&nbsp;${headingFor}
</h1>

<div ng-app="jsp:userRoleList" class="table-actions-left">

    <h2><spring:message code="views.reservationRequest.userRoles"/></h2>
    <tag:url var="aclUrl" value="<%= ClientWebUrl.USER_ROLE_LIST_DATA %>">
        <tag:param name="objectId" value=":id"/>
    </tag:url>
    <tag:url var="aclCreateUrl" value="<%= ClientWebUrl.USER_ROLE_CREATE %>">
        <tag:param name="objectId" value="${objectId}"/>
    </tag:url>
    <tag:url var="participantDeleteUrl" value="<%= ClientWebUrl.USER_ROLE_DELETE %>">
        <tag:param name="objectId" value="${objectId}"/>
    </tag:url>
    <tag:userRoleList dataUrl="${aclUrl}" dataUrlParameters="id: '${objectId}'"
                      createUrl="${aclCreateUrl}" deleteUrl="${participantDeleteUrl}"/>

</div>

<div class="table-actions pull-right">
    <tag:url var="backUrl" value="${requestScope.backUrl}"/>
    <a class="btn" href="${backUrl}"><spring:message code="views.button.back"/></a>
</div>