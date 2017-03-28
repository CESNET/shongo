<%@ page import="cz.cesnet.shongo.controller.FilterType" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>


<script type="text/javascript">
    var module = angular.module('jsp:capabilities', []);
    module.controller("CapabilitiesController", ['$scope', '$log', function ($scope, $log) {



        $(".submitCapability").bind("click",function() {
            $('#example').submit();
        });




    }])

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

    <button class="btn btn-primary" data-toggle="modal" data-target="#largePopup">Open</button>

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
                            <option selected="selected">ValueProviderCapability</option>
                            <option>RoomProviderCapability</option>
                            <option>StreamingCapability</option>
                            <option>TerminalCapability</option>
                            <option>AliasProviderCapability</option>
                            <option>RecordingCapability</option>
                        </select>
                    </div>
                    <form:form class="capabilityForm form-horizontal"
                               modelAttribute="terminalcapability"
                               method="post"
                               ng-show="addCapabilityType=='TerminalCapability'">
                        terminal capa
                    </form:form>
                    <form:form class="capabilityForm form-horizontal"
                               modelAttribute="streamingcapability"
                               method="post"
                               ng-show="addCapabilityType=='StreamingCapability'">
                        streaming capa
                    </form:form>
                    <form:form class="capabilityForm form-horizontal"
                               modelAttribute="recordingcapability"
                               method="post"
                               ng-show="addCapabilityType=='RecordingCapability'">
                        <form:label class="col-xs-3 control-label" path="id">
                            License count
                        </form:label>
                        <div class="col-xs-4">
                            <form:input cssClass="form-control" path="licenseCount" />
                        </div>
                    </form:form>

                </div>
                <div class="modal-footer">
                    <a class="btn btn-default" href="javascript: $('.capabilityForm').submit();">

                    </a>
                    <button type="button" class="btn btn-success">Add</button>
                    <button type="button" class="btn btn-tertiary" data-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    </div>


    <button ng-click="dialogue()">Add capability</button>
</div>