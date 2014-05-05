<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="name" required="true" type="java.lang.String" %>
<%@ attribute name="expandable" required="false" type="java.lang.Boolean" %>
<%@ attribute name="expandCode" required="false" type="java.lang.String" %>
<%@ attribute name="collapseCode" required="false" type="java.lang.String" %>
<%@ attribute name="cssClass" required="false" type="java.lang.String" %>
<c:if test="${expandable == null}">
    <c:set var="expandable" value="${true}"/>
</c:if>

<script type="text/javascript">
    angular.provideModule('tag:expandableBlock', ['ngCookies']);

    function ExpandableControllerController($scope, $cookieStore) {
        $scope.expanded = true;
        if ( ${expandable} ) {
            $scope.expanded = $cookieStore.get("${name}.expanded") == true;
            $scope.$watch("expanded", function () {
                $cookieStore.put("${name}.expanded", $scope.expanded, Infinity, '/');
            });
        }
    }
</script>

<div class="${cssClass}" ng-controller="ExpandableControllerController" ng-class="{'collapsed': !expanded}">
    <c:if test="${expandable}">
            <spring:message code="views.button.toggleExpandable" var="toggleTitle"/>
            <a class="fa" ng-click="expanded = !expanded" ng-class="{'fa-plus': !expanded, 'fa-minus': expanded}"
               href="" title="${toggleTitle}"></a>
    </c:if>
    <span ng-show="expanded">
        <c:if test="${collapseCode != null}">
            <a ng-click="expanded = false" href=""><spring:message code="${collapseCode}"/></a>&nbsp;
        </c:if>
        <jsp:doBody/>
    </span>
    <span ng-hide="expanded">
        <c:if test="${expandCode != null}">
            <a ng-click="expanded = true" href=""><spring:message code="${expandCode}"/></a>&nbsp;
        </c:if>
    </span>
</div>


