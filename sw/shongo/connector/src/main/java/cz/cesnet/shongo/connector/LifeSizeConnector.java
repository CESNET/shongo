package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.DeviceLoadInfo;
import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.connector.api.DeviceInfo;
import cz.cesnet.shongo.connector.api.EndpointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
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

    /**
     * String used as the shell prompt.
     */
    private static final String SHELL_PROMPT = "$ ";

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

    /**
     * An example of interaction with the device.
     *
     * Just for debugging purposes.
     *
     * @param args
     */
    public static void main(String[] args) throws IOException, CommandException, InterruptedException
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

        final LifeSizeConnector conn = new LifeSizeConnector();
        conn.connect(Address.parseAddress(address), username, password);

        DeviceInfo di = conn.getDeviceInfo();
        System.out.printf("Device info: %s; %s (SN: %s, version: %s)\n",
                di.getName(), di.getDescription(), di.getSerialNumber(), di.getSoftwareVersion());

        String callId = conn.dial("950087201");
        System.out.printf("Dialing... (call id: %s)\n", callId);

        Thread.sleep(10000);

        System.out.printf("Hanging up call %s...\n", callId);
//        conn.hangUp(callId);
        conn.hangUpAll();

        Thread.sleep(2000);

        System.out.println("All done, disconnecting");
        conn.disconnect();
    }

    public LifeSizeConnector()
    {
        super("^(?:ok|error),(\\p{XDigit}{2})$");
    }

    @Override
    protected void initSession() throws IOException
    {
        sendCommand(createCommand("set help-mode off"));
        // read the result of the 'help-mode off' command
        readOutput();
    }

    @Override
    protected DeviceInfo getDeviceInfo() throws IOException, CommandException
    {
        try {
            DeviceInfo di = new DeviceInfo();

            di.setName(getResultFields(createCommand("get system model"))[1]);
            di.setDescription(getResultString(createCommand("get system name")));

            String[] sn = getResultFields(createCommand("get system serial-number"));
            di.setSerialNumber(String.format("CPU board: %s, System board: %s", sn));

            di.setSoftwareVersion(getResultFields(createCommand("get system version"))[1]);

            return di;
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw new CommandException("Error getting device info", e);
        }
    }

    /**
     * Creates a new Command with formatting set for this type of device.
     *
     * @param command command text
     * @return a new Command object set appropriately for this connector
     */
    private Command createCommand(String command)
    {
        return new Command(command, " ");
    }

    /**
     * Send a command to the device.
     * In case of an error, throws a CommandException with a detailed message.
     *
     * @param command command to be issued
     * @return lines of result of the command
     */
    private String[] issueCommand(Command command) throws CommandException
    {
        logger.info(String.format("%s issuing command %s on %s", LifeSizeConnector.class, command,
                info.getDeviceAddress()));

        try {
            sendCommand(command);
Reading:
            while (true) {
                String output = readOutput();
                logger.debug("Command output: {}", output);
                Matcher lastLineMatcher = getLastLineMatcher();
                int errCode = Integer.parseInt(lastLineMatcher.group(1));
                if (errCode != 0) {
                    String description = errorDescriptions.get(errCode);
                    logger.info(
                            String.format("Command %s failed on %s: %s", command, info.getDeviceAddress(), description));
                    throw new CommandException(description);
                }

                // the device echoes the command (and it cannot be disabled) - so cut the first line
                // moreover, asynchronous messages might have been printed since last command result
                String[] lines = output.split("\\r\\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    if (line.isEmpty() || line.equals(SHELL_PROMPT)) {
                        // just a rubbish left by asynchronous messages
                        // ignore it
                    }
                    else if (line.equals("ok,00")) {
                        // left by a spontaneous asynchronous message - read more output
                        continue Reading;
                    }
                    else if (line.startsWith(SHELL_PROMPT)) {
                        // this line should contain the issued command and the rest should contain the command output
                        if (!line.equals(SHELL_PROMPT + command.toString())) {
                            String message = String.format(
                                    "Read output of an unexpected command - probably a connector bug (command: %s)",
                                    command);
                            throw new CommandException(message);
                        }

                        // OK, the command output follows
                        return Arrays.copyOfRange(lines, i + 1, lines.length);
                    }
                    else {
                        // asynchronous message
                        processAsynchronousMessage(line.split(COMMAND_RESULT_COLUMN_DELIMITER));
                    }
                }

                logger.info(String.format("Command %s succeeded on %s", command, info.getDeviceAddress()));
                return lines;
            }
        }
        catch (IOException e) {
            logger.error("Error issuing a command", e);
            throw new CommandException("Command issuing error", e);
        }
    }

    /**
     * Asks the connector to get all unhandled asynchronous messages.
     */
    private void flushAsynchronousMessages() throws CommandException
    {
        issueCommand(new Command(" "));
    }

    /**
     * Processes an asynchronous message.
     *
     * @param fields fields containing an asynchronous message
     */
    private void processAsynchronousMessage(String[] fields)
    {
        if (fields[0].equals("CS")) {
            logger.debug("Call Status message: {},{},{},{},{},{},{},{}", fields);
            // ignoring
        }
        else if (fields[0].equals("IC")) {
            logger.debug("Incoming Call message: {},{},{},{},{},{},{}", fields);
            // ignoring
        }
        else if (fields[0].equals("FC")) {
            logger.debug("Far Camera message: {},{},{},{},{},{}", fields);
            // ignoring
        }
        else if (fields[0].equals("MS")) {
            logger.debug("Mute Status message: {},{}", fields);
            // ignoring
        }
        else if (fields[0].equals("VC")) {
            logger.debug("Video Capabilities message: {},{},{},{},{}", fields);
            // ignoring
        }
        else if (fields[0].equals("SS")) {
            logger.debug("System Sleep message: {},{}", fields);
            // ignoring
        }
        else {
            logger.error("Unknown asynchronous message encountered: {}", fields);
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
        String[] output = issueCommand(command);
        return output[0];
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
        String originalValue = getResultString(createCommand("get").addArgument(settingName));
        if (!originalValue.equals(neededValue)) {
            issueCommand(createCommand("set").addArgument(settingName).addArgument(neededValue));
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
            issueCommand(createCommand("set").addArgument(settingName).addArgument(originalValue));
        }
    }

    // COMMON SERVICE

    @Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException
    {
        DeviceLoadInfo info = new DeviceLoadInfo();

        try {
            // uptime
            String[] upArr = getResultFields(createCommand("get system uptime")); // days, hours, minutes, seconds
            int uptime = Integer.parseInt(upArr[3])
                    + Integer.parseInt(upArr[2]) * 60
                    + Integer.parseInt(upArr[1]) * 60 * 60
                    + Integer.parseInt(upArr[0]) * 60 * 60 * 24;
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
        issueCommand(createCommand("control call dial").addArgument(address));
        while (true) {
            flushAsynchronousMessages();
            // get the call ID from the device status
            if (true) { // FIXME: if there is a new call to address
                return ""; // FIXME: return the ID of the call
            }
        }
    }

    @Override
    public String dial(Alias alias) throws CommandException
    {
        return dial(alias.getValue());
    }

    @Override
    public void hangUp(String callId) throws CommandException
    {
        issueCommand(createCommand("control call hangup").addArgument(callId));
    }

    @Override
    public void hangUpAll() throws CommandException
    {
        issueCommand(createCommand("control call hangup -a"));
    }

    @Override
    public void standBy() throws CommandException
    {
        issueCommand(createCommand("control sleep"));
    }

    @Override
    public void resetDevice() throws CommandException
    {
        final int delay = 1; // delay before rebooting the device (just to catch the output)
        issueCommand(createCommand("control reboot").addArgument(delay));
    }

    @Override
    public void mute() throws CommandException
    {
        // the "mute" command either applies to the active microphone, or all microphones, depending on the
        //   "mute-device" setting; as the command should mute the whole endpoint, we must properly adjust the setting
        String origValue = adjustSetting("audio mute-device", "all");
        issueCommand(createCommand("set audio mute on"));
        adjustSettingBack("audio mute-device", "all", origValue);
    }

    @Override
    public void unmute() throws CommandException
    {
        // the "mute" command either applies to the active microphone, or all microphones, depending on the
        //   "mute-device" setting; as the command should mute the whole endpoint, we must properly adjust the setting
        String origValue = adjustSetting("audio mute-device", "all");
        issueCommand(createCommand("set audio mute off"));
        adjustSettingBack("audio mute-device", "all", origValue);
    }

    @Override
    public void setMicrophoneLevel(int level) throws CommandException
    {
        level /= 5; // device takes the gain in the range 0..20
        issueCommand(createCommand("set audio gain").addArgument(level));
    }

    @Override
    public void setPlaybackLevel(int level) throws CommandException
    {
        issueCommand(createCommand("set volume speaker").addArgument(level));
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
        issueCommand(createCommand("set system presentation on")); // just for sure H.239 is enabled
        issueCommand(createCommand("control call presentation 1 start"));
    }

    @Override
    public void stopPresentation() throws CommandException
    {
        issueCommand(createCommand("control call presentation 1 stop"));
    }

    @Override
    public void showMessage(int duration, String text) throws CommandException
    {
        Command command = createCommand("set system message");
        // treat special characters
        text = text.replace('"', '\''); // double-quote character may not be entered
        text = text.replace("\n", "\\n"); // newlines are to be passed as '\n' sequences
        // NOTE: other special characters are treated as they are - '\' only escapes 'n' to make a newline

        command.addArgument('"' + text + '"');
        command.setParameter("-t", duration);
        issueCommand(command);
    }
}
