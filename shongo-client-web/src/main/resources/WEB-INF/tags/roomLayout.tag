<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="id" required="true" type="java.lang.String" %>
<%@ attribute name="model" required="false" type="java.lang.String" %>
<%@ attribute name="content" required="false" type="java.lang.String" %>
<%@ attribute name="width" required="false" type="java.lang.String" %>

<c:set var="javascript">
    function tagRoomLayout_${id}Init() {
        function formatRoomLayout(roomLayout) {
            return "<span class='" + roomLayout.id + "'>" + roomLayout.text +"</span>";
        }
        $("#${id}").select2({
        <c:if test="${not empty width}">
            width: "${width}",
        </c:if>
            minimumResultsForSearch: -1,
            dropdownCssClass: "room-layout",
            formatResult: formatRoomLayout,
            formatSelection: formatRoomLayout,
            escapeMarkup: function(markup) { return markup; }
        });
    };
</c:set>

<c:set var="html">
    <select id="${id}" class="room-layout" ng-model="${model}">
        <spring:eval var="layouts" expression="T(cz.cesnet.shongo.api.RoomLayout).values()"/>
        <c:forEach items="${layouts}" var="layout">
            <c:choose>
                <c:when test="${layout == 'OTHER'}">
                    <option value="${layout}" ng-show="layout == 'OTHER'">
                </c:when>
                <c:otherwise>
                    <option value="${layout}">
                </c:otherwise>
            </c:choose>
            <spring:message code="views.room.layout.${layout}"/></option>
        </c:forEach>
    </select>
</c:set>

<c:choose>
    <c:when test="${content == 'javascript'}">
        ${javascript}
    </c:when>
    <c:when test="${content == 'html'}">
        ${html}
    </c:when>
    <c:otherwise>
        <script type="text/javascript">
            ${javascript}
            setTimeout(function(){
                tagRoomLayout_${id}Init();
            },0);
        </script>
        ${html}
    </c:otherwise>
</c:choose>