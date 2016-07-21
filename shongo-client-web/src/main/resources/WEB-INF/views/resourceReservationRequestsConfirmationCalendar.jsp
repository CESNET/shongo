<%--
  -- Page displaying resource reservation requests requiring confirmation (calendar view). Shows only resources owned by current user.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<tag:url var="reservationRequestConfirmationDataUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_CONFIRMATION_DATA %>">
  <tag:param name="count" value="10"/>
</tag:url>
<tag:url var="reservationRequestConfirmUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_CONFIRM %>">
</tag:url>
<tag:url var="reservationRequestDenyUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DENY %>">
</tag:url>

<script type="text/javascript">
  refreshCalendarAfterConfirm = function() {
    var calendar = $('#resourceReservationRequestsConfirmationCalendar');
    calendar.fullCalendar('refetchEvents');
    calendar.fullCalendar('rerenderEvents');
    $(".qtip").remove();
  }

  updateRequestConfirmation = function (url) {
    $.ajax({
      url: url,
      mimeType:"text/html",
      success: function(){
        refreshCalendarAfterConfirm();
      },
      error: function(){
        refreshCalendarAfterConfirm();
//        $application.handleAjaxFailure;
      }
    });
  };

  var module = angular.module('jsp:resourceReservationRequestsConfirmationCalendar', ['ngApplication', 'ui.calendar', 'ngDateTime', 'ngPagination', 'ngTooltip', 'ngCookies', 'ngSanitize', 'ui.select2']);
  module.controller("ResourceReservationRequestConfirmationCalendarController", function($scope, $compile ,$application ,uiCalendarConfig) {
    var date = new Date();
    var d = date.getDate();
    var m = date.getMonth();
    var y = date.getFullYear();

    // default values
    $scope.showExistingReservations = true;

    $scope.resourceIdOptions = {
      escapeMarkup: function (markup) {
        return markup;
      },
      data: [
        <c:forEach items="${resources}" var="resource">
        {id: "${resource.key}", text: "${resource.value}"},
        </c:forEach>
      ]
    };

    $scope.reservationsFilter = {
      resourceId: $scope.resourceIdOptions.data[0].id
    };

    // Ajax call for reservation request for confirmation
    $scope.eventsF = function(start, end, timezone, callback) {
      <c:choose>
        <c:when test="${resourceId != null}">
          var resourceId = '${resourceId}';
        </c:when>
        <c:otherwise>
          var resourceId = $scope.resourceIdOptions.data[0].id;
        </c:otherwise>
      </c:choose>

      if (typeof $scope.reservationsFilter.resourceId == 'object') {
        resourceId = $scope.reservationsFilter.resourceId.id;
      }

      $.ajax("${reservationRequestConfirmationDataUrl}&interval-from="+start.format()+"&interval-to="+end.format()+"&resource-id="+resourceId+"&showExisting="+$scope.showExistingReservations, {
        dataType: "json"
      }).done(function (data) {
        var events = [];
        data.items.forEach(function(event) {
          var descriptionTitle = "<spring:message code="views.room.description"/>";
          var none = "<spring:message code="views.reservationRequest.description.none"/>";
          events.push({
            id: event.id,
            title: event.description,
            description: event.description ? event.description : none,
            bookedBy: event.user,
            ownersEmail: event.userEmail,
            start: event.start,
            end: event.end,
            isReservaion: event.reservation
          });
        });
        callback(events);
      }).fail($application.handleAjaxFailure);
    };

    $scope.eventRender = function( event, element, view ) {
      var descriptionTitle = "<spring:message code="views.room.description"/>";
      var requestedByTitle = "<spring:message code="views.resourceReservationRequests.confirmation.requestedBy"/>";
      var requestedBy = event.bookedBy + " (<a href=\"mailto:" + event.ownersEmail + "\">" + event.ownersEmail + "</a>)";
      // event color reservation/request

      if (!event.isReservaion) {
        // Render qTip for confirmation
        element.css('border-color', 'orange');
        element.css('background-color', 'orange');

        element.qtip({
          content:
          "<center>" +
          "<a class=\"btn-primary btn-xs\" href=\"\" onclick=\"updateRequestConfirmation('${reservationRequestConfirmUrl}?reservationRequestId=" + event.id + "');return false;\"><spring:message code="views.list.action.confirm.title"/></a>" +
          "&nbsp;" +
          "<a class=\"btn-danger btn-xs\" href=\"\" onclick=\"updateRequestConfirmation('${reservationRequestDenyUrl}?reservationRequestId=" + event.id + "');return false;\"><spring:message code="views.list.action.deny.title"/></a>" +
          "</center>" +
          "<strong>" + descriptionTitle + ":</strong><br /><span onclick=''>" + event.description + "</span>" +
          "<br />" +
          "<strong>" + requestedByTitle + ":</strong><br /><span>" + requestedBy + "</span>",
          position: {
            my: 'left top',
            at: 'top right'
          },
          show: {
            solo: true
          },
          hide: {
            fixed: true,
            delay: 600
          },
          style: {
            classes: 'qtip-app',
          }
        });
      }
      // Render information about existing reservation
      else {
        var descriptionTitle = "<spring:message code="views.room.description"/>";
        var bookedByTitle = "<spring:message code="views.room.bookedBy"/>";
        var bookedBy = event.bookedBy ? (event.bookedBy + " (<a href=\"mailto:" + event.ownersEmail + "\">" + event.ownersEmail + "</a>)") : event.foreignDomain;
        element.qtip({
          content: "<strong>" + descriptionTitle + ":</strong><br /><span>" + event.description + "</span><br />" +
          "<strong>" + bookedByTitle + ":</strong><br /><span>" + bookedBy + "</span>",
          position: {
            my: 'left top',
            at: 'top right'
          },
          show: {
            solo: true
          },
          hide: {
            fixed: true,
            delay: 600
          },
          style: {
            classes: 'qtip-app'
          }
        });
      }
    };


    $scope.uiConfig = {
      calendar:{
        lang: '${requestContext.locale.language}',
        <c:choose>
          <c:when test="${date != null}">
            defaultDate: '${date}',
            defaultView: 'agendaWeek',
          </c:when>
          <c:otherwise>
            defaultView: 'agendaWeek',
          </c:otherwise>
        </c:choose>
        nowIndicator: true,
//        height: 'auto',
        editable: false,
        lazyFetching: false,
        header:{
          left: 'title',
          center: '',
          right: 'prev,today,next agendaDay,agendaWeek,month'
        },
        loading: function (bool) {
          if (bool) {
            $('#directives-calendar').fadeTo(100,0.5);
            $('#loadingImg').fadeTo(100,1);
            $('#loadingImg').show();
          } else {
            $('#loadingImg').fadeOut();
            $('#loadingImg').hide();
            $('#directives-calendar').fadeTo(100,1);
          }
        },
        eventRender: $scope.eventRender
      }
    };

    $scope.eventSources = [$scope.eventsF];

    $scope.$watch('reservationsFilter.resourceId', function(newResourceId, oldResourceId, scope) {
      if (typeof newResourceId == "object") {
        $scope.refreshCalendar();
//        if ($scope.resourceUriKeys.data[newResourceId.id]) {
//          var resourceCalendarUrl = calendarUrlBase + $scope.resourceUriKeys.data[newResourceId.id]
//          $('[qtip-init]').show();
//          $('[qtip-init]').each(function() {
//            $(this).qtip('option', 'content.text', exportCalendarMessage + "<input id='input' onClick='this.setSelectionRange(0, this.value.length)' readonly value='" + resourceCalendarUrl + "' />");
//          });
//        }
//        else {
//          $('[qtip-init]').hide();
//        }
      }
    });

    $scope.$watch('showExistingReservations', function(newValue, oldValue, scope) {
      if (typeof newValue === "boolean") {
        $scope.refreshCalendar();
      }
    });

    $scope.refreshCalendar = function() {
      var calendar = uiCalendarConfig.calendars['resourceReservationRequestsConfirmationCalendar'];
      if (calendar) {
        calendar.fullCalendar('refetchEvents');
        calendar.fullCalendar('rerenderEvents');
      }
    }
//    $scope.$on("refresh-resourceReservationRequestsConfirmationCalendar", function() {
//      $scope.initCalendar();
////      if (!$scope.resourceUriKeys.data[$scope.resourceIdOptions.data[0].id]) {
////        $('[qtip-init]').hide();
////      }
//    });
  });
