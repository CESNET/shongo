package cz.cesnet.shongo.connector.common;

import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.JadeReport;
import cz.cesnet.shongo.JadeReportSet;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.jade.SimpleCommandException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ConnectorConfiguration;
import cz.cesnet.shongo.api.ConnectorStatus;
import cz.cesnet.shongo.connector.jade.ConnectorAgent;
import cz.cesnet.shongo.controller.api.jade.ControllerCommand;
import cz.cesnet.shongo.controller.api.jade.GetUserInformation;
import cz.cesnet.shongo.jade.SendLocalCommand;
import org.joda.time.Duration;

import java.lang.reflect.Method;
import java.util.*;

/**
 * A common functionality for connectors.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
abstract public class AbstractConnector implements CommonService
{
    /**
     * {@link ConnectorAgent} which can be used for performing {@link cz.cesnet.shongo.api.jade.Command}s.
     */
    protected ConnectorAgent agent;

    /**
     * {@link}
     */
    protected ConnectorConfiguration configuration;

    /**
     * Cache of user-principal-name (EPPN) by user principal-id.
     */
    protected ExpirationMap<String, String> cachedPrincipalNameByPrincipalId =
            new ExpirationMap<String, String>(Duration.standardHours(1));

    /**
     * Cache of {@link cz.cesnet.shongo.api.UserInformation} by user-principal-name (EPPN).
     */
    protected ExpirationMap<String, UserInformation> cachedUserInformationByPrincipalName =
            new ExpirationMap<String, UserInformation>(Duration.standardHours(1));

    /**
     * Cache of {@link UserInformation} by shongo-user-id.
     */
    protected ExpirationMap<String, UserInformation> cachedUserInformationById =
            new ExpirationMap<String, UserInformation>(Duration.standardHours(1));

    /**
     * @param agent sets the {@link #agent}
     */
    public void setAgent(ConnectorAgent agent)
    {
        this.agent = agent;
    }

    /**
     * @return {@link #configuration}
     */
    public ConnectorConfiguration getConfiguration()
    {
        return configuration;
    }

    /**
     * @param configuration sets the {@link #configuration}
     */
    public void setConfiguration(ConnectorConfiguration configuration)
    {
        this.configuration = configuration;
    }

    @Override
    public ConnectorStatus getStatus()
    {
        ConnectorStatus connectorStatus = new ConnectorStatus();
        connectorStatus.setState(ConnectorStatus.State.NOT_AVAILABLE);
        return connectorStatus;
    }

    /**
     * Lists names of all implemented methods supported by the implementing connector.
     * <p/>
     * Uses reflection.
     * <p/>
     * Any method that declares throwing CommandUnsupportedException is considered not implemented on the connector.
     * Thus, it relies just on the fact that the method is not declaring throwing CommandUnsupportedException.
     * Note that even if a method is actually implemented and works, it is not listed by getSupportedMethods() if it
     * still declares throwing CommandUnsupportedException (which is needless, though).
     *
     * @return collection of names of public methods implemented from an interface, not throwing
     * CommandUnsupportedException
     */
    @Override
    public List<String> getSupportedMethods()
    {
        List<String> result = new ArrayList<String>();

        // get public methods not raising CommandUnsupportedException
        Map<String, Class[]> methods = new HashMap<String, Class[]>();
MethodsLoop:
        for (Method m : getClass().getMethods()) {
            final Class[] exceptionTypes = m.getExceptionTypes();
            for (Class ex : exceptionTypes) {
                if (ex.equals(CommandUnsupportedException.class)) {
                    continue MethodsLoop;
                }
            }
            // CommandUnsupportedException not found - the method seems good
            methods.put(m.getName(), m.getParameterTypes());
        }
        // promote those implementing an interface
        List<Class> interfaces = new LinkedList<Class>();
        Class clazz = getClass();
        while (clazz != null) {
            for (Class intfc : clazz.getInterfaces()) {
                interfaces.add(intfc);
            }
            clazz = clazz.getSuperclass();
        }

        for (Class intfc : interfaces) {
            for (Method m : intfc.getMethods()) {
                final String mName = m.getName();
                if (methods.containsKey(mName) && Arrays.equals(m.getParameterTypes(), methods.get(mName))) {
                    result.add(mName);
                }
            }
        }

        return result;
    }

    /**
     * @param controllerCommand to be performed
     * @return result from the action
     */
    public Object performControllerAction(ControllerCommand controllerCommand) throws CommandException
    {
        if (agent == null) {
            throw new IllegalStateException("Connector agent must be set for performing controller action.");
        }
        String agentName = agent.getCachedControllerAgentName();
        if (agentName == null) {
            throw new RuntimeException("Controller agent was not found.");
        }
        SendLocalCommand sendLocalCommand = agent.sendCommand(agentName, controllerCommand);
        if (sendLocalCommand.getState() == SendLocalCommand.State.SUCCESSFUL) {
            return sendLocalCommand.getResult();
        }
        JadeReport jadeReport = sendLocalCommand.getJadeReport();
        String jadeReportCode = null;
        if (jadeReport instanceof JadeReportSet.CommandFailedReport) {
            JadeReportSet.CommandFailedReport commandFailedReport = (JadeReportSet.CommandFailedReport) jadeReport;
            commandFailedReport.getCode();
            jadeReportCode = commandFailedReport.getCode();
        }
        throw new SimpleCommandException(
                jadeReportCode, String.format("Controller action failed: %s", jadeReport.getMessage()));
    }

    /**
     * @param userPrincipalName principal-name of an user (EPPN)
     * @return {@link UserInformation} for given {@code userPrincipalName}
     * @throws CommandException
     */
    public UserInformation getUserInformationByPrincipalName(String userPrincipalName) throws CommandException
    {
        UserInformation userInformation;
        if (cachedUserInformationByPrincipalName.contains(userPrincipalName)) {
            userInformation = cachedUserInformationByPrincipalName.get(userPrincipalName);
        }
        else {
            userInformation = (UserInformation) performControllerAction(
                    GetUserInformation.byPrincipalName(userPrincipalName));
            cachedUserInformationByPrincipalName.put(userPrincipalName, userInformation);
        }
        return userInformation;
    }

    /**
     * @param userId shongo-user-id
     * @return {@link UserInformation} for given {@code userId} or null when user doesn't exist
     */
    public UserInformation getUserInformationById(String userId) throws CommandException
    {
        UserInformation userInformation;
        if (cachedUserInformationById.contains(userId)) {
            userInformation = cachedUserInformationById.get(userId);
        }
        else {
            userInformation = (UserInformation) performControllerAction(GetUserInformation.byUserId(userId));
            cachedUserInformationById.put(userId, userInformation);
        }
        return userInformation;
    }
}
