<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<h1>${title}</h1>
<spring:message code="views.index.welcome"/>
<hr>
<p><spring:message code="views.index.suggestions" arguments="vidcon@cesnet.cz"/></p>
<p style="text-align: center"><spring:message code="views.index.developmentTeam"/></p>