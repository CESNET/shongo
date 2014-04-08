<%--
  -- Page displaying resource reservations.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="tabIndex" value="1"/>
<script type="text/javascript">
    var module = angular.module('jsp:resourceReservations', ['ngApplication', 'ngDateTime', 'ngTooltip', 'ngCookies', 'ngSanitize']);

    function update()
    {
        var resourceId = $("#resourceId").val();
        var intervalFrom = $("#intervalFrom").val();
        var intervalTo = $("#intervalTo").val();
        var url = "<tag:url value="<%= ClientWebUrl.RESOURCE_RESERVATIONS_TABLE %>"/>";
        console.debug("update", resourceId, intervalFrom, intervalTo);
        $("#content").html("<div class='spinner'></div>");
        $.ajax({
            type: "GET",
            url: url,
            cache:false,
            success: function(result) {
                $("#content").html(result);
            }
        });

    }

    $(function(){
        $("#resourceId").select2({
            width: 300,
            escapeMarkup: function (markup) { return markup; },
            data: [
                {id: "", text: "All resources"}
                <c:forEach items="${resources}" var="resource">
                    ,{id: "${resource.key}", text: "${resource.value}"}
                </c:forEach>
            ]
        });
        $("#resourceId,#intervalFrom,#intervalTo").change(function () {
            update();
        });
        var dateTime = moment();
        $("#intervalFrom").val(dateTime.weekday(0).format("YYYY-MM-DD"));
        $("#intervalTo").val(dateTime.weekday(6).format("YYYY-MM-DD"));
        update();
    });
</script>

<div ng-app="jsp:resourceReservations">
    <form class="form-inline">
        <label for="resourceId">Resource:</label>
        <input id="resourceId"/>

        &nbsp;
        <label for="intervalFrom">Interval:</label>
        <div class="input-prepend">
            <span class="add-on">
                From:
            </span>
            <input id="intervalFrom" class="form-control" type="text" date-picker="true" readonly="true" style="width: 100px; background-color: white;"/>
        </div>
        <div class="input-prepend">
            <span class="add-on">
                To:
            </span>
            <input id="intervalTo" class="form-control" type="text" date-picker="true" readonly="true" style="width: 100px; background-color: white;"/>
        </div>

    </form>

    <div id="content" class="center">
    </div>


</div>


