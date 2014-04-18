package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.*;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.connector.api.*;
import cz.cesnet.shongo.controller.RecordingUnavailableException;
import cz.cesnet.shongo.controller.api.jade.GetRecordingFolderId;
import cz.cesnet.shongo.controller.api.jade.NotifyTarget;
import cz.cesnet.shongo.controller.api.jade.Service;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
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
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link AbstractConnector} for Adobe Connect.
 *
 * @author opicak <pavelka@cesnet.cz>
 */
public class AdobeConnectConnector extends AbstractMultipointConnector implements RecordingService
{
    private static Logger logger = LoggerFactory.getLogger(AdobeConnectConnector.class);

    /**
     * Options for the {@link AdobeConnectConnector}.
     */
    public static final String URL_PATH_EXTRACTION_FROM_URI = "url-path-extraction-from-uri";

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
    protected String meetingsFolderID;

    /**
     * Name of folder for recordings
     */
    protected String recordingsFolderName;

    /**
     * Root folder ID for meetings
     */
    protected String recordingsFolderID;

    /**
     * This is the user log in name, typically the user email address.
     */
    private String login;

    /**
     * The password of the user.
     */
    private String password;

    /**
     * Timeout for {@link #request}.
     */
    private int requestTimeout;

    /**
     * The Java session ID that is generated upon successful login.  All calls
     * except login must provide this ID for authentication.
     */
    private String breezesession;

    /**
     * If capacity check is running.
     */
    private volatile boolean capacityChecking = false;


    /**
     * If recording check is running.
     */
    private volatile boolean recordingChecking = false;

    /**
     * Timeout for checking room capacity, default value is 5 minutes
     */
    private final long CAPACITY_CHECK_TIMEOUT = Duration.standardMinutes(5).getMillis();

    /**
     * Timeout for checking if recording are in right folder, default value is 5 minutes
     */
    private final long RECORDING_CHECK_TIMEOUT = Duration.standardMinutes(5).getMillis();

    /**
     * Small timeout used between some AC request
     */
    private final long AC_REQUEST_DELAY = 100;

    /*
     * @param serverUrl     the base URL of the Breeze server, including the
     *                      trailing slash http://www.breeze.example/ is a typical
     *                      example.  Most Breeze installations will not need any
     *                      path except that of the host.
     * @param login         the login of the user as whom the adapter will act on
     *                      the Breeze system.  This is often an administrator but
     *                      Breeze will properly apply permissions for any user.
     * @param password      The password of the user who's logging in.
     * @param breezesession The Java session ID created by the Breeze server upon
     *                      successful login.
     *
    public AdobeConnectConnector()
    {
        this.serverUrl = serverUrl;
        this.login = login;
        this.password = password;
        this.breezesession = breezesession;
    }*/

    @java.lang.Override
    public void connect(Address address, String username, String password) throws CommandException
    {
        this.info.setDeviceAddress(address);
        this.login = username;
        this.password = password;

        // Setup options
        this.requestTimeout = (int) getOptionDuration(OPTION_TIMEOUT, DEFAULT_TIMEOUT).getMillis();
        this.urlPathExtractionFromUri = getOptionPattern(URL_PATH_EXTRACTION_FROM_URI);
        this.recordingsFolderName = getOption("recordings-folder-name");
        this.meetingsFolderName = getOption("meetings-folder-name");

        this.login();

        this.info.setConnectionState(ConnectorInfo.ConnectionState.LOOSELY_CONNECTED);
    }

    /**
     * Returns true if state is set to LOOSELY_CONNECTED, false otherwise.
     *
     * @return boolean for connection state
     */
    public boolean isConnected()
    {
        return (this.info.getConnectionState() == ConnectorInfo.ConnectionState.LOOSELY_CONNECTED);
    }

