package cz.cesnet.shongo.connector.device;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.RecordingService;
import cz.cesnet.shongo.connector.api.RecordingSettings;
import cz.cesnet.shongo.connector.api.UsageStats;
import cz.cesnet.shongo.connector.common.AbstractMultipointConnector;
import cz.cesnet.shongo.connector.common.RequestAttributeList;
import cz.cesnet.shongo.controller.api.jade.NotifyTarget;
import cz.cesnet.shongo.controller.api.jade.Service;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.JDOMParseException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link cz.cesnet.shongo.connector.common.AbstractConnector} for Adobe Connect.
 *
 * @author opicak <pavelka@cesnet.cz>
 */
public class AdobeConnectConnector extends AbstractMultipointConnector implements RecordingService
{
    private static Logger logger = LoggerFactory.getLogger(AdobeConnectConnector.class);

    /**
     * Options for the {@link AdobeConnectConnector}.
     */
    public static final String CAPACITY_CHECK_PERIOD = "capacity-check-period";
    public static final Duration CAPACITY_CHECK_PERIOD_DEFAULT = Duration.standardMinutes(5);
    public static final String MEETINGS_FOLDER_NAME = "meetings-folder-name";
    public static final String RECORDINGS_FOLDER_NAME = "recordings-folder-name";
    public static final String RECORDINGS_PREFIX = "recordings-prefix";
    public static final String RECORDINGS_CHECK_PERIOD = "recordings-check-period";
    public static final Duration RECORDINGS_CHECK_PERIOD_DEFAULT = Duration.standardMinutes(5);
    public static final String URL_PATH_EXTRACTION_FROM_URI = "url-path-extraction-from-uri";


    /**
     * Small timeout used between some AC request
     */
    public final static long REQUEST_DELAY = 100;

    /**
     * The Java session ID that is generated upon successful login.  All calls
     * except login must provide this ID for authentication.
     */
    private String connectionSession;

    /**
     * @see ConnectionState
     */
    private ConnectionState connectionState;

    /**
     * Patterns for options.
     */
    private Pattern urlPathExtractionFromUri = null;

    /**
     * Name of folder for meetings
     */
    protected String meetingsFolderName;

    /**
     * Root folder ID for meetings
     */
    protected String meetingsFolderId;

    /**
     * This is the user log in name, typically the user email address.
     */
    private String login;

    /**
     * The password of the user.
     */
    private String password;

    /**
     * Timeout for checking room capacity.
     */
    private int capacityCheckTimeout;

    /**
     * Thread for capacity checking.
     */
    private AtomicReference<Thread> capacityCheckThreadReference;

    /**
     * If capacity check is running.
     */
    private volatile boolean capacityChecking = false;

    /**
     * @see AdobeConnectRecordingManager
     */
    private AdobeConnectRecordingManager recordingManager;

    @Override
    public void connect(DeviceAddress deviceAddress, String username, String password) throws CommandException
    {
        this.login = username;
        this.password = password;

        // Setup options
        this.capacityCheckTimeout = (int) configuration.getOptionDuration(
                CAPACITY_CHECK_PERIOD, CAPACITY_CHECK_PERIOD_DEFAULT).getMillis();
        this.urlPathExtractionFromUri = configuration.getOptionPattern(URL_PATH_EXTRACTION_FROM_URI);
        this.meetingsFolderName = configuration.getOptionString(MEETINGS_FOLDER_NAME);

        this.login();

        if (this.recordingManager != null) {
            this.recordingManager.destroy();
        }
        this.recordingManager = new AdobeConnectRecordingManager(this);
    }

    @Override
    public ConnectionState getConnectionState()
    {
        try {
            execApi(getActionUrl("common-info"), CONNECTION_STATE_TIMEOUT);
            return ConnectionState.CONNECTED;
        }
        catch (Exception exception) {
            logger.warn("Not connected", exception);
            return ConnectionState.DISCONNECTED;
        }
    }

    @Override
    public void disconnect() throws CommandException
    {
        synchronized (capacityCheckThreadReference) {
            Thread capacityCheckThread = this.capacityCheckThreadReference.get();
            this.capacityCheckThreadReference.set(null);
            capacityCheckThread.interrupt();
        }
        this.recordingManager.destroy();
        this.connectionState = ConnectionState.DISCONNECTED;
        this.logout();
    }

    /**
     * Creates Adobe Connect user.
     *
     * @return user identification (principal-id)
     */
    protected String createAdobeConnectUser(String principalName, UserInformation userInformation) throws CommandException
    {
        if (principalName == null) {
            throw new IllegalArgumentException("Principal mustn't be null.");
        }

        RequestAttributeList userSearchAttributes = new RequestAttributeList();
        userSearchAttributes.add("filter-login", principalName);
        Element principalList = execApi("principal-list", userSearchAttributes);

        if (principalList.getChild("principal-list").getChild("principal") != null)
            return principalList.getChild("principal-list").getChild("principal")
                    .getAttributeValue("principal-id");

        RequestAttributeList newUserAttributes = new RequestAttributeList();
        newUserAttributes.add("first-name", userInformation.getFirstName());
        newUserAttributes.add("last-name", userInformation.getLastName());
        newUserAttributes.add("login", principalName);
        newUserAttributes.add("email", userInformation.getPrimaryEmail());
        newUserAttributes.add("type", "user");
        newUserAttributes.add("has-children", "false");

        Element response = execApi("principal-update", newUserAttributes);
        return response.getChild("principal").getAttributeValue("principal-id");
    }

    /**
     * Set session state
     *
     * @param roomId identifier of the room
     * @param state  state of session; true for end, false for start session
     */
    protected void endMeetingUpdate(String roomId, Boolean state, String message, Boolean redirect, String url) throws CommandException
    {
        RequestAttributeList sessionsAttributes = new RequestAttributeList();
        sessionsAttributes.add("sco-id", roomId);
        Element sessionsResponse = execApi("report-meeting-sessions", sessionsAttributes);

        if (sessionsResponse.getChild("report-meeting-sessions").getChildren().size() == 0) {
            return;
        }

        RequestAttributeList endMeetingAttributes = new RequestAttributeList();
        endMeetingAttributes.add("sco-id", roomId);
        endMeetingAttributes.add("state", state.toString());

        if (message != null) {
            endMeetingAttributes.add("message",message);
        }

        if (redirect == true && url != null) {
            endMeetingAttributes.add("redirect",redirect.toString());
            endMeetingAttributes.add("url",url);
        }

        try {
            logger.debug("{} meeting (sco-ID: {}) session.", (state ? "Starting" : "Ending"), roomId);
            execApi("meeting-roommanager-endmeeting-update", endMeetingAttributes);
        } catch (CommandException exception) {
            logger.warn("Failed to end/start meeting. Probably just AC error, everything should be working properly.",
                    exception);
        }
    }

