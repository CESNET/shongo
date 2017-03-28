<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:if test="${valueProvider['class'].simpleName == 'DeviceResource'}">
    <c:out value="${valueProvider.id}"/>
</c:if>
<c:if test="${valueProvider['class'].simpleName == 'String'}">
    <c:out value="${valueProvider}"/>
</c:if>
<c:if test="${valueProvider['class'].simpleName == 'Pattern'}">
    <dl class="dl-horizontal">
        <dt>Value provider:</dt>
        <dd>PATTERN VALUE PROVIDER</dd>
        <dt>Patterns:</dt>
        <c:forEach items="${valueProvider.patterns}" var="pattern">
            <dd><c:out value="${pattern}"/></dd>
        </c:forEach>

        <dt>Allow any requested value:</dt>
        <dd><c:out value="${capability.valueProvider.allowAnyRequestedValue == true}"/></dd>
    </dl>
</c:if>
<c:if test="${valueProvider['class'].simpleName == 'Filtered'}">
    <dl class="dl-horizontal">
        <dt>Value provider:</dt>
        <dd>FILTERED VALUE PROVIDER</dd>
        <dt>Filter type:</dt>
        <c:if test="${valueProvider.filterType == 'CONVERT_TO_URL'}">
            <dd>Convert to url</dd>
        </c:if>
        <dt>Value provider:</dt>
        <c:set var="valueProvider" scope="request" value="${valueProvider.valueProvider}"/>
        <div class="fc-clear"></div>
        <dd><div><c:import url="/WEB-INF/views/valueProviderCapability.jsp"/></div></dd>

    </dl>
</c:if>