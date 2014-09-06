package cz.cesnet.shongo.connector.device;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.DeviceLoadInfo;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.common.AbstractConnector;
import cz.cesnet.shongo.connector.common.AbstractDeviceConnector;
import cz.cesnet.shongo.connector.common.Command;
import cz.cesnet.shongo.connector.api.EndpointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A connector for Polycom HDX.
 *
 * NOTE: just a skeleton, has not been really connected to the device yet
 *
 * TODO: apply the following tips from the HDX API:
 *
 * - Polycom does not recommend sending multiple commands simultaneously without a pause or delay between them.
 *
 * - For commands with a single action and a single response: A delay of
 * 200 milliseconds between commands is usually sufficient. Examples of
 * these commands include the commands for switching cameras (camera
 * near 1), sending content (vcbutton play), and checking the status of the
 * audio mute (mute near get).
 *
 * - For commands with a single action and a more extensive response: The
 * time required to receive the response, and thus the time between
 * commands, may be longer than 200 milliseconds. The response length,
 * which can vary in size, determines the time required to receive the
 * response. Examples of these commands include the commands for
 * retrieving the local address book (addrbook all), the global address book
 * (gaddrbook all), the list of system settings (displayparams), and system
 * session information (whoami).
 *
 * - When developing your program, always allow enough time for the
 * response to the requested command to complete before sending another
 * command.
 *
 * - Do not send any commands while an incoming or outgoing call is being
 * established.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class PolycomHDXConnector extends AbstractDeviceConnector implements EndpointService
{
    private static Logger logger = LoggerFactory.getLogger(PolycomHDXConnector.class);

    /**
     * An example of interaction with the device.
     * <p/>
     * Just for debugging purposes.
     *
     * @param args
     */
    public static void main(String[] args) throws IOException, CommandException, CommandUnsupportedException
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

        final PolycomHDXConnector conn = new PolycomHDXConnector();
        conn.connect(DeviceAddress.parseAddress(address), username, password);

        try {
            conn.mute();
        }
        catch (CommandException e) {
            e.printStackTrace();
        }

        System.out.println("All done, disconnecting");
        conn.disconnect();
    }


    /**
     * Sends a command to the device. Blocks until response to the command is complete.
     *
     * @param command a command to the device; note that some parameters may be added to the command
     */
    private void exec(Command command)
    {
        // TODO
    }


    @Override
    public void connect(DeviceAddress deviceAddress, String username, String password) throws CommandException
    {
        // TODO
    }

    @Override
    public ConnectionState getConnectionState()
    {
        throw new TodoImplementException();
    }

    @Override
    public void disconnect() throws CommandException
    {
        // TODO
    }

    @Override
    public void standBy() throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public void rebootDevice() throws CommandException, CommandUnsupportedException
    {
        exec(new Command("reboot now")); // TODO: test
    }

    @Override
    public String dial(Alias alias) throws CommandException, CommandUnsupportedException
    {
        return null; // TODO: see dial command, and possibly mcupassword
    }

    @Override
    public void hangUp(String callId) throws CommandException
    {
        exec(new Command("hangup video " + callId)); // TODO: test
    }

    @Override
    public void hangUpAll() throws CommandException
    {
        exec(new Command("hangup all")); // TODO: test
    }

    @Override
    public void mute() throws CommandException
    {
        exec(new Command("mute near on")); // TODO: test
    }

    @Override
    public void unmute() throws CommandException
    {
        exec(new Command("mute near off")); // TODO: test
    }

    @Override
    public void setMicrophoneLevel(int level) throws CommandUnsupportedException
    {
        throw new CommandUnsupportedException();
    }

    @Override
    public void setPlaybackLevel(int level) throws CommandException
    {
        // adapt range - on the device, it is in <0..50>
        level /= 2;

        exec(new Command("volume set " + level)); // TODO: test
    }

    @Override
    public void enableVideo() throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public void disableVideo() throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public void startPresentation() throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public void stopPresentation() throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public void showMessage(int duration, String text) throws CommandException, CommandUnsupportedException
    {
        // TODO
    }
}
