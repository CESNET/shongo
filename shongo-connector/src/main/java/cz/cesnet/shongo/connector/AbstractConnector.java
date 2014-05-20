package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.JadeReport;
import cz.cesnet.shongo.JadeReportSet;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.jade.SimpleCommandException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ConnectorInfo;
import cz.cesnet.shongo.connector.api.ConnectorOptions;
import cz.cesnet.shongo.controller.api.jade.ControllerCommand;
import cz.cesnet.shongo.controller.api.jade.GetUserInformation;
import cz.cesnet.shongo.jade.*;
import org.jdom2.Element;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import sun.tools.jar.resources.jar_es;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * A common functionality for connectors.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
abstract public class AbstractConnector implements CommonService
{
    private static Logger logger = LoggerFactory.getLogger(AbstractConnector.class);

    /**
     * Timeout option.
     */
    public static final String OPTION_TIMEOUT = "timeout";
    public static final Duration DEFAULT_TIMEOUT = Duration.standardSeconds(30);


    /**
     * {@link ConnectorAgent} which can be used for performing {@link cz.cesnet.shongo.api.jade.Command}s.
     */
    private ConnectorAgent connectorAgent;

    /**
     * {@link ConnectorOptions}.
     */
    private ConnectorOptions options = new ConnectorOptions() {
        @Override
        public String getString(String key)
        {
            return null;
        }

        @Override
        public List<String> getStringList(String key)
        {
            return null;
        }

        @Override
        public List<ConnectorOptions> getOptionsList(String key)
        {
            return null;
        }
    };

    /**
     * Info about the connector and the device.
     */
    protected volatile ConnectorInfo info = new ConnectorInfo(getClass().getSimpleName());

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
     * @param connectorAgent sets the {@link #connectorAgent}
     */
    public void setConnectorAgent(ConnectorAgent connectorAgent)
    {
        this.connectorAgent = connectorAgent;
    }

    @Override
    public ConnectorInfo getConnectorInfo()
    {
        return info;
    }

    public boolean isConnected()
    {
        ConnectorInfo.ConnectionState connState = info.getConnectionState();
        return (connState == ConnectorInfo.ConnectionState.CONNECTED || connState == ConnectorInfo.ConnectionState.LOOSELY_CONNECTED);
    }

    @Override
    public void setOptions(ConnectorOptions options)
    {
        this.options = options;
    }

    /**
     * @param name
     * @return value of options with given {@code name}
     */
    protected String getOption(String name)
    {
        return options.getString(name);
    }

    /**
     * @param name
     * @return value of option with given {@code name}
     */
    protected Pattern getOptionPattern(String name)
    {
        return options.getPattern(name);
    }

    /**
     * @param name
     * @param defaultValue
     * @return {@link Duration} of option with given {@code name} or given {@code defaultValue}
     */
    protected Duration getOptionDuration(String name, Duration defaultValue)
    {
        return options.getDuration(name, defaultValue);
    }

    /**
     * @param controllerCommand to be performed
     * @return result from the action
     */
    protected Object performControllerAction(ControllerCommand controllerCommand) throws CommandException
    {
        if (connectorAgent == null) {
            throw new IllegalStateException("Connector agent must be set for performing controller action.");
        }
        String agentName = connectorAgent.getCachedControllerAgentName();
        if (agentName == null) {
            throw new RuntimeException("Controller agent was not found.");
        }
        SendLocalCommand sendLocalCommand = connectorAgent.sendCommand(agentName, controllerCommand);
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
     *         CommandUnsupportedException
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

    /**
     * Just for debugging purposes, for printing results of commands.
     * <p/>
     * Taken from:
     * http://stackoverflow.com/questions/2325388/java-shortest-way-to-pretty-print-to-stdout-a-org-w3c-dom-document
     *
     * @param doc XML document to be printed
     * @param out stream to print the document to
     * @throws IOException
     * @throws javax.xml.transform.TransformerException
     *
     */
    protected static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException
    {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(doc),
                new StreamResult(new OutputStreamWriter(out, "UTF-8")));
    }
}
