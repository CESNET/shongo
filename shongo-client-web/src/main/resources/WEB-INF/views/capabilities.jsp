<%@ page import="cz.cesnet.shongo.controller.FilterType" %>
<%@ page import="cz.cesnet.shongo.AliasType" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>


<script type="text/javascript">
    var module = angular.module('jsp:capabilities', []);
    module.controller("CapabilitiesController", ['$scope', '$log', function ($scope, $log) {



    }])

    $("#submitCapability").bind("click",function() {
        $('#example').submit();
    });
</script>

<div ng-app="jsp:capabilities" ng-controller="CapabilitiesController">

<c:forEach items="${capabilities}" var="capability">
    <div class="bordered" style="float: left;
    width: 70%;">
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

            <dd><c:out value="${capability.licenseCount}"/></dd>
        </dl>
    </c:if>
    </div>
    <div class="fc-clear"></div>
</c:forEach>

    <hr/>

    <button class="btn btn-primary" data-toggle="modal" data-target="#largePopup">Add capability</button>

    <div class="modal fade" id="largePopup" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                    <h4 class="modal-title" id="myModalLabel">Add capability</h4>
                </div>
                <div class="modal-body">
                    <p>Select which type of capability you want to add.</p>
                    <div>
                        <select ng-model="addCapabilityType" class="selectpicker">
                            <option disabled selected value> -- select an option -- </option>
                            <option value="valueProviderCapabilityForm" selected>ValueProviderCapability</option>
                            <option value="roomProviderCapabilityForm">RoomProviderCapability</option>
                            <option value="streamingCapabilityForm">StreamingCapability</option>
                            <option value="terminalCapabilityForm">TerminalCapability</option>
                            <option value="aliasProviderCapabilityForm">AliasProviderCapability</option>
                            <option value="recordingCapabilityForm">RecordingCapability</option>
                        </select>
                    </div>
                    <%-- Room Provider Capability --%>
                    <form:form
                            id="roomProviderCapabilityForm"
                            class="form-horizontal"
                            modelAttribute="roomprovidercapability"
                            method="post"
                            action="/resource/${resourceId}/capabilities/roomProvider"
                            ng-show="addCapabilityType=='roomProviderCapabilityForm'">
                        License count:<input type="number" name="licenseCount">
                        AliasType :
                        <select name="requiredAliasTypes" multiple="true">
                        <option disabled selected value> -- select an option -- </option>
                            <c:forEach items="${aliasTypes}" var="aliasType">
                                <option >
                                    <%--<spring:message code="${aliasType.name}"/>--%>${aliasType}
                                </option>
                            </c:forEach>
                        </select>
                    </form:form>

                    <%-- Terminal Capability --%>
                    <form:form
                                id="terminalCapabilityForm"
                                class="form-horizontal"
                               modelAttribute="terminalcapability"
                               method="post"
                               action="/resource/${resourceId}/capabilities/terminal"
                               ng-show="addCapabilityType=='terminalCapabilityForm'">
                        <div ng-init="aliases = [[]];">

                            <div ng-repeat="alias in aliases">
                                Alias type {{$index+1}}:
                                <select name="alias[{{$index}}].type">
                                    <option disabled selected value> -- select an option -- </option>
                                    <c:forEach items="${aliasTypes}" var="aliasType">
                                        <option value="${aliasType}">
                                            <spring:message code="${aliasType.name}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                                Alias value {{$index+1}}: <input type="text" name="alias[{{$index}}].value">
                            </div>
                            <br/>
                            <a ng-click="aliases.push([])" class="btn btn-default"><i class="fa fa-plus" aria-hidden="true"></i>
                                Add alias</a>

                        </div>
                    </form:form>

                    <%-- Streaming Capability --%>
                    <form:form
                                id="streamingCapabilityForm"
                            class="form-horizontal"
                               modelAttribute="streamingcapability"
                               method="post"
                               action="/resource/${resourceId}/capabilities/streaming"
                               ng-show="addCapabilityType=='streamingCapabilityForm'">
                        <%-- empty form --%>
                    </form:form>

                    <%-- Recording Capability --%>
                    <form:form
                                id="recordingCapabilityForm"
                                class="form-horizontal"
                               modelAttribute="recordingcapability"
                               method="post"
                               action="/resource/${resourceId}/capabilities/recording"
                               ng-show="addCapabilityType=='recordingCapabilityForm'">
                        recording capa
                        <input type="number" name="licenseCount">
                    </form:form>

                    <%-- Value Provider Capability --%>
                    <div ng-show="addCapabilityType=='valueProviderCapabilityForm'">
                        <h3>Value Provider Capability</h3>
                        <form:form
                                id="valueProviderCapabilityForm"
                                class="form-horizontal"
                                modelAttribute="valueprovidercapability"
                                method="post"
                                action="/resource/${resourceId}/capabilities/valueProvider">

                            Select type:
                            <select name="valueProviderType" class="selectpicker" ng-model="addValueProviderType" >
                                <option disabled selected value> -- select an option -- </option>
                                <option value="pattern">Pattern</option>
                                <option value="filtered">Filtered</option>
                            </select>

                            <div ng-show="addValueProviderType=='pattern'">
                                <input type="checkbox" name="allowAnyRequestedValue"> Allow any requested value<br>


                                <div ng-init="patterns = [[]];">

                                    <div ng-repeat="pattern in patterns">
                                        Pattern {{$index+1}}: <input type="text" name="pattern[{{$index}}]">
                                    </div>
                                    <br/>
                                    <a ng-click="patterns.push([])" class="btn btn-default"><i class="fa fa-plus" aria-hidden="true"></i>
                                        Add pattern</a>

                                </div>
                            </div>

                            <div ng-show="addValueProviderType=='filtered'">
                                <input type="text" name="filteredResourceId">
                            </div>
                            <input type="hidden" name="ownerResourceId" value="${resourceId}">
                        </form:form>

                     </div>
                <div class="modal-footer">
                    <button type="submit" class="btn btn-success" form="{{addCapabilityType}}" value="Submit">Submit</button>
                    <button type="button" class="btn btn-tertiary" data-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    </div>
    </div>
</div>
