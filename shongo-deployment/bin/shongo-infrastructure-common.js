/**
 * Requirements:
 * - node.js (http://sekati.com/etc/install-nodejs-on-debian-squeeze)
 * - npm install exec-sync
 */
var sys = require("sys");
var exec = require("exec-sync");

/**
 * Module exports.
 */
module.exports = {
    /**
     * Available colors for {@link formatColored}.
     */
    Color: {
        RESET: "0",
        BRIGHT: "1",
        DIM: "2",
        UNDERSCORE: "4",
        BLINK: "5",
        REVERSE: "7",
        HIDDEN: "8",
        BLACK: "30",
        RED: "31",
        GREEN: "32",
        YELLOW: "33",
        BLUE: "34",
        MAGENTA: "35",
        CYAN: "36",
        WHITE: "37",
        BgBlack: "40",
        BgRed: "41",
        BgGreen: "42",
        BgYellow: "43",
        BgBlue: "44",
        BgMagenta: "45",
        BgCyan: "46",
        BgWhite: "47"
    },

    /**
     * @returns {string}
     */
    getBin: function() {
        return process.argv[0] + " " + process.argv[1];
    },

    /**
     *
     * @returns {Array|string|Blob}
     */
    getArguments: function() {
        return process.argv.slice(2);
    },

    /**
     * @param index
     * @returns true when argument at given {@code index} exits, false otherwise
     */
    hasArgument: function(index) {
        return (process.argv.length - 2) > index;
    },

    /**
     * @param index
     * @returns argument value at given {@code index}
     */
    getArgument: function(index) {
        return process.argv[index + 2];
    },

    /**
     * Wait for key press.
     */
    waitForKeyPress: function(message) {
        if (message == null) {
            message = "Press any key to continue...";
        }
        sys.print(message);
        var fs = require('fs');
        var fd = fs.openSync( "/dev/stdin", "rs" );
        fs.readSync(fd, new Buffer(1), 0, 1);
        fs.closeSync(fd );
    },

    /**
     * @param object to be dumped
     * @param indent if the result should be indented
     */
    dumpObject: function(object, indent) {
        if (indent) {
            object = JSON.stringify(object, null, 4);
        }
        else {
            object = require('util').inspect(object, true, 10);
        }
        console.log(this.formatColored(object, this.Color.DIM));
    },

    /**
     * @param key
     * @param valueByKey
     * @returns value from {@code valueByKey} for given {@code key}
     */
    select: function(key, valueByKey) {
        if (valueByKey[key] != null) {
            return valueByKey[key];
        }
        else {
            return valueByKey["default"];
        }
    },

    /**
     * Format given {@code text} to given {@code color}.
     *
     * @param text
     * @param color
     * @returns {string}
     */
    formatColored: function(text, color) {
        if (color != null) {
            return "\x1b[" + color + "m" + text + "\x1b[0m";
        }
        else {
            return text;
        }
    },

    /**
     * Format given {@code resources} to colored string.
     *
     * @param resources
     * @returns {string}
     */
    formatResources: function(resources) {
        var output = "";
        for (var index = 0; index < resources.length; index++) {
            var resource = resources[index];

            // Append attribute to {@link output} variable.
            function appendBreak() {
                output += "\n    ";
            }
            function appendAttribute(name, color, value) {
                if (value == null) {
                    value = resource[name];
                }
                if (output[output.length - 1] != "(" && output[output.length - 1] != " ") {
                    output += ", ";
                }
                output += name;
                output += ": ";
                if (color != null) {
                    output += module.exports.formatColored(value, color);
                }
                else {
                    output += value;
                }
            }
            if (output.length > 0) {
                output += "\n";
            }

            // Format resource
            output += (index + 1) + ") ";
            output += this.formatColored(resource.name, this.Color.GREEN);
            output += " - " + resource.type.toUpperCase() + " (";
            if (resource.agent != null) {
                appendAttribute("agent", this.Color.GREEN);
            }
            if (resource.allocationOrder != null) {
                appendAttribute("order", this.Color.RED, resource.allocationOrder);
            }
            if (resource.maximumFuture != null) {
                appendAttribute("maxFuture", this.Color.RED, resource.maximumFuture);
            }
            if (resource.type == "value") {
                appendAttribute("patternPrefix", this.Color.YELLOW);
            }
            output += ")";
            if (resource.allocatable == 0) {
                output += this.formatColored(" [not-allocatable]", this.Color.RED);
            }
            // Format resource description
            if (resource.type == "mcu" || resource.type == "connect" || resource.type == "tcs") {
                appendBreak();
                appendAttribute("address", this.Color.RED);
                appendBreak();
                appendAttribute("licenseCount", this.Color.RED);
            }
            if (resource.type == "mcu" || resource.type == "connect") {
                if (resource.aliases != null && resource.aliases.namePrefix != "") {
                    appendBreak();
                    appendAttribute("namePrefix", this.Color.YELLOW, resource.aliases.namePrefix);
                    appendBreak();
                    appendAttribute("nameValueProvider", this.Color.YELLOW, resource.aliases.nameValueProvider);
                }
            }
            if (resource.type == "mcu") {
                appendBreak();
                appendAttribute("number", this.Color.YELLOW, resource.aliases.number);
                appendBreak();
                appendAttribute("domain", this.Color.YELLOW, resource.aliases.domain);
            }
            if (resource.administrators != null) {
                var administrators = "";
                for (var administratorIndex = 0; administratorIndex < resource.administrators.length; administratorIndex++) {
                    var administrator = resource.administrators[administratorIndex];
                    if (administrators.length > 0) {
                        administrators += ", ";
                    }
                    if (administrator.userId != null) {
                        administrators += "user:" + administrator.userId;
                    }
                    else {
                        administrators += administrator.email;
                    }
                }
                appendBreak();
                appendAttribute("administrators", this.Color.BLUE, administrators);
            }

            output = output.trim();
        }
        return output;
    },

    /**
     * @param resourceName
     * @return resourceId for given {@code resourceName}
     */
    getResourceIdByName: function(resourceName) {
        var result = this.execClientCliCommandJsonResult("list-resources -name " + resourceName);
        if (result == null || result.length != 1) {
            return null;
        }
        return result[0].id;
    },

    /**
     * @param resourceId
     * @return resource for given {@code resourceId}
     */
    getResource: function(resourceId) {
        var result = this.execClientCliCommandJsonResult("get-resource " + resourceId);
        if (result == null) {
            return null;
        }
        return result;
    },

    /**
     * @param description
     * @param resourceId
     * @return reservationRequestId for given {@code description} and {@coe resourceId}
     */
    getReservationRequestId: function(description, resourceId) {
        var result = this.execClientCliCommandJsonResult("list-reservation-requests -description '" + description +"' -resource " + resourceId);
        if (result == null || result.length != 1) {
            return null;
        }
        return result[0].id;
    },

    /**
     * Create or update given {@code resource}.
     */
    mergeResource: function(resource, onlyDump) {
        var resourceName = resource.name;
        var resourceId = this.getResourceIdByName(resourceName);
        var oldResource = null;
        if (resourceId != null) {
            oldResource = this.getResource(resourceId);
        }
        var resourceAdministrators = resource.administrators;

        resource.id = resourceId;
        switch (resource.type) {
            case "value":
                resource = this.prepareResourceValue(resource, oldResource);
                break;
            case "connect":
                resource = this.prepareResourceConnect(resource, oldResource);
                break;
            case "mcu":
                resource = this.prepareResourceMcu(resource, oldResource);
                break;
            case "tcs":
                resource = this.prepareResourceTcs(resource, oldResource);
                break;
            default:
                throw "Unknown resource type '" + resource.type + "'.";
        }

        var command;
        if (resourceId == null) {
            console.log("Creating resource '" + resourceName + "'...");
            command = "create-resource " + JSON.stringify(resource);
        }
        else {
            console.log("Updating resource '" + resourceName + "' with id '" + resourceId + "'...");
            command = "modify-resource " + resourceId + " " + JSON.stringify(resource);
        }
        if (onlyDump) {
            this.dumpObject(command);
        }
        else {
            console.log(this.execClientCliCommand(command));
        }

        // Manage ACL records for resource administrators
        var resourceAdministratorUserIds = null;
        if (resourceAdministrators != null && resourceAdministrators.length > 0) {
            resourceAdministratorUserIds = {};
            for (var index = 0; index < resourceAdministrators.length; index++) {
                var administrator = resourceAdministrators[index];
                if (administrator.userId != null) {
                    resourceAdministratorUserIds[administrator.userId] = true;
                }
            }
        }
        if (resourceAdministratorUserIds != null) {
            // Get resource id
            if (resourceId == null) {
                resourceId = this.getResourceIdByName(resourceName);
                if (resourceId == null) {
                    throw "Resource with name'" + resourceName + "' doesn't exist."
                }
            }

            // List existing acl-records and remove them
            var aclEntries = this.execClientCliCommandJsonResult("list-acl -object " + resourceId);
            if (aclEntries != null && aclEntries.length > 0) {
                for (var index = 0; index < aclEntries.length; index++) {
                    var aclEntry = aclEntries[index];
                    var aclEntryPrincipalId = aclEntry.identityPrincipalId;
                    if (aclEntry.identityType == "USER" && aclEntry.role == "OWNER") {
                        if (resourceAdministratorUserIds[aclEntryPrincipalId]) {
                            console.log("Resource '" + resourceName + "': User administrator '" + aclEntryPrincipalId + "' already exists...");
                            delete resourceAdministratorUserIds[aclEntryPrincipalId];
                        }
                        else {
                            var commandDeleteAcl = "delete-acl " + aclEntry.id;
                            console.log("Resource '" + resourceName + "': Deleting user administrator '" + aclEntryPrincipalId + "'...");
                            if (onlyDump) {
                                this.dumpObject(commandDeleteAcl);
                            }
                            else {
                                console.log(this.execClientCliCommand(commandDeleteAcl));
                            }
                        }
                    }
                }

            }

            // Create the rest of resource administrator acl entries
            for (var resourceAdministratorUserId in resourceAdministratorUserIds) {
                var commandCreateAcl = "create-acl " + resourceAdministratorUserId + " " + resourceId + " OWNER";
                console.log("Resource '" + resourceName + "': Creating user administrator '" + resourceAdministratorUserId + "'...");
                if (onlyDump) {
                    this.dumpObject(commandCreateAcl);
                }
                else {
                    console.log(this.execClientCliCommand(commandCreateAcl));
                }
            }
        }
    },

    /**
     * @param resourceTarget to be initialized
     * @param resourceSource to get attributes
     */
    prepareResourceCommon: function(resourceTarget, resourceSource) {
        if (resourceTarget.class == null) {
            resourceTarget.class = "Resource";
        }
        if (resourceSource.id != null) {
            resourceTarget.id = resourceSource.id;
        }
        resourceTarget.name = resourceSource.name;
        if (resourceSource.description != null) {
            resourceTarget.description = resourceSource.description;
        }
        if (resourceSource.allocatable != null) {
            resourceTarget.allocatable = resourceSource.allocatable;
        }
        if (resourceTarget.allocatable == null) {
            resourceTarget.allocatable = 1;
        }
        if (resourceSource.allocationOrder != null) {
            resourceTarget.allocationOrder = resourceSource.allocationOrder;
        }
        if (resourceSource.maximumFuture != null) {
            resourceTarget.maximumFuture = resourceSource.maximumFuture;
        }
        if (resourceSource.address != null) {
            resourceTarget.address = resourceSource.address;
        }
        if (resourceSource.agent != null) {
            resourceTarget.mode = {
                class : "ManagedMode",
                connectorAgentName: resourceSource.agent
            };
        }
        if (resourceTarget.capabilities == null) {
            resourceTarget.capabilities = [];
        }
        if (resourceSource.administrators != null && resourceSource.administrators.length > 0) {
            resourceTarget.administratorEmails = [];
            for (var index = 0; index < resourceSource.administrators.length; index++) {
                var administrator = resourceSource.administrators[index];
                if (administrator.email != null) {
                    resourceTarget.administratorEmails.push(administrator.email);
                }
            }
        }
    },

    /**
     * Prepare value provider resource.
     */
    prepareResourceValue: function(resource, oldResource) {
        // Get old resource components
        var oldValueProviderCapability = {};
        var oldValueProvider = {};
        if (oldResource != null) {
            for (var index = 0; index < oldResource.capabilities.length; index++) {
                var capability = oldResource.capabilities[index];
                if (capability.class == "ValueProviderCapability") {
                    oldValueProviderCapability = capability;
                    if (oldValueProviderCapability.valueProvider != null) {
                        oldValueProvider = oldValueProviderCapability.valueProvider;
                    }
                }
            }
        }

        // Prepare resource
        var resourceValue = {};
        this.prepareResourceCommon(resourceValue, resource);
        resourceValue.capabilities.push({
            class: "ValueProviderCapability",
            id: oldValueProviderCapability.id,
            valueProvider : {
                class : "ValueProvider.Pattern",
                id: oldValueProvider.id,
                allowAnyRequestedValue: "1",
                patterns : [
                    resource.patternPrefix + "-{hash}"
                ]
            }
        });
        return resourceValue;
    },

    /**
     * Prepare Adobe Connect resource.
     */
    prepareResourceConnect: function(resource, oldResource) {
        // Get old resource components
        var oldRoomProviderCapability = {};
        var oldRecordingCapability = {};
        var oldUriAliasProviderCapability = {};
        var oldUriValueProvider = {};
        if (oldResource != null) {
            for (var index = 0; index < oldResource.capabilities.length; index++) {
                var capability = oldResource.capabilities[index];
                if (capability.class == "RoomProviderCapability") {
                    oldRoomProviderCapability = capability;
                }
                else if (capability.class == "RecordingCapability") {
                    oldRecordingCapability = capability;
                }
                else if (capability.class == "AliasProviderCapability") {
                    oldUriAliasProviderCapability = capability;
                    if (oldUriAliasProviderCapability.valueProvider != null) {
                        oldUriValueProvider = oldUriAliasProviderCapability.valueProvider;
                    }
                }
            }
        }

        // Prepare requirements
        var aliasesValueProvider = null;
        var aliasesNamePrefix = "";
        if (resource.aliases != null) {
            if (resource.aliases.namePrefix != null) {
                aliasesNamePrefix = resource.aliases.namePrefix;
            }
            if (resource.aliases.nameValueProvider != null) {
                aliasesValueProvider = this.getResourceIdByName(resource.aliases.nameValueProvider);
            }
        }
        if (aliasesValueProvider == null) {
            throw "Name ValueProvider for resource '" + resource.name + "' doesn't exists";
        }

        // Prepare resource
        var resourceConnect =  {
            class: "DeviceResource",
            technologies: ["ADOBE_CONNECT"]
        };
        this.prepareResourceCommon(resourceConnect, resource);
        resourceConnect.capabilities.push({
            class: "RoomProviderCapability",
            id: oldRoomProviderCapability.id,
            licenseCount: resource.licenseCount,
            requiredAliasTypes: ["ROOM_NAME"]
        });
        resourceConnect.capabilities.push({
            class: "RecordingCapability",
            id: oldRecordingCapability.id
        });
        resourceConnect.capabilities.push({
            class: "AliasProviderCapability",
            id: oldUriAliasProviderCapability.id,
            valueProvider: {
                class: "ValueProvider.Filtered",
                id: oldUriValueProvider.id,
                filterType: "CONVERT_TO_URL",
                valueProvider: aliasesValueProvider
            },
            aliases: [
                { type: "ROOM_NAME", value: aliasesNamePrefix + "{requested-value}" },
                { type: "ADOBE_CONNECT_URI", value: "{device.address}/" + aliasesNamePrefix + "{value}" }
            ],
            restrictedToResource: 1
        });
        return resourceConnect;
    },

    /**
     * Prepare Cisco MCU resource.
     */
    prepareResourceMcu: function(resource, oldResource) {
        // Get old resource components
        var oldRoomProviderCapability = {};
        var oldRoomNameAliasProviderCapability = {};
        var oldNumberAliasProviderCapability = {};
        var oldNumberValueProvider = {};
        if (oldResource != null) {
            for (var index = 0; index < oldResource.capabilities.length; index++) {
                var capability = oldResource.capabilities[index];
                if (capability.class == "RoomProviderCapability") {
                    oldRoomProviderCapability = capability;
                }
                else if (capability.class == "RecordingCapability") {
                    oldRecordingCapability = capability;
                }
                else if (capability.class == "AliasProviderCapability") {
                    if (capability.aliases.length == 1 && capability.aliases[0].type == "ROOM_NAME") {
                        oldRoomNameAliasProviderCapability = capability;
                    }
                    else {
                        oldNumberAliasProviderCapability = capability;
                        if (oldNumberAliasProviderCapability.valueProvider != null) {
                            oldNumberValueProvider = oldNumberAliasProviderCapability.valueProvider;
                        }
                    }
                }
            }
        }

        // Prepare requirements
        var aliasesValueProvider = null;
        var aliasesNamePrefix = "";
        var aliasesNumberRange = "";
        var aliasesNumberPrefix = "";
        if (resource.aliases != null) {
            if (resource.aliases.namePrefix != null) {
                aliasesNamePrefix = resource.aliases.namePrefix;
            }
            if (resource.aliases.nameValueProvider != null) {
                aliasesValueProvider = this.getResourceIdByName(resource.aliases.nameValueProvider);
            }
            if (resource.aliases.number != null) {
                var numberSplitted = resource.aliases.number.replace(/(\[|\])/g, " ").trim().split(" ");
                if (numberSplitted.length == 1) {
                    aliasesNumberRange = numberSplitted[0];
                }
                else if (numberSplitted.length == 2) {
                    aliasesNumberPrefix = numberSplitted[0];
                    aliasesNumberRange = numberSplitted[1];
                }
                else {
                     throw "Illegal format of number '" + resource.aliases.number + "'.";
                }
            }
        }
        if (aliasesValueProvider == null) {
            throw "Name ValueProvider for resource '" + resource.name + "'doesn't exists";
        }

        // Prepare resource
        var resourceMcu = {
            class: "DeviceResource",
            technologies: ["H323","SIP"]
        };
        this.prepareResourceCommon(resourceMcu, resource);
        resourceMcu.capabilities.push({
            class: "RoomProviderCapability",
            id: oldRoomProviderCapability.id,
            licenseCount: resource.licenseCount,
            requiredAliasTypes: ["ROOM_NAME", "H323_E164"]
        });
        resourceMcu.capabilities.push({
            class: "AliasProviderCapability",
            id: oldRoomNameAliasProviderCapability.id,
            valueProvider: aliasesValueProvider,
            aliases: [
                { type: "ROOM_NAME", value: aliasesNamePrefix + "{value}" }
            ],
            restrictedToResource: 1
        });
        resourceMcu.capabilities.push({
            class: "AliasProviderCapability",
            id: oldNumberAliasProviderCapability.id,
            valueProvider: {
                class: "ValueProvider.Pattern",
                id: oldNumberValueProvider.id,
                patterns: ["{number:" + aliasesNumberRange + "}"]
            },
            aliases: [
                { type: "H323_E164", value: aliasesNumberPrefix + "{value}" },
                { type: "H323_URI", value: aliasesNumberPrefix + "{value}@{device.address}" },
                { type: "H323_IP", value: "195.113.222.60 {value}#" },
                { type: "SIP_URI", value: aliasesNumberPrefix + "{value}@cesnet.cz" },
                { type: "SIP_IP", value: "195.113.222.60 {value}#" }
            ],
            restrictedToResource: 1
        });
        return resourceMcu;
    },

    /**
     * Prepare Cisco TCS resource.
     */
    prepareResourceTcs: function(resource, oldResource) {
        // Get old resource components
        var oldRecordingCapability = {};
        if (oldResource != null) {
            for (var index = 0; index < oldResource.capabilities.length; index++) {
                var capability = oldResource.capabilities[index];
                if (capability.class == "RecordingCapability") {
                    oldRecordingCapability = capability;
                }
            }
        }

        // Prepare resource
        var resourceTcs = {
            class: "DeviceResource",
            technologies: ["H323", "SIP"]
        };
        this.prepareResourceCommon(resourceTcs, resource);
        resourceTcs.capabilities.push({
            class: "RecordingCapability",
            id: oldRecordingCapability.id,
            licenseCount: resource.licenseCount
        });
        return resourceTcs;
    },

    /**
     * Create or update reservation request for values with given {@code description} and for resource with given {@code resourceName}.
     *
     * @param resourceName
     * @param description
     * @param values
     */
    bookValues: function(resourceName, description, values) {
        console.log("Booking values for '" + resourceName + "'...");
        var resourceId = this.getResourceIdByName(resourceName);
        if (resourceId == null) {
            throw "Resource with name '" + resourceName + "' cannot be found.";
        }
        var reservationRequestId = this.getReservationRequestId(description, resourceId);
        var reservationRequest = {
            id: reservationRequestId,
            class: 'ReservationRequest',
            purpose: 'OWNER',
            priority: 1,
            slot: '*/*',
            description: description,
            specification: {
                class: 'ValueSpecification',
                resourceId: resourceId,
                values: values
            }
        };
        var command;
        if (reservationRequestId == null) {
            console.log("Creating new reservation request for values...");
            command = "create-reservation-request " + JSON.stringify(reservationRequest);
        }
        else {
            console.log("Modifying existing reservation request '" + reservationRequestId + "' with values...");
            command = "modify-reservation-request " + reservationRequestId + " " + JSON.stringify(reservationRequest);
        }
        var result = this.execClientCliCommand(command);
        console.log(result);
    },

    /**
     * @param command to be executed by shongo-client-cli
     * @returns result from command
     */
    execClientCliCommand: function(command) {
        var clientCliBin = __dirname + "/shongo-client-cli.sh --scripting";
        var clientCliCommand = "--cmd \"" + command.replace(/"/g, "\\\"") + "\"";
        var exec = clientCliBin + " " + clientCliCommand;
        var result = this.exec(exec);
        if (result.trim().length == 0) {
            return null;
        }
        return result;
    },

    /**
     * @param command to be executed by shongo-client-cli
     * @returns result from command
     */
    execClientCliCommandJsonResult: function(command) {
        var result = this.execClientCliCommand(command);
        if (result == null) {
            return null;
        }
        return JSON.parse(result);
    },

    /**
     * @param command to be executed in shell
     * @param stderrCallback callback to receive stderr
     * @returns result
     */
    exec: function(command, stderrCallback) {
        var result = exec(command, true);
        var error = result.stderr.trim();
        if (error.length > 0) {
            if (stderrCallback != null) {
                stderrCallback(error);
            }
            else {
                console.error(error);
            }
        }
        return result.stdout;
    }
};