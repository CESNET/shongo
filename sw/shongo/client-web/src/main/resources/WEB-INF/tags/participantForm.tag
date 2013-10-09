<%@ tag import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%--
  -- Reservation request form.
  --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="confirmTitle" required="false" type="java.lang.String" %>
<%@attribute name="cancelUrl" required="false" type="java.lang.String" %>
<%@attribute name="cancelTitle" required="false" type="java.lang.String" %>

<%
    String formUrl = request.getParameter("form-url");
    jspContext.setAttribute("formUrl", formUrl);
%>

<c:if test="${empty formUrl}">
    <c:set var="formUrl" value="${requestUrl}"/>
</c:if>

<c:set var="tabIndex" value="1"/>

<tag:url var="userListUrl" value="<%= ClientWebUrl.USER_LIST_DATA %>"/>
<tag:url var="userUrl" value="<%= ClientWebUrl.USER_DATA %>">
    <tag:param name="userId" value=":userId"/>
</tag:url>

<script type="text/javascript">
    window.formatUser = function(user) {
        var text = user.firstName;
        if ( user.lastName != null ) {
            text += " " + user.lastName;
        }
        if ( user.originalId != null ) {
            text += " (" + user.originalId + ")";
        }
        return text;
    };
    $(function () {
        $("#userId").select2({
            placeholder: "<spring:message code="views.select.user"/>",
            width: 'resolve',
            minimumInputLength: 2,
            ajax: {
                url: "${userListUrl}",
                dataType: 'json',
                data: function (term, page) {
                    return {
                        filter: term
                    };
                },
                results: function (data, page) {
                    var results = [];
                    for (var index = 0; index < data.length; index++) {
                        var dataItem = data[index];
                        results.push({id: dataItem.userId, text: window.formatUser(dataItem)});
                    }
                    return {results: results};
                }
            },
            initSelection: function (element, callback) {
                var id = $(element).val();
                callback({id: 0, text: '<spring:message code="views.select.loading"/>'});
                $.ajax("${userUrl}".replace(':userId', id), {
                    dataType: "json"
                }).done(function (data) {
                            callback({id: id, text: window.formatUser(data)});
                        });
            }
        });
    });
</script>

<form:form class="form-horizontal"
           commandName="participant"
           method="post">

    <fieldset>

        <form:hidden path="id"/>

        <div class="control-group">
            <form:label class="control-label" path="type">
                <spring:message code="views.participant.type"/>:
            </form:label>
            <div class="controls">
                <form:radiobutton path="type" value="USER"/><spring:message code="views.participant.type.USER"/>
                <br/>
                <form:radiobutton path="type" value="ANONYMOUS"/><spring:message code="views.participant.type.ANONYMOUS"/>
                <form:errors path="type" cssClass="error"/>
            </div>
        </div>

        <div class="control-group">
            <form:label class="control-label" path="userId">
                <spring:message code="views.aclRecord.user"/>:
            </form:label>
            <div class="controls double-width">
                <form:input path="userId" cssErrorClass="error" tabindex="${tabIndex}"/>
                <form:errors path="userId" cssClass="error"/>
            </div>
        </div>

    </fieldset>

    <c:if test="${not empty confirmTitle || cancelUrl != null}">
        <div class="control-group">
            <div class="controls">
                <c:if test="${not empty confirmTitle}">
                    <spring:message code="${confirmTitle}" var="confirmTitle"/>
                    <input class="btn btn-primary" type="submit" value="${confirmTitle}" tabindex="${tabIndex}"/>
                </c:if>
                <c:if test="${cancelUrl != null}">
                    <c:if test="${empty cancelTitle}">
                        <c:set var="cancelTitle" value="views.button.cancel"/>
                    </c:if>
                    <a class="btn" href="${cancelUrl}" tabindex="${tabIndex}"><spring:message code="${cancelTitle}"/></a>
                </c:if>
            </div>
        </div>
    </c:if>

</form:form>
