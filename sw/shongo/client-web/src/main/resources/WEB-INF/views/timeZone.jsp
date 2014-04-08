<%--
  -- Page for time zone detection in javascript.
  -- It redirects to home url with "time-zone" parameter set.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>
<tag:url var="homeUrl" value="<%= ClientWebUrl.HOME %>"/>
<html>
<head>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/jstz.min.js"></script>
</head>
<body>
<script type="text/javascript">
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

    // Try to determine timezone or use only offset
    getTimeZone(function(timeZone){
        console.debug("Get timezone SUCCEEDED", timeZone);
        var date = new Date();
        var timeZoneOffset = (date.getTimezoneOffset() * 60) * (-1);
        setTimeout(function(){
            window.location.href = "${homeUrl}?time-zone=" + timeZone;
        }, 0);
    }, function(error){
        console.debug("Get timezone FAILED", error);
        var date = new Date();
        var timeZoneOffset = (date.getTimezoneOffset() * 60) * (-1);
        console.debug("Using time zone offset", timeZoneOffset);
        setTimeout(function(){
            window.location.href = "${homeUrl}?time-zone=" + timeZoneOffset;
        }, 0);
    });
</script>
</body>
</html>
