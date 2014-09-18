<%--
  -- Page for time zone detection in javascript.
  -- It redirects to home url with "time-zone" parameter set.
  --
  -- Timezone is detected by:
  -- 1) HTML 5 Geolocation and The Google Time Zone API (http://www.w3schools.com/html/html5_geolocation.asp, https://developers.google.com/maps/documentation/timezone/)
  -- 2) or Javascript JSTZ library (http://pellepim.bitbucket.org/jstz/)
  -- 3) or Javascript Date Time Zone Offset
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>
<tag:url var="homeUrl" value="<%= ClientWebUrl.HOME %>"/>
<html>
<head>
    <title>Shongo</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/style.css"/>
    <script type="text/javascript" src="${pageContext.request.contextPath}/js/browser-supported.js"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery.min.js"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/js/jstz.min.js"></script>
    <script type="text/javascript">
        /**
         * Determine current timezone offset
         *
         * @return current timezone offset
         */
        function getTimeZoneOffset()
        {
            console.debug("Getting time zone offset");
            var date = new Date();
            return (date.getTimezoneOffset() * 60) * (-1);
        }

        /**
         * Redirect to set timezone.
         */
        function redirectSetTimeZone(timeZone)
        {
            console.debug("Redirecting with time zone", timeZone);
            setTimeout(function(){
                window.location.href = "${homeUrl}?time-zone=" + timeZone;
            }, 0);
        }

    <security:authorize access="isAuthenticated()" var="isAuthenticated"/>

    <%-- User is not logged in and thus detect only time zone offset --%>
    <c:if test="${!isAuthenticated}">
        function init() {
            redirectSetTimeZone(getTimeZoneOffset());
        }
    </c:if>

    <%-- User is logged in and thus detect precisely the current time zone by GEO location, jstz library or offset --%>
    <c:if test="${isAuthenticated}">
        /**
         * Determine GEO location.
         *
         * @param successCallback to be called with {latitude: <latitude>, longitude: <longitude>}
         * @param errorCallback
         */
        function getGeoLocation(successCallback, errorCallback)
        {
            if (navigator != null && navigator.geolocation) {
                console.debug("Getting GEO location");
                navigator.geolocation.getCurrentPosition(function(position) {
                    console.debug("Getting GEO location SUCCEEDED.", position);
                    successCallback({latitude: position.coords.latitude, longitude: position.coords.longitude});
                }, function(error){
                    console.debug("Getting GEO location FAILED.", error);
                    errorCallback(error);
                });
            }
            else {
                errorCallback("Geolocation is not supported by this browser.");
            }
        }

        /**
         * Determine current timezone based on GEO location.
         *
         * @param successCallback to be called with <timeZoneId>
         * @param errorCallback
         */
        function getTimeZone(successCallback, errorCallback)
        {
            var errorHandler = function(error) {
                console.debug("Getting timezone based on GEO location FAILED", error);
                var timeZone = jstz.determine();
                if (timeZone != null) {
                    successCallback(timeZone.name());
                }
                else {
                    errorCallback("Cannot determine timezone");
                }
            };
            getGeoLocation(function(location){
                console.debug("Getting timezone based on GEO location", location);
                var url = "https://maps.googleapis.com/maps/api/timezone/json";
                url += "?location=" + location.latitude + "," + location.longitude;
                url += "&timestamp=" + (new Date().getTime() / 1000);
                url += "&sensor=false";
                console.debug("Google API URL:", url);
                $.ajax({
                    type: "GET",
                    url: url,
                    cache:false,
                    dataType:'json',
                    success: function(results) {
                        if (results.status == "OK") {
                            console.debug("Getting timezone based on GEO location SUCCEEDED", results);
                            successCallback(results.timeZoneId);
                        }
                        else {
                            errorHandler(results);
                        }
                    },
                    error: errorHandler,
                    fail: errorHandler
                });

            }, function(error){
                errorHandler(error);
            });
        }

        /**
         * Cancel getting timezone and set it fast by jstz or just offset.
         */
        function cancelTimeZone() {
            var timeZone = jstz.determine();
            if (timeZone != null) {
                redirectSetTimeZone(timeZone.name());
            }
            else {
                redirectSetTimeZone(getTimeZoneOffset());
            }
        }

        // Try to determine timezone or use only offset
        function init() {
            getTimeZone(function(timeZone){
                console.debug("Get timezone SUCCEEDED", timeZone);
                redirectSetTimeZone(timeZone);
            }, function(error){
                console.debug("Get timezone FAILED", error);
                console.debug("Using time zone offset", getTimeZoneOffset());
                setTimeout(function(){
                    redirectSetTimeZone(getTimeZoneOffset());
                }, 0);
            });
        }
    </c:if>

        if (!isBrowserSupported()) {
            document.write("<h1><spring:message code="browser.notSupported"/></h1><p><spring:message code="browser.supported"/></p>");
        }
        else {
            init();
        }

        var count=15;

        var counter=setInterval(delayedCancelTimeZone, 1000);

        function delayedCancelTimeZone()
        {
            count=count-1;
            if (count <= 0)
            {
                clearInterval(counter);
                cancelTimeZone();
                return;
            }

            document.getElementById("timer").innerHTML=count + " secs"; // watch for spelling
        }
    </script>
</head>

<%-- When user is logged in the detection can last longer and thus show message with cancel button, autocancel in 15 s --%>
<security:authorize access="isAuthenticated()">
<body onload="delayedCancelTimeZone();">
<div class="center-content">
    <div class="information-box">
        <span class="spinner"></span>
        <span><spring:message code="views.timeZone.detect"/></span>
        <a class="btn btn-primary" href="javascript: cancelTimeZone();">
            <spring:message code="views.button.cancel"/>
        </a>
        <br/>
        <span class="description">
            <spring:message code="views.timeZone.waiting"/>
        </span>
        <span id="timer" class="description"></span>
    </div>
</div>
</body>
</security:authorize>

</html>
