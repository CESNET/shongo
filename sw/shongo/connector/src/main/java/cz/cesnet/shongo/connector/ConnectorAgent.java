package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ConnectorInitException;
import cz.cesnet.shongo.connector.api.ConnectorOptions;
import cz.cesnet.shongo.connector.api.jade.ConnectorAgentAction;
import cz.cesnet.shongo.connector.api.jade.ConnectorOntology;
import cz.cesnet.shongo.controller.api.jade.ControllerOntology;
import cz.cesnet.shongo.jade.*;
import jade.content.AgentAction;
import jade.core.AID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Jade Agent for Device Connector
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ConnectorAgent extends Agent
{
    private static Logger logger = LoggerFactory.getLogger(ConnectorAgent.class);

    private CommonService connector;

    @Override
    protected void setup()
    {
        addOntology(ConnectorOntology.getInstance());
        addOntology(ControllerOntology.getInstance());

        super.setup();

        registerService(Constants.CONNECTOR_SERVICE, "Connector Service");
    }

    @Override
    protected void takeDown()
    {
        if (connector != null) {
            try {
                connector.disconnect();
            }
            catch (CommandException e) {
                // just suppress the exception, the agent is going not to be working anyway
            }
        }
        super.takeDown();
    }

    /**
     * Starts managing a device. Initializes a connector to the device.
     */
    public void manage(String connectorClass, String address, int port, String username, String password,
            ConnectorOptions options)
            throws ConnectorInitException, CommandException
    {
        try {
            Constructor co = Class.forName(connectorClass).getConstructor();
            connector = (CommonService) co.newInstance();
            if (connector == null) {
                throw new ConnectorInitException(
                        "Invalid connector class: " + connectorClass + " (must implement the CommonService interface)");
            }

            connector.setOptions(options);
            connector.connect(new Address(address, port), username, password);

            logger.info("Connector ready: {}", connector.getConnectorInfo());
        }
        catch (NoSuchMethodException e) {
            throw new ConnectorInitException(
                    "Invalid connector class: " + connectorClass + " (does not define an appropriate constructor)",
                    e
            );
        }
        catch (ClassNotFoundException e) {
            throw new ConnectorInitException("Connector class not found: " + connectorClass, e);
        }
        catch (InvocationTargetException e) {
            throw new ConnectorInitException("Connector class init failed", e);
        }
        catch (InstantiationException e) {
            throw new ConnectorInitException("Connector class init failed", e);
        }
        catch (IllegalAccessException e) {
            throw new ConnectorInitException("Connector class not accessible: " + connectorClass, e);
        }
    }

    @Override
    public cz.cesnet.shongo.jade.Command performCommand(cz.cesnet.shongo.jade.Command command)
    {
        if (command instanceof AgentActionCommand) {
            AgentActionCommand actionCommand = (AgentActionCommand) command;
            cz.cesnet.shongo.api.jade.AgentAction action = actionCommand.getAgentAction();
            Connector.requestedAgentActions.info("Action:{} {}.", action.getId(), action);
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
            Connector.requestedAgentActions.info("Action:{} Done ({}).", action.getId(), commandState);
        }
        else {
            Connector.requestedAgentActions.info("Command: {}.", command.getName());
            command = super.performCommand(command);
            Connector.requestedAgentActions.info("Command: Done.");
        }
        return command;
    }

    @Override
    public Object handleAgentAction(AgentAction agentAction, AID sender)
            throws CommandException, CommandUnsupportedException
    {
        if (agentAction instanceof ConnectorAgentAction) {
            ConnectorAgentAction connectorAgentAction = (ConnectorAgentAction) agentAction;
            Connector.executedAgentActions.info("Action:{} {}.", connectorAgentAction.getId(), connectorAgentAction.toString());
            Object result = null;
            String resultState = "OK";
            try {
                result = connectorAgentAction.exec(connector);
                if (result != null && result instanceof String) {
                    resultState = String.format("OK: %s", result);
                }
            }
            catch (CommandException exception) {
                resultState = String.format("FAILED: %s", exception.getMessage());
                throw exception;
            }
            catch (CommandUnsupportedException exception) {
                resultState = "NOT-SUPPORTED";
                throw exception;
            }
            catch (RuntimeException exception) {
                resultState = String.format("FAILED: %s", exception.getMessage());
                throw exception;
            }
            finally {
                Connector.executedAgentActions.info("Action:{} Done ({}).", connectorAgentAction.getId(), resultState);
            }
            return result;
        }
        return super.handleAgentAction(agentAction, sender);
    }
}
