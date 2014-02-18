var applicationModule = angular.module('ngApplication', ['ui.bootstrap']);

/**
 * Global Configuration.
 */
applicationModule.config(['$httpProvider', function($httpProvider) {
    // Send header "X-Requested-With"
    $httpProvider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';
}]);

/**
 * Service $application.
 */
applicationModule.factory("$application", function() {
    return {
        formatUsers: function(users, emptyText, maximumCount) {
            var content = "";
            for (var index = 0; index < users.length; index++) {
                var user = users[index];
                if (content != '') {
                    content += ', ';
                }
                content += user.fullName;
                if (maximumCount != null && index > maximumCount) {
                    content += ', ...';
                    break;
                }
            }
            if (content == "") {
                content = emptyText;
            }
            return content;
        },
        handleAjaxFailure: function(response) {
            if (response.status == 401) {
                // User login timeout and thus refresh
                window.location.reload();
                return true;
            }
            return false;
        }
    };
});

/**
 * Tab controller.
 */
applicationModule.controller("TabController", function($scope, $element) {
    /**
     * Id
     */
    $scope.id = $element.attr('id');

    /**
     * Url from which the tab content should be loaded.
     */
    $scope.contentUrl = null;

    /**
     * Specifies whether tab content has already been initialized.
     */
    $scope.inited = false;

    /**
     * Initialize this tab in parent scope.
     */
    if ($scope.$parent.onInitTab != null) {
        $scope.$parent.onInitTab($scope.id, $scope);
    }

    /**
     * Watch for activation/deactivation of tab.
     */
    $scope.$watch("active", function(active) {
        if (active) {
            if (!$scope.inited) {
                $scope.init();
                $scope.inited = true;
            }
            else {
                $scope.refresh();
            }
            if ($scope.$parent != null && $scope.$parent.onActivateTab != null) {
                $scope.$parent.onActivateTab($scope.id);
            }
        }
    });

    /**
     * Initialize tab content.
     */
    $scope.init = function() {
    };

    /**
     * Refresh tab content.
     */
    $scope.refresh = function() {
    };
});

/**
 * URL for dynamic tab content.
 */
applicationModule.directive('contentUrl', function ($http, $compile) {
    var SCRIPT_PATTERN = /\s*<script((?!<\/script>)[\s\S])*<\/script>/;
    return {
        restrict: 'A',
        scope: false,
        link: function(scope, element, attrs) {
            // Create content element and replace it in the transclude
            var contentElement = $compile("<div class='spinner'></div>")(scope.$parent);
            scope.$transcludeFn = function(scope, callback) {
                callback(contentElement);
            };

            // Init or schedule the init for later
            if (!scope.inited) {
                scope.$watch('inited', function(newInited, oldInited) {
                    if (newInited) {
                        loadContent(attrs.contentUrl, contentElement, scope);
                    }
                });
            }
            else {
                loadContent(attrs.url, contentElement, scope);
            }
        }
    };
    function loadContent(url, element, scope){
        $http.get(url).success(function (html) {
            // Extract scripts from html
            var scripts = "";
            var result = SCRIPT_PATTERN.exec(html);
            while (result != null) {
                scripts += result[0];
                scripts += "\n";
                html = html.substring(0, result.index) + html.substring(result.index + result[0].length);
                result = SCRIPT_PATTERN.exec(html);
            }

            // Append scripts to head
            $("head").append(scripts);

            // Replace element content
            var elementContent = $compile("<div>" + html + "</div>")(scope.$parent);
            element.replaceWith(elementContent);
        });
    }
});