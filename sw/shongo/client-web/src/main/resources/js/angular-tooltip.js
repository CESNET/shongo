/**
 * Tooltip module.
 */
var tooltipModule = angular.module('ngTooltip', []);

// Current active tooltip
tooltipModule.activeTooltipContext = null;

/**
 * Date/Time picker
 */
tooltipModule.directive('tooltip', function() {
    return {
        restrict: 'A',
        link: function(scope, element, attrs, controller) {
            var tooltipContent;
            var tooltipContentContext;
            var bind = {
                mouseover: function(){
                    if (tooltipModule.activeTooltipContext != null && tooltipModule.activeTooltipContext != tooltipContentContext) {
                        tooltipModule.activeTooltipContext.tooltipContent.stop()
                        tooltipModule.activeTooltipContext.tooltipContent.hide();
                    }
                    tooltipModule.activeTooltipContext = tooltipContentContext;
                    tooltipContent.stop();
                    tooltipContent.init();
                    tooltipContent.fadeIn();
                    clearInterval(tooltipContentContext.timeout);
                },
                mouseleave: function(){
                    tooltipContentContext.timeout = setTimeout(function(){
                        tooltipContent.stop();
                        tooltipContent.fadeOut();
                    }, 200);
                }
            };
            setTimeout(function(){
                // Get the tooltip content
                tooltipContent = $("#" + attrs.tooltip);
                tooltipContent.init = function() {
                    if (tooltipContent.inited) {
                        return;
                    }
                    tooltipContent.inited = true;
                    if (attrs.position == "bottom-left") {
                        var marginLeft = parseInt(tooltipContent.css('margin-left'));
                        tooltipContent.css('margin-left', marginLeft - parseInt(tooltipContent.css('max-width')));
                        tooltipContent.css('margin-left', marginLeft - parseInt(tooltipContent.css('width')));
                    }
                };

                // Skip not existing and empty tooltip content
                if (tooltipContent.length == 0 || tooltipContent.children().length == 0) {
                    return;
                }

                // Bind the tooltip main label
                element.bind(bind);

                // Get the tooltip additional label
                var tooltipLabel = $("#" + attrs.label);
                if ( tooltipLabel.length > 0) {
                    tooltipLabel.addClass('tooltip-label');
                    tooltipLabel.bind(bind);
                }

                // Setup tooltip content
                if ( tooltipContent[0].context == null ) {
                    // Setup context
                    tooltipContent[0].context = {
                        tooltipContent: tooltipContent
                    };

                    // Bind the tooltip content also
                    tooltipContent.bind(bind);
                }
                tooltipContentContext = tooltipContent[0].context;
            }, 0);
        }
    }
});
