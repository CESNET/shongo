/**
 * Pagination module.
 */
var paginationModule = angular.module('ngPagination', ['ngResource', 'ngCookies']);

/**
 * Ready controller.
 */
paginationModule.controller('ReadyController', function ($scope) {
    $scope.readyCount = 0;
    $scope.ready = false;
    $scope.$watch('readyCount', function () {
        if ($scope.readyCount == 0) {
            $scope.ready = true;
        }
    });
});

/**
 * Pagination controller.
 *
 * Must be initialized by {@link init(name, url, urlParameters)} method.
 * URL must return data in format:
 *
 *     {
 *         start: <index-of-first-requested-item>,
 *         count: <total-number-of-all-items>,
 *         sort: <column-by-which-are-items-sorted>,
 *         sort-desc: <boolean-whether-sorting-is-descending>,
 *         items: [
 *             <requested-items>
 *         ]
 *     }
 *
 * $scope.ready        - can be used to determine whether data can be shown
 * $scope.errorContent - can be used to show an error
 */
paginationModule.controller('PaginationController', function ($scope, $application, $element, $resource, $window, $cookieStore) {
    // URL
    $scope.url = null;
    $scope.urlParameters = null;

    // URL for deletion of reservation requests
    $scope.deleteUrl = null;
    // Checkbox name for deleting multiple reservation requests
    $scope.checkboxName = null;

    // Current page index
    $scope.pageIndex = null;
    // Current page size (number of items per page)
    $scope.pageSize = 10;
    // Specifies whether items are ready to show (e.g., they have been fetched for the first time)
    $scope.ready = false;
    // Increment parent readyCount
    if ($scope.$parent != null) {
        $scope.$parent.readyCount++;
    }
    // Error
    $scope.error = false;
    $scope.errorContent = null;
    // List of current items
    $scope.items = [];
    // List of current pages
    $scope.pages = [
        {start: 0, active: true}
    ];
    // Sorting
    $scope.sort = null;
    $scope.sortDesc = null;
    $scope.sortDefault = null;
    $scope.sortDescDefault = null;
    $scope.setSortDefault = function(sort, sortDesc) {
        $scope.sortDefault = sort;
        $scope.sortDescDefault = (sortDesc != null ? sortDesc : false);
        $scope.setSort();
    };
    $scope.setSort = function(sort, event) {
        if (typeof(event) == "boolean") {
            $scope.sort = null;
            $scope.sortDesc = event;
        }
        if (sort == null) {
            $scope.sort = $scope.sortDefault;
            $scope.sortDesc = $scope.sortDescDefault;
        }
        else if (event != null && event.shiftKey && $scope.sort != null) {
            $scope.sort = null;
            $scope.sortDesc = null;
        }
        else if ($scope.sort == sort ) {
            $scope.sortDesc = !$scope.sortDesc;
        }
        else {
            $scope.sort = sort;
            if ($scope.sortDesc == null) {
                $scope.sortDesc = false;
            }
        }
        if ($scope.ready) {
            $scope.refresh();
        }
    };

    /**
     * Test if given value is empty.
     *
     * @param value
     * @returns {boolean}
     */
    $scope.isEmpty = function(value) {
        return value == null || value == '';
    };

    /**
     * First time data is ready.
     */
    $scope.setReady = function (ready) {
        if (ready == null) {
            ready = true;
        }

        // Update element height
        if (ready) {
            $element.css('height', "auto");
        }
        else if ($scope.ready) {
            var height = parseInt($element.css('height'));
            $element.css('height', height + "px");
        }

        // Set new ready
        $scope.ready = ready;

        // Update parent readyCount
        if (ready && $scope.$parent != null && $scope.$parent.readyCount > 0) {
            $scope.$parent.readyCount--;
        }
    };

    /**
     * Set fetched data.
     *
     * @param data to be set
     */
    $scope.onSetData = null;
    var setData = function (data) {
        $scope.setReady(true);
        $scope.error = false;
        $scope.errorContent = null;
        if ($scope.$parent != null) {
            $scope.$parent.error = false;
            $scope.$parent.errorContent = null;
        }

        // Set sorting
        if (data['sort'] != null ) {
            $scope.sort = data['sort'];
        }
        if (data['sort-desc'] != null ) {
            $scope.sortDesc = data['sort-desc'];
        }

        // Set current items
        $scope.items = data.items;
        // Create pages
        $scope.pages = [];
        var pageCount = 1;
        if ($scope.pageSize != -1) {
            pageCount = Math.floor((data.count - 1) / $scope.pageSize) + 1;
            if (pageCount == 0) {
                pageCount = 1;
            }
        }
        for (var pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            var pageStart = pageIndex * $scope.pageSize;
            var pageActive = (data.start >= pageStart) && (data.start < (pageStart + $scope.pageSize));
            if (pageActive && pageIndex != $scope.pageIndex) {
                $scope.pageIndex = pageIndex;
            }
            $scope.pages.push({start: pageStart, active: pageActive});
        }

        if ( $scope.onSetData != null ) {
            $scope.onSetData($scope.items);
        }
    };

    /**
     * Error happened.
     */
    $scope.setError = function (response) {
        if (response.status == 500) {
            // Update element height
            $element.css('height', "auto");

            // Set error content
            $scope.errorContent = $application.getErrorContent(response.data);
            if ($scope.$parent != null) {
                $scope.$parent.error = true;
                $scope.$parent.errorContent = $scope.errorContent;
            }
        }
        $scope.error = true;
    };

    /**
     * Initialize the controller.
     *
     * @param name of the controller for storing data to cookies
     * @param url for listing items with ":start" and ":count" parameters
     * @param urlParameters for the url
     * @param refreshEvent to which it should listen and refresh when it happens (the first time auto refresh is skipped)
     * @param deleteUrl for deleting multiple reservation requests at once
     * @param checkboxName name of checkbox input for deleting multiple reservation requests
     */
    $scope.init = function (name, url, urlParameters, refreshEvent, deleteUrl, checkboxName) {
        // Setup name and resource
        $scope.name = name;
        $scope.url = url;
        $scope.urlParameters = urlParameters;
        $scope.deleteUrl = deleteUrl;
        $scope.checkboxName = checkboxName

        // Load configuration
        var configuration = null;
        try {
            configuration = angular.fromJson($cookieStore.get(name));
        } catch (error) {
            console.warn("Failed to load pagination configuration", error);
        }
        if (configuration != null) {
            $scope.pageSize = configuration.pageSize;
        }
        if (refreshEvent != null) {
            // Bind to refresh event
            $scope.$on(refreshEvent, function(){
                $scope.refresh();
            });
        }
        else {
            // List items for the first time (to determine total count)
            $scope.performList(0, function (result) {
                setData(result);
                // If configuration is loaded set configured page index
                if (configuration != null) {
                    $scope.pageSize = configuration.pageSize;
                    $scope.setPage(configuration.pageIndex, function () {
                        $scope.setReady(true);
                    });
                }
                else {
                    $scope.setReady(true);
                }
            });
        }
    };

    /**
     * Store current configuration (page index and page size).
     */
    $scope.storeConfiguration = function () {
        var configuration = {
            pageIndex: $scope.pageIndex,
            pageSize: $scope.pageSize
        };
        $cookieStore.put($scope.name, angular.toJson(configuration), Infinity, '/');
    };

    /**
     * Set current page.
     *
     * @param pageIndex
     * @param callback to be called after page is set
     * @param forceReload
     */
    $scope.setPage = function (pageIndex, callback, forceReload) {
        if (pageIndex == $scope.pageIndex && !forceReload) {
            if (callback != null) {
                callback.call();
            }
            return;
        }
        if (!/^\d+$/.test(pageIndex)) {
            pageIndex = 0;
        }
        else if (pageIndex < 0) {
            pageIndex = 0;
        }
        else if (pageIndex >= $scope.pages.length) {
            pageIndex = $scope.pages.length - 1;
        }

        $scope.pageIndex = pageIndex;

        // Get page
        var page = $scope.pages[pageIndex];

        // List items
        $scope.performList(page.start, function (data) {
            setData(data);
            if (callback != null) {
                callback.call();
            }

            // Store configuration
            $scope.storeConfiguration();
        });
    };

    $scope.performList = function (start, callback) {
        var listParameters = {
            'start': start,
            'count': $scope.pageSize
        };
        if ($scope.sort != null) {
            listParameters['sort'] = $scope.sort;
            listParameters['sort-desc'] = $scope.sortDesc;
        }

        $scope.setReady(false);

        var url = $scope.url;
        if (typeof url == "function") {
            url = url();
        }
        $scope.resource = $resource(url, $scope.urlParameters, {
            list: {method: 'GET'}
        });
        return $scope.resource.list(listParameters, function(response) {
            callback(response);
        }, function(response){
            if (!$application.handleAjaxFailure(response)) {
                $scope.setError(response);
            }
        });
    };

    /**
     * Update page sizes by current page size.
     */
    $scope.updatePageSize = function () {
        $scope.pageSize = parseInt($scope.pageSize);

        // Find new start
        var start = 0;
        if ($scope.pageSize != -1) {
            for (var pageIndex = 0; pageIndex < $scope.pages.length; pageIndex++) {
                var page = $scope.pages[pageIndex];
                if (page.active) {
                    start = page.start;
                }
            }
            start = Math.floor(start / $scope.pageSize) * $scope.pageSize;
        }

        // List items
        $scope.performList(start, function (data) {
            setData(data);

            // Store configuration
            $scope.storeConfiguration();
        });
    };

    /**
     * Refresh current page
     */
    $scope.refresh = function() {
        $scope.setPage($scope.pageIndex, null, true);
    };

    /**
     * Deletes multiple reservation requests
     */
    $scope.removeCheckedReservationRequests = function () {
        if ($scope.deleteUrl == null || $scope.checkboxName == null) {
            console.error("No URL or name of checkbox element specified for deleting reservation requests.");
            return false;
        }

        var checkboxesChecked = document.querySelectorAll("input[type='checkbox'][name='" + $scope.checkboxName + "']:checked");
        if (checkboxesChecked.length == 0) {
            return false;
        }

        $scope.removeReservationRequests($scope.deleteUrl, checkboxesChecked);
    }

    $scope.removeAllReservationRequests = function () {
        if ($scope.deleteUrl == null || $scope.checkboxName == null) {
            console.error("No URL or name of checkbox element specified for deleting reservation requests.");
            return false;
        }

        var checkboxes = document.querySelectorAll("input[type='checkbox'][name='" + $scope.checkboxName + "']");
        if (checkboxes.length == 0) {
            return false;
        }

        $scope.removeReservationRequests($scope.deleteUrl, checkboxes);
    }

    $scope.removeReservationRequests = function (deleteUrl, checkboxElements) {
        if (deleteUrl == null || checkboxElements == null) {
            console.error("No URL or name of checkbox element specified for deleting reservation requests.");
            return false;
        }

        deleteUrl += '?';
        for (var i = 0; i < checkboxElements.length; i++) {
            deleteUrl += 'reservationRequestId=' + checkboxElements[i].getAttribute("value");
            if(i != checkboxElements.length-1) {
                deleteUrl += '&';
            }
        }
        window.location = deleteUrl;
    }
});

