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

    function formatId(id)
    {
        if (id == null) {
            return null;
        }
        var id = id.split(":");
        return id[2] + ":" + id[3];
    }

    function update()
    {
        var resourceId = $("#resourceId").val();
        var type = $("#type").val();
        var intervalFrom = $("#intervalFrom").val();
        var intervalTo = $("#intervalTo").val();
        var url = "<tag:url value="<%= ClientWebUrl.RESOURCE_RESERVATIONS_DATA %>"/>";
        url += "?interval-from=" + intervalFrom + "&interval-to=" + intervalTo;
        if (resourceId != null && resourceId != "") {
            url += "&resource-id=" + resourceId;
        }
        if (type != null && type != "") {
            url += "&type=" + type;
        }
        $("#content").html("<div class='spinner'></div>");
        $.ajax({
            type: "GET",
            url: url,
            cache:false,
            dataType: "json",
            success: function(data) {
                var html = "<table class='table table-striped table-hover'>";
                html += "<thead>";
                html += "<tr>";
                html += "<th>id</th>";
                html += "<th>resource</th>";
                html += "<th>type</th>";
                html += "<th>slot</th>";
                html += "<th>allocated</th>";
                html += "</tr>";
                html += "</thead>";
                html += "<tbody>";
                for (var index = 0; index < data.length; index++) {
                    var reservation = data[index];
                    var reservationType = reservation.type;
                    var reservationAllocated = "";
                    switch (reservationType) {
                        case "VALUE": {
                            reservationAllocated = reservation.value;
                            break;
                        }
                        case "ALIAS": {
                            reservationAllocated = reservation.value;
                            if (reservation.aliasTypes != null) {
                                reservationAllocated += " (" + reservation.aliasTypes.join(", ") + ")";
                            }
                            break;
                        }
                        case "ROOM": {
                            reservationAllocated = reservation.roomLicenseCount + " " + (reservation.roomLicenseCount == 1 ? "license" : "licenses");
                            if (reservation.roomName != null) {
                                reservationAllocated = reservation.roomName + " (" + reservationAllocated + ")";
                            }
                            break;
                        }
                    }
                    html += "<tr>";
                    html += "<td>" + formatId(reservation.id) +"</td>";
                    html += "<td>" + formatId(reservation.resourceId) +"</td>";
                    html += "<td>" + reservationType +"</td>";
                    html += "<td>" + moment(reservation.slotStart).format("YYYY-MM-DD HH:mm") + " - " + moment(reservation.slotEnd).format("YYYY-MM-DD HH:mm") + "</td>";
                    html += "<td>" + reservationAllocated +"</td>";
                    html += "</tr>";
                }
                html += "</tbody>";
                html += "</table>";
                $("#content").html(html);
            }
        });
    }

    function initToolbar()
    {
        $("#resourceId").select2({
            width: 300,
            escapeMarkup: function (markup) { return markup; },
            data: [
                {id: "", text: "<spring:message code="views.resourceReservations.resource.all"/>"}
                <c:forEach items="${resources}" var="resource">
                    ,{id: "${resource.key}", text: "${resource.value}"}
                </c:forEach>
            ]
        });
        $("#resourceId,#intervalFrom,#intervalTo,#type").change(function () {
            update();
        });
        var dateTime = moment();
        $("#intervalFrom").val(dateTime.weekday(0).format("YYYY-MM-DD"));
        $("#intervalTo").val(dateTime.weekday(6).format("YYYY-MM-DD"));
        update();
    }

    $(document).ready(function(){
        initToolbar();
    });
</script>

<div ng-app="jsp:resourceReservations">
    <form class="form-inline">
        <label for="resourceId"><spring:message code="views.resourceReservations.resource"/>:</label>
        <input id="resourceId"/>

        <spring:eval var="types" expression="T(cz.cesnet.shongo.controller.api.ReservationSummary$Type).values()"/>
        <label for="resourceId"><spring:message code="views.resourceReservations.type"/>:</label>
        <select id="type" class="form-control" tabindex="${tabIndex}">
            <option value=""><spring:message code="views.resourceReservations.type.all"/></option>
            <c:forEach items="${types}" var="type">
                <option value="${type}"><spring:message code="views.resourceReservations.type.${type}"/></option>
            </c:forEach>
        </select>

        &nbsp;
        <label for="intervalFrom"><spring:message code="views.resourceReservations.interval"/>:</label>
        <div class="input-group" style="display: inline-table;">
            <span class="input-group-addon">
                From:
            </span>
            <input id="intervalFrom" class="form-control" type="text" date-picker="true" readonly="true" style="width: 100px; background-color: white;"/>
        </div>
        <div class="input-group" style="display: inline-table">
            <span class="input-group-addon">
                To:
            </span>
            <input id="intervalTo" class="form-control" type="text" date-picker="true" readonly="true" style="width: 100px; background-color: white;"/>
        </div>

    </form>

    <div class="calendar">
    </div>

    <hr/>

    <div id="content">
    </div>

</div>


