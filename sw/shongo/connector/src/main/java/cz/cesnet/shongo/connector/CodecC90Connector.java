package cz.cesnet.shongo.connector;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import cz.cesnet.shongo.connector.api.ConnectorInfo;
import cz.cesnet.shongo.connector.api.EndpointService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A connector for Cisco TelePresence System Codec C90.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CodecC90Connector implements EndpointService
{
    public static void main(String[] args) throws IOException, JSchException, InterruptedException
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
        conn.exec("xstatus SystemUnit uptime");
        conn.disconnect();
    }


    /**
     * A stream for providing commands to the device.
     */
    private CommandInputStream commandInputStream = new CommandInputStream();

    /**
     * A reader for results of commands.
     */
    private CommandResultStream commandResultStream = new CommandResultStream(new String[]{
            "\r\nOK\r\n",
            "\r\nERROR\r\n",
            "</XmlDoc>",
    });

    /**
     * Shell channel open to the device.
     */
    private ChannelShell channel;


    public void connect(String address, String username, final String password)
            throws JSchException, IOException, InterruptedException
    {
        connect(address, 22, username, password);
    }

    public void connect(String address, int port, String username, final String password)
            throws JSchException, IOException, InterruptedException
    {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, address, port);
        session.setPassword(password);
        // disable key checking - otherwise, the host key must be present in ~/.ssh/known_hosts
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        channel = (ChannelShell) session.openChannel("shell");
        channel.setInputStream(commandInputStream);
//        channel.setOutputStream(commandResultStream);
        channel.setOutputStream(System.out);
        channel.connect(); // runs a separate thread for handling the streams

        initSession();
    }

    private void initSession() throws IOException, InterruptedException
    {
        // read the welcome message
        //commandResultStream.getOutput();

        exec("echo off");
        exec("xpreferences outputmode xml", false);
    }

    public void disconnect() throws JSchException
    {
        Session session = channel.getSession();
        channel.disconnect();
        channel = null;
        session.disconnect();
    }

    /**
     * Sends a command to the device. If the output is expected, blocks until the response is complete.
     *
     * @param command       a command to the device
     * @param expectOutput  whether to expect (and block for) output for this command
     * @return output of the command, or NULL if the output is not expected
     * @throws IOException
     */
    private String exec(String command, boolean expectOutput) throws IOException, InterruptedException
    {
        commandInputStream.pushCommand(command);
        if (expectOutput) {
            return commandResultStream.getOutput();
        }
        else {
            return null;
        }
    }

    private String exec(String command) throws IOException, InterruptedException
    {
        return exec(command, false);
    }

    @Override
    public void dial(String server)
    {
    }

    @Override
    public void resetDevice()
    {
    }

    @Override
    public void mute()
    {
    }

    @Override
    public void unmute()
    {
    }

    @Override
    public void setMicrophoneLevel(int level)
    {
    }

    @Override
    public void setPlaybackLevel(int level)
    {
    }

    @Override
    public void enableVideo()
    {
    }

    @Override
    public void disableVideo()
    {
    }

    @Override
    public ConnectorInfo getConnectorInfo()
    {
        return null;
    }
}

