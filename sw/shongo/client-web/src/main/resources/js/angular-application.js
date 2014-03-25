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

applicationModule.controller("DynamicContentController", function($scope, $element, $timeout) {
    /**
     * Specifies whether dynamic content has already been loaded.
     */
    $scope.contentLoaded = false;

    /**
     * Refresh content.
     */
    $scope.refresh = function(contentUrl) {
        if (contentUrl != null) {
            $scope.contentUrl = contentUrl;
        }
        return true;
    };
});


/**
 * Tab controller.
 */
applicationModule.controller("TabController", function($scope, $element, $timeout) {
    /**
     * Id
     */
    $scope.id = $element.attr('id');

    /**
     * Url from which the tab content should be loaded.
     */
    $scope.contentUrl = null;

    /**
     * Specifies whether tab has been initialized (displayed).
     * @type {boolean}
     */
    $scope.inited = false;

    /**
     * Specifies whether tab content has already been loaded.
     */
    $scope.contentLoaded = false;

    /**
     * Initialize this tab in parent scope.
     */
    if ($scope.$parent != null && $scope.$parent.onCreateTab != null) {
        $scope.$parent.onCreateTab($scope.id, $scope);
    }

    /**
     * Watch for activation/deactivation of tab.
     */
    $scope.$watch("active", function(active) {
        if (active) {
            if ($scope.disabled) {
                $scope.active = false;
                return;
            }
            if (!$scope.contentLoaded) {
                $scope.refresh();
                $scope.contentLoaded = true;
            }
            if ($scope.$parent != null && $scope.$parent.onActivateTab != null) {
                $scope.$parent.onActivateTab($scope.id, $scope);
            }
            $scope.inited = true;
        }
    });

    /**
     * Refresh tab content.
     */
    $scope.refresh = function(contentUrl) {
        if (contentUrl != null) {
            $scope.contentUrl = contentUrl;
        }
    };
});

/**
 * URL for dynamic tab content.
 */
applicationModule.directive('contentUrl', function ($http, $compile) {
    var SCRIPT_PATTERN = /\s*<script((?!<\/script>)[\s\S])*<\/script>/;
    var index = 0;
    return {
        restrict: 'A',
        scope: false,
        link: function(scope, element, attrs) {
            // Check if "content-url" attribute is on <tab>
            var isTab = scope.$transcludeFn != null;

            // Determine content element id
            scope.contentElementId = (isTab ? null : element.attr("id"));
            if (scope.contentElementId == null) {
                scope.contentElementId = "__contentElement" + (++index);
            }
            scope.contentUrl = attrs.contentUrl;
            if (attrs.contentLoaded != null) {
                scope.contentLoaded = attrs.contentLoaded;
            }

            // Create content element
            var contentElement = $compile("<div id ='" + scope.contentElementId + "'><div class='spinner'></div></div>")(scope.$parent);
            if (scope.$transcludeFn != null) {
                // Init tab content (replace the content element in the transclude)
                scope.$transcludeFn = function(scope, callback) {
                    callback(contentElement);
                };
            }
            else {
                // Init div content
                if (scope.contentLoaded) {
                    // Modify existing content element id
                    element.attr("id", scope.contentElementId);
                }
                else {
                    // Replace content element
                    element.replaceWith(contentElement);
                }
            }

            // Load content for the first time
            if (!scope.contentLoaded && (scope.inited == null || scope.inited == true)) {
                loadContent(scope);
            }

            // Bind to scope refresh
            var defaultRefresh = scope.refresh;
            scope.refresh = function(contentUrl) {
                defaultRefresh(contentUrl);
                loadContent(scope);
            };
        }
    };

    /**
     * Load content from given scope.
     *
     * @param scope
     */
    function loadContent(scope){
        // Hide all tooltips
        $('.qtip-app').qtip('hide');
        // Hide all select2 drop boxes
        $('.select2-drop').hide();

        // Load new content
        $http.get(scope.contentUrl).success(function (html) {
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
            var oldElement = $("#" + scope.contentElementId);
            var newElement = $compile("<div>" + html + "</div>")(scope.$parent);
            oldElement.empty();
            oldElement.append(newElement);
        });
    }
});