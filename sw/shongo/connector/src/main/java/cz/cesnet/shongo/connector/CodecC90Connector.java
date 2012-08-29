package cz.cesnet.shongo.connector;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.connector.api.CommandException;
import cz.cesnet.shongo.connector.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.ConnectorInfo;
import cz.cesnet.shongo.connector.api.EndpointService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * A connector for Cisco TelePresence System Codec C90.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CodecC90Connector implements EndpointService
{

    public static final int MICROPHONES_COUNT = 8;

    public static void main(String[] args)
            throws IOException, JSchException, InterruptedException, SAXException, ParserConfigurationException,
                   XPathExpressionException
    {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        final String address;
        final String username;
        final String password;

        if (args.length > 0) {
            address = args[0];
        }
        else {
            System.out.print("address: ");
            address = in.readLine();
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
        conn.connect(address, username, password);

        Document result = conn.exec("xstatus SystemUnit uptimefs");
        if (isError(result)) {
            System.err.println("Error: " + getErrorMessage(result));
            System.exit(1);
        }

        System.out.println("Uptime: " + getResultString(result, "/XmlDoc/Status/SystemUnit/Uptime"));
        System.out.println("All done, disconnecting");
        conn.disconnect();
    }

    /**
     * The default port number to connect to.
     */
    public static final int DEFAULT_PORT = 22;

    /**
     * Shell channel open to the device.
     */
    private ChannelShell channel;

    /**
     * A writer for commands to be passed though the SSH channel to the device. Should be flushed explicitly.
     */
    private OutputStreamWriter commandStreamWriter;

    /**
     * A stream for reading results of commands.
     * Should be handled carefully (especially, it should not be buffered), because reading may cause a deadlock when
     * trying to read more than expected.
     */
    private InputStream commandResultStream;

    private static boolean staticInitialized = false;
    private static DocumentBuilder resultBuilder;
    private static XPathFactory xPathFactory;
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


    public CodecC90Connector() throws ParserConfigurationException, XPathExpressionException
    {
        if (!staticInitialized) {
            // NOTE: cannot be initialized in the static section since the possible exception
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            resultBuilder = factory.newDocumentBuilder();

            xPathFactory = XPathFactory.newInstance();
        }
    }

    /**
     * Connects the connector to the managed device on the default port.
     *
     * @param address  address of the device
     * @param username username to use for authentication on the device
     * @param password password to use for authentication on the device
     * @throws JSchException
     * @throws IOException
     */
    public void connect(String address, String username, final String password)
            throws JSchException, IOException
    {
        connect(address, DEFAULT_PORT, username, password);
    }

    /**
     * Connects the connector to the managed device.
     *
     * @param address  address of the device
     * @param port     device port to connect to
     * @param username username to use for authentication on the device
     * @param password password to use for authentication on the device
     * @throws JSchException
     * @throws IOException
     */
    public void connect(String address, int port, String username, final String password)
            throws JSchException, IOException
    {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, address, port);
        session.setPassword(password);
        // disable key checking - otherwise, the host key must be present in ~/.ssh/known_hosts
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        channel = (ChannelShell) session.openChannel("shell");
        commandStreamWriter = new OutputStreamWriter(channel.getOutputStream());
        commandResultStream = channel.getInputStream();
        channel.connect(); // runs a separate thread for handling the streams

        initSession();
    }

    private void initSession() throws IOException
    {
        // read the welcome message
        readOutput();

        sendCommand("echo off");
        // read the result of the 'echo off' command
        readOutput();

        sendCommand("xpreferences outputmode xml");
    }

    /**
     * Disconnects the connector from the managed device.
     *
     * @throws JSchException
     */
    public void disconnect() throws JSchException
    {
        Session session = channel.getSession();
        channel.disconnect();
        if (session != null) {
            session.disconnect();
        }
        commandStreamWriter = null;
        commandResultStream = null;
    }

    /**
     * Sends a command to the device. If some output is expected, blocks until the response is complete.
     *
     * @param command a command to the device
     * @return output of the command, or NULL if the output is not expected
     * @throws IOException
     */
    private Document exec(String command) throws IOException, SAXException
    {
        sendCommand(command);

        String output = readOutput();
        InputSource is = new InputSource(new StringReader(output));
        return resultBuilder.parse(is);
    }

    private void sendCommand(String command) throws IOException
    {
        if (commandStreamWriter == null) {
            throw new IllegalStateException("The connector is disconnected");
        }

        commandStreamWriter.write(command + '\n');
        commandStreamWriter.flush();
    }

    /**
     * Reads the output of the least recent unhandled command. Blocks until the output is complete.
     *
     * @return output of the least recent unhandled command
     * @throws IOException when the reading fails or end of the reading stream is met (which is not expected)
     */
    private String readOutput() throws IOException
    {
        if (commandResultStream == null) {
            throw new IllegalStateException("The connector is disconnected");
        }

        /**
         * Strings marking end of a command output.
         * Each must begin and end with "\r\n".
         */
        String[] endMarkers = new String[]{
                "\r\nOK\r\n",
                "\r\nERROR\r\n",
                "\r\n</XmlDoc>\r\n",
        };

        StringBuilder sb = new StringBuilder();
        int lastEndCheck = 0;
        int c;
reading:
        while ((c = commandResultStream.read()) != -1) {
            sb.append((char) c);
            if ((char) c == '\n') {
                // check for an output end marker
                for (String em : endMarkers) {
                    if (sb.indexOf(em, lastEndCheck) != -1) {
                        break reading;
                    }
                }
                // the next end marker check is needed only after this point
                lastEndCheck = sb.length() - 2; // one for the '\r' character, one for the end offset
            }
        }
        if (c == -1) {
            throw new IOException("Unexpected end of stream (was the connection closed?)");
        }
        return sb.toString();
    }

    /**
     * Finds out whether a given result XML denotes an error.
     *
     * @param result an XML document - result of a command
     * @return true if the result marks an error, false if the result is an ordinary result record
     */
    private static boolean isError(Document result) throws XPathExpressionException
    {
        String status = getResultString(result, "/XmlDoc/*[@status != '']/@status");
        return (status.contains("Error"));
    }

    /**
     * Given an XML result of a erroneous command, returns the error message.
     *
     * @param result an XML document - result of a command
     * @return error message contained in the result document, or null if the document does not denote an error
     */
    private static String getErrorMessage(Document result) throws XPathExpressionException
    {
        if (!isError(result)) {
            return null;
        }

        String reason = getResultString(result, "/XmlDoc/Status[@status='Error']/Reason");
        String xPath = getResultString(result, "/XmlDoc/Status[@status='Error']/XPath");
        if (!reason.isEmpty() || !xPath.isEmpty()) {
            return reason + " (XPath: " + xPath + ")";
        }

        String usage = getResultString(result, "/XmlDoc/*[@status='ParameterError']/../Usage");
        if (!usage.isEmpty()) {
            usage = usage.replace("&lt;", "<").replace("&gt;", ">");
            return "Parameter error. Usage: " + usage;
        }

        return "Uncategorized error";
    }

    /**
     * Send an xCommand command to the device.
     * In case of an error, throws a CommandException with a detailed message.
     *
     * @param command command to be issued; see the codec_c90_api_reference_guide_tc51.pdf for reference
     * @return the result of the command
     */
    private Document issueCommand(Command command) throws CommandException
    {
        try {
            Document result = exec(command.toString());
            if (isError(result)) {
                throw new CommandException(getErrorMessage(result));
            }
            return result;
        }
        catch (IOException e) {
            throw new RuntimeException("Command issuing error", e);
        }
        catch (SAXException e) {
            throw new RuntimeException("Command result parsing error", e);
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException("Command result handling error", e);
        }
    }

    @Override
    public void dial(Alias server) throws CommandException
    {
        Command command = new Command("xCommand Dial");
        command.setParameter("Number", server.getValue());
        command.setParameter("Protocol", server.getTechnology().getCode());
        // TODO: find out whether the device accepts all types of aliases, or whether it must be told explicitly
        // NOTE: the BookingId parameter could be used to identify the reservation for which this dial is issued in call
        //       logs; other connectors are missing such a feature, however, so do we

        issueCommand(command);
        // NOTE: the CallId returned by the command could be used for something
    }

    @Override
    public void resetDevice() throws CommandException
    {
        Command command = new Command("xCommand Boot");
        command.setParameter("Action", "Restart"); // should be default anyway, but just for sure...
        issueCommand(command);
    }

    @Override
    public void mute() throws CommandException
    {
        // TODO: test that it really mutes the device
        issueCommand(new Command("xCommand Audio Microphones Mute"));
    }

    @Override
    public void unmute() throws CommandException
    {
        // TODO: test that it really unmutes the device
        issueCommand(new Command("xCommand Audio Microphones Unmute"));
    }

    @Override
    public void setMicrophoneLevel(int level) throws CommandException
    {
        // TODO: test that it really affects the microphones gain
        for (int i = 0; i < MICROPHONES_COUNT; i++) {
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
        issueCommand(new Command("xCommand Presentation Start"));
    }

    @Override
    public void stopPresentation() throws CommandException
    {
        issueCommand(new Command("xCommand Presentation Stop"));
    }

    @Override
    public void standBy() throws CommandException
    {
        issueCommand(new Command("xCommand Standby Activate"));
    }

    @Override
    public ConnectorInfo getConnectorInfo()
    {
        return null; // FIXME: implement
    }
}
