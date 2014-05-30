package cz.cesnet.shongo.connector.device;


import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.common.AbstractDeviceConnector;
import cz.cesnet.shongo.connector.common.Command;
import cz.cesnet.shongo.connector.api.RecordingService;
import cz.cesnet.shongo.connector.api.RecordingSettings;
import cz.cesnet.shongo.connector.storage.*;
import cz.cesnet.shongo.connector.storage.File;
import cz.cesnet.shongo.controller.api.jade.NotifyTarget;
import cz.cesnet.shongo.controller.api.jade.Service;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
     * @see ConnectionState
     */
    private ConnectionState connectionState;

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
     * If recording check is running.
     */
    private volatile boolean recordingChecking = false;

    /**
     * Timeout for checking if recording are in right folder, default value is 5 minutes
     */
    private int recordingsCheckTimeout;

    /**
     * Prefix for recordings.
     */
    private String recordingsPrefix;

    /**
     * Namespace
     */
    private Namespace ns1;

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
    private final Map<String, String> recordingsBeingMoved = new ConcurrentHashMap<String, String>();

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
    public void connect(DeviceAddress deviceAddress, String username, String password) throws CommandException
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
        this.recordingAlias = configuration.getOptionStringRequired("alias");

        String metadataStorage = configuration.getOptionStringRequired("metadata-storage");
        this.metadataStorage = new LocalStorageHandler(metadataStorage);

        String storage = configuration.getOptionStringRequired("storage");
        String downloadableUrlBase = configuration.getOptionStringRequired("downloadable-url-base");
        this.storage = new ApacheStorage(storage, downloadableUrlBase, new AbstractStorage.UserInformationProvider()
        {
            @Override
            public UserInformation getUserInformation(String userId) throws CommandException
            {
                return getUserInformationById(userId);
            }
        });

        this.debug = configuration.getOptionBool("debug");

        checkServerVitality();

        this.connectionState = ConnectionState.LOOSELY_CONNECTED;

        Thread moveRecordingThread = new Thread()
        {
            private Logger logger = LoggerFactory.getLogger(CiscoTCSConnector.class);

            @Override
            public void run()
            {
                setRecordingChecking(true);
                logger.info("Checking of recordings - starting...");
                try {
                    while (isConnected()) {
                        try {
                            Thread.sleep(recordingsCheckTimeout);

                        }
                        catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            continue;
                        }

                        try {
                            performCheckRecordings();
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
        };
        moveRecordingThread.setName(Thread.currentThread().getName() + "-recordings");

        synchronized (this) {
            if (!this.recordingChecking) {
                moveRecordingThread.start();
            }
        }
    }

    @Override
    public ConnectionState getConnectionState()
    {
        return connectionState;
    }

    @Override
    public void disconnect() throws CommandException
    {
        this.connectionState = ConnectionState.DISCONNECTED;
    }

    /**
     * Check if TCS server is online and engine status is OK, otherwise throws CommandException.
     *
     * @throws CommandException thrown if something is wrong
     */
    public void checkServerVitality() throws CommandException
    {
        Command command = new Command("GetSystemInformation");
        Element result = exec(command).getChild("GetSystemInformationResponse");
        if (!"true".equals(result.getChild("GetSystemInformationResult").getChildText("EngineOK"))) {
            this.connectionState = ConnectionState.DISCONNECTED;
            throw new CommandException("Server " + deviceAddress.getHost() + " is not working. Check its status.");
        }
    }

    @Override
    public String createRecordingFolder(RecordingFolder recordingFolder) throws CommandException
    {
        // Folder for recordings
        // TODO: return id even if storage not accessible, info saved in alias
        Folder folder = new Folder(null, recordingFolder.getName());
        String folderId = storage.createFolder(folder);
        storage.setFolderPermissions(folderId, recordingFolder.getUserPermissions());

        // Folder on local storage for metadata
        if (!metadataStorage.createFolder(folder).equals(folderId)) {
            throw new TodoImplementException("Fix folderId for metadata's and recording's folders.");
        }

        return folderId;
    }

    @Override
    public void modifyRecordingFolder(RecordingFolder recordingFolder) throws CommandException
    {
        storage.setFolderPermissions(recordingFolder.getId(), recordingFolder.getUserPermissions());
    }

    @Override
    public void deleteRecordingFolder(String recordingFolderId) throws CommandException
    {
        if (recordingFolderId == null || recordingFolderId.isEmpty()) {
            throw new IllegalArgumentException("Argument recordingFolderId must be not empty.");
        }
        synchronized (CiscoTCSConnector.class) {
            logger.debug("Removing recording folder (" + recordingFolderId + ").");
            storage.deleteFolder(recordingFolderId);
            metadataStorage.deleteFolder(recordingFolderId);

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
                }
                catch (InterruptedException e) {
                    continue;
                }
            }

            // Delete original recordings
            for (Recording recording : listTcsRecordings(recordingFolderId + "*")) {
                String recordingTcsId = getRecordingTcsIdFromRecordingId(recording.getId());
                deleteTcsRecording(recordingTcsId);
            }
        }
    }

    @Override
    public Collection<Recording> listRecordings(String recordingFolderId)
            throws CommandException, CommandUnsupportedException
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
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
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
                    Recording recordingTcs = getTcsRecording(recordingTcsId);
                    recording.setState(recordingTcs.getState());
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
                recording.setState(Recording.State.AVAILABLE);
                recording.setDownloadUrl(storage.getFileDownloadableUrl(recordingFolderId, recording.getFileName()));
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

        Element result = exec(command);
        Element callInfoElement = result.getChild("GetCallInfoResponse").getChild("GetCallInfoResult");
        return "IN_CALL".endsWith(callInfoElement.getChildText("CallState"));
    }

/*    private String createAdHocAlias() throws CommandException
    {
        Command command = new Command("AddRecordingAlias");
        command.setParameter("SourceAlias","999");
        command.setParameter("Data","<Name>123</Name<E164Alias>123</E164Alias>");
        exec(command,true);
        return null;
    }

    private void deleteAlias(String aliasId) throws CommandException
    {
        Command command = new Command("DeleteRecordingAlias");
        command.setParameter("Alias",aliasId);

        exec(command);
    } */

    /**
     * @param recordingFolderId
     * @param alias             alias of an endpoint which should be recorded (it can be a virtual room)
     * @param recordingSettings recording settings
     * @return recordingId is {@link java.lang.String} composed like this "recordingFolderId:fileId:recordingTCSId"
     * @throws CommandException
     */
    @Override
    public String startRecording(String recordingFolderId, Alias alias, RecordingSettings recordingSettings)
            throws CommandException
    {
        if (!alias.getType().equals(AliasType.H323_E164)) {
            throw new TodoImplementException("TODO: implement recording for other aliases than H.323_164.");
        }

        String recordingName = formatRecordingName(recordingFolderId, alias, DateTime.now());
        Command command = new Command("RequestConferenceID");
        command.setParameter("owner", "admin");
        command.setParameter("password", "");
        command.setParameter("startDateTime", "0");
        command.setParameter("duration", "0");
        command.setParameter("title", recordingName);
        command.setParameter("groupId", "");
        command.setParameter("isRecurring", "false");

        String conferenceID = exec(command).getChild("RequestConferenceIDResponse").getChildText(
                "RequestConferenceIDResult");

        command = new Command("Dial");
        command.setParameter("Number", alias.getValue());
        String bitrate = recordingSettings.getBitrate() == null ? recordingDefaultBitrate : recordingSettings.getBitrate();
        command.setParameter("Bitrate", bitrate);
        //TODO: create alias for adhoc recording, find out if necessary
        command.setParameter("Alias", recordingAlias);
        command.setParameter("ConferenceID", conferenceID);
        //TODO: set technology if SIP
        command.setParameter("CallType", "h323");
        command.setParameter("SetMetadata", true);
        command.setParameter("PIN", recordingSettings.getPin());

        Element result = exec(command);
        String recordingTcsId = result.getChild("DialResponse").getChild("DialResult").getChildText("ConferenceID");
        if (recordingTcsId == null) {
            throw new CommandException("No recordingId was returned from dialing.");
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

    @Override
    public void stopRecording(String recordingId) throws CommandException
    {
        Command command = new Command("DisconnectCall");
        command.setParameter("ConferenceID", getRecordingTcsIdFromRecordingId(recordingId));

        exec(command);

        // create metadata file
        String recordingTcsId = getRecordingTcsIdFromRecordingId(recordingId);
        Element originalRecordingElement = getTcsRecordingElementRequired(recordingTcsId);
        createMetadataFile(recordingId, originalRecordingElement);
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
                if (file.getFileName().contains(fileId)) {
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
     * Execute command on Cisco TCS server
     *
     * @param command to execute
     * @param debug
     * @return Document Element
     * @throws CommandException
     */
    private synchronized Element exec(Command command, boolean debug) throws CommandException
    {
        try {
            HttpClient lHttpClient = new DefaultHttpClient();
            while (true) {

                logger.debug(String.format("Issuing command '%s' on %s", command.getCommand(), deviceAddress));

                final ContextAwareAuthScheme md5Auth = new DigestScheme();

                // Setup POST request
                HttpPost lHttpPost = new HttpPost("http://" + deviceAddress + "/tcs/SoapServer.php");

                ConfiguredSSLContext.getInstance().addAdditionalCertificates(lHttpPost.getURI().getHost());

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
                HttpResponse authResponse = lHttpClient.execute(lHttpPost);

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
                                new BasicHttpRequest(HttpPost.METHOD_NAME, "/tcs/SoapServer.php"),
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

                        final HttpResponse goodResponse = lHttpClient.execute(lHttpPost);

                        String resultString = EntityUtils.toString(goodResponse.getEntity());

                        if (debug) {
                            System.out.println("==========");
                            System.out.println("OUTPUT");
                            System.out.println("==========");
                            System.out.println(resultString);
                        }

                        // Remove namespace NS_NS1
                        if (this.ns1 == null) {
                            Document resultDocumentTmp = saxBuilder.build(new StringReader(resultString));
                            Element rootElementTmp = resultDocumentTmp.getRootElement();
                            this.ns1 = rootElementTmp.getNamespace(NS_NS1);
                        }
                        Document resultDocument = saxBuilder.build(new StringReader(removeNamespace(resultString)));
                        Element rootElement = resultDocument.getRootElement();

                        this.connectionState = ConnectionState.LOOSELY_CONNECTED;
                        Namespace envelopeNS = rootElement.getNamespace(NS_ENVELOPE);
                        Element bodyElement = rootElement.getChild("Body", envelopeNS);
                        if (goodResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                            String fault = bodyElement.getChild("Fault", envelopeNS).getChildText("faultstring");
                            throw new FaultException("Command '" + command.getCommand() + "' issuing error: "
                                    + goodResponse.getStatusLine().getReasonPhrase(), fault);
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
            this.connectionState = ConnectionState.DISCONNECTED;
            throw new CommandException("Command '" + command.getCommand() + "' issuing error", exception);
        }
    }

    /**
     * Exec command with debug output to std if set in option file.
     *
     * @param command
     * @return
     * @throws CommandException
     */
    private Element exec(Command command) throws CommandException
    {
        return exec(command, this.debug);
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
     * @param recordingTcsId identifier on Cisco TCS server
     * @return raw recording xml data for recording with given {@code recordingTcsId} or {@code null} if doesn't exist
     */
    private Element getTcsRecordingElement(String recordingTcsId) throws CommandException
    {
        Command command = new Command("GetConference");
        command.setParameter("ConferenceID", recordingTcsId);
        try {
            return exec(command).getChild("GetConferenceResponse").getChild("GetConferenceResult");
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
        exec(command);
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

        Element result = exec(command).getChild("GetConferencesResponse");

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
    private void createMetadataFile(String recordingId, Element recordingTcsElement) throws CommandException
    {
        String fileId = getFileIdFromRecordingId(recordingId);
        String folderId = getRecordingFolderIdFromRecordingId(recordingId);

        File metadataFile = new File();
        metadataFile.setFileName(getMetadataFilename(fileId));
        metadataFile.setFolderId(folderId);

        storage.createFile(metadataFile,
                new ByteArrayInputStream(xmlOutputter.outputString(recordingTcsElement).getBytes()));
        metadataStorage.createFile(metadataFile,
                new ByteArrayInputStream(xmlOutputter.outputString(recordingTcsElement).getBytes()));
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
    private static String formatRecordingFileId(DateTime dateTime)
    {
        return FILE_ID_DATE_TIME_FORMATTER.print(dateTime);
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
        recordingIdBuilder.append(recordingFolderId.replaceAll("_", "__"));
        recordingIdBuilder.append("_");
        recordingIdBuilder.append(recordingFileId.replaceAll("_", "__"));
        recordingIdBuilder.append("_");
        recordingIdBuilder.append(recordingTcsId.replaceAll("_", "__"));
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
    private String formatRecordingName(String recordingFolderId, Alias alias, DateTime recordingStartedAt)
    {
        return formatRecordingName(recordingFolderId, alias.getValue(), formatRecordingFileId(recordingStartedAt));
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
        recordingNameBuilder.append(recordingFolderId.replaceAll("_", "__"));
        recordingNameBuilder.append("_");
        recordingNameBuilder.append(alias.replaceAll("_", "__"));
        recordingNameBuilder.append("_");
        recordingNameBuilder.append(recordingFileId.replaceAll("_", "__"));
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
        return matcher.group(1).replaceAll("__", "_");
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
        return matcher.group(2).replaceAll("__", "_");
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
            return result.replaceAll("__", "_");
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
        return matcher.group(segment).replaceAll("__", "_");
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
     * @throws CommandException
     */
    private void moveRecordingToAppropriateRecordingFolder(String recordingId) throws CommandException
    {
        try {
            String recordingFolderId = getRecordingFolderIdFromRecordingId(recordingId);
            String recordingTcsId = getRecordingTcsIdFromRecordingId(recordingId);
            Element recordingTcsElement = getTcsRecordingElementRequired(recordingTcsId);
            Recording recording = parseRecording(recordingTcsElement);
            logger.info("Moving recording (id: " + recordingId + ") to recording folder (id: " + recordingFolderId + ")");

            // Create recording file
            File file = new File();
            file.setFileName(recording.getFileName());
            file.setFolderId(recordingFolderId);
            final String recordingUrl = recording.getDownloadUrl();
            final HttpClient httpClient = new DefaultHttpClient();
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

            // Delete existing and create new metadata file
            try {
                storage.deleteFile(recordingFolderId, getMetadataFilename(getFileIdFromRecordingId(recordingId)));
                metadataStorage.deleteFile(recordingFolderId,
                        getMetadataFilename(getFileIdFromRecordingId(recordingId)));
            }
            catch (Exception e) {
                logger.warn("Deleting of temporary metadata file failed (recording ID: " + recordingId + ")", e);
            }
            createMetadataFile(recordingId, recordingTcsElement);

            // Delete original recording on TCS
            deleteTcsRecording(recordingTcsId);

            recordingsBeingMoved.remove(recordingId);
        }
        catch (Exception exception) {
            throw new CommandException("Error while moving recording " + recordingId + ".", exception);
        }
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
                if (recordings.size() > 0) {
                    Set<String> recordingFolderIds = getRecordingFolderIds();
                    for (Recording recording : recordings) {
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
                            moveRecordingToAppropriateRecordingFolder(recordingId);
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
