<%--
  -- List of participants.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="isWritable" required="false" %>
<%@attribute name="data" required="false" type="java.util.Collection" %>
<%@attribute name="dataUrl" required="false" %>
<%@attribute name="dataUrlParameters" required="false" %>
<%@attribute name="createUrl" required="false" %>
<%@attribute name="modifyUrl" required="false" %>
<%@attribute name="deleteUrl" required="false" %>
<%@attribute name="hideRole" required="false" type="java.lang.Boolean" %>

<c:set var="isWritable" value="${isWritable != null ? isWritable : true}"/>
<c:set var="tableHead">
    <thead>
    <tr>
        <th><spring:message code="views.participant.userId"/></th>
        <c:if test="${!hideRole}">
            <th>
                <spring:message code="views.participant.role"/>
                <tag:help>
                    <spring:eval var="roles" expression="T(cz.cesnet.shongo.ParticipantRole).values()"/>
                    <c:forEach items="${roles}" var="role">
                        <strong><spring:message code="views.participant.role.${role}"/></strong>
                        <p><spring:message code="views.participant.roleHelp.${role}"/></p>
                    </c:forEach>
                </tag:help>
            </th>
        </c:if>
        <th><spring:message code="views.participant.email"/></th>
        <c:if test="${isWritable && (not empty modifyUrl || not empty deleteUrl)}">
            <th style="min-width: 85px; width: 85px;">
                <spring:message code="views.list.action"/>
            </th>
        </c:if>
    </tr>
    </thead>
</c:set>
<c:set var="tableEmptyRow">
    <td colspan="4" class="empty"><spring:message code="views.list.none"/></td>
</c:set>

<div class="tagParticipantList">
    <c:choose>
        <%-- Static list of participants --%>
        <c:when test="${data != null}">
            <jsp:doBody/>
            <table class="table table-striped table-hover">
                ${tableHead}
                <tbody>
                <c:forEach items="${data}" var="participant">
                    <tr>
                        <td>${participant.name}
                            <c:if test="${participant.type == 'USER' && not empty participant.user.organization}">
                                (${participant.user.organization})
                            </c:if>
                        </td>
                        <c:if test="${!hideRole}">
                            <td><spring:message code="views.participant.role.${participant.role}"/></td>
                        </c:if>
                        <td>${participant.email}</td>
                        <c:if test="${isWritable && (not empty modifyUrl || not empty deleteUrl)}">
                            <td>
                                <c:if test="${not empty participant.id && not empty modifyUrl}">
                                    <tag:url var="participantModifyUrl" value="${modifyUrl}">
                                        <tag:param name="participantId" value="${participant.id}"/>
                                    </tag:url>
                                    <tag:listAction code="modify" url="${participantModifyUrl}" tabindex="2"/>
                                </c:if>
                                <c:if test="${not empty participant.id && not empty deleteUrl}">
                                    <tag:url var="participantDeleteUrl" value="${deleteUrl}">
                                        <tag:param name="participantId" value="${participant.id}"/>
                                    </tag:url>
                                    <tag:listAction code="delete" url="${participantDeleteUrl}" tabindex="2"/>
                                </c:if>
                            </td>
                        </c:if>
                    </tr>
                </c:forEach>
                <c:if test="${empty data}">
                    <tr>${tableEmptyRow}</tr>
                </c:if>
                </tbody>
            </table>
            <c:if test="${isWritable && createUrl != null}">
                <div class="table-actions">
                    <a class="btn btn-primary" href="${createUrl}">
                        <spring:message code="views.button.add"/>
                    </a>
                </div>
            </c:if>
        </c:when>

        <%-- Dynamic list of participants --%>
        <c:when test="${dataUrl != null}">
            <tag:url var="participantModifyUrl" value="${modifyUrl}">
                <tag:param name="participantId" value="{{participant.id}}" escape="false"/>
            </tag:url>
            <tag:url var="participantDeleteUrl" value="${deleteUrl}">
                <tag:param name="participantId" value="{{participant.id}}" escape="false"/>
            </tag:url>
            <div ng-controller="PaginationController"
                 ng-init="init('participants', '${dataUrl}', {${dataUrlParameters}})">
                <spring:message code="views.pagination.records.all" var="paginationRecordsAll"/>
                <spring:message code="views.button.refresh" var="paginationRefresh"/>
                <pagination-page-size class="pull-right" unlimited="${paginationRecordsAll}" refresh="${paginationRefresh}">
                    <spring:message code="views.pagination.records"/>
                </pagination-page-size>
                <jsp:doBody/>
                <div class="spinner" ng-hide="ready || errorContent"></div>
                <span ng-controller="HtmlController" ng-show="errorContent" ng-bind-html="html(errorContent)"></span>
                <table class="table table-striped table-hover" ng-show="ready">
                    ${tableHead}
                    <tbody>
                    <tr ng-repeat="participant in items" class="user-role">
                        <td>{{participant.name}}
                            <span ng-show="participant.organization">
                                ({{participant.organization}})
                            </span>
                        </td>
                        <c:if test="${!hideRole}">
                            <td>{{participant.role}}</td>
                        </c:if>
                        <td>{{participant.email}}</td>
                        <c:if test="${isWritable && (not empty modifyUrl || not empty deleteUrl)}">
                            <td>
                                <c:if test="${not empty modifyUrl}">
                                    <span ng-show="participant.id">
                                        <tag:listAction code="modify" url="${participantModifyUrl}" tabindex="2"/>
                                    </span>
                                </c:if>
                                <c:if test="${not empty deleteUrl}">
                                    <span ng-show="participant.id">
                                        <tag:listAction code="delete" url="${participantDeleteUrl}" tabindex="2"/>
                                    </span>
                                </c:if>
                            </td>
                        </c:if>
                    </tr>
                    <tr ng-hide="items.length">${tableEmptyRow}</tr>
                    </tbody>
                </table>
                <c:if test="${isWritable && createUrl != null}">
                    <div class="table-actions">
                        <a class="btn btn-primary" href="${createUrl}" tabindex="1">
                            <spring:message code="views.button.add"/>
                        </a>
                    </div>
                </c:if>
                <pagination-pages class="${(isWritable && createUrl != null) ? 'pull-right' : ''}" ng-show="ready">
                    <spring:message code="views.pagination.pages"/>
                </pagination-pages>
            </div>

        </c:when>

                <%-- Error --%>
        <c:otherwise>
            Neither attribute
            <pre>data</pre>
            or
            <pre>dataUrl</pre>
            has been specified.
        </c:otherwise>
    </c:choose>

</div>



