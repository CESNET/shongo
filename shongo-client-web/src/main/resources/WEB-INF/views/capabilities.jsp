<%@ page import="cz.cesnet.shongo.controller.FilterType" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>



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

