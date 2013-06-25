/**
 * Date/Time module.
 */
var tooltipModule = angular.module('ngTooltip', []);

// Current active tooltip
tooltipModule.activeTooltip = null;

/**
 * Date/Time picker
 */
tooltipModule.directive('tooltip', function() {
    return {
        restrict: 'A',
        link: function postLink(scope, element, attrs, controller) {
            var tooltipContent;
            var timeout;
            var bind = {
                mouseover: function(){
                    if (tooltipModule.activeTooltip != null && tooltipModule.activeTooltip != tooltipContent) {
                        tooltipModule.activeTooltip.stop()
                        tooltipModule.activeTooltip.hide();
                    }
                    tooltipModule.activeTooltip = tooltipContent;
                    tooltipContent.fadeIn();
                    clearInterval(timeout);
                },
                mouseleave: function(){
                    timeout = setTimeout(function(){
                        tooltipContent.fadeOut();
                    }, 200);
                }
            };
            // Bind the tooltip label
            element.bind(bind);
            // Get the tooltip content and bind it also
            setTimeout(function(){
                tooltipContent = $("#" + attrs.tooltip);
                tooltipContent.bind(bind);
            }, 0);
        }
    }
});
