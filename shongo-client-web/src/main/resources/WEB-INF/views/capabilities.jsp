<%@ page import="cz.cesnet.shongo.controller.FilterType" %>
<%@ page import="cz.cesnet.shongo.AliasType" %>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<tag:url var="resourceFinish" value="<%= ClientWebUrl.RESOURCE_CAPABILITIES_FINISH %>"/>

<c:set value="${resource.capabilities}" var="capabilities"/>
<script type="text/javascript">
    var module = angular.module('jsp:capabilities', []);
    module.controller("CapabilitiesController", ['$scope', '$log', function ($scope, $log) {



    }])

    $("#submitCapability").bind("click",function() {
        $('#example').submit();
    });

    $(document).ready(function() {
        $("#requiredAliasTypes").select2();
    });
</script>

<div ng-app="jsp:capabilities" ng-controller="CapabilitiesController">

<c:forEach items="${capabilities}" var="capability">
    <hr/>
    <div style="display: inline-block">

            <%-- Room Provider Capability --%>
        <c:if test="${capability['class'].simpleName == 'RoomProviderCapability'}">
            <h4>Room Provider Capability</h4>
            <dl class="dl-horizontal">
                <dt><spring:message code="views.capabilities.RoomProvider.licencesNumber"/>:</dt>

                <dd><c:out value="${capability.licenseCount}"/></dd>


                <dt><spring:message code="views.capabilities.RoomProvider.AliasType"/>:</dt>
                <c:forEach items="${capability.requiredAliasTypes}" var="aliasType">
                    <dd><spring:message code="${aliasType.name}"/></dd>
                </c:forEach>
            </dl>
        </c:if>

            <%-- Alias Provider Capability --%>
        <c:if test="${capability['class'].simpleName == 'AliasProviderCapability'}">
            <h4>Alias Provider Capability</h4>
            <dl class="dl-horizontal">
                <dt><spring:message code="views.capabilities.AliasProvider.aliases"/>:</dt>

                <c:forEach items="${capability.aliases}" var="alias">
                    <dd>ALIAS(type: <spring:message code="${alias.type.name}"/>, value: <c:out value="${alias.value}"/>)</dd>
                </c:forEach>

                <dt>Value provider:</dt>
                <c:set var="valueProvider" scope="request" value="${capability.valueProvider}"/>
                <c:if test="${!(valueProvider['class'].simpleName == 'String')}">
                    <div class="fc-clear"></div>
                </c:if>
                <dd><div><c:import url="/WEB-INF/views/valueProviderCapability.jsp"/></div></dd>

                <dt>Restricted to owner:</dt>
                <dd><c:out value="${alias.restrictedToOwner == true}"/></dd>
            </dl>

        </c:if>

            <%-- Value Provider Capability --%>
        <c:if test="${capability['class'].simpleName == 'ValueProviderCapability'}">

            <c:set var="valueProvider" scope="request" value="${capability.valueProvider}"/>
            <h4>Value Provider Capability</h4>
            <c:import url="/WEB-INF/views/valueProviderCapability.jsp"/>
        </c:if>

            <%-- Recording Capability --%>
        <c:if test="${capability['class'].simpleName == 'RecordingCapability'}">
            <h4>Recording Capability</h4>
            <dl class="dl-horizontal">
                <dt>License count:</dt>
                <c:set var="recordingLicenseCount" value="${capability.licenseCount}"/>
                <dd>
                    <c:choose>
                        <c:when test="${not empty recordingLicenseCount}">
                            <c:out value="${recordingLicenseCount}"/>
                        </c:when>
                        <c:otherwise>
                            <spring:message code="views.capabilities.licenseNumber.unlimited"/>
                        </c:otherwise>
                    </c:choose>
                </dd>
            </dl>
        </c:if>

            <%-- Value Provider Capability --%>
        <c:if test="${capability['class'].simpleName == 'TerminalCapability'}">
            <h4>Terminal Capability</h4>
            <dl class="dl-horizontal">
                <dt><spring:message code="views.capabilities.AliasProvider.aliases"/>:</dt>
                <c:forEach items="${capability.aliases}" var="alias">
                    <dd>ALIAS(type: <spring:message code="${alias.type.name}"/>, value: <c:out value="${alias.value}"/>)</dd>
                </c:forEach>
            </dl>


        </c:if>


    </div>
    <span class="pull-right" style="size: 20px;">
        <tag:url var="deleteCapability" value="<%= ClientWebUrl.RESOURCE_CAPABILITY_DELETE %>">
            <tag:param name="capabilityId" value="${capability.id}" escape="false"/>
        </tag:url>
            <tag:listAction  code="delete" url="${deleteCapability}"/>
        </span>
    <div class="fc-clear"></div>


