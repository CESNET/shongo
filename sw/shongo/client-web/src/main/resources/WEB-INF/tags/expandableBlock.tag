<%@ tag trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="name" required="true" type="java.lang.String" %>
<%@ attribute name="collapsedText" required="false" type="java.lang.String" %>
<%@ attribute name="cssClass" required="false" type="java.lang.String" %>

<c:set var="advancedUserInterface" value="${sessionScope.user.advancedUserInterface}"/>

<script type="text/javascript">
    angular.provideModule('tag:expandableBlock', ['ngCookies']);

    function ExpandableControllerController($scope, $cookieStore) {
        $scope.expanded = true;
        if ( ${advancedUserInterface} ) {
            $scope.expanded = $cookieStore.get("${name}.expanded") == 'true';
            $scope.$watch("expanded", function () {
                $cookieStore.put("${name}.expanded", $scope.expanded, Infinity, '/');
            });
        }
    }
</script>

<div class="${cssClass}" ng-controller="ExpandableControllerController" ng-class="{'collapsed': !expanded}">
    <c:if test="${advancedUserInterface}">
            <spring:message code="views.button.toggleExpandable" var="toggleTitle"/>
            <a ng-click="expanded = !expanded" class="pull-right" ng-class="{'icon-plus': !expanded, 'icon-minus': expanded}"
               href="" title="${toggleTitle}"></a>
    </c:if>
    <div ng-show="expanded">
        <jsp:doBody/>
    </div>
    <div ng-hide="expanded">
        <span><a ng-click="expanded = true" href="">${collapsedText}</a></span>&nbsp;
    </div>
</div>


