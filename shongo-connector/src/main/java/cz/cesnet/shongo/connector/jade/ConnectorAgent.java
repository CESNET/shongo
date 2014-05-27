package cz.cesnet.shongo.connector.jade;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.connector.ConnectorContainer;
import cz.cesnet.shongo.connector.ConnectorContainerConfiguration;
import cz.cesnet.shongo.connector.ConnectorScope;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ConnectorConfiguration;
import cz.cesnet.shongo.connector.api.ConnectorInitException;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.connector.api.jade.ConnectorOntology;
import cz.cesnet.shongo.connector.common.AbstractConnector;
import cz.cesnet.shongo.controller.ControllerScope;
import cz.cesnet.shongo.controller.api.jade.ControllerOntology;
import cz.cesnet.shongo.jade.*;
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

    /**
     * {@link CommonService} which holds the connector to be used for the {@link ConnectorAgent}.
     */
    private CommonService connectorService;

    /**
     * Cached name of controller agent (to not search for it every time when it is requested).
     */
    private String cachedControllerAgentName;

    @Override
    protected void setup()
    {
        // Get arguments
        Object[] arguments = getArguments();
        if (arguments.length != 2) {
            throw new IllegalArgumentException("Two arguments are required for " + ConnectorAgent.class);
        }
        if (!(arguments[0] instanceof ConnectorContainerConfiguration)) {
            throw new IllegalArgumentException(
                    "First argument must be instance of " + ConnectorContainerConfiguration.class);
        }
        if (!(arguments[1] instanceof ConnectorConfiguration)) {
            throw new IllegalArgumentException(
                    "Second argument must be instance of " + ConnectorConfiguration.class);
        }
        ConnectorContainerConfiguration connectorContainerConfiguration =
                (ConnectorContainerConfiguration) arguments[0];
        ConnectorConfiguration connectorConfiguration = (ConnectorConfiguration) arguments[1];

        // Configure this agent
        addOntology(ConnectorOntology.getInstance());
        addOntology(ControllerOntology.getInstance());
        setCommandTimeout((int) connectorContainerConfiguration.getJadeCommandTimeout().getMillis());

        // Create and initialize connector
        Class<? extends CommonService> connectorClass;
        try {
            connectorClass = connectorConfiguration.getConnectorClass();
        }
        catch (Exception exception) {
            throw new RuntimeException("Invalid connector class", exception);
        }
        try {

            Constructor constructor = connectorClass.getConstructor();
            connectorService = (CommonService) constructor.newInstance();
            if (connectorService instanceof AbstractConnector) {
                AbstractConnector abstractConnector = (AbstractConnector) connectorService;
                abstractConnector.setConfiguration(connectorConfiguration);
                abstractConnector.setAgent(this);
            }
            connectorService.connect(connectorConfiguration);
            logger.info("Connector managed: {}", connectorService.getStatus());
        }
        catch (NoSuchMethodException exception) {
            throw new ConnectorInitException("Invalid connector class: " + connectorClass +
                    " (does not define an appropriate constructor)", exception);
        }
        catch (InvocationTargetException exception) {
            throw new ConnectorInitException("Connector class init failed", exception);
        }
        catch (InstantiationException exception) {
            throw new ConnectorInitException("Connector class init failed", exception);
        }
        catch (IllegalAccessException exception) {
            throw new ConnectorInitException("Connector class not accessible: " + connectorClass, exception);
        }
        catch (CommandException exception) {
            throw new ConnectorInitException("Connector failed to manage", exception);
        }

        setupAgent();

        // Create services
        registerService(ConnectorScope.CONNECTOR_AGENT_SERVICE, ConnectorScope.CONNECTOR_AGENT_SERVICE_NAME);

        super.setup();
    }

    @Override
    protected void takeDown()
    {
        if (connectorService != null) {
            try {
                connectorService.disconnect();
            }
            catch (CommandException e) {
                // just suppress the exception, the agent is going not to be working anyway
            }
        }
        super.takeDown();
    }

    /**
     * @return name of the controller agent or null if it doesn't exists
     * @throws RuntimeException when multiple controller agents are present
     */
    public String getCachedControllerAgentName()
    {
        if (cachedControllerAgentName == null) {
            AID[] controllerAgents = findAgentsByService(ControllerScope.CONTROLLER_AGENT_SERVICE, 100);
            if (controllerAgents.length > 0) {
                if (controllerAgents.length > 1) {
                    throw new RuntimeException("Multiple controller agents were found.");
                }
                cachedControllerAgentName = controllerAgents[0].getLocalName();
            }
        }
        return cachedControllerAgentName;
    }

    @Override
    public SendLocalCommand sendCommand(String receiverAgentName, Command command)
    {
        ConnectorContainer.requestedCommands.info("Action:{} {}.", command.getId(), command);
        SendLocalCommand sendLocalCommand = super.sendCommand(receiverAgentName, command);
        String commandState;
        switch (sendLocalCommand.getState()) {
            case SUCCESSFUL:
                Object result = sendLocalCommand.getResult();
                if (result != null && result instanceof String) {
                    commandState = String.format("OK: %s", result);
                }
                else {
                    commandState = "OK";
                }
                break;
            case FAILED:
                commandState = String.format("FAILED: %s", sendLocalCommand.getJadeReport().getMessage());
                break;
            default:
                commandState = "UNKNOWN";
                break;
        }
        ConnectorContainer.requestedCommands.info("Action:{} Done ({}).", command.getId(), commandState);
        return sendLocalCommand;
    }

    @Override
    public Object handleCommand(Command command, AID sender)
            throws CommandException, CommandUnsupportedException
    {
        if (connectorService != null && command instanceof ConnectorCommand) {
            ConnectorCommand connectorCommand = (ConnectorCommand) command;
            ConnectorContainer.executedCommands.info("Action:{} {}.", connectorCommand.getId(), connectorCommand.toString());
            Object result = null;
            String resultState = "OK";
            try {
                result = connectorCommand.execute(connectorService);
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
                ConnectorContainer.executedCommands.info("Action:{} Done ({}).", connectorCommand.getId(), resultState);
            }
            return result;
        }
        return super.handleCommand(command, sender);
    }
}