    /**
     * End current session.
     *
     * @param roomId identifier of the room
     * @throws CommandException
     */
    protected void endMeeting(String roomId) throws CommandException
    {
        String message = null; //"The room is currently unavailable for joining / Do místnosti se aktuálně není možné připojit";

        endMeeting(roomId, message, false, null);
    }

    /**
     * End current session, set message show after stopping meeting, set url to be redirect (for recreating rooms)
     *
     * @param roomId identifier of the room
     * @param message message shown after ending meeting session
     * @param redirect boolean value for redirecting after ending meeting session
     * @param url url to redirect if redirect == true
     * @throws CommandException
     */
    protected void endMeeting(String roomId, String message, Boolean redirect, String url) throws CommandException
    {
        endMeetingUpdate(roomId, true, message, redirect, url);
    }

    /**
     * Start new session. Host can do it from AC.
     *
     * @param roomId identifier of the room
     * @throws CommandException
     */
    protected void startMeeting(String roomId) throws CommandException
    {
        endMeetingUpdate(roomId, false, null, false, null);
    }

    /**
     * Returns room access mode (public, protected, private). Mode in AC v8.*, v9.0, v9.1 are "view-hidden" (public), "remove" (protected), "denied" (private)
     *
     * @param roomId
     * @return
     * @throws CommandException
     */
    protected AdobeConnectPermissions getRoomAccessMode(String roomId) throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("acl-id",roomId);
        attributes.add("filter-principal-id","public-access");

        String accessMode = execApi("permissions-info", attributes).getChild("permissions").getChild("principal").getAttributeValue(
                "permissions-id");

        AdobeConnectPermissions adobeConnectAccessMode = AdobeConnectPermissions.PROTECTED;

        switch (AdobeConnectPermissions.valueByCode(accessMode)) {
            case PRIVATE:
                adobeConnectAccessMode = AdobeConnectPermissions.PRIVATE;
                break;
            case PROTECTED:
                adobeConnectAccessMode = AdobeConnectPermissions.PROTECTED;
                break;
            case PUBLIC:
                adobeConnectAccessMode = AdobeConnectPermissions.PUBLIC;
                break;
        }

