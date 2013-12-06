/**
 * Tooltip module.
 */
var tooltipModule = angular.module('ngTooltip', []);

// Current active tooltip
tooltipModule.activeTooltipContext = null;

/**
 * Date/Time picker
 */
tooltipModule.directive('tooltip', function($compile) {
    return {
        restrict: 'A',
        link: function(scope, element) {
            var title = element.attr('title');
            if (title == null) {
                title = element.next().html();
            }
            if (title.indexOf("{{") != -1 || title.indexOf(" ng-") != -1) {
                title = $compile(title)(scope);
            }
            var options = {
                content: {
                    text: title
                },
                position: {
                    my: 'top left',
                    at: 'bottom right'
                },
                style: {
                    classes: 'qtip-app'
                }
            };
            var width = element.attr('tooltip-width');
            if ( width != null && width != "" ) {
                options["style"]["classes"] = options["style"]["classes"] + " qtip-app-width";
                options["style"]["width"] = width;
            }
            var position = element.attr('position');
            if (position == "bottom-left") {
                options["position"] = {
                    my: 'top right',
                    at: 'bottom left'
                };
            }
            var selectable = element.attr('selectable');
            if (selectable == "true") {
                options["hide"] = {
                    fixed: true,
                        delay: 300
                };
            }
            element.qtip(options);
        }
    }
});
