package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ConnectorInitException;
import cz.cesnet.shongo.connector.api.ConnectorOptions;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.connector.api.jade.ConnectorOntology;
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
        Object[] arguments = getArguments();
        if (arguments.length == 0 || !(arguments[0] instanceof ConnectorConfiguration)) {
            throw new IllegalArgumentException("ConnectorConfiguration is required as first argument.");
        }
        ConnectorConfiguration configuration = (ConnectorConfiguration) arguments[0];
        setCommandTimeout((int) configuration.getJadeCommandTimeout().getMillis());

        addOntology(ConnectorOntology.getInstance());
        addOntology(ControllerOntology.getInstance());

        super.setup();

        registerService(ConnectorScope.CONNECTOR_AGENT_SERVICE, ConnectorScope.CONNECTOR_AGENT_SERVICE_NAME);
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

    /**
     * Starts managing a device. Initializes a connector to the device.
     */
    public void manage(String connectorClass, String address, int port, String username, String password,
            ConnectorOptions options)
            throws ConnectorInitException
    {
        try {
            Constructor co = Class.forName(connectorClass).getConstructor();
            connectorService = (CommonService) co.newInstance();
            if (connectorService == null) {
                throw new ConnectorInitException(
                        "Invalid connector class: " + connectorClass + " (must implement the CommonService interface)");
            }
            if (connectorService instanceof AbstractConnector) {
                AbstractConnector abstractConnector = (AbstractConnector) connectorService;
                abstractConnector.setConnectorAgent(this);
            }

            connectorService.setOptions(options);
            connectorService.connect(new Address(address, port), username, password);

            logger.info("Connector ready: {}", connectorService.getConnectorInfo());
        }
        catch (CommandException exception) {
            throw new ConnectorInitException("Connector failed to connect to the device", exception);
        }
        catch (NoSuchMethodException exception) {
            throw new ConnectorInitException(
                    "Invalid connector class: " + connectorClass + " (does not define an appropriate constructor)",
                    exception
            );
        }
        catch (ClassNotFoundException exception) {
            throw new ConnectorInitException("Connector class not found: " + connectorClass, exception);
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
    }

    @Override
    public SendLocalCommand sendCommand(String receiverAgentName, Command command)
    {
        Connector.requestedCommands.info("Action:{} {}.", command.getId(), command);
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
        Connector.requestedCommands.info("Action:{} Done ({}).", command.getId(), commandState);
        return sendLocalCommand;
    }

    @Override
    public Object handleCommand(Command command, AID sender)
            throws CommandException, CommandUnsupportedException
    {
        if (connectorService != null && command instanceof ConnectorCommand) {
            ConnectorCommand connectorCommand = (ConnectorCommand) command;
            Connector.executedCommands.info("Action:{} {}.", connectorCommand.getId(), connectorCommand.toString());
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
                Connector.executedCommands.info("Action:{} Done ({}).", connectorCommand.getId(), resultState);
            }
            return result;
        }
        return super.handleCommand(command, sender);
    }
}