/**
 * Directive <pagination-page-size> for displaying page size selection.
 */
paginationModule.directive('paginationPageSize', function () {
    return {
        restrict: 'E',
        compile: function (element, attrs, transclude) {
            var text = element.html();
            var attributeClass = (attrs.class != null ? (' ' + attrs.class) : '');
            var optionUnlimited = '';
            if ( attrs.unlimited != null ) {
                optionUnlimited = '<option value="-1">' + attrs.unlimited + '</option>';
            }

            var remove = '';
            if ( attrs.remove != null ) {
                remove += '&nbsp;&nbsp;<a href="" ng-click="removeCheckedReservationRequests()" class="btn btn-default" title="' + attrs.remove + '"><span class="fa fa-trash-o"></span></a>'
            }
            var removeAll = '';
            if ( attrs.removeAll != null ) {
                removeAll += '&nbsp;&nbsp;<a href="" ng-click="removeAllReservationRequests()" class="btn btn-default" title="' + attrs.removeAll + '"><span class="fa fa-trash-o fa-red"></span></a>'
            }
            var refresh = '';
            if ( attrs.refresh != null ) {
                refresh += '&nbsp;&nbsp;<a href="" ng-click="refresh()" class="btn btn-default" title="' + attrs.refresh +'"><span class="fa fa-refresh"></span></a>';
            }
            var html =
                '<div class="form-inline pagination-page-size' + attributeClass + '">' +
                '<span ng-hide="pages.length == 1 && items.length <= 10">' + text + '&nbsp;' +
                '<select class="form-control" ng-model="pageSize" ng-change="updatePageSize()" style="width: 60px; margin-bottom: 0px; padding: 0px 4px; height: 24px;">' +
                '<option value="10">10</option>' +
                '<option value="15">15</option>' +
                '<option value="20">20</option>' +
                optionUnlimited +
                '</select>' +
                '</span>' +
                remove + removeAll + refresh +
                '</div>';
            element.replaceWith(html);
        }
    }
});