    @java.lang.Override
    public void disconnect() throws CommandException
    {
        this.info.setConnectionState(ConnectorInfo.ConnectionState.DISCONNECTED);
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
        Element principalList = request("principal-list", userSearchAttributes);

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

        Element response = request("principal-update", newUserAttributes);
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
        Element sessionsResponse = request("report-meeting-sessions", sessionsAttributes);

        if (sessionsResponse.getChild("report-meeting-sessions").getChildren().size() == 0) {
            return;
        }

        RequestAttributeList endMeetingAttributes = new RequestAttributeList();
        endMeetingAttributes.add("sco-id", roomId);
        endMeetingAttributes.add("state", state.toString());

        // Not working on connect.cesnet.cz
        /*if (message != null) {
            // Replace all sequences of " " and "." by single space
            message = message.replaceAll("[ \\.]+", " ");
            try {
                endMeetingAttributes.add("message",URLEncoder.encode(message,"UTF8"));
            }
            catch (UnsupportedEncodingException e) {
                throw new CommandException("Error while message encoding.", e);
            }
        }*/

        if (redirect == true && url != null) {
            endMeetingAttributes.add("redirect",redirect.toString());
            endMeetingAttributes.add("url",url);
        }

        try {
            logger.debug("{} meeting (sco-ID: {}) session.", (state ? "Starting" : "Ending"), roomId);
            request("meeting-roommanager-endmeeting-update", endMeetingAttributes);
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
        String message = "The room is currently unavailable for joining / Do mistnosti se aktualne neni mozne pripojit";

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
    protected AdobeConnectAccessMode getRoomAccessMode(String roomId) throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("acl-id",roomId);
        attributes.add("filter-principal-id","public-access");

        String accessMode = request("permissions-info", attributes).getChild("permissions").getChild("principal").getAttributeValue(
                "permissions-id");

        AdobeConnectAccessMode adobeConnectAccessMode = AdobeConnectAccessMode.PROTECTED;

        if (AdobeConnectAccessMode.PRIVATE.getPermissionId().equals(accessMode)) {
            adobeConnectAccessMode = AdobeConnectAccessMode.PRIVATE;
        }
        else if (AdobeConnectAccessMode.PROTECTED.getPermissionId().equals(accessMode)) {
            adobeConnectAccessMode = AdobeConnectAccessMode.PROTECTED;
        }
        else if (AdobeConnectAccessMode.PUBLIC.getPermissionId().equals(accessMode)) {
            adobeConnectAccessMode = AdobeConnectAccessMode.PUBLIC;
        }

        return adobeConnectAccessMode;
    }

    /**
     * Set room access mode (public, protected, private). Mode in AC v8.*, v9.0, v9.1 are "view-hidden" (public), "remove" (protected), "denied" (private)
     * Default access mode (when param mode is null) is AdobeConnectAccessMode.PROTECTED
     *
     * @param roomId
     * @param mode
     */
    protected void setRoomAccessMode(String roomId, AdobeConnectAccessMode mode) throws CommandException
    {
        RequestAttributeList accessModeAttributes = new RequestAttributeList();
        accessModeAttributes.add("acl-id",roomId);
        accessModeAttributes.add("principal-id","public-access");
        if (mode == null) {
            accessModeAttributes.add("permission-id",AdobeConnectAccessMode.PROTECTED.getPermissionId());
        } else {
            accessModeAttributes.add("permission-id",mode.getPermissionId());
        }

        request("permissions-update", accessModeAttributes);
    }

    /**
     * Make room public (Anyone who has the URL for the meeting can enter the ro
     om).
     * @param roomId
     */
    public void makeRoomPublic(String roomId) throws CommandException
    {
        setRoomAccessMode(roomId, AdobeConnectAccessMode.PUBLIC);
    }

    /**
     * Make room protected (Only registered users and accepted guests can enter the room).
     * @param roomId
     */
    public void makeRoomProtected(String roomId) throws CommandException
    {
        setRoomAccessMode(roomId,AdobeConnectAccessMode.PROTECTED);
    }

    /**
     * Make room private (Only registered users and participants can enter).
     * @param roomId
     */
    public void makeRoomPrivate(String roomId) throws CommandException
    {
        setRoomAccessMode(roomId,AdobeConnectAccessMode.PRIVATE);
    }

    /**
     * This method is not supported, cause the AC XML API (secret one) is not working
     *
     * @throws cz.cesnet.shongo.api.jade.CommandUnsupportedException
     *
     */
    @java.lang.Override
    public void muteParticipant(String roomId, String roomParticipantId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(
                "Adobe Connect does not support this function. This setting is accessible in Adobe Connect virtual room.");
    }

    /**
     * This method is not supported, cause the AC XML API (secret one) is not working
     *
     * @throws CommandUnsupportedException
     */
    @java.lang.Override
    public void unmuteParticipant(String roomId, String roomParticipantId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(
                "Adobe Connect does not support this function. This setting is accessible in Adobe Connect virtual room.");
    }

    @java.lang.Override
    public void setParticipantMicrophoneLevel(String roomId, String roomParticipantId, int level)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(
                "Adobe Connect does not support changing microphone level. This setting is accessible in Adobe Connect virtual room.");
    }

