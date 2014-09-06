package cz.cesnet.shongo.connector.device;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.DeviceLoadInfo;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.MonitoringService;
import cz.cesnet.shongo.connector.api.UsageStats;
import cz.cesnet.shongo.connector.common.AbstractSSHConnector;
import cz.cesnet.shongo.connector.common.Command;
import cz.cesnet.shongo.connector.api.ConnectorInitException;
import cz.cesnet.shongo.connector.api.EndpointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * A connector for Cisco TelePresence System Codec C90.
 *
 * TODO: have a look at "xCommand HttpFeedback Register" command - some feedback may be used to prevent polling
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CodecC90Connector extends AbstractSSHConnector implements EndpointService, MonitoringService
{
    private static Logger logger = LoggerFactory.getLogger(CodecC90Connector.class);

    public static final int MICROPHONES_COUNT = 8;

    public CodecC90Connector()
    {
        super("^(OK|ERROR|</XmlDoc>)$");
    }

    /**
     * An example of interaction with the device.
     *
     * Just for debugging purposes.
     *
     * @param args
     * @throws IOException
     * @throws CommandException
     * @throws InterruptedException
     * @throws ConnectorInitException
     * @throws SAXException
     * @throws XPathExpressionException
     */
    public static void main(String[] args)
            throws IOException, CommandException, InterruptedException, ConnectorInitException, SAXException,
                   XPathExpressionException, TransformerException, ParserConfigurationException
    {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

        final String address;
        final String username;
        final String password;

        if (args.length > 0) {
            address = args[0];
        }
        else {
            System.out.print("address: ");
            address = in.readLine();
            if (address == null) {
                throw new IllegalArgumentException("Address is empty.");
            }
        }

        if (args.length > 1) {
            username = args[1];
        }
        else {
            System.out.print("username: ");
            username = in.readLine();
        }

        if (args.length > 2) {
            password = args[2];
        }
        else {
            System.out.print("password: ");
            password = in.readLine();
        }

        final CodecC90Connector conn = new CodecC90Connector();
        conn.connect(DeviceAddress.parseAddress(address), username, password);

        Document result = conn.exec(new Command("xstatus SystemUnit uptime"));
        System.out.println("result:");
        printDocument(result, System.out);
        if (conn.isError(result)) {
            System.err.println("Error: " + conn.getErrorMessage(result));
            System.exit(1);
        }
        System.out.println("Uptime: " + getResultString(result, "/XmlDoc/Status/SystemUnit/Uptime"));
        System.out.println();

        Document calls = conn.exec(new Command("xStatus Call"));
        System.out.println("calls:");
        printDocument(calls, System.out);
        boolean activeCalls = !getResultString(calls, "/XmlDoc/Status/*").isEmpty();
        if (activeCalls) {
            System.out.println("There are some active calls");
        }
        else {
            System.out.println("There is no active call at the moment");
        }
        System.out.println();

        System.out.println("All done, disconnecting");
        conn.disconnect();
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

    @Override
    protected void initSession() throws IOException
    {
        // read the welcome message
        readOutput();

        sendCommand(new Command("echo off"));
        // read the result of the 'echo off' command
        readOutput();

        sendCommand(new Command("xpreferences outputmode xml"));
    }

    protected void initDeviceInfo() throws IOException, CommandException
    {
        try {
            Document result = exec(new Command("xstatus SystemUnit"));

            setDeviceName(getResultString(result, "/XmlDoc/Status/SystemUnit/ProductId"));
            setDeviceDescription(getResultString(result, "/XmlDoc/Status/SystemUnit/ContactInfo"));

            String softwareVersion = getResultString(result, "/XmlDoc/Status/SystemUnit/Software/Version")
                    + " (released "
                    + getResultString(result, "/XmlDoc/Status/SystemUnit/Software/ReleaseDate")
                    + ")";
            setDeviceSoftwareVersion(softwareVersion);

            String serialNumber = "Module: " + getResultString(result, "/XmlDoc/Status/SystemUnit/Hardware/Module/SerialNumber")
                    + ", MainBoard: " + getResultString(result, "/XmlDoc/Status/SystemUnit/Hardware/MainBoard/SerialNumber")
                    + ", VideoBoard: " + getResultString(result,
                    "/XmlDoc/Status/SystemUnit/Hardware/VideoBoard/SerialNumber")
                    + ", AudioBoard: " + getResultString(result,
                    "/XmlDoc/Status/SystemUnit/Hardware/AudioBoard/SerialNumber");
            setDeviceSerialNumber(serialNumber);
        }
        catch (SAXException exception) {
            throw new CommandException("Command gave unexpected output", exception);
        }
        catch (ParserConfigurationException exception) {
            throw new CommandException("Error initializing result parser", exception);
        }
        catch (XPathExpressionException exception) {
            throw new CommandException("Error querying command output XML tree", exception);
        }
    }

    @Override
    protected void initDeviceState() throws IOException, CommandException
    {
        // TODO
    }

    /**
     * Send a command to the device.
     * In case of an error, throws a CommandException with a detailed message.
     *
     * @param command command to be issued
     * @return the result of the command
     */
    private Document issueCommand(Command command) throws CommandException
    {
        logger.debug(String.format("Issuing command '%s' on %s", command, deviceAddress));

        try {
            Document result = exec(command);
            if (isError(result)) {
                String errMsg = getErrorMessage(result);
                logger.error(String.format("Command %s failed on %s: %s", command, deviceAddress, errMsg));
                throw new CommandException(errMsg);
            }
            else {
                logger.debug(String.format("Command '%s' succeeded on %s", command, deviceAddress));
                return result;
            }
        }
        catch (IOException e) {
            logger.error("Error issuing a command", e);
            throw new CommandException("Command issuing error", e);
        }
        catch (SAXException e) {
            logger.error("Error issuing a command", e);
            throw new CommandException("Command result parsing error", e);
        }
        catch (XPathExpressionException e) {
            logger.error("Error issuing a command", e);
            throw new CommandException("Command result handling error", e);
        }
        catch (ParserConfigurationException e) {
            logger.error("Error issuing a command", e);
            throw new CommandException("Error initializing result parser", e);
        }
    }


    private static DocumentBuilder resultBuilder = null;

    /**
     * Sends a command to the device. Blocks until response to the command is complete.
     *
     * @param command a command to the device
     * @return output of the command
     * @throws IOException
     */
    private Document exec(Command command) throws IOException, SAXException, ParserConfigurationException
    {
        sendCommand(command);

        String output = readOutput();
        InputSource is = new InputSource(new StringReader(output));

        if (resultBuilder == null) {
            // lazy initialization
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            resultBuilder = factory.newDocumentBuilder();
        }

        return resultBuilder.parse(is);
    }

    private static XPathFactory xPathFactory = XPathFactory.newInstance();
    private static Map<String, XPathExpression> xPathExpressionCache = new HashMap<String, XPathExpression>();


    /**
     * Returns the result of an XPath expression on a given document. Caches the expressions for further usage.
     *
     * @param result      an XML document
     * @param xPathString an XPath expression
     * @return result of the XPath expression
     */
    private static String getResultString(Document result, String xPathString) throws XPathExpressionException
    {
        XPathExpression expr = xPathExpressionCache.get(xPathString);
        if (expr == null) {
            expr = xPathFactory.newXPath().compile(xPathString);
            xPathExpressionCache.put(xPathString, expr);
        }
        return expr.evaluate(result);
    }


    /**
     * Finds out whether a given result XML denotes an error.
     *
     * @param result an XML document - result of a command
     * @return true if the result marks an error, false if the result is an ordinary result record
     */
    private boolean isError(Document result) throws XPathExpressionException
    {
        String status = getResultString(result, "/XmlDoc/*[@status != '']/@status");
        return (status.contains("Error"));
    }


    /**
     * Given an XML result of an erroneous command, returns the error message.
     *
     * @param result an XML document - result of a command
     * @return error message contained in the result document, or null if the document does not denote an error
     */
    private String getErrorMessage(Document result) throws XPathExpressionException
    {
        if (!isError(result)) {
            return null;
        }

        String reason = getResultString(result, "/XmlDoc/Status[@status='Error']/Reason");
        String xPath = getResultString(result, "/XmlDoc/Status[@status='Error']/XPath");
        if (!reason.isEmpty() || !xPath.isEmpty()) {
            return reason + (xPath.isEmpty() ? "" : " (XPath: " + xPath + ")");
        }

        String description = getResultString(result, "/XmlDoc/*[@status='Error']/Description");
        if (!description.isEmpty()) {
            String cause = getResultString(result, "/XmlDoc/*[@status='Error']/Cause");
            return description + (cause.isEmpty() ? "" : String.format(" (Cause: %s)", cause));
        }

        String usage = getResultString(result, "/XmlDoc/*[@status='ParameterError']/../Usage");
        if (!usage.isEmpty()) {
            usage = usage.replace("&lt;", "<").replace("&gt;", ">");
            return "Parameter error. Usage: " + usage;
        }

        return "Uncategorized error";
    }

    // COMMON SERVICE

    @Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException
    {
        Command command = new Command("xStatus SystemUnit Uptime");
        Document result = issueCommand(command);
        try {
            String uptime = getResultString(result, "/XmlDoc/Status/SystemUnit/Uptime");

            DeviceLoadInfo info = new DeviceLoadInfo();
            info.setUptime(Integer.valueOf(uptime));
            return info;
        }
        catch (XPathExpressionException e) {
            throw new CommandException("Program error in parsing the command result.", e);
        }
    }

    @Override
    public UsageStats getUsageStats() throws CommandException, CommandUnsupportedException
    {
        return null;
    }

    // ENDPOINT SERVICE

    @Override
    public String dial(Alias alias) throws CommandException
    {
        String address = alias.getValue();

        Command command = new Command("xCommand Dial");
        command.setParameter("Number", address);
        // NOTE: the BookingId parameter could be used to identify the reservation for which this dial is issued in call
        //       logs; other connectors are missing such a feature, however, so do we

        Document result = issueCommand(command);
        try {
            return getResultString(result, "/XmlDoc/DialResult/CallId");
        }
        catch (XPathExpressionException e) {
            throw new CommandException("Program error in parsing the command result.", e);
        }
    }

    @Override
    public void hangUp(String callId) throws CommandException
    {
        Command command = new Command("xCommand Call Disconnect");
        command.setParameter("CallId", callId);
        issueCommand(command);
    }

    @Override
    public void hangUpAll() throws CommandException
    {
        issueCommand(new Command("xCommand Call DisconnectAll"));
    }

    @Override
    public void rebootDevice() throws CommandException
    {
        Command command = new Command("xCommand Boot");
        command.setParameter("Action", "Restart"); // should be default anyway, but just for sure...
        issueCommand(command);
    }

    @Override
    public void mute() throws CommandException
    {
        issueCommand(new Command("xCommand Audio Microphones Mute"));
    }

    @Override
    public void unmute() throws CommandException
    {
        issueCommand(new Command("xCommand Audio Microphones Unmute"));
    }

    @Override
    public void setMicrophoneLevel(int level) throws CommandException
    {
        level = level * 24 / 100; // device takes the gain in range 0..24 (dB)

        for (int i = 1; i <= MICROPHONES_COUNT; i++) {
            Command cmd = new Command("xConfiguration Audio Input Microphone " + i);
            cmd.setParameter("Level", String.valueOf(level));
            issueCommand(cmd);
        }
    }

    @Override
    public void setPlaybackLevel(int level) throws CommandException
    {
        Command cmd = new Command("xConfiguration Audio");
        cmd.setParameter("Volume", String.valueOf(level));
        issueCommand(cmd);
    }

    @Override
    public void enableVideo() throws CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Enabling video is not supported on Codec C90.");
    }

    @Override
    public void disableVideo() throws CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Disabling video is not supported on Codec C90.");
    }

    @Override
    public void startPresentation() throws CommandException
    {
        // TODO: test
        issueCommand(new Command("xCommand Presentation Start"));
    }

    @Override
    public void stopPresentation() throws CommandException
    {
        // TODO: test
        issueCommand(new Command("xCommand Presentation Stop"));
    }

    @Override
    public void showMessage(int duration, String text) throws CommandException
    {
        Command command = new Command("xCommand Message Alert Display");
        command.setParameter("Duration", duration);

        // treat special characters
        text = text.replace('"', '\''); // API does not support double-quote characters
        text = text.replace("\t", "    "); // API ignores tab characters
        text = text.replace("\n", "        "); // API does not support multi-line messages
        command.setParameter("Text", '"' + text + '"');

        issueCommand(command);
    }

    @Override
    public void standBy() throws CommandException
    {
        if (true /* TODO: should be "if there are some active calls" (maintain the device state) */) {
            // must hang up first for the command to work properly; otherwise, the command succeeds, but the device is
            //   not in the stand by state
            hangUpAll();

            // and we also must wait until all calls are really hung up; until then, the standby command has no effect
            final int attemptsLimit = 50;
            final int attemptDelay = 100;
            for (int i = 0; i < attemptsLimit; i++) {
                try {
                    Thread.sleep(attemptDelay); // wait awhile; who knows for how long to be sure, though :-(
                }
                catch (InterruptedException e) {
                    // ignore - the calls are checked anyway and possibly sleeping again
                    Thread.currentThread().interrupt(); // but don't swallow the interrupt information
                }
                Document calls = issueCommand(new Command("xStatus Call"));
                try {
                    if (getResultString(calls, "/XmlDoc/Status/*").isEmpty()) {
                        break;
                    }
                }
                catch (XPathExpressionException e) {
                    throw new CommandException("Program error in command execution.", e);
                }
            }
        }

        issueCommand(new Command("xCommand Standby Activate"));
    }
}
