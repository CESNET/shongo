package cz.cesnet.shongo.connector.device;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.DeviceLoadInfo;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.common.AbstractSSHConnector;
import cz.cesnet.shongo.connector.common.Command;
import cz.cesnet.shongo.connector.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cz.cesnet.shongo.Technology.H323;
import static cz.cesnet.shongo.Technology.SIP;

/**
 * A connector to Logitech LifeSize.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class LifeSizeConnector extends AbstractSSHConnector implements EndpointService, MonitoringService
{
    private static Logger logger = LoggerFactory.getLogger(LifeSizeConnector.class);

    /**
     * String used to separate columns in the command results.
     */
    private static final String COMMAND_RESULT_COLUMN_DELIMITER = ",";

    /**
     * String used as the shell prompt.
     */
    private static final String SHELL_PROMPT = "$ ";

    /**
     * Descriptions for errors. See the API or issue command "help errors" (with help-mode on) to get the current list.
     */
    private static final Map<Integer, String> ERROR_DESCRIPTIONS;

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
        ERROR_DESCRIPTIONS = Collections.unmodifiableMap(em);
    }

    /**
     * @see cz.cesnet.shongo.connector.api.EndpointDeviceState
     */
    private EndpointDeviceState state;

    /**
     * An example of interaction with the device.
     * <p/>
     * Just for debugging purposes.
     *
     * @param args
     */
    public static void main(String[] args) throws IOException, CommandException, InterruptedException
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

        final LifeSizeConnector conn = new LifeSizeConnector();
        conn.connect(DeviceAddress.parseAddress(address), username, password);

        System.out.printf("Device info: %s; %s (SN: %s, version: %s)\n", conn.getDeviceName(),
                conn.getDeviceDescription(), conn.getDeviceSerialNumber(), conn.getDeviceSoftwareVersion());

        System.out.println("Dialing...");
        String callId = conn.dial(new Alias(AliasType.H323_E164, "950087201"));
        System.out.printf("Dialing... (call id: %s)\n", callId);

        System.out.println("Sleeping for 10 seconds...");
        Thread.sleep(10000);
        System.out.println("Woke up!");

        EndpointDeviceState eds = conn.state;
        System.out.println("Active calls:");
        if (eds.getCalls().isEmpty()) {
            System.out.println("  -- none --");
        }
        else {
            for (CallInfo callInfo : eds.getCalls().values()) {
                System.out.println("  " + callInfo);
            }
        }

        if (callId == null) {
            System.out.println("CallID is null, hanging up all calls...\n");
            conn.hangUpAll();
        }
        else {
            System.out.printf("Hanging up call %s...\n", callId);
            conn.hangUp(callId);
        }

        Thread.sleep(15000);

        EndpointDeviceState eds2 = conn.state;
        System.out.println("Active calls:");
        if (eds2.getCalls().isEmpty()) {
            System.out.println("  -- none --");
        }
        else {
            for (CallInfo callInfo : eds2.getCalls().values()) {
                System.out.println("  " + callInfo);
            }
        }

        System.out.println("All done, disconnecting");
        conn.disconnect();
    }


    /**
     * Queue of asynchronous message to be processed.
     */
    private LinkedBlockingDeque<String[]> asyncMsgQueue = new LinkedBlockingDeque<String[]>();

    /**
     * Is the connector processing asynchronous messages now?
     */
    private boolean processingAsyncMessages = false;


    public LifeSizeConnector()
    {
        super("^(?:ok|error),(\\p{XDigit}{2})$");
    }

    @Override
    public void connect(DeviceAddress deviceAddress, String username, String password) throws CommandException
    {
        super.connect(deviceAddress, username, password);
        startMonitoring();
    }

    @Override
    public void disconnect() throws CommandException
    {
        stopMonitoring();
        super.disconnect();
    }

    private class MonitoringThread extends Thread
    {
        private final long sleepTime;
        private volatile boolean quitRequested = false;

        public MonitoringThread(long sleepTime)
        {
            super("Monitoring-" + deviceAddress);
            this.sleepTime = sleepTime;
        }

        public void run()
        {
            while (!quitRequested) {
                if (connectionState.equals(ConnectionState.CONNECTED)) {
                    flushAsynchronousMessages();
                }

                try {
                    sleep(sleepTime);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        public void quit()
        {
            quitRequested = true;
        }
    }

    private MonitoringThread monitoringThread = null;

    private void startMonitoring()
    {
        if (monitoringThread != null) {
            return;
        }
        monitoringThread = new MonitoringThread(2000);
        monitoringThread.start();
    }

    private void stopMonitoring()
    {
        if (monitoringThread == null) {
            logger.error("The monitoring thread for {} died unexpectedly.", deviceAddress);
        }
        else {
            monitoringThread.quit();
            monitoringThread = null;
        }
    }

    @Override
    protected void initSession() throws IOException
    {
        sendCommand(createCommand("set help-mode off"));
        // read the result of the 'help-mode off' command
        readOutput();
    }

    @Override
    protected void initDeviceInfo() throws IOException, CommandException
    {
        try {
            String[] serialNumber = getResultFields(createCommand("get system serial-number"));
            setDeviceName(getResultFields(createCommand("get system model"))[1]);
            setDeviceDescription(getResultString(createCommand("get system name")));
            setDeviceSerialNumber(String.format("CPU board: %s, System board: %s", serialNumber[0], serialNumber[1]));
            setDeviceSoftwareVersion(getResultFields(createCommand("get system version"))[1]);
        }
        catch (ArrayIndexOutOfBoundsException exception) {
            throw new CommandException("Error getting device info", exception);
        }
    }

    @Override
    protected void initDeviceState() throws IOException, CommandException
    {
        state = new EndpointDeviceState();

        String[] result;

        // calls
        String[] lines = issueCommand(createCommand("status call active"));
        for (String line : lines) {
            CallInfo ci = getCallInfo(line);
            if (ci != null) {
                state.getCalls().put(ci.getCallId(), ci);
            }
        }

        // mute status
        result = getResultFields(createCommand("get audio mute"));
        state.setMuted(("on".equals(result[0])));

        // sleeping status cannot be determined, it just comes from asynchronous messages when changed

        // presentation status
        result = getResultFields(createCommand("status presentation statistics"));
        if ("false".equals(result[1]) || "none".equals(result[2])) {
            state.setReceivingPresentation(false);
            state.setSendingPresentation(false);
        }
        else {
            state.setReceivingPresentation(("rx".equals(result[2])));
            state.setSendingPresentation(("tx".equals(result[2])));
        }

        flushAsynchronousMessages();
    }

    /**
     * Parses call info out of a string got from "status call active" command output.
     *
     * @param callInfoStr line as returned by the "status call active" command
     * @return call info, or <code>null</code> if there was an error and call info could not be parsed out
     */
    private static CallInfo getCallInfo(String callInfoStr)
    {
        if (callInfoStr == null) {
            throw new NullPointerException("callInfoStr");
        }

        String[] result = callInfoStr.split(COMMAND_RESULT_COLUMN_DELIMITER);
        CallInfo ci = new CallInfo();
        try {
            // call ID
            ci.setCallId(Integer.parseInt(result[0]));
        }
        catch (NumberFormatException e) {
            logger.error("Error parsing call status: expected an integer for call ID, got '{}'", result[0]);
            return null;
        }

        try {
            // conference ID
            ci.setConferenceId(Integer.parseInt(result[1]));
        }
        catch (NumberFormatException e) {
            logger.error("Error parsing call status: expected an integer for conference ID, got '{}'", result[1]);
            return null;
        }

        // call state
        CallState callState = DEVICE_CALL_STATES.get(result[2]);
        if (callState == null) {
            logger.error("Unknown call state got from active call list: '{}'", result[2]);
            return null;
        }
        ci.setState(callState);

        // incoming or outgoing
        ci.setIncoming("Yes".equals(result[3]));

        // call type - audio and video channel
        if (!fillChannelInfo(ci, result[4])) {
            logger.error("Unknown call type '{}' got from active call list with call ID {}", result[4], ci.getCallId());
            return null;
        }

        // technology
        String technology = result[8];
        if ("h323".equals(technology)) {
            ci.setTechnology(H323);
        }
        else if ("sip".equals(technology)) {
            ci.setTechnology(SIP); // TODO: only a guess, test whether it is really used as "sip"
        }
        else {
            logger.error("Unknown protocol '{}' got from active call list with call ID {}",
                    technology, ci.getCallId());
            return null;
        }

        // remote alias
        String aliasValue = result[5];
        AliasType aliasType = parseAliasType(ci.getTechnology(), aliasValue);
        if (aliasType == null) {
            logger.error("Unknown technology '{}' got for call ID {}, cannot make the remote party alias",
                    ci.getTechnology(), ci.getCallId());
            return null;
        }
        ci.setRemoteAlias(new Alias(aliasType, aliasValue));

        // start time
        if (result.length >= 11) {
            // the Duration field is in the call status only if it has been connected (which API does not mention...)
            ci.setStartTime(getStartTime(result[10]));
        }

        return ci;
    }

    private static Date getStartTime(String durationStr)
    {
        String[] durParts = durationStr.split(":");
        long durSec = Integer.parseInt(durParts[2]);
        durSec += Integer.parseInt(durParts[1]) * 60;
        durSec += Integer.parseInt(durParts[0]) * 60 * 60;
        return new Date(new Date().getTime() - durSec);
    }

    private static final Pattern E164_PATTERN = Pattern.compile("^\\+?\\d{10,14}$");

    private static AliasType parseAliasType(Technology technology, String aliasValue)
    {
        switch (technology) {
            case H323:
                if (E164_PATTERN.matcher(aliasValue).matches()) {
                    return AliasType.H323_E164;
                }
                else {
                    return AliasType.H323_URI;
                }
            case SIP:
                return AliasType.SIP_URI;
            default:
                return null;
        }
    }

    /**
     * Fills channel info in the supplied CallInfo object according to the contents of the corresponding field.
     *
     * @param ci            <code>CallInfo</code> object to set up
     * @param channelsField contents of the channels info field
     * @return <code>true</code> if the channel info gets recognized, <code>false</code> if not
     */
    private static boolean fillChannelInfo(CallInfo ci, String channelsField)
    {
        if ("Video".equals(channelsField)) {
            ci.setAudioContained(true);
            ci.setVideoContained(true);
        }
        else if ("Audio".equals(channelsField)) {
            ci.setAudioContained(true);
            ci.setVideoContained(false);
        }
        else if ("Unknown".equals(channelsField)) {
            ci.setAudioContained(null);
            ci.setVideoContained(null);
        }
        else {
            return false;
        }

        return true;
    }

    /**
     * Mapping of device call states to generic call states recognized by Shongo.
     */
    private static final Map<String, CallState> DEVICE_CALL_STATES;

    static {
        Map<String, CallState> dcs = new HashMap<String, CallState>();
        dcs.put("On Hook", CallState.TERMINATED);
        dcs.put("Terminating", CallState.TERMINATING);
        dcs.put("Terminated", CallState.TERMINATED);
        dcs.put("Off Hook", CallState.TERMINATED);
        dcs.put("Valid Number", CallState.DIALING);
        dcs.put("Dialing", CallState.DIALING);
        dcs.put("Proceeding", CallState.DIALING);
        dcs.put("Ringing", CallState.RINGING);
        dcs.put("Ringback", CallState.RING_INCOMING); // TODO: test whether it is really that, API is quite brief...
        dcs.put("Answered Number", CallState.RINGING);
        dcs.put("Answered Consult", CallState.RINGING);
        dcs.put("Connected", CallState.CONNECTED);
        dcs.put("Call Encrypted", CallState.CONNECTED);
        dcs.put("Call Not Encrypted", CallState.CONNECTED);
        dcs.put("Notify Info", CallState.OTHER);
        dcs.put("Ring Incoming", CallState.RING_INCOMING);
        dcs.put("Caller ID", CallState.OTHER);
        dcs.put("Local Ring Back Off", CallState.OTHER);
        dcs.put("Remote Pres Begin", CallState.OTHER);
        dcs.put("Remote Pres End", CallState.OTHER);
        dcs.put("Remote Pres Failed", CallState.OTHER);
        dcs.put("Far End Mute", CallState.CONNECTED);
        dcs.put("Far End Unmute", CallState.CONNECTED);
        dcs.put("Far End Hold", CallState.CONNECTED);
        dcs.put("Far End Resume", CallState.CONNECTED);
        DEVICE_CALL_STATES = dcs;
    }

    /**
     * Creates a new Command with formatting set for this type of device.
     *
     * @param command command text
     * @return a new Command object set appropriately for this connector
     */
    private static Command createCommand(String command)
    {
        return new Command(command, " ");
    }

    private volatile ReentrantLock commandLock = new ReentrantLock();

    /**
     * Send a command to the device.
     * In case of an error, throws a CommandException with a detailed message.
     *
     * @param command command to be issued
     * @return lines of result of the command (without the error code line and the blank line before it)
     */
    private String[] issueCommand(Command command) throws CommandException
    {
        if (command.getCommand().trim().isEmpty()) {
            // for flushing commands, just try to get the lock
            if (!commandLock.tryLock()) {
                return null;
            }
        }
        else {
            commandLock.lock();
        }

        try {
            logger.debug(String.format("Issuing command '%s' on %s", command, deviceAddress));
            sendCommand(command);
Reading:
            while (true) {
                String output = readOutput();
                Matcher lastLineMatcher = getLastLineMatcher();
                int errCode = Integer.parseInt(lastLineMatcher.group(1));
                if (errCode != 0) {
                    String description = ERROR_DESCRIPTIONS.get(errCode);
                    logger.debug(String.format("Command %s failed on %s: %s", command, deviceAddress, description));
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

                        // OK, the command output follows until a blank line, which denotes end of the command output
                        int startLine = i + 1;
                        int endLine = startLine;
                        for (; endLine < lines.length; endLine++) {
                            if (lines[endLine].trim().isEmpty()) {
                                break;
                            }
                        }

                        logger.debug(String.format("Command '%s' succeeded on %s", command, deviceAddress));
                        return Arrays.copyOfRange(lines, startLine, endLine);
                    }
                    else {
                        // asynchronous message received after the output of the previous command
                        asyncMsgQueue.add(line.split(COMMAND_RESULT_COLUMN_DELIMITER));
                    }
                }

                throw new CommandException("Unexpected end of command output");
            }
        }
        catch (IOException e) {
            logger.error("Error issuing command '" + command + "'", e);
            reconnect();
            throw new CommandException("Command issuing error", e);
        }
        finally {
            commandLock.unlock();

            if (!processingAsyncMessages && !asyncMsgQueue.isEmpty()) {
                processingAsyncMessages = true;
                while (!asyncMsgQueue.isEmpty()) {
                    String[] msg = asyncMsgQueue.pop();
                    processAsynchronousMessage(msg);
                }
                processingAsyncMessages = false;
            }
        }
    }

    /**
     * Asks the connector to get all unhandled asynchronous messages.
     */
    private void flushAsynchronousMessages()
    {
        try {
            // NOTE: it's necessary to send an empty command to the device; otherwise, we wouldn't know how much to read
            issueCommand(createCommand(" "));
        }
        catch (CommandException e) {
            logger.error("Error flushing asynchronous messages", e);
        }
    }

    /**
     * Processes an asynchronous message. Some messages are ignored, some change the device state.
     * <p/>
     * Watch out that the device state may not be initialized yet (in which case it is <code>null</code>).
     *
     * @param fields fields containing an asynchronous message
     */
    private void processAsynchronousMessage(String[] fields)
    {
        if (fields[0].equals("CS")) {
            logger.debug("Processing Call Status message: {},{},{},{},{},{},{},{}", fields);
            processCallStatusMessage(fields, false);
        }
        else if (fields[0].equals("IC")) {
            logger.debug("Processing Incoming Call message: {},{},{},{},{},{},{}", fields);
            processCallStatusMessage(fields, true);
        }
        else if (fields[0].equals("PS")) {
            logger.debug("Processing Presentation Status message: {},{},{},{},{},{}", fields);
            boolean remote = ("Yes".equals(fields[4]));
            String state = fields[3]; // Initiated, Terminated, or Relinquished
            if ("Relinquished".equals(state)) {
                this.state.setSendingPresentation(false);
                this.state.setReceivingPresentation(true);
            }
            else if ("Initiated".equals(state)) {
                if (remote) {
                    this.state.setReceivingPresentation(true);
                }
                else {
                    this.state.setSendingPresentation(true);
                }
            }
            else if ("Terminated".equals(state)) {
                if (remote) {
                    this.state.setReceivingPresentation(false);
                }
                else {
                    this.state.setSendingPresentation(false);
                }
            }
            else {
                logger.error("Unknown presentation state got from an asynchronous message: '{}'", state);
            }
        }
        else if (fields[0].equals("FC")) {
            logger.debug("Processing Far Camera message: {},{},{},{},{},{}", fields);
            // ignoring
        }
        else if (fields[0].equals("MS")) {
            logger.debug("Processing Mute Status message: {},{}", fields);
            this.state.setMuted("true".equals(fields[1]));
        }
        else if (fields[0].equals("VC")) {
            logger.debug("Processing Video Capabilities message: {},{},{},{},{}", fields);
            // ignoring
        }
        else if (fields[0].equals("SS")) {
            logger.debug("Processing System Sleep message: {},{}", fields);
            this.state.setSleeping("true".equals(fields[1]));
        }
        else {
            logger.error("Unknown asynchronous message encountered: {}", fields);
        }
    }

    private void processCallStatusMessage(String[] fields, boolean incoming)
    {
        Map<Integer, CallInfo> calls = state.getCalls();

        CallState callState = DEVICE_CALL_STATES.get(fields[3]);
        if (callState == null) {
            logger.error("Unknown call state got from asynchronous message: {}", fields[3]);
            callState = CallState.OTHER;
        }

        int callId;

        try {
            callId = Integer.parseInt(fields[1]);
        }
        catch (NumberFormatException e) {
            logger.error("Error parsing call status: expected an integer for call ID, got '{}'", fields[1]);
            return;
        }

        if (callState == CallState.TERMINATED) {
            logger.debug("Terminated call #{}", callId);
            calls.remove(callId);
            return;
        }

        // if the call is new, get any available info about it
        CallInfo ci = calls.get(callId);
        if (ci == null) {
            try {
                String res = getResultString(createCommand("status call active").setParameter("-C", callId));
                if (res != null && !res.isEmpty()) {
                    ci = getCallInfo(res);
                }
                if (ci == null) {
                    ci = new CallInfo(); // the call has not yet appeared among the active calls, create a blank one
                    ci.setCallId(callId);
                }
                calls.put(ci.getCallId(), ci);
                logger.debug("Created call #{}", callId);
            }
            catch (CommandException e) {
                logger.error("Could not get call status for call " + callId, e);
                return;
            }
        }
        else {
            logger.debug("Call #{} changed from {}", callId, ci);
        }

        CallState originalCallState = ci.getState();
        ci.setState(callState);

        // in certain state changes, more info about the call show up - in these cases load the whole call
        if (originalCallState != null && (
                (!originalCallState.isActive() && callState.isActive()) ||
                        (!originalCallState.hasConnected() && callState.hasConnected())
        )) {
            try {
                String res = getResultString(createCommand("status call active").setParameter("-C", callId));
                if (res != null && !res.isEmpty()) {
                    CallInfo refreshed = getCallInfo(res);
                    if (refreshed != null) {
                        ci = refreshed;
                        calls.put(callId, ci);
                        logger.debug("Refreshed info about call #{} to {}", callId, ci);
                        return;
                    }
                }
            }
            catch (CommandException e) {
                logger.error("Could not get call status for call " + callId, e);
            }
        }

        try {
            ci.setConferenceId(Integer.parseInt(fields[2]));
        }
        catch (NumberFormatException e) {
            logger.error("Error parsing call status: expected integer for conference ID, got '{}'", fields[2]);
        }

        if (!fillChannelInfo(ci, fields[4])) {
            logger.error("Unknown call type '{}' got from active call list with call ID {}", fields[4], ci.getCallId());
        }

        if (ci.getTechnology() != null && fields.length > 6) {
            String aliasValue = (incoming ? fields[5] : fields[6]);
            AliasType aliasType = parseAliasType(ci.getTechnology(), aliasValue);
            if (aliasType == null) {
                logger.error("Unknown technology '{}' got for call ID {}, cannot make the remote party alias",
                        ci.getTechnology(), ci.getCallId());
            }
            else {
                ci.setRemoteAlias(new Alias(aliasType, aliasValue));
            }
        }

        logger.debug("Call #{} changed to {}", callId, ci);
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
        if (output.length == 0) {
            return null;
        }
        else {
            return output[0];
        }
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
        String resStr = getResultString(command);
        if (resStr == null) {
            return null;
        }
        else {
            return resStr.split(COMMAND_RESULT_COLUMN_DELIMITER);
        }
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

    @Override
    public UsageStats getUsageStats() throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException();
    }

    // ENDPOINT SERVICE

    @Override
    public String dial(Alias alias) throws CommandException
    {
        String address = alias.getValue();

        // get a snapshot of calls
        Set<Integer> originalCallIds = new TreeSet<Integer>(state.getCalls().keySet());

        issueCommand(createCommand("control call dial").addArgument(address));

        // get an asynchronous message about initiating the call
        final int attemptsLimit = 50;
        final int attemptDelay = 100;
        for (int i = 0; i < attemptsLimit; i++) {
            flushAsynchronousMessages();
            for (Integer callId : state.getCalls().keySet()) {
                if (!originalCallIds.contains(callId)) {
                    return String.valueOf(callId);
                }
            }

            // not yet initiated, wait for awhile and try again
            try {
                Thread.sleep(attemptDelay);
            }
            catch (InterruptedException e) {
                // ignore - the calls are checked anyway and possibly sleeping again
                Thread.currentThread().interrupt(); // but don't swallow the interrupt information
            }
        }

        // the call has not appeared so far, sorry...
        return null;
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
        // NOTE: it does not really sleep the device, it just parks the camera into sleep position, but it is still transmitting video
    }

    @Override
    public void rebootDevice() throws CommandException
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