</script>
<div ng-app="jsp:resourceReservationRequestsConfirmationCalendar">
  <div ng-controller="ResourceReservationRequestConfirmationCalendarController">
    <button class="pull-right fa fa-refresh btn btn-default" ng-click="refreshCalendar()"></button>
    <div class="btn-group pull-right">
      <form class="form-inline">
        <label for="resourceId"><spring:message code="views.room"/>:</label>
        <input id="resourceId" ng-model="reservationsFilter.resourceId" ui-select2="resourceIdOptions" class="min-input"/>
      </form>

        <input id="showExistingReservations" type="checkbox" ng-model="showExistingReservations"/>
        <label for="showExistingReservations">
          <spring:message code="views.resourceReservationRequests.confirmation.showExistingReservations"/>
        </label>
    </div>

    <div class="alert alert-warning push-top short-line"><spring:message code="views.resourceReservationRequests.confirmation.description"/></div>

    <div id="directives-calendar" class="top-margin">
        <div class="alert-success calAlert" ng-show="alertMessage != undefined && alertMessage != ''">
          <h4>{{alertMessage}}</h4>
        </div>
        <div class="spinner centered-in-element" id="loadingImg"></div>
        <div class="top-margin" ng-model="eventSources" id="resourceReservationRequestsConfirmationCalendar" calendar="resourceReservationRequestsConfirmationCalendar"  ui-calendar="uiConfig.calendar"></div>
      </div>
  </div>
</div>