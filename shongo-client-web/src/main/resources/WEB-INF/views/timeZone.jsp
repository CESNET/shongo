<%--
  -- Page for time zone detection in javascript.
  -- It redirects to home url with "time-zone" parameter set.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>
<tag:url var="homeUrl" value="<%= ClientWebUrl.HOME %>"/>
<html>
<head>
    <title>${name}</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/bootstrap.min.css"/>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/application.css">
    <script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery.min.js"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/js/jstz.min.js"></script>
    <script type="text/javascript">
        /**
         * Determine current timezone offset.
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

    <%-- User is not logged in and thus detect only time zone offset --%>
    <security:authorize access="!isAuthenticated()">
        redirectSetTimeZone(getTimeZoneOffset());
    </security:authorize>

    <%-- User is logged in and thus detect precisely the current time zone by GEO location, jstz library or offset --%>
    <security:authorize access="isAuthenticated()">
        /**
         * Determine GEO location.
         *
         * @param successCallback
         * @param errorCallback
         */
        function getGeoLocation(successCallback, errorCallback)
        {
            if (navigator.geolocation) {
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
         * Determine current timezone.
         *
         * @param successCallback
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
    </security:authorize>

    </script>
</head>

<%-- When user is logged in the detection can last longer and thus show message with cancel button --%>
<security:authorize access="isAuthenticated()">
<body>
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
    </div>
</div>
</body>
</security:authorize>

</html>
