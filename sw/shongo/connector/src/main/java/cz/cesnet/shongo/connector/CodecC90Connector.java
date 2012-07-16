package cz.cesnet.shongo.connector;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.*;

/**
 * A connector for Cisco TelePresence System Codec C90.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CodecC90Connector //implements EndpointService // FIXME: implement the EndpointService interface
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
        System.out.println(conn.exec("xstatus SystemUnit uptime"));
        System.out.println("All done, disconnecting");
        conn.disconnect();
    }


    /**
     * Shell channel open to the device.
     */
    private ChannelShell channel;

    private OutputStreamWriter commandStreamWriter;
    private InputStream commandResultStream;


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
        commandStreamWriter = new OutputStreamWriter(channel.getOutputStream());
        commandResultStream = channel.getInputStream();
        channel.connect(); // runs a separate thread for handling the streams

        initSession();
    }

    private void initSession() throws IOException, InterruptedException
    {
        // read the welcome message
        readOutput();

        exec("echo off");
        exec("xpreferences outputmode xml", false);
    }

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
     * Sends a command to the device. If the output is expected, blocks until the response is complete.
     *
     * @param command       a command to the device
     * @param expectOutput  whether to expect (and block for) output for this command
     * @return output of the command, or NULL if the output is not expected
     * @throws IOException
     */
    private String exec(String command, boolean expectOutput) throws IOException
    {
        if (commandStreamWriter == null) {
            throw new IllegalStateException("The connector is disconnected");
        }

        commandStreamWriter.write(command + '\n');
        commandStreamWriter.flush();
        if (expectOutput) {
            return readOutput();
        }
        else {
            return null;
        }
    }

    private String exec(String command) throws IOException
    {
        return exec(command, true);
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
        String[] endMarkers = new String[] {
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
}

