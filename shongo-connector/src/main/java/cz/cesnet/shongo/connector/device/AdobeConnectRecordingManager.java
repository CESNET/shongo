package cz.cesnet.shongo.connector.device;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.Recording;
import cz.cesnet.shongo.api.RecordingFolder;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.ConnectorConfiguration;
import cz.cesnet.shongo.connector.api.RecordingSettings;
import cz.cesnet.shongo.controller.RecordingUnavailableException;
import cz.cesnet.shongo.controller.RoomNotExistsException;
import cz.cesnet.shongo.controller.api.jade.GetRecordingFolderId;
import org.jdom2.Element;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link cz.cesnet.shongo.connector.common.AbstractConnector} for Adobe Connect.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AdobeConnectRecordingManager
{
    private static Logger logger = LoggerFactory.getLogger(AdobeConnectRecordingManager.class);

    /**
     * Pattern for recording names (with _[])
     */
    private final Pattern RECORDING_NAME_PATTERN = Pattern.compile("(.*)_([0-9]+)$");

    /**
     * Pattern for recording file name.
     */
    private final Pattern RECORDING_FILE_NAME_PATTERN =
            Pattern.compile("([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z)");

    /**
     * Date/time format for recording name.
     */
    private static final DateTimeFormatter RECORDING_NAME_DATETIME_FORMATTER =
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZoneUTC();

    /**
     * @see AdobeConnectConnector
     */
    private AdobeConnectConnector connector;

    /**
     * Timeout for checking if recording are in right folder, default value is 5 minutes
     */
    private final int recordingsCheckTimeout;

    /**
     * Prefix for recordings.
     */
    private final String recordingsPrefix;

    /**
     * Name of folder for recordings
     */
    protected String recordingsFolderName;

    /**
     * Root folder ID for meetings
     */
    protected String recordingsFolderId;

    /**
     * Cache of moved recording-ids.
     */
    private List<String> cachedMovedRecordings = new ArrayList<String>();

    /**
     * Constructor.
     *
     * @param connector sets the {@link #connector}
     */
    public AdobeConnectRecordingManager(final AdobeConnectConnector connector) throws CommandException
    {
        ConnectorConfiguration connectorConfiguration = connector.getConfiguration();
        this.connector = connector;
        this.recordingsCheckTimeout = (int) connectorConfiguration.getOptionDuration(
                AdobeConnectConnector.RECORDINGS_CHECK_PERIOD,
                AdobeConnectConnector.RECORDINGS_CHECK_PERIOD_DEFAULT).getMillis();
        this.recordingsPrefix = connectorConfiguration.getOptionString(
                AdobeConnectConnector.RECORDINGS_PREFIX, "");
        this.recordingsFolderName = connectorConfiguration.getOptionString(
                AdobeConnectConnector.RECORDINGS_FOLDER_NAME);
        this.recordingsFolderId = getRecordingsFolderId();

        Thread moveRecordingThread = new Thread()
        {
            private Logger logger = LoggerFactory.getLogger(AdobeConnectConnector.class);

            @Override
            public void run()
            {
                logger.info("Checking of recordings - starting...");
                while (connector.isConnected()) {
                    try {
                        Thread.sleep(recordingsCheckTimeout);
                    }
                    catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        continue;
                    }

                    try {
                        checkRecordings();
                    }
                    catch (Exception exception) {
                        logger.warn("Checking location of recording failed", exception);
                    }
                }
                logger.info("Checking of recordings - exiting...");
            }
        };
        moveRecordingThread.setName(Thread.currentThread().getName() + "-recordings");
        moveRecordingThread.start();
    }

    /**
     * Sets and returns SCO-ID of folder for recordings.
     *
     * @return recordings folder SCO-ID
     * @throws cz.cesnet.shongo.api.jade.CommandException
     */
    private String getRecordingsFolderId() throws CommandException
    {
        if (recordingsFolderId == null) {
            Element response = connector.request("sco-shortcuts", null);
            for (Element sco : response.getChild("shortcuts").getChildren("sco")) {
                if ("content".equals(sco.getAttributeValue("type"))) {
                    // Find sco-id of recordings-folder folder
                    AdobeConnectConnector.RequestAttributeList searchAttributes = new AdobeConnectConnector.RequestAttributeList();
                    searchAttributes.add("sco-id", sco.getAttributeValue("sco-id"));
                    searchAttributes.add("filter-is-folder", "1");

                    Element shongoFolder = connector.request("sco-contents", searchAttributes);
                    for (Element folder : shongoFolder.getChild("scos").getChildren("sco")) {
                        if (recordingsFolderName.equals(folder.getChildText("name"))) {
                            recordingsFolderId = folder.getAttributeValue("sco-id");
                        }
                    }

                    // Creates recording folder if not exists
                    if (recordingsFolderId == null) {
                        logger.debug("Folder /{} for shongo meetings does not exists, creating...",
                                recordingsFolderName);

                        AdobeConnectConnector.RequestAttributeList folderAttributes = new AdobeConnectConnector.RequestAttributeList();
                        folderAttributes.add("folder-id", sco.getAttributeValue("sco-id"));
                        folderAttributes.add("name", recordingsFolderName);
                        folderAttributes.add("type", "folder");

                        Element folder = connector.request("sco-update", folderAttributes);
                        recordingsFolderId = folder.getChild("sco").getAttributeValue("sco-id");
                        logger.debug("Folder /{} for meetings created with sco-id: {}",
                                recordingsFolderName, recordingsFolderId);
                    }

                    break;
                }
            }
        }

        // Check if permission for this folder is denied, or sets it
        AdobeConnectConnector.RequestAttributeList permissionsInfoAttributes = new AdobeConnectConnector.RequestAttributeList();
        permissionsInfoAttributes.add("acl-id", recordingsFolderId);
        permissionsInfoAttributes.add("filter-principal-id", "public-access");

        String permissions = connector.request("permissions-info", permissionsInfoAttributes).getChild("permissions")
                .getChild("principal").getAttributeValue(
                        "permission-id");

        if (!"denied".equals(permissions)) {
            AdobeConnectConnector.RequestAttributeList permissionsUpdateAttributes = new AdobeConnectConnector.RequestAttributeList();
            permissionsUpdateAttributes.add("acl-id", recordingsFolderId);
            permissionsUpdateAttributes.add("principal-id", "public-access");
            permissionsUpdateAttributes.add("permission-id", "denied");

            connector.request("permissions-update", permissionsUpdateAttributes);
        }

        return recordingsFolderId;
    }

    /**
     * @see cz.cesnet.shongo.connector.api.RecordingService#createRecordingFolder
     */
    public String createRecordingFolder(RecordingFolder recordingFolder) throws CommandException
    {
        String recordingFolderName = recordingFolder.getName();
        String suffix = "";
        Integer index = 0;
        while (true) {
            try {
                AdobeConnectConnector.RequestAttributeList attributes = new AdobeConnectConnector.RequestAttributeList();
                attributes.add("sco-id", getRecordingsFolderId());
                attributes.add("filter-name", URLEncoder.encode(recordingFolderName + " " + suffix, "UTF8"));
                Element recFolders = connector.request("sco-contents", attributes);
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

        AdobeConnectConnector.RequestAttributeList folderAttributes = new AdobeConnectConnector.RequestAttributeList();
        folderAttributes.add("folder-id", getRecordingsFolderId());
        folderAttributes.add("name", recordingFolderName);
        folderAttributes.add("type", "folder");

        Element folder = connector.request("sco-update", folderAttributes);
        String recordingId = folder.getChild("sco").getAttributeValue("sco-id");

        if (recordingFolder.getUserPermissions().size() > 0) {
            recordingFolder.setId(recordingId);
            modifyRecordingFolder(recordingFolder);
        }

        return recordingId;
    }

    /**
     * @see cz.cesnet.shongo.connector.api.RecordingService#modifyRecordingFolder
     */
    public void modifyRecordingFolder(RecordingFolder recordingFolder) throws CommandException
    {
        connector.resetPermissions(recordingFolder.getId());

        AdobeConnectConnector.RequestAttributeList userAttributes = new AdobeConnectConnector.RequestAttributeList();
        userAttributes.add("acl-id", recordingFolder.getId());

        for (Map.Entry<String, RecordingFolder.UserPermission> userPermissions : recordingFolder.getUserPermissions()
                .entrySet()) {
            UserInformation userInformation = connector.getUserInformationById(userPermissions.getKey());

            // Configure all principal names for participant
            Set<String> principalNames = userInformation.getPrincipalNames();
            if (principalNames.size() == 0) {
                throw new CommandException("User " + userInformation.getFullName() + " has no principal names.");
            }
            for (String principalName : principalNames) {
                String userId = connector.createAdobeConnectUser(principalName, userInformation);
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
                        new Object[]{userPermissions.getValue(), recordingFolder.getId(), userInformation.getFullName(),
                                userId
                        });
            }

        }

        connector.request("permissions-update", userAttributes);
    }

    /**
     * @see cz.cesnet.shongo.connector.api.RecordingService#deleteRecordingFolder
     */
    public void deleteRecordingFolder(String recordingFolderId) throws CommandException
    {
        connector.deleteSCO(recordingFolderId);
    }

    /**
     * @see cz.cesnet.shongo.connector.api.RecordingService#listRecordings
     */
    public Collection<Recording> listRecordings(String recordingFolderId) throws CommandException
    {
        ArrayList<Recording> recordingList = new ArrayList<Recording>();

        AdobeConnectConnector.RequestAttributeList attributes = new AdobeConnectConnector.RequestAttributeList();
        attributes.add("sco-id", recordingFolderId);
        attributes.add("filter-icon", "archive");
        attributes.add("filter-out-date-end", "null");

        Element response = connector.request("sco-contents", attributes);
        for (Element resultRecording : response.getChild("scos").getChildren()) {
            recordingList.add(parseRecording(resultRecording));
        }
        return Collections.unmodifiableList(recordingList);
    }

    /**
     * @see cz.cesnet.shongo.connector.api.RecordingService#getRecording
     */
    public Recording getRecording(String recordingId) throws CommandException
    {
        Element resultRecording = connector.getScoInfo(recordingId);
        return parseRecording(resultRecording);
    }

    /**
     * @see cz.cesnet.shongo.connector.api.RecordingService#getActiveRecording
     */
    public Recording getActiveRecording(Alias alias) throws CommandException
    {
        String path = connector.getLastPathSegmentFromURI(alias.getValue());
        String scoId = connector.getScoByUrl(path);
        AdobeConnectConnector.RequestAttributeList attributes = new AdobeConnectConnector.RequestAttributeList();
        attributes.add("sco-id", scoId);
        attributes.add("filter-icon", "archive");
        attributes.add("filter-date-end", "null");
        Element response = connector.request("sco-contents", attributes);
        Element resultRecording = response.getChild("scos").getChild("sco");
        if (resultRecording == null) {
            return null;
        }
        return parseRecording(resultRecording);
    }

    /**
     * @see cz.cesnet.shongo.connector.api.RecordingService#isRecordingActive
     */
    public boolean isRecordingActive(String recordingId) throws CommandException
    {
        return connector.getScoInfo(recordingId).getChild("date-end") == null;
    }

    /**
     * @see cz.cesnet.shongo.connector.api.RecordingService#startRecording
     */
    public String startRecording(String folderId, Alias alias, RecordingSettings recordingSettings)
            throws CommandException
    {
        AdobeConnectConnector.RequestAttributeList attributes = new AdobeConnectConnector.RequestAttributeList();

        String recordingName = formatRecordingName(folderId, alias, DateTime.now());
        String path = connector.getLastPathSegmentFromURI(alias.getValue());
        String scoId = connector.getScoByUrl(path);
        attributes.add("sco-id", scoId);
        attributes.add("active", "true");
        attributes.add("name", recordingName);

        // throw exception if recording is not ready = no participants in the room
        try {
            connector.request("meeting-recorder-activity-update", attributes);
        }
        catch (AdobeConnectConnector.RequestFailedCommandException exception) {
            if ("no-access".equals(exception.getCode()) && "not-available".equals(exception.getSubCode())) {
                throw new RecordingUnavailableException("Recording is not available now.");
            }
            if ("internal-error".equals(exception.getCode())) {
                logger.warn("AC internal error thrown during starting recording (meeting is probably just starting).");
                throw new RecordingUnavailableException("Recording is not available now.");
            }

            throw exception;
        }

        AdobeConnectConnector.RequestAttributeList recAttributes = new AdobeConnectConnector.RequestAttributeList();
        recAttributes.add("sco-id", scoId);

        Element response;
        int count = 0;
        while (true) {
            try {
                Thread.sleep(AdobeConnectConnector.REQUEST_DELAY);
            }
            catch (InterruptedException e) {
                logger.debug("unexpected wakening, but nothing to worry about");
            }
            response = connector.request("meeting-recorder-activity-info", recAttributes);

            if (response.getChild("meeting-recorder-activity-info").getChildText("recording-sco-id") != null) {
                break;
            }

            count++;
            logger.debug("Failed to get recording id for " + count +
                    ". times. It probably meens, that recording didn't start.");
            if (count > 4) {
                throw new CommandException("Cannot get recording id for.");
            }
        }

        return response.getChild("meeting-recorder-activity-info").getChildText("recording-sco-id");
    }

    /**
     * @see cz.cesnet.shongo.connector.api.RecordingService#stopRecording
     */
    public void stopRecording(String recordingId) throws CommandException
    {
        Element recordingInfo = connector.getScoInfo(recordingId);
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
            recordingFolderId = (String) connector.performControllerAction(new GetRecordingFolderId(roomId));
            if (recordingFolderId == null) {
                throw new CommandException("FolderId from GetRecordingFolderId was null.");
            }
        }

        // Stop recording
        AdobeConnectConnector.RequestAttributeList attributes = new AdobeConnectConnector.RequestAttributeList();
        attributes.add("sco-id", roomId);
        attributes.add("active", "false");
        connector.request("meeting-recorder-activity-update", attributes);

        // Move recording
        moveRecording(recordingId, recordingFolderId);
        cachedMovedRecordings.add(recordingId);
    }

    /**
     * @see cz.cesnet.shongo.connector.api.RecordingService#deleteRecording
     */
    public void deleteRecording(String recordingId) throws CommandException
    {
        connector.deleteSCO(recordingId);
    }

    /**
     * Set parent (meetings) permissions for the recordings.
     *
     * @param roomId identifier of the room
     * @throws CommandException
     */
    protected void setRecordingPermissionsAsMeetings(String roomId) throws CommandException
    {
        setRecordingPermissionsAsMeetings(roomId, connector.getSCOPermissions(roomId));
    }

    /**
     * Set parent (meetings) permissions for the recordings.
     *
     * @param roomId      identifier of the room
     * @param permissions XML Element from API call "permissions-info"
     * @throws CommandException
     */
    public void setRecordingPermissionsAsMeetings(String roomId, Element permissions) throws CommandException
    {
        AdobeConnectConnector.RequestAttributeList userAttributes = new AdobeConnectConnector.RequestAttributeList();

        for (Recording recording : listRecordings(roomId)) {
            userAttributes.add("acl-id", recording.getId());

            connector.resetPermissions(recording.getId());

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

            logger.debug("Setting permissions for recording '{}' (sco ID: '{}').", recording.getName(),
                    recording.getId());
            connector.request("permissions-update", userAttributes);
        }
    }

    /**
     * Backup room recordings to recording folder (e.g., when room is being deleted).
     *
     * @param roomId
     * @throws CommandException
     */
    public void backupRoomRecordings(String roomId) throws CommandException
    {
        // Recordings
        AdobeConnectConnector.RequestAttributeList attributes = new AdobeConnectConnector.RequestAttributeList();
        attributes.add("sco-id", roomId);
        attributes.add("filter-icon", "archive");
        List<Element> recordings = connector.request("sco-contents", attributes).getChild("scos").getChildren();

        if (recordings.size() > 0) {
            String recordingFolderId = null;
            try {
                recordingFolderId = (String) connector.performControllerAction(new GetRecordingFolderId(roomId));
            }
            catch (CommandException exception) {
                if (RoomNotExistsException.CODE.equals(exception.getCode())) {
                    logger.warn("Cannot get recording folder id while deleting room " + roomId + "...", exception);
                }
                else {
                    throw exception;
                }
            }
            if (recordingFolderId != null) {
                for (Element recording : recordings) {
                    moveRecording(recording.getAttributeValue("sco-id"), recordingFolderId);
                }
            }
        }
    }

    /**
     * @param recordingElement
     * @return {@link Recording} from given {@code resultRecording}
     */
    private Recording parseRecording(Element recordingElement)
    {
        Recording recording = new Recording();
        recording.setId(recordingElement.getAttributeValue("sco-id"));
        recording.setRecordingFolderId(recordingElement.getAttributeValue("folder-id"));

        String recordingName = recordingElement.getChildText("name");
        recording.setName(recordingName);
        Matcher matcher = RECORDING_FILE_NAME_PATTERN.matcher(recordingName);
        if (matcher.find()) {
            recording.setFileName(matcher.group(1));
        }
        else {
            recording.setFileName(recordingName);
        }

        String description = recordingElement.getChildText("description");
        recording.setDescription(description == null ? "" : description);

        String dateBegin = recordingElement.getChildText("date-begin");
        recording.setBeginDate(DateTime.parse(dateBegin));

        String dateEnd = recordingElement.getChildText("date-end");
        if (dateEnd != null) {
            recording.setDuration(new Interval(DateTime.parse(dateBegin), DateTime.parse(dateEnd)).toPeriod());
        }

        DeviceAddress deviceAddress = connector.getDeviceAddress();
        String recordingUrlPath = recordingElement.getChildText("url-path");
        String recordingUrl = "https://" + deviceAddress.getHost() + ":" + deviceAddress.getPort() + recordingUrlPath;
        recording.setViewUrl(recordingUrl);
        if (dateEnd != null) {
            recording.setEditUrl(recordingUrl + "?pbMode=edit");
        }

        return recording;
    }

    /**
     * @param folderId
     * @param alias
     * @return new recording name
     * @throws CommandException
     */
    private String formatRecordingName(String folderId, Alias alias, DateTime dateTime) throws CommandException
    {
        Element recordingFolderElement = connector.getScoInfo(folderId);
        String recordingFolderName = recordingFolderElement.getChild("name").getValue();
        try {
            String recordingName = recordingsPrefix + recordingFolderName
                    + "_" + RECORDING_NAME_DATETIME_FORMATTER.print(dateTime);
            return URLEncoder.encode(recordingName, "UTF8");
        }
        catch (UnsupportedEncodingException e) {
            throw new CommandException("Error while URL encoding.", e);
        }
    }

    /**
     * Add suffix _[0-9] to name or increment it.
     *
     * @param name
     * @return {@code name} with incremented suffix
     */
    private String formatRecordingNameWithIncrementedSuffix(String name)
    {
        Matcher matcher = RECORDING_NAME_PATTERN.matcher(name);
        if (!matcher.find()) {
            return name + "_0";
        }
        else {
            int nextNumber = Integer.parseInt(matcher.group(2)) + 1;
            System.out.println(Integer.parseInt(matcher.group(2)));
            return matcher.group(1) + "_" + nextNumber;
        }
    }

    /**
     * Check if recording is stored in dedicated folder (given or system if null),
     * or still in room. Logs warning, if recording is stored in another folder, then it is supposed to.
     *
     * @param recordingId
     * @return result
     * @throws CommandException
     */
    private boolean isRecordingStored(String recordingId) throws CommandException
    {
        return isRecordingStored(recordingId, null);
    }

    /**
     * Check if recording is stored in dedicated folder (given or system if null),
     * or still in room. Logs warning, if recording is stored in another folder, then it is supposed to.
     *
     * @param recordingId
     * @param recordingFolderId
     * @return result
     * @throws cz.cesnet.shongo.api.jade.CommandException
     */
    private boolean isRecordingStored(String recordingId, String recordingFolderId) throws CommandException
    {
        if (cachedMovedRecordings.contains(recordingId)) {
            return true;
        }

        Element recording = connector.getScoInfo(recordingId);
        String folderId = recording.getAttributeValue("folder-id");

        if (recordingFolderId != null) {
            if (folderId.equals(recordingFolderId)) {
                return true;
            }
            else {
                return false;
            }
        }

        AdobeConnectConnector.RequestAttributeList recFoldersAttributes = new AdobeConnectConnector.RequestAttributeList();
        recFoldersAttributes.add("sco-id", getRecordingsFolderId());
        List<Element> recFolders = connector.request("sco-contents", recFoldersAttributes)
                .getChild("scos").getChildren();
        for (Element recFolder : recFolders) {
            if (recFolder.getAttributeValue("sco-id").equals(folderId)) {
                cachedMovedRecordings.add(recordingId);
                return true;
            }
        }

        Element folder = connector.getScoInfo(folderId);
        if ("folder".equals(folder.getAttributeValue("type"))) {
            logger.warn("Recording is stored in wrong folder (outside folder {}): {}",
                    recordingsFolderName, folder.getChildText("name"));
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * @param recordingId       to be moved
     * @param recordingFolderId where it should be moved
     * @throws CommandException
     */
    private synchronized void moveRecording(String recordingId, String recordingFolderId) throws CommandException
    {
        if (isRecordingStored(recordingId, recordingFolderId)) {
            return;
        }

        AdobeConnectConnector.RequestAttributeList moveAttributes = new AdobeConnectConnector.RequestAttributeList();
        moveAttributes.add("sco-id", recordingId);
        moveAttributes.add("folder-id", recordingFolderId);

        logger.info("Moving recording (id: " + recordingId + ") to folder (id: " + recordingFolderId + ")");
        // Counter for duplicate names
        int i = 0;
        // Move or rename if duplicate name (add "_X")
        while (true) {
            try {
                connector.request("sco-move", moveAttributes);
                break;
            }
            catch (AdobeConnectConnector.RequestFailedCommandException exception) {
                Element status = exception.getRequestResult().getChild("status");
                String code = exception.getCode();
                String subCode = status.getChild("invalid").getAttributeValue("subcode");
                if ("invalid".equals(code) && "duplicate".equals(subCode)) {
                    String field = status.getChild(code).getAttributeValue("field");
                    String name = connector.getScoInfo(recordingId).getChildText(field);
                    connector.renameSco(recordingId, formatRecordingNameWithIncrementedSuffix(name));
                    continue;
                }
                throw exception;
            }
        }
    }

    /**
     * Check if all recordings are stored, otherwise move them to appropriate folder (asks controller for folder name)
     *
     * @throws cz.cesnet.shongo.api.jade.CommandException
     */
    public synchronized void checkRecordings() throws CommandException
    {
        AdobeConnectConnector.RequestAttributeList recordingsAttributes =
                new AdobeConnectConnector.RequestAttributeList();
        // choose only recordings
        recordingsAttributes.add("filter-icon", "archive");
        // filter out all recordings in progress
        recordingsAttributes.add("filter-out-date-end", "null");

        List<Element> recordings = connector.request("report-bulk-objects", recordingsAttributes)
                .getChild("report-bulk-objects").getChildren();

        List<String> allStoredRecordings = new ArrayList<String>();

        for (Element recording : recordings) {
            String recordingId = recording.getAttributeValue("sco-id");
            String folderId = connector.getScoInfo(recordingId).getAttributeValue("folder-id");

            // Get all shongo meetings
            AdobeConnectConnector.RequestAttributeList attributes = new AdobeConnectConnector.RequestAttributeList();
            attributes.add("sco-id", connector.getMeetingsFolderId());
            attributes.add("type", "meeting");
            Element shongoRoomsElement = connector.request("sco-contents", attributes);

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
            }
            else {
                Element folder = connector.getScoInfo(folderId);

                if ("meeting".equals(folder.getAttributeValue("type"))) {
                    String destinationId = (String) connector.performControllerAction(
                            new GetRecordingFolderId(folderId));
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
}
