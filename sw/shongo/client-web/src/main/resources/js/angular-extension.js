/**
 * Angular define module if not exists.
 *
 * @param name
 * @param requires
 * @returns {*}
 */
angular.provideModule = function(name, requires, configFn) {
    try {
        return angular.module(name);
    }
    catch (exception) {
        return angular.module(name, requires, configFn);
    }
};