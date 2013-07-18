<%--
  -- Page for creation/modification of a reservation request.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.models.ReservationRequestModel" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="app" uri="/WEB-INF/client-web.tld" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="confirmUrl" value="${contextPath}${urlConfirm}"/>
<c:set var="backUrl" value="${contextPath}${urlBack}"/>
<tiles:importAttribute/>

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
            placeholder: "Select user by name/email",
            width: 'resolve',
            minimumInputLength: 2,
            ajax: {
                url: "/user",
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
                callback({id: 0, text: 'Loading...'});
                $.ajax("/user/" + id, {
                    dataType: "json"
                }).done(function (data) {
                            callback({id: id, text: window.formatUser(data)});
                        });
            }
        });
    });
</script>

<form:form class="form-horizontal"
           commandName="aclRecord"
           action="${confirmUrl}"
           method="post">

    <fieldset>

        <form:hidden path="id"/>

        <div class="control-group">
            <form:label class="control-label" path="entityId">
                <spring:message code="${entity}"/>:
            </form:label>
            <div class="controls double-width">
                <form:input path="entityId" readonly="true"/>
            </div>
        </div>

        <div class="control-group">
            <form:label class="control-label" path="userId">
                <spring:message code="views.aclRecord.user"/>:
            </form:label>
            <div class="controls double-width">
                <form:input path="userId" cssErrorClass="error"/>
                <form:errors path="userId" cssClass="error"/>
            </div>
        </div>

        <div class="control-group">
            <form:label class="control-label" path="role">
                <spring:message code="views.aclRecord.role"/>:
            </form:label>
            <div class="controls">
                <form:select path="role">
                    <c:forEach items="${roles}" var="role">
                        <form:option value="${role}"><spring:message code="views.aclRecord.role.${role}"/></form:option>
                    </c:forEach>
                </form:select>
                <form:errors path="role" cssClass="error"/>
            </div>
        </div>

    </fieldset>

    <div class="control-group">
        <div class="controls">
            <spring:message code="${confirmUrl}" var="confirm"/>
            <input class="btn btn-primary" type="submit" value="${confirmUrl}"/>
            <a class="btn" href="${backUrl}"><spring:message code="views.button.cancel"/></a>
        </div>
    </div>

</form:form>
