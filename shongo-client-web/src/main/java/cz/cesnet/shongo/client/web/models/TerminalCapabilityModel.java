package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.api.TerminalCapability;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class TerminalCapabilityModel {

    private List<Alias> aliases = new LinkedList<Alias>();

    public TerminalCapabilityModel() {
    }

    public List<Alias> getAliases() {
        return aliases;
    }

    public void setAliases(List<Alias> aliases) {
        this.aliases = aliases;
    }

    public TerminalCapability toApi() {
        TerminalCapability terminalCapability = new TerminalCapability();
        for (Alias alias: getAliases()) {
            terminalCapability.addAlias(alias);
        }

        return terminalCapability;
    }
}
