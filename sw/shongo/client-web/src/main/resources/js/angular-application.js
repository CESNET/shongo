var applicationModule = angular.module('ngApplication', ['ui.bootstrap']);

// Configuration
applicationModule.config(['$httpProvider', function($httpProvider) {
    // Send header "X-Requested-With"
    $httpProvider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';
}]);

// Service $application
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