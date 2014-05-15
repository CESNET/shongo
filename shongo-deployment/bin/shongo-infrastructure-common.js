/**
 * Requirements:
 * - node.js (http://sekati.com/etc/install-nodejs-on-debian-squeeze)
 * - npm install exec-sync
 */
var sys = require('sys');
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
     * @param mode
     * @param valueByMode
     * @returns value from {@code valueByMode} for given {@code mode}
     */
    select: function(mode, valueByMode) {
        if (valueByMode[mode] != null) {
            return valueByMode[mode];
        }
        else {
            return valueByMode["default"];
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
            if (resource.type == "value") {
                appendAttribute("patternPrefix", this.Color.YELLOW);
            }
            output += ")";
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
                }
            }
            if (resource.type == "mcu") {
                appendBreak();
                appendAttribute("number", this.Color.YELLOW, resource.aliases.number);
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

    getResourceIdByName: function(resourceName) {
        var result = JSON.parse(this.execClientCliCommand("list-resources -name " + resourceName));
        if (result == null || result.length != 1) {
            return null;
        }
        return result[0].id;
    },

    getReservationRequestId: function(description, resourceId) {
        var result = this.execClientCliCommand("list-reservation-requests -description '" + description +"' -resource " + resourceId);
        if (result == null || result.length == 0) {
            return null;
        }
        result = JSON.parse(result);
        if (result == null || result.length != 1) {
            return null;
        }
        return result[0].id;
    },

    bookValues: function (resourceName, description, values) {
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
            console.log("Modifying existing reservation request " + reservationRequestId + " for values...");
            command = "modify-reservation-request " + reservationRequestId + " " + JSON.stringify(reservationRequest);
        }
        var result = this.execClientCliCommand(command);
        console.log(result);
    },

    execClientCliCommand: function(command) {
        var clientCliBin = __dirname + "/shongo-client-cli.sh --scripting";
        var clientCliCommand = "--cmd \"" + command.replace(/"/g, "\\\"") + "\"";
        var exec = clientCliBin + " " + clientCliCommand;
        return this.exec(exec);
    },

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