</c:forEach>
    <c:if test="${fn:length(capabilities) eq 0}">
        <div colspan="6" class="full-width" style="padding:30px;">
            <div class="center-content"><spring:message code="views.list.none"/></div>
        </div>
    </c:if>

    <hr/>

    <button class="btn btn-default" data-toggle="modal" data-target="#largePopup"><spring:message code="views.button.add"/></button>



    <div class="modal fade" id="largePopup" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                    <h4 class="modal-title" id="myModalLabel"><spring:message code="views.capabilities.modal.title"/> </h4>
                </div>
                <div class="modal-body">
                    <p><spring:message code="views.capabilities.modal.selectionTitle"/></p>
                    <div>
                        <select ng-model="addCapabilityType" class="selectpicker form-control">
                            <option disabled selected value> -- <spring:message code="views.select.choose"/> -- </option>
                            <option value="valueProviderCapabilityForm" selected><spring:message code="views.capabilities.ValueProviderCapability"/></option>
                            <option value="aliasProviderCapabilityForm"><spring:message code="views.capabilities.AliasProviderCapability"/></option>
                            <c:if test="${isDeviceResource}">
                                <option value="roomProviderCapabilityForm"><spring:message code="views.capabilities.RoomProviderCapability"/></option>
                                <option value="terminalCapabilityForm"><spring:message code="views.capabilities.TerminalCapability"/></option>
                                <option value="recordingCapabilityForm"><spring:message code="views.capabilities.RecordingCapability"/></option>
                                <option value="streamingCapabilityForm"><spring:message code="views.capabilities.StreamingCapability"/></option>
                            </c:if>
                        </select>
                    </div>
                    <br/>
                    <%-- Room Provider Capability --%>
                    <form:form
                            id="roomProviderCapabilityForm"
                            class="form-horizontal"
                            modelAttribute="roomprovidercapability"
                            method="post"
                            action="/resource/capabilities/roomProvider"
                            ng-show="addCapabilityType=='roomProviderCapabilityForm'">
                        <h3><spring:message code="views.capabilities.RoomProviderCapability"/></h3>
                        <p><spring:message code="views.capabilities.RoomProvider.description"/></p>
                        <div class="form-group">
                            <label class="col-xs-3 control-label" for="licenseCount">
                                <spring:message code="views.capabilities.RoomProvider.licencesNumber"/>
                            </label>
                            <div class="col-xs-4">
                                <input class="form-control" type="text" id="licenseCount" name="licenseCount">
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="col-xs-3 control-label" for="requiredAliasTypes">
                                <spring:message code="views.capabilities.RoomProvider.AliasType"/>
                            </label>
                            <div class="col-xs-4">
                                <select class="form-control" style="padding-left:0;" id="requiredAliasTypes" name="requiredAliasTypes" multiple="true">
                                    <c:forEach items="${aliasTypes}" var="aliasType">
                                        <option value="${aliasType}">
                                            <spring:message code="${aliasType.name}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                    </form:form>

                    <%-- Terminal Capability --%>
                    <form:form
                                id="terminalCapabilityForm"
                                class="form-horizontal"
                               modelAttribute="terminalcapability"
                               method="post"
                               action="/resource/capabilities/terminal"
                               ng-show="addCapabilityType=='terminalCapabilityForm'">
                        <h3><spring:message code="views.capabilities.TerminalCapability"/></h3>
                        <p><spring:message code="views.capabilities.Terminal.description"/></p>
                        <div ng-init="aliases = [[]];">
                            <div class="form-group" ng-repeat="alias in aliases">
                                <label class="control-label col-xs-2" for="aliases[{{$index}}].type">
                                    <spring:message code="views.capabilities.AliasProvider.aliasType"/> {{$index}}:
                                </label>
                                <div class="col-xs-3">
                                    <select class="form-control" name="aliases[{{$index}}].type" id="aliases[{{$index}}].type">
                                        <option disabled selected value> -- select an option -- </option>
                                        <c:forEach items="${aliasTypes}" var="aliasType">
                                            <option value="${aliasType}">
                                                <spring:message code="${aliasType.name}"/>
                                            </option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <label class="control-label col-xs-2" for="aliases[{{$index}}].value">
                                    <spring:message code="views.capabilities.AliasProvider.aliasValue"/> {{$index}}:
                                </label>
                                <div class="col-xs-3">
                                    <input class="col-xs-3 form-control" type="text" name="aliases[{{$index}}].value" id="aliases[{{$index}}].value">
                                </div>
                            </div>
                            <br/>
                            <a ng-click="aliases.push([])" class="btn btn-default"><i class="fa fa-plus" aria-hidden="true"></i>
                                <spring:message code="views.capabilities.AliasProvider.addAlias"/></a>
                        </div>
                    </form:form>

                    <%-- Streaming Capability --%>
                    <form:form
                                id="streamingCapabilityForm"
                            class="form-horizontal"
                               modelAttribute="streamingcapability"
                               method="post"
                               action="/resource/capabilities/streaming"
                               ng-show="addCapabilityType=='streamingCapabilityForm'">
                        <h3><spring:message code="views.capabilities.StreamingCapability"/></h3>
                        <p><spring:message code="views.capabilities.Streaming.description"/></p>
                        <%-- empty form --%>
                    </form:form>

                    <%-- Alias Provider Capability --%>
                    <form:form
                            id="aliasProviderCapabilityForm"
                            class="form-horizontal"
                            modelAttribute="aliasprovidercapability"
                            method="post"
                            action="/resource/capabilities/aliasProvider"
                            ng-show="addCapabilityType=='aliasProviderCapabilityForm'">
                        <h3><spring:message code="views.capabilities.AliasProviderCapability"/></h3>
                        <p><spring:message code="views.capabilities.AliasProvider.description"/></p>
                        <h5>Value Provider</h5>
                            <div class="form-group">
                                <label class="control-label col-xs-2" for="valueProviderTypeForAlias">
                                    <spring:message code="views.capabilities.ValueProviderCapability.type"/>
                                </label>
                                <div class="col-xs-3">
                                    <select id="valueProviderTypeForAlias" name="valueProviderType" class="selectpicker form-control" ng-model="addValueProviderType" >
                                        <option disabled selected value> -- <spring:message code="views.select.choose"/> -- </option>
                                        <option value="resource"><spring:message code="views.capabilities.ValueProviderCapability.resource"/></option>
                                        <option value="pattern"><spring:message code="views.capabilities.ValueProviderCapability.pattern"/></option>
                                        <option value="filtered"><spring:message code="views.capabilities.ValueProviderCapability.filtered"/></option>
                                    </select>
                                </div>
                            </div>


                            <div ng-show="addValueProviderType=='resource'">
                                <div class="form-group">
                                    <label class="control-label col-xs-2" for="remoteResourceString">
                                        <spring:message code="views.capabilities.ValueProviderCapability.resourceId"/>
                                    </label>
                                    <div class="col-xs-4">
                                        <input class="form-control" id="remoteResourceString" type="text" name="remoteResourceString">
                                    </div>
                                </div>
                            </div>

                            <div ng-show="addValueProviderType=='pattern'">
                                <div class="form-group">
                                    <label class="col-xs-3 control-label" for="allowAnyRequestedValueForAP">
                                        <spring:message code="views.capabilities.ValueProviderCapability.anyValues"/>:
                                    </label>
                                    <div class="checkbox col-xs-4">
                                        <input id="allowAnyRequestedValueForAP" name="allowAnyRequestedValue" type="checkbox">
                                    </div>
                                </div>

                                <div ng-init="patterns = [[]];">

                                    <div class="form-group" ng-repeat="pattern in patterns">
                                        <label class="col-xs-3 control-label" for="patterns[{{$index}}]ForAP">
                                            <spring:message code="views.capabilities.ValueProviderCapability.pattern"/>: {{$index+1}}:
                                        </label>
                                        <div class="col-xs-4">
                                            <input id="patterns[{{$index}}]ForAP" class="form-control" type="text" name="patterns[{{$index}}]">
                                        </div>
                                    </div>

                                    <a ng-click="patterns.push([])" class="btn btn-default"><i class="fa fa-plus" aria-hidden="true"></i>
                                        <spring:message code="views.capabilities.ValueProviderCapability.addPattern"/>:</a>

                                </div>
                            </div>

                            <div ng-show="addValueProviderType=='filtered'">
                                <div class="form-group">
                                    <label class="col-xs-3 control-label" for="filteredResourceIdForAP">
                                        <spring:message code="views.capabilities.ValueProviderCapability.filteredResource"/>::
                                    </label>
                                    <div class="col-xs-4">
                                        <input id="filteredResourceIdForAP" class="form-control" type="text" name="filteredResourceId">
                                    </div>
                                </div>
                            </div>

                        <br/>
                        <h5><spring:message code="views.capabilities.AliasProvider.aliases"/>:</h5>
                            <div ng-init="aliases = [[]];" >
                                <div class="form-group" ng-repeat="alias in aliases">
                                    <label class="control-label col-xs-2" for="aliases[{{$index}}].typeForAP">
                                        <spring:message code="views.capabilities.AliasProvider.aliasType"/> {{$index}}:
                                    </label>
                                    <div class="col-xs-3">
                                        <select class="form-control" name="aliases[{{$index}}].type" id="aliases[{{$index}}].typeForAP">
                                            <option disabled selected value> -- <spring:message code="views.select.choose"/> -- </option>
                                            <c:forEach items="${aliasTypes}" var="aliasType">
                                                <option value="${aliasType}">
                                                    <spring:message code="${aliasType.name}"/>
                                                </option>
                                            </c:forEach>
                                        </select>
                                    </div>
                                    <label class="control-label col-xs-2" for="aliases[{{$index}}].valueForAP">
                                        <spring:message code="views.capabilities.AliasProvider.aliasTemplate"/> {{$index}}:
                                    </label>
                                    <div class="col-xs-3">
                                        <input class="col-xs-3 form-control" type="text" name="aliases[{{$index}}].value" id="aliases[{{$index}}].valueForAP">
                                    </div>
                                </div>
                                <br/>
                                <a ng-click="aliases.push([])" class="btn btn-default"><i class="fa fa-plus" aria-hidden="true"></i>
                                    <spring:message code="views.capabilities.AliasProvider.addAlias"/></a>
                            </div>
                        <br/>

                        <div class="form-group">
                            <label class="col-xs-4 control-label" for="restrictedToResource">
                                <spring:message code="views.capabilities.AliasProvider.capabilityRestricted"/>:
                            </label>
                            <div class="checkbox col-xs-4">
                                <input id="restrictedToResource" name="restrictedToResource" type="checkbox">
                            </div>
                        </div>
                        <br/>

                    </form:form>

                    <%-- Recording Capability --%>
                    <form:form
                                id="recordingCapabilityForm"
                                class="form-horizontal"
                               modelAttribute="recordingcapability"
                               method="post"
                               action="/resource/capabilities/recording"
                               ng-show="addCapabilityType=='recordingCapabilityForm'">
                    <h3><spring:message code="views.capabilities.RecordingCapability"/></h3>
                        <p><spring:message code="views.capabilities.Recording.description"/></p>
                        <div class="form-group">
                            <label class="col-xs-3 control-label" for="recordingLicenseCount">
                                <spring:message code="views.capabilities.Recording.licencesNumber"/>:
                            </label>
                            <div class="col-xs-4">
                                <input class="form-control" id="recordingLicenseCount" name="licenseCount" type="text">
                            </div>
                        </div>
                    </form:form>

                    <%-- Value Provider Capability --%>
                    <form:form
                            id="valueProviderCapabilityForm"
                            class="form-horizontal"
                            modelAttribute="valueprovidercapability"
                            method="post"
                            action="/resource/capabilities/valueProvider"
                            ng-show="addCapabilityType=='valueProviderCapabilityForm'">
                        <h3><spring:message code="views.capabilities.ValueProviderCapability"/></h3>
                        <p><spring:message code="views.capabilities.ValueProvider.description"/></p>
                        <div class="form-group">
                            <label class="col-xs-2 control-label" for="valueProviderType">
                                <spring:message code="views.capabilities.ValueProviderCapability.type"/>
                            </label>
                            <div class="col-xs-4">
                                <select id="valueProviderType" name="valueProviderType" class="form-control" ng-model="addValueProviderType" >
                                    <option disabled selected value> -- <spring:message code="views.select.choose"/> -- </option>
                                    <option value="pattern"><spring:message code="views.capabilities.ValueProviderCapability.pattern"/></option>
                                    <option value="filtered"><spring:message code="views.capabilities.ValueProviderCapability.filtered"/></option>
                                </select>
                            </div>
                        </div>
                        <hr/>

                        <div ng-show="addValueProviderType=='pattern'">

                            <div class="form-group">
                                <label class="col-xs-3 control-label" for="allowAnyRequestedValue">
                                    <spring:message code="views.capabilities.ValueProviderCapability.anyValues"/>:
                                </label>
                                <div class="checkbox col-xs-4">
                                    <input id="allowAnyRequestedValue" name="allowAnyRequestedValue" type="checkbox">
                                </div>
                            </div>

                            <div ng-init="patterns = [[]];">

                                <div class="form-group" ng-repeat="pattern in patterns">
                                    <label class="col-xs-3 control-label" for="patterns[{{$index}}]">
                                        <spring:message code="views.capabilities.ValueProviderCapability.pattern"/> {{$index+1}}:
                                    </label>
                                    <div class="col-xs-4">
                                        <input id="patterns[{{$index}}]" class="form-control" type="text" name="patterns[{{$index}}]">
                                    </div>
                                </div>

                               <a ng-click="patterns.push([])" class="btn btn-default"><i class="fa fa-plus" aria-hidden="true"></i>
                                   <spring:message code="views.capabilities.ValueProviderCapability.addPattern"/></a>

                            </div>

                        </div>

                        <div ng-show="addValueProviderType=='filtered'">
                            <div class="form-group">
                                <label class="col-xs-3 control-label" for="filteredResourceId">
                                    <spring:message code="views.capabilities.ValueProviderCapability.filteredResource"/>:
                                </label>
                                <div class="col-xs-4">
                                    <input id="filteredResourceId" class="form-control" type="text" name="filteredResourceId">
                                </div>
                            </div>
                        </div>

                    </form:form>

                    <div class="modal-footer">
                <button type="submit" class="btn btn-success" form="{{addCapabilityType}}" value="Submit"><spring:message code="views.button.create"/> </button>
                <button type="button" class="btn btn-tertiary" data-dismiss="modal"><spring:message code="views.button.cancel"/> </button>
            </div>
                </div>
            </div>
        </div>
    </div>
    <a class="btn btn-default pull-right" style="margin-left: 5px;" href="${resourceFinish}">
        <spring:message code="views.button.finish"/>
    </a>

</div>
