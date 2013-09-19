<%@ tag trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%----%>
<%@ attribute name="code" required="true" type="java.lang.String" %>
<%@ attribute name="url" required="true" type="java.lang.String" %>
<%@ attribute name="tabindex" required="true" type="java.lang.Integer" %>
<%----%>
<spring:message var="actionIcon" code="views.list.action.${code}.icon"/>
<spring:message var="actionTitle" code="views.list.action.${code}.title"/>
<a href="${url}" tabindex="4"><b class="${actionIcon}" title="${actionTitle}"></b></a>