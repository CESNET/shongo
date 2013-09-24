<%--
  -- Help for reservation request state.
  --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<tag:help position="bottom-left">
    <div class="room-state">
        <table>
            <c:forEach items="NOT_STARTED,STARTED,STARTED_NOT_AVAILABLE,STARTED_AVAILABLE,STOPPED,FAILED" var="state">
                <tr>
                    <td class="${state}"><spring:message code="views.executable.roomState.${state}"/></td>
                    <td><spring:message code="help.executable.roomState.${state}"/></td>
                </tr>
            </c:forEach>
        </table>
    </div>
</tag:help>