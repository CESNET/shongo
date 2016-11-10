package cz.cesnet.shongo.connector.device;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.Configuration;
import cz.cesnet.shongo.connector.api.UsageStats;
import cz.cesnet.shongo.connector.common.AbstractMultipointConnector;
import cz.cesnet.shongo.connector.common.Command;
import cz.cesnet.shongo.connector.support.KeepAliveTransportFactory;
import cz.cesnet.shongo.controller.api.jade.NotifyTarget;
import cz.cesnet.shongo.controller.api.jade.Service;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import cz.cesnet.shongo.util.MathHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketException;
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
public class CiscoMCUConnector extends AbstractMultipointConnector
{
    private static final Logger logger = LoggerFactory.getLogger(CiscoMCUConnector.class);

    private static final Pattern E164_PATTERN = Pattern.compile("^\\+?\\d{9,14}$");

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
     * Max gain of dB.
     */
    private static final int MAX_ABS_GAIN_DB = 20;

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
     * {@link XmlRpcClient} used for the XML-RPC API communication with the device.
     */
    private XmlRpcClient xmlRpcClient;

    /**
     * {@link XmlRpcClient} used for the Http communication with the device.
     */
    private HttpClient httpClient;

    /**
     * Detector for {@link MediaType}s.
     */
    private DefaultDetector detector = new DefaultDetector();

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

    /**
     * Addresses of participants which should be hidden.
     */
    private Set<String> hiddenParticipantAddresses = new HashSet<String>();

    //ClearSea connector - temporarily
    volatile LifeSizeUVCClearSea lifeSizeUVCClearSea;
    public static final String ALIAS_SERVICE_HOST = "alias-service.host";
    public static final String ALIAS_SERVICE_PORT = "alias-service.port";
    public static final String ALIAS_SERVICE_USERNAME = "alias-service.auth.username";
    public static final String ALIAS_SERVICE_PASSWORD = "alias-service.auth.password";
    public static final String ALIAS_SERVICE_GATEKEEPER = "alias-service.gatekeeper";

    /**
     * Cache of results of previous calls to commands supporting revision numbers.
     * Map of cache ID to previous results.
     */
    private final Map<String, ResultsCache> resultsCache = new HashMap<String, ResultsCache>();

    /**
     * Cache of snapshot URL for room participants ("roomId:roomParticipantId").
     */
    private final Map<String, String> roomParticipantSnapshotUrlCache = new HashMap<String, String>();

    /**
     * Cache of {@link MediaData} snapshots for room participants ("roomId:roomParticipantId").
     */
    private final ExpirationMap<String, MediaData> roomParticipantSnapshotCache =
            new ExpirationMap<String, MediaData>(Duration.standardSeconds(10));

    /**
     * @return URL for communication with the device via XML-RPC API
     */
    private URL getDeviceApiUrl() throws CommandException
    {
        // RPC2 is a fixed path given by Cisco, see the API docs
        String protocol = (deviceAddress.isSsl() ? "https" : "http");
        try {
            return new URL(protocol, deviceAddress.getHost(), deviceAddress.getPort(), "/RPC2");
        }
        catch (MalformedURLException exception) {
            throw new CommandException("Error constructing URL of the device.", exception);
        }
    }

    /**
     * @param file relative file
     * @return URL for communication with the device via Http
     */
    private URL getDeviceHttpUrl(String file) throws MalformedURLException
    {
        String protocol = (deviceAddress.isSsl() ? "https" : "http");
        return new URL(protocol, deviceAddress.getHost(), deviceAddress.getPort(), file);
    }

    // COMMON SERVICE

    /**
     * Connects to the MCU.
     * <p/>
     * Sets up the device URL where to send requests.
     * The communication protocol is stateless, though, so it just gets some info and does not hold the line.
     *
     * @param deviceAddress  device address to connect to
     * @param username username for authentication on the device
     * @param password password for authentication on the device
     * @throws cz.cesnet.shongo.api.jade.CommandException
     *
     */
    @Override
    public synchronized void connect(DeviceAddress deviceAddress, String username, String password) throws CommandException
    {
        if (deviceAddress.getPort() == DeviceAddress.DEFAULT_PORT) {
            deviceAddress.setPort(DEFAULT_PORT);
        }

        // Load options
        roomNumberFromH323Number = configuration.getOptionPattern(ROOM_NUMBER_EXTRACTION_FROM_H323_NUMBER);
        roomNumberFromSIPURI = configuration.getOptionPattern(ROOM_NUMBER_EXTRACTION_FROM_SIP_URI);

        hiddenParticipantAddresses.clear();
        for (Configuration participant : configuration.getOptionConfigurationList("participants.participant")) {
            boolean hide = participant.getBool("hide");
            if (hide) {
                String address = participant.getString("address");
                if (address == null || address.isEmpty()) {
                    throw new IllegalArgumentException("Address for participant must be filled.");
                }
                hiddenParticipantAddresses.add(address);
            }
        }

        try {
            // not standard basic auth - credentials are to be passed together with command parameters
            authUsername = username;
            authPassword = password;

            // Create XmlRpcClient for XML-RPC API communication
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(getDeviceApiUrl());
            config.setConnectionTimeout(requestTimeout);
            config.setReplyTimeout(requestTimeout);
            xmlRpcClient = new XmlRpcClient();
            xmlRpcClient.setConfig(config);
            xmlRpcClient.setTransportFactory(new KeepAliveTransportFactory(xmlRpcClient));

            // Create HttpClient for Http communication
            httpClient = ConfiguredSSLContext.getInstance().createHttpClient(requestTimeout);

            // Get and check device info
            Map<String, Object> device = execApi(new Command("device.query"));
            try {
                String apiVersionString = (String) device.get("apiVersion");
                String[] apiVersionParts = apiVersionString.split("\\.");
                Double apiVersion = Double.valueOf(apiVersionParts[0]) + (Double.valueOf(apiVersionParts[1]) / 1000.0);
                if (apiVersion < 2.009) {
                    throw new CommandException(String.format(
                            "Device API %.1f too old. The connector only works with API 2.9 or higher.", apiVersion));
                }
            }
            catch (Exception exception) {
                throw new CommandException("Cannot determine the device API version.", exception);
            }
            setDeviceName((String) device.get("model"));
            setDeviceSerialNumber((String) device.get("serial"));
            setDeviceSoftwareVersion(device.get("softwareVersion") + " ("
                    + "API: " + device.get("apiVersion") + ", "
                    + "build: " + device.get("buildVersion") + ")");
        }
        catch (CommandException exception) {
            throw new CommandException("Error setting up connection to the device.", exception);
        }

        // Alias service: ClearSea
        if (configuration.getOptionString(ALIAS_SERVICE_HOST) != null) {
            String csHost = configuration.getOptionStringRequired(ALIAS_SERVICE_HOST);
            int csPort = configuration.getOptionIntRequired(ALIAS_SERVICE_PORT);
            String csUsername = configuration.getOptionStringRequired(ALIAS_SERVICE_USERNAME);
            String csPassword = configuration.getOptionStringRequired(ALIAS_SERVICE_PASSWORD);
            String csGatekeeper = configuration.getOptionStringRequired(ALIAS_SERVICE_GATEKEEPER);
            DeviceAddress clearSeaAddress = new DeviceAddress(csHost, csPort);

            this.lifeSizeUVCClearSea = new LifeSizeUVCClearSea(csGatekeeper);
            this.lifeSizeUVCClearSea.connect(clearSeaAddress, csUsername, csPassword);
        }
    }

    @Override
    public ConnectionState getConnectionState()
    {
        try {
            synchronized (this) {
                XmlRpcClientConfigImpl configuration = ((XmlRpcClientConfigImpl) xmlRpcClient.getConfig());
                configuration.setReplyTimeout(CONNECTION_STATE_TIMEOUT);
                execApi("device.query", null);
                configuration.setReplyTimeout(requestTimeout);
            }
            return ConnectionState.CONNECTED;
        }
        catch (Exception exception) {
            logger.warn("Not connected", exception);
            return ConnectionState.DISCONNECTED;
        }
    }

    @Override
    public synchronized void disconnect() throws CommandException
    {
        // TODO: consider publishing feedback events from the MCU
        // no real operation - the communication protocol is stateless
        xmlRpcClient = null; // just for sure the attributes are not used anymore

        //Disconnect Alias service
        if (this.lifeSizeUVCClearSea != null) {
            this.lifeSizeUVCClearSea.disconnect();
            this.lifeSizeUVCClearSea = null;
        }
    }

    //</editor-fold>

    //<editor-fold desc="ROOM SERVICE">

    @Override
    public Collection<RoomSummary> listRooms() throws CommandException
    {
        Command cmd = new Command("conference.enumerate");
        cmd.setParameter("moreThanFour", Boolean.TRUE);
        cmd.setParameter("enumerateFilter", "!completed");

        Collection<RoomSummary> rooms = new ArrayList<RoomSummary>();
        List<Map<String, Object>> conferences = execApiEnumerate(cmd, "conferences");
        for (Map<String, Object> conference : conferences) {
            RoomSummary info = extractRoomSummary(conference);
            rooms.add(info);
        }

        return rooms;
    }

    @Override
    public Room getRoom(String roomId) throws CommandException
    {
        Map<String, Object> conferenceStatus;
        try {
            Command cmd = new Command("conference.status");
            cmd.setParameter("conferenceName", truncateString(roomId));
            conferenceStatus = execApi(cmd);
        }
        catch (CommandException exception) {
            if (exception.getMessage().equals("no such conference or auto attendant")) {
                return null;
            }
            else {
                throw exception;
            }
        }

        Room room = new Room();
        room.setId((String) conferenceStatus.get("conferenceName"));
        room.addAlias(AliasType.ROOM_NAME, (String) conferenceStatus.get("conferenceName"));
        if (conferenceStatus.containsKey("maximumVideoPorts")) {
            room.setLicenseCount((Integer) conferenceStatus.get("maximumVideoPorts"));
        }
        room.addTechnology(Technology.H323);

        if (conferenceStatus.containsKey("description") && !conferenceStatus.get("description").equals("")) {
            room.setDescription((String) conferenceStatus.get("description"));
        }

        // aliases
        if (conferenceStatus.containsKey("numericId") && !conferenceStatus.get("numericId").equals("")) {
            Alias numAlias = new Alias(AliasType.H323_E164, (String) conferenceStatus.get("numericId"));
            room.addAlias(numAlias);
        }

        // layout
        if (conferenceStatus.containsKey("customLayout")) {
            room.setLayout(getRoomLayoutByLayoutIndex((Integer) conferenceStatus.get("customLayout")));
        }

        // options
        H323RoomSetting h323RoomSetting = new H323RoomSetting();
        if (!conferenceStatus.get("pin").equals("")) {
            h323RoomSetting.setPin((String) conferenceStatus.get("pin"));
        }
        h323RoomSetting.setListedPublicly(!(Boolean) conferenceStatus.get("private"));
        h323RoomSetting.setAllowContent((Boolean) conferenceStatus.get("contentContribution"));
        h323RoomSetting.setJoinMicrophoneDisabled((Boolean) conferenceStatus.get("joinAudioMuted"));
        h323RoomSetting.setJoinVideoDisabled((Boolean) conferenceStatus.get("joinVideoMuted"));
        h323RoomSetting.setRegisterWithGatekeeper((Boolean) conferenceStatus.get("registerWithGatekeeper"));
        h323RoomSetting.setRegisterWithRegistrar((Boolean) conferenceStatus.get("registerWithSIPRegistrar"));
        h323RoomSetting.setStartLocked((Boolean) conferenceStatus.get("startLocked"));
        h323RoomSetting.setConferenceMeEnabled((Boolean) conferenceStatus.get("conferenceMeEnabled"));
        h323RoomSetting.setContentImportant((Boolean) conferenceStatus.get("contentImportant"));
        room.addRoomSetting(h323RoomSetting);

        return room;
    }

    @Override
    public String createRoom(final Room room) throws CommandException
    {
        Command cmd = new Command("conference.create");

        cmd.setParameter("customLayoutEnabled", Boolean.TRUE);
        cmd.setParameter("newParticipantsCustomLayout", Boolean.TRUE);

        cmd.setParameter("enforceMaximumAudioPorts", Boolean.TRUE);
        cmd.setParameter("maximumAudioPorts", 0); // audio-only participants are forced to use video slots
        cmd.setParameter("enforceMaximumVideoPorts", Boolean.TRUE);

        // defaults (may be overridden by specified room options
        cmd.setParameter("private", Boolean.FALSE);
        cmd.setParameter("contentContribution", Boolean.TRUE);
        cmd.setParameter("contentTransmitResolutions", "allowAll");
        cmd.setParameter("joinAudioMuted", Boolean.FALSE);
        cmd.setParameter("joinVideoMuted", Boolean.FALSE);
        cmd.setParameter("startLocked", Boolean.FALSE);
        cmd.setParameter("conferenceMeEnabled", Boolean.FALSE);

        // Set default layout to SPEAKER_CORNER
        if (room.getLayout() == null) {
            room.setLayout(RoomLayout.SPEAKER_CORNER);
        }
        setConferenceParametersByRoom(cmd, room);

        // Room name must be filled
        if (cmd.getParameterValue("conferenceName") == null) {
            throw new RuntimeException("Room name must be filled for the new room.");
        }

        final String roomName = (String) cmd.getParameterValue("conferenceName");

        try {
            execApi(cmd);
        }
        catch (CommandException ex) {
            // Check if the room is really not created
            try {
                if (getRoom(roomName) == null) {
                    throw ex;
                }
            }
            catch (Exception e) {
                throw ex;
            }
        }

        //TODO if CS alias
        if (this.lifeSizeUVCClearSea != null) {
            new Thread() {
                public void run() {
                    try {
                        // Allocate alias in ClearSea
                        Alias alias = room.getAlias(AliasType.H323_E164);
                        if (alias == null) {
                            throw new CommandException("H323_E164 must be set for ClearSea alias.");
                        }
                        lifeSizeUVCClearSea.createAlias(alias.getType(), alias.getValue(), roomName);
                    } catch (CommandException ex) {
                        logger.error("Failed to create ClearSea alias: " + ex.getMessage());

                        NotifyTarget notifyTarget = new NotifyTarget(Service.NotifyTargetType.RESOURCE_ADMINS);
                        notifyTarget.addMessage("en",
                                "Failed to create ClearSea alias pro místnost: " + room.getName(),
                                "Creation of ClearSea alias failed for room \"" + room.getName() + "\"." + "\n"
                                        + "Thrown exception:" + ex.getMessage());
                        notifyTarget.addMessage("cs",
                                "Selhalo vytvoření ClearSea aliasu pro místnost: " + room.getName(),
                                "Pro místnost \"" + room.getName() + "\" nebylo možné vytvořit alias pro ClearSea." + "\n"
                                        + "Nastala následující chyba:" + ex.getMessage());

                        try {
                            performControllerAction(notifyTarget);
                        } catch (CommandException e) {
                            logger.error("Failed to send notification: " + e);
                        }
                    }
                }
            }.start();
        }

        return roomName;
    }

    private void setConferenceParametersByRoom(Command cmd, Room room) throws CommandException
    {
        // Set the room forever
        cmd.setParameter("durationSeconds", 0);

        // Set the license count
        cmd.setParameter("maximumVideoPorts", (room.getLicenseCount() > 0 ? room.getLicenseCount() : 0));

        // Default layout
        RoomLayout roomLayout = room.getLayout();
        if (roomLayout != null) {
            Integer layoutIndex = getLayoutIndexByRoomLayout(roomLayout);
            if (layoutIndex != null) {
                cmd.setParameter("customLayout", layoutIndex);
            }
        }

        // Set the description
        if (room.getDescription() != null) {
            cmd.setParameter("description", truncateString(room.getDescription()));
        }

        // Create/Update aliases
        if (room.getAliases() != null) {
            for (Alias alias : room.getAliases()) {
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
                    case H323_IP:
                    case SIP_IP:
                    case CS_DIAL_STRING:
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
        }

        H323RoomSetting h323RoomSetting = room.getRoomSetting(H323RoomSetting.class);
        if (h323RoomSetting != null) {
            if (h323RoomSetting.getPin() != null) {
                cmd.setParameter("pin", h323RoomSetting.getPin());
            }
            if (h323RoomSetting.getListedPublicly() != null) {
                cmd.setParameter("private", !h323RoomSetting.getListedPublicly());
            }
            if (h323RoomSetting.getAllowContent() != null) {
                cmd.setParameter("contentContribution", h323RoomSetting.getAllowContent());
            }
            if (h323RoomSetting.getJoinMicrophoneDisabled() != null) {
                cmd.setParameter("joinAudioMuted", h323RoomSetting.getJoinMicrophoneDisabled());
            }
            if (h323RoomSetting.getJoinVideoDisabled() != null) {
                cmd.setParameter("joinVideoMuted", h323RoomSetting.getJoinVideoDisabled());
            }
            if (h323RoomSetting.getRegisterWithGatekeeper() != null) {
                cmd.setParameter("registerWithGatekeeper", h323RoomSetting.getRegisterWithGatekeeper());
            }
            if (h323RoomSetting.getRegisterWithRegistrar() != null) {
                cmd.setParameter("registerWithSIPRegistrar", h323RoomSetting.getRegisterWithRegistrar());
            }
            if (h323RoomSetting.getStartLocked() != null) {
                cmd.setParameter("startLocked", h323RoomSetting.getStartLocked());
            }
            if (h323RoomSetting.getConferenceMeEnabled() != null) {
                cmd.setParameter("conferenceMeEnabled", h323RoomSetting.getConferenceMeEnabled());
            }
            if (h323RoomSetting.getContentImportant() != null) {
                cmd.setParameter("contentImportant", h323RoomSetting.getContentImportant());
            }
            if (h323RoomSetting.getAllowGuests() != null) {
                throw new CommandException("Room Setting " + H323RoomSetting.ALLOW_GUESTS + "is not implemented yet.");
            }
        }
    }

    @Override
    protected boolean isRecreateNeeded(Room oldRoom, Room newRoom) throws CommandException
    {
        Alias oldRoomName = oldRoom.getAlias(AliasType.ROOM_NAME);
        Alias newRoomName = newRoom.getAlias(AliasType.ROOM_NAME);
        if (oldRoomName == null) {
            throw new CommandException("Room " + oldRoom.getId() + " doesn't have room name.");
        }
        if (newRoomName == null) {
            throw new CommandException("Room name must be present.");
        }
        return !newRoomName.equals(oldRoomName);
    }

    /**
     *
     * Disconnect participants in room to except number specified by {@code licenseCount}.
     *
     * @param licenseCount number of participants which can be kept in the room (0 means disconnect all participants,
     *                     1 all participants except one, etc)
     */
    private void disconnectRoomParticipants(String roomId, int licenseCount) throws CommandException
    {
        int participantCount = 0;
        for (RoomParticipant roomParticipant : getRoomParticipants(roomId, true)) {
            // Disconnect participant only when he excess specified license count
            if (participantCount < licenseCount) {
                logger.debug("Keeping participant {} connected to room {}...",
                        new Object[]{roomParticipant, roomId});
                participantCount++;
                continue;
            }
            try {
                logger.warn("Disconnecting participant {} in room {} to meet license count {}...",
                        new Object[]{roomParticipant, roomId, licenseCount});
                disconnectRoomParticipant(roomParticipant.getRoomId(), roomParticipant.getId());
            }
            catch (CommandException exception) {
                throw new CommandException("Cannot disconnect participant" + roomParticipant + ".", exception);
            }
        }
    }

    @Override
    protected void onModifyRoom(final Room room) throws CommandException
    {
        String roomId = room.getId();
        final Room oldRoom = getRoom(roomId);
        disconnectRoomParticipants(roomId, room.getLicenseCount());

        Command cmd = new Command("conference.modify");
        cmd.setParameter("conferenceName", truncateString(roomId));
        setConferenceParametersByRoom(cmd, room);
        if (this.lifeSizeUVCClearSea != null) {
            new Thread() {
                public void run() {
                    try {
                        Alias oldAlias = oldRoom.getAlias(AliasType.H323_E164);
                        Alias newAlias = room.getAlias(AliasType.H323_E164);

                        // Parse room number from new alias (old room contains only short version)
                        if (roomNumberFromH323Number == null) {
                            throw new CommandException(String.format(
                                    "Cannot set H.323 E164 number - missing connector device option '%s'",
                                    ROOM_NUMBER_EXTRACTION_FROM_H323_NUMBER));
                        }
                        Matcher matcher = roomNumberFromH323Number.matcher(newAlias.getValue());
                        if (!matcher.find()) {
                            throw new CommandException("Invalid E164 number: " + newAlias.getValue());
                        }
                        String roomNumber = matcher.group(1);

                        // Modify CS alias if changed
                        if (newAlias != null && !(room.getName().equals(oldRoom.getName()) && roomNumber.equals(oldAlias.getValue()))) {
                            lifeSizeUVCClearSea.modifyAlias(oldRoom.getName(), room.getName(), newAlias.getType(), oldAlias.getValue(), newAlias.getValue());
                        }
                    } catch (CommandException ex) {
                        logger.error("Failed to delete ClearSea alias: " + ex.getMessage());

                        NotifyTarget notifyTarget = new NotifyTarget(Service.NotifyTargetType.RESOURCE_ADMINS);
                        notifyTarget.addMessage("en",
                                "Failed to modify ClearSea alias pro místnost: " + room.getName(),
                                "Modify of ClearSea alias failed for room \"" + room.getName() + "\"." + "\n"
                                        + "Thrown exception:" + ex.getMessage());
                        notifyTarget.addMessage("cs",
                                "Selhala modifikace ClearSea aliasu pro místnost: " + room.getName(),
                                "Pro místnost \"" + room.getName() + "\" nebylo možné upravit alias pro ClearSea." + "\n"
                                        + "Nastala následující chyba:" + ex.getMessage());

                        try {
                            performControllerAction(notifyTarget);
                        } catch (CommandException e) {
                            logger.error("Failed to send notification: " + e);
                        }
                    }
                }
            }.start();
        }
        execApi(cmd);
    }

    @Override
    public void deleteRoom(String roomId) throws CommandException
    {
        final Room room = getRoom(roomId);
        if (this.lifeSizeUVCClearSea != null) {
            new Thread() {
                public void run() {
                    try {
                        lifeSizeUVCClearSea.deleteAlias(room.getName());
                    } catch (CommandException ex) {
                        logger.error("Failed to delete ClearSea alias: " + ex.getMessage());

                        NotifyTarget notifyTarget = new NotifyTarget(Service.NotifyTargetType.RESOURCE_ADMINS);
                        notifyTarget.addMessage("en",
                                "Failed to delete ClearSea alias pro místnost: " + room.getName(),
                                "Deleting of ClearSea alias failed for room \"" + room.getName() + "\"." + "\n"
                                        + "Thrown exception:" + ex.getMessage());
                        notifyTarget.addMessage("cs",
                                "Selhalo smaznání ClearSea aliasu pro místnost: " + room.getName(),
                                "Pro místnost \"" + room.getName() + "\" nebylo možné smazat alias pro ClearSea." + "\n"
                                        + "Nastala následující chyba:" + ex.getMessage());

                        try {
                            performControllerAction(notifyTarget);
                        } catch (CommandException e) {
                            logger.error("Failed to send notification: " + e);
                        }
                    }
                }
            }.start();
        }

        Command cmd = new Command("conference.destroy");
        cmd.setParameter("conferenceName", truncateString(roomId));
        execApi(cmd);
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
    public Collection<RoomParticipant> listRoomParticipants(String roomId) throws CommandException
    {
        return getRoomParticipants(roomId, false);
    }

    @Override
    public RoomParticipant getRoomParticipant(String roomId, String roomParticipantId) throws CommandException
    {
        Command cmd = new Command("participant.status");
        identifyParticipant(cmd, roomId, roomParticipantId);
        cmd.setParameter("operationScope", new String[]{"currentState"});

        Map<String, Object> result = execApi(cmd);

        return extractRoomParticipant(result);
    }

    @Override
    public Map<String, MediaData> getRoomParticipantSnapshots(String roomId, Set<String> roomParticipantIds)
            throws CommandException
    {
        Map<String, MediaData> participantSnapshots = new HashMap<String, MediaData>();
        for (String roomParticipantId : roomParticipantIds) {
            String cacheId = roomId + ":" + roomParticipantId;
            synchronized (roomParticipantSnapshotCache) {
                MediaData roomParticipantSnapshot = roomParticipantSnapshotCache.get(cacheId);
                if (roomParticipantSnapshot == null) {
                    roomParticipantSnapshot = getRoomParticipantSnapshot(roomId, roomParticipantId);
                    roomParticipantSnapshotCache.put(cacheId, roomParticipantSnapshot);
                }
                participantSnapshots.put(roomParticipantId, roomParticipantSnapshot);
            }
        }
        return participantSnapshots;
    }

    @Override
    public void modifyRoomParticipant(RoomParticipant roomParticipant)
            throws CommandException
    {
        String roomId = roomParticipant.getRoomId();
        if (roomId == null) {
            throw new IllegalArgumentException("RoomId must be not null.");
        }
        String roomParticipantId = roomParticipant.getId();
        if (roomParticipantId == null) {
            throw new IllegalArgumentException("RoomParticipantId must be not null.");
        }

        Command cmd = new Command("participant.modify");
        identifyParticipant(cmd, roomId, roomParticipantId);

        // NOTE: oh yes, Cisco MCU wants "activeState" for modify while for status, it gets "currentState"...
        cmd.setParameter("operationScope", "activeState");

        // @see extractRoomParticipant for more info we we don't return participant layout
        //if (roomParticipant.getLayout() != null) {
        //    Integer layoutIndex = getLayoutIndexByRoomLayout(roomParticipant.getLayout());
        //    if (layoutIndex != null) {
        //        cmd.setParameter("cpLayout", "layout" + layoutIndex);
        //    }
        //}

        // Set parameters
        if (roomParticipant.getDisplayName() != null) {
            cmd.setParameter("displayNameOverrideValue", truncateString(roomParticipant.getDisplayName()));
            cmd.setParameter("displayNameOverrideStatus", Boolean.TRUE); // for the value to take effect
        }
        if (roomParticipant.getMicrophoneEnabled() != null) {
            cmd.setParameter("audioRxMuted", !roomParticipant.getMicrophoneEnabled());
        }
        if (roomParticipant.getVideoEnabled() != null) {
            cmd.setParameter("videoRxMuted", !roomParticipant.getVideoEnabled());
        }
        Integer microphoneLevel = roomParticipant.getMicrophoneLevel();
        if (microphoneLevel != null && !microphoneLevel.equals(RoomParticipant.DEFAULT_MICROPHONE_LEVEL)) {
            double gainDb = MathHelper.getDbFromPercent(
                    ((double) roomParticipant.getMicrophoneLevel() - 5.0) / 5.0, MAX_ABS_GAIN_DB);
            cmd.setParameter("audioRxGainMillidB", (int)(gainDb * 1000.0));
            cmd.setParameter("audioRxGainMode", "fixed");
        }
        else {
            cmd.setParameter("audioRxGainMode", "default");
        }

        // Content
        // NOTE: it seems it is not possible to enable content using current API (2.9)
        //throw new CommandUnsupportedException();

        execApi(cmd);
    }

    @Override
    public void modifyRoomParticipants(RoomParticipant roomParticipantConfiguration)
            throws CommandException, CommandUnsupportedException
    {
        for (RoomParticipant roomParticipant : getRoomParticipants(roomParticipantConfiguration.getRoomId(), false)) {
            roomParticipantConfiguration.setId(roomParticipant.getId());
            if (!roomParticipantConfiguration.isSame(roomParticipant)) {
                modifyRoomParticipant(roomParticipantConfiguration);
            }
        }
    }

    @Override
    public String dialRoomParticipant(String roomId, Alias alias) throws CommandException
    {
        // FIXME: refine just as the createRoom() method - get just a RoomParticipant object and set parameters according to it

        // NOTE: adding participants as ad_hoc - the MCU autogenerates their IDs (but they are just IDs, not names),
        //       thus, commented out the following generation of participant names
        //String roomParticipantId = generateRoomParticipantId(roomId); // FIXME: treat potential race conditions; and it is slow...

        Command cmd = new Command("participant.add");
        cmd.setParameter("conferenceName", truncateString(roomId));
        //cmd.setParameter("participantName", truncateString(roomParticipantId));
        cmd.setParameter("address", truncateString(alias.getValue()));
        cmd.setParameter("participantType", "ad_hoc");
        cmd.setParameter("addResponse", Boolean.TRUE);

        Map<String, Object> result = execApi(cmd);

        @SuppressWarnings("unchecked")
        Map<String, Object> participant = (Map<String, Object>) result.get("participant");
        if (participant == null) {
            return null;
        }
        else {
            return String.valueOf(participant.get("participantName"));
        }
    }

    @Override
    public void disconnectRoomParticipant(String roomId, String roomParticipantId) throws CommandException
    {
        Command cmd = new Command("participant.remove");
        identifyParticipant(cmd, roomId, roomParticipantId);

        execApi(cmd);
    }

    //</editor-fold>

    //<editor-fold desc="MONITORING SERVICE">

    @Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException
    {
        Map<String, Object> health = execApi(new Command("device.health.query"));
        Map<String, Object> status = execApi(new Command("device.query"));

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

    //</editor-fold>

    /**
     * Perform http request for given {@code file}
     *
     * @param file
     * @return content as {@link MediaData}
     * @throws CommandException
     */
    private synchronized MediaData execHttp(String file) throws CommandException
    {
        try {
            URL requestUrl = getDeviceHttpUrl(file);
            HttpGet request = new HttpGet(requestUrl.toURI());
            HttpContext context = new BasicHttpContext();
            HttpResponse response = httpClient.execute(request, context);
            HttpRequest responseRequest = (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
            StatusLine responseStatusLine = response.getStatusLine();
            if (responseStatusLine.getStatusCode() == HttpStatus.SC_OK) {
                if (responseRequest.getRequestLine().getUri().startsWith("/login.html")) {
                    // Perform login
                    loginHttp();
                    // Perform the request again
                    response = httpClient.execute(request, context);
                }
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    byte[] mediaContent = EntityUtils.toByteArray(responseEntity);
                    MediaType mediaType = detector.detect(TikaInputStream.get(mediaContent), new Metadata());
                    return new MediaData(mediaType, mediaContent);
                }
            }
            throw new RuntimeException(response.getStatusLine().toString());
        }
        catch (CommandException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new CommandException("Http request " + file + " failed.", exception);
        }
    }

    /**
     * Try to login for {@link #httpClient}
     *
     * @throws CommandException when login fails
     */
    private void loginHttp() throws CommandException
    {
        try {
            HttpPost request = new HttpPost(getDeviceHttpUrl("/login_change.html").toURI());
            List<NameValuePair> parameters = new ArrayList<NameValuePair>(2);
            parameters.add(new BasicNameValuePair("user_name", authUsername));
            parameters.add(new BasicNameValuePair("password", authPassword));
            parameters.add(new BasicNameValuePair("ok", "OK"));
            request.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));
            HttpContext context = new BasicHttpContext();
            HttpResponse response = httpClient.execute(request, context);
            HttpRequest responseRequest = (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
            StatusLine responseStatusLine = response.getStatusLine();
            if (responseStatusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new RuntimeException("Wrong status " + responseStatusLine);
            }
            String responseRequestUrl = responseRequest.getRequestLine().getUri();
            if (responseRequestUrl.startsWith("/index.html") || responseRequestUrl.equals("/")) {
                logger.info("Http login successful.");
            }
            else {
                throw new RuntimeException("Wrong response url " + responseRequestUrl);
            }
        }
        catch (Exception exception) {
            throw new CommandException("Http login failed", exception);
        }
    }

    /**
     * Sends a command to the device. Blocks until response to the command is complete.
     *
     * @param command a command to the device; note that some parameters may be added to the command
     * @return output of the command
     */
    private Map<String, Object> execApi(Command command) throws CommandException
    {
        int retryCount = 5;
        while (retryCount > 0) {
            try {
                return execApi(command.getCommand(), command.getParameters());
            }
            catch (XmlRpcException exception) {
                if (isExecApiRetryPossible(exception)) {
                    retryCount--;
                    logger.warn("{}: Trying again...", exception.getMessage());
                    continue;
                }
                else {
                    throw new CommandException(exception.getMessage(), exception.getCause());
                }
            }
        }
        throw new CommandException(String.format("Command %s failed.", command));
    }

    /**
     * Sends a command to the device. Blocks until response to the command is complete.
     *
     * @param command
     * @param params
     * @return output of the command
     * @throws XmlRpcException
     */
    private synchronized Map<String, Object> execApi(String command, Map<String, Object> params) throws XmlRpcException
    {
        logger.debug(String.format("Issuing command '%s' on %s", command, deviceAddress));
        HashMap<String, Object> content = new HashMap<String, Object>();
        if (params != null) {
            content.putAll(params);
        }
        content.put("authenticationUser", authUsername);
        content.put("authenticationPassword", authPassword);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) xmlRpcClient.execute(command, new Object[]{content});
        return result;
    }

    /**
     * @param exception
     * @return true whether given {@code exception} allows to retry the API request, false otherwise
     */
    protected boolean isExecApiRetryPossible(XmlRpcException exception)
    {
        Throwable cause = exception.getCause();
        return (cause instanceof SocketException && cause.getMessage().equals("Connection reset"));
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
     *         the list is unmodifiable (so that it may be reused by the execApiEnumerate() method)
     * @throws CommandException
     */
    private synchronized List<Map<String, Object>> execApiEnumerate(Command command, String enumField)
            throws CommandException
    {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

        // use revision numbers to get just the difference from the previous call of this command
        Integer lastRevision = prepareCaching(command);
        Integer currentRevision = null;

        for (int enumPage = 0; ; enumPage++) {
            // safety pages number check - to prevent infinite loop if the device does not work correctly
            if (enumPage >= ENUMERATE_PAGES_LIMIT) {
                String message = String.format(
                        "Enumerate pages safety limit reached - the device gave more than %d result pages!",
                        ENUMERATE_PAGES_LIMIT);
                throw new CommandException(message);
            }

            // ask for data
            Map<String, Object> result = execApi(command);
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

    private static RoomSummary extractRoomSummary(Map<String, Object> conference)
    {
        RoomSummary roomSummary = new RoomSummary();
        roomSummary.setId((String) conference.get("conferenceName"));
        roomSummary.setName((String) conference.get("conferenceName"));
        roomSummary.setDescription((String) conference.get("description"));
        roomSummary.setAlias((String) conference.get("numericId"));
        String timeField = (conference.containsKey("startTime") ? "startTime" : "activeStartTime");
        roomSummary.setStartDateTime(new DateTime(conference.get(timeField)));
        return roomSummary;
    }

    private synchronized Collection<RoomParticipant> getRoomParticipants(String roomId, boolean withHidden) throws CommandException
    {
        Command cmd = new Command("participant.enumerate");
        cmd.setParameter("operationScope", new String[]{"currentState"});
        cmd.setParameter("enumerateFilter", "connected");
        List<Map<String, Object>> participants = execApiEnumerate(cmd, "participants");

        List<RoomParticipant> hiddenResult = new ArrayList<RoomParticipant>();
        List<RoomParticipant> result = new ArrayList<RoomParticipant>();
        for (Map<String, Object> participant : participants) {
            if (participant == null) {
                continue;
            }
            if (!roomId.equals(participant.get("conferenceName"))) {
                // not from this room
                continue;
            }
            Map<String, Object> participantState = (Map<String, Object>) participant.get("currentState");
            String participantAddress = (String) participantState.get("address");
            if (participantAddress != null && hiddenParticipantAddresses.contains(participantAddress)) {
                hiddenResult.add(extractRoomParticipant(participant));
            }
            else {
                result.add(extractRoomParticipant(participant));
            }
        }

        // Append hidden participants to the end
        if (withHidden) {
            result.addAll(hiddenResult);
        }

        return result;
    }

    /**
     * Extracts a {@link RoomParticipant} out of participant.enumerate or participant.status result.
     *
     * @param participant participant structure, as defined in the MCU API, command participant.status
     * @return {@link RoomParticipant} extracted from the participant structure
     */
    private RoomParticipant extractRoomParticipant(Map<String, Object> participant)
    {
        RoomParticipant roomParticipant = new RoomParticipant();
        RoomParticipantIdentifier identifier = new RoomParticipantIdentifier(participant);
        String protocol = identifier.getParticipantProtocol();

        String roomId = (String) participant.get("conferenceName");
        roomParticipant.setId(identifier.toString());
        roomParticipant.setRoomId(roomId);

        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) participant.get("currentState");

        String address = (String) state.get("address");
        AliasType aliasType;
        if (protocol.equals("sip")) {
            aliasType = AliasType.SIP_URI;
        }
        else {
            if (E164_PATTERN.matcher(address).matches()) {
                aliasType = AliasType.H323_E164;
            }
            else {
                aliasType = AliasType.H323_URI;
            }
        }
        roomParticipant.setAlias(new Alias(aliasType, address));
        roomParticipant.setDisplayName((String) state.get("displayName"));

        roomParticipant.setMicrophoneEnabled(!(Boolean) state.get("audioRxMuted"));
        roomParticipant.setVideoEnabled(!(Boolean) state.get("videoRxMuted"));
        if (state.get("audioRxGainMode").equals("fixed")) {
            double gainDb = (double)((Integer) state.get("audioRxGainMillidB")) / 1000.0;
            roomParticipant.setMicrophoneLevel((int) (MathHelper.getPercentFromDb(gainDb, MAX_ABS_GAIN_DB * 5.0) + 5));
        }
        else {
            roomParticipant.setMicrophoneLevel(RoomParticipant.DEFAULT_MICROPHONE_LEVEL);
        }
        roomParticipant.setJoinTime(new DateTime(state.get("connectTime")));

        String previewUrl = (String) state.get("previewURL");
        if (previewUrl != null && !previewUrl.isEmpty()) {
            String cacheId = roomId + ":" + roomParticipant.getId();
            synchronized (roomParticipantSnapshotUrlCache) {
                roomParticipantSnapshotUrlCache.put(cacheId, previewUrl);
            }
            roomParticipant.setVideoSnapshot(true);
        }

        // We can't get room layout, because it is current participant layout and not the configured one
        // (we need "cpLayout" attribute but it is missing).
        // If we configure a "GRID" layout, and only one participant is present the "currentLayout=1",
        // but we need "cpLayout=<grid-index>"
        //if (currentState.containsKey("currentLayout")) {
        //    roomParticipant.setLayout(getRoomLayoutByLayoutIndex((Integer) currentState.get("currentLayout")));
        //}
        return roomParticipant;
    }

    private void identifyParticipant(Command cmd, String roomId, String roomParticipantId)
    {
        RoomParticipantIdentifier roomParticipantIdentifier = new RoomParticipantIdentifier(roomParticipantId);
        cmd.setParameter("conferenceName", truncateString(roomId));
        cmd.setParameter("participantProtocol", truncateString(roomParticipantIdentifier.getParticipantProtocol()));
        cmd.setParameter("participantName", truncateString(roomParticipantIdentifier.getParticipantName()));
        // NOTE: it is necessary to identify a participant also by type; ad_hoc participants receive auto-generated
        //       numbers, so we distinguish the type by the fact whether the name is a number or not
        cmd.setParameter("participantType",
                (StringUtils.isNumeric(roomParticipantIdentifier.getParticipantName()) ? "ad_hoc" : "by_address"));
    }

    /**
     * @param roomId
     * @param roomParticipantId
     * @return snapshot {@link MediaData} for given {@code roomId} and {@code roomParticipantId}
     * @throws CommandException
     */
    private MediaData getRoomParticipantSnapshot(String roomId, String roomParticipantId) throws CommandException
    {
        String roomParticipantSnapshotUrl;
        String cacheId = roomId + ":" + roomParticipantId;
        synchronized (this) {
            synchronized (roomParticipantSnapshotUrlCache) {
                roomParticipantSnapshotUrl = roomParticipantSnapshotUrlCache.get(cacheId);
                if (roomParticipantSnapshotUrl == null) {
                    Command cmd = new Command("participant.status");
                    cmd.setParameter("operationScope", new String[]{"currentState"});
                    identifyParticipant(cmd, roomId, roomParticipantId);
                    Map<String, Object> result = execApi(cmd);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> state = (Map<String, Object>) result.get("currentState");
                    roomParticipantSnapshotUrl = (String) state.get("previewURL");
                    if (roomParticipantSnapshotUrl == null) {
                        throw new CommandException("Participant " + roomParticipantId + " doesn't have snapshot.");
                    }
                    roomParticipantSnapshotUrlCache.put(cacheId, roomParticipantSnapshotUrl);
                }
            }
        }

        logger.debug("Fetching snapshot for participant {} in room {}...", roomParticipantId, roomId);
        try {
            MediaData mediaData = execHttp(roomParticipantSnapshotUrl);
            MediaType mediaType = mediaData.getType();
            String type = mediaType.getType();
            if (mediaType.equals(MediaType.TEXT_PLAIN)) {
                String error = new String(mediaData.getData());
                if (error.contains("Unable to generate participant preview")) {
                    throw new CommandException("Cannot get snapshot for participant " + roomParticipantId
                            + ". Participant doesn't exist.");
                }
                else {
                    throw new CommandException("Cannot get snapshot for participant " + roomParticipantId + "." + error);
                }

            }
            if (!type.equals("image")) {
                throw new CommandException(
                        "Cannot get participant snapshot. Device returned " + mediaType + " instead of image.");
            }
            return mediaData;
        }
        catch (CommandException exception) {
            logger.warn("Retrieving snapshot for participant " + roomParticipantId + " in room " + roomId + " failed.",
                    exception);
            return null;
        }
    }

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

        CiscoMCUConnector connector = new CiscoMCUConnector();
        connector.connect(DeviceAddress.parseAddress(address), username, password);

        // Participant snapshot
        //MediaData mediaData = connector.getRoomParticipantSnapshots("YY-shongo-local-qgotdi", "4");
        //System.out.println(mediaData.getType() + " " + mediaData.getData());

        // Room status by multiple threads
        /*List<Thread> threads = new LinkedList<Thread>();
        for (int i = 0; i < 2; i++ ) {
            Thread thread = new Thread() {
                @Override
                public void run()
                {
                    try {
                        Room shongoTestRoom = conn.getRoom("shongo-test");
                        System.out.println("shongo-test room:");
                        System.out.println(shongoTestRoom);
                    }
                    catch (CommandException exception) {
                        exception.printStackTrace();
                    }
                    super.run();
                }
            };
            thread.start();
            threads.add(thread);
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            }
            catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }*/

        // gatekeeper status
//        Map<String, Object> gkInfo = conn.execApi(new Command("gatekeeper.query"));
//        System.out.println("Gatekeeper status: " + gkInfo.get("gatekeeperUsage"));

        // test of listRooms() command
//        Collection<RoomInfo> roomList = conn.listRooms();
//        System.out.println("Existing rooms:");
//        for (RoomInfo room : roomList) {
//            System.out.printf("  - %s (%s, started at %s, owned by %s)\n", room.getCode(), room.getType(),
//                    room.getStartDateTime(), room.getOwner());
//        }

        // test that the second enumeration query fills data that has not changed and therefore were not transferred
//        Command enumParticipantsCmd = new Command("participant.enumerate");
//        enumParticipantsCmd.setParameter("operationScope", new String[]{"currentState"});
//        enumParticipantsCmd.setParameter("enumerateFilter", "connected");
//        List<Map<String, Object>> participants = conn.execApiEnumerate(enumParticipantsCmd, "participants");
//        List<Map<String, Object>> participants2 = conn.execApiEnumerate(enumParticipantsCmd, "participants");

        // test that the second enumeration query fills data that has not changed and therefore were not transferred
//        Command enumConfCmd = new Command("conference.enumerate");
//        enumConfCmd.setParameter("moreThanFour", Boolean.TRUE);
//        enumConfCmd.setParameter("enumerateFilter", "completed");
//        List<Map<String, Object>> confs = conn.execApiEnumerate(enumConfCmd, "conferences");
//        List<Map<String, Object>> confs2 = conn.execApiEnumerate(enumConfCmd, "conferences");

        // test of getRoom() command
//        Room shongoTestRoom = conn.getRoom("shongo-test");
//        System.out.println("shongo-test room:");
//        System.out.println(shongoTestRoom);

        // test of deleteRoom() command
//        Collection<RoomInfo> roomList = conn.listRooms();
//        System.out.println("Existing rooms:");
//        for (RoomInfo room : roomList) {
//            System.out.println(room);
//        }
//        System.out.println("Deleting 'shongo-test'");
//        conn.deleteRoom("shongo-test");
//        roomList = conn.listRooms();
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
//        Collection<RoomInfo> roomList = conn.listRooms();
//        System.out.println("Existing rooms:");
//        for (RoomInfo room : roomList) {
//            System.out.println(room);
//        }

        // test of bad caching
//        Room newRoom = new Room("shongo-testX", 5);
//        String newRoomId = conn.createRoom(newRoom);
//        System.out.println("Created room " + newRoomId);
//        Collection<RoomSummary> roomList = conn.listRooms();
//        System.out.println("Existing rooms:");
//        for (RoomSummary roomSummary : roomList) {
//            System.out.println(roomSummary);
//        }
//        conn.deleteRoom(newRoomId);
//        System.out.println("Deleted room " + newRoomId);
//        Map<String, Object> atts = new HashMap<String, Object>();
//        atts.put(Room.NAME, "shongo-testing");
//        String changedRoomId = conn.modifyRoom("shongo-test", atts, null);
//        Collection<RoomSummary> newRoomList = conn.listRooms();
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

        // test of listRoomParticipants() method
//        System.out.println("Listing shongo-test room:");
//        Collection<RoomParticipant> shongoUsers = conn.listRoomParticipants("shongo-test");
//        for (RoomParticipant ru : shongoUsers) {
//            System.out.println("  - " + ru.getUserId() + " (" + ru.getDisplayName() + ")");
//        }
//        System.out.println("Listing done");

        // user connect by alias
//        String ruId = conn.dialRoomParticipant("shongo-test", new Alias(Technology.H323, AliasType.E164, "950081038"));
//        System.out.println("Added user " + ruId);
        // user connect by address
//        String ruId2 = conn.dialRoomParticipant("shongo-test", "147.251.54.102");
        // user disconnect
//        conn.disconnectRoomParticipant("shongo-test", "participant1");

//        System.out.println("All done, disconnecting");

        // test of modifyRoomParticipant
//        Map<String, Object> attributes = new HashMap<String, Object>();
//        attributes.put(RoomParticipant.VIDEO_ENABLED, Boolean.TRUE);
//        attributes.put(RoomParticipant.DISPLAY_NAME, "Ondrej Bouda");
//        conn.modifyRoomParticipant("shongo-test", "3447", attributes);

        //Room room = conn.getRoom("shongo-test");

        connector.disconnect();
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
     * @param layoutIndex   index of the layout as defined by Cisco
     * @return room layout according to the given Cisco layout index
     */
    private static RoomLayout getRoomLayoutByLayoutIndex(int layoutIndex)
    {
        switch (layoutIndex) {
            case 1:
                return RoomLayout.SPEAKER;
            case 2:
            case 3:
            case 4:
            case 8:
            case 9:
                return RoomLayout.GRID;
            case 5:
            case 6:
            case 7:
                return RoomLayout.SPEAKER_CORNER;
            default:
                return RoomLayout.OTHER;
        }
    }

    /**
     * @param roomLayout
     * @return Cisco layout index according to the given room layout
     */
    private static Integer getLayoutIndexByRoomLayout(RoomLayout roomLayout)
    {
        switch (roomLayout) {
            case OTHER:
                return null;
            case SPEAKER:
                return 1;
            case SPEAKER_CORNER:
                return 5;
            case GRID:
                return 3;
            default:
                throw new TodoImplementException(roomLayout);
        }
    }

    private static class RoomParticipantIdentifier
    {
        private final String participantProtocol;

        private final String participantName;

        public RoomParticipantIdentifier(Map<String, Object> participant)
        {
            this.participantProtocol = (String) participant.get("participantProtocol");
            this.participantName = (String) participant.get("participantName");
        }

        public RoomParticipantIdentifier(String roomParticipantId)
        {
            String[] parts = roomParticipantId.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Room participant id must in format '<protocol>:<name>'.");
            }
            this.participantProtocol = parts[0];
            this.participantName = parts[1];
        }

        public String getParticipantProtocol()
        {
            return participantProtocol;
        }

        public String getParticipantName()
        {
            return participantName;
        }

        @Override
        public String toString()
        {
            return participantProtocol + ":" + participantName;
        }
    }
}
