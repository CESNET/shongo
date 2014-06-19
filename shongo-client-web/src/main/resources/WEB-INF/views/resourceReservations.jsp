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

    window.formatId = function(id) {
        if (id == null) {
            return null;
        }
        var id = id.split(":");
        return id[2] + ":" + id[3];
    };

    window.updateTable = function() {
        var resourceId = $("#resourceId").val();
        var type = $("#type").val();
        var start = $("#start").val();
        var end = $("#end").val();
        var url = "<tag:url value="<%= ClientWebUrl.RESOURCE_RESERVATIONS_DATA %>"/>";
        url += "?start=" + start + "&end=" + end;
        if (resourceId != null && resourceId != "") {
            url += "&resource-id=" + resourceId;
        }
        if (type != null && type != "") {
            url += "&type=" + type;
        }
        $("#table").html("<div class='spinner'></div>");
        $.ajax({
            type: "GET",
            url: url,
            cache:false,
            dataType: "json",
            success: function(data) {
                var tableHtml = "<table class='table table-striped table-hover'>";
                tableHtml += "<thead>";
                tableHtml += "<tr>";
                tableHtml += "<th><spring:message code="views.reservation.id"/></th>";
                tableHtml += "<th><spring:message code="views.reservation.resource"/></th>";
                tableHtml += "<th><spring:message code="views.reservation.type"/></th>";
                tableHtml += "<th><spring:message code="views.reservation.slot"/></th>";
                tableHtml += "<th><spring:message code="views.reservation.allocated"/></th>";
                tableHtml += "</tr>";
                tableHtml += "</thead>";
                tableHtml += "<tbody>";
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
                    tableHtml += "<tr>";
                    tableHtml += "<td>" + formatId(reservation.id) +"</td>";
                    tableHtml += "<td>" + formatId(reservation.resourceId) +"</td>";
                    tableHtml += "<td>" + reservationType +"</td>";
                    tableHtml += "<td>" + moment(reservation.slotStart).format("YYYY-MM-DD HH:mm") + " - " + moment(reservation.slotEnd).format("YYYY-MM-DD HH:mm") + "</td>";
                    tableHtml += "<td>" + reservationAllocated +"</td>";
                    tableHtml += "</tr>";
                }
                if (data.length == 0) {
                    tableHtml += "<tr><td colspan='5' class='empty'><spring:message code="views.list.none"/></td></tr>";
                }
                tableHtml += "</tbody>";
                tableHtml += "</table>";
                $("#table").html(tableHtml);
            }
        });
    };

    $(document).ready(function(){
        $("#resourceId").select2({
            width: 300,
            escapeMarkup: function (markup) { return markup; },
            data: [
                {id: "", text: "<spring:message code="views.resource.all"/>"}
                <c:forEach items="${resources}" var="resource">
                ,{id: "${resource.key}", text: "${resource.value}"}
                </c:forEach>
            ]
        });
        $("#resourceId,#start,#end,#type").change(function () {
            updateTable();
        });
        var dateTime = moment();
        $("#start").val(dateTime.weekday(0).format("YYYY-MM-DD"));
        $("#end").val(dateTime.weekday(6).format("YYYY-MM-DD"));
        updateTable();
    });
</script>

<div ng-app="jsp:resourceReservations">
    <form class="form-inline">
        <label for="resourceId"><spring:message code="views.resource"/>:</label>
        <input id="resourceId"/>

        <spring:eval var="types" expression="T(cz.cesnet.shongo.controller.api.ReservationSummary$Type).values()"/>
        <label for="type"><spring:message code="views.reservation.type"/>:</label>
        <select id="type" class="form-control" tabindex="${tabIndex}">
            <option value=""><spring:message code="views.reservation.type.all"/></option>
            <c:forEach items="${types}" var="type">
                <option value="${type}"><spring:message code="views.reservation.type.${type}"/></option>
            </c:forEach>
        </select>

        &nbsp;
        <label for="start"><spring:message code="views.interval"/>:</label>
        <div class="input-group" style="display: inline-table;">
            <span class="input-group-addon">
                <spring:message code="views.interval.from"/>:
            </span>
            <input id="start" class="form-control form-picker" type="text" date-picker="true" readonly="true" style="width: 100px;"/>
        </div>
        <div class="input-group" style="display: inline-table">
            <span class="input-group-addon">
                <spring:message code="views.interval.to"/>:
            </span>
            <input id="end" class="form-control form-picker" type="text" date-picker="true" readonly="true" style="width: 100px;"/>
        </div>

    </form>

    <hr/>

    <div id="table">
    </div>

</div>