        return adobeConnectAccessMode;
    }

    /**
     * Set {@link cz.cesnet.shongo.api.AdobeConnectPermissions} room access mode (public, protected, private).
     * Default access mode (when param mode is null) is AdobeConnectAccessMode.PROTECTED
     *
     * @param roomId
     * @param roomAccessMode
     */
    protected void setRoomAccessMode(String roomId, AdobeConnectPermissions roomAccessMode) throws CommandException
    {
        AdobeConnectPermissions.checkIfUsableByMeetings(roomAccessMode);
        setScoPermissions(roomId, roomAccessMode);
    }

    /**
     * Make room public (Anyone who has the URL for the meeting can enter the ro
     om).
     * @param roomId
     */
    public void makeRoomPublic(String roomId) throws CommandException
    {
        setRoomAccessMode(roomId, AdobeConnectPermissions.PUBLIC);
    }

    /**
     * Make room protected (Only registered users and accepted guests can enter the room).
     * @param roomId
     */
    public void makeRoomProtected(String roomId) throws CommandException
    {
        setRoomAccessMode(roomId,AdobeConnectPermissions.PROTECTED);
    }

    /**
     * Make room private (Only registered users and participants can enter).
     * @param roomId
     */
    public void makeRoomPrivate(String roomId) throws CommandException
    {
        setRoomAccessMode(roomId, AdobeConnectPermissions.PRIVATE);
    }

    /**
     * Set permissions for SCO (public, protected, private). Mode in AC v8+ are "view-hidden" (public), "remove" (protected/none), "denied" (private)
     * Default access mode (when param permissionId is null) is remove.
     *
     * @param scoId
     * @param permissionId
     */
    protected void setScoPermissions(String scoId, AdobeConnectPermissions permissionId) throws CommandException
    {
        RequestAttributeList accessModeAttributes = new RequestAttributeList();
        accessModeAttributes.add("acl-id",scoId);
        accessModeAttributes.add("principal-id","public-access");
        if (permissionId == null) {
            accessModeAttributes.add("permission-id",AdobeConnectPermissions.PROTECTED.getPermissionId());
        } else {
            accessModeAttributes.add("permission-id",permissionId.getPermissionId());
        }

        execApi("permissions-update", accessModeAttributes);
    }

    @Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException, CommandUnsupportedException
    {
        return null; // TODO
    }

    @Override
    public UsageStats getUsageStats() throws CommandException, CommandUnsupportedException
    {
        //report-bulk-consolidated-transactions
        //report-meeting....
        return null;  //TODO
    }

    @Override
    public String createRecordingFolder(RecordingFolder recordingFolder) throws CommandException
    {
        return recordingManager.createRecordingFolder(recordingFolder);
    }

    @Override
    public void modifyRecordingFolder(RecordingFolder recordingFolder) throws CommandException
    {
        recordingManager.modifyRecordingFolder(recordingFolder);
    }

    @Override
    public void deleteRecordingFolder(String recordingFolderId) throws CommandException
    {
        recordingManager.deleteRecordingFolder(recordingFolderId);
    }

    @Override
    public Collection<Recording> listRecordings(String recordingFolderId) throws CommandException
    {
        return recordingManager.listRecordings(recordingFolderId);
    }

    @Override
    public Recording getRecording(String recordingId) throws CommandException
    {
        return recordingManager.getRecording(recordingId);
    }

    @Override
    public Recording getActiveRecording(Alias alias) throws CommandException
    {
        return recordingManager.getActiveRecording(alias);
    }

    @Override
    public boolean isRecordingActive(String recordingId) throws CommandException
    {
        return recordingManager.isRecordingActive(recordingId);
    }

    @Override
    public String startRecording(String recordingFolderId, Alias alias, String recordingPrefixName, RecordingSettings recordingSettings)
            throws CommandException
    {
        return recordingManager.startRecording(recordingFolderId, alias, recordingSettings);
    }

    @Override
    public void stopRecording(String recordingId) throws CommandException
    {
        recordingManager.stopRecording(recordingId);
    }

    @Override
    public void deleteRecording(String recordingId) throws CommandException
    {
        recordingManager.deleteRecording(recordingId);
    }

    @Override
    public void checkRecording(String recordingId) throws CommandException
    {
        recordingManager.checkRecording(recordingId);
    }

    @Override
    public void checkRecordings() throws CommandException
    {
        recordingManager.checkRecordings();
    }

    @Override
    public void makeRecordingPublic(String recordingId) throws CommandException {
        recordingManager.makeRecordingPublic(recordingId);
    }

    @Override
    public void makeRecordingPrivate(String recordingId) throws CommandException {
        recordingManager.makeRecordingPrivate(recordingId);
    }

    @Override
    public void makeRecordingFolderPublic(String recordingFolderId) throws CommandException, CommandUnsupportedException {
        recordingManager.makeRecordingFolderPublic(recordingFolderId);
    }

    @Override
    public void makeRecordingFolderPrivate(String recordingFolderId) throws CommandException, CommandUnsupportedException {
        recordingManager.makeRecordingFolderPrivate(recordingFolderId);
    }

    @Override
    public boolean isRecordingFolderPublic(String recordingFolderId) throws CommandException {
        return recordingManager.isRecordingFolderPublic(recordingFolderId);
    }

    @Override
    public MediaData getRoomContent(String roomId) throws CommandException, CommandUnsupportedException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", roomId);

        Element response = execApi("sco-contents", attributes);

        for (Element child : response.getChild("scos").getChildren("sco")) {
            //TODO: archive all
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addRoomContent(String roomId, String name, MediaData data)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function.");
    }

    @Override
    public void removeRoomContentFile(String roomId, String name) throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", roomId);
        attributes.add("filter-name", name);

        Element response = execApi("sco-contents", attributes);

        if (response.getChild("scos").getChild("sco") != null) {
            deleteSCO(response.getChild("scos").getChild("sco").getAttributeValue("sco-id"));
        }
    }

    @Override
    public void clearRoomContent(String roomId) throws CommandException, CommandUnsupportedException
    {
        // TODO: erase content and re-create room?
    }

    @Override
    public Collection<RoomSummary> listRooms() throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("filter-type", "meeting");

        Element response = execApi("report-bulk-objects", attributes);

        List<RoomSummary> meetings = new ArrayList<RoomSummary>();

        for (Element room : response.getChild("report-bulk-objects").getChildren("row")) {
            if (room.getChildText("name").matches("(?i).*Template")) {
                continue;
            }

            RoomSummary roomSummary = new RoomSummary();
            roomSummary.setId(room.getAttributeValue("sco-id"));
            roomSummary.setName(room.getChildText("name"));
            roomSummary.setDescription(room.getChildText("description"));
            roomSummary.setAlias(room.getChildText("url"));

            String dateCreated = room.getChildText("date-created");
            if (dateCreated != null) {
                roomSummary.setStartDateTime(DateTime.parse(dateCreated));
            }

            meetings.add(roomSummary);
        }

        return Collections.unmodifiableList(meetings);
    }

    @Override
    public Room getRoom(String roomId) throws CommandException
    {
        Room room = new Room();
        try {
            Element sco = getScoInfo(roomId);

            room.setId(roomId);
            room.addAlias(AliasType.ROOM_NAME, sco.getChildText("name"));
            room.setDescription(sco.getChildText("description"));
            if (sco.getChildText("sco-tag") != null) {
                room.setLicenseCount(Integer.valueOf(sco.getChildText("sco-tag")));
            }
            else {
                logger.warn("Licence count not set for room " + roomId + " (using 0 licenses).");
                room.setLicenseCount(0);
            }
            room.addTechnology(Technology.ADOBE_CONNECT);

            String uri = "https://" + deviceAddress + sco.getChildText("url-path");
            room.addAlias(new Alias(AliasType.ADOBE_CONNECT_URI, uri));

            // options
            AdobeConnectRoomSetting adobeConnectRoomSetting = new AdobeConnectRoomSetting();
            String pin = sco.getChildText("meeting-passcode");
            if (pin != null) {
                adobeConnectRoomSetting.setPin(pin);
            }

            adobeConnectRoomSetting.setAccessMode(getRoomAccessMode(roomId));
            room.addRoomSetting(adobeConnectRoomSetting);


        }
        catch (RequestFailedCommandException exception) {
            if ("no-data".equals(exception.getCode())) {
                return null;
            }
        }
        return room;
    }

    private void setRoomAttributes(RequestAttributeList attributes, Room room)
            throws CommandException
    {
        // Set the description
        if (room.getDescription() != null) {
            attributes.add("description", room.getDescription());
        }

        // Set capacity
        attributes.add("sco-tag", String.valueOf(room.getLicenseCount()));


        // Create/Update aliases
        if (room.getAliases() != null) {
            for (Alias alias : room.getAliases()) {
                switch (alias.getType()) {
                    case ROOM_NAME:
                        attributes.add("name", alias.getValue());
                        break;
                    case ADOBE_CONNECT_URI:
                        if (urlPathExtractionFromUri == null) {
                            throw new CommandException(String.format(
                                    "Cannot set Adobe Connect Url Path - missing connector device option '%s'",
                                    URL_PATH_EXTRACTION_FROM_URI));
                        }
                        Matcher matcher = urlPathExtractionFromUri.matcher(alias.getValue());
                        if (!matcher.find()) {
                            throw new CommandException("Invalid Adobe Connect URI: " + alias.getValue());
                        }
                        attributes.add("url-path", matcher.group(1));
                        break;
                    default:
                        throw new RuntimeException("Unrecognized alias: " + alias.toString());
                }
            }
        }
    }

    /**
     * Sets participants roles in room.
     *
     * @param roomId Identifier of the room
     * @param participants Collection of participants
     * @throws CommandException
     */
    protected void addRoomParticipants(String roomId, List<RoomParticipantRole> participants) throws CommandException
    {
        if (participants.size() == 0) {
            return;
        }
        RequestAttributeList userAttributes = new RequestAttributeList();
        userAttributes.add("acl-id", roomId);

        for (RoomParticipantRole participant : participants) {
            UserInformation userInformation = getUserInformationById(participant.getUserId());
            if (userInformation == null) {
                throw new CommandException("User " + participant.getUserId() + " doesn't exist.");
            }

            // Configure all principal names for participant
            Set<String> principalNames = userInformation.getPrincipalNames();
            if (principalNames.size() == 0) {
                throw new CommandException("User " + userInformation.getFullName() + " has no principal names.");
            }
            for (String principalName : principalNames) {
                //Skip configuring principal name if too long
                if (principalName.length() > 60) {
                    logger.warn("Skipping configuration of principal name '{}'. Name to long.",
                            new Object[]{principalName});
                    continue;
                }
                String principalId = createAdobeConnectUser(principalName, userInformation);
                String role = "remove";
                switch (participant.getRole()) {
                    case PARTICIPANT:
                        role = "view";
                        break;
                    case PRESENTER:
                        role = "mini-host";
                        break;
                    case ADMINISTRATOR:
                        role = "host";
                        break;
                }
                userAttributes.add("principal-id", principalId);
                userAttributes.add("permission-id", role);
                logger.debug("Configuring participant '{}' in the room (principal-id: {}, role: {}).",
                        new Object[]{userInformation.getFullName(), principalId, role});
            }
        }

        execApi("permissions-update", userAttributes);
    }

    /**
     * Return permissions of the SCO.
     *
     * @param scoId identifier of the room
     * @return XML Element from API call "permissions-info"
     * @throws CommandException
     */
    protected Element getSCOPermissions(String scoId) throws CommandException
    {
        RequestAttributeList permissionsAttributes = new RequestAttributeList();
        permissionsAttributes.add("acl-id", scoId);
        permissionsAttributes.add("filter-out-permission-id", "null");

        return execApi("permissions-info", permissionsAttributes);
    }

    protected void resetPermissions(String roomId) throws CommandException
    {
        RequestAttributeList permissionsResetAttributes = new RequestAttributeList();
        permissionsResetAttributes.add("acl-id", roomId);

        execApi("permissions-reset", permissionsResetAttributes);
    }

    @Override
    public String createRoom(Room room) throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("folder-id",
                (this.meetingsFolderId != null ? this.meetingsFolderId : this.getMeetingsFolderId()));
        attributes.add("type", "meeting");
        attributes.add("date-begin", DateTime.now().toString());

        // Set room attributes
        setRoomAttributes(attributes, room);

        // Room name must be filled
        if (attributes.getValue("name") == null) {
            throw new RuntimeException("Room name must be filled for the new room.");
        }

        Element response = execApi("sco-update", attributes);
        String roomId = response.getChild("sco").getAttributeValue("sco-id");

        // Add room participants
        if (room.getLicenseCount() > 0) {
            startMeeting(roomId);
            addRoomParticipants(roomId, room.getParticipantRoles());
        }
        else if (room.getLicenseCount() == 0) {
            endMeeting(roomId);
        }

        // Set passcode (pin), since Adobe Connect 9.0
        RequestAttributeList passcodeAttributes = new RequestAttributeList();
        passcodeAttributes.add("acl-id",roomId);
        passcodeAttributes.add("field-id","meeting-passcode");

        String pin = "";
        AdobeConnectPermissions accessMode = null;
        AdobeConnectRoomSetting adobeConnectRoomSetting = room.getRoomSetting(AdobeConnectRoomSetting.class);
        if (adobeConnectRoomSetting != null) {
            pin = adobeConnectRoomSetting.getPin() == null ? "" : adobeConnectRoomSetting.getPin();
            accessMode = adobeConnectRoomSetting.getAccessMode();
        }

        // Set pin for room if set
        passcodeAttributes.add("value",pin);
        execApi("acl-field-update", passcodeAttributes);

        // Set room access mode, when null setRoomAccessMode set default value {@link AdobeConnectAccessMode.PROTECTED}
        setRoomAccessMode(roomId,accessMode);

        //importRoomSettings(response.getChild("sco").getAttributeValue("sco-id"),room.getConfiguration());

        return roomId;
    }

    @Override
    protected boolean isRecreateNeeded(Room oldRoom, Room newRoom) throws CommandException
    {
        String roomId = oldRoom.getId();
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", roomId);
        attributes.add("type", "meeting");
        setRoomAttributes(attributes, newRoom);
        String newRoomUrl = attributes.getValue("url-path");
        Alias oldRoomAlias = oldRoom.getAlias(AliasType.ADOBE_CONNECT_URI);
        String oldRoomUrl = oldRoomAlias != null ? oldRoomAlias.getValue() : null;
        String segment = getLastPathSegmentFromURI(oldRoomUrl);
        return segment == null || !segment.equalsIgnoreCase(newRoomUrl);
    }

    @Override
    public void onModifyRoom(Room room) throws CommandException
    {
        String roomId = room.getId();

        getRoom(roomId);
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", roomId);
        attributes.add("type", "meeting");

        // Set room attributes
        setRoomAttributes(attributes, room);

        // Add/modify participants
        if (room.getLicenseCount() > 0) {
            //TODO: set permisions for recordings - done in controller
            resetPermissions(roomId);
            startMeeting(roomId);
            addRoomParticipants(roomId, room.getParticipantRoles());
        }
        else if (room.getLicenseCount() == 0) {
            recordingManager.backupRoomRecordings(roomId);
            resetPermissions(roomId);
            endMeeting(roomId);
        }

        execApi("sco-update", attributes);

        // Set passcode (pin), since Adobe Connect 9.0
        RequestAttributeList passcodeAttributes = new RequestAttributeList();
        passcodeAttributes.add("acl-id",roomId);
        passcodeAttributes.add("field-id","meeting-passcode");

        String pin = "";
        AdobeConnectPermissions accessMode = null;
        AdobeConnectRoomSetting adobeConnectRoomSetting = room.getRoomSetting(AdobeConnectRoomSetting.class);
        if (adobeConnectRoomSetting != null) {
            pin = adobeConnectRoomSetting.getPin() == null ? "" : adobeConnectRoomSetting.getPin();
            accessMode = adobeConnectRoomSetting.getAccessMode();
        }

        // Set pin for room if set
        passcodeAttributes.add("value",pin);
        execApi("acl-field-update", passcodeAttributes);

        // Set room access mode, when null setRoomAccessMode set default value {@link AdobeConnectAccessMode.PROTECTED}
        setRoomAccessMode(roomId,accessMode);
    }

    @Override
    protected String recreateRoom(Room oldRoom, Room newRoom) throws CommandException
    {
        // Create new room
        String oldRoomId = oldRoom.getId();
        String newRoomId = createRoom(newRoom);

        // Redirect from old room to new room
        try {
            //TODO: manage creating new Room with same name
            //TODO: backup recordings ???

            Alias newRoomAlias = newRoom.getAlias(AliasType.ADOBE_CONNECT_URI);
            if (newRoomAlias == null) {
                throw new CommandException("Room doesn't have ADOBE_CONNECT_URI alias.");
            }
            String newRoomUrl = newRoomAlias.getValue();
            String msg = "Room has been modified, you have been redirected to the new one (" + newRoomUrl + ").";
            endMeeting(oldRoomId, msg, true, newRoomUrl);
        }
        catch (CommandException exception) {
            deleteRoom(newRoomId);
            throw exception;
        }

        // Delete old room
        deleteRoom(oldRoomId);

        return newRoomId;
    }

    /**
     * Returns last segment of URI. For http://example.com/test/myPage returns myPage.
     *
     * @param uri given URI
     * @return last segment or null if URI has only domain
     */
    protected String getLastPathSegmentFromURI(String uri)
    {
        if (uri == null) {
            return null;
        }
        String[] uriArray = uri.split("/");
        return (uriArray.length > 1 ? uriArray[uriArray.length - 1] : null);
    }

    @Override
    public void deleteRoom(String roomId) throws CommandException
    {
        endMeeting(roomId);

        // Backup content
        recordingManager.backupRoomRecordings(roomId);

        deleteSCO(roomId);
    }

    @Override
    public String exportRoomSettings(String roomId) throws CommandException
    {
        Element scoInfo = getScoInfo(roomId);
        Document document = scoInfo.getDocument();
        if (document == null) {
            throw new IllegalStateException("Document isn't set.");
        }
        XMLOutputter xmlOutput = new XMLOutputter(Format.getPrettyFormat());
        return xmlOutput.outputString(document);
    }

    @Override
    public void importRoomSettings(String roomId, String settings) throws CommandException
    {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document document;
        try {
            document = saxBuilder.build(new StringReader(settings));
        }
        catch (Exception exception) {
            throw new CommandException(exception.getMessage(), exception);
        }

        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        String xmlString = outputter.outputString(document);


        RequestAttributeList attributes = new RequestAttributeList();

        attributes.add("sco-id", roomId);
//        attributes.add("date-begin", document.getRootElement().getChild("sco").getChild("date-begin").getText());
//        attributes.add("date-end", document.getRootElement().getChild("sco").getChild("date-end").getText());
        if (document.getRootElement().getChild("sco").getChild("description") != null) {
            attributes.add("description", document.getRootElement().getChild("sco").getChild("description").getText());
        }
        attributes.add("url-path", document.getRootElement().getChild("sco").getChild("url-path").getText());
        attributes.add("name", document.getRootElement().getChild("sco").getChild("name").getText());

        execApi("sco-update", attributes);
    }

    /**
     * @param userPrincipalId principal-id of an user
     * @return user-original-id (EPPN) for given {@code userPrincipalId} or null when user was not found
     * @throws CommandException
     */
    public String getUserPrincipalNameByPrincipalId(String userPrincipalId) throws CommandException
    {
        String userPrincipalName;
        if (cachedPrincipalNameByPrincipalId.contains(userPrincipalId)) {
            logger.debug("Using cached user-original-id by principal-id '{}'...", userPrincipalId);
            userPrincipalName = cachedPrincipalNameByPrincipalId.get(userPrincipalId);

        }
        else {
            logger.debug("Fetching user-original-id by principal-id '{}'...", userPrincipalId);
            RequestAttributeList userAttributes = new RequestAttributeList();
            userAttributes.add("filter-principal-id", userPrincipalId);
            Element userResponse = execApi("principal-list", userAttributes);

            if (userResponse.getChild("principal-list").getChild("principal") == null) {
                return null;
            }

            userPrincipalName = userResponse.getChild("principal-list").getChild("principal").getChildText("login");
            cachedPrincipalNameByPrincipalId.put(userPrincipalId, userPrincipalName);
        }
        return userPrincipalName;
    }

    @Override
    public Collection<RoomParticipant> listRoomParticipants(String roomId) throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", roomId);

        ArrayList<RoomParticipant> participantList = new ArrayList<RoomParticipant>();

        Element response;
        try {
            response = execApi("meeting-usermanager-user-list", attributes);
        }
        catch (RequestFailedCommandException exception) {
            // Participant list is not available, so return empty list
            if ("no-access".equals(exception.getCode()) && "not-available".equals(exception.getSubCode())) {
                return participantList;
            }
            // Participant list cannot be retrieved because of internal error
            else if ("internal-error".equals(exception.getCode())) {
                logger.debug("Adobe Connect issued internal error while getting meeting participants (UNSAFE API CALL)."
                        + " This should just mean, that there is no participants.", exception);
                return participantList;
            }
            throw exception;
        }

        for (Element userDetails : response.getChild("meeting-usermanager-user-list").getChildren()) {
            RoomParticipant roomParticipant = new RoomParticipant();
            roomParticipant.setId(userDetails.getChildText("user-id"));
            roomParticipant.setRoomId(roomId);
            roomParticipant.setDisplayName(userDetails.getChildText("username"));

            String role = userDetails.getChildText("role");
            if ("participant".equals(role)) {
                roomParticipant.setRole(ParticipantRole.PARTICIPANT);
            }
            else if ("presenter".equals(role)) {
                roomParticipant.setRole(ParticipantRole.PRESENTER);
            }
            else if ("host".equals(role)) {
                roomParticipant.setRole(ParticipantRole.ADMINISTRATOR);
            }

            String userPrincipalName = getUserPrincipalNameByPrincipalId(userDetails.getChildText("principal-id"));

            // If participant is registered (is not guest)
            if (userPrincipalName != null) {
                UserInformation userInformation = getUserInformationByPrincipalName(userPrincipalName);
                if (userInformation != null) {
                    roomParticipant.setUserId(userInformation.getUserId());
                }
            }

            participantList.add(roomParticipant);
        }

        return Collections.unmodifiableList(participantList);
    }

    @Override
    public RoomParticipant getRoomParticipant(String roomId, String roomParticipantId) throws CommandException
    {
        Collection<RoomParticipant> participants = this.listRoomParticipants(roomId);
        for (RoomParticipant participant : participants) {
            if (participant.getId().equals(roomParticipantId)) {
                return participant;
            }
        }
        return null;
    }

    @Override
    public Map<String, MediaData> getRoomParticipantSnapshots(String roomId, Set<String> roomParticipantIds)
            throws CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function.");
    }

    @Override
    public void modifyRoomParticipant(RoomParticipant roomParticipant) throws CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function.");
    }

    @Override
    public void modifyRoomParticipants(RoomParticipant roomParticipantConfiguration)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function.");
    }

    @Override
    public String dialRoomParticipant(String roomId, Alias alias) throws CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function.");
    }

    @Override
    public void disconnectRoomParticipant(String roomId, String roomParticipantId)
            throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", roomId);
        attributes.add("user-id", roomParticipantId);

        execApi("meeting-usermanager-remove-user", attributes);
    }

    protected Element getScoInfo(String scoId) throws CommandException
    {
        try {
            RequestAttributeList attributes = new RequestAttributeList();
            attributes.add("sco-id", scoId);

            return execApi("sco-info", attributes).getChild("sco");
        } catch (RequestFailedCommandException ex) {
            if ("no-access".equals(ex.getCode()) && "denied".equals(ex.getSubCode())) {
                throw new CommandException("SCO-ID '" + scoId + "' doesn't exist.",ex);
            } else {
                throw ex;
            }
        }
    }

    protected void renameSco(String scoId, String name) throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id",scoId);
        attributes.add("name",name);

        execApi("sco-update", attributes);
    }

    protected String getScoByUrl(String url) throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("url-path", url);

        return execApi("sco-by-url", attributes).getChild("sco").getAttributeValue("sco-id");
    }

    protected void deleteSCO(String scoId) throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", scoId);

        execApi("sco-delete", attributes);
    }

    /**
     * Retrieves the appropriate Breeze URL.
     *
     * @param action    the action to perform
     * @return the URL to perform the action
     */
    protected String getActionUrl(String action) throws CommandException
    {
        if (action == null || action.isEmpty()) {
            throw new CommandException("Action of AC call cannot be empty.");
        }
        return deviceAddress.getUrl() + "/api/xml?" + "action=" + action;
    }

    /**
     * Retrieves the appropriate Breeze URL.
     *
     * @param action    the action to perform
     * @param attributes the map os parameters for the action
     * @return the URL to perform the action
     */
    protected String getActionUrl(String action, RequestAttributeList attributes) throws CommandException
    {
        String queryString = "";
        if (attributes != null) {
            for (RequestAttributeList.Entry entry : attributes) {
                try {
                    queryString += '&' + entry.getKey() + '=' + URLEncoder.encode(entry.getValue(),"UTF8");
                } catch (UnsupportedEncodingException e) {
                    throw new CommandException("Failed to process command " + action + ": ", e);
                }
            }
        }
        return getActionUrl(action) + queryString;
    }

    protected String getConnectionSession() throws Exception
    {

        if (connectionSession == null) {
            login();
        }
        return connectionSession;
    }

    /**
     * Sets and returns SCO-ID of folder for meetings.
     *
     * @return meeting folder SCO-ID
     * @throws CommandException
     */
    protected String getMeetingsFolderId() throws CommandException
    {
        if (meetingsFolderId == null) {
            Element response = execApi("sco-shortcuts", null);
            for (Element sco : response.getChild("shortcuts").getChildren("sco")) {
                if ("meetings".equals(sco.getAttributeValue("type"))) {
                    // Find sco-id of meetings folder
                    RequestAttributeList searchAttributes = new RequestAttributeList();
                    searchAttributes.add("sco-id", sco.getAttributeValue("sco-id"));
                    searchAttributes.add("filter-is-folder", "1");

                    Element shongoFolder = execApi("sco-contents", searchAttributes);

                    for (Element folder : shongoFolder.getChild("scos").getChildren("sco")) {
                        if (meetingsFolderName.equals(folder.getChildText("name"))) {
                            meetingsFolderId = folder.getAttributeValue("sco-id");
                        }
                    }

                    // Creates meetings folder if not exists
                    if (meetingsFolderId == null) {
                        logger.debug("Folder /" + meetingsFolderName + " for shongo meetings does not exists, creating...");

                        RequestAttributeList folderAttributes = new RequestAttributeList();
                        folderAttributes.add("folder-id",
                                sco.getAttributeValue("sco-id"));
                        folderAttributes.add("name", meetingsFolderName);
                        folderAttributes.add("type", "folder");

                        Element folder = execApi("sco-update", folderAttributes);

                        meetingsFolderId = folder.getChild("sco").getAttributeValue("sco-id");

                        logger.debug("Folder /" + meetingsFolderName + " for meetings created with sco-id: " + meetingsFolderId);
                    }

                    break;
                }
            }
        }

        return meetingsFolderId;
    }

    /**
     * Performs the action to log into Adobe Connect server. Stores the breezeseession ID.
     */
    protected void login() throws CommandException
    {
        if (this.connectionSession != null) {
            logout();
        }

        RequestAttributeList loginAttributes = new RequestAttributeList();
        loginAttributes.add("login", this.login);
        loginAttributes.add("password", this.password);

        URLConnection connection;
        try {
            String loginUrl = getActionUrl("login", loginAttributes);
            connection = new URL(loginUrl).openConnection();
            connection.connect();

            InputStream resultStream = connection.getInputStream();
            Document resultDocument = new SAXBuilder().build(resultStream);
            if (this.isError(resultDocument)) {
                throw new CommandException("Login to server " + deviceAddress + " failed");
            }
            else {
                logger.debug(String.format("Login to server %s succeeded", deviceAddress));
            }
        }
        catch (Exception exception) {
            throw new CommandException(exception.getMessage(), exception);
        }

        String connectionSessionString = connection.getHeaderField("Set-Cookie");
        StringTokenizer st = new StringTokenizer(connectionSessionString, "=");
        String sessionName = null;
        if (st.countTokens() > 1) {
            sessionName = st.nextToken();
        }

        if (sessionName != null && (sessionName.equals("JSESSIONID") || sessionName.equals("BREEZESESSION"))) {
            String connectionSessionNext = st.nextToken();
            int separatorIndex = connectionSessionNext.indexOf(';');
            this.connectionSession = connectionSessionNext.substring(0, separatorIndex);
            this.meetingsFolderId = this.getMeetingsFolderId();
        }

        if (connectionSession == null) {
            throw new CommandException("Could not log in to Adobe Connect server: " + deviceAddress);
        }
        this.connectionState = ConnectionState.LOOSELY_CONNECTED;

        final AtomicReference<Thread> threadReference = new AtomicReference<>();
        threadReference.set(new Thread(Thread.currentThread().getName() + "-capacities")
        {
            private Logger logger = LoggerFactory.getLogger(AdobeConnectConnector.class);

            @Override
            public void run()
            {
                setCapacityChecking(true);
                logger.info("Check of rooms capacity - starting...");
                while (threadReference.get() != null) {
                    try {
                        Thread.sleep(capacityCheckTimeout);
                    } catch (InterruptedException e) {
                       Thread.currentThread().interrupt();
                       continue;
                    }

                    try {
                        if (isConnected()) {
                            checkAllRoomsCapacity();
                        }
                        else {
                            logger.info("Check of rooms capacity skipped, connector is disconnected.");
                        }
                    } catch (Exception exception) {
                        logger.warn("Check of rooms capacity failed", exception);
                    }
                }
                logger.info("Check of rooms capacity - exiting...");
                setCapacityChecking(false);
            }
        });
        synchronized (this) {
            if (!this.capacityChecking) {
                threadReference.get().start();
                this.capacityCheckThreadReference = threadReference;
            }
        }
    }

    private synchronized void setCapacityChecking(boolean value)
    {
        this.capacityChecking = value;
    }

    /**
     *
     * @throws CommandException
     */
    protected void checkAllRoomsCapacity() throws CommandException
    {
        Element response = execApi("report-active-meetings", new RequestAttributeList());

        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", getMeetingsFolderId());
        attributes.add("type", "meeting");
        Element shongoRoomsElement = execApi("sco-contents", attributes);

        List<String> shongoRooms = new ArrayList<String>();
        for (Element sco : shongoRoomsElement.getChild("scos").getChildren()) {
            shongoRooms.add(sco.getAttributeValue("sco-id"));
        }

        for (Element sco : response.getChild("report-active-meetings").getChildren())
        {
            String scoId = sco.getAttributeValue("sco-id");
            if (shongoRooms.contains(scoId)) {
                checkRoomCapacity(scoId);
            }
            else {
                logger.debug("There is active room (sco-id: " + scoId + ", url: " + sco.getChildText("url-path") + ", name: " + sco.getChildText("name") + "), which was not created by shongo.");
            }
        }
    }

    protected void checkRoomCapacity(String roomId) throws CommandException
    {
        Room room;
        int participants = 0;

        try {
            participants = countRoomParticipants(roomId);
        } catch (RequestFailedCommandException ex) {
            if ("no-access".equals(ex.getCode()) && "not-available".equals(ex.getSubCode())) {
                logger.warn("Can't get number of room participants! Skipping capacity check for room " + roomId + ". This may be normal behavior.");
                return;
            } else {
                throw ex;
            }
        }

        try {
            room = getRoom(roomId);
        } catch (RequestFailedCommandException ex) {
            if ("no-data".equals(ex.getCode())) {
                logger.warn("Can't get room capacity! Skipping capacity check for room " + roomId + ". This may be normal behavior.");
                return;
            } else {
                throw ex;
            }
        }

        if (room == null) {
            logger.warn("Can't get room info (room ID: " + roomId + "), skipping capacity check.");
            return;
        }

        logger.debug("Checking capacity for room " + roomId + " with capacity " + room.getLicenseCount() + " and " + participants + " participants.");
        if (participants > room.getLicenseCount()) {
            logger.warn("Capacity has been exceeded in room " + room.getName() + " (room ID: " + roomId + ").");

            NotifyTarget notifyTarget = new NotifyTarget(Service.NotifyTargetType.ROOM_OWNERS, roomId);
            notifyTarget.addMessage("en",
                    "Room capacity exceeded: " + room.getName(),
                    "Capacity has been exceeded in your room \"" + room.getName() + "\".\n\n"
                    + "Booked licence count: " + room.getLicenseCount() + "\n"
                    + "Number of connected participants: " + participants + "\n\n"
                    + "Please use only the booked license count and disconnect participants who exceed it.");
            notifyTarget.addMessage("cs",
                    "Kapacita místnosti překročena: " + room.getName(),
                    "Kapacita vaší místnosti \"" + room.getName() + "\" byla překročena.\n\n"
                            + "Počet zarezervovaných licencí: " + room.getLicenseCount() + "\n"
                            + "Počet připojených účastníků: " + participants + "\n\n"
                            + "Prosíme dodržujte zarezervovaný počet licencí a odpojte účastníky, kteří jej překračují.");

            performControllerAction(notifyTarget);
        }
    }

    /**
     * Returns number of participants.
     * @param roomId sco-id
     * @return number of participants
     */
    protected int countRoomParticipants(String roomId) throws CommandException
    {
        RequestAttributeList scoInfoAttributes = new RequestAttributeList();
        scoInfoAttributes.add("sco-id",roomId);

        Element response = execApi("meeting-usermanager-user-list", scoInfoAttributes);

        return response.getChild("meeting-usermanager-user-list").getChildren("userdetails").size();
    }

    /**
     * Logout of the server, clearing the session as well.
     */
    public void logout() throws CommandException
    {
        execApi("logout", null);
        this.connectionSession = null;
    }

    /**
     * Execute command on Adobe Connect server and returns XML response. Throws CommandException when some error on Adobe Connect server occured or some parser error occured.
     *
     * @param action     name of Adobe Connect action
     * @param attributes attributes of action
     * @return XML action response
     * @throws CommandException
     */
    protected Element execApi(String action, RequestAttributeList attributes) throws CommandException
    {
        try {
            if (this.connectionSession == null) {
                if (action.equals("logout")) {
                    return null;
                }
                else {
                    login();
                }
            }

            String actionUrl = getActionUrl(action, attributes);
            logger.debug(String.format("Calling action %s on %s", actionUrl, deviceAddress));

            int retryCount = 5;
            while (retryCount > 0) {
                // Read result from url
                Document result;
                try {
                    // Read result
                    InputStream resultStream = execApi(actionUrl, requestTimeout);
                    result = new SAXBuilder().build(resultStream);
                }
                catch (IOException exception) {
                    if (isRequestApiRetryPossible(exception)) {
                        retryCount--;
                        logger.warn("{}: Trying again...", exception.getMessage());
                        continue;
                    }
                    else {
                        throw exception;
                    }
                }

                // Check for error and reconnect if login is needed
                if (isError(result)) {
                    if (isLoginNeeded(result)) {
                        retryCount--;
                        logger.debug(String.format("Reconnecting to server %s", deviceAddress));
                        this.connectionState = ConnectionState.RECONNECTING;
                        connectionSession = null;
                        login();
                        continue;
                    }
                    throw new RequestFailedCommandException(actionUrl, result);
                }
                else {
                    logger.debug(String.format("Command %s succeeded on %s", action, deviceAddress));
                    return result.getRootElement();
                }
            }
            throw new CommandException(String.format("Command %s failed.", action));
        }
        catch (IOException e) {
            throw new RuntimeException("Command issuing error", e);
        }
        catch (JDOMParseException e) {
            throw new RuntimeException("Command result parsing error", e);
        }
        catch (JDOMException e) {
            throw new RuntimeException("Error initializing parser", e);
        }
        catch (RequestFailedCommandException exception) {
            logger.warn(String.format("Command %s has failed on %s: %s", action, deviceAddress, exception));
            throw exception;
        }
    }

    /**
     * Execute command on Adobe Connect server and returns {@link InputStream} with response.
     *
     * @param actionUrl
     * @param timeout
     * @return response in {@link InputStream}
     * @throws IOException
     */
    protected InputStream execApi(String actionUrl, int timeout) throws IOException
    {
        // Send request
        URL url = new URL(actionUrl);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        connection.setRequestProperty("Cookie", "BREEZESESSION=" + this.connectionSession);
        connection.connect();
        return connection.getInputStream();
    }

    /**
     * @param exception
     * @return true whether given {@code exception} allows to retry the API request, false otherwise
     */
    protected boolean isRequestApiRetryPossible(Exception exception)
    {
        Throwable cause = exception.getCause();
        return (cause instanceof SocketException && cause.getMessage().equals("Connection reset"));
    }

    /**
     * @param result Document returned by AC API call
     * @return true if the given {@code result} represents and error,
     *         false otherwise
     */
    private boolean isError(Document result)
    {
        return isError(result.getRootElement());
    }

    /**
     * @param response Element returned by AC API call
     * @return true if the given {@code result} represents and error,
     *         false otherwise
     */
    private boolean isError(Element response)
    {
        Element status = response.getChild("status");
        if (status != null) {
            String code = status.getAttributeValue("code");
            if ("ok".equals(code)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param result XML result to parse for error
     * @return true if the given {@code result} saying that login is needed,
     *         false otherwise
     */
    private boolean isLoginNeeded(Document result)
    {
        Element status = result.getRootElement().getChild("status");
        if (status != null) {
            String code = status.getAttributeValue("code");
            if ("no-access".equals(code)) {
                String subCode = status.getAttributeValue("subcode");
                if ("no-login".equals(subCode)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void main(String[] args) throws Exception
    {
        try {
            /* Testovaci AC server */
            String server = "tconn.cesnet.cz";

            AdobeConnectConnector acc = new AdobeConnectConnector();
            DeviceAddress deviceAddress = new DeviceAddress(server, 443);

            acc.connect(deviceAddress, "admin", "<password>");

            /************************/


            /************************/

            acc.disconnect();

        }
        catch (ExceptionInInitializerError exception) {
            logger.error("Cannot initialize adobe connect", exception);
        }
    }

    /**
     * Returns if permission view or view-hidden is set for public-access.
     *
     * @param scoId
     * @return if SCO is public
     */
    public boolean isSCOPublic(String scoId) throws CommandException
    {
        RequestAttributeList permissionsAttributes = new RequestAttributeList();
        permissionsAttributes.add("acl-id", scoId);
        permissionsAttributes.add("filter-principal-id", "public-access");

        String permissionId = execApi("permissions-info", permissionsAttributes).getChild("permissions").getChild("principal").getAttributeValue("permission-id");
        AdobeConnectPermissions permission = AdobeConnectPermissions.valueByCode(permissionId);
        return AdobeConnectPermissions.PUBLIC.equals(permission) || AdobeConnectPermissions.VIEW.equals(permission) || AdobeConnectPermissions.VIEW_ONLY.equals(permission);
    }

    public static class RequestFailedCommandException extends CommandException
    {
        private String requestUrl;

        private Document requestResult;

        public RequestFailedCommandException(String requestUrl, Document requestResult)
        {
            this.requestUrl = requestUrl;
            this.requestResult = requestResult;
        }

        @Override
        public String getMessage()
        {
            return String.format("%s. URL: %s", getError(), requestUrl);
        }

        public String getError()
        {
            Element status = requestResult.getRootElement().getChild("status");

            List<Attribute> statusAttributes = status.getAttributes();
            StringBuilder errorMsg = new StringBuilder();
            for (Attribute attribute : statusAttributes) {
                String attributeName = attribute.getName();
                String attributeValue = attribute.getValue();
                errorMsg.append(" ");
                errorMsg.append(attributeName);
                errorMsg.append(": ");
                errorMsg.append(attributeValue);

            }

            String code = status.getAttributeValue("code");
            if (status.getChild(code) != null) {
                List<Attribute> childAttributes = status.getChild(code).getAttributes();
                for (Attribute attribute : childAttributes) {
                    errorMsg.append(", ");
                    errorMsg.append(attribute.getName());
                    errorMsg.append(": ");
                    errorMsg.append(attribute.getValue());
                }
            }
            return errorMsg.toString();
        }

        public String getCode()
        {
            Element status = requestResult.getRootElement().getChild("status");
            return status.getAttributeValue("code");
        }

        public String getSubCode()
        {
            Element status = requestResult.getRootElement().getChild("status");
            return status.getAttributeValue("subcode");
        }

        public Element getRequestResult()
        {
            return this.requestResult.getRootElement();
        }
    }

    /*public static class RequestAttributeList extends LinkedList<Entry>
    {
        public void add(String key, String value) throws CommandException
        {
            if (!add(new Entry(key, value))) {
                throw new CommandException("Failed to add attribute to attribute list.");
            }
        }

        public String getValue(String key)
        {
            for (Entry entry : this)
            {
                if (entry.getKey().equals(key))
                {
                    return entry.getValue();
                }
            }

            return null;
        }

        public Entry getEntry(String key)
        {
            for (Entry entry : this)
            {
                if (entry.getKey().equals(key))
                {
                    return entry;
                }
            }

            return null;
        }
    }

    public static class Entry
    {
        private String key;
        private String value;

        public Entry(String key,String value)
        {
            setKey(key);
            setValue(value);
        }

        public String getKey()
        {
            return key;
        }

        public void setKey(String key)
        {
            this.key = key;
        }

        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }
    }*/
}
