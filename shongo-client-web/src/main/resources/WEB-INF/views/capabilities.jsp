<%@ page import="cz.cesnet.shongo.controller.FilterType" %>
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
                    <h4 class="modal-title" id="myModalLabel">Popup</h4>
                </div>
                <div class="modal-body">
                    <p>Select which type of capability you want to create.</p>
                    <div>
                        <select class="selectpicker" ng-model="addCapabilityType">
                            <option value="valueProviderCapabilityForm" selected="selected">ValueProviderCapability</option>
                            <option value="roomProviderCapabilityForm">RoomProviderCapability</option>
                            <option value="streamingCapabilityForm">StreamingCapability</option>
                            <option value="terminalCapabilityForm">TerminalCapability</option>
                            <option value="aliaspPoviderCapabilityForm">AliasProviderCapability</option>
                            <option value="recordingCapabilityForm">RecordingCapability</option>
                        </select>
                    </div>
                    <form:form
                                id="terminalCapabilityForm"
                                class="form-horizontal"
                               modelAttribute="terminalcapability"
                               method="post"
                               action="/resource/${resourceId}/capabilities/terminal"
                               ng-show="addCapabilityType=='terminalCapabilityForm'">
                        terminal capa
                    </form:form>
                    <form:form
                                id="streamingCapabilityForm"
                            class="form-horizontal"
                               modelAttribute="streamingcapability"
                               method="post"
                               action="/resource/${resourceId}/capabilities/streaming"
                               ng-show="addCapabilityType=='streamingCapabilityForm'">
                        streaming capa
                    </form:form>
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
                    <form:form
                            id="valueProviderCapabilityForm"
                            class="form-horizontal"
                            modelAttribute="valueprovidercapability"
                            method="post"
                            action="/resource/${resourceId}/capabilities/valueProvider"
                            ng-show="addCapabilityType=='valueProviderCapabilityForm'">
                        <h3>Value Provider Capability</h3>
s
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