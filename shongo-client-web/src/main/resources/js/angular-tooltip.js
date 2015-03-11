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
            var content = element.attr('content');
            if (content != null) {
                var callback = function(event, api){
                    if (callback.value == null) {
                        callback.value = scope.$eval(callback.expression, {
                            event: {
                                setResult: function(result, persistent) {
                                    callback.value = result;
                                    callback.resetValue = (persistent != null && persistent != true);
                                    api.set('content.text', callback);
                                }
                            }
                        });
                    }
                    var result = callback.value;
                    if (callback.resetValue) {
                        callback.value = null;
                    }
                    return result;
                };
                callback.expression = content;
                callback.value = null;
                content = callback;
            }
            else {
                content = element.attr('title');
                if (content == null) {
                    content = element.next().html();
                }
                if (content.indexOf("{{") != -1 || content.indexOf(" ng-") != -1) {
                    content = $compile(content)(scope);
                }
            }
            var options = {
                content: {
                    text: content
                },
                position: {
                    my: 'top left',
                    at: 'bottom right'
                },
                show: {
                    solo: true
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
