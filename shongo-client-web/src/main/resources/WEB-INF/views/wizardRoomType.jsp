<%--
  -- Wizard page for creating a new room.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<tag:url var="createAdhocRoomUrl" value="<%= ClientWebUrl.WIZARD_ROOM_ADHOC %>">
    <tag:param name="back-url" value="${requestScope.backUrl}"/>
</tag:url>
<tag:url var="createPermanentRoomUrl" value="<%= ClientWebUrl.WIZARD_ROOM_PERMANENT %>">
    <tag:param name="back-url" value="${requestScope.backUrl}"/>
</tag:url>
<tag:url var="helpUrl" value="<%= ClientWebUrl.HELP %>"/>

<div class="jspWizardRoomType">
    <div class="actions">
        <span><spring:message code="views.wizard.room.type"/></span>
        <ul style="list-style-type:none; padding-right: 40px;">
            <li>
                <a href="${createAdhocRoomUrl}" tabindex="1"><spring:message code="views.wizard.room.type.adhoc"/></a>
                <p><spring:message code="views.help.roomType.ADHOC_ROOM.description"/></p>
                <ul style="line-height:170%; list-style-type:none; margin: 20px; font-size: .8em;">
                    <li><i style="color:green" class="fa fa-plus" aria-hidden="true"></i> <spring:message code="views.help.roomType.ADHOC_ROOM.positive"/></li>
                    <li><i style="color:red" class="fa fa-minus" aria-hidden="true"></i> <spring:message code="views.help.roomType.ADHOC_ROOM.negative1"/></li>
                    <li><i style="color:red" class="fa fa-minus" aria-hidden="true"></i> <spring:message code="views.help.roomType.ADHOC_ROOM.negative2"/></li>
                </ul>
                <%--<table style="margin: 20px; font-size: .8em;">
                    <tr>
                        <td colspan="3">
                            <strong><spring:message code="views.wizard.room.type.adhoc.quick"/>:</strong>
                        </td>
                    </tr>
                    <tr>
                        <td></td>
                        <c:forEach var="technology" items="H323_SIP,ADOBE_CONNECT">
                            <td class="header"><strong><spring:eval expression="T(cz.cesnet.shongo.client.web.models.TechnologyModel).valueOf(technology).title"/></strong></td>
                        </c:forEach>
                    </tr>
                    <c:forEach var="participantCount" items="2,3,4,5">
                        <tr>
                            <td>
                                <spring:eval expression="T(Integer).parseInt(participantCount)" var="participantCount"/>
                                <spring:message code="views.room.participants.value" arguments="${participantCount}"/>
                            </td>
                            <c:forEach var="technology" items="H323_SIP,ADOBE_CONNECT">
                                <td>
                                    <c:forEach var="minutes" items="30,60,120,180">
                                        <spring:eval expression="T(org.joda.time.Period).parse('PT' + minutes + 'M').normalizedStandard()" var="duration"/>
                                        <a class="btn-sm btn-default" href="${createAdhocRoomUrl}&technology=${technology}&participantCount=${participantCount}&duration=${duration}&confirm=false"><tag:format value="${duration}"/></a>
                                    </c:forEach>
                                </td>
                            </c:forEach>
                        </tr>
                    </c:forEach>
                </table>--%>
                <a class="btn btn-success" href="${createAdhocRoomUrl}" tabindex="1"><b><spring:message code="views.wizard.room.attributes.create.ADHOC_ROOM"/></b></a>
            </li>
            <br/>
            <li>
                <a href="${createPermanentRoomUrl}" tabindex="1"><spring:message code="views.wizard.room.type.permanent"/></a>
                <p><spring:message code="views.help.roomType.PERMANENT_ROOM.description"/></p>
                <ul style="line-height:170%; list-style-type:none; margin: 20px; font-size: .8em;">
                    <li><i  style="color:green" class="fa fa-plus" aria-hidden="true"></i> <spring:message code="views.help.roomType.PERMANENT_ROOM.positive1"/></li>
                    <li><i  style="color:green" class="fa fa-plus" aria-hidden="true"></i> <spring:message code="views.help.roomType.PERMANENT_ROOM.positive2"/></li>
                    <li><i style="color:red" class="fa fa-minus" aria-hidden="true"></i> <spring:message code="views.help.roomType.PERMANENT_ROOM.negative"/></li>
                </ul>
                <a class="btn btn-success" href="${createPermanentRoomUrl}" tabindex="1"><b><spring:message code="views.wizard.room.attributes.create.PERMANENT_ROOM"/></b></a>
            </li>
            <br/>
            <a  href="${helpUrl}#rooms" target="_blank">
                <spring:message code="views.help.rooms.display"/>
            </a>
        </ul>
    </div>
</div>