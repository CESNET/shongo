package cz.cesnet.shongo.connector;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.connector.api.ConnectorInfo;
import cz.cesnet.shongo.connector.api.DeviceInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A common superclass for all connectors using SSH for communication with the device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
abstract public class AbstractSSHConnector extends AbstractConnector
{
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

    /**
     * Regular expression for a complete command output.
     *
     * When the output is being read from the device, each line (terminated with '\n') gets trimmed and matched against
     * the specified regular expression. If the expression matches the trimmed line, the whole content read so far
     * is returned as the command output (including the trailing whitespace) and nothing else is read from the device.
     *
     * It is important to specify an exact expression. If the expression does not recognize the end of a command output,
     * the connector would end up in a deadlock (expecting some more output from the device, which would not come,
     * though). On the other hand, if the expression matched only a part of a command output, the command would be
     * answered with an incomplete result string and the following commands would be answered with the rest of
     * a previous result.
     *
     * Note that it is probably a good idea to put '$' at the end of the pattern for matching the end of the string.
     */
    private Pattern commandOutputPattern;

    private Matcher lastLineMatcher = null;

    protected AbstractSSHConnector(String commandOutputPatternStr)
    {
        commandOutputPattern = Pattern.compile(commandOutputPatternStr);
    }

    /**
     * Connects the connector to the managed device.
     *
     * @param address  device address to connect to
     * @param username username to use for authentication on the device
     * @param password password to use for authentication on the device
     */
    @Override
    public void connect(Address address, String username, String password) throws CommandException
    {
        if (address.getPort() == Address.DEFAULT_PORT) {
            address.setPort(DEFAULT_PORT);
        }

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, address.getHost(), address.getPort());
            session.setPassword(password);
            // disable key checking - otherwise, the host key must be present in ~/.ssh/known_hosts
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            channel = (ChannelShell) session.openChannel("shell");
            commandStreamWriter = new OutputStreamWriter(channel.getOutputStream());
            commandResultStream = channel.getInputStream();
            channel.connect(); // runs a separate thread for handling the streams

            info.setConnectionState(ConnectorInfo.ConnectionState.CONNECTED);
            info.setDeviceAddress(address);

            initSession();
            info.setDeviceInfo(gatherDeviceInfo());
            initDeviceState();
        }
        catch (JSchException e) {
            throw new CommandException("Error in communication with the device", e);
        }
        catch (IOException e) {
            throw new CommandException("Error connecting to the device", e);
        }
    }

    /**
     * A hook method for initialization of the session. It gets called right after connecting to the device.
     * @throws IOException
     */
    protected void initSession() throws IOException
    {
    }

    /**
     * Gets static info about the device (i.e., name, description, ...).
     *
     * @return a <code>DeviceInfo</code> object
     */
    protected abstract DeviceInfo gatherDeviceInfo() throws IOException, CommandException;

    /**
     * Initializes state info about the device (i.e., mute state, active calls, ...).
     */
    protected abstract void initDeviceState() throws IOException, CommandException;


    /**
     * Disconnects the connector from the managed device.
     */
    public void disconnect() throws CommandException
    {
        Session session = null;
        if (channel != null) {
            try {
                session = channel.getSession();
            }
            catch (JSchException e) {
                throw new CommandException("Error disconnecting from the device", e);
            }
            channel.disconnect();
        }

        if (session != null) {
            session.disconnect();
        }
        commandStreamWriter = null; // just for sure the streams will not be used
        commandResultStream = null;

        info.setConnectionState(ConnectorInfo.ConnectionState.DISCONNECTED);
    }

    protected void sendCommand(Command command) throws IOException
    {
        if (info.getConnectionState() == ConnectorInfo.ConnectionState.DISCONNECTED) {
            throw new IllegalStateException("The connector is disconnected");
        }

        commandStreamWriter.write(command.toString() + '\n');
        commandStreamWriter.flush();
    }

    /**
     * Reads the output of the least recent unhandled command. Blocks until the output is complete.
     *
     * @return output of the least recent unhandled command
     * @throws IOException when the reading fails or end of the reading stream is met (which is not expected)
     */
    protected String readOutput() throws IOException
    {
        if (info.getConnectionState() == ConnectorInfo.ConnectionState.DISCONNECTED) {
            throw new IllegalStateException("The connector is disconnected");
        }
        if (commandResultStream == null) {
            throw new NullPointerException("The result stream is NULL, even though the connector seems connected.");
        }

        StringBuilder output = new StringBuilder();
        StringBuilder lastLineBuf = new StringBuilder();
        int c;
        while ((c = commandResultStream.read()) != -1) {
            output.append((char) c);
            lastLineBuf.append((char) c);
            if ((char) c == '\n') {
                // the line got terminated, check whether to stop reading
                String lastLine = lastLineBuf.toString().trim();
                Matcher matcher = commandOutputPattern.matcher(lastLine);
                if (matcher.matches()) {
                    lastLineMatcher = matcher;
                    break;
                }
                else {
                    lastLineBuf.delete(0, lastLineBuf.length());
                }
            }
        }
        if (c == -1) {
            throw new IOException("Unexpected end of stream (was the connection closed?)");
        }
        return output.toString();
    }

    /**
     * Returns the matcher for the last line of the last command output.
     *
     * May be used to get parts of the last line captured by groups defined by <code>commandOutputPattern</code>.
     */
    protected Matcher getLastLineMatcher()
    {
        return lastLineMatcher;
    }
}
