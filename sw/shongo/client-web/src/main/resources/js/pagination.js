var module = angular.module('pagination', ['ngResource']);

module.controller('PaginationController', function ($scope, $resource) {

    var resource = null;

    /**
     * Initialize the controller.
     *
     * @param url for listing items with ":start" and ":count" parameters
     */
    $scope.init = function (url) {
        resource = $resource(url, null, {
            list: {method: 'GET'}
        });
        $scope.setPage(0);
    };

    var setData = function (data) {
        $scope.items = data.items;
        $scope.pages = [];
        var pageCount = Math.floor((data.count - 1) / $scope.pageSize) + 1;
        for (var pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            var pageStart = pageIndex * $scope.pageSize;
            var pageActive = (data.start >= pageStart) && (data.start < (pageStart + $scope.pageSize));
            $scope.pages.push({start: pageStart, active: pageActive});
        }
    };

    $scope.pageSize = 5;
    $scope.items = [];
    $scope.pages = [
        {start: 0, active: true}
    ];
    $scope.setPage = function (pageIndex) {
        // Get page
        var page = $scope.pages[pageIndex];

        // List items
        resource.list({start: page.start, count: $scope.pageSize}, setData);
    };
    $scope.updatePageSize = function () {
        $scope.pageSize = parseInt($scope.pageSize);

        // Find new start
        var start = 0;
        for (var pageIndex = 0; pageIndex < $scope.pages.length; pageIndex++) {
            var page = $scope.pages[pageIndex];
            if (page.active) {
                start = page.start;
            }
        }
        start = Math.floor(start / $scope.pageSize) * $scope.pageSize;

        // List items
        resource.list({start: start, count: $scope.pageSize}, setData);
    };
});

/**
 * Directive <pagination-page-size> for displaying page size selection.
 */
module.directive('paginationPageSize', function () {
    return {
        restrict: 'E',
        replace: true,
        template: '<div>Records per page ' +
            '<select ng-model="pageSize" ng-change="updatePageSize()"  style="width: 60px;">' +
            '  <option value="5" selected="true">5</option>' +
            '  <option value="10">10</option>' +
            '  <option value="15">15</option>' +
            '</select>' +
            '</div>'
    }
});

/**
 * Directive <pagination-pages> for displaying page links.
 */
module.directive('paginationPages', function () {
    return {
        restrict: 'E',
        replace: true,
        template: '<div>Pages:' +
            '<span ng-repeat="page in pages">' +
            '  <a ng-hide="page.active" class="page" href="" ng-click="setPage($index)">{{$index + 1}}</a>' +
            '  <span ng-show="page.active" class="page">{{$index + 1}}</span>' +
            '</span>' +
            '</div>'
    }
});