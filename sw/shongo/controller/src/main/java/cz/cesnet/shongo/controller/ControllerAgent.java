package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.jade.AgentAction;
import cz.cesnet.shongo.connector.ConnectorScope;
import cz.cesnet.shongo.connector.api.jade.ConnectorOntology;
import cz.cesnet.shongo.controller.api.jade.Service;
import cz.cesnet.shongo.controller.api.jade.ControllerAgentAction;
import cz.cesnet.shongo.controller.api.jade.ControllerOntology;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.AgentActionCommand;
import cz.cesnet.shongo.jade.Command;
import cz.cesnet.shongo.shell.CommandHandler;
import cz.cesnet.shongo.shell.CommandSet;
import cz.cesnet.shongo.shell.Shell;
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
     * Service to be used for handling {@link ControllerAgentAction}s.
     */
    private Service service;

    /**
     * Constructor.
     */
    public ControllerAgent()
    {
    }

    /**
     * @param service sets the {@link #service}
     */
    public void setService(Service service)
    {
        this.service = service;
    }

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
        return findAgentsByService(ConnectorScope.CONNECTOR_AGENT_SERVICE, 1000);
    }

    @Override
    protected void setup()
    {
        addOntology(ConnectorOntology.getInstance());
        addOntology(ControllerOntology.getInstance());

        super.setup();

        registerService(ControllerScope.CONTROLLER_AGENT_SERVICE, ControllerScope.CONTROLLER_AGENT_SERVICE_NAME);
    }

    @Override
    public Command performCommand(Command command)
    {
        if (command instanceof AgentActionCommand) {
            AgentActionCommand actionCommand = (AgentActionCommand) command;
            AgentAction action = actionCommand.getAgentAction();
            Controller.requestedAgentActions.info("Action:{} {}.", action.getId(), action);
            command = super.performCommand(command);
            String commandState;
            switch (command.getState()) {
                case SUCCESSFUL:
                    Object result = command.getResult();
                    if (result != null && result instanceof String) {
                        commandState = String.format("OK: %s", result);
                    }
                    else {
                        commandState = "OK";
                    }
                    break;
                case FAILED:
                    commandState = String.format("FAILED: %s", command.getFailure().getMessage());
                    break;
                default:
                    commandState = "UNKNOWN";
                    break;
            }
            Controller.requestedAgentActions.info("Action:{} Done ({}).", action.getId(), commandState);
        }
        else {
            Controller.requestedAgentActions.info("Command: {}.", command.getName());
            command = super.performCommand(command);
            Controller.requestedAgentActions.info("Command: Done.");
        }
        return command;
    }

    @Override
    public Object handleAgentAction(jade.content.AgentAction agentAction, AID sender)
            throws CommandException, CommandUnsupportedException
    {
        if (service != null && agentAction instanceof ControllerAgentAction) {
            ControllerAgentAction controllerAgentAction = (ControllerAgentAction) agentAction;
            Controller.executedAgentActions.info("Action:{} {}.", controllerAgentAction.getId(),
                    controllerAgentAction.toString());
            Object result = null;
            String resultState = "OK";
            try {
                result = controllerAgentAction.execute(service);
                if (result != null && result instanceof String) {
                    resultState = String.format("OK: %s", result);
                }
            }
            catch (CommandException exception) {
                resultState = String.format("FAILED: %s", exception.getMessage());
                throw exception;
            }
            catch (RuntimeException exception) {
                resultState = String.format("FAILED: %s", exception.getMessage());
                throw exception;
            }
            finally {
                Controller.executedAgentActions.info("Action:{} Done ({}).", controllerAgentAction.getId(), resultState);
            }
            return result;
        }
        return super.handleAgentAction(agentAction, sender);
    }
}
