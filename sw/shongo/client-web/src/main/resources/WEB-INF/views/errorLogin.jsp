<%--
  -- Page which is displayed when error during authentication occurs.
  --%>
<%@ page language="java" pageEncoding="UTF-8" contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<spring:message code="views.errorLogin.title" var="title"/>

<html>
<head>
    <title>${title}</title>
</head>
<body>
<h1>${title}</h1>
${exception.message}
</body>
</html>
