<%--
  -- Help for reservation request state.
  --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%----%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %><%--
--%><tag:help position="bottom-left"><%--
    --%><div class="reservation-request-state"><%--
        --%><table><%--
            --%><c:forEach items="ALLOCATED,ALLOCATED_STARTED,ALLOCATED_STARTED_NOT_AVAILABLE,ALLOCATED_STARTED_AVAILABLE,ALLOCATED_FINISHED,FAILED,MODIFICATION_FAILED" var="state"><%--
                --%><tr><%--
                    --%><td class="${state}"><spring:message code="views.reservationRequest.state.${state}"/></td><%--
                    --%><td><spring:message code="help.reservationRequest.state.${state}"/></td><%--
                --%></tr><%--
            --%></c:forEach><%--
        --%></table><%--
    --%></div><%--
--%></tag:help>