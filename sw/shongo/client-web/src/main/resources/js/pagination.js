var module = angular.module('pagination', ['ngResource', 'ngCookies']);

/**
 *
 */
module.controller('PaginationController', function ($scope, $resource, $cookieStore) {
    // Resource used for fetching items
    $scope.resource = null;
    // Current page index
    $scope.pageIndex = null;
    // Current page size (number of items per page)
    $scope.pageSize = 5;
    // Specifies whether items are ready to show (e.g., they have been fetched for the first time)
    $scope.ready = false;
    // List of current items
    $scope.items = [];
    // List of current pages
    $scope.pages = [
        {start: 0, active: true}
    ];

    /**
     * Set fetched data.
     *
     * @param data to be set
     */
    var setData = function (data) {
        // Set current items
        $scope.items = data.items;
        // Create pages
        $scope.pages = [];
        var pageCount = Math.floor((data.count - 1) / $scope.pageSize) + 1;
        for (var pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            var pageStart = pageIndex * $scope.pageSize;
            var pageActive = (data.start >= pageStart) && (data.start < (pageStart + $scope.pageSize));
            if (pageActive && pageIndex != $scope.pageIndex) {
                $scope.pageIndex = pageIndex;
            }
            $scope.pages.push({start: pageStart, active: pageActive});
        }
    };

    /**
     * Initialize the controller.
     *
     * @param name of the controller for storing data to cookies
     * @param url for listing items with ":start" and ":count" parameters
     */
    $scope.init = function (name, url) {
        // Setup name and resource
        $scope.name = name;
        $scope.resource = $resource(url, null, {
            list: {method: 'GET'}
        });
        // Load configuration
        var configuration = $cookieStore.get(name);
        if (configuration != null) {
            $scope.pageSize = configuration.pageSize;
        }
        // List items for the first time (to determine total count)
        $scope.resource.list({start: 0, count: $scope.pageSize}, function(result){
            setData(result);
            // If configuration is loaded set configured page index
            if (configuration != null) {
                $scope.pageSize = configuration.pageSize;
                $scope.setPage(configuration.pageIndex, function(){
                    // First time data is ready
                    $scope.ready = true;
                });
            }
            else {
                // First time data is ready
                $scope.ready = true;
            }
        });
    };

    /**
     * Store current configuration (page index and page size).
     */
    $scope.storeConfiguration = function () {
        var configuration = {
            pageIndex: $scope.pageIndex,
            pageSize: $scope.pageSize
        };
        $cookieStore.put($scope.name, configuration);
    };

    /**
     * Set current page.
     *
     * @param pageIndex
     * @param callback to be called after page is set
     */
    $scope.setPage = function (pageIndex, callback) {
        if (pageIndex == $scope.pageIndex) {
            if (callback != null) {
                callback.call();
            }
            return;
        }
        if (!/^\d+$/.test(pageIndex)) {
            pageIndex = 0;
        }
        else if (pageIndex < 0 ) {
            pageIndex = 0;
        }
        else if (pageIndex >= $scope.pages.length ) {
            pageIndex = $scope.pages.length - 1;
        }

        $scope.pageIndex = pageIndex;

        // Get page
        var page = $scope.pages[pageIndex];

        // List items
        $scope.resource.list({start: page.start, count: $scope.pageSize}, function(data){
            setData(data);
            if (callback != null) {
                callback.call();
            }

            // Store configuration
            $scope.storeConfiguration();
        });
    };

    /**
     * Update page sizes by current page size.
     */
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
        $scope.resource.list({start: start, count: $scope.pageSize}, function(data){
            setData(data);

            // Store configuration
            $scope.storeConfiguration();
        });
    };
});

/**
 * Directive <pagination-page-size> for displaying page size selection.
 */
module.directive('paginationPageSize', function () {
    return {
        restrict: 'E',
        compile: function(element, attrs, transclude) {
            var text = element[0].innerText;
            var html = '<div class="' + attrs.class + '">' + text + '&nbsp;&nbsp;' +
                '<select ng-model="pageSize" ng-change="updatePageSize()" style="width: 60px; margin-bottom: 0px; padding: 0px 4px; height: 24px;">' +
                '  <option value="5" selected="true">5</option>' +
                '  <option value="10">10</option>' +
                '  <option value="15">15</option>' +
                '</select>' +
                '</div>'
            element.replaceWith(html);
        }
    }
});

/**
 * Directive <pagination-pages> for displaying page links.
 */
module.directive('paginationPages', function () {
    return {
        restrict: 'E',
        compile: function(element, attrs, transclude) {
            var text = element[0].innerText;
            var html = '<div class="' + attrs.class + '">' + text + ' ' +
                '<span ng-repeat="page in pages">' +
                '  <a ng-hide="page.active" class="page" href="" ng-click="setPage($index)">{{$index + 1}}</a>' +
                '  <span ng-show="page.active" class="page">{{$index + 1}}</span>' +
                '</span>' +
                '</div>';
            element.replaceWith(html);
        }
    }
});