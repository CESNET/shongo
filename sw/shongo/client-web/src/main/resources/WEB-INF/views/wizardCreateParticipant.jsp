<%--
  -- Wizard page for setting participant attributes.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<script type="text/javascript">
    angular.module('jsp:wizardCreateParticipant', ['ngTooltip']);
</script>

<div ng-app="jsp:wizardCreateParticipant">

    <h1>
        <c:choose>
            <c:when test="${empty participant.id}">
                <spring:message code="views.wizard.createParticipant.add"/>
            </c:when>
            <c:otherwise>
                <spring:message code="views.wizard.createParticipant.modify"/>
            </c:otherwise>
        </c:choose>
    </h1>

    <hr/>

    <tag:participantForm confirmTitle="views.button.add"/>

    <hr/>

</div>