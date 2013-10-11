<%--
  -- Wizard page for setting participant attributes.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<script type="text/javascript">
    angular.module('jsp:wizardCreateParticipant', ['tag:participantForm']);
</script>

<div ng-app="jsp:wizardCreateParticipant">

    <c:choose>
        <c:when test="${empty participant.id}">
            <c:set var="title" value="views.wizard.createParticipants.add"/>
            <c:set var="confirmTitle" value="views.button.add"/>
        </c:when>
        <c:otherwise>
            <c:set var="title" value="views.wizard.createParticipants.modify"/>
            <c:set var="confirmTitle" value="views.button.modify"/>
        </c:otherwise>
    </c:choose>

    <h1><spring:message code="${title}"/></h1>

    <hr/>

    <tag:participantForm confirmTitle="${confirmTitle}"/>

    <hr/>

</div>