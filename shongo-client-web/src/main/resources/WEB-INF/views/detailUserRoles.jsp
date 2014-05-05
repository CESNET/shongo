<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<h2><spring:message code="views.userRoleList.title"/></h2>

<tag:url var="userRoleDataUrl" value="<%= ClientWebUrl.DETAIL_USER_ROLES_DATA %>">
    <tag:param name="objectId" value=":id"/>
</tag:url>
<tag:url var="userRoleCreateUrl" value="<%= ClientWebUrl.DETAIL_USER_ROLE_CREATE %>">
    <tag:param name="objectId" value="${objectId}"/>
    <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
</tag:url>
<tag:url var="userRoleDeleteUrl" value="<%= ClientWebUrl.DETAIL_USER_ROLE_DELETE %>">
    <tag:param name="objectId" value="${objectId}"/>
    <tag:param name="back-url" value="{{requestUrl}}" escape="false"/>
</tag:url>
<tag:userRoleList dataUrl="${userRoleDataUrl}" dataUrlParameters="id: '${reservationRequestId}'"
                  createUrl="${userRoleCreateUrl}" deleteUrl="${userRoleDeleteUrl}">

    <spring:message code="views.wizard.room.roles.description"/>
        <span ng-show="reservationRequest.technology == 'ADOBE_CONNECT'">
            <br/>
            <spring:message code="views.wizard.room.roles.description.participants"/>
        </span>

</tag:userRoleList>