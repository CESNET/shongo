/**
 * @returns {boolean} whether this application is supported in current browser
 */
window.isBrowserSupported = function() {
    if (typeof JSON !== 'undefined'         // IE7 doesn't have a native JSON parser
            && 'addEventListener' in window // IE8 doesn't have "window.addEventListener"
            && 'querySelector' in document  // JQuery requires "document.querySelector"
            && 'console' in window          // Application requires "window.console"
            && 'log' in window.console      // Application requires "window.console.log"
        ) {
        return true;
    }
};

/**
 * Supply "console.log" as "console.debug" if it doesn't exist.
 */
if (window.console != null && window.console.log != null && window.console.debug == null) {
    window.console.debug = window.console.log;
}