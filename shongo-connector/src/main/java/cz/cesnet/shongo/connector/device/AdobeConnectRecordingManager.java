package cz.cesnet.shongo.connector.device;

import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.ConnectorConfiguration;
import cz.cesnet.shongo.connector.api.RecordingSettings;
import cz.cesnet.shongo.connector.common.RequestAttributeList;
import cz.cesnet.shongo.controller.RecordingUnavailableException;
import cz.cesnet.shongo.controller.RoomNotExistsException;
import cz.cesnet.shongo.controller.api.jade.GetRecordingFolderId;
import org.jdom2.Element;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
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
     * Recordings are recognized by icon type
     */
    private final String RECORDING_ICON = "archive";

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
     * Thread for checking recordings.
     */
    private final AtomicReference<Thread> checkRecordingsThreadReference;

    /**
     * Constructor.
     *
     * @param mainConnector sets the {@link #connector}
     */
    public AdobeConnectRecordingManager(final AdobeConnectConnector mainConnector) throws CommandException
    {
        ConnectorConfiguration connectorConfiguration = mainConnector.getConfiguration();
        this.connector = mainConnector;
        this.recordingsCheckTimeout = (int) connectorConfiguration.getOptionDuration(
                AdobeConnectConnector.RECORDINGS_CHECK_PERIOD,
                AdobeConnectConnector.RECORDINGS_CHECK_PERIOD_DEFAULT).getMillis();
        this.recordingsPrefix = connectorConfiguration.getOptionString(
                AdobeConnectConnector.RECORDINGS_PREFIX, "");
        this.recordingsFolderName = connectorConfiguration.getOptionString(
                AdobeConnectConnector.RECORDINGS_FOLDER_NAME);
        this.recordingsFolderId = getRecordingsFolderId();

        final AtomicReference<Thread> threadReference = new AtomicReference<>();
        threadReference.set(new Thread(Thread.currentThread().getName() + "-recordings")
        {
            private Logger logger = LoggerFactory.getLogger(AdobeConnectConnector.class);

            @Override
            public void run()
            {
                logger.info("Checking of recordings - starting...");
                while (threadReference.get() != null) {
                    try {
                        Thread.sleep(recordingsCheckTimeout);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        continue;
                    }

                    try {
                        if (mainConnector.isConnected()) {
                            checkRecordings();
                        }
                        else {
                            logger.info("Checking of recording skipped, connector is disconnected.");
                        }
                    } catch (Exception exception) {
                        logger.warn("Checking of recording failed", exception);
                    }
                }
                logger.info("Checking of recordings - exiting...");
            }
        });
        synchronized (this) {
            threadReference.get().start();
            this.checkRecordingsThreadReference = threadReference;
        }
    }

    /**
     * Destroy.
     */
    public void destroy()
    {
        synchronized (checkRecordingsThreadReference) {
            Thread checkRecordingsThread = this.checkRecordingsThreadReference.get();
            this.checkRecordingsThreadReference.set(null);
            checkRecordingsThread.interrupt();
        }
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
            Element response = connector.execApi("sco-shortcuts", null);
            for (Element sco : response.getChild("shortcuts").getChildren("sco")) {
                if ("content".equals(sco.getAttributeValue("type"))) {
                    // Find sco-id of recordings-folder folder
                    RequestAttributeList searchAttributes = new RequestAttributeList();
                    searchAttributes.add("sco-id", sco.getAttributeValue("sco-id"));
                    searchAttributes.add("filter-is-folder", "1");

                    Element shongoFolder = connector.execApi("sco-contents", searchAttributes);
                    for (Element folder : shongoFolder.getChild("scos").getChildren("sco")) {
                        if (recordingsFolderName.equals(folder.getChildText("name"))) {
                            recordingsFolderId = folder.getAttributeValue("sco-id");
                        }
                    }

                    // Creates recording folder if not exists
                    if (recordingsFolderId == null) {
                        logger.debug("Folder /{} for shongo meetings does not exists, creating...",
                                recordingsFolderName);

                        RequestAttributeList folderAttributes = new RequestAttributeList();
                        folderAttributes.add("folder-id", sco.getAttributeValue("sco-id"));
                        folderAttributes.add("name", recordingsFolderName);
                        folderAttributes.add("type", "folder");

                        Element folder = connector.execApi("sco-update", folderAttributes);
                        recordingsFolderId = folder.getChild("sco").getAttributeValue("sco-id");
                        logger.debug("Folder /{} for meetings created with sco-id: {}",
                                recordingsFolderName, recordingsFolderId);
                    }

                    break;
                }
            }
        }

        // Check if permission for this folder is denied, or sets it
        RequestAttributeList permissionsInfoAttributes = new RequestAttributeList();
        permissionsInfoAttributes.add("acl-id", recordingsFolderId);
        permissionsInfoAttributes.add("filter-principal-id", "public-access");

        String permissions = connector.execApi("permissions-info", permissionsInfoAttributes).getChild("permissions")
                .getChild("principal").getAttributeValue(
                        "permission-id");

        if (!"denied".equals(permissions)) {
            RequestAttributeList permissionsUpdateAttributes = new RequestAttributeList();
            permissionsUpdateAttributes.add("acl-id", recordingsFolderId);
            permissionsUpdateAttributes.add("principal-id", "public-access");
            permissionsUpdateAttributes.add("permission-id", "denied");

            connector.execApi("permissions-update", permissionsUpdateAttributes);
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
        // Check if folder with the same name doesn't already exists on AC server
        while (true) {
            RequestAttributeList attributes = new RequestAttributeList();
            attributes.add("sco-id", getRecordingsFolderId());
            attributes.add("filter-name", recordingFolderName + suffix);
            Element recFolders = connector.execApi("sco-contents", attributes);
            if (recFolders.getChild("scos").getChildren().size() == 0) {
                recordingFolderName = recordingFolderName + suffix;
                break;
            }
            index = index + 1;
            suffix = "_" + index.toString();
        }

        RequestAttributeList folderAttributes = new RequestAttributeList();
        folderAttributes.add("folder-id", getRecordingsFolderId());
        folderAttributes.add("name", recordingFolderName);
        folderAttributes.add("type", "folder");

        Element folder = connector.execApi("sco-update", folderAttributes);
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
        boolean isRecordingFolderPublic = isRecordingFolderPublic(recordingFolder.getId());
        connector.resetPermissions(recordingFolder.getId());

        RequestAttributeList userAttributes = new RequestAttributeList();
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

        if (isRecordingFolderPublic) {
            userAttributes.add("principal-id", "public-access");
            userAttributes.add("permission-id", AdobeConnectPermissions.VIEW_ONLY.getPermissionId());
        }
        connector.execApi("permissions-update", userAttributes);
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

        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", recordingFolderId);
        attributes.add("filter-icon", RECORDING_ICON);
        attributes.add("filter-out-date-end", "null");

        Element response = connector.execApi("sco-contents", attributes);
        for (Element resultRecording : response.getChild("scos").getChildren()) {
            Recording recording = parseRecording(resultRecording);
            recording.setPublic(isRecordingPublic(recording.getId()));
            recordingList.add(recording);
        }
        return Collections.unmodifiableList(recordingList);
    }

    private boolean isRecordingPublic(String recordingId) throws CommandException {
        return connector.isSCOPublic(recordingId);
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
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", scoId);
        attributes.add("filter-icon", RECORDING_ICON);
        attributes.add("filter-date-end", "null");
        Element response = connector.execApi("sco-contents", attributes);
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
        RequestAttributeList attributes = new RequestAttributeList();

        String recordingName = formatRecordingName(folderId, alias, DateTime.now());
        String path = connector.getLastPathSegmentFromURI(alias.getValue());
        String scoId = connector.getScoByUrl(path);
        attributes.add("sco-id", scoId);
        attributes.add("active", "true");
        attributes.add("name", recordingName);

        // throw exception if recording is not ready = no participants in the room
        try {
            connector.execApi("meeting-recorder-activity-update", attributes);
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

        RequestAttributeList recAttributes = new RequestAttributeList();
        recAttributes.add("sco-id", scoId);

        Element response;
        int count = 0;
        while (true) {
            try {
                Thread.sleep(AdobeConnectConnector.REQUEST_DELAY);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("unexpected wakening, but nothing to worry about");
            }
            response = connector.execApi("meeting-recorder-activity-info", recAttributes);

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
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", roomId);
        attributes.add("active", "false");
        connector.execApi("meeting-recorder-activity-update", attributes);

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
     * Make Recording public. Accessed by everyone.
     *
     * @throws CommandException
     */
    public void makeRecordingPublic(String recordingId) throws CommandException
    {
        checkIfRecording(recordingId);
        connector.setScoPermissions(recordingId, AdobeConnectPermissions.VIEW);
    }

    /**
     * Make Recording private. Authorized like recorded room.
     *
     * @throws CommandException
     */
    public void makeRecordingPrivate(String recordingId) throws CommandException
    {
        checkIfRecording(recordingId);
        connector.setScoPermissions(recordingId, AdobeConnectPermissions.PROTECTED);
    }

    /**
     * Make Recording public. Accessed by everyone.
     *
     * @throws CommandException
     */
    public void makeRecordingFolderPublic(String recordingId) throws CommandException
    {
        checkIfRecordingFolder(recordingId);
        connector.setScoPermissions(recordingId, AdobeConnectPermissions.VIEW_ONLY);
    }

    /**
     * Make Recording private. Authorized like recorded room.
     *
     * @throws CommandException
     */
    public void makeRecordingFolderPrivate(String recordingFolderId) throws CommandException
    {
        checkIfRecordingFolder(recordingFolderId);
        connector.setScoPermissions(recordingFolderId, AdobeConnectPermissions.PROTECTED);
    }

    /**
     * Returns if existing {@link RecordingFolder} is public.
     *
     * @param recordingFolderId
     */
    public boolean isRecordingFolderPublic(String recordingFolderId) throws CommandException {
        checkIfRecordingFolder(recordingFolderId);
        return connector.isSCOPublic(recordingFolderId);
    }

    /**
     * Throws Command exception if SCO is not recording.
     *
     * @param scoId
     * @throws CommandException
     */
    public void checkIfRecording(String scoId) throws CommandException
    {
        Element scoElement = connector.getScoInfo(scoId);
        if (!RECORDING_ICON.equals(scoElement.getAttributeValue("icon"))) {
            throw new IllegalArgumentException("SCO for given sco-id is not recording.");
        }
    }

    /**
     * Throws Command exception if SCO is not recording folder.
     * @param scoId
     * @throws CommandException
     */
    public void checkIfRecordingFolder(String scoId) throws CommandException
    {
        Element scoElement = connector.getScoInfo(scoId);
        if (!"folder".equals(scoElement.getAttributeValue("icon")) || !getRecordingsFolderId().equals(scoElement.getAttributeValue("folder-id"))) {
            throw new IllegalArgumentException("SCO for given sco-id is not recording folder.");
        }
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
        //TODO: remove if not used
        RequestAttributeList userAttributes = new RequestAttributeList();

        String recordingFolderId = (String) connector.performControllerAction(new GetRecordingFolderId(roomId));

        for (Recording recording : listRecordings(roomId)) {
            userAttributes.add("acl-id", recording.getId());

            connector.resetPermissions(recording.getId());

            for (Element principal : permissions.getChild("permissions").getChildren("principal")) {
                String principalId = principal.getAttributeValue("principal-id");
                userAttributes.add("principal-id", principalId);
                if ("host".equals(principal.getAttributeValue("permission-id"))) {
                    userAttributes.add("permission-id", "manage");
                }
                else {
                    userAttributes.add("permisson-id", "publish");
                }
            }

            if (userAttributes.getValue("principal-id") == null) {
                return;
            }

            /*if (isRecordingFolderPublic) {
                userAttributes.add("principal-id", "public-access");
                userAttributes.add("permission-id", AdobeConnectPermissions.VIEW_ONLY.getPermissionId());
            }*/

            logger.debug("Setting permissions for recording '{}' (sco ID: '{}').", recording.getName(),
                    recording.getId());
            connector.execApi("permissions-update", userAttributes);
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
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", roomId);
        attributes.add("filter-icon", RECORDING_ICON);
        List<Element> recordings = connector.execApi("sco-contents", attributes).getChild("scos").getChildren();

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
            recording.setDuration(new Interval(DateTime.parse(dateBegin), DateTime.parse(dateEnd)).toDuration());
        }

        DeviceAddress deviceAddress = connector.getDeviceAddress();
        String recordingUrlPath = recordingElement.getChildText("url-path");
        String recordingUrl = "https://" + deviceAddress.getHost() + ":" + deviceAddress.getPort() + recordingUrlPath;
        recording.setViewUrl(recordingUrl);
        if (dateEnd != null) {
            recording.setEditUrl(recordingUrl + "?pbMode=edit");
            recording.setDownloadUrl(recordingUrl + "?pbMode=offline");
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
            String recordingName = recordingsPrefix + recordingFolderName
                    + "_" + RECORDING_NAME_DATETIME_FORMATTER.print(dateTime);
            return recordingName;
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
        String currentRecordingFolderId = recording.getAttributeValue("folder-id");
        if (recordingFolderId != null) {
            if (currentRecordingFolderId.equals(recordingFolderId)) {
                return true;
            }
            else {
                return false;
            }
        }

        RequestAttributeList recFoldersAttributes = new RequestAttributeList();
        recFoldersAttributes.add("sco-id", getRecordingsFolderId());
        List<Element> recFolders = connector.execApi("sco-contents", recFoldersAttributes)
                .getChild("scos").getChildren();
        for (Element recFolder : recFolders) {
            if (recFolder.getAttributeValue("sco-id").equals(currentRecordingFolderId)) {
                cachedMovedRecordings.add(recordingId);
                return true;
            }
        }

        Element folder = connector.getScoInfo(currentRecordingFolderId);
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

        RequestAttributeList moveAttributes = new RequestAttributeList();
        moveAttributes.add("sco-id", recordingId);
        moveAttributes.add("folder-id", recordingFolderId);

        logger.info("Moving recording (id: " + recordingId + ") to recording folder (id: " + recordingFolderId + ")");
        // Counter for duplicate names
        int i = 0;
        // Move or rename if duplicate name (add "_X")
        while (true) {
            try {
                connector.execApi("sco-move", moveAttributes);
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
        RequestAttributeList recordingsAttributes =
                new RequestAttributeList();
        // choose only recordings
        recordingsAttributes.add("filter-icon", RECORDING_ICON);
        // filter out all recordings in progress
        recordingsAttributes.add("filter-out-date-end", "null");

        List<Element> recordings = connector.execApi("report-bulk-objects", recordingsAttributes)
                .getChild("report-bulk-objects").getChildren();

        List<String> allStoredRecordings = new ArrayList<String>();

        // Get set of all managed rooms
        Set<String> roomIds = getRoomIds();

        // Iterate over all recordings
        for (Element recording : recordings) {
            String recordingId = recording.getAttributeValue("sco-id");
            String roomId = connector.getScoInfo(recordingId).getAttributeValue("folder-id");

            // Process only recordings which are located in managed rooms
            if (!roomIds.contains(roomId)) {
                // Skip recordings which are located in not-managed rooms
                continue;
            }

            // Move recording to appropriate recording folder
            if (moveRecordingToAppropriateRecordingFolder(recordingId, roomId)) {
                allStoredRecordings.add(recordingId);
            }
            else {
                String recordingName = recording.getChildText("name");
                logger.warn("Recording " + recordingName + " (id: " + recordingId
                        + ") for shongo room cannot be moved to appropriate folder.");
            }
        }

        // Retain only existing stored recordings
        cachedMovedRecordings.retainAll(allStoredRecordings);
    }

    /**
     * @param recordingId to be checked
     * @throws CommandException
     */
    public synchronized void checkRecording(String recordingId) throws CommandException
    {
        String roomId = connector.getScoInfo(recordingId).getAttributeValue("folder-id");
        Set<String> roomIds = getRoomIds();
        if (roomIds.contains(roomId)) {
            moveRecordingToAppropriateRecordingFolder(recordingId, roomId);
        }
    }

    /**
     * @return list of sco-ids of managed rooms
     * @throws CommandException
     */
    private Set<String> getRoomIds() throws CommandException
    {
        // Get all shongo meetings
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("sco-id", connector.getMeetingsFolderId());
        attributes.add("type", "meeting");
        Element shongoRoomsElement = connector.execApi("sco-contents", attributes);

        Set<String> roomIds = new HashSet<String>();
        for (Element sco : shongoRoomsElement.getChild("scos").getChildren()) {
            roomIds.add(sco.getAttributeValue("sco-id"));
        }
        return roomIds;
    }

    /**
     * @param recordingId to be moved
     * @param roomId      where the recording is currently stored
     * @return true when the moving is done, false otherwise
     * @throws CommandException
     */
    private synchronized boolean moveRecordingToAppropriateRecordingFolder(String recordingId, String roomId)
            throws CommandException
    {
        if (isRecordingStored(recordingId, null)) {
            // Recording is already stored
            return true;
        }
        else {
            // Move is not stored and thus move it
            Element room = connector.getScoInfo(roomId);
            if ("meeting".equals(room.getAttributeValue("type"))) {
                String recordingFolderId = (String) connector.performControllerAction(new GetRecordingFolderId(roomId));
                if (recordingFolderId == null) {
                    throw new CommandException("GetRecordingFolderId for room " + roomId + " returned null.");
                }
                moveRecording(recordingId, recordingFolderId);
                cachedMovedRecordings.add(recordingId);
                return true;
            }
        }
        return false;
    }
}
