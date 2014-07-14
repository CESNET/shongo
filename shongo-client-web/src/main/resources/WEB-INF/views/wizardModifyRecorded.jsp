<%--
  -- Wizard page for modifying whether room is recorded.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<c:set var="administrationMode" value="${sessionScope.SHONGO_USER.administrationMode}"/>
<c:set var="tabIndex" value="1"/>

<tag:url var="resourceListUrl" value="<%= ClientWebUrl.RESOURCE_LIST_DATA %>"/>

<script type="text/javascript">
    var module = angular.module('jsp:wizardModifyRecorded', ['ngApplication', 'ngTooltip']);

    /**
     * Get list of resources.
     *
     * @param capabilityClass
     * @param callback
     */
    window.getResources = function(capabilityClass, callback) {
        var technology = "${reservationRequest.technology}";
        $.ajax("${resourceListUrl}?capabilityClass=" + capabilityClass + "&technology=" + technology, {
            dataType: "json"
        }).done(function (data) {
            var resources = [{id: "", text: "<spring:message code="views.reservationRequest.specification.resourceId.none"/>"}];
            for (var index = 0; index < data.length; index++) {
                var resource = data[index];
                resources.push({
                    id: resource.id,
                    text: "<strong>" + resource.name + "</strong> (" + resource.id + ")"
                });
            }
            callback(resources);
        })
    };
</script>

<spring:message code="views.specificationType.for.${reservationRequest.specificationType}" var="specificationType"/>
<h1><spring:message code="views.wizard.modifyRecorded" arguments="${specificationType}"/></h1>

<hr/>

<div ng-app="jsp:wizardModifyRecorded">

<form:form class="form-horizontal"
           commandName="reservationRequest"
           method="post">

    <fieldset>

        <c:if test="${errors != null}">
            <div class="alert alert-danger"><spring:message code="views.wizard.error.failed"/></div>
        </c:if>
        <c:set var="roomRecordedError" value="${errors.hasFieldErrors('roomRecorded') ? errors.getFieldErrors('roomRecorded')[0].defaultMessage : null}"/>

        <div class="form-group">
            <form:label class="col-xs-2 control-label" path="id">
                <spring:message code="views.reservationRequest.identifier"/>:
            </form:label>
            <div class="col-xs-4">
                <form:input cssClass="form-control" path="id" readonly="true" tabindex="${tabIndex}"/>
            </div>
        </div>

        <div class="form-group" ng-hide="technology == 'ADOBE_CONNECT'">
            <form:label class="col-xs-2 control-label" path="roomRecorded">
                <spring:message code="views.reservationRequest.specification.roomRecorded" var="roomRecordedLabel"/>
                <tag:help label="${roomRecordedLabel}:"><spring:message code="views.reservationRequest.specification.roomRecordedHelp"/></tag:help>
            </form:label>
            <div class="col-xs-4">
                <div class="checkbox">
                    <form:checkbox path="roomRecorded" cssErrorClass="error" tabindex="${tabIndex}" disabled="true"/>
                </div>
            </div>
            <c:if test="${true || not empty roomRecordedError}">
                <div class="col-xs-offset-2 col-xs-10">
                     <span class="error">
                             ${roomRecordedError}
                     </span>
                </div>
            </c:if>
        </div>

        <c:if test="${administrationMode}">
            <script type="text/javascript">
                $(function(){
                    window.getResources("RecordingCapability", function(resources) {
                        $("#roomRecordingResourceId").select2({
                            data: resources,
                            escapeMarkup: function (markup) {
                                return markup;
                            },
                            initSelection: function(element, callback) {
                                var id = $(element).val();
                                for (var index = 0; index < resources.length; index++) {
                                    if (resources[index].id == id) {
                                        callback(resources[index]);
                                        return;
                                    }
                                }
                                // Id wasn't found and thus set default value
                                callback(resources[0]);
                                $("#roomRecordingResourceId").val(resources[0].id);
                            }
                        });
                    });
                });
            </script>
            <div class="form-group">
                <form:label class="col-xs-2 control-label" path="roomRecordingResourceId">
                    <spring:message code="views.reservationRequest.specification.roomRecordingResourceId"/>:
                </form:label>
                <div class="col-xs-4">
                    <form:input cssClass="form-control" cssErrorClass="form-control error" path="roomRecordingResourceId" tabindex="${tabIndex}"/>
                </div>
                <div class="col-xs-offset-2 col-xs-10">
                    <form:errors path="roomRecordingResourceId" cssClass="error"/>
                </div>
            </div>
        </c:if>

    </fieldset>

</form:form>

</div>

<hr/>