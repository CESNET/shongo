package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.connector.api.*;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
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
 * FIXME: string parameters to device commands have to be at most 31 characters long
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CiscoMCUConnector extends AbstractConnector implements MultipointService
{
    private static Logger logger = LoggerFactory.getLogger(CiscoMCUConnector.class);

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
        Map<String, Object> gkInfo = conn.exec(new Command("gatekeeper.query"));
        System.out.println("Gatekeeper status: " + gkInfo.get("gatekeeperUsage"));

        // test of getRoomList() command
//        Collection<RoomInfo> roomList = conn.getRoomList();
//        System.out.println("Existing rooms:");
//        for (RoomInfo room : roomList) {
//            System.out.printf("  - %s (%s, started at %s, owned by %s)\n", room.getName(), room.getType(),
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

        // test of getRoomInfo() command
//        RoomInfo shongoTestRoom = conn.getRoomInfo("shongo-test");
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

        // test of modifyRoom() method
//        System.out.println("Modifying shongo-test");
//        Map<String, Object> atts = new HashMap<String, Object>();
//        atts.put(Room.NAME, "shongo-testing");
//        atts.put(Room.OPT_LISTED_PUBLICLY, false);
//        atts.put(Room.OPT_PIN, "1234");
//        conn.modifyRoom("shongo-test", atts);
//        Map<String, Object> atts2 = new HashMap<String, Object>();
//        atts2.put(Room.ALIASES, Collections.singletonList(new Alias(Technology.H323, AliasType.E164, "950087201")));
//        atts2.put(Room.NAME, "shongo-test");
//        conn.modifyRoom("shongo-testing", atts2);

        // test of listRoomUsers() method
        System.out.println("Listing shongo-test room:");
        Collection<RoomUser> shongoUsers = conn.listRoomUsers("shongo-test");
        for (RoomUser ru : shongoUsers) {
            System.out.println("  - " + ru.getUserId());
        }
        System.out.println("Listing done");

        // user connect by alias
//        String ruId = conn.dialParticipant("shongo-test", new Alias(Technology.H323, AliasType.E164, "950081038"));
//        System.out.println("Added user " + ruId);
        // user connect by address
//        String ruId2 = conn.dialParticipant("shongo-test", "147.251.54.102");
        // user disconnect
//        conn.disconnectRoomUser("shongo-test", "participant1");

//        System.out.println("All done, disconnecting");
        conn.disconnect();
    }


    /**
     * The default port number to connect to.
     */
    public static final int DEFAULT_PORT = 443;

    private XmlRpcClient client;

    private String authUsername;
    private String authPassword;

    /**
     * H.323 gatekeeper registration prefix - prefix added to room numericIds to get the full number under which the
     * room is callable.
     */
    private String gatekeeperRegistrationPrefix = null;


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

        try {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(getDeviceURL());
            // not standard basic auth - credentials are to be passed together with command parameters
            authUsername = username;
            authPassword = password;

            client = new XmlRpcClient();
            client.setConfig(config);

            // FIXME: remove, the production code should not trust any certificate
            try {
                setTrustAllCertificates();
            }
            catch (NoSuchAlgorithmException e) {
                logger.error("Error setting trust to all certificates", e);
            }
            catch (KeyManagementException e) {
                logger.error("Error setting trust to all certificates", e);
            }

            initSession();
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

    private void initSession() throws CommandException
    {
        Command gkInfoCmd = new Command("gatekeeper.query");
        Map<String, Object> gkInfo = exec(gkInfoCmd);
        if (!gkInfo.get("gatekeeperUsage").equals("disabled")) {
            gatekeeperRegistrationPrefix = (String) gkInfo.get("registrationPrefix");
        }
    }

    private void initDeviceInfo() throws CommandException
    {
        Map<String, Object> device = exec(new Command("device.query"));
        DeviceInfo di = new DeviceInfo();

        di.setName((String) device.get("model"));

        String version = (String) device.get("softwareVersion")
                + " (API: " + (String) device.get("apiVersion")
                + ", build: " + (String) device.get("buildVersion")
                + ")";
        di.setSoftwareVersion(version);

        di.setSerialNumber((String) device.get("serial"));

        info.setDeviceInfo(di);
    }

    /**
     * Configures the client to trust any certificate, without the need to have it in the keystore.
     * <p/>
     * Just a quick and dirty solution for certificate issues. The production solution should not use this method!
     * <p/>
     * Taken from http://ws.apache.org/xmlrpc/ssl.html
     */
    private void setTrustAllCertificates() throws NoSuchAlgorithmException, KeyManagementException
    {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager()
                {
                    public X509Certificate[] getAcceptedIssuers()
                    {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType)
                    {
                        // Trust always
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType)
                    {
                        // Trust always
                    }
                }
        };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        // Create empty HostnameVerifier
        HostnameVerifier hv = new HostnameVerifier()
        {
            public boolean verify(String arg0, SSLSession arg1)
            {
                return true;
            }
        };

        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
    }

    @Override
    public void disconnect() throws CommandException
    {
        // TODO: consider publishing feedback events from the MCU
        // no real operation - the communication protocol is stateless
        info.setConnectionState(ConnectorInfo.ConnectionState.DISCONNECTED);
        client = null; // just for sure the attributes are not used anymore
        gatekeeperRegistrationPrefix = null;
    }


    /**
     * Returns the URL on which to communicate with the device.
     *
     * @return URL for communication with the device
     */
    private URL getDeviceURL() throws MalformedURLException
    {
        // RPC2 is a fixed path given by Cisco, see the API docs
        return new URL("https", info.getDeviceAddress().getHost(), info.getDeviceAddress().getPort(), "RPC2");
    }

    /**
     * Sends a command to the device. Blocks until response to the command is complete.
     *
     * @param command a command to the device; note that some parameters may be added to the command
     * @return output of the command
     */
    private Map<String, Object> exec(Command command) throws CommandException
    {
        command.setParameter("authenticationUser", authUsername);
        command.setParameter("authenticationPassword", authPassword);
        Object[] params = new Object[]{command.getParameters()};
        try {
            return (Map<String, Object>) client.execute(command.getCommand(), params);
        }
        catch (XmlRpcException e) {
            throw new CommandException("Error calling the method via XML-RPC", e);
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

    private static RoomSummary extractRoomInfo(Map<String, Object> conference)
    {
        RoomSummary info = new RoomSummary();
        info.setIdentifier((String) conference.get("conferenceName"));
        info.setName((String) conference.get("conferenceName"));
        info.setDescription((String) conference.get("description"));

        // TODO: get the conference owner

        String timeField = (conference.containsKey("startTime") ? "startTime" : "activeStartTime");
        info.setStartDateTime(new DateTime(conference.get(timeField)));
        return info;
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
            Integer lastRevision,
            Command command, String enumField)
    {
        // we got just the difference since lastRevision (or full set if this is the first issue of the command)
        final String cacheId = getCommandCacheId(command);

        if (lastRevision != null) {
            // fill the values that have not changed since lastRevision
            ListIterator<Map<String, Object>> iterator = results.listIterator();
            while (iterator.hasNext()) {
                Map<String, Object> item = iterator.next();
                if (!itemChanged(item, enumField)) {
                    ResultsCache cache = resultsCache.get(cacheId);
                    iterator.set(cache.getItem(item));
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

    private boolean itemChanged(Map<String, Object> item, String enumField)
    {
        Map<String, Object> changedStruct = null;

        if (enumField.equals("conferences")) {
            changedStruct = item;
        }
        else if (enumField.equals("participants")) {
            changedStruct = (Map<String, Object>) item.get("currentState");
        }

        if (changedStruct == null) {
            return true;
        }
        else {
            return !(changedStruct.containsKey("changed") && changedStruct.get("changed").equals(Boolean.FALSE));
        }
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
            this.results = new ArrayList<Item>();
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
    public RoomSummary getRoomInfo(String roomId) throws CommandException
    {
        Command cmd = new Command("conference.status");
        cmd.setParameter("conferenceName", roomId);
        Map<String, Object> result = exec(cmd);
        return extractRoomInfo(result);
    }

    @Override
    public String createRoom(Room room) throws CommandException
    {
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

        exec(cmd);

        return room.getName();
    }

    private void setConferenceParametersByRoom(Command cmd, Room room) throws CommandException
    {
        if (room.getName() != null) {
            cmd.setParameter("conferenceName", room.getName());
        }

        if (room.getPortCount() >= 0) {
            cmd.setParameter("maximumVideoPorts", room.getPortCount());
        }

        if (room.getAliases() != null) {
            cmd.setParameter("numericId", "");
            for (Alias alias : room.getAliases()) {
                if (alias.getTechnology() == Technology.H323 && alias.getType() == AliasType.E164) {
                    if (!cmd.getParameterValue("numericId").equals("")) {
                        // multiple number aliases
                        final String m = "The connector supports only one numeric H.323 alias, requested another: " + alias;
                        throw new CommandException(m);
                    }
                    // number of the room
                    String number = alias.getValue();
                    if (gatekeeperRegistrationPrefix != null) {
                        if (!number.startsWith(gatekeeperRegistrationPrefix)) {
                            throw new CommandException(
                                    String.format("Assigned numbers should be prefixed with %s, number %s given.",
                                            gatekeeperRegistrationPrefix, number));
                        }
                        number = number.substring(gatekeeperRegistrationPrefix.length());
                    }
                    cmd.setParameter("numericId", number);
                }
                else {
                    throw new CommandException("Unrecognized alias: " + alias);
                }
            }
        }

        if (room.getStartTime() != null) {
            cmd.setParameter("startTime", room.getStartTime());
        }
        if (room.getEndTime() != null) {
            final long milliDiff;
            if (room.getStartTime() != null) {
                milliDiff = room.getEndTime().getTime() - room.getStartTime().getTime();
            }
            else {
                milliDiff = 0; // FIXME: get current room start time
            }
            cmd.setParameter("durationSeconds", milliDiff / 1000);
        }
        else {
            cmd.setParameter("durationSeconds", 0);
        }

        // options
        setCommandOption(cmd, room, "registerWithGatekeeper", Room.OPT_REGISTER_WITH_H323_GATEKEEPER);
        setCommandOption(cmd, room, "registerWithSIPRegistrar", Room.OPT_REGISTER_WITH_SIP_REGISTRAR);
        if (room.hasOption(Room.OPT_LISTED_PUBLICLY)) {
            cmd.setParameter("private", !(Boolean) room.getOption(Room.OPT_LISTED_PUBLICLY));
        }
        setCommandOption(cmd, room, "contentContribution", Room.OPT_ALLOW_CONTENT);
        setCommandOption(cmd, room, "joinAudioMuted", Room.OPT_JOIN_AUDIO_MUTED);
        setCommandOption(cmd, room, "joinVideoMuted", Room.OPT_JOIN_VIDEO_MUTED);
        setCommandOption(cmd, room, "pin", Room.OPT_PIN);
        setCommandOption(cmd, room, "description", Room.OPT_DESCRIPTION);
        setCommandOption(cmd, room, "startLocked", Room.OPT_START_LOCKED);
        setCommandOption(cmd, room, "conferenceMeEnabled", Room.OPT_CONFERENCE_ME_ENABLED);
    }

    private static void setCommandOption(Command cmd, Room room, String cmdParam, String roomOption)
    {
        if (room.hasOption(roomOption)) {
            cmd.setParameter(cmdParam, room.getOption(roomOption));
        }
    }

    @Override
    public String modifyRoom(String roomId, Map<String, Object> attributes) throws CommandException
    {
        // based on attributes, construct a Room instance, according to which we build the command
        Room room = new Room();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String att = entry.getKey();
            Object val = entry.getValue();
            if (att.equals(Room.NAME)) {
                room.setName((String) val);
            }
            else if (att.equals(Room.PORT_COUNT)) {
                room.setPortCount((Integer) val);
            }
            else if (att.equals(Room.ALIASES)) {
                room.setAliases((List<Alias>) val);
            }
            else if (att.equals(Room.START_TIME)) {
                room.setStartTime((Date) val);
            }
            else if (att.equals(Room.END_TIME)) {
                room.setEndTime((Date) val);
            }
            else {
                room.setOption(att, val);
            }
        }

        // build the command
        Command cmd = new Command("conference.modify");
        setConferenceParametersByRoom(cmd, room);
        // treat the name and new name of the conference
        cmd.setParameter("conferenceName", roomId);
        if (room.getName() != null) {
            cmd.setParameter("newConferenceName", room.getName());
        }

        exec(cmd);

        if (room.getName() != null) {
            // the room name changed - the room ID must change, too
            return room.getName();
        }
        else {
            return roomId;
        }
    }

    @Override
    public void deleteRoom(String roomId) throws CommandException
    {
        Command cmd = new Command("conference.destroy");
        cmd.setParameter("conferenceName", roomId);
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
    public RoomUser getRoomUser(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
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

        // FIXME: slow...
        String roomUserId = generateRoomUserId(roomId); // FIXME: treat potential race conditions

        Command cmd = new Command("participant.add");
        cmd.setParameter("conferenceName", roomId);
        cmd.setParameter("participantName", roomUserId);
        cmd.setParameter("address", address);
        cmd.setParameter("participantType", "by_address");

        exec(cmd);

        return roomUserId;
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
    public Collection<RoomUser> listRoomUsers(String roomId) throws CommandException
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
            RoomUser ru = new RoomUser();

            ru.setUserId((String) part.get("participantName"));
            ru.setRoomId((String) part.get("conferenceName"));

            Map<String, Object> state = (Map<String, Object>) part.get("currentState");

            ru.setMuted((Boolean) state.get("audioRxMuted"));
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

            result.add(ru);
        }

        return result;
    }

    @Override
    public void modifyRoomUser(String roomId, String roomUserId, Map attributes)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public void disconnectRoomUser(String roomId, String roomUserId) throws CommandException
    {
        Command cmd = new Command("participant.remove");
        cmd.setParameter("conferenceName", roomId);
        cmd.setParameter("participantName", roomUserId);

        exec(cmd);
    }

    @Override
    public void enableContentProvider(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public void disableContentProvider(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    //</editor-fold>

    //<editor-fold desc="I/O SERVICE">

    @Override
    public void disableUserVideo(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public void enableUserVideo(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public void muteUser(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public void setUserMicrophoneLevel(String roomId, String roomUserId, int level)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public void setUserPlaybackLevel(String roomId, String roomUserId, int level)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
    }

    @Override
    public void unmuteUser(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(); // TODO
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
        info.setCpuLoad((Integer) health.get("cpuLoad"));
        if (status.containsKey("uptime")) {
            info.setUpTime((Long) status.get("uptime")); // NOTE: 'uptime' not documented, but it is there
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
    public Collection<RoomSummary> getRoomList() throws CommandException
    {
        Command cmd = new Command("conference.enumerate");
        cmd.setParameter("moreThanFour", Boolean.TRUE);
        cmd.setParameter("enumerateFilter", "!completed");

        Collection<RoomSummary> rooms = new ArrayList<RoomSummary>();
        List<Map<String, Object>> conferences = execEnumerate(cmd, "conferences");
        for (Map<String, Object> conference : conferences) {
            RoomSummary info = extractRoomInfo(conference);
            rooms.add(info);
        }

        return rooms;
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

}
