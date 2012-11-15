package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.ontology.ConnectorOntology;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.UnknownAgentActionException;
import cz.cesnet.shongo.shell.CommandHandler;
import cz.cesnet.shongo.shell.CommandSet;
import cz.cesnet.shongo.shell.Shell;
import jade.content.AgentAction;
import jade.core.AID;
import org.apache.commons.cli.CommandLine;

/**
 * Jade Agent for Domain Controller
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerAgent extends Agent
{
    /**
     * @return {@link CommandSet} for {@link ControllerAgent}
     */
    public CommandSet createCommandSet()
    {
        CommandSet commandSet = new CommandSet();
        commandSet.addCommand("list", "List all connector agents in the domain", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                for (AID agent : listConnectorAgents()) {
                    Shell.printInfo("Agent [%s]", agent.getName());
                }
            }
        });
        return commandSet;
    }

    /**
     * @return list of all connector agents
     */
    public AID[] listConnectorAgents()
    {
        return findAgentsByService("connector", 1000);
    }

    @Override
    protected void setup()
    {
        addOntology(ConnectorOntology.getInstance());
        super.setup();
    }

    @Override
    public Object handleAgentAction(AgentAction action, AID sender)
            throws UnknownAgentActionException, CommandException, CommandUnsupportedException
    {
        return super.handleAgentAction(action, sender);
    }
}
