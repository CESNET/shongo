<%--
  -- Help for RoomType (ADHOC_ROOM, PERMANENT_ROOM).
  --%>
<%@ tag body-content="empty" %>
<%----%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>
<%----%>
<%@attribute name="roomType" required="false" type="java.lang.String" %>
<%--
<%----%>
<div class="tagHelpRoomType">
    <c:choose>
        <c:when test="${roomType == 'ADHOC_ROOM'}">
            <p><spring:message code="views.help.roomType.ADHOC_ROOM.description"/></p>
            <div class="room-examples">
                <p><spring:message code="views.help.roomType.ADHOC_ROOM.example"/></p>
                <div class="room-example">
                    <div class="room-image">
                        <img src="/img/room/adhoc_room1.png"/>

                        <p class="room-name"><spring:message code="views.help.roomType.ADHOC_ROOM.example.room1.name"/></p>
                    </div>
                    <p><spring:message code="views.help.roomType.ADHOC_ROOM.example.room1.description"/></p>
                </div>
                <div class="room-example">
                    <div class="room-image">
                        <img src="/img/room/adhoc_room2.png"/>

                        <p class="room-name"><spring:message code="views.help.roomType.ADHOC_ROOM.example.room2.name"/></p>
                    </div>
                    <p><spring:message code="views.help.roomType.ADHOC_ROOM.example.room2.description"/></p>
                </div>
            </div>
        </c:when>
        <c:when test="${roomType == 'PERMANENT_ROOM'}">
            <p><spring:message code="views.help.roomType.PERMANENT_ROOM.description"/></p>
            <div class="room-examples">
                <p><spring:message code="views.help.roomType.PERMANENT_ROOM.example"/></p>
                <div class="room-example">
                    <div class="room-image">
                        <img src="/img/room/permanent_room.png"/>

                        <p class="room-name"><spring:message code="views.help.roomType.PERMANENT_ROOM.example.room.name"/></p>
                    </div>
                    <p><spring:message code="views.help.roomType.PERMANENT_ROOM.example.room.description"/></p>
                </div>
                <p><spring:message code="views.help.roomType.PERMANENT_ROOM.example.capacity"/></p>
                <div class="room-example">
                    <div class="room-image">
                        <img src="/img/room/permanent_room_capacity1.png"/>

                        <p class="room-name"><spring:message code="views.help.roomType.PERMANENT_ROOM.example.room.name"/></p>
                    </div>
                    <p><spring:message code="views.help.roomType.PERMANENT_ROOM.example.capacity1"/></p>
                </div>
                <div class="room-example">
                    <div class="room-image">
                        <img src="/img/room/permanent_room_capacity2.png"/>

                        <p class="room-name"><spring:message code="views.help.roomType.PERMANENT_ROOM.example.room.name"/></p>
                    </div>
                    <p><spring:message code="views.help.roomType.PERMANENT_ROOM.example.capacity2"/></p>
                </div>
            </div>
        </c:when>
    </c:choose>
</div>