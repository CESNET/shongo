<%--
  -- Page for time zone detection in javascript.
  -- It redirects to "/" with "time-zone-offset" parameter set.
  --%>
<%@ page language="java" pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<html>
<body>
<script type="text/javascript">
    var date = new Date();
    var timeZoneOffset = (date.getTimezoneOffset() * 60) * (-1);
    window.location.href = "${contextPath}/?time-zone-offset=" + timeZoneOffset;
</script>
</body>
</html>
