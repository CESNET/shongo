package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.connector.api.*;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
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

        Collection<RoomInfo> roomList = conn.getRoomList();
        System.out.println("Existing rooms:");
        for (RoomInfo room : roomList) {
            System.out.printf("  - %s (%s, started at %s, owned by %s)\n", room.getName(), room.getType(),
                    room.getStartTime(), room.getOwner());
        }

//        Command enumParticipantsCmd = new Command("participant.enumerate");
//        enumParticipantsCmd.setParameter("operationScope", new String[]{"currentState"});
//        enumParticipantsCmd.setParameter("enumerateFilter", "connected");
//        List<Map<String, Object>> participants = conn.execEnumerate(enumParticipantsCmd, "participants");
//        List<Map<String, Object>> participants2 = conn.execEnumerate(enumParticipantsCmd, "participants");

//        Command enumConfCmd = new Command("conference.enumerate");
//        enumConfCmd.setParameter("moreThanFour", Boolean.TRUE);
//        enumConfCmd.setParameter("enumerateFilter", "completed");
//        List<Map<String, Object>> confs = conn.execEnumerate(enumConfCmd, "conferences");
//        List<Map<String, Object>> confs2 = conn.execEnumerate(enumConfCmd, "conferences");

        System.out.println("All done, disconnecting");
        conn.disconnect();
    }


    /**
     * The default port number to connect to.
     */
    public static final int DEFAULT_PORT = 443;

    private XmlRpcClient client;

    private String authUsername;
    private String authPassword;


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
        }
        catch (MalformedURLException e) {
            throw new CommandException("Error constructing URL of the device.", e);
        }

        info.setConnectionState(ConnectorInfo.ConnectionState.LOOSELY_CONNECTED);

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

    @Override
    public void disconnect() throws CommandException
    {
        // TODO: consider publishing feedback events from the MCU
        // no real operation - the communication protocol is stateless
        info.setConnectionState(ConnectorInfo.ConnectionState.DISCONNECTED);
        client = null; // just for sure the client is not used anymore
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
            if (enumPage == 0 && result.containsKey("currentRevision")) {
                currentRevision = (Integer) result.get("currentRevision");
            }

            if (!result.containsKey(enumField)) {
                break; // no data at all
            }
            Object[] data = (Object[]) result.get(enumField);
            for (Object obj : data) {
                results.add((Map<String, Object>) obj);
            }

            // ask for more results, or break if there was all
            if (result.containsKey("enumerateID")) {
                command.setParameter("enumerateID", result.get("enumerateID"));
            }
            else {
                break; // that's all, folks
            }
        }

        populateResultsFromCache(results, currentRevision, lastRevision, command, enumField);

        return Collections.unmodifiableList(results);
    }


    //<editor-fold desc="RESULTS CACHING">

    /**
     * Prepares caching of result of the supplied command.
     * @param command    command to be issued; may be modified (some parameters regarding caching may be added)
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
     *
     * If there was a previous call to the same command, the changed items are just stubs in the new result set. To use
     * the results, this method populates all the stubs and puts the objects from the previous call in their place.
     *
     * @param results            list of results, some of which may be stubs; gets modified so that it contains no stubs
     * @param currentRevision    the revision of this results
     * @param lastRevision       the revision of the previous call of the same command
     * @param command            the command called to get the supplied results
     * @param enumField          the field name from which the supplied results where taken within the command result
     */
    private void populateResultsFromCache(List<Map<String, Object>> results, Integer currentRevision,
            Integer lastRevision,
            Command command, String enumField)
    {
        if (currentRevision != null) {
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

                return false;
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

        public Map<String, Object> getItem(Map<String, Object> item)
        {
            for (Item cachedItem : results) {
                if (cachedItem.equals(new Item(item))) {
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
        return (rc == null ? null : rc.revision);
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
    public RoomInfo getRoomInfo(String roomId) throws CommandException, CommandUnsupportedException
    {
        return null; // TODO
    }

    @Override
    public String createRoom(Room room) throws CommandException, CommandUnsupportedException
    {
        return null; // TODO
    }

    @Override
    public void modifyRoom(String roomId, Map attributes) throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public void deleteRoom(String roomId) throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public String exportRoomSettings(String roomId) throws CommandException, CommandUnsupportedException
    {
        return null; // TODO
    }

    @Override
    public void importRoomSettings(String roomId, String settings) throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    //</editor-fold>

    //<editor-fold desc="ROOM CONTENT SERVICE">

    @Override
    public void removeRoomContentFile(String roomId, String name) throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public MediaData getRoomContent(String roomId) throws CommandException, CommandUnsupportedException
    {
        return null; // TODO
    }

    @Override
    public void clearRoomContent(String roomId) throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public void addRoomContent(String roomId, String name, MediaData data)
            throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    //</editor-fold>

    //<editor-fold desc="USER SERVICE">

    @Override
    public void disableContentProvider(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public void disconnectRoomUser(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public void enableContentProvider(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public RoomUser getRoomUser(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException
    {
        return null; // TODO
    }

    @Override
    public Collection<RoomUser> listRoomUsers(String roomId) throws CommandException, CommandUnsupportedException
    {
        return null; // TODO
    }

    @Override
    public void modifyRoomUser(String roomId, String roomUserId, Map attributes)
            throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    //</editor-fold>

    //<editor-fold desc="I/O SERVICE">

    @Override
    public void disableUserVideo(String roomUserId) throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public void enableUserVideo(String roomUserId) throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public void muteUser(String roomUserId) throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public void setUserMicrophoneLevel(String roomUserId, int level)
            throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public void setUserPlaybackLevel(String roomUserId, int level) throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public void unmuteUser(String roomUserId) throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    //</editor-fold>

    //<editor-fold desc="RECORDING SERVICE">

    @Override
    public void deleteRecording(int recordingId) throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public void downloadRecording(String downloadURL, String targetPath)
            throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    @Override
    public String getRecordingDownloadURL(int recordingId) throws CommandException, CommandUnsupportedException
    {
        return null; // TODO
    }

    @Override
    public Collection<String> notifyParticipants(int recordingId) throws CommandException, CommandUnsupportedException
    {
        return null; // TODO
    }

    @Override
    public int startRecording(String roomId, ContentType format, RoomLayout layout)
            throws CommandException, CommandUnsupportedException
    {
        return 0; // TODO
    }

    @Override
    public void stopRecording(int recordingId) throws CommandException, CommandUnsupportedException
    {
        // TODO
    }

    //</editor-fold>

    //<editor-fold desc="MONITORING SERVICE">

    @Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException, CommandUnsupportedException
    {
        return null; // TODO
    }

    @Override
    public UsageStats getUsageStats() throws CommandException, CommandUnsupportedException
    {
        return null; // TODO
    }

    @Override
    public Collection<RoomInfo> getRoomList() throws CommandException, CommandUnsupportedException
    {
        Command cmd = new Command("conference.enumerate");
        cmd.setParameter("moreThanFour", Boolean.TRUE);
        cmd.setParameter("enumerateFilter", "!completed");

        Collection<RoomInfo> rooms = new ArrayList<RoomInfo>();
        List<Map<String, Object>> conferences = execEnumerate(cmd, "conferences");
        for (Map<String, Object> conference : conferences) {
            RoomInfo info = new RoomInfo();
            info.setName((String) conference.get("conferenceName"));
            info.setDescription((String) conference.get("description"));
            info.setType(Technology.H323); // FIXME: SIP as well?

            // get the conference owner
            // TODO: API says there should be "chairParticipant" field, but it is not...
//                info.setOwner((String) conference.get(""));

            String timeField = (conference.containsKey("startTime") ? "startTime" : "activeStartTime");
            info.setStartTime((Date) conference.get(timeField));

            rooms.add(info);
        }

        return rooms;
    }

    @Override
    public MediaData getReceivedVideoSnapshot(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        return null; // TODO
    }

    @Override
    public MediaData getSentVideoSnapshot(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        return null; // TODO
    }

    //</editor-fold>

}