    @java.lang.Override
    public void setParticipantPlaybackLevel(String roomId, String roomParticipantId, int level)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support changing playback level.");
    }

    @java.lang.Override
    public void enableParticipantVideo(String roomId, String roomParticipantId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(
                "Adobe Connect does not support this function. This setting is accessible in Adobe Connect virtual room.");
    }

    @java.lang.Override
    public void disableParticipantVideo(String roomId, String roomParticipantId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(
                "Adobe Connect does not support this function. This setting is accessible in Adobe Connect virtual room.");
    }

    @java.lang.Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException, CommandUnsupportedException
    {
        return null;  //TODO
    }

    @java.lang.Override
    public UsageStats getUsageStats() throws CommandException, CommandUnsupportedException
    {
        //report-bulk-consolidated-transactions
        //report-meeting....
        return null;  //TODO
    }

    @Override
    public String createRecordingFolder(RecordingFolder recordingFolder) throws CommandException
    {
        String recordingFolderName = recordingFolder.getName();
        String suffix = "";
        Integer index = 0;
        while (true) {
            try {
                RequestAttributeList attributes = new RequestAttributeList();
                attributes.add("sco-id", getRecordingsFolderID());
                attributes.add("filter-name", URLEncoder.encode(recordingFolderName + " " + suffix, "UTF8"));


                Element recFolders = request("sco-contents", attributes);
                if (recFolders.getChild("scos").getChildren().size() == 0) {
                    recordingFolderName = URLEncoder.encode(recordingFolderName + " " + suffix, "UTF8");
                    break;
                }

                index = index + 1;
                suffix = index.toString();
            }
            catch (UnsupportedEncodingException e) {
                throw new CommandException("Error while message encoding.", e);
            }
        }

        RequestAttributeList folderAttributes = new RequestAttributeList();
        folderAttributes.add("folder-id", getRecordingsFolderID());
        folderAttributes.add("name", recordingFolderName);
        folderAttributes.add("type", "folder");

        Element folder = request("sco-update", folderAttributes);
        String recordingId = folder.getChild("sco").getAttributeValue("sco-id");

        if (recordingFolder.getUserPermissions().size() > 0) {
            recordingFolder.setId(recordingId);
            modifyRecordingFolder(recordingFolder);
        }

        return recordingId;
    }

    @Override
    public void modifyRecordingFolder(RecordingFolder recordingFolder) throws CommandException
    {
        resetPermissions(recordingFolder.getId());

        RequestAttributeList userAttributes = new RequestAttributeList();
        userAttributes.add("acl-id",recordingFolder.getId());

        for (Map.Entry<String, RecordingFolder.UserPermission> userPermissions : recordingFolder.getUserPermissions().entrySet()) {
            UserInformation userInformation = getUserInformationById(userPermissions.getKey());

            // Configure all principal names for participant
            Set<String> principalNames = userInformation.getPrincipalNames();
            if (principalNames.size() == 0) {
                throw new CommandException("User " + userInformation.getFullName() + " has no principal names.");
            }
            for (String principalName : principalNames) {
                String userId = createAdobeConnectUser(principalName, userInformation);
                String role = "denied";

                switch (userPermissions.getValue()) {
                    case READ:
                        role = "view";
                        break;
                    case WRITE:
                        role = "manage";
                        break;
                }
                userAttributes.add("principal-id", userId);
                userAttributes.add("permission-id", role);
                logger.debug("Setting permissions '{}' for recordings folder '{}' for user '{}' (principal-id: {}).",
                        new Object[]{userPermissions.getValue(), recordingFolder.getId(), userInformation.getFullName(), userId});
            }

        }

        request("permissions-update",userAttributes);
    }

    @Override
    public void deleteRecordingFolder(String recordingFolderId) throws CommandException
    {
        deleteSCO(recordingFolderId);
    }

    @Override
    public Collection<Recording> listRecordings(String recordingFolderId) throws CommandException
    {
        ArrayList<Recording> recordingList = new ArrayList<Recording>();

        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", recordingFolderId);
        attributes.add("filter-icon", "archive");
        attributes.add("filter-out-date-end","null");

        Element response = request("sco-contents", attributes);
        for (Element resultRecording : response.getChild("scos").getChildren()) {
            recordingList.add(extractRecording(resultRecording));
        }
        return Collections.unmodifiableList(recordingList);
    }

    @Override
    public Recording getRecording(String recordingId) throws CommandException
    {
        Element resultRecording = getScoInfo(recordingId);
        return extractRecording(resultRecording);
    }

    /**
     * Returns active recording for room with alias, if any
     * @param roomId
     * @return Recording info, or null, if no active recording
     * @throws CommandException
     */
    protected Recording getActiveRecording(String roomId) throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", roomId);
        attributes.add("filter-icon", "archive");
        attributes.add("filter-date-end", "null");

        Element response = request("sco-contents", attributes);

        Element resultRecording = response.getChild("scos").getChild("sco");
        if (resultRecording == null) {
            return null;
        }

        return extractRecording(resultRecording);
    }

    @Override
    public Recording getActiveRecording(Alias alias) throws CommandException
    {
        String path = getLastPathSegmentFromURI(alias.getValue());
        String scoId = getScoByUrl(path);

        return getActiveRecording(scoId);
    }

    @Override
    public boolean isRecordingActive(String recordingId) throws CommandException
    {
        return getScoInfo(recordingId).getChild("date-end") == null;
    }

    @Override
    public String startRecording(String folderId, Alias alias, RecordingSettings recordingSettings)
            throws CommandException
    {
        String recordingName;
        try {
            recordingName = URLEncoder.encode("[flr:" + folderId + "] " + DateTimeFormat.forStyle("SM").print(DateTime.now()),"UTF8");
        }
        catch (UnsupportedEncodingException e) {
            throw new CommandException("Error while URL encoding.", e);
        }

        RequestAttributeList attributes = new RequestAttributeList();

        String path = getLastPathSegmentFromURI(alias.getValue());
        String scoId = getScoByUrl(path);
        attributes.add("sco-id", scoId);
        attributes.add("active", "true");
        attributes.add("name",recordingName);

        // throw exception if recording is not ready = no participants in the room
        try {
            request("meeting-recorder-activity-update", attributes);
        } catch (RequestFailedCommandException ex) {
            if ("no-access".equals(ex.getCode()) && "not-available".equals(ex.getSubCode())) {
                throw new RecordingUnavailableException("Recording is not available now.");
            }

            if ("internal-error".equals(ex.getCode())) {
                logger.warn("AC internal error thrown during starting recording (meeting is probably just starting).");
                throw new RecordingUnavailableException("Recording is not available now.");
            }

            throw ex;
        }

        RequestAttributeList recAttributes = new RequestAttributeList();
        recAttributes.add("sco-id", scoId);

        Element response;
        int count = 0;
        while (true) {
            try {
                Thread.sleep(AC_REQUEST_DELAY);
            }
            catch (InterruptedException e) {
                logger.debug("unexpected wakening, but nothing to worry about");
            }
            response = request("meeting-recorder-activity-info", recAttributes);

            if (response.getChild("meeting-recorder-activity-info").getChildText("recording-sco-id") != null) {
                break;
            }

            count++;
            logger.debug("Failed to get recording id for " + count + ". times. It probably meens, that recording didn't start.");

            if (count > 4) {
                throw new CommandException("Cannot get recording id for.");
            }
        }

        return response.getChild("meeting-recorder-activity-info").getChildText("recording-sco-id");
    }

    @Override
    public void stopRecording(String recordingId) throws CommandException
    {
        Element recordingInfo = getScoInfo(recordingId);
        String roomId = recordingInfo.getAttributeValue("folder-id");

        // Get identifier of recording folder
        String recordingFolderId;
        String recordingName = recordingInfo.getChildText("name");
        Pattern pattern = Pattern.compile("\\[[^:]+:(\\d+)\\]");
        Matcher matcher = pattern.matcher(recordingName);
        if (matcher.find()) {
            // Get recording folder id from matcher
            recordingFolderId = matcher.group(1);
        }
        else {
            // Get recording folder id from controller
            recordingFolderId = (String) performControllerAction(new GetRecordingFolderId(roomId));
            if (recordingFolderId == null) {
                throw new CommandException("FolderId from GetRecordingFolderId was null.");
            }
        }

        // Stop recording
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", roomId);
        attributes.add("active", "false");
        request("meeting-recorder-activity-update", attributes);

        // Move recording
        moveRecording(recordingId,recordingFolderId);
        cachedMovedRecordings.add(recordingId);
    }

    @Override
    public void deleteRecording(String recordingId) throws CommandException
    {
        deleteSCO(recordingId);
    }

    private synchronized void moveRecording(String recordingId, String recordingFolderId) throws CommandException
    {
        if (isRecordingStored(recordingId,recordingFolderId)) {
            return;
        }

        RequestAttributeList moveAttributes = new RequestAttributeList();
        moveAttributes.add("sco-id", recordingId);
        moveAttributes.add("folder-id", recordingFolderId);

        logger.info("Moving recording (id: " + recordingId + ") to folder (id: " + recordingFolderId + ")");
        request("sco-move", moveAttributes);
    }

    /**
     * Cache of {@link UserInformation} by shongo-user-id.
     */
    private List<String> cachedMovedRecordings = new ArrayList<String>();

    public boolean isRecordingStored(String recordingId) throws CommandException
    {
        return isRecordingStored(recordingId, null);
    }

    /**
     * Check if recording is stored in dedicated folder (given or system if null), or still in room. Logs warning, if recording is stored in another folder, then it is supposed to.
     *
     * @param recordingId
     * @return
     * @throws CommandException
     */
    public boolean isRecordingStored(String recordingId, String recordingFolderId) throws CommandException
    {
        if (cachedMovedRecordings.contains(recordingId)) {
            return true;
        }

        Element recording = getScoInfo(recordingId);
        String folderId = recording.getAttributeValue("folder-id");

        if (recordingFolderId != null) {
            if (folderId.equals(recordingFolderId)) {
                return true;
            } else {
                return false;
            }
        }


        RequestAttributeList recFoldersAttributes = new RequestAttributeList();
        recFoldersAttributes.add("sco-id", getRecordingsFolderID());

        List<Element> recFolders = request("sco-contents",recFoldersAttributes).getChild("scos").getChildren();

        for (Element recFolder : recFolders) {
            if (recFolder.getAttributeValue("sco-id").equals(folderId)) {
                cachedMovedRecordings.add(recordingId);
                return true;
            }
        }

        Element folder = getScoInfo(folderId);
        if ("folder".equals(folder.getAttributeValue("type"))) {
            logger.warn("Recording is stored in wrong folder (outside folder " + recordingsFolderName + "): " + folder.getChildText("name"));
            return true;
        } else {
            return false;
        }
    }

    @java.lang.Override
    public MediaData getRoomContent(String roomId) throws CommandException, CommandUnsupportedException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", roomId);

        Element response = request("sco-contents", attributes);

        for (Element child : response.getChild("scos").getChildren("sco")) {
            //TODO: archive all
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public void addRoomContent(String roomId, String name, MediaData data)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function.");
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public void removeRoomContentFile(String roomId, String name) throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", roomId);
        attributes.add("filter-name", name);

        Element response = request("sco-contents", attributes);

        if (response.getChild("scos").getChild("sco") != null) {
            deleteSCO(response.getChild("scos").getChild("sco").getAttributeValue("sco-id"));
        }
    }

    @java.lang.Override
    public void clearRoomContent(String roomId) throws CommandException, CommandUnsupportedException
    {
        // TODO: erase content and re-create room?
    }

    @java.lang.Override
    public Collection<RoomSummary> getRoomList() throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("filter-type", "meeting");

        Element response = request("report-bulk-objects", attributes);

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

    @java.lang.Override
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

            String uri = "https://" + info.getDeviceAddress().getHost() + ":" + info.getDeviceAddress().getPort() +
                    sco.getChildText("url-path");
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
        catch (RequestFailedCommandException ex) {
            if ("no-data".equals(ex.getCode())) {
                throw new CommandException("Room (sco-id: " + roomId + ") does not exist.", ex);
            }
        }
        return room;
    }

    private void setRoomAttributes(RequestAttributeList attributes, Room room)
            throws CommandException
    {
        try {
            // Set the description
            if (room.getDescription() != null) {
                attributes.add("description", URLEncoder.encode(room.getDescription(), "UTF8"));
            }

            // Set capacity
            attributes.add("sco-tag", String.valueOf(room.getLicenseCount()));


            // Create/Update aliases
            if (room.getAliases() != null) {
                for (Alias alias : room.getAliases()) {
                    switch (alias.getType()) {
                        case ROOM_NAME:
                            attributes.add("name", URLEncoder.encode(alias.getValue(), "UTF8"));
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
        catch (UnsupportedEncodingException ex) {
            throw new CommandException("Error while URL encoding.", ex);
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

        request("permissions-update", userAttributes);
    }

    /**
     * Return permissions of the SCO.
     *
     * @param ScoId identifier of the room
     * @return XML Element from API call "permissions-info"
     * @throws CommandException
     */
    protected Element getSCOPermissions(String ScoId) throws CommandException
    {
        RequestAttributeList permissionsAttributes = new RequestAttributeList();
        permissionsAttributes.add("acl-id", ScoId);
        permissionsAttributes.add("filter-out-permission-id", "null");

        return request("permissions-info", permissionsAttributes);
    }

    /**
     * Set parent (meetings) permissions for the recordings.
     *
     * @param roomId identifier of the room
     * @param permissions XML Element from API call "permissions-info"
     * @throws CommandException
     */
    protected void setRecordingPermissionsAsMeetings(String roomId, Element permissions) throws CommandException
    {
        RequestAttributeList userAttributes = new RequestAttributeList();

        for (Recording recording : listRecordings(roomId))
        {
            userAttributes.add("acl-id", recording.getId());

            resetPermissions(recording.getId());

            for (Element principal : permissions.getChild("permissions").getChildren("principal")) {
                String principalId = principal.getAttributeValue("principal-id");
                userAttributes.add("principal-id", principalId);
                if ("host".equals(principal.getAttributeValue("permission-id"))) {
                    userAttributes.add("permission-id", "manage");
                }
                else { //TODO: zatim se ale nepropaguji
                    userAttributes.add("permisson-id", "publish");
                }
            }

            if (userAttributes.getValue("principal-id") == null) {
                return;
            }

            logger.debug("Setting permissions for recording '{}' (sco ID: '{}').", recording.getName(), recording.getId());
            request("permissions-update", userAttributes);
        }
    }

    /**
     * Set parent (meetings) permissions for the recordings.
     *
     * @param roomId identifier of the room
     * @throws CommandException
     */
    protected void setRecordingPermissionsAsMeetings(String roomId) throws CommandException
    {
        setRecordingPermissionsAsMeetings(roomId, getSCOPermissions(roomId));
    }

    protected void resetPermissions(String roomId) throws CommandException
    {
        RequestAttributeList permissionsResetAttributes = new RequestAttributeList();
        permissionsResetAttributes.add("acl-id", roomId);

        request("permissions-reset", permissionsResetAttributes);
    }

    @java.lang.Override
    public String createRoom(Room room) throws CommandException
    {
        try {
            RequestAttributeList attributes = new RequestAttributeList();
            attributes.add("folder-id",
                    (this.meetingsFolderID != null ? this.meetingsFolderID : this.getMeetingsFolderID()));
            attributes.add("type", "meeting");
            attributes.add("date-begin", URLEncoder.encode(DateTime.now().toString(), "UTF8"));

            // Set room attributes
            setRoomAttributes(attributes, room);

            // Room name must be filled
            if (attributes.getValue("name") == null) {
                throw new RuntimeException("Room name must be filled for the new room.");
            }

            Element response = request("sco-update", attributes);
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
            AdobeConnectAccessMode accessMode = null;
            AdobeConnectRoomSetting adobeConnectRoomSetting = room.getRoomSetting(AdobeConnectRoomSetting.class);
            if (adobeConnectRoomSetting != null) {
                pin = adobeConnectRoomSetting.getPin() == null ? "" : adobeConnectRoomSetting.getPin();
                accessMode = adobeConnectRoomSetting.getAccessMode();
            }

            // Set pin for room if set
            passcodeAttributes.add("value",pin);
            request("acl-field-update",passcodeAttributes);

            // Set room access mode, when null setRoomAccessMode set default value {@link AdobeConnectAccessMode.PROTECTED}
            setRoomAccessMode(roomId,accessMode);

            //importRoomSettings(response.getChild("sco").getAttributeValue("sco-id"),room.getConfiguration());

            return roomId;

        }
        catch (UnsupportedEncodingException ex) {
            throw new CommandException("Error while encoding date: ", ex);
        }
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
        String oldRoomUrl = oldRoom.getAlias(AliasType.ADOBE_CONNECT_URI).getValue();
        return !getLastPathSegmentFromURI(oldRoomUrl).equalsIgnoreCase(newRoomUrl);
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
            //TODO: set permisions for recordings
            resetPermissions(roomId);
            startMeeting(roomId);
            addRoomParticipants(roomId, room.getParticipantRoles());
        }
        else if (room.getLicenseCount() == 0) {
            setRecordingPermissionsAsMeetings(roomId);
            resetPermissions(roomId);
            endMeeting(roomId);
        }

        request("sco-update", attributes);

        // Set passcode (pin), since Adobe Connect 9.0
        RequestAttributeList passcodeAttributes = new RequestAttributeList();
        passcodeAttributes.add("acl-id",roomId);
        passcodeAttributes.add("field-id","meeting-passcode");

        String pin = "";
        AdobeConnectAccessMode accessMode = null;
        AdobeConnectRoomSetting adobeConnectRoomSetting = room.getRoomSetting(AdobeConnectRoomSetting.class);
        if (adobeConnectRoomSetting != null) {
            pin = adobeConnectRoomSetting.getPin() == null ? "" : adobeConnectRoomSetting.getPin();
            accessMode = adobeConnectRoomSetting.getAccessMode();
        }

        // Set pin for room if set
        passcodeAttributes.add("value",pin);
        request("acl-field-update",passcodeAttributes);

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

            String newRoomUrl = newRoom.getAlias(AliasType.ADOBE_CONNECT_URI).getValue();
            String msg = "Room has been modified, you have been redirected to the new one (" + newRoomUrl + ").";
            endMeeting(oldRoomId, URLEncoder.encode(msg, "UTF8"), true, URLEncoder.encode(newRoomUrl, "UTF8"));
        }
        catch (UnsupportedEncodingException ex) {
            deleteRoom(newRoomId);
            throw new CommandException("Error while encoding URL. ", ex);
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
        String[] uriArray = uri.split("/");

        return (uriArray.length > 1 ? uriArray[uriArray.length - 1] : null);
    }

    @java.lang.Override
    public void deleteRoom(String roomId) throws CommandException
    {
        endMeeting(roomId);

        // Backup content
        // Recordings
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", roomId);
        attributes.add("filter-icon", "archive");
        List<Element> recordings = request("sco-contents", attributes).getChild("scos").getChildren();

        for(Element recording : recordings) {
            String recordingFolderId = (String) performControllerAction(new GetRecordingFolderId(roomId));
            moveRecording(recording.getAttributeValue("sco-id"),recordingFolderId);
        }

        deleteSCO(roomId);
    }

    @java.lang.Override
    public String exportRoomSettings(String roomId) throws CommandException
    {
        Element scoInfo = getScoInfo(roomId);
        Document document = scoInfo.getDocument();

        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        String xmlString = outputter.outputString(document);

        return xmlString;
    }

    @java.lang.Override
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

        request("sco-update", attributes);
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
            Element userResponse = request("principal-list", userAttributes);

            if (userResponse.getChild("principal-list").getChild("principal") == null) {
                return null;
            }

            userPrincipalName = userResponse.getChild("principal-list").getChild("principal").getChildText("login");
            cachedPrincipalNameByPrincipalId.put(userPrincipalId, userPrincipalName);
        }
        return userPrincipalName;
    }

    @java.lang.Override
    public Collection<RoomParticipant> listRoomParticipants(String roomId) throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", roomId);

        ArrayList<RoomParticipant> participantList = new ArrayList<RoomParticipant>();

        Element response;
        try {
            response = request("meeting-usermanager-user-list", attributes);
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

    @java.lang.Override
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

    @java.lang.Override
    public void modifyRoomParticipant(RoomParticipant roomParticipant) throws CommandUnsupportedException
    {
        throw new TodoImplementException("Modify participant " + roomParticipant);
    }

    @java.lang.Override
    public String dialRoomParticipant(String roomId, Alias alias) throws CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function.");
    }

    @java.lang.Override
    public void disconnectRoomParticipant(String roomId, String roomParticipantId)
            throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", roomId);
        attributes.add("user-id", roomParticipantId);

        request("meeting-usermanager-remove-user", attributes);
    }

    @java.lang.Override
    public void enableContentProvider(String roomId, String roomParticipantId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function. Use user role instead.");
    }

    @java.lang.Override
    public void disableContentProvider(String roomId, String roomParticipantId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function. Use user role instead.");
    }

    private Recording extractRecording(Element resultRecording)
    {
        Recording recording = new Recording();
        recording.setId(resultRecording.getAttributeValue("sco-id"));
        recording.setRecordingFolderId(resultRecording.getAttributeValue("folder-id"));
        recording.setName(resultRecording.getChildText("name"));

        String description = resultRecording.getChildText("description");
        recording.setDescription(description == null ? "" : description);

        String dateBegin = resultRecording.getChildText("date-begin");
        recording.setBeginDate(DateTime.parse(dateBegin));

        String dateEnd = resultRecording.getChildText("date-end");
        if (dateEnd != null) {
            recording.setDuration(new Interval(DateTime.parse(dateBegin), DateTime.parse(dateEnd)).toPeriod());
        }

        String baseUrl = "https://" + info.getDeviceAddress().getHost() + ":" + info.getDeviceAddress().getPort()
                + resultRecording.getChildText("url-path");

        recording.setFileName(resultRecording.getChildText("name"));
        recording.setViewUrl(baseUrl);
        if (dateEnd != null) {
            recording.setEditUrl(baseUrl + "?pbMode=edit");
        }

        return recording;
    }

    protected Element getScoInfo(String scoId) throws CommandException
    {
        try {
            RequestAttributeList attributes = new RequestAttributeList();
            attributes.add("sco-id", scoId);

            return request("sco-info", attributes).getChild("sco");
        } catch (RequestFailedCommandException ex) {
            if ("no-access".equals(ex.getCode()) && "denied".equals(ex.getSubCode())) {
                throw new CommandException("SCO-ID '" + scoId + "' doesn't exist.",ex);
            } else {
                throw ex;
            }
        }
    }

    protected String getScoByUrl(String url) throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("url-path",url);

        return request("sco-by-url",attributes).getChild("sco").getAttributeValue("sco-id");
    }

    protected void deleteSCO(String scoId) throws CommandException
    {
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", scoId);

        request("sco-delete", attributes);
    }

    /**
     * Retrieves the appropriate Breeze URL.
     *
     * @param action    the action to perform
     * @param attributes the map os parameters for the action
     * @return the URL to perform the action
     */
    protected URL breezeUrl(String action, RequestAttributeList attributes) throws IOException, CommandException
    {
        if (action == null || action.isEmpty()) {
            throw new CommandException("Action of AC call cannot be empty.");
        }

        String queryString = "";

        if (attributes != null) {
            for (Entry entry : attributes) {
                queryString += '&' + entry.getKey() + '=' + entry.getValue();
            }
        }

        return new URL("https://" + info.getDeviceAddress().getHost() + ":" + info.getDeviceAddress()
                .getPort() + "/api/xml?" + "action=" + action
                + queryString);
    }

    protected String getBreezesession() throws Exception
    {

        if (breezesession == null) {
            login();
        }
        return breezesession;
    }

    /**
     * Sets and returns SCO-ID of folder for meetings.
     *
     * @return meeting folder SCO-ID
     * @throws CommandException
     */
    protected String getMeetingsFolderID() throws CommandException
    {
        if (meetingsFolderID == null) {
            Element response = request("sco-shortcuts", null);
            for (Element sco : response.getChild("shortcuts").getChildren("sco")) {
                if ("meetings".equals(sco.getAttributeValue("type"))) {
                    // Find sco-id of meetings folder
                    RequestAttributeList searchAttributes = new RequestAttributeList();
                    searchAttributes.add("sco-id", sco.getAttributeValue("sco-id"));
                    searchAttributes.add("filter-is-folder", "1");

                    Element shongoFolder = request("sco-contents", searchAttributes);

                    for (Element folder : shongoFolder.getChild("scos").getChildren("sco")) {
                        if (meetingsFolderName.equals(folder.getChildText("name"))) {
                            meetingsFolderID = folder.getAttributeValue("sco-id");
                        }
                    }

                    // Creates meetings folder if not exists
                    if (meetingsFolderID == null) {
                        logger.debug("Folder /" + meetingsFolderName + " for shongo meetings does not exists, creating...");

                        RequestAttributeList folderAttributes = new RequestAttributeList();
                        folderAttributes.add("folder-id",
                                sco.getAttributeValue("sco-id"));
                        folderAttributes.add("name", meetingsFolderName);
                        folderAttributes.add("type", "folder");

                        Element folder = request("sco-update", folderAttributes);

                        meetingsFolderID = folder.getChild("sco").getAttributeValue("sco-id");

                        logger.debug("Folder /" + meetingsFolderName + " for meetings created with sco-id: " + meetingsFolderID);
                    }

                    break;
                }
            }
        }

        return meetingsFolderID;
    }

    /**
     * Sets and returns SCO-ID of folder for recordings.
     *
     * @return recordings folder SCO-ID
     * @throws CommandException
     */
    protected String getRecordingsFolderID() throws CommandException
    {
        if (recordingsFolderID == null) {
            Element response = request("sco-shortcuts", null);
            for (Element sco : response.getChild("shortcuts").getChildren("sco")) {
                if ("content".equals(sco.getAttributeValue("type"))) {
                    // Find sco-id of recordings-folder folder
                    RequestAttributeList searchAttributes = new RequestAttributeList();
                    searchAttributes.add("sco-id", sco.getAttributeValue("sco-id"));
                    searchAttributes.add("filter-is-folder", "1");

                    Element shongoFolder = request("sco-contents", searchAttributes);

                    for (Element folder : shongoFolder.getChild("scos").getChildren("sco")) {
                        if (recordingsFolderName.equals(folder.getChildText("name"))) {
                            recordingsFolderID = folder.getAttributeValue("sco-id");
                        }
                    }

                    // Creates recording folder if not exists
                    if (recordingsFolderID == null) {
                        logger.debug("Folder /" + recordingsFolderName + " for shongo meetings does not exists, creating...");

                        RequestAttributeList folderAttributes = new RequestAttributeList();
                        folderAttributes.add("folder-id", sco.getAttributeValue("sco-id"));
                        folderAttributes.add("name", recordingsFolderName);
                        folderAttributes.add("type", "folder");

                        Element folder = request("sco-update", folderAttributes);

                        recordingsFolderID = folder.getChild("sco").getAttributeValue("sco-id");

                        logger.debug("Folder /" + recordingsFolderName + " for meetings created with sco-id: " + recordingsFolderID);
                    }

                    break;
                }
            }
        }

        // Check if permission for this folder is denied, or sets it
        RequestAttributeList permissionsInfoAttributes = new RequestAttributeList();
        permissionsInfoAttributes.add("acl-id",recordingsFolderID);
        permissionsInfoAttributes.add("filter-principal-id","public-access");

        String permissions = request("permissions-info",permissionsInfoAttributes).getChild("permissions").getChild("principal").getAttributeValue(
                "permission-id");

        if (!"denied".equals(permissions)) {
            RequestAttributeList permissionsUpdateAttributes = new RequestAttributeList();
            permissionsUpdateAttributes.add("acl-id", recordingsFolderID);
            permissionsUpdateAttributes.add("principal-id", "public-access");
            permissionsUpdateAttributes.add("permission-id", "denied");

            request("permissions-update", permissionsUpdateAttributes);
        }

        return recordingsFolderID;
    }

    /**
     * Performs the action to log into Adobe Connect server. Stores the breezeseession ID.
     */
    protected void login() throws CommandException
    {
        if (this.breezesession != null) {
            logout();
        }

        ConfiguredSSLContext.getInstance().addAdditionalCertificates(info.getDeviceAddress().getHost());

        RequestAttributeList loginAtributes = new RequestAttributeList();
        loginAtributes.add("login", this.login);
        loginAtributes.add("password", this.password);

        URLConnection conn;
        try {
            URL loginUrl = breezeUrl("login", loginAtributes);
            conn = loginUrl.openConnection();
            conn.connect();

            InputStream resultStream = conn.getInputStream();
            Document doc = new SAXBuilder().build(resultStream);

            if (this.isError(doc)) {
                throw new CommandException("Login to server " + info.getDeviceAddress() + " failed");
            }
            else {
                logger.debug(String.format("Login to server %s succeeded", info.getDeviceAddress()));
            }
        }
        catch (Exception exception) {
            throw new CommandException(exception.getMessage(), exception);
        }

        String breezesessionString = conn.getHeaderField("Set-Cookie");

        StringTokenizer st = new StringTokenizer(breezesessionString, "=");
        String sessionName = null;

        if (st.countTokens() > 1) {
            sessionName = st.nextToken();
        }

        if (sessionName != null &&
                (sessionName.equals("JSESSIONID") ||
                         sessionName.equals("BREEZESESSION"))) {

            String breezesessionNext = st.nextToken();
            int semiIndex = breezesessionNext.indexOf(';');
            this.breezesession = breezesessionNext.substring(0, semiIndex);


            this.meetingsFolderID = this.getMeetingsFolderID();
            this.recordingsFolderID = this.getRecordingsFolderID();
        }


        if (breezesession == null) {
            throw new CommandException("Could not log in to Adobe Connect server: " + info.getDeviceAddress());
        }

        Thread capacityCheckThread = new Thread() {
            private Logger logger = LoggerFactory.getLogger(AdobeConnectConnector.class);

            @Override
            public void run()
            {
                setCapacityChecking(true);
                logger.info("Checking of rooms capacity - starting...");

                while (isConnected()) {
                    try {
                        Thread.sleep(CAPACITY_CHECK_TIMEOUT);
                    } catch (InterruptedException e) {
                       Thread.currentThread().interrupt();
                       continue;
                    }

                    try {
                        checkAllRoomsCapacity();
                    } catch (Exception exception) {
                        logger.warn("Capacity check failed", exception);
                    }
                }

                setCapacityChecking(false);
            }
        };

        Thread moveRecordingThread = new Thread() {
            private Logger logger = LoggerFactory.getLogger(AdobeConnectConnector.class);

            @Override
            public void run()
            {
                setRecordingChecking(true);
                logger.info("Checking of recordings - starting...");

                while (isConnected()) {
                    try {
                        Thread.sleep(RECORDING_CHECK_TIMEOUT);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        continue;
                    }

                    try {
                        checkRecordings();
                    } catch (Exception exception) {
                        logger.warn("Checking location of recording failed", exception);
                    }
                }

                setRecordingChecking(false);
            }
        };

        synchronized (this) {
            if (!this.capacityChecking) {
                capacityCheckThread.start();
            }
        }

        synchronized (this) {
            if (!this.recordingChecking) {
                moveRecordingThread.start();
            }
        }
    }

    private synchronized void setCapacityChecking(boolean value)
    {
        this.capacityChecking = value;
    }

    private synchronized void setRecordingChecking(boolean value)
    {
        this.recordingChecking = value;
    }

    /**
     *
     * @throws CommandException
     */
    protected void checkAllRoomsCapacity() throws CommandException
    {
        Element response = request("report-active-meetings",new RequestAttributeList());

        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", getMeetingsFolderID());
        attributes.add("type", "meeting");
        Element shongoRoomsElement = request("sco-contents",attributes);

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
                    + "Licence count: " + room.getLicenseCount() + "\n"
                    + "Number of participants: " + participants + "\n\n");
            notifyTarget.addMessage("cs",
                    "Kapacita místnosti překročena: " + room.getName(),
                    "Kapacita vaší místnosti \"" + room.getName() + "\" byla překročena.\n\n"
                            + "Počet licencí: " + room.getLicenseCount() + "\n"
                            + "Počet účastníků: " + participants + "\n\n");

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

        Element response = request("meeting-usermanager-user-list", scoInfoAttributes);

        return response.getChild("meeting-usermanager-user-list").getChildren("userdetails").size();
    }

    /**
     * Check if all recordings are stored, otherwise move them to appropriate folder (asks controller for folder name)
     *
     * @throws CommandException
     */
    protected void checkRecordings() throws CommandException
    {
        RequestAttributeList recordingsAttributes = new RequestAttributeList();
        // choose only recordings
        recordingsAttributes.add("filter-icon","archive");
        // filter out all recordings in progress
        recordingsAttributes.add("filter-out-date-end","null");

        List<Element> recordings = request("report-bulk-objects", recordingsAttributes).getChild("report-bulk-objects").getChildren();

        List<String> allStoredRecordings = new ArrayList<String>();

        for (Element recording : recordings) {
            String recordingId = recording.getAttributeValue("sco-id");
            String folderId = getScoInfo(recordingId).getAttributeValue("folder-id");

            // Get all shongo meetings
            RequestAttributeList attributes = new RequestAttributeList();
            attributes.add("sco-id",getMeetingsFolderID());
            attributes.add("type", "meeting");
            Element shongoRoomsElement = request("sco-contents",attributes);

            List<String> shongoRooms = new ArrayList<String>();
            for (Element sco : shongoRoomsElement.getChild("scos").getChildren()) {
                shongoRooms.add(sco.getAttributeValue("sco-id"));
            }

            // Skip all non-shongo rooms
            if (!shongoRooms.contains(folderId)) {
                //logger.debug("There is recording for non-shongo room");
                continue;
            }

            // Move if not stored yet
            if (isRecordingStored(recordingId)) {
                allStoredRecordings.add(recordingId);
            } else {
                Element folder = getScoInfo(folderId);

                if ("meeting".equals(folder.getAttributeValue("type"))) {
                    String destinationId = (String) performControllerAction(new GetRecordingFolderId(folderId));
                    if (destinationId == null) {
                        throw new CommandException("FolderId from GetRecordingFolderId was null.");
                    }

                    moveRecording(recordingId, destinationId);
                    cachedMovedRecordings.add(recordingId);
                    allStoredRecordings.add(recordingId);

                    continue;
                }

                logger.warn("Recording " + recording.getChildText("name") + " (id: " + recording
                        .getAttributeValue("sco-id") + ") for shongo room was not stored or found in any meeting.");
            }
        }

        // Retain only existing stored recordings
        cachedMovedRecordings.retainAll(allStoredRecordings);
    }

    /**
     * Logout of the server, clearing the session as well.
     */
    public void logout() throws CommandException
    {
        request("logout", null);
        this.breezesession = null;
    }

    /**
     * Execute command on Adobe Connect server and returns XML response. Throws CommandException when some error on Adobe Connect server occured or some parser error occured.
     *
     * @param action     name of Adobe Connect action
     * @param attributes atrtributes of action
     * @return XML action response
     * @throws CommandException
     */
    protected Element request(String action, RequestAttributeList attributes)
            throws CommandException
    {
        try {
            if (this.breezesession == null) {
                if (action.equals("logout")) {
                    return null;
                }
                else {
                    login();
                }
            }

            URL url = breezeUrl(action, attributes);
            logger.debug(String.format("Calling action %s on %s", url, info.getDeviceAddress()));

            while (true) {
                // Send request
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(requestTimeout);
                conn.setRequestProperty("Cookie", "BREEZESESSION=" + this.breezesession);
                conn.connect();

                // Read result
                InputStream resultStream = conn.getInputStream();
                Document result = new SAXBuilder().build(resultStream);

                // Check for error and reconnect if login is needed
                if (isError(result)) {
                    if (isLoginNeeded(result)) {
                        logger.debug(String.format("Reconnecting to server %s", info.getDeviceAddress()));
                        this.info.setConnectionState(ConnectorInfo.ConnectionState.RECONNECTING);

                        breezesession = null;
                        login();
                        continue;
                    }
                    throw new RequestFailedCommandException(url, result);
                }
                else {
                    logger.debug(String.format("Command %s succeeded on %s", action, info.getDeviceAddress()));
                    return result.getRootElement();
                }
            }
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
            logger.warn(String.format("Command %s has failed on %s: %s", action, info.getDeviceAddress(), exception));
            throw exception;
        }
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
            String server = "actest-w3.cesnet.cz";

            AdobeConnectConnector acc = new AdobeConnectConnector();
            Address address = new Address(server, 443);

            acc.connect(address, "admin", "cip9skovi3t2");

            /************************/


            /************************/

            acc.disconnect();

        }
        catch (ExceptionInInitializerError exception) {
            logger.error("Cannot initialize adobe connect", exception);
        }
    }

    public static class RequestFailedCommandException extends CommandException
    {
        private URL requestUrl;

        private Document requestResult;

        public RequestFailedCommandException(URL requestUrl, Document requestResult)
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
    }

    public class RequestAttributeList extends LinkedList<Entry>
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
    
    public class Entry
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
    }
}
