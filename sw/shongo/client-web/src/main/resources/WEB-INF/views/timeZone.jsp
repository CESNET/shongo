<%--
  -- Page for time zone detection in javascript.
  -- It redirects to "/" with "time-zone-offset" parameter set.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>
<tag:url var="homeUrl" value="<%= ClientWebUrl.HOME %>"/>
<html>
<body>
<script type="text/javascript">
    var date = new Date();
    var timeZoneOffset = (date.getTimezoneOffset() * 60) * (-1);
    window.location.href = "${homeUrl}?time-zone-offset=" + timeZoneOffset;
</script>
</body>
</html>