/**
 * Directive <pagination-pages> for displaying page links.
 */
paginationModule.directive('paginationPages', function () {
    return {
        restrict: 'E',
        compile: function (element, attrs, transclude) {
            var text = element.html();
            var attributeClass = (attrs.class != null ? (' ' + attrs.class) : '');
            var html =
                '<div class="pagination-pages' + attributeClass + '" style="text-align: right;">' +
                '<div ng-hide="pages.length == 1">' + text + ' ' +
                '<span ng-repeat="page in pages">' +
                '<a ng-hide="page.active" class="page" href="" ng-click="setPage($index)">{{$index + 1}}</a>' +
                '<span ng-show="page.active" class="page">{{$index + 1}}</span>' +
                '</span>' +
                '</div>' +
                '</div>' +
                '<div style="clear: right;"></div>';
            element.replaceWith(html);
        }
    }
});

/**
 * Directive <pagination-sort> for displaying link for sorting by a single column.
 */
paginationModule.directive('paginationSort', function () {
    return {
        restrict: 'E',
        compile: function (element, attrs, transclude) {
            var body = element.html();
            var column = attrs.column;
            var html =
                '<div style="display: inline-block;">' +
                '<a href="" ng-click="setSort(\'' + column + '\', $event)">' + body + '</a>' +
                '&nbsp;' +
                '<span class="fa fa-chevron-up" ng-show="sort == \'' + column + '\' && !sortDesc"></span>' +
                '<span class="fa fa-chevron-down" ng-show="sort == \'' + column + '\' && sortDesc"></span>' +
                '</div>';
            element.replaceWith(html);
        }
    }
});

/**
 * Directive <pagination-sort-default> for link to set default sorting.
 */
paginationModule.directive('paginationSortDefault', function () {
    return {
        restrict: 'E',
        compile: function (element, attrs, transclude) {
            var text = element.html();
            var attributeClass = (attrs.class != null ? attrs.class : '');
            var html =
                '<div class="' + attributeClass + '">' +
                '<a class="pull-right bordered" href="" ng-click="setSort()" title="' + text + '">' +
                '<i class="fa fa-disable-sorting"></i>' +
                '</a>' +
                '</div>';
            element.replaceWith(html);
        }
    }
});
