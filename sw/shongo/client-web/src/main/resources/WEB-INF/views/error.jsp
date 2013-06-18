<%--
  -- Page which is displayed when uncaught exception is thrown or when other error happens.
  --%>
<%@ page language="java" pageEncoding="UTF-8" contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<spring:message code="views.error.title" var="title"/>

<html>
<head>
    <title>${title} ${code}</title>
</head>
<body>
<h1>${title} ${code}</h1>
Processing of request ${url}:
<br>
<pre style="margin: 10px;">
${message}
</pre>
</body>
</html>
