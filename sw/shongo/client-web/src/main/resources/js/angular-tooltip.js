/**
 * Date/Time module.
 */
var tooltipModule = angular.module('ngTooltip', []);

/**
 * Date/Time picker
 */
tooltipModule.directive('tooltip', function() {
    return {
        restrict: 'A',
        link: function postLink(scope, element, attrs, controller) {
            var tooltip = $("#" + attrs.tooltip);
            var timeout;
            var bind = {
                mouseover: function(){
                    $(this).next().fadeIn();
                    clearInterval(timeout);
                },
                mouseleave: function(){
                    timeout = setTimeout(function(){
                        $(this).next().fadeOut();
                        tooltip.fadeOut();
                    }, 200);
                }
            };
            element.bind(bind);
            tooltip.bind(bind);
        }
    }
});
