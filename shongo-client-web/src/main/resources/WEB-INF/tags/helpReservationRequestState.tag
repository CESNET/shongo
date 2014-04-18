<%--
  -- Help for reservation request state.
  --%>
<%@ tag body-content="empty" %>
<%----%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>
<%----%>
<%@attribute name="specificationType" required="false" type="java.lang.String" %><%--
<%----%><%--
--%><c:if test="${specificationType != null}"><%--
    --%><c:set var="specificationType">${specificationType}.</c:set><%--
--%></c:if>
<%----%><%--
--%><tag:help position="bottom-left" selectable="true"><%--
    --%><div class="reservation-request-state">${specificationType}<%--
        --%><table class="table table-striped"><%--
            --%><c:forEach items="ALLOCATED,ALLOCATED_STARTED,ALLOCATED_STARTED_NOT_AVAILABLE,ALLOCATED_STARTED_AVAILABLE,ALLOCATED_FINISHED,FAILED,MODIFICATION_FAILED" var="state"><%--
                --%><tr><%--
                    --%><td class="${state}"><spring:message code="views.reservationRequest.state.${specificationType}${state}"/></td><%--
                    --%><td><spring:message code="views.reservationRequest.stateHelp.${specificationType}${state}"/></td><%--
                --%></tr><%--
            --%></c:forEach><%--
        --%></table><%--
    --%></div><%--
--%></tag:help>