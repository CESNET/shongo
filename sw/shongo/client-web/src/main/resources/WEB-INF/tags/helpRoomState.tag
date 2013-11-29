<%--
  -- Help for room state.
  --%>
<%@ tag body-content="empty" %>
<%----%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>
<%----%>
<%@attribute name="roomType" required="false" type="java.lang.String" %><%--
<%----%><%--
--%><c:if test="${roomType != null}"><%--
    --%><c:set var="roomType">${roomType}.</c:set><%--
--%></c:if>
<%----%><%--
--%><tag:help position="bottom-left" selectable="true"><%--
    --%><div class="room-state"><%--
        --%><table class="table table-striped"><%--
            --%><c:forEach items="NOT_STARTED,STARTED,STARTED_NOT_AVAILABLE,STARTED_AVAILABLE,STOPPED,FAILED" var="state"><%--
                --%><tr><%--
                    --%><td class="${state}"><spring:message code="views.executable.roomState.${roomType}${state}"/></td><%--
                    --%><td><spring:message code="views.executable.roomStateHelp.${roomType}${state}"/></td><%--
                --%></tr><%--
            --%></c:forEach><%--
        --%></table><%--
    --%></div><%--
--%></tag:help>