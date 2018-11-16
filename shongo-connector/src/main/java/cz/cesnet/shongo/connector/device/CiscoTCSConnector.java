package cz.cesnet.shongo.connector.device;

import com.google.common.base.Strings;
import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.Recording;
import cz.cesnet.shongo.api.RecordingFolder;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.RecordingService;
import cz.cesnet.shongo.connector.api.RecordingSettings;
import cz.cesnet.shongo.connector.common.AbstractDeviceConnector;
import cz.cesnet.shongo.connector.common.Command;
import cz.cesnet.shongo.connector.storage.*;
import cz.cesnet.shongo.connector.storage.File;
import cz.cesnet.shongo.controller.NotEnoughSpaceException;
import cz.cesnet.shongo.controller.api.jade.NotifyTarget;
import cz.cesnet.shongo.controller.api.jade.Service;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import cz.cesnet.shongo.util.MathHelper;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.auth.ContextAwareAuthScheme;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.tika.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link cz.cesnet.shongo.connector.common.AbstractConnector} for Cisco TelePresence Content Server
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class CiscoTCSConnector extends AbstractDeviceConnector implements RecordingService
{
    private static Logger logger = LoggerFactory.getLogger(CiscoTCSConnector.class);

    /**
     * Separator for recordings title
     */
    private final String SEPARATOR = "_";

    /**
     * Separator replacement for #link{SEPARATOR} in names
     */
    private final String SEPARATOR_REPLACEMENT = "__";

    /**
     * Initial waiting time for conference distribution.
     */
    private static final long INITIAL_WAITING_MILIS = 50000;

    /**
     * Maximum waiting time for conference distribution.
     */
    private static final long MAX_DISTRIBUTION_TIME_MILIS = 90000;

    /**
     * This is the user log in name, typically the user email address.
     */
    private String login;

    /**
     * The password of the user.
     */
    private String password;

    /**
     * {@link DateTimeFormatter} for new file-ids.
     * <p/>
     * We should not use {@link DateTimeFormat#forStyle} because it is {@link java.util.Locale} dependent.
     */
    private static final DateTimeFormatter FILE_ID_DATE_TIME_FORMATTER =
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZoneUTC();

    /**
     * Pattern for recording id (<recordingFolderId>_<recordingFileId>_<recordingTcsId>).
     */
    private final Pattern RECORDING_ID_PATTERN = Pattern.compile("^(.*[^_])_([^_].*[^_])_([^_].*)?$");

    /**
     * Pattern for recording name stored on TCS (<recordingFolderId>_<alias>_<recordingFileId>).
     */
    private final Pattern RECORDING_NAME_PATTERN = Pattern.compile("^(.*[^_])_([^_].*[^_])_([^_].*)$");

    /**
     * Pattern for display name of TCS downloadavke recording with size.
     */
    private final Pattern TCS_DOWNLOADABLE_URL_NAME = Pattern.compile("^.*[(](.*)[)]$");

    /**
     * Namespace constant for Cisco TCS
     */
    private final String NS_ENVELOPE = "SOAP-ENV";

    /**
     * Namespace constant for Cisco TCS
     */
    private final String NS_NS1 = "ns1";

    /**
     * Maximal number of threads for mooving recordings to storage.
     */
    private final int NUM_OF_THREADS = 10;

    /**
     * Path for SOAP request on TCS
     */
    private final String SOAP_PATH = "/tcs/SoapServer.php";

    /**
     * Path for status request.
     */
    private final String STATUS_PATH = "/tcs/status.xml";

    /**
     * If recording check is running.
     */
    private volatile boolean recordingChecking = false;

    /**
     * Timeout for checking if recording are in right folder, default value is 5 minutes
     */
    private int recordingsCheckTimeout;

    /**
     * Prefix for recordings (only on TCS server).
     */
    private String recordingsPrefix;

    /**
     * Namespace
     */
    private Namespace ns1;

    /**
     * Free space limit on TCS to start recording.
     */
    private Integer freeSpaceLimit = 1024;

    /**
     * Drive letter of recordings drive on TCS
     */
    private String recordingsTCSDrive;

    /**
     * Default bitrate for recordings.
     */
    private String recordingDefaultBitrate = "768";

    /**
     * TCS Alias for shongo recordings.
     */
    private String recordingAlias;

    /**
     * for debuging TCS date flow
     */
    private boolean debug = false;

    /**
     * Thread for checking recordings.
     */
    private AtomicReference<Thread> checkRecordingsThreadReference;

    /**
     * Storage unit for recordings (can have slow access)
     */
    private AbstractStorage storage;

    /**
     * Local storage unit for recordings (quick access)
     */
    private LocalStorageHandler metadataStorage;

    /**
     * Concurrent map of recordingFolderId by recordingId which are being moved from device to storage.
     */
    private final ConcurrentHashMap<String, String> recordingsBeingMoved = new ConcurrentHashMap<String, String>();

    private SAXBuilder saxBuilder = new SAXBuilder();
    private XMLOutputter xmlOutputter = new XMLOutputter();

    private synchronized void setRecordingChecking(boolean value)
    {
        this.recordingChecking = value;
    }


    public Storage getStorage()
    {
        return storage;
    }

    @Override
    public synchronized void connect(DeviceAddress deviceAddress, String username, String password) throws CommandException
    {
        this.login = username;
        this.password = password;

        this.recordingsCheckTimeout = (int) configuration.getOptionDuration(
                "recordings-check-period", Duration.standardMinutes(5)).getMillis();
        this.recordingsPrefix = configuration.getOptionString("recordings-prefix", "shongo_");

        String recordingDefaultBitrate = configuration.getOptionString("default-bitrate");
        if (recordingDefaultBitrate != null) {
            this.recordingDefaultBitrate = recordingDefaultBitrate;
        }
        this.freeSpaceLimit = configuration.getOptionInt("tcs-free-space-low-limit", this.freeSpaceLimit);
        this.recordingsTCSDrive = configuration.getOptionStringRequired("tcs-recordings-drive");

        this.recordingAlias = configuration.getOptionStringRequired("alias");

        String metadataStorage = configuration.getOptionStringRequired("metadata-storage");
        try {
            this.metadataStorage = new LocalStorageHandler(metadataStorage);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Cannot initialized CiscoTCSConnector, because folder for metadata does not exist.",e);
        }

        String storage = configuration.getOptionStringRequired("storage");
        String permission = configuration.getOptionString("storage-permission", "Require user ${userPrincipalName}");
        URL downloadableUrlBase = configuration.getOptionURLRequired("downloadable-url-base");
        try {
            this.storage = new ApacheStorage(storage, permission, downloadableUrlBase,
                    new AbstractStorage.UserInformationProvider() {
                        @Override
                        public UserInformation getUserInformation(String userId) throws CommandException {
                            return getUserInformationById(userId);
                        }
                    });
        } catch (FileNotFoundException e) {
            logger.error("Storage folder \"" + storage + "\" does not exist or is not accessible.");
            //TODO: agent.getContainerController().isJoined()
            throw new RuntimeException("Cannot initialized CiscoTCSConnector, because folder for recordings does not exist.",e);
        }

        // Enable xml output do debug log
        this.debug = configuration.getOptionBool("debug");

        checkServerVitality();

        final AtomicReference<Thread> threadReference = new AtomicReference<>();
        threadReference.set(new Thread(Thread.currentThread().getName() + "-recordings")
        {
            private Logger logger = LoggerFactory.getLogger(CiscoTCSConnector.class);

            @Override
            public void run()
            {
                setRecordingChecking(true);
                logger.info("Checking of recordings - starting...");
                try {
                    while (threadReference != null) {
                        try {
                            Thread.sleep(recordingsCheckTimeout);
                        }
                        catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            continue;
                        }

                        try {
                            if (isConnected()) {
                                performCheckRecordings();
                            }
                            else {
                                logger.error("Cannot check recording due to unavailable TCS server.");
                            }
                        }
                        catch (Exception exception) {
                            logger.warn("Checking location of recording failed", exception);
                        }
                    }
                }
                finally {
                    logger.info("Checking of recordings - exiting...");
                    setRecordingChecking(false);
                }
            }
        });

        synchronized (this) {
            if (!recordingChecking) {
                threadReference.get().start();
                this.checkRecordingsThreadReference = threadReference;
            }
        }
    }

    private void sendUnavailableNotification(String url) throws CommandException {
        NotifyTarget notifyTarget = new NotifyTarget(Service.NotifyTargetType.RESOURCE_ADMINS);
        notifyTarget.addMessage("en",
                "TCS storage folder not accessible:" + url,
                "TCS storage directory \"" + url + "\" does not exist or stale.");
        notifyTarget.addMessage("cs",
                "Adresář uložiště pro TCS není dostupný: " + url,
                "Adresář \"" + url + "\" pro ukládání nahrávek z TCS není dostupný.");

        /*TODO: get connection status
        Controller action failed: Sender agent tcs1 is not started yet.
        while (!agent.isStarted()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                continue;
            }
        }
        */
        performControllerAction(notifyTarget);
    }

    @Override
    public ConnectionState getConnectionState()
    {
        try {
            execApi(new Command("GetSystemInformation"), CONNECTION_STATE_TIMEOUT, null);
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
        synchronized (checkRecordingsThreadReference) {
            Thread checkRecordingsThread = this.checkRecordingsThreadReference.get();
            this.checkRecordingsThreadReference.set(null);
            checkRecordingsThread.interrupt();
        }
    }

    /**
     * Check if TCS server is online and engine status is OK, otherwise throws CommandException.
     *
     * @throws CommandException thrown if something is wrong
     */
    public void checkServerVitality() throws CommandException
    {
        Command command = new Command("GetSystemInformation");
        Element result = execApi(command).getChild("GetSystemInformationResponse");
        if (!"true".equals(result.getChild("GetSystemInformationResult").getChildText("EngineOK"))) {
            throw new CommandException("Server " + deviceAddress.getHost() + " is not working. Check its status.");
        }
    }

    @Override
    public String createRecordingFolder(RecordingFolder recordingFolder) throws CommandException {
        // TODO: return id even if storage not accessible, info saved in alias
        Folder folder = new Folder(null, recordingFolder.getName());

        // Folder on local storage for metadata
        String metadataFolderId;
        try {
            metadataFolderId = metadataStorage.createFolder(folder);
        } catch (FileNotFoundException e) {
            throw new CommandException("Cannot create metadata storage directory \"" + folder.getFolderName() + "\".",e);
        }
        recordingFolder.setName(folder.getFolderName());

        // Folder for recordings
        String folderId = null;
        try {
            folderId = storage.createFolder(folder);
            storage.setFolderPermissions(folderId, recordingFolder.getUserPermissions());
        } catch (FileNotFoundException e) {
            logger.warn("Cannot create TCS storage directory \"" + folder.getFolderName() + "\".");
            sendUnavailableNotification(storage.getUrl() + "/" + recordingFolder.getName());
            backupPermissions(recordingFolder, metadataFolderId);
        }

        if (folderId != null && !folderId.equals(metadataFolderId)) {
            throw new TodoImplementException("Fix different folderId for metadata's and recording's folders.");
        }

        return metadataFolderId;
    }

    @Override
    public void modifyRecordingFolder(RecordingFolder recordingFolder) throws CommandException
    {
        try {
            storage.setFolderPermissions(recordingFolder.getId(), recordingFolder.getUserPermissions());
        } catch (RuntimeException e) {
            try {
                if (!((ApacheStorage) storage).isFolderPermissionsSet(recordingFolder.getId())) {
                    backupPermissions(recordingFolder, recordingFolder.getId());
                }
            } catch (FileNotFoundException ex) {
                logger.warn("Cannot set permissons for TCS storage folder \"" + storage.getUrl() + "/" + recordingFolder.getId() + "\".");
                sendUnavailableNotification(storage.getUrl() + "/" + recordingFolder.getName());
                backupPermissions(recordingFolder, recordingFolder.getId());
                return;
            }
            throw e;
        }
    }

    private void backupPermissions(RecordingFolder recordingFolder, String folderId) throws CommandException {
        StringBuilder permissionData =
                ((ApacheStorage) storage).preparePermissionFileContent(recordingFolder.getUserPermissions());
        File backupPermissions = new File();
        backupPermissions.setFileName(ApacheStorage.PERMISSION_FILE_NAME);
        backupPermissions.setFolderId(folderId);
        if (metadataStorage.fileExists(backupPermissions)) {
            metadataStorage.deleteFile(folderId,ApacheStorage.PERMISSION_FILE_NAME);
        }
        metadataStorage.createFile(backupPermissions,new ByteArrayInputStream(permissionData.toString().getBytes()));
    }

    @Override
    public void deleteRecordingFolder(String recordingFolderId) throws CommandException
    {
        if (recordingFolderId == null || recordingFolderId.isEmpty()) {
            throw new IllegalArgumentException("Argument recordingFolderId must be not empty.");
        }
        synchronized (CiscoTCSConnector.class) {
            logger.debug("Removing recording folder (" + recordingFolderId + ").");
            // First call delete folder - it will wait till the moving is done there
            boolean skipWaiting = false;
            try {
                storage.deleteFolder(recordingFolderId);
            } catch (RuntimeException e) {
                logger.warn(e.getMessage());
                sendUnavailableNotification(storage.getUrl() + "/" + recordingFolderId);
                skipWaiting = true;
            }
            metadataStorage.deleteFolder(recordingFolderId);

            // Skip if the folder is not accessible at the time
            if (!skipWaiting) {
                // Wait until moving recordings is done
                // TODO: Stop the moving (because it can last very long)
                while (recordingsBeingMoved.containsValue(recordingFolderId)) {
                    try {
                        StringBuilder recordings = new StringBuilder();
                        for (Map.Entry<String, String> entry : recordingsBeingMoved.entrySet()) {
                            if (entry.getValue().equals(recordingFolderId)) {
                                if (recordings.length() > 0) {
                                    recordings.append(", ");
                                }
                                recordings.append(entry.getKey());
                            }
                        }
                        logger.debug(
                                "Waiting with deletion of recording folder {} for recordings [{}] to be moved into it.",
                                recordingFolderId, recordings);
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        continue;
                    }
                }
            }

            // Delete original recordings
            for (Recording recording : listTcsRecordings(maskSeparator(recordingFolderId) + "*")) {
                String recordingTcsId = getRecordingTcsIdFromRecordingId(recording.getId());
                deleteTcsRecording(recordingTcsId);
            }
        }
    }

    @Override
    public Collection<Recording> listRecordings(String recordingFolderId)
            throws CommandException
    {
        List<Recording> recordings = new ArrayList<Recording>();
        for (cz.cesnet.shongo.connector.storage.File file : metadataStorage.listFiles(recordingFolderId, null)) {
            if (isMetadataFilename(file.getFileName())) {
                String recordingId = formatRecordingId(recordingFolderId, formatRecordingFileId(file.getFileName()));
                Recording recording = getRecording(recordingId);
                recordings.add(recording);
            }
        }
        return Collections.unmodifiableList(recordings);
    }

    @Override
    public Recording getRecording(String recordingId) throws CommandException
    {
        String recordingFolderId = getRecordingFolderIdFromRecordingId(recordingId);
        String fileId = getFileIdFromRecordingId(recordingId);
        String metadataFileName = getMetadataFilename(fileId);
        if (metadataStorage.fileExists(new File(recordingFolderId, metadataFileName))) {
            Recording recording;
            try {
                InputStream inputStream = metadataStorage.getFileContent(recordingFolderId, metadataFileName);
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder inputStringBuilder = new StringBuilder();
                String line = bufferedReader.readLine();
                while (line != null) {
                    inputStringBuilder.append(line);
                    inputStringBuilder.append('\n');
                    line = bufferedReader.readLine();
                }
                bufferedReader.close();
                String recordingXml = inputStringBuilder.toString();
                Document resultDocument = saxBuilder.build(new StringReader(recordingXml));
                Element rootElement = resultDocument.getRootElement();
                recording = parseRecording(rootElement);
                if (Recording.State.NOT_PROCESSED.equals(recording.getState())) {
                    // Refresh state from TCS
                    String recordingTcsId = getRecordingTcsIdFromRecordingId(recording.getId());
                    Element recordingTcsElement = getTcsRecordingElement(recordingTcsId);
                    if (recordingTcsElement != null) {
                        Recording recordingTcs = parseRecording(recordingTcsElement);
                        recording.setState(recordingTcs.getState());
                    }
                }
            }
            catch (IOException e) {
                throw new RuntimeException(
                        "Error while reading file " + getRecordingFolderIdFromRecordingId(
                                recordingId) + "/" + getMetadataFilename(
                                getFileIdFromRecordingId(recordingId)) + ".".replaceAll(
                                "//", "/"));
            }
            catch (JDOMException e) {
                throw new RuntimeException(
                        "Error while parsing file " + getRecordingFolderIdFromRecordingId(
                                recordingId) + "/" + getMetadataFilename(
                                getFileIdFromRecordingId(recordingId)) + ".".replaceAll("//", "/"));
            }
            if (recording.getDownloadUrl() != null) {
                // Set downloadable URL if file is available in storage
                String downloadableUrl = null;
                try {
                    downloadableUrl = storage.getFileDownloadableUrl(recordingFolderId, recording.getFileName());
                } catch (MalformedURLException e) {
                    String message = "Failed to format downloadable URL for recording.";
                    logger.error(message, e);
                }
                if (downloadableUrl != null) {
                    recording.setState(Recording.State.AVAILABLE);
                    recording.setDownloadUrl(downloadableUrl);
                } else {
                    recording.setDownloadUrl(null);
                    logger.warn("TCS recording \"" + recordingFolderId + "/" + recording.getFileName() + "\" is not available, because the folder is not accessible.");
                    sendUnavailableNotification(storage.getUrl() + "/" + recordingFolderId);
                    //TODO: show error to user
                }
            }
            recording.setRecordingFolderId(getRecordingFolderIdFromRecordingId(recordingId));
            return recording;
        }
        else {
            String recordingTcsId = getRecordingTcsIdFromRecordingId(recordingId);
            Recording recording = getTcsRecording(recordingTcsId);
            return recording;
        }
    }

    /**
     * Not supported for Cisco TCS connector, returns null every time.
     *
     * @param alias alias for room
     * @return
     * @throws CommandException
     * @throws CommandUnsupportedException
     */
    @Override
    public Recording getActiveRecording(Alias alias) throws CommandException, CommandUnsupportedException
    {
        return null;
    }

    @Override
    public boolean isRecordingActive(String recordingId) throws CommandException
    {
        Command command = new Command("GetCallInfo");
        command.setParameter("ConferenceID", getRecordingTcsIdFromRecordingId(recordingId));

        Element result = execApi(command);
        Element callInfoElement = result.getChild("GetCallInfoResponse").getChild("GetCallInfoResult");
        return "IN_CALL".endsWith(callInfoElement.getChildText("CallState"));
    }

/*    private String createAdHocAlias() throws CommandException
    {
        Command command = new Command("AddRecordingAlias");
        command.setParameter("SourceAlias","999");
        command.setParameter("Data","<Name>123</Name<E164Alias>123</E164Alias>");
        execApi(command,true);
        return null;
    }

    private void deleteAlias(String aliasId) throws CommandException
    {
        Command command = new Command("DeleteRecordingAlias");
        command.setParameter("Alias",aliasId);

        execApi(command);
    } */

    /**
     * @param recordingFolderId
     * @param alias             alias of an endpoint which should be recorded (it can be a virtual room)
     * @param recordingSettings recording settings
     * @return recordingId is {@link java.lang.String} composed like this "recordingFolderId:fileId:recordingTCSId"
     * @throws CommandException
     */
    @Override
    public String startRecording(String recordingFolderId, Alias alias, String recordingPrefixName, RecordingSettings recordingSettings)
            throws CommandException
    {
        //TODO: check if available slots

        if (!hasEnoughSpaceOnTCS()) {
            logger.error("Not enough space on TCS server.");
            throw new NotEnoughSpaceException("Recordings cannot start, because there is no free space on TCS.");
        }
        if (!alias.getType().equals(AliasType.H323_E164)) {
            throw new TodoImplementException("TODO: implement recording for other aliases than H.323_164.");
        }

        String recordingName = formatRecordingName(recordingFolderId, alias, DateTime.now(), recordingPrefixName);
        Command command = new Command("RequestConferenceID");
        command.setParameter("owner", "admin");
        command.setParameter("password", "");
        command.setParameter("startDateTime", "0");
        command.setParameter("duration", "0");
        command.setParameter("title", recordingName);
        command.setParameter("groupId", "");
        command.setParameter("isRecurring", "false");

        String conferenceID = execApi(command).getChild("RequestConferenceIDResponse").getChildText(
                "RequestConferenceIDResult");

        Command dialCommand = new Command("Dial");
        dialCommand.setParameter("Number", alias.getValue());
        String bitrate = recordingSettings.getBitrate() == null ? recordingDefaultBitrate : recordingSettings.getBitrate();
        dialCommand.setParameter("Bitrate", bitrate);
        //TODO: create alias for adhoc recording, find out if necessary
        dialCommand.setParameter("Alias", recordingAlias);
        dialCommand.setParameter("ConferenceID", conferenceID);
        //TODO: set technology if SP
        dialCommand.setParameter("CallType", "h323");
        dialCommand.setParameter("SetMetadata", true);
        dialCommand.setParameter("PIN", recordingSettings.getPin());

        // Wait for distribution
        try {
            logger.debug("Initial waiting " + (int)((INITIAL_WAITING_MILIS / 1000) % 60) + " sec.");
            Thread.sleep(INITIAL_WAITING_MILIS);
        } catch (InterruptedException e) {
            logger.error("Sleep interrupted");
        }

        // Try to dial and confirm dial
        long startTime = System.currentTimeMillis();
        String callState = null;
        String recordingTcsId = null;
        try {
            recordingTcsId = executeDial(dialCommand);
            logger.debug("Dial command done.");
            callState = confirmDialExecuted(recordingTcsId);
        } catch (CommandException ex) {
            logger.debug("Unable to fetch callInfo. Will try again.");
            ex.printStackTrace();
        }

        long dialExecutionTime = System.currentTimeMillis() - startTime;
        long timeLeft = MAX_DISTRIBUTION_TIME_MILIS - (dialExecutionTime + INITIAL_WAITING_MILIS);


        // Check call state and try again if needed
        if (callState == null || !callState.equals("IN_CALL")) {
            if (timeLeft > 0) {
                try {
                    logger.debug("Waiting another " + (int)((timeLeft / 1000) % 60)+ " sec and will try to dial again.");
                    Thread.sleep(timeLeft);
                } catch (InterruptedException e) {
                    logger.error("Sleep interrupted");
                }
            }
            recordingTcsId = executeDial(dialCommand);
            callState = confirmDialExecuted(recordingTcsId);
            if (callState == null || !callState.equals("IN_CALL")) {
                throw new CommandException("Unable to set up recording. Call state: " + callState + ".");
            }
        } else {
            logger.info("Recording set up successfully.");
        }

        // Get recording ID
        Recording recording = getTcsRecording(recordingTcsId);
        String recordingId = recording.getId();
        return recordingId;

        // Check that recording can be listed
        /*List<Recording> listedRecordings = listTcsRecordings("*");
        List<Recording> listedRecordings2 = listTcsRecordings("*");
        List<Recording> listedRecordings3 = listTcsRecordings("*");
        if (true) {
            throw new RuntimeException("test");
        }
        for (Recording listedRecording : listedRecordings) {
            if (listedRecording.getId().equals(recordingId)) {
                return recordingId;
            }
        }
        deleteTcsRecording(recordingTcsId);
        throw new CommandException("The started recording '" + recordingId + "' isn't possible to list."
                + " The started recording was deleted and the starting has failed.");*/
    }

    public String executeDial(Command dialCommand) throws CommandException {
        if (!dialCommand.getCommand().equals("Dial")) {
            throw new RuntimeException("Method received wrong command to dial.");
        }
        Element result = execApi(dialCommand);
        String recordingTcsId = result.getChild("DialResponse").getChild("DialResult").getChildText("ConferenceID");
        if (recordingTcsId == null) {
            throw new CommandException("No recordingId was returned from dialing.");
        }
        return recordingTcsId;
    }

    public String confirmDialExecuted(String conferenceId) throws CommandException {
        String callState = null;
        Element result;
        // Passing this means that TCS executed dial
        for (int i = 0; i < 2; i++) {
            try {
                logger.debug("Waiting 30sec for TCS .");
                Thread.sleep(30*1000);
                Command callInfoCommand = new Command("GetCallInfo");
                callInfoCommand.setParameter("ConferenceID", conferenceId);
                result = execApi(callInfoCommand);
                callState = result.getChild("GetCallInfoResponse").getChild("GetCallInfoResult").getChildText("CallState");
                break;
            } catch (FaultException e) {
                logger.debug((i+1) + ". try to fetch callInfo failed. ConferenceID not found in TCS." );
                if (i == 1) {
                    throw new CommandException("Unable to fetch getCallInfo. Dial was not executed.", e);
                }
            } catch (InterruptedException ex) {
                logger.error("Sleep interrupted.");
                if (i == 1) {
                    throw new RuntimeException("Interrupted sleep on last cycle to fetch callInfo.");
                }
            }
        }
        return callState;
    }

    @Override
    public void stopRecording(String recordingId) throws CommandException
    {
        Command command = new Command("DisconnectCall");
        command.setParameter("ConferenceID", getRecordingTcsIdFromRecordingId(recordingId));

        execApi(command);

        // create metadata file
        String recordingTcsId = getRecordingTcsIdFromRecordingId(recordingId);
        Element originalRecordingElement = getTcsRecordingElementRequired(recordingTcsId);
        createMetadataFiles(recordingId, originalRecordingElement);
    }

    @Override
    public void deleteRecording(String recordingId) throws CommandException
    {
        String folderId = getRecordingFolderIdFromRecordingId(recordingId);
        String fileId = getFileIdFromRecordingId(recordingId);
        String recordingTcsId = getRecordingTcsIdFromRecordingId(recordingId);

        // Delete recording on TCS if exists
        Element recordingTcsElement = getTcsRecordingElement(recordingTcsId);
        if (recordingTcsElement != null) {
            deleteTcsRecording(recordingTcsId);
        }

        // Delete storage files
        if (storage.folderExists(folderId)) {
            for (File file : storage.listFiles(folderId, fileId)) {
                if (storage.filenameEqualsFileId(file, fileId)) {
                    storage.deleteFile(folderId, file.getFileName());
                    if (isMetadataFilename(file.getFileName())) {
                        metadataStorage.deleteFile(folderId, file.getFileName());
                    }
                }
            }
        }
    }

    @Override
    public void checkRecording(String recordingId) throws CommandException
    {
        throw new TodoImplementException("is never used");
        /*
        String recordingTcsId = getRecordingTcsIdFromRecordingId(recordingId);
        Element recordingTcsElement = getTcsRecordingElement(recordingTcsId);
        if (recordingTcsElement == null) {
            return;
        }
        Recording recording = parseRecording(recordingTcsElement);
        recordingId = recording.getId();
        String recordingFolderId = recording.getRecordingFolderId();
        Set<String> recordingFolderIds = getRecordingFolderIds();
        if (!recordingFolderIds.contains(recordingFolderId)) {
            return;
        }
        if (recording.getDownloadUrl() == null) {
            return;
        }
        synchronized (recordingsBeingMoved) {
            if (recordingsBeingMoved.containsKey(recordingId)) {
                return;
            }
            recordingsBeingMoved.put(recordingId, recordingFolderId);
        }
        moveRecordingToAppropriateRecordingFolder(recordingId);
        */
    }



    @Override
    public void checkRecordings() throws CommandException
    {
        ExecutorService executorService = performCheckRecordings();
        try {
            executorService.awaitTermination(getRequestTimeout(), TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException exception) {
            throw new CommandException("Check recordings interrupted", exception);
        }
    }

    /**
     * Not supported for Cisco TCS connector.
     *
     * @return
     * @throws CommandException
     * @throws CommandUnsupportedException
     */
    @Override
    public void makeRecordingPublic(String recordingId) throws CommandException, CommandUnsupportedException
    {
    }

    /**
     * Not supported for Cisco TCS connector.
     *
     * @return
     * @throws CommandException
     * @throws CommandUnsupportedException
     */
    @Override
    public void makeRecordingPrivate(String recordingId) throws CommandException, CommandUnsupportedException
    {
    }

    /**
     * Not supported for Cisco TCS connector.
     *
     * @return
     * @throws CommandException
     * @throws CommandUnsupportedException
     */
    @Override
    public void makeRecordingFolderPublic(String recordingFolderId) throws CommandException, CommandUnsupportedException {
    }

    /**
     * Not supported for Cisco TCS connector.
     *
     * @return
     * @throws CommandException
     * @throws CommandUnsupportedException
     */
    @Override
    public void makeRecordingFolderPrivate(String recordingFolderId) throws CommandException, CommandUnsupportedException {
    }

    /**
     * Not supported for Cisco TCS connector, returns false every time.
     *
     * @return
     * @throws CommandException
     * @throws CommandUnsupportedException
     */
    @Override
    public boolean isRecordingFolderPublic(String recordingFolderId) throws CommandException {
        return false;
    }

    public boolean hasEnoughSpaceOnTCS() throws CommandException {
        Command command = new Command("GetSystemStatus");
        Element systemStatus = execApi(command, this.requestTimeout, STATUS_PATH);
        Namespace ns = systemStatus.getNamespace();
        for (Element drive : systemStatus.getChild("DriveInfo",ns).getChildren()) {
            if (recordingsTCSDrive.equals(drive.getChildText("DriveLetter",ns))) {
                int freeSpace = Integer.decode(drive.getChildText("Free",ns));
                if (this.freeSpaceLimit.compareTo(freeSpace) < 0) {
                    logger.debug("Space left on TCS server \"" + getDeviceAddress().getHost() + "\": " + freeSpace + " kB.");
                    return true;
                } else {
                    logger.warn("Space left on TCS server \"" + getDeviceAddress().getHost() + "\": " + freeSpace + " kB.");
                    return false;
                }
            }
        }

        logger.warn("No system information found for drive \"" + this.recordingsTCSDrive + "\" found ");
        return false;
    }

    /**
     * Exec command with debug output to std if set in option file.
     *
     * @param command
     * @return
     * @throws CommandException
     */
    private Element execApi(Command command) throws CommandException
    {
        return execApi(command, this.requestTimeout, this.SOAP_PATH);
    }

    /**
     * Execute command on Cisco TCS server
     *
     * @param command to execute
     * @return Document Element
     * @throws CommandException
     */
    private synchronized Element execApi(Command command, int timeout, String requestUrl) throws CommandException
    {
        if (requestUrl == null) {
            requestUrl = SOAP_PATH;
        }
        try {
            HttpClient httpClient = ConfiguredSSLContext.getInstance().createHttpClient(timeout);
            while (true) {

                logger.debug(String.format("Issuing command '%s' on %s", command.getCommand(), deviceAddress));

                final ContextAwareAuthScheme md5Auth = new DigestScheme();

                // Setup POST request
                HttpPost lHttpPost = new HttpPost(deviceAddress.getUrl() + requestUrl);

                // Set SOAPAction header
                lHttpPost.addHeader("SOAPAction", "http://www.tandberg.net/XML/Streaming/1.0/GetSystemInformation");

                // Add XML to request, direct in the body - no parameter name
                String xml = buildExecXml(command);

                if (debug) {
                    System.out.println("===================");
                    System.out.println("INPUT");
                    System.out.println("===================");
                    System.out.println(xml);
                }

                StringEntity lEntity = new StringEntity(xml, ContentType.create("text/xml", "utf-8"));
                lHttpPost.setEntity(lEntity);

                // Protocol version should be 1.0 because of compatibility with TCS
                lHttpPost.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_0);
                HttpResponse authResponse = httpClient.execute(lHttpPost);

                // Validate that we got an HTTP 401 back
                if (authResponse.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                    if (authResponse.containsHeader("WWW-Authenticate")) {
                        // Get the challenge.
                        final Header challenge =
                                authResponse.getHeaders("WWW-Authenticate")[0];
                        // Solve it.
                        md5Auth.processChallenge(challenge);
                        // Generate a solution Authentication header using your
                        // username and password.
                        // Do another POST, but this time include the solution
                        final Header solution = md5Auth.authenticate(
                                new UsernamePasswordCredentials(this.login, this.password),
                                new BasicHttpRequest(HttpPost.METHOD_NAME, requestUrl),
                                new BasicHttpContext());

                        // Authentication header as generated by HttpClient.
                        lHttpPost.setHeader(solution);

                        lHttpPost.releaseConnection();

                        /*
                        System.out.println("===================");
                        System.out.println(lHttpPost.getURI());
                        System.out.println("===================");
                        for (Header header : lHttpPost.getAllHeaders()){
                            System.out.println(header.toString() + " : " + header.getName() + " --- " + header.getValue());
                        }
                        System.out.println("===================");
                        */

                        final HttpResponse goodResponse = httpClient.execute(lHttpPost);

                        String resultString = EntityUtils.toString(goodResponse.getEntity());

                        if (debug) {
                            System.out.println("==========");
                            System.out.println("OUTPUT");
                            System.out.println("==========");
                            System.out.println(resultString);
                        }

                        // For requested status info return whole XML response
                        if (requestUrl.equals(STATUS_PATH)) {
                            Document resultDocument = saxBuilder.build(new StringReader(removeNamespace(resultString)));
                            return resultDocument.getRootElement();
                        }

                        // Remove namespace NS_NS1
                        if (this.ns1 == null) {
                            Document resultDocumentTmp = saxBuilder.build(new StringReader(resultString));
                            Element rootElementTmp = resultDocumentTmp.getRootElement();
                            this.ns1 = rootElementTmp.getNamespace(NS_NS1);
                            if (this.ns1 == null) {
                                throw new IllegalStateException("Namespace doesn't exist.");
                            }

                        }
                        Document resultDocument = saxBuilder.build(new StringReader(removeNamespace(resultString)));
                        Element rootElement = resultDocument.getRootElement();
                        Namespace envelopeNS = rootElement.getNamespace(NS_ENVELOPE);
                        Element bodyElement = rootElement.getChild("Body", envelopeNS);
                        if (bodyElement == null || goodResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                            String faultString = "Unknown";
                            Element fault = bodyElement != null ? bodyElement.getChild("Fault", envelopeNS) : null;
                            if (fault != null) {
                                faultString = fault.getChildText("faultstring");
                            }
                            throw new FaultException("Command '" + command.getCommand() + "' issuing error: "
                                    + goodResponse.getStatusLine().getReasonPhrase(), faultString);
                        }
                        return bodyElement;
                    }
                    else {
                        throw new Error("Web-service responded with Http 401, " +
                                "but didn't send us a usable WWW-Authenticate header.");
                    }
                }
                else {
                    throw new Error("Didn't get an Http 401 like we were expecting, but (" +
                            authResponse.getStatusLine().getStatusCode() + ").");
                }
            }
        }
        catch (FaultException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new CommandException("Command '" + command.getCommand() + "' issuing error", exception);
        }
    }

    /**
     * Converts from recording info from source {@link org.jdom2.Element} to {@link Recording}
     *
     * @param recordingTcsElement raw recording xml data
     * @return recording info
     */
    private Recording parseRecording(Element recordingTcsElement) throws InvalidFormatException
    {
        String recordingName = recordingTcsElement.getChildText("Title");
        String recordingFolderId = getRecordingFolderIdFromRecordingName(recordingName);
        String recordingFileId = getRecordingFileIdFromRecordingName(recordingName);
        String recordingTcsId = recordingTcsElement.getChildText("ConferenceID");
        String recordingId = formatRecordingId(recordingFolderId, recordingFileId, recordingTcsId);

        DateTime beginDate = new DateTime(Long.decode(recordingTcsElement.getChildText("DateTime")) * 1000);
        Duration duration = new Duration(Long.decode(recordingTcsElement.getChildText("Duration")).longValue());

        Recording recording = new Recording();
        recording.setId(recordingId);
        recording.setRecordingFolderId(recordingFolderId);
        recording.setName(recordingName);
        recording.setBeginDate(beginDate);
        recording.setDuration(duration);
        if (duration.isEqual(Duration.standardSeconds(0))) {
            recording.setState(Recording.State.NOT_STARTED);
        }
        else {
            recording.setState(Recording.State.NOT_PROCESSED);
        }
        if ("true".equals(recordingTcsElement.getChildText("HasDownloadableMovie"))) {
            Element downloadableMovie = recordingTcsElement.getChild("DownloadableMovies").getChild("DownloadableMovie");

            String originDescription = downloadableMovie.getChildText("Display");
            recording.setSize(parseSize(originDescription));

            String downloadUrl = downloadableMovie.getChildText("URL");
            String[] downloadUrlParts = downloadUrl.split("\\.");
            String extension = downloadUrlParts[downloadUrlParts.length - 1];
            recording.setDownloadUrl(downloadUrl);
            recording.setFileName(recordingFileId + "." + extension);
            recording.setState(Recording.State.PROCESSED);
        }
        return recording;
    }

    /**
     * Parse size from TCS name of downloadable url
     * @param originDescription
     * @return
     */
    private long parseSize(String originDescription)
    {
        String size = "";
        Matcher matcher = TCS_DOWNLOADABLE_URL_NAME.matcher(originDescription);
        if (!matcher.find()) {
            logger.error("Failed to parse size from <display>  \"" + originDescription + "\"");
        }
        else {
            size = matcher.group(1);
        }
        return MathHelper.toBytes(size);
    }

    /**
     * @param recordingTcsId identifier on Cisco TCS server
     * @return raw recording xml data for recording with given {@code recordingTcsId} or {@code null} if doesn't exist
     */
    private Element getTcsRecordingElement(String recordingTcsId) throws CommandException
    {
        Command command = new Command("GetConference");
        command.setParameter("ConferenceID", recordingTcsId);
        try {
            return execApi(command).getChild("GetConferenceResponse").getChild("GetConferenceResult");
        }
        catch (FaultException exception) {
            if (exception.getFault().equals("Unknown ConferenceID")) {
                return null;
            }
            else {
                throw exception;
            }
        }
    }

    /**
     * @param recordingTcsId identifier on Cisco TCS server
     * @return raw recording xml data for recording with given {@code recordingTcsId}
     * @throws CommandException
     */
    private Element getTcsRecordingElementRequired(String recordingTcsId) throws CommandException
    {
        Element recordingTcsElement = getTcsRecordingElement(recordingTcsId);
        if (recordingTcsElement == null) {
            throw new CommandException("Recording " + recordingTcsId + " doesn't exist in the device.");
        }
        return recordingTcsElement;
    }

    /**
     * @param recordingTcsId
     * @return {@link Recording} from TCS server for given {@code recordingTcsId}
     * @throws CommandException
     */
    private Recording getTcsRecording(String recordingTcsId) throws CommandException
    {
        Element recordingTcsElement = getTcsRecordingElementRequired(recordingTcsId);
        return parseRecording(recordingTcsElement);
    }

    /**
     * Delete recording from Cisco TCS server
     *                                                                                     f
     * @param recordingTcsId identifier of the recording to delete
     * @throws CommandException
     */
    private void deleteTcsRecording(String recordingTcsId) throws CommandException
    {
        logger.debug("Deleting original recording from TCS (ID: " + recordingTcsId + ").");
        Command command = new Command("DeleteRecording");
        command.setParameter("conferenceID", recordingTcsId);
        execApi(command);
    }

    /**
     * @param regex according Cisco TelePresence Content Server documentation of function GetConferences)
     * @return list of recordings by given {@code regex}
     */
    private List<Recording> listTcsRecordings(String regex) throws CommandException
    {
        Command command = new Command("GetConferences");
        command.setParameter("SearchExpression", recordingsPrefix + regex);
        command.setParameter("ResultRange", "");
        command.setParameter("DateTime", "");
        command.setParameter("UpdateTime", "");
        command.setParameter("Owner", "");
        command.setParameter("Category", "");
        command.setParameter("Sort", "DateTime");

        Element result = execApi(command).getChild("GetConferencesResponse");

        ArrayList<Recording> recordings = new ArrayList<Recording>();
        for (Element recordingElement : result.getChild("GetConferencesResult").getChildren("Conference")) {
            try {
                recordings.add(parseRecording(recordingElement));
            }
            catch (InvalidFormatException exception) {
                logger.warn(exception.getMessage());
                continue;
            }
        }

        return recordings;
    }

    /**
     * Save metadata of recording to storage.
     *
     * @param recordingId      identifier of the recording
     * @param recordingTcsElement recording xml metadata
     * @throws CommandException
     */
    private void createMetadataFiles(String recordingId, Element recordingTcsElement) throws CommandException
    {
        String fileId = getFileIdFromRecordingId(recordingId);
        String folderId = getRecordingFolderIdFromRecordingId(recordingId);

        File metadataFile = new File();
        metadataFile.setFileName(getMetadataFilename(fileId));
        metadataFile.setFolderId(folderId);

        metadataStorage.createFile(metadataFile,
                new ByteArrayInputStream(xmlOutputter.outputString(recordingTcsElement).getBytes()));

        if (storage.folderExists(folderId)) {
            storage.createFile(metadataFile,
                    new ByteArrayInputStream(xmlOutputter.outputString(recordingTcsElement).getBytes()));
        }
    }

    private void updateMetadataFiles(String recordingFolderId, String recordingId, Element recordingTcsElement) throws CommandException {
        try {
            storage.deleteFile(recordingFolderId, getMetadataFilename(getFileIdFromRecordingId(recordingId)));
            metadataStorage.deleteFile(recordingFolderId,
                    getMetadataFilename(getFileIdFromRecordingId(recordingId)));
        }
        catch (Exception e) {
            logger.warn("Deleting of temporary metadata file failed (recording ID: " + recordingId + ").");
        }
        createMetadataFiles(recordingId, recordingTcsElement);
    }

    /**
     * Returns filename for metadata file.
     *
     * @param fileId file identifier of the recording
     * @return
     */
    private String getMetadataFilename(String fileId)
    {
        return "." + fileId + ".xml";
    }

    /**
     * Return {@code true} if filename is in metadata filename format, {@code false} otherwise
     *
     * @param filename
     * @return
     */
    private boolean isMetadataFilename(String filename)
    {
        return filename.startsWith(".") && filename.endsWith(".xml");
    }

    /**
     * @param dateTime
     * @return recordingFileId for given dateTime
     */
    private static String formatRecordingFileId(DateTime dateTime, String recordingPrefixName)
    {
        if (!Strings.isNullOrEmpty(recordingPrefixName)) {
            recordingPrefixName += ":";
        }
        else {
            recordingPrefixName = "";
        }
        return recordingPrefixName + FILE_ID_DATE_TIME_FORMATTER.print(dateTime);
    }

    /**
     * @param filename
     * @return recordingFileId for given filename
     */
    private String formatRecordingFileId(String filename)
    {
        if (filename.startsWith(".")) {
            filename = filename.substring(1);
        }
        if (filename.indexOf(".") > 0) {
            filename = filename.substring(0, filename.lastIndexOf("."));
        }
        return filename;
    }

    /**
     * @param recordingFolderId
     * @param recordingFileId
     * @param recordingTcsId
     * @return recordingId constructed from given {@code folderId}, {@code fileId} and {@code tcsRecordingId} segments
     */
    private String formatRecordingId(String recordingFolderId, String recordingFileId, String recordingTcsId)
    {
        StringBuilder recordingIdBuilder = new StringBuilder();
        recordingIdBuilder.append(maskSeparator(recordingFolderId));
        recordingIdBuilder.append(SEPARATOR);
        recordingIdBuilder.append(maskSeparator(recordingFileId));
        recordingIdBuilder.append(SEPARATOR);
        recordingIdBuilder.append(maskSeparator(recordingTcsId));
        return recordingIdBuilder.toString();
    }

    /**
     * @param recordingFolderId
     * @param recordingFileId
     * @return recordingId constructed from given {@code folderId}, {@code fileId} and empty tcsRecordingId segments
     */
    private String formatRecordingId(String recordingFolderId, String recordingFileId)
    {
        return formatRecordingId(recordingFolderId, recordingFileId, "");
    }

    /**
     * @param recordingFolderId
     * @param alias
     * @param recordingStartedAt
     * @return recording name for given {@code recordingFolderId}, {@code alias} and {@code dateTime}
     */
    private String formatRecordingName(String recordingFolderId, Alias alias, DateTime recordingStartedAt, String recordingPrefixName) {
        return formatRecordingName(recordingFolderId, alias.getValue(), formatRecordingFileId(recordingStartedAt, recordingPrefixName));
    }

    /**
     * @param recordingFolderId
     * @param alias
     * @param recordingFileId
     * @return recording name for given {@code recordingFolderId}, {@code alias} and {@code dateTime}
     */
    private String formatRecordingName(String recordingFolderId, String alias, String recordingFileId)
    {
        StringBuilder recordingNameBuilder = new StringBuilder();
        recordingNameBuilder.append(recordingsPrefix);
        recordingNameBuilder.append(maskSeparator(recordingFolderId));
        recordingNameBuilder.append(SEPARATOR);
        recordingNameBuilder.append(maskSeparator(alias));
        recordingNameBuilder.append(SEPARATOR);
        recordingNameBuilder.append(maskSeparator(recordingFileId));
        return recordingNameBuilder.toString();
    }

    /**
     * @param recordingId
     * @return folderId (1st part of recordingId) from given {@code recordingId}
     */
    private String getRecordingFolderIdFromRecordingId(String recordingId)
    {
        Matcher matcher = RECORDING_ID_PATTERN.matcher(recordingId);
        if (!matcher.find()) {
            throw new RuntimeException("Invalid format of recording id: " + recordingId);
        }
        return unmaskSeparator(matcher.group(1));
    }

    /**
     * @param recordingId
     * @return fileId (2nd part of recordingId) from given {@code recordingId}
     */
    private String getFileIdFromRecordingId(String recordingId)
    {
        Matcher matcher = RECORDING_ID_PATTERN.matcher(recordingId);
        if (!matcher.find()) {
            throw new RuntimeException("Invalid format of recording id: " + recordingId);
        }
        return unmaskSeparator(matcher.group(2));
    }

    /**
     * @param recordingId
     * @return tcsRecordingId (1st part of recordingId) from given {@code recordingId}
     */
    private String getRecordingTcsIdFromRecordingId(String recordingId)
    {
        Matcher matcher = RECORDING_ID_PATTERN.matcher(recordingId);
        if (!matcher.find()) {
            throw new RuntimeException("Invalid format of recording id: " + recordingId);
        }
        String result = matcher.group(3);
        if (result != null) {
            return unmaskSeparator(result);
        }
        else {
            return null;
        }
    }

    /**
     * @param recordingName
     * @return dateTime from given {@code recordingName}
     * @throws InvalidFormatException
     */
    private String getSegmentFromRecordingName(String recordingName, int segment) throws InvalidFormatException
    {
        if (recordingsPrefix.length() > 0) {
            if (recordingName.startsWith(recordingsPrefix)) {
                recordingName = recordingName.substring(recordingsPrefix.length());
            }
        }
        Matcher matcher = RECORDING_NAME_PATTERN.matcher(recordingName);
        if (!matcher.matches()) {
            throw new InvalidFormatException("Invalid format of recording name (" + recordingName + ")");
        }
        return unmaskSeparator(matcher.group(segment));
    }

    private String maskSeparator(String text)
    {
        return text.replace(SEPARATOR, SEPARATOR_REPLACEMENT);
    }

    private String unmaskSeparator(String text)
    {
        return text.replace(SEPARATOR_REPLACEMENT, SEPARATOR);
    }

    /**
     * @param recordingName
     * @return recordingFolderId from given {@code recordingName}
     * @throws InvalidFormatException
     */
    private String getRecordingFolderIdFromRecordingName(String recordingName) throws InvalidFormatException
    {
        return getSegmentFromRecordingName(recordingName, 1);
    }

    /**
     * @param recordingName
     * @return alias from given {@code recordingName}
     * @throws InvalidFormatException
     */
    private String getAliasFromRecordingName(String recordingName) throws InvalidFormatException
    {
        return getSegmentFromRecordingName(recordingName, 2);
    }

    /**
     * @param recordingName
     * @return dateTime from given {@code recordingName}
     * @throws InvalidFormatException
     */
    private String getRecordingFileIdFromRecordingName(String recordingName) throws InvalidFormatException
    {
        return getSegmentFromRecordingName(recordingName, 3);
    }

    private String buildXmlTag(String unpairTag)
    {
        return "<" + unpairTag + " />";
    }

    private String buildXmlTag(Map.Entry<String, Object> pairTag)
    {
        StringBuilder tag = new StringBuilder();

        tag.append("<" + pairTag.getKey() + ">");
        tag.append(pairTag.getValue() == null ? "" : pairTag.getValue());
        tag.append("</" + pairTag.getKey() + ">");

        return tag.toString();
    }

    private String buildExecXml(Command command) throws CommandException
    {
        StringBuilder xml = new StringBuilder();

        // Headers
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Body>\n");

        // Body
        if (command.getCommand() == null || command.getCommand().isEmpty()) {
            throw new CommandException("Command cannot be null or empty.");
        }

        xml.append("<" + command.getCommand() + " xmlns=\"http://www.tandberg.net/XML/Streaming/1.0\" >");

        for (Object argument : command.getArguments()) {
            if (!String.class.isInstance(argument) || argument.toString().isEmpty()) {
                throw new CommandException("Command arguments must be String and not empty.");
            }

            xml.append(buildXmlTag((String) argument));
        }

        for (Map.Entry<String, Object> entry : command.getParameters().entrySet()) {
            xml.append(buildXmlTag(entry));
        }

        // Footers
        xml.append("</" + command.getCommand() + ">");

        xml.append("</soap:Body>\n" +
                "</soap:Envelope>");

        return xml.toString();
    }

    /**
     * Remove {@link org.jdom2.Namespace} {@link #NS_NS1}
     *
     * @param xmlData
     * @return xmlData without {@link org.jdom2.Namespace} {@link #NS_NS1}
     */
    private String removeNamespace(String xmlData)
    {
        return xmlData.replaceAll("<" + ns1.getPrefix() + ":", "<").replaceAll("</" + ns1.getPrefix() + ":", "</");
    }

    /**
     * @return set of existing recordingFolderIds
     */
    private Set<String> getRecordingFolderIds()
    {
        Set<String> recordingFolderIds = new HashSet<String>();
        for (Folder folder : metadataStorage.listFolders(null, null)) {
            recordingFolderIds.add(folder.getFolderId());
        }
        return recordingFolderIds;
    }

    /**
     * @param recording
     * @return true whether given {@code recording} is ready to be moved from TCS to storage, false otherwise
     */
    private boolean isTcsRecordingReadyForMoving(Recording recording)
    {
        return recording.getDownloadUrl() != null;
    }

    /**
     * @param recordingId to be moved to appropriate recording folder
     * @return true if recording has been moved successfully
     * @throws CommandException
     */
    private boolean moveRecordingToAppropriateRecordingFolder(String recordingId) throws CommandException
    {
        try {
            String recordingFolderId = getRecordingFolderIdFromRecordingId(recordingId);
            if (!storage.folderExists(recordingFolderId)) {
                Folder folder = new Folder(null, recordingFolderId);
                try {
                    storage.createFolder(folder);
                    setFolderPermissionsFromMetadata(recordingFolderId);
                } catch (FileNotFoundException e) {
                    logger.warn("Cannot create TCS storage directory \"" + folder.getFolderName() + "\" and move recording. Skipping.");
                    sendUnavailableNotification(storage.getUrl() + "/" + folder.getFolderName());

                    //Remove recording from #link{recordingsBeingMoved} till storage is accessible
                    recordingsBeingMoved.remove(recordingId, recordingFolderId);
                    return false;
                } catch (Exception e) {
                    throw new CommandException("Recreating of TCS storage folder \"" + recordingFolderId + "\" failed.",e);
                }
            }
            String recordingTcsId = getRecordingTcsIdFromRecordingId(recordingId);
            Element recordingTcsElement = getTcsRecordingElementRequired(recordingTcsId);
            Recording recording = parseRecording(recordingTcsElement);
            logger.info("Moving recording (id: " + recordingId + ") to recording folder (id: " + recordingFolderId + ")");

            // Create recording file
            File file = new File();
            file.setFileName(recording.getFileName());
            file.setFolderId(recordingFolderId);
            final String recordingUrl = recording.getDownloadUrl();
            final HttpClient httpClient = ConfiguredSSLContext.getInstance().createHttpClient();
            HttpGet request = new HttpGet(recordingUrl);
            HttpResponse response = httpClient.execute(request);
            InputStream inputStream = response.getEntity().getContent();
            storage.createFile(file, inputStream, new ResumeSupport()
            {
                @Override
                public InputStream reopenInputStream(InputStream oldInputStream, int offset) throws IOException
                {
                    // resume input stream for recording data at given offset
                    HttpGet request = new HttpGet(recordingUrl);
                    request.setHeader("Range", "bytes=" + offset + "-");
                    HttpResponse response = httpClient.execute(request);
                    return response.getEntity().getContent();
                }
            });

            updateMetadataFiles(recordingFolderId, recordingId, recordingTcsElement);

            // Delete original recording on TCS
            if (validateRecording(file, recording)) {
                deleteTcsRecording(recordingTcsId);
                return true;
            }
            else {
                NotifyTarget notifyTarget = new NotifyTarget(Service.NotifyTargetType.RESOURCE_ADMINS);
                notifyTarget.addMessage("en",
                        "Downloaded recording is not complete",
                        "Downloaded recording may have encountered some problem. Actual size does not match given on TCS.\n" +
                                "Recording id: " + recordingId +
                                recording.toString());
                notifyTarget.addMessage("cs",
                        "Stazena nahravka neni kompletni",
                        "Velikost stažené nahrávky neodpovídá udávané velikosti na TCS.\n" +
                                "Id nahrávky: " + recordingId +
                                recording.toString());
                try {
                    performControllerAction(notifyTarget);
                }
                catch (CommandException notifyException) {
                    logger.error("Failed to report that moving of recording has failed.", notifyException);
                }
                return false;
            }
        }
        catch (Exception exception) {
            throw new CommandException("Error while moving recording " + recordingId + ".", exception);
        }
        finally {
            recordingsBeingMoved.remove(recordingId);
        }
    }

    private boolean validateRecording(File file, Recording recording)
    {
        Long expectedSize = recording.getSize();
        return storage.validateFile(file, expectedSize);
    }

    private void setFolderPermissionsFromMetadata(String recordingFolderId) throws IOException, CommandException {
        InputStream permissionsData =
                metadataStorage.getFileContent(recordingFolderId, ApacheStorage.PERMISSION_FILE_NAME);
        ((ApacheStorage) storage).setFolderPermissions(recordingFolderId, new StringBuilder(IOUtils.toString(permissionsData)));
    }

    /**
     * Check if all recordings are stored, otherwise move them to appropriate folder (asks controller for folder name)
     *
     * @throws CommandException
     */
    private ExecutorService performCheckRecordings() throws CommandException
    {
        ExecutorService exec = Executors.newFixedThreadPool(NUM_OF_THREADS);
        try {
            List<Recording> recordingsToMove = new LinkedList<Recording>();
            synchronized (CiscoTCSConnector.class) {
                logger.debug("Checking recordings to be moved...");
                List<Recording> recordings = listTcsRecordings("*");
                logger.debug("Checking " + recordings.size() + " recordings...");
                if (recordings.size() > 0) {
                    Set<String> recordingFolderIds = getRecordingFolderIds();
                    for (Recording recording : recordings) {
                        logger.debug("checking recording: " + recording.getName());
                        try {
                            String recordingId = recording.getId();
                            String recordingFolderId = recording.getRecordingFolderId();
                            if (!recordingFolderIds.contains(recordingFolderId)) {
                                continue;
                            }
                            if (!isTcsRecordingReadyForMoving(recording)) {
                                continue;
                            }
                            synchronized (recordingsBeingMoved) {
                                if (recordingsBeingMoved.containsKey(recordingId)) {
                                    continue;
                                }
                                recordingsBeingMoved.put(recordingId, recordingFolderId);
                            }
                            recordingsToMove.add(recording);
                        }
                        catch (Exception exception) {
                            logger.warn("Recordings CheckAndMove failed.", exception);
                            continue;
                        }
                    }
                    logger.debug("Number of recordings to move at the time: " + recordingsBeingMoved.size() + "");
                }
            }

            for (final Recording recording : recordingsToMove) {
                exec.submit(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        String recordingId = recording.getId();

                        try {
                            boolean moved = moveRecordingToAppropriateRecordingFolder(recordingId);
                            if (moved) {
                                Recording movedRecording = getRecording(recordingId);
                                NotifyTarget notifyTarget = new NotifyTarget(Service.NotifyTargetType.REC_FOLDER_OWNERS, recording.getRecordingFolderId());
                                notifyTarget.addMessage("en",
                                        "Your recording was successfully processed",
                                        "Your recording is ready to be downloaded at " + movedRecording.getDownloadUrl() + ".\n");
                                notifyTarget.addMessage("cs",
                                        "Vaše nahrávka byla zpracována",
                                        "Vaše nahrávka je připravena ke stažení na " + movedRecording.getDownloadUrl() + ".\n");
                                try {
                                    performControllerAction(notifyTarget);
                                } catch (CommandException notifyException) {
                                    logger.error("Failed to report that moving of recording has failed.", notifyException);
                                }
                            }

                        }
                        catch (Exception exception) {
                            logger.error("Error while moving recording " + recordingId + ".", exception);
                            String recordingFolderId = getRecordingFolderIdFromRecordingId(recordingId);
                            String recordingTcsId = getRecordingTcsIdFromRecordingId(recordingId);
                            NotifyTarget notifyTarget = new NotifyTarget(Service.NotifyTargetType.RESOURCE_ADMINS);
                            notifyTarget.addMessage("en",
                                    "Moving recording from TCS failed",
                                    "Error ocured while moving recording.\n"
                                            + "Recording TCS ID: " + recordingTcsId + "\n"
                                            + "Recording folder ID: " + recordingFolderId + "\n"
                                            + "Recording filename: " + recording.getFileName() + "\n\n"
                                            + "Thrown exception: " + exception);
                            notifyTarget.addMessage("cs",
                                    "Přesunutí nahrávky z TCS selhalo",
                                    "Nastala chyba při přesouvání nahrávky.\n"
                                            + "TCS ID nahrávky: " + recordingTcsId + "\n"
                                            + "ID složky: " + recordingFolderId + "\n"
                                            + "Název souboru nahrávky: " + recording.getFileName() + "\n\n"
                                            + "Vyhozená výjimka: " + exception);
                            try {
                                performControllerAction(notifyTarget);
                            }
                            catch (CommandException notifyException) {
                                logger.error("Failed to report that moving of recording has failed.", notifyException);
                            }
                        }
                    }
                });
            }
            return exec;
        }
        finally {
            exec.shutdown();
        }
    }

    public class FaultException extends CommandException
    {
        private final String fault;

        public FaultException(String message, String fault)
        {
            super(message + ": " + fault);
            this.fault = fault;
        }

        public String getFault()
        {
            return fault;
        }
    }

    public class InvalidFormatException extends CommandException
    {
        /**
         * Constructor.
         */
        protected InvalidFormatException()
        {
        }

        /**
         * @param message description of the failure
         */
        public InvalidFormatException(String message)
        {
            super(message);
        }

        /**
         * @param message description of the failure
         * @param cause   the cause of the failure
         */
        public InvalidFormatException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}
