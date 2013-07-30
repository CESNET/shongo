package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.connector.api.*;
import cz.cesnet.shongo.controller.api.jade.GetUserInformation;
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
public class AdobeConnectConnector extends AbstractConnector implements MultipointService, RecordingService
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
     * Root folder for meetings
     */
    protected String meetingsFolderID;

    /**
     * Root folder for meetings
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
     * The Java session ID that is generated upon successful login.  All calls
     * except login must provide this ID for authentication.
     */
    private String breezesession;

    /**
     * If capacity check is running.
     */
    private volatile boolean capacityChecking = false;

    /**
     * Timeout for checking room capacity, default value is 5 minutes
     */
    private final int CAPACITY_CHECK_TIMEOUT = 5*60*1000;

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
        this.urlPathExtractionFromUri = options.getPattern(URL_PATH_EXTRACTION_FROM_URI);

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
        return (this.info.getConnectionState() == ConnectorInfo.ConnectionState.LOOSELY_CONNECTED ? true :false);
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
    protected String createAdobeConnectUser(UserInformation userInformation) throws CommandException
    {
        String principalName = userInformation.getOriginalId();
        if (principalName == null) {
            throw new CommandException("EPPN is unset for user " + userInformation.getFullName() + ".");
        }

        HashMap<String, String> userSearchAttributes = new HashMap<String, String>();
        userSearchAttributes.put("filter-login", principalName);
        Element principalList = request("principal-list", userSearchAttributes);

        if (principalList.getChild("principal-list").getChild("principal") != null) {
            String principalId = principalList.getChild("principal-list").getChild("principal")
                    .getAttributeValue("principal-id");
            return principalId;
        }

        HashMap<String, String> newUserAttributes = new HashMap<String, String>();
        newUserAttributes.put("first-name", userInformation.getFirstName());
        newUserAttributes.put("last-name", userInformation.getLastName());
        newUserAttributes.put("login", principalName);
        newUserAttributes.put("email", userInformation.getPrimaryEmail());
        newUserAttributes.put("type", "user");
        newUserAttributes.put("has-children", "false");

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
        HashMap<String, String> sessionsAttributes = new HashMap<String, String>();
        sessionsAttributes.put("sco-id", roomId);
        Element sessionsResponse = request("report-meeting-sessions", sessionsAttributes);

        if (sessionsResponse.getChild("report-meeting-sessions").getChildren().size() == 0) {
            return;
        }

        HashMap<String, String> endMeetingAttributes = new HashMap<String, String>();
        endMeetingAttributes.put("sco-id", roomId);
        endMeetingAttributes.put("state", state.toString());
        if (message != null) {
            endMeetingAttributes.put("message",message);
        }
        if (redirect == true && url != null) {
            endMeetingAttributes.put("redirect",redirect.toString());
            endMeetingAttributes.put("url",url);
        }

        try {
            request("meeting-roommanager-endmeeting-update", endMeetingAttributes);
        } catch (CommandException ex) {
            logger.debug("Failed to end/start meeting. Probably just AC error, everything should be working properly.");
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
        String message = "Capacity of the room has ended.";

        endMeeting(roomId, message, false, null);
    }

    /**
     * End current session, set message show after stoping meeting, set url to be redirect (for recreating rooms)
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
     * This method is not supported, cause the AC XML API (secret one) is not working
     *
     * @throws cz.cesnet.shongo.api.jade.CommandUnsupportedException
     *
     */
    @java.lang.Override
    public void muteParticipant(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException
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
    public void unmuteParticipant(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(
                "Adobe Connect does not support this function. This setting is accessible in Adobe Connect virtual room.");
    }

    @java.lang.Override
    public void setParticipantMicrophoneLevel(String roomId, String roomUserId, int level)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(
                "Adobe Connect does not support changing microphone level. This setting is accessible in Adobe Connect virtual room.");
    }

    @java.lang.Override
    public void setParticipantPlaybackLevel(String roomId, String roomUserId, int level)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support changing playback level.");
    }

    @java.lang.Override
    public void enableParticipantVideo(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(
                "Adobe Connect does not support this function. This setting is accessible in Adobe Connect virtual room.");
    }

    @java.lang.Override
    public void disableParticipantVideo(String roomId, String roomUserId)
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

    @java.lang.Override
    public MediaData getReceivedVideoSnapshot(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function.");
    }

    @java.lang.Override
    public MediaData getSentVideoSnapshot(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function.");
    }

    @java.lang.Override
    public String startRecording(String folderId, Alias alias)
            throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", folderId);
        attributes.put("active", "true");

        request("meeting-recorder-activity-update", attributes);

        HashMap<String, String> recAttributes = new HashMap<String, String>();
        recAttributes.put("sco-id", folderId);

        Element response = request("meeting-recorder-activity-info", recAttributes);

        //TODO: return 0 for allready recording - maybe
        return response.getChild("meeting-recorder-activity-info").getChildText("recording-sco-id");
    }

    @java.lang.Override
    public void stopRecording(String recordingId) throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", recordingId);
        attributes.put("active", "false");

        request("meeting-recorder-activity-update", attributes);
    }

    @java.lang.Override
    public Recording getRecording(String recordingId) throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", recordingId);

        Element response = request("sco-info", attributes);

        Recording recording = new Recording();

        String baseUrl = "https://" + info.getDeviceAddress().getHost() + ":" + info.getDeviceAddress().getPort() + response
                .getChild("sco").getChildText("url-path");
        recording.setUrl(baseUrl);
        //TODO: vse ostatni
        //recording.setDownloadableUrl();                 output/filename.zip?download=zip
        recording.setEditableUrl(baseUrl + "?pbMode=edit");

        return recording;
    }

    public Collection<Recording> listRecordings(String folderId) throws CommandException
    {
        //TODO: rozeznat folderId a roomId

        ArrayList<Recording> recordingList = new ArrayList<Recording>();

        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", folderId);
        attributes.put("filter-icon", "archive");

        Element response = request("sco-contents", attributes);

        for (Element resultRecording : response.getChild("scos").getChildren()) {
            if (resultRecording.getChild("date-end") == null) {
                continue;
            }
            Recording recording = new Recording();

            recording.setName(resultRecording.getChildText("name"));

            String baseUrl = "https://" + info.getDeviceAddress().getHost() + ":" + info.getDeviceAddress().getPort()
                    + resultRecording.getChildText("url-path");

            recording.setUrl(baseUrl);
            recording.setEditableUrl(baseUrl + "?pbMode=edit");
            //recording.setDownloadableUrl();                 output/filename.zip?download=zip

            //TODO: vse ostatni

            logger.debug("RECORDING: {}",recording.getName());
            logger.debug("RECORDING-URL: {}",recording.getUrl());
            logger.debug("RECORDING-EditableUrl: {}",recording.getEditableUrl());
            recordingList.add(recording);
        }

        return Collections.unmodifiableList(recordingList);
    }

    @Override
    public void moveRecording(String recordingId, String folderId) throws CommandException, CommandUnsupportedException
    {
        HashMap<String,String> folderAttributes = new HashMap<String, String>();
        folderAttributes.put("folder-id",recordingsFolderID);
        folderAttributes.put("name", folderId);
        folderAttributes.put("type", "folder");

        String folderScoId = request("sco-update",folderAttributes).getChild("sco").getAttributeValue("sco-id");

        HashMap<String,String> moveAttributes = new HashMap<String, String>();
        moveAttributes.put("sco-id",recordingId);
        moveAttributes.put("folder-id",folderScoId);

        request("sco-move",moveAttributes);
    }

    @java.lang.Override
    public void deleteRecording(String recordingId) throws CommandException
    {
        deleteSCO(recordingId);
    }

    @java.lang.Override
    public MediaData getRoomContent(String roomId) throws CommandException, CommandUnsupportedException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", roomId);

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
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", roomId);
        attributes.put("filter-name", name);

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
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("filter-type", "meeting");

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
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", roomId);

        Element response = request("sco-info", attributes);
        Element sco = response.getChild("sco");

        Room room = new Room();
        room.setId(roomId);
        room.setName(sco.getChildText("name"));
        room.setDescription(sco.getChildText("description"));
        if (sco.getChildText("sco-tag") != null) {
            room.setLicenseCount(Integer.valueOf(sco.getChildText("sco-tag")));
        } else {
            throw new CommandException("Licence count not set for room " + roomId + ".");
        }
        room.addTechnology(Technology.ADOBE_CONNECT);

        List<Alias> aliasList = new ArrayList<Alias>();
        String uri = "https://" + info.getDeviceAddress().getHost() + ":" + info.getDeviceAddress().getPort() +
                sco.getChildText("url-path");
        aliasList.add(new Alias(AliasType.ADOBE_CONNECT_URI, uri));

        room.setAliases(aliasList);

        return room;
    }

    private void setRoomAttributes(HashMap<String, String> attributes, Room room)
            throws CommandException
    {
        try {
            // Set the description
            if (room.getDescription() != null) {
                attributes.put("description", URLEncoder.encode(room.getDescription(), "UTF8"));
            }

            // Set capacity
            attributes.put("sco-tag", String.valueOf(room.getLicenseCount()));


            // Create/Update aliases
            if (room.getAliases() != null) {
                for (Alias alias : room.getAliases()) {
                    switch (alias.getType()) {
                        case ROOM_NAME:
                            attributes.put("name", URLEncoder.encode(alias.getValue(), "UTF8"));
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
                            attributes.put("url-path", matcher.group(1));
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

    private void addRoomParticipant(String roomId, UserInformation participant) throws CommandException
    {
        String principalId = this.createAdobeConnectUser(participant);

        //TODO: more user roles
        logger.debug("Configuring participant '{}' (sco ID: '{}') as host in the room.", participant.getFullName(),
                principalId);

        HashMap<String, String> userAttributes = new HashMap<String, String>();
        userAttributes.put("acl-id", roomId);
        userAttributes.put("principal-id", principalId);
        userAttributes.put("permission-id", "host");
        request("permissions-update", userAttributes);
    }

    @java.lang.Override
    public String createRoom(Room room) throws CommandException
    {
        try {
            HashMap<String, String> attributes = new HashMap<String, String>();
            attributes.put("folder-id",
                    (this.meetingsFolderID != null ? this.meetingsFolderID : this.getMeetingsFolderID()));
            attributes.put("type", "meeting");
            attributes.put("date-begin", URLEncoder.encode(DateTime.now().toString(), "UTF8"));

            // Set room attributes
            setRoomAttributes(attributes, room);

            // Room name must be filled
            if (attributes.get("name") == null) {
                throw new RuntimeException("Room name must be filled for the new room.");
            }

            Element response = request("sco-update", attributes);
            String roomId = response.getChild("sco").getAttributeValue("sco-id");

            // Add room participants
            if (room.getLicenseCount() > 0) {
                startMeeting(roomId);
                for (UserInformation participant : room.getParticipants()) {
                    addRoomParticipant(roomId, participant);
                }
            }
            else if (room.getLicenseCount() == 0) {
                endMeeting(roomId);
            }

            //importRoomSettings(response.getChild("sco").getAttributeValue("sco-id"),room.getConfiguration());

            return roomId;

        }
        catch (UnsupportedEncodingException ex) {
            throw new CommandException("Error while encoding date: ", ex);
        }
    }

    @java.lang.Override
    public String modifyRoom(Room room) throws CommandException
    {
        String roomId = room.getId();

        getRoom(roomId);
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", roomId);
        attributes.put("type", "meeting");

        // Set room attributes
        setRoomAttributes(attributes, room);

        // Recreate room if new URL PATH is set
        String oldRoomURI = getRoom(roomId).getAlias(AliasType.ADOBE_CONNECT_URI).getValue();
        if (attributes.get("url-path") != null && !attributes.get("url-path").equalsIgnoreCase(getLastPathSegmentFromURI(oldRoomURI))) {
            return recreateRoom(room);
        }

        // Remove all participants first
        HashMap<String, String> permissionsResetAttributes = new HashMap<String, String>();
        permissionsResetAttributes.put("acl-id", roomId);
        request("permissions-reset", permissionsResetAttributes);

        // Add/modify participants
        if (room.getLicenseCount() > 0) {
            startMeeting(roomId);
            for (UserInformation participant : room.getParticipants()) {
                addRoomParticipant(roomId, participant);
            }
        }
        else if (room.getLicenseCount() == 0) {
            endMeeting(roomId);
        }

        request("sco-update", attributes);
        return roomId;
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

    /**
     * Create new room and while deleting the old one redirect to the new one.
     *
     * Is necessary for changing room URL.
     *
     * @param room new room
     * @return
     */
    public String recreateRoom(Room room) throws CommandException
    {
        try {
            String roomId = room.getId();

            String newRoomId = createRoom(room);

            //TODO: manage creating new Room with same name

            String msg = "Room has been modified, you have been redirected to the new one (" + room.getAlias(AliasType.ADOBE_CONNECT_URI).getValue() + ").";
            endMeeting(roomId, URLEncoder.encode(msg,"UTF8"), true, URLEncoder.encode(room.getAlias(AliasType.ADOBE_CONNECT_URI).getValue(), "UTF8"));

            deleteRoom(roomId);

            return newRoomId;
        } catch (UnsupportedEncodingException ex) {
            throw new CommandException("Error while encoding URL. ", ex);
        }
    }

    @java.lang.Override
    public void deleteRoom(String roomId) throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", roomId);

        this.request("meeting-stop", attributes);

        deleteSCO(roomId);
    }

    @java.lang.Override
    public String exportRoomSettings(String roomId) throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", roomId);

        Element response = request("sco-info", attributes);
        Document document = response.getDocument();

        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        String xmlString = outputter.outputString(document);

        return xmlString;
    }

    @java.lang.Override
    public void importRoomSettings(String roomId, String settings) throws CommandException
    {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document document = null;
        try {
            document = saxBuilder.build(new StringReader(settings));
        }
        catch (Exception exception) {
            throw new CommandException(exception.getMessage(), exception);
        }

        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        String xmlString = outputter.outputString(document);


        HashMap<String, String> attributes = new HashMap<String, String>();

        attributes.put("sco-id", roomId);
//        attributes.put("date-begin", document.getRootElement().getChild("sco").getChild("date-begin").getText());
//        attributes.put("date-end", document.getRootElement().getChild("sco").getChild("date-end").getText());
        if (document.getRootElement().getChild("sco").getChild("description") != null) {
            attributes.put("description", document.getRootElement().getChild("sco").getChild("description").getText());
        }
        attributes.put("url-path", document.getRootElement().getChild("sco").getChild("url-path").getText());
        attributes.put("name", document.getRootElement().getChild("sco").getChild("name").getText());

        request("sco-update", attributes);
    }

    @java.lang.Override
    public String dialParticipant(String roomId, Alias alias) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function.");
    }

    /**
     * Cache of user login (EPPN) by user principal-id.
     */
    private ExpirationMap<String, String> cachedLoginByPrincipalId =
            new ExpirationMap<String, String>(Duration.standardHours(1));

    /**
     * Cache of {@link UserInformation} by user login (EPPN).
     */
    private ExpirationMap<String, UserInformation> cachedUserInformationByLogin =
            new ExpirationMap<String, UserInformation>(Duration.standardHours(1));

    /**
     * @param userPrincipalId principal-id of an user
     * @return user login (EPPN) for given {@code userPrincipalId}
     * @throws CommandException
     */
    public String getUserLoginByPrincipalId(String userPrincipalId) throws CommandException
    {
        String userLogin;
        if (cachedLoginByPrincipalId.contains(userPrincipalId)) {
            logger.debug("Using cached user login by principal-id '{}'...", userPrincipalId);
            userLogin = cachedLoginByPrincipalId.get(userPrincipalId);

        }
        else {
            logger.debug("Fetching user login by principal-id '{}'...", userPrincipalId);
            HashMap<String, String> userAttributes = new HashMap<String, String>();
            userAttributes.put("filter-principal-id", userPrincipalId);
            Element userResponse = request("principal-list", userAttributes);
            userLogin = userResponse.getChild("principal-list").getChild("principal").getChildText("login");
            cachedLoginByPrincipalId.put(userPrincipalId, userLogin);
        }
        return userLogin;
    }

    /**
     * @param userLogin login of an user
     * @return {@link UserInformation} for given {@code userLogin}
     * @throws CommandException
     */
    public UserInformation getUserInformationByLogin(String userLogin) throws CommandException
    {
        UserInformation userInformation;
        if (cachedUserInformationByLogin.contains(userLogin)) {
            logger.debug("Using cached user information by login '{}'...", userLogin);
            userInformation = cachedUserInformationByLogin.get(userLogin);
        }
        else {
            logger.debug("Fetching user information by login '{}'...", userLogin);
            userInformation = (UserInformation) performControllerAction(GetUserInformation.byOriginalId(userLogin));
            cachedUserInformationByLogin.put(userLogin, userInformation);
        }
        return userInformation;
    }

    @java.lang.Override
    public Collection<RoomUser> listParticipants(String roomId) throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", roomId);

        ArrayList<RoomUser> participantList = new ArrayList<RoomUser>();

        Element response = null;
        try {
            response = request("meeting-usermanager-user-list", attributes);
        }
        catch (RequestFailedCommandException exception) {
            // Participant list is not available, so return empty list
            if (exception.getCode().equals("no-access") && exception.getSubCode().equals("not-available")) {
                return participantList;
            }
            // Participant list cannot be retrieved because of internal error
            else if (exception.getCode().equals("internal-error")) {
                logger.debug("Adobe Connect issued internal error while getting meeting participants (UNSAFE API CALL)."
                        + " This should just mean, that there is no participants.", exception);
                return participantList;
            }
            throw exception;
        }

        for (Element userDetails : response.getChild("meeting-usermanager-user-list").getChildren()) {
            RoomUser roomUser = new RoomUser();
            roomUser.setId(userDetails.getChildText("user-id"));
            roomUser.setRoomId(roomId);
            roomUser.setAudioMuted(Boolean.parseBoolean(userDetails.getChildText("mute")));
            roomUser.setDisplayName(userDetails.getChildText("username"));

            String userLogin = getUserLoginByPrincipalId(userDetails.getChildText("principal-id"));
            UserInformation userInformation = getUserInformationByLogin(userLogin);
            if (userInformation != null) {
                roomUser.setUserId(userInformation.getUserId());
            }

            participantList.add(roomUser);
        }

        return Collections.unmodifiableList(participantList);
    }

    @java.lang.Override
    public RoomUser getParticipant(String roomId, String roomUserId) throws CommandException
    {
        Collection<RoomUser> participants = this.listParticipants(roomId);
        for (RoomUser roomUser : participants) {
            if (roomUser.getId().equals(roomUserId)) {
                return roomUser;
            }
        }
        return null;
    }

    @java.lang.Override
    public void modifyParticipant(String roomId, String roomUserId, Map attributes)
            throws CommandException, CommandUnsupportedException
    {
        //TODO
    }

    @java.lang.Override
    public void disconnectParticipant(String roomId, String roomUserId)
            throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", roomId);
        attributes.put("user-id", roomUserId);

        Element response = request("meeting-usermanager-remove-user", attributes);
    }

    @java.lang.Override
    public void enableContentProvider(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function. Use user role instead.");
    }

    @java.lang.Override
    public void disableContentProvider(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function. Use user role instead.");
    }

    protected void deleteSCO(String scoID) throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", scoID);

        request("sco-delete", attributes);
    }

    /**
     * Retrieves the appropriate Breeze URL.
     *
     * @param action    the action to perform
     * @param atributes the map os parameters for the action
     * @return the URL to perform the action
     */
    protected URL breezeUrl(String action, Map<String, String> atributes) throws IOException, CommandException
    {
        if (action == null || action.isEmpty()) {
            throw new CommandException("Action of AC call cannot be empty.");
        }

        String queryString = "";

        if (atributes != null) {
            for (Map.Entry<String, String> entry : atributes.entrySet()) {
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
                if (sco.getAttributeValue("type").equals("meetings")) {
                    // Find sco-id of /shongo folder
                    HashMap<String, String> searchAttributes = new HashMap<String, String>();
                    searchAttributes.put("sco-id", sco.getAttributeValue("sco-id"));
                    searchAttributes.put("filter-is-folder", "1");

                    Element shongoFolder = request("sco-contents", searchAttributes);

                    for (Element folder : shongoFolder.getChild("scos").getChildren("sco")) {
                        if (folder.getChildText("name").equals("shongo")) {
                            meetingsFolderID = folder.getAttributeValue("sco-id");
                        }
                    }

                    // Creates /shongo folder if not exists
                    if (meetingsFolderID == null) {
                        logger.debug("Folder /shongo for shongo meetings does not exists, creating...");

                        HashMap<String, String> folderAttributes = new HashMap<String, String>();
                        folderAttributes.put("folder-id", sco.getAttributeValue("sco-id"));
                        folderAttributes.put("name", "shongo");
                        folderAttributes.put("type", "folder");

                        Element folder = request("sco-update", folderAttributes);

                        meetingsFolderID = folder.getChild("sco").getAttributeValue("sco-id");

                        logger.debug("Folder /shongo for meetings created with sco-id: " + meetingsFolderID);
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
    protected String getRecordingFolderID() throws CommandException
    {
        if (recordingsFolderID == null) {
            Element response = request("sco-shortcuts", null);
            for (Element sco : response.getChild("shortcuts").getChildren("sco")) {
                if (sco.getAttributeValue("type").equals("content")) {
                    // Find sco-id of /shongo folder
                    HashMap<String, String> searchAttributes = new HashMap<String, String>();
                    searchAttributes.put("sco-id", sco.getAttributeValue("sco-id"));
                    searchAttributes.put("filter-is-folder", "1");

                    Element shongoFolder = request("sco-contents", searchAttributes);

                    for (Element folder : shongoFolder.getChild("scos").getChildren("sco")) {
                        if (folder.getChildText("name").equals("shongo-rec")) {
                            recordingsFolderID = folder.getAttributeValue("sco-id");
                        }
                    }

                    // Creates /shongo folder if not exists
                    if (recordingsFolderID == null) {
                        logger.debug("Folder /shongo for shongo meetings does not exists, creating...");

                        HashMap<String, String> folderAttributes = new HashMap<String, String>();
                        folderAttributes.put("folder-id", sco.getAttributeValue("sco-id"));
                        folderAttributes.put("name", "shongo-rec");
                        folderAttributes.put("type", "folder");

                        Element folder = request("sco-update", folderAttributes);

                        recordingsFolderID = folder.getChild("sco").getAttributeValue("sco-id");

                        logger.debug("Folder /shongo-rec for meetings created with sco-id: " + recordingsFolderID);
                    }

                    break;
                }
            }
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

        HashMap<String, String> loginAtributes = new HashMap<String, String>();
        loginAtributes.put("login", this.login);
        loginAtributes.put("password", this.password);

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

        String breezesessionString = (String) (conn.getHeaderField("Set-Cookie"));

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
            this.recordingsFolderID = this.getRecordingFolderID();
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

                while (isConnected()) {
                    try {
                        Thread.sleep(CAPACITY_CHECK_TIMEOUT);
                    } catch (InterruptedException e) {
                       Thread.currentThread().interrupt();
                       continue;
                    }

                    try {
                        checkAllRoomsCapacity();
                    } catch (CommandException e) {
                        logger.warn(String.format("Capacity check failed: " + e));
                        continue;
                    }
                }

                setCapacityChecking(false);
            }
        };

        synchronized (this) {
            if (!this.capacityChecking) {
                capacityCheckThread.start();
            }
        }
    }

    public synchronized void setCapacityChecking(boolean value)
    {
        this.capacityChecking = value;
    }

    /**
     *         threads.add(thread);
     thread.start();

     * @throws CommandException
     */
    protected void checkAllRoomsCapacity() throws CommandException
    {
        HashMap<String,String> attributes = new HashMap<String, String>();
        Element response = request("report-active-meetings",attributes);

        for (Element sco : response.getChild("report-active-meetings").getChildren())
        {
            checkRoomCapacity(sco.getAttributeValue("sco-id"));
        }
    }

    protected void checkRoomCapacity(String roomId) throws CommandException
    {
        Room room = null;
        int participants = 0;

        try {
            participants = countRoomParticipants(roomId);
        } catch (RequestFailedCommandException ex) {
            if (ex.getCode().equals("no-access") && ex.getSubCode().equals("not-available")) {
                logger.warn("Can't get number of room participants! Skipping capacity check for room " + roomId + ". This may be normal behavior.");
                return;
            } else {
                throw ex;
            }
        }

        try {
            room = getRoom(roomId);
        } catch (RequestFailedCommandException ex) {
            if (ex.getCode().equals("no-data")) {
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

        logger.info("Checking capacity for room " + roomId + " with capacity " + room.getLicenseCount() + " and " + participants + " participants.");
        if (participants > room.getLicenseCount()) {
            logger.info("Capacity has been exceeded in room " + room.getName() + " (room ID: " + roomId + ").");

            //TODO: will do controller in future

            String notificationTitle = "Room capacity exceeded / Kapacita místnosti překročena : " + room.getName();
            String notificationMessage = "Capacity has been exceeded in your room \"" + room.getName() + "\".\n\n"
                    + "Licence count: " + room.getLicenseCount() + "\n"
                    + "Number of participants: " + participants + "\n\n"
                    + "===========================================================\n\n"
                    + "Kapacita vaší místnosti \"" + room.getName() + "\" byla překročena.\n\n"
                    + "Počet licencí: " + room.getLicenseCount() + "\n"
                    + "Počet účastníků: " + participants + "\n\n";

            //TODO: list of participants
            performControllerAction(new NotifyTarget(Service.NotifyTargetType.ROOM_OWNERS, roomId, notificationTitle, notificationMessage));
        }
    }

    /**
     * Returns number of participants.
     * @param roomId sco-id
     * @return number of participants
     */
    protected int countRoomParticipants(String roomId) throws CommandException
    {
        HashMap<String,String> scoInfoAttributes = new HashMap<String, String>();
        scoInfoAttributes.put("sco-id",roomId);

        Element response = request("meeting-usermanager-user-list", scoInfoAttributes);

        return response.getChild("meeting-usermanager-user-list").getChildren("userdetails").size();
    }

    //TODO: delete
    /**           DELETE
     * Returns room capacity stored in sco-tag (in sco-info).
     *
     * @param roomId sco-id of the room
     * @return room capacity, value -1 for unlimited or not set
     *
    protected int getRoomCapacity(String roomId) throws CommandException
    {
        HashMap<String,String> scoInfoAttributes = new HashMap<String, String>();
        scoInfoAttributes.put("sco-id",roomId);

        Element response = request("sco-info", scoInfoAttributes);
        if (response.getChild("sco").getChildText("sco-tag") == null) {
            throw new CommandException("Licence count not set for room " + roomId + ".");
        }

        return Integer.parseInt(response.getChild("sco").getChildText("sco-tag"));
    }    */

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
     * @throws RequestFailedCommandException
     */
    protected Element request(String action, Map<String, String> attributes)
            throws CommandException, RequestFailedCommandException
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
                conn.setRequestProperty("Cookie", "BREEZESESSION=" + this.breezesession);
                conn.connect();

                // Read result
                InputStream resultStream = conn.getInputStream();
                Document result = new SAXBuilder().build(resultStream);

                // Check for error and reconnect if login is needed
                if (isError(result)) {
                    if (isLoginNeeded(result)) {
                        logger.debug(String.format("Reconnecting to server %s", info.getDeviceAddress()));
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
            logger.debug(String.format("Command %s has failed on %s: %s", action, info.getDeviceAddress(), exception));
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
            if (code != null && code.equals("ok")) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param result
     * @return true if the given {@code result} saying that login is needed,
     *         false otherwise
     */
    private boolean isLoginNeeded(Document result)
    {
        Element status = result.getRootElement().getChild("status");
        if (status != null) {
            String code = status.getAttributeValue("code");
            if (code != null && code.equals("no-access")) {
                String subCode = status.getAttributeValue("subcode");
                if (subCode != null && subCode.equals("no-login")) {
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

            acc.connect(address, "admin", "******");

            /************************/


            acc.disconnect();

        }
        catch (ExceptionInInitializerError exception) {
            logger.error("Cannot initialize adobe connect", exception);
        }
    }

    /*public static void printInputStream(InputStream inputStream) throws IOException
    {
        System.out.println();
        System.out.println("-- SOURCE DATA --");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        String line = new String();

        while (bufferedReader.ready()) {
            line = bufferedReader.readLine();
            System.out.println(line);
        }
        System.out.println("-- SOURCE DATA --");
        System.out.println();
    }*/

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
}
