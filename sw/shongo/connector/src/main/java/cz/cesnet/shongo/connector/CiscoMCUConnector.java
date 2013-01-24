package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.api.xmlrpc.KeepAliveTransportFactory;
import cz.cesnet.shongo.connector.api.*;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.util.HostTrustManager;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A connector for Cisco TelePresence MCU.
 * <p/>
 * Uses HTTPS (only).
 * <p/>
 * Works using API 2.9. The following Cisco TelePresence products are supported, provided they are running MCU version
 * 4.3 or later:
 * - Cisco TelePresence MCU 4200 Series
 * - Cisco TelePresence MCU 4500 Series
 * - Cisco TelePresence MCU MSE 8420
 * - Cisco TelePresence MCU MSE 8510
 * <p/>
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CiscoMCUConnector extends AbstractConnector implements MultipointService
{
    private static Logger logger = LoggerFactory.getLogger(CiscoMCUConnector.class);

    /**
     * Options for the {@link CiscoMCUConnector}.
     */
    public static final String ROOM_NUMBER_EXTRACTION_FROM_H323_NUMBER = "room-number-extraction-from-h323-number";
    public static final String ROOM_NUMBER_EXTRACTION_FROM_SIP_URI = "room-number-extraction-from-sip-uri";

    /**
     * Maximum length of string which can be sent to the device.
     */
    private static final int STRING_MAX_LENGTH = 31;

    /**
     * A safety limit for number of enumerate pages.
     * <p/>
     * When enumerating some objects, the MCU returns the results page by page. To protect the connector from infinite
     * loop when the device gives an incorrect results, there is a limit on the number of pages the connector processes.
     * If this limit is reached, an exception is thrown (i.e., no part of the result may be used), as such a behaviour
     * is considered erroneous.
     */
    private static final int ENUMERATE_PAGES_LIMIT = 1000;

    /**
     * The default port number to connect to.
     */
    public static final int DEFAULT_PORT = 443;

    /**
     * {@link XmlRpcClient} used for the communication with the device.
     */
    private XmlRpcClient client;

    /**
     * Authentication for the device.
     */
    private String authUsername;
    private String authPassword;

    /**
     * Patterns for options.
     */
    private Pattern roomNumberFromH323Number = null;
    private Pattern roomNumberFromSIPURI = null;


    // COMMON SERVICE

    /**
     * Connects to the MCU.
     * <p/>
     * Sets up the device URL where to send requests.
     * The communication protocol is stateless, though, so it just gets some info and does not hold the line.
     *
     * @param address  device address to connect to
     * @param username username for authentication on the device
     * @param password password for authentication on the device
     * @throws CommandException
     */
    @Override
    public void connect(Address address, String username, String password) throws CommandException
    {
        if (address.getPort() == Address.DEFAULT_PORT) {
            address.setPort(DEFAULT_PORT);
        }

        info.setDeviceAddress(address);

        initOptions();

        try {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(getDeviceURL());
            // not standard basic auth - credentials are to be passed together with command parameters
            authUsername = username;
            authPassword = password;

            client = new XmlRpcClient();
            client.setConfig(config);
            client.setTransportFactory(new KeepAliveTransportFactory(client));

            // FIXME: remove, the production code should not trust any certificate
            HostTrustManager.addTrustedHost(getDeviceURL().getHost());

            initDeviceInfo();
        }
        catch (MalformedURLException e) {
            throw new CommandException("Error constructing URL of the device.", e);
        }
        catch (CommandException e) {
            throw new CommandException("Error setting up connection to the device.", e);
        }

        info.setConnectionState(ConnectorInfo.ConnectionState.LOOSELY_CONNECTED);

    }

    private void initDeviceInfo() throws CommandException
    {
        Map<String, Object> device = exec(new Command("device.query"));

        try {
            Double apiVersion = Double.valueOf((String) device.get("apiVersion"));
            if (apiVersion < 2.9) {
                throw new CommandException(String.format(
                        "Device API %.1f too old. The connector only works with API 2.9 or higher.",
                        apiVersion
                ));
            }
        }
        catch (NullPointerException e) {
            throw new CommandException("Cannot determine the device API version.", e);
        }
        catch (NumberFormatException e) {
            throw new CommandException("Cannot determine the device API version.", e);
        }


        DeviceInfo di = new DeviceInfo();

        di.setName((String) device.get("model"));

        String version = device.get("softwareVersion")
                + " (API: " + device.get("apiVersion")
                + ", build: " + device.get("buildVersion")
                + ")";
        di.setSoftwareVersion(version);

        di.setSerialNumber((String) device.get("serial"));

        info.setDeviceInfo(di);
    }

    private void initOptions()
    {
        roomNumberFromH323Number = null;
        roomNumberFromSIPURI = null;

        if (options == null) {
            return;
        }

        String h323Number = options.getString(ROOM_NUMBER_EXTRACTION_FROM_H323_NUMBER);
        if (h323Number != null) {
            roomNumberFromH323Number = Pattern.compile(h323Number);
        }
        String sipNumber = options.getString(ROOM_NUMBER_EXTRACTION_FROM_SIP_URI);
        if (sipNumber != null) {
            roomNumberFromSIPURI = Pattern.compile(sipNumber);
        }
    }

    @Override
    public void disconnect() throws CommandException
    {
        // TODO: consider publishing feedback events from the MCU
        // no real operation - the communication protocol is stateless
        info.setConnectionState(ConnectorInfo.ConnectionState.DISCONNECTED);
        client = null; // just for sure the attributes are not used anymore
    }


    /**
     * Returns the URL on which to communicate with the device.
     *
     * @return URL for communication with the device
     */
    private URL getDeviceURL() throws MalformedURLException
    {
        // RPC2 is a fixed path given by Cisco, see the API docs
        return new URL("https", info.getDeviceAddress().getHost(), info.getDeviceAddress().getPort(), "/RPC2");
    }

    /**
     * Sends a command to the device. Blocks until response to the command is complete.
     *
     * @param command a command to the device; note that some parameters may be added to the command
     * @return output of the command
     */
    private Map<String, Object> exec(Command command) throws CommandException
    {
        command.unsetParameter("authenticationPassword");
        logger.debug(String.format("%s issuing command '%s' on %s",
                CiscoMCUConnector.class, command, info.getDeviceAddress()));

        command.setParameter("authenticationUser", authUsername);
        command.setParameter("authenticationPassword", authPassword);
        Object[] params = new Object[]{command.getParameters()};
        try {
            return (Map<String, Object>) client.execute(command.getCommand(), params);
        }
        catch (XmlRpcException e) {
            throw new CommandException(e.getMessage());
        }
    }

    /**
     * Executes a command enumerating some objects.
     * <p/>
     * When possible (currently for commands conference.enumerate and participant.enumerate), caches the results and
     * asks just for the difference since previous call of the same command.
     * The caching is intentionally disabled for the autoAttendants.enumerate command, as the revisioning mechanism
     * seems to be broken on the device (it reports dead items even with the listAll parameter set to true), and either
     * way it generates short lists.
     *
     * @param command   command for enumerating the objects; note that some parameters may be added to the command
     * @param enumField the field within result containing the list of enumerated objects
     * @return list of objects from the enumField, each as a map from field names to values;
     *         the list is unmodifiable (so that it may be reused by the execEnumerate() method)
     * @throws CommandException
     */
    private List<Map<String, Object>> execEnumerate(Command command, String enumField) throws CommandException
    {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

        // use revision numbers to get just the difference from the previous call of this command
        Integer lastRevision = prepareCaching(command);
        Integer currentRevision = null;

        for (int enumPage = 0; ; enumPage++) {
            // safety pages number check - to prevent infinite loop if the device does not work correctly
            if (enumPage >= ENUMERATE_PAGES_LIMIT) {
                String message = String
                        .format("Enumerate pages safety limit reached - the device gave more than %d result pages!",
                                ENUMERATE_PAGES_LIMIT);
                throw new CommandException(message);
            }

            // ask for data
            Map<String, Object> result = exec(command);
            // get the revision number of the first page - for using cache
            if (enumPage == 0) {
                currentRevision = (Integer) result.get("currentRevision"); // might not exist in the result and be null
            }

            // process data
            if (!result.containsKey(enumField)) {
                break; // no data at all
            }
            Object[] data = (Object[]) result.get(enumField);
            for (Object obj : data) {
                results.add((Map<String, Object>) obj);
            }

            // ask for more results, or break if that was all
            if (result.containsKey("enumerateID")) {
                command.setParameter("enumerateID", result.get("enumerateID"));
            }
            else {
                break; // that's all, folks
            }
        }

        if (currentRevision != null) {
            populateResultsFromCache(results, currentRevision, lastRevision, command, enumField);
        }

        return Collections.unmodifiableList(results);
    }

    private static RoomSummary extractRoomSummary(Map<String, Object> conference)
    {
        RoomSummary roomSummary = new RoomSummary();
        roomSummary.setId((String) conference.get("conferenceName"));
        roomSummary.setName((String) conference.get("conferenceName"));
        roomSummary.setDescription((String) conference.get("description"));
        String timeField = (conference.containsKey("startTime") ? "startTime" : "activeStartTime");
        roomSummary.setStartDateTime(new DateTime(conference.get(timeField)));
        return roomSummary;
    }


    /**
     * For string parameters, MCU accepts only strings of limited length.
     * <p/>
     * There are just a few exceptions to the limit. For the rest, this method ensures truncation with logging strings
     * that are longer.
     * <p/>
     * Constant <code>STRING_MAX_LENGTH</code> is used as the limit.
     *
     * @param str string to be (potentially) truncated
     * @return <code>str</code> truncated to the maximum length supported by the device
     */
    private static String truncateString(String str)
    {
        if (str == null) {
            return "";
        }
        if (str.length() > STRING_MAX_LENGTH) {
            logger.warn(
                    "Too long string: '" + str + "', the device only supports " + STRING_MAX_LENGTH + "-character strings");
            str = str.substring(0, STRING_MAX_LENGTH);
        }
        return str;
    }

    //<editor-fold desc="RESULTS CACHING">

    /**
     * Prepares caching of result of the supplied command.
     *
     * @param command command to be issued; may be modified (some parameters regarding caching may be added)
     * @return the last revision when the same command was issued
     */
    private Integer prepareCaching(Command command)
    {
        Integer lastRevision = getCachedRevision(command);
        if (lastRevision != null) {
            command.setParameter("lastRevision", lastRevision);
            command.setParameter("listAll", Boolean.TRUE);
        }
        return lastRevision;
    }

    /**
     * Populates the results list - puts the original objects instead of item stubs.
     * <p/>
     * If there was a previous call to the same command, the changed items are just stubs in the new result set. To use
     * the results, this method populates all the stubs and puts the objects from the previous call in their place.
     *
     * @param results         list of results, some of which may be stubs; gets modified so that it contains no stubs
     * @param currentRevision the revision of this results
     * @param lastRevision    the revision of the previous call of the same command
     * @param command         the command called to get the supplied results
     * @param enumField       the field name from which the supplied results where taken within the command result
     */
    private void populateResultsFromCache(List<Map<String, Object>> results, Integer currentRevision,
            Integer lastRevision, Command command, String enumField)
            throws CommandException
    {
        // we got just the difference since lastRevision (or full set if this is the first issue of the command)
        final String cacheId = getCommandCacheId(command);

        if (lastRevision != null) {
            // fill the values that have not changed since lastRevision
            ListIterator<Map<String, Object>> iterator = results.listIterator();
            while (iterator.hasNext()) {
                Map<String, Object> item = iterator.next();

                if (isItemDead(item)) {
                    // from the MCU API: "The device will also never return a dead record if listAll is set to true."
                    // unfortunately, the buggy MCU still reports some items as dead even though listAll = true, so we
                    //   must remove them by ourselves (according to the API, a dead item should not have been ever
                    //   listed when listAll = true)
                    iterator.remove();
                }
                else if (!hasItemChanged(item)) {
                    ResultsCache cache = resultsCache.get(cacheId);
                    Map<String, Object> it = cache.getItem(item);
                    if (it == null) {
                        throw new CommandException(
                                "Item reported as not changed by the device, but was not found in the cache: " + item
                        );
                    }
                    iterator.set(it);
                }
            }
        }

        // store the results and the revision number for the next time
        ResultsCache rc = resultsCache.get(cacheId);
        if (rc == null) {
            rc = new ResultsCache();
            resultsCache.put(cacheId, rc);
        }
        rc.store(currentRevision, results);
    }

    /**
     * Tells whether an item from a result of an enumeration command has been removed since last time the command was
     * issued.
     *
     * @param item an item from the resulting list
     * @return <code>true</code> if the item is marked as removed, <code>false</code> if not
     */
    private static boolean isItemDead(Map<String, Object> item)
    {
        // try directly the item "dead" attribute
        if (Boolean.TRUE.equals(item.get("dead"))) {
            return true;
        }

        // for some enumeration items (namely "participants"), the "dead" attribute might? (who knows, it shouldn't
        //   have been there for any result, since listAll=true) be listed in "currentState"
        @SuppressWarnings("unchecked")
        Map<String, Object> currentState = (Map<String, Object>) item.get("currentState");
        if (currentState != null && Boolean.TRUE.equals(currentState.get("dead"))) {
            return true;
        }

        return false; // not reported as dead
    }


    /**
     * Tells whether an item from a result of an enumeration command changed since last time the command was issued.
     *
     * @param item an item from the resulting list
     * @return <code>false</code> if the item is marked as not changed,
     *         <code>true</code> if the item is not marked as not changed
     */
    private static boolean hasItemChanged(Map<String, Object> item)
    {
        // try directly the item "changed" attribute
        if (Boolean.FALSE.equals(item.get("changed"))) {
            return false;
        }

        // for some enumeration items (namely "participants"), the "changed" attribute might be listed in "currentState"
        @SuppressWarnings("unchecked")
        Map<String, Object> currentState = (Map<String, Object>) item.get("currentState");
        if (currentState != null && Boolean.FALSE.equals(currentState.get("changed"))) {
            return false;
        }

        return true; // not reported as not changed
    }

    /**
     * Cache storing results from a single command.
     * <p/>
     * Stores the revision number and the corresponding result set.
     * <p/>
     * The items stored in the cache are compared just according to their unique identifiers. They may differ in other
     * attributes. The reason for this is to provide simple searching for an item - the cache is given an item which has
     * just its unique ID, and should find the previously stored, full version of the item. Hence comparing just
     * according to the IDs.
     * <p/>
     * If the item contains a "participantName" key, the value under this key is used as the item unique ID.
     * If the item contains a "conferenceName" key, the value under this key is used as the item unique ID.
     * Otherwise, only items with equal contents are considered equal.
     */
    private class ResultsCache
    {
        private class Item
        {

            private final Map<String, Object> contents;

            public Item(Map<String, Object> contents)
            {
                if (contents == null) {
                    throw new NullPointerException("contents");
                }

                this.contents = contents;
            }

            public Map<String, Object> getContents()
            {
                return contents;
            }

            @Override
            public boolean equals(Object o)
            {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }

                Item item = (Item) o;

                final Object participantName = contents.get("participantName");
                if (participantName != null) {
                    return (participantName.equals(item.contents.get("participantName")));
                }

                final Object conferenceName = contents.get("conferenceName");
                if (conferenceName != null) {
                    return (conferenceName.equals(item.contents.get("conferenceName")));
                }

                return contents.equals(item.contents);
            }

            @Override
            public int hashCode()
            {
                final Object participantName = contents.get("participantName");
                if (participantName != null) {
                    return participantName.hashCode();
                }
                final Object conferenceName = contents.get("conferenceName");
                if (conferenceName != null) {
                    return conferenceName.hashCode();
                }
                return contents.hashCode();
            }

        }

        private int revision;

        private List<Item> results;

        public int getRevision()
        {
            return revision;
        }

        public Map<String, Object> getItem(Map<String, Object> item)
        {
            final Item it = new Item(item);
            // FIXME: optimize - should be O(1) rather than O(n)
            for (Item cachedItem : results) {
                if (cachedItem.equals(it)) {
                    return cachedItem.getContents();
                }
            }
            return null;
        }

        public void store(int revision, List<Map<String, Object>> results)
        {
            this.revision = revision;
            this.results = new ArrayList<Item>(results.size());
            for (Map<String, Object> res : results) {
                this.results.add(new Item(res));
            }
        }

    }

    /**
     * Cache of results of previous calls to commands supporting revision numbers.
     * Map of cache ID to previous results.
     */
    private Map<String, ResultsCache> resultsCache = new HashMap<String, ResultsCache>();

    /**
     * Returns the revision number of the previous call of the given command.
     * <p/>
     * The purpose of this method is to enable caching of previous calls and asking for just the difference since then.
     * <p/>
     * All the parameters of the command are considered, except enumerateID, lastRevision, and listAll.
     * <p/>
     * Note that the return value must be boxed, because the MCU API does not say anything about the revision numbers
     * issued by the device. So it may have any value, thus, we must recognize the special case by the null value.
     *
     * @param command a command which will be performed
     * @return revision number of the previous call of the given command,
     *         or null if the command has not been issued yet or does not support revision numbers
     */
    private Integer getCachedRevision(Command command)
    {
        if (command.getCommand().equals("autoAttendant.enumerate")) {
            return null; // disabled for the autoAttendant.enumerate command - it is broken on the device
        }
        String cacheId = getCommandCacheId(command);
        ResultsCache rc = resultsCache.get(cacheId);
        return (rc == null ? null : rc.getRevision());
    }

    private String getCommandCacheId(Command command)
    {
        final String[] ignoredParams = new String[]{
                "enumerateID", "lastRevision", "listAll", "authenticationUser", "authenticationPassword"
        };

        StringBuilder sb = new StringBuilder(command.getCommand());
ParamsLoop:
        for (Map.Entry<String, Object> entry : command.getParameters().entrySet()) {
            for (String ignoredParam : ignoredParams) {
                if (entry.getKey().equals(ignoredParam)) {
                    continue ParamsLoop; // the parameter is ignored
                }
            }
            sb.append(";");
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
        }

        return sb.toString();
    }


    //</editor-fold>

    //<editor-fold desc="ROOM SERVICE">

    @Override
    public Collection<RoomSummary> getRoomList() throws CommandException
    {
        Command cmd = new Command("conference.enumerate");
        cmd.setParameter("moreThanFour", Boolean.TRUE);
        cmd.setParameter("enumerateFilter", "!completed");

        Collection<RoomSummary> rooms = new ArrayList<RoomSummary>();
        List<Map<String, Object>> conferences = execEnumerate(cmd, "conferences");
        for (Map<String, Object> conference : conferences) {
            RoomSummary info = extractRoomSummary(conference);
            rooms.add(info);
        }

        return rooms;
    }

    @Override
    public Room getRoom(String roomId) throws CommandException
    {
        Command cmd = new Command("conference.status");
        cmd.setParameter("conferenceName", truncateString(roomId));
        Map<String, Object> result = exec(cmd);

        Room room = new Room();
        room.setId((String) result.get("conferenceName"));
        room.setName((String) result.get("conferenceName"));
        if (result.containsKey("maximumVideoPorts")) {
            room.setLicenseCount((Integer) result.get("maximumVideoPorts"));
        }
        room.addTechnology(Technology.H323);

        if (result.containsKey("description") && !result.get("description").equals("")) {
            room.setDescription((String) result.get("description"));
        }

        // aliases
        if (result.containsKey("numericId") && !result.get("numericId").equals("")) {
            Alias numAlias = new Alias(AliasType.H323_E164, (String) result.get("numericId"));
            room.addAlias(numAlias);
        }

        // options
        room.setOption(Room.Option.REGISTER_WITH_H323_GATEKEEPER, result.get("registerWithGatekeeper"));
        room.setOption(Room.Option.REGISTER_WITH_SIP_REGISTRAR, result.get("registerWithSIPRegistrar"));
        room.setOption(Room.Option.LISTED_PUBLICLY, !(Boolean) result.get("private"));
        room.setOption(Room.Option.ALLOW_CONTENT, result.get("contentContribution"));
        room.setOption(Room.Option.JOIN_AUDIO_MUTED, result.get("joinAudioMuted"));
        room.setOption(Room.Option.JOIN_VIDEO_MUTED, result.get("joinVideoMuted"));
        if (!result.get("pin").equals("")) {
            room.setOption(Room.Option.PIN, result.get("pin"));
        }
        room.setOption(Room.Option.START_LOCKED, result.get("startLocked"));
        room.setOption(Room.Option.CONFERENCE_ME_ENABLED, result.get("conferenceMeEnabled"));

        return room;
    }

    @Override
    public String createRoom(Room room) throws CommandException
    {
        try {
            room.setupNewEntity();
        }
        catch (FaultException exception) {
            throw new IllegalStateException(exception);
        }

        Command cmd = new Command("conference.create");

        cmd.setParameter("customLayoutEnabled", Boolean.TRUE);

        cmd.setParameter("enforceMaximumAudioPorts", Boolean.TRUE);
        cmd.setParameter("maximumAudioPorts", 0); // audio-only participants are forced to use video slots
        cmd.setParameter("enforceMaximumVideoPorts", Boolean.TRUE);

        // defaults (may be overridden by specified room options
        cmd.setParameter("registerWithGatekeeper", Boolean.FALSE);
        cmd.setParameter("registerWithSIPRegistrar", Boolean.FALSE);
        cmd.setParameter("private", Boolean.TRUE);
        cmd.setParameter("contentContribution", Boolean.TRUE);
        cmd.setParameter("contentTransmitResolutions", "allowAll");
        cmd.setParameter("joinAudioMuted", Boolean.FALSE);
        cmd.setParameter("joinVideoMuted", Boolean.FALSE);
        cmd.setParameter("startLocked", Boolean.FALSE);
        cmd.setParameter("conferenceMeEnabled", Boolean.FALSE);

        setConferenceParametersByRoom(cmd, room);

        // Room name must be filled
        if (cmd.getParameterValue("conferenceName") == null) {
            throw new IllegalStateException("Room name must be filled for the new room.");
        }

        exec(cmd);

        return (String) cmd.getParameterValue("conferenceName");
    }

    private void setConferenceParametersByRoom(Command cmd, Room room) throws CommandException
    {
        // Set the room forever
        cmd.setParameter("durationSeconds", 0);

        // Set the license count
        if (room.isPropertyFilled(Room.LICENSE_COUNT)) {
            cmd.setParameter("maximumVideoPorts", (room.getLicenseCount() > 0 ? room.getLicenseCount() : 0));
        }

        // Set the description
        if (room.isPropertyFilled(Room.DESCRIPTION)) {
            cmd.setParameter("description", truncateString(room.getDescription()));
        }

        // Create/Update aliases
        if (room.getAliases() != null) {
            for (Alias alias : room.getAliases()) {
                // Create new alias
                if (room.isPropertyItemMarkedAsNew(Room.ALIASES, alias)) {
                    // Derive number/name of the room
                    String roomNumber = null;
                    String roomName = null;
                    Matcher m;

                    switch (alias.getType()) {
                        case ROOM_NAME:
                            roomName = alias.getValue();
                            break;
                        case H323_E164:
                            if (roomNumberFromH323Number == null) {
                                throw new CommandException(String.format(
                                        "Cannot set H.323 E164 number - missing connector device option '%s'",
                                        ROOM_NUMBER_EXTRACTION_FROM_H323_NUMBER));
                            }
                            m = roomNumberFromH323Number.matcher(alias.getValue());
                            if (!m.find()) {
                                throw new CommandException("Invalid E164 number: " + alias.getValue());
                            }
                            roomNumber = m.group(1);
                            break;
                        case SIP_URI:
                            if (roomNumberFromSIPURI == null) {
                                throw new CommandException(String.format(
                                        "Cannot set SIP URI to room - missing connector device option '%s'",
                                        ROOM_NUMBER_EXTRACTION_FROM_SIP_URI));
                            }
                            m = roomNumberFromSIPURI.matcher(alias.getValue());
                            if (m.find()) {
                                // SIP URI contains number
                                roomNumber = m.group(1);
                            }
                            else {
                                // SIP URI contains name
                                String value = alias.getValue();
                                int atSign = value.indexOf('@');
                                assert atSign > 0;
                                roomName = value.substring(0, atSign);
                            }
                            break;
                        case H323_URI:
                            // TODO: Check the alias value
                            break;
                        default:
                            throw new CommandException("Unrecognized alias: " + alias.toString());
                    }

                    if (roomNumber != null) {
                        // Check we are not already assigning a different number to the room
                        final Object oldRoomNumber = cmd.getParameterValue("numericId");
                        if (oldRoomNumber != null && !oldRoomNumber.equals("") && !oldRoomNumber.equals(roomNumber)) {
                            // multiple number aliases
                            throw new CommandException(String.format(
                                    "The connector supports only one number for a room, requested another: %s", alias));
                        }
                        cmd.setParameter("numericId", truncateString(roomNumber));
                    }

                    if (roomName != null) {
                        // Check that more aliases do not request different room name
                        final Object oldRoomName = cmd.getParameterValue("conferenceName");
                        if (oldRoomName != null && !oldRoomName.equals("") && !oldRoomName.equals(roomName)) {
                            throw new CommandException(String.format(
                                    "The connector supports only one room name, requested another: %s", alias));
                        }
                        cmd.setParameter("conferenceName", truncateString(roomName));
                    }
                }
                // Modify existing alias
                else {
                    switch (alias.getType()) {
                        case H323_E164:
                            cmd.setParameter("numericId", truncateString(alias.getValue()));
                            break;
                        default:
                            throw new IllegalStateException("TODO: Implement modification of "
                                    + alias.getType().toString() + " alias.");
                    }
                }
            }
        }
        // Delete aliases
        Set<Alias> aliasesToDelete = room.getPropertyItemsMarkedAsDeleted(Room.ALIASES);
        for (Alias alias : aliasesToDelete) {
            throw new IllegalStateException("TODO: Implement room alias deletion.");
        }

        // options
        // Create/Update options
        for (Room.Option option : room.getOptions().keySet()) {
            if (room.isPropertyItemMarkedAsNew(Room.OPTIONS, option)) {
                setRoomOption(cmd, option, room.getOption(option));
            }
            else {
                setRoomOption(cmd, option, room.getOption(option));
            }
        }
        // Delete options
        Set<Room.Option> optionsToDelete = room.getPropertyItemsMarkedAsDeleted(Room.OPTIONS);
        for (Room.Option option : optionsToDelete) {
            setRoomOption(cmd, option, null);
        }
    }

    private static void setRoomOption(Command cmd, Room.Option roomOption, Object value)
    {
        if (value instanceof String) {
            value = truncateString((String) value);
        }
        switch (roomOption) {
            case REGISTER_WITH_H323_GATEKEEPER:
                cmd.setParameter("registerWithGatekeeper", value);
                break;
            case REGISTER_WITH_SIP_REGISTRAR:
                cmd.setParameter("registerWithSIPRegistrar", value);
                break;
            case LISTED_PUBLICLY:
                cmd.setParameter("private", !(Boolean) value);
                break;
            case ALLOW_CONTENT:
                cmd.setParameter("contentContribution", value);
                break;
            case JOIN_AUDIO_MUTED:
                cmd.setParameter("joinAudioMuted", value);
                break;
            case JOIN_VIDEO_MUTED:
                cmd.setParameter("joinVideoMuted", value);
                break;
            case PIN:
                cmd.setParameter("pin", value);
                break;
            case START_LOCKED:
                cmd.setParameter("startLocked", value);
                break;
            case CONFERENCE_ME_ENABLED:
                cmd.setParameter("conferenceMeEnabled", value);
                break;
        }
    }

    @Override
    public String modifyRoom(Room room) throws CommandException
    {
        // build the command
        Command cmd = new Command("conference.modify");

        cmd.setParameter("conferenceName", truncateString(room.getId()));
        setConferenceParametersByRoom(cmd, room);

        exec(cmd);

        return room.getId();
    }

    @Override
    public void deleteRoom(String roomId) throws CommandException
    {
        Command cmd = new Command("conference.destroy");
        cmd.setParameter("conferenceName", truncateString(roomId));
        exec(cmd);
    }

    @Override
    public String exportRoomSettings(String roomId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public void importRoomSettings(String roomId, String settings) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    //</editor-fold>

    //<editor-fold desc="ROOM CONTENT SERVICE">

    @Override
    public void removeRoomContentFile(String roomId, String name) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public MediaData getRoomContent(String roomId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public void clearRoomContent(String roomId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public void addRoomContent(String roomId, String name, MediaData data)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    //</editor-fold>

    //<editor-fold desc="USER SERVICE">

    @Override
    public RoomUser getParticipant(String roomId, String roomUserId) throws CommandException
    {
        Command cmd = new Command("participant.status");
        identifyParticipant(cmd, roomId, roomUserId);
        cmd.setParameter("operationScope", new String[]{"currentState"});

        Map<String, Object> result = exec(cmd);

        return extractRoomUser(result);
    }

    @Override
    public String dialParticipant(String roomId, Alias alias) throws CommandException
    {
        return dialParticipant(roomId, alias.getValue());
    }

    @Override
    public String dialParticipant(String roomId, String address) throws CommandException
    {
        // FIXME: refine just as the createRoom() method - get just a RoomUser object and set parameters according to it

        // NOTE: adding participants as ad_hoc - the MCU autogenerates their IDs (but they are just IDs, not names),
        //       thus, commented out the following generation of participant names
//        String roomUserId = generateRoomUserId(roomId); // FIXME: treat potential race conditions; and it is slow...

        Command cmd = new Command("participant.add");
        cmd.setParameter("conferenceName", truncateString(roomId));
//        cmd.setParameter("participantName", truncateString(roomUserId));
        cmd.setParameter("address", truncateString(address));
        cmd.setParameter("participantType", "ad_hoc");
        cmd.setParameter("addResponse", Boolean.TRUE);

        Map<String, Object> result = exec(cmd);

        @SuppressWarnings("unchecked")
        Map<String, Object> participant = (Map<String, Object>) result.get("participant");
        if (participant == null) {
            return null;
        }
        else {
            return String.valueOf(participant.get("participantName"));
        }
    }

    /**
     * Generates a room user ID for a new user.
     *
     * @param roomId technology ID of the room to generate a new user ID for
     * @return a free roomUserId to be assigned (free in the moment of processing this method, might race condition with
     *         someone else)
     */
    private String generateRoomUserId(String roomId) throws CommandException
    {
        List<Map<String, Object>> participants;
        try {
            Command cmd = new Command("participant.enumerate");
            cmd.setParameter("operationScope", new String[]{"currentState"});
            participants = execEnumerate(cmd, "participants");
        }
        catch (CommandException e) {
            throw new CommandException("Cannot generate a new room user ID - cannot list current room users.", e);
        }

        // generate the new ID as maximal ID of present users increased by one
        int maxFound = 0;
        Pattern pattern = Pattern.compile("^participant(\\d+)$");
        for (Map<String, Object> part : participants) {
            if (!part.get("conferenceName").equals(roomId)) {
                continue;
            }
            Matcher m = pattern.matcher((String) part.get("participantName"));
            if (m.find()) {
                maxFound = Math.max(maxFound, Integer.parseInt(m.group(1)));
            }
        }

        return String.format("participant%d", maxFound + 1);
    }

    @Override
    public Collection<RoomUser> listParticipants(String roomId) throws CommandException
    {
        Command cmd = new Command("participant.enumerate");
        cmd.setParameter("operationScope", new String[]{"currentState"});
        cmd.setParameter("enumerateFilter", "connected");
        List<Map<String, Object>> participants = execEnumerate(cmd, "participants");

        List<RoomUser> result = new ArrayList<RoomUser>();
        for (Map<String, Object> part : participants) {
            if (!part.get("conferenceName").equals(roomId)) {
                continue; // not from this room
            }
            result.add(extractRoomUser(part));
        }

        return result;
    }

    /**
     * Extracts a room-user out of participant.enumerate or participant.status result.
     *
     * @param participant participant structure, as defined in the MCU API, command participant.status
     * @return room user extracted from the participant structure
     */
    private static RoomUser extractRoomUser(Map<String, Object> participant)
    {
        RoomUser ru = new RoomUser();

        ru.setUserId((String) participant.get("participantName"));
        ru.setRoomId((String) participant.get("conferenceName"));

        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) participant.get("currentState");

        ru.setDisplayName((String) state.get("displayName"));

        ru.setAudioMuted((Boolean) state.get("audioRxMuted"));
        ru.setVideoMuted((Boolean) state.get("videoRxMuted"));
        if (state.get("audioRxGainMode").equals("fixed")) {
            ru.setMicrophoneLevel((Integer) state.get("audioRxGainMillidB"));
        }
        ru.setJoinTime(new DateTime(state.get("connectTime")));

        // room layout
        if (state.containsKey("currentLayout")) {
            RoomLayout.VoiceSwitching vs;
            if (state.get("focusType").equals("voiceActivated")) {
                vs = RoomLayout.VoiceSwitching.VOICE_SWITCHED;
            }
            else {
                vs = RoomLayout.VoiceSwitching.NOT_VOICE_SWITCHED;
            }
            final Integer layoutIndex = (Integer) state.get("currentLayout");
            RoomLayout rl = RoomLayout.getByCiscoId(layoutIndex, RoomLayout.SPEAKER_CORNER, vs);

            ru.setLayout(rl);
        }
        return ru;
    }

    @Override
    public void modifyParticipant(String roomId, String roomUserId, Map<String, Object> attributes)
            throws CommandException
    {
        Command cmd = new Command("participant.modify");
        identifyParticipant(cmd, roomId, roomUserId);

        // NOTE: oh yes, Cisco MCU wants "activeState" for modify while for status, it gets "currentState"...
        cmd.setParameter("operationScope", "activeState");

        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
            String attName = attribute.getKey();
            Object attValue = attribute.getValue();

            if (attName.equals(RoomUser.DISPLAY_NAME)) {
                cmd.setParameter("displayNameOverrideValue", truncateString((String) attValue));
                cmd.setParameter("displayNameOverrideStatus", Boolean.TRUE); // for the value to take effect
            }
            else if (attName.equals(RoomUser.AUDIO_MUTED)) {
                cmd.setParameter("audioRxMuted", attValue);
            }
            else if (attName.equals(RoomUser.VIDEO_MUTED)) {
                cmd.setParameter("videoRxMuted", attValue);
            }
            else if (attName.equals(RoomUser.MICROPHONE_LEVEL)) {
                cmd.setParameter("audioRxGainMillidB", attValue);
                cmd.setParameter("audioRxGainMode", "fixed"); // for the value to take effect
            }
            else if (attName.equals(RoomUser.PLAYBACK_LEVEL)) {
                logger.info("Ignoring request to set PLAYBACK_LEVEL - Cisco MCU does not support it.");
            }
            else if (attName.equals(RoomUser.LAYOUT)) {
                RoomLayout layout = (RoomLayout) attValue;
                cmd.setParameter("focusType",
                        (layout.getVoiceSwitching() == RoomLayout.VoiceSwitching.VOICE_SWITCHED ? "voiceActivated" : "participant"));
                logger.info("Setting only voice-switching mode. The layout itself cannot be set by Cisco MCU.");
            }
            else {
                throw new IllegalArgumentException("Unknown RoomUser attribute: " + attName);
            }
        }

        exec(cmd);
    }

    @Override
    public void disconnectParticipant(String roomId, String roomUserId) throws CommandException
    {
        Command cmd = new Command("participant.remove");
        identifyParticipant(cmd, roomId, roomUserId);

        exec(cmd);
    }

    private void identifyParticipant(Command cmd, String roomId, String roomUserId)
    {
        cmd.setParameter("conferenceName", truncateString(roomId));
        cmd.setParameter("participantName", truncateString(roomUserId));
        // NOTE: it is necessary to identify a participant also by type; ad_hoc participants receive auto-generated
        //       numbers, so we distinguish the type by the fact whether the name is a number or not
        cmd.setParameter("participantType", (StringUtils.isNumeric(roomUserId) ? "ad_hoc" : "by_address"));
    }

    @Override
    public void enableContentProvider(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        // NOTE: it seems it is not possible to enable content using current API (2.9)
        throw new CommandUnsupportedException();
    }

    @Override
    public void disableContentProvider(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        // NOTE: it seems it is not possible to disable content using current API (2.9)
        throw new CommandUnsupportedException();
    }

    //</editor-fold>

    //<editor-fold desc="I/O SERVICE">

    @Override
    public void disableParticipantVideo(String roomId, String roomUserId) throws CommandException
    {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(RoomUser.VIDEO_MUTED, Boolean.TRUE);

        modifyParticipant(roomId, roomUserId, attributes);
    }

    @Override
    public void enableParticipantVideo(String roomId, String roomUserId) throws CommandException
    {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(RoomUser.VIDEO_MUTED, Boolean.FALSE);

        modifyParticipant(roomId, roomUserId, attributes);
    }

    @Override
    public void muteParticipant(String roomId, String roomUserId) throws CommandException
    {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(RoomUser.AUDIO_MUTED, Boolean.TRUE);

        modifyParticipant(roomId, roomUserId, attributes);
    }

    @Override
    public void setParticipantMicrophoneLevel(String roomId, String roomUserId, int level) throws CommandException
    {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(RoomUser.MICROPHONE_LEVEL, level);

        modifyParticipant(roomId, roomUserId, attributes);
    }

    @Override
    public void setParticipantPlaybackLevel(String roomId, String roomUserId, int level)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException();
    }

    @Override
    public void unmuteParticipant(String roomId, String roomUserId) throws CommandException
    {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(RoomUser.AUDIO_MUTED, Boolean.FALSE);

        modifyParticipant(roomId, roomUserId, attributes);
    }

    //</editor-fold>

    //<editor-fold desc="RECORDING SERVICE">

    @Override
    public void deleteRecording(int recordingId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public void downloadRecording(String downloadURL, String targetPath)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public String getRecordingDownloadURL(int recordingId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public Collection<String> notifyParticipants(int recordingId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public int startRecording(String roomId, ContentType format, RoomLayout layout)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public void stopRecording(int recordingId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    //</editor-fold>

    //<editor-fold desc="MONITORING SERVICE">

    @Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException
    {
        Map<String, Object> health = exec(new Command("device.health.query"));
        Map<String, Object> status = exec(new Command("device.query"));

        DeviceLoadInfo info = new DeviceLoadInfo();
        info.setCpuLoad(((Integer) health.get("cpuLoad")).doubleValue());
        if (status.containsKey("uptime")) {
            info.setUptime((Integer) status.get("uptime")); // NOTE: 'uptime' not documented, but it is there
        }

        // NOTE: memory and disk usage not accessible via API

        return info;
    }

    @Override
    public UsageStats getUsageStats() throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public MediaData getReceivedVideoSnapshot(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO: call participant.status and use previewURL
    }

    @Override
    public MediaData getSentVideoSnapshot(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    //</editor-fold>

    /**
     * An example of interaction with the device.
     * <p/>
     * Just for debugging purposes.
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException, CommandException, CommandUnsupportedException
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

        CiscoMCUConnector conn = new CiscoMCUConnector();
        conn.connect(Address.parseAddress(address), username, password);

        // gatekeeper status
//        Map<String, Object> gkInfo = conn.exec(new Command("gatekeeper.query"));
//        System.out.println("Gatekeeper status: " + gkInfo.get("gatekeeperUsage"));

        // test of getRoomList() command
//        Collection<RoomInfo> roomList = conn.getRoomList();
//        System.out.println("Existing rooms:");
//        for (RoomInfo room : roomList) {
//            System.out.printf("  - %s (%s, started at %s, owned by %s)\n", room.getCode(), room.getType(),
//                    room.getStartDateTime(), room.getOwner());
//        }

        // test that the second enumeration query fills data that has not changed and therefore were not transferred
//        Command enumParticipantsCmd = new Command("participant.enumerate");
//        enumParticipantsCmd.setParameter("operationScope", new String[]{"currentState"});
//        enumParticipantsCmd.setParameter("enumerateFilter", "connected");
//        List<Map<String, Object>> participants = conn.execEnumerate(enumParticipantsCmd, "participants");
//        List<Map<String, Object>> participants2 = conn.execEnumerate(enumParticipantsCmd, "participants");

        // test that the second enumeration query fills data that has not changed and therefore were not transferred
//        Command enumConfCmd = new Command("conference.enumerate");
//        enumConfCmd.setParameter("moreThanFour", Boolean.TRUE);
//        enumConfCmd.setParameter("enumerateFilter", "completed");
//        List<Map<String, Object>> confs = conn.execEnumerate(enumConfCmd, "conferences");
//        List<Map<String, Object>> confs2 = conn.execEnumerate(enumConfCmd, "conferences");

        // test of getRoom() command
//        Room shongoTestRoom = conn.getRoom("shongo-test");
//        System.out.println("shongo-test room:");
//        System.out.println(shongoTestRoom);

        // test of deleteRoom() command
//        Collection<RoomInfo> roomList = conn.getRoomList();
//        System.out.println("Existing rooms:");
//        for (RoomInfo room : roomList) {
//            System.out.println(room);
//        }
//        System.out.println("Deleting 'shongo-test'");
//        conn.deleteRoom("shongo-test");
//        roomList = conn.getRoomList();
//        System.out.println("Existing rooms:");
//        for (RoomInfo room : roomList) {
//            System.out.println(room);
//        }

        // test of createRoom() method
//        Room newRoom = new Room("shongo-test9", 5);
//        newRoom.addAlias(new Alias(Technology.H323, AliasType.E164, "950087209"));
//        newRoom.setOption(Room.OPT_DESCRIPTION, "Shongo testing room");
//        newRoom.setOption(Room.OPT_LISTED_PUBLICLY, true);
//        String newRoomId = conn.createRoom(newRoom);
//        System.out.println("Created room " + newRoomId);
//        Collection<RoomInfo> roomList = conn.getRoomList();
//        System.out.println("Existing rooms:");
//        for (RoomInfo room : roomList) {
//            System.out.println(room);
//        }

        // test of bad caching
//        Room newRoom = new Room("shongo-testX", 5);
//        String newRoomId = conn.createRoom(newRoom);
//        System.out.println("Created room " + newRoomId);
//        Collection<RoomSummary> roomList = conn.getRoomList();
//        System.out.println("Existing rooms:");
//        for (RoomSummary roomSummary : roomList) {
//            System.out.println(roomSummary);
//        }
//        conn.deleteRoom(newRoomId);
//        System.out.println("Deleted room " + newRoomId);
//        Map<String, Object> atts = new HashMap<String, Object>();
//        atts.put(Room.NAME, "shongo-testing");
//        String changedRoomId = conn.modifyRoom("shongo-test", atts, null);
//        Collection<RoomSummary> newRoomList = conn.getRoomList();
//        System.out.println("Existing rooms:");
//        for (RoomSummary roomSummary : newRoomList) {
//            System.out.println(roomSummary);
//        }
//        atts = new HashMap<String, Object>();
//        atts.put(Room.NAME, "shongo-test");
//        conn.modifyRoom(changedRoomId, atts, null);

        // test of modifyRoom() method
//        System.out.println("Modifying shongo-test");
//        Map<String, Object> atts = new HashMap<String, Object>();
//        atts.put(Room.NAME, "shongo-testing");
//        Map<Room.Option, Object> opts = new EnumMap<Room.Option, Object>(Room.Option.class);
//        opts.put(Room.Option.LISTED_PUBLICLY, false);
//        opts.put(Room.Option.PIN, "1234");
//        conn.modifyRoom("shongo-test", atts, opts);
//        Map<String, Object> atts2 = new HashMap<String, Object>();
//        atts2.put(Room.ALIASES, Collections.singletonList(new Alias(Technology.H323, AliasType.E164, "950087201")));
//        atts2.put(Room.NAME, "shongo-test");
//        conn.modifyRoom("shongo-testing", atts2, null);

        // test of listParticipants() method
//        System.out.println("Listing shongo-test room:");
//        Collection<RoomUser> shongoUsers = conn.listParticipants("shongo-test");
//        for (RoomUser ru : shongoUsers) {
//            System.out.println("  - " + ru.getUserId() + " (" + ru.getDisplayName() + ")");
//        }
//        System.out.println("Listing done");

        // user connect by alias
//        String ruId = conn.dialParticipant("shongo-test", new Alias(Technology.H323, AliasType.E164, "950081038"));
//        System.out.println("Added user " + ruId);
        // user connect by address
//        String ruId2 = conn.dialParticipant("shongo-test", "147.251.54.102");
        // user disconnect
//        conn.disconnectParticipant("shongo-test", "participant1");

//        System.out.println("All done, disconnecting");

        // test of modifyParticipant
//        Map<String, Object> attributes = new HashMap<String, Object>();
//        attributes.put(RoomUser.VIDEO_MUTED, Boolean.TRUE);
//        attributes.put(RoomUser.DISPLAY_NAME, "Ondrej Bouda");
//        conn.modifyParticipant("shongo-test", "3447", attributes);

        Room room = conn.getRoom("shongo-test");

        conn.disconnect();
    }

}
