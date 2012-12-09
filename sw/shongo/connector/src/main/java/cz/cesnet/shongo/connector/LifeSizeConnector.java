package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.DeviceLoadInfo;
import cz.cesnet.shongo.connector.api.DeviceInfo;
import cz.cesnet.shongo.connector.api.EndpointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * A connector to Logitech LifeSize.
 * <p/>
 * TODO: test everything
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class LifeSizeConnector extends AbstractSSHConnector implements EndpointService
{
    private static Logger logger = LoggerFactory.getLogger(LifeSizeConnector.class);

    /**
     * Descriptions for errors. See the API or issue command "help errors" (with help-mode on) to get the current list.
     */
    private static final Map<Integer, String> errorDescriptions;

    /**
     * String used to separate columns in the command results.
     */
    private static final String COMMAND_RESULT_COLUMN_DELIMITER = ",";

    static {
        Map<Integer, String> em = new HashMap<Integer, String>();
        em.put(0x01, "No Memory");
        em.put(0x02, "File Error");
        em.put(0x03, "Invalid Instance");
        em.put(0x04, "Invalid Parameter");
        em.put(0x05, "Argument is not Repeatable");
        em.put(0x06, "Invalid Selection Parameter Value");
        em.put(0x07, "Missing Argument");
        em.put(0x08, "Extra Arguments on Command Line");
        em.put(0x09, "Invalid Command");
        em.put(0x0a, "Ambiguous Command");
        em.put(0x0b, "Conflicting Parameter");
        em.put(0x0c, "Operational Error");
        em.put(0x0d, "No Data Available");
        em.put(0x0e, "Not In Call");
        em.put(0x0f, "Interrupted");
        em.put(0x10, "Ambiguous Selection");
        em.put(0x11, "No Matching Entries");
        em.put(0x12, "Not Supported");
        errorDescriptions = Collections.unmodifiableMap(em);
    }

    public LifeSizeConnector()
    {
        super("^(?:ok|error),(\\p{XDigit}{2})$");
    }

    @Override
    protected void initSession() throws IOException
    {
        sendCommand(new Command("set help-mode off"));
        // read the result of the 'help-mode off' command
        readOutput();
    }

    @Override
    protected DeviceInfo getDeviceInfo() throws IOException, CommandException
    {
        DeviceInfo di = new DeviceInfo();

        di.setName(getResultFields(new Command("get system model"))[1]);
        di.setDescription(getResultString(new Command("get system name")));

        String[] sn = getResultFields(new Command("get system serial-number"));
        di.setSerialNumber(String.format("CPU board: %s, System board: %s", sn));

        di.setSoftwareVersion(getResultFields(new Command("get system version"))[1]);

        return di;
    }

    /**
     * Send a command to the device.
     * In case of an error, throws a CommandException with a detailed message.
     *
     * @param command command to be issued
     * @return the result of the command
     */
    private String issueCommand(Command command) throws CommandException
    {
        logger.info(String.format("%s issuing command %s on %s", LifeSizeConnector.class, command,
                info.getDeviceAddress()));

        try {
            sendCommand(command);
            String output = readOutput();
            Matcher lastLineMatcher = getLastLineMatcher();
            int errCode = Integer.parseInt(lastLineMatcher.group(1));
            if (errCode == 0) {
                logger.info(String.format("Command %s succeeded on %s", command, info.getDeviceAddress()));
                return output;
            }
            else {
                String description = errorDescriptions.get(errCode);
                logger.info(
                        String.format("Command %s failed on %s: %s", command, info.getDeviceAddress(), description));
                throw new CommandException(description);
            }
        }
        catch (IOException e) {
            logger.error("Error issuing a command", e);
            throw new CommandException("Command issuing error", e);
        }
    }

    /**
     * Issues a command and returns the first line of the output.
     *
     * @param command command to issue
     * @return first line of the command output, trimmed off the trailing whitespace
     * @throws CommandException
     */
    private String getResultString(Command command) throws CommandException
    {
        String output = issueCommand(command);
        return output.substring(0, output.indexOf('\n')).trim();
    }

    /**
     * Issues a command and returns array of fields from the first line of the result.
     *
     * @param command command to issue
     * @return fields from the first line of the result
     * @throws CommandException
     */
    private String[] getResultFields(Command command) throws CommandException
    {
        return getResultString(command).split(COMMAND_RESULT_COLUMN_DELIMITER);
    }

    /**
     * Adjusts a setting to a value, returning the original value of the setting.
     * <p/>
     * If the setting already has the needed value, it just returns the value. Otherwise, it sets the value to the
     * needed value.
     *
     * @param settingName name of the setting
     * @param neededValue value to set
     * @return the original value of the setting
     * @throws CommandException
     */
    private String adjustSetting(String settingName, String neededValue) throws CommandException
    {
        String originalValue = getResultString(new Command("get").addArgument(settingName));
        if (!originalValue.equals(neededValue)) {
            issueCommand(new Command("set").addArgument(settingName).addArgument(neededValue));
        }
        return originalValue;
    }

    /**
     * Adjusts a setting back after setting it to a requested value.
     * <p/>
     * To be used in conjunction with <code>adjustSetting</code>.
     *
     * @param settingName   name of the setting
     * @param adjustedValue value that the setting has been adjusted to
     * @param originalValue the original value before adjusting the setting
     * @throws CommandException
     */
    private void adjustSettingBack(String settingName, String adjustedValue, String originalValue)
            throws CommandException
    {
        if (!originalValue.equals(adjustedValue)) {
            issueCommand(new Command("set").addArgument(settingName).addArgument(originalValue));
        }
    }

    // COMMON SERVICE

    @Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException
    {
        DeviceLoadInfo info = new DeviceLoadInfo();

        try {
            // uptime
            String[] upArr = getResultFields(new Command("get system uptime")); // days, hours, minutes, seconds
            int uptime = Integer.parseInt(upArr[3])
                    + Integer.parseInt(upArr[2]) * 60
                    + Integer.parseInt(upArr[1]) * 60*60
                    + Integer.parseInt(upArr[0]) * 60*60*24;
            info.setUptime(uptime);

            // other info seem to be unsupported by the current API
            return info;
        }
        catch (NumberFormatException e) {
            throw new CommandException("Program error in parsing the command result.", e);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw new CommandException("Program error in parsing the command result.", e);
        }
    }

    // ENDPOINT SERVICE


    @Override
    public String dial(String address) throws CommandException
    {
        issueCommand(new Command("control call dial").addArgument(address));
        return ""; // FIXME get the call ID - catch the asynchronous message CS
    }

    @Override
    public String dial(Alias alias) throws CommandException
    {
        return dial(alias.getValue());
    }

    @Override
    public void hangUp(String callId) throws CommandException
    {
        issueCommand(new Command("control call hangup").addArgument(callId));
    }

    @Override
    public void hangUpAll() throws CommandException
    {
        issueCommand(new Command("control call hangup -a"));
    }

    @Override
    public void standBy() throws CommandException
    {
        issueCommand(new Command("control sleep"));
    }

    @Override
    public void resetDevice() throws CommandException
    {
        final int delay = 1; // delay before rebooting the device (just to catch the output)
        issueCommand(new Command("control reboot").addArgument(delay));
    }

    @Override
    public void mute() throws CommandException
    {
        // the "mute" command either applies to the active microphone, or all microphones, depending on the
        //   "mute-device" setting; as the command should mute the whole endpoint, we must properly adjust the setting
        String origValue = adjustSetting("audio mute-device", "all");
        issueCommand(new Command("set audio mute on"));
        adjustSettingBack("audio mute-device", "all", origValue);
    }

    @Override
    public void unmute() throws CommandException
    {
        // the "mute" command either applies to the active microphone, or all microphones, depending on the
        //   "mute-device" setting; as the command should mute the whole endpoint, we must properly adjust the setting
        String origValue = adjustSetting("audio mute-device", "all");
        issueCommand(new Command("set audio mute off"));
        adjustSettingBack("audio mute-device", "all", origValue);
    }

    @Override
    public void setMicrophoneLevel(int level) throws CommandException
    {
        level /= 5; // device takes the gain in the range 0..20
        issueCommand(new Command("set audio gain").addArgument(level));
    }

    @Override
    public void setPlaybackLevel(int level) throws CommandException
    {
        issueCommand(new Command("set volume speaker").addArgument(level));
    }

    @Override
    public void enableVideo() throws CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // NOTE: it seems it is unsupported by the current API
    }

    @Override
    public void disableVideo() throws CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // NOTE: it seems it is unsupported by the current API
    }

    @Override
    public void startPresentation() throws CommandException
    {
        issueCommand(new Command("set system presentation on")); // just for sure H.239 is enabled
        issueCommand(new Command(("control call presentation 1 start")));
    }

    @Override
    public void stopPresentation() throws CommandException
    {
        issueCommand(new Command(("control call presentation 1 stop")));
    }

    // TODO: show message: set system message -t <seconds> "message"
}
