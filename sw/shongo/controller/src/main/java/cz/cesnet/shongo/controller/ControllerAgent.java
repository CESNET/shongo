package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.common.jade.Agent;
import cz.cesnet.shongo.common.shell.CommandHandler;
import cz.cesnet.shongo.common.shell.CommandSet;
import cz.cesnet.shongo.common.shell.Shell;
import jade.core.AID;
import org.apache.commons.cli.CommandLine;

/**
 * Jade Agent for Domain Controller
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerAgent extends Agent
{
    public CommandSet createCommandSet()
    {
        CommandSet commandSet = new CommandSet();
        commandSet.addCommand("list", "List all connectors in the domain", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                AID[] agents = findAgentsByService("connector");
                for (AID agent : agents) {
                    Shell.printInfo("Agent [%s]", agent.getName());
                }
            }
        });
        return commandSet;
    }
}
