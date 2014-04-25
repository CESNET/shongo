package cz.cesnet.shongo.connector;


import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.connector.api.ConnectorInfo;
import cz.cesnet.shongo.connector.api.RecordingService;
import cz.cesnet.shongo.connector.api.RecordingSettings;
import cz.cesnet.shongo.connector.storage.AbstractStorage;
import cz.cesnet.shongo.connector.storage.ApacheStorage;
import cz.cesnet.shongo.connector.storage.Storage;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link AbstractConnector} for Cisco TelePresence Content Server
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class CiscoTCSConnector extends AbstractConnector implements RecordingService
{
    private static Logger logger = LoggerFactory.getLogger(CiscoTCSConnector.class);

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
     * Pattern for recording ID, which is in format recordingFolderId:recordingFileId:recordingTCSId
     */
    private final Pattern RECORDING_ID_PATTERN = Pattern.compile("(.*[^:]):([^:].*[^:]):([^:].*)");

    /**
     * Pattern for recording title stored on TCS
     */
    private final Pattern RECORDING_TCS_TITLE_PATTERN = Pattern.compile("^\\[(.*):flr:(.*);alias:.*;created:.*\\]$");

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
    private final long RECORDING_CHECK_TIMEOUT = Duration.standardMinutes(5).getMillis();

    /**
     * Namespace
     */
    private Namespace ns1;

    /**
     * Default bitrate for recordings.
     */
    private String DEFAULT_BITRATE = "768";

    /**
     * Recordings prefix.
     */
    private String RECORDINGS_PREFIX = "";

    /**
     * TCS Alias for shongo recordings.
     */
    private String ALIAS;

    /**
     * for debuging TCS date flow
     */

    private boolean DEBUG = false;

    /**
     * Storage unit for recordings
     */
    private AbstractStorage storage;

    /**
     * Concurrent list of recordings to be moved to storage.
     */
    private CopyOnWriteArrayList<Recording> recordingsToMove = new CopyOnWriteArrayList<Recording>();


    public Storage getStorage()
    {
        return storage;
    }

    SAXBuilder saxBuilder = new SAXBuilder();
    XMLOutputter xmlOutputter = new XMLOutputter();

    @Override
    public void connect(Address address, String username, String password) throws CommandException
    {
        this.info.setDeviceAddress(address);
        this.login = username;
        this.password = password;

        if (getOption("default-bitrate") != null) {
            this.DEFAULT_BITRATE = getOption("default-bitrate");
        }

        if (getOption("recordings-prefix") != null) {
            this.RECORDINGS_PREFIX = getOption("recordings-prefix");
        }

        if (getOption("alias") != null) {
            this.ALIAS = getOption("alias");
        }
        else {
            throw new RuntimeException("Option alias must be set in connector config file.");
        }

        if (getOption("storage-url") == null) {
            throw new RuntimeException("Option storage-url must be set in connector config file.");
        }

        if (getOption("downloadable-url-base") == null) {
            throw new RuntimeException("Option storage-url must be set in connector config file.");
        }

        if (getOption("debug") != null) {
            this.DEBUG = new Boolean(getOption("debug"));
        }

        storage = new ApacheStorage(getOption("storage-url"), getOption("downloadable-url-base"),
                new AbstractStorage.UserInformationProvider()
                {
                    @Override
                    public UserInformation getUserInformation(String userId) throws CommandException
                    {
                        return getUserInformationById(userId);
                    }
                });

        checkServerVitality();

        this.info.setConnectionState(ConnectorInfo.ConnectionState.LOOSELY_CONNECTED);


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
                            Thread.sleep(RECORDING_CHECK_TIMEOUT);

                        }
                        catch (InterruptedException e) {
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
                }
                finally {
                    setRecordingChecking(false);
                }
            }
        };

        synchronized (this) {
            if (!this.recordingChecking) {
               moveRecordingThread.start();
            }
        }
    }

    @Override
    public void disconnect() throws CommandException
    {
        this.info.setConnectionState(ConnectorInfo.ConnectionState.DISCONNECTED);
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

    /**
     * Check if TCS server is online and engine status is OK, otherwise throws CommandException.
     *
     * @throws CommandException thrown if something is wrong
     */
    public void checkServerVitality() throws CommandException
    {
        Command command = new Command("GetSystemInformation");
        Element result = exec(command);
        if (!"true".equals(result.getChild("GetSystemInformationResponse")
                .getChild("GetSystemInformationResult").getChildText("EngineOK"))) {
            this.info.setConnectionState(ConnectorInfo.ConnectionState.DISCONNECTED);

            throw new CommandException(
                    "Server " + this.info.getDeviceAddress().getHost() + " is not working. Check its status.");
        }
    }

    @Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException, CommandUnsupportedException
    {
        throw new TodoImplementException("CiscoTCSConnector.getDeviceLoadInfo");
    }

    @Override
    public String createRecordingFolder(RecordingFolder recordingFolder) throws CommandException
    {
        //TODO: return id even if storage not accessible, info saved in alias
        Storage.Folder folder = new Storage.Folder(null, recordingFolder.getName());
        String folderId = storage.createFolder(folder);
        storage.setFolderPermissions(folderId, recordingFolder.getUserPermissions());

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
        if (recordingFolderId == null) {
            throw new IllegalArgumentException("deleteRecordingFolder() argument recordingFolderId cannot be null.");
        }
        if (recordingFolderId.isEmpty()) {
            throw new IllegalArgumentException("deleteRecordingFolder() argument recordingFolderId cannot be empty.");
        }
        synchronized (CiscoTCSConnector.class) {
            logger.debug("Removing recording folder (" + recordingFolderId + ").");
            storage.deleteFolder(recordingFolderId);

            // Stop moving recordings and delete them
            for (Recording recording : recordingsToMove) {
                if (!recordingFolderId.equals(recording.getRecordingFolderId())) {
                    continue;
                }
                while (recordingsToMove.contains(recording)) {
                    try {
                        Thread.sleep(100);
                        logger.debug("Waiting to recordingToMove (R: " + recording + ")");
                    }
                    catch (InterruptedException e) {
                        continue;
                    }
                }
            }

            // Delete original recordings
            for (Recording recording : getOriginalRecordingsByName(":flr:" + recordingFolderId)) {
                deleteOriginalRecording(recording.getId());
            }
        }
    }

    @Override
    public Collection<Recording> listRecordings(String folderId) throws CommandException, CommandUnsupportedException
    {
        List<Recording> recordings = new ArrayList<Recording>();
        for (Storage.File file : storage.listFiles(folderId, null)) {
            if (isMetadataFilename(file.getFileName())) {
                Recording recording = getRecording(makeRecordingId(folderId, getFileId(file.getFileName()), "null"));
                recordings.add(recording);
            }
        }

        return Collections.unmodifiableList(recordings);
    }

    /**
     * Returns xml {@link org.jdom2.Element} of recording info
     *
     * @param recordingTCSId identifier on Cisco TCS server
     * @return
     * @throws CommandException
     */
    protected Element getRecordingRawData(String recordingTCSId) throws CommandException
    {
        Command command = new Command("GetConference");
        command.setParameter("ConferenceID", recordingTCSId);

        return exec(command).getChild("GetConferenceResponse").getChild("GetConferenceResult");
    }

    /**
     * Convers from recording info from source {@link org.jdom2.Element} to {@link cz.cesnet.shongo.api.Recording}
     *
     * @param recordingData raw xml data in {@link org.jdom2.Element}
     * @return recording info
     */
    protected Recording parseRecording(Element recordingData) throws InvalidFormatException
    {
        Recording recording = new Recording();

        recording.setBeginDate(new DateTime(Long.decode(recordingData.getChildText("DateTime")) * 1000));
        String fileId = getFileId(recording.getBeginDate());

        recording.setName(recordingData.getChildText("Title"));

        String folderId = selectFolderIdFromTitle(recording.getName());

        recording.setRecordingFolderId(folderId);
        recording.setId(makeRecordingId(folderId, fileId, recordingData.getChildText("ConferenceID")));
        recording.setDuration(new Period(Long.decode(recordingData.getChildText("Duration")).longValue()));
        if ("true".equals(recordingData.getChildText("HasDownloadableMovie"))) {
            recording.setDownloadUrl(
                    recordingData.getChild("DownloadableMovies").getChild("DownloadableMovie").getChildText("URL"));
            String downloadUrl = recording.getDownloadUrl();
            String[] downloadUrlParts = downloadUrl.split("\\.");
            String extension = downloadUrlParts[downloadUrlParts.length - 1];
            recording.setFileName(fileId + "." + extension);
        }

        return recording;
    }

    @Override
    public Recording getRecording(String recordingId) throws CommandException
    {
        String recordingXml = null;
        try {
            String folderId = selectFolderId(recordingId);
            String fileId = selectFileId(recordingId);

            InputStream inputStream = storage.getFileContent(folderId, getMetadataFilename(fileId));
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
            recordingXml = inputStringBuilder.toString();
            Document resultDocument = saxBuilder.build(new StringReader(recordingXml));
            Element rootElement = resultDocument.getRootElement();
            Recording recording = parseRecording(rootElement);
            if (recording.getDownloadUrl() != null) {
                recording.setDownloadUrl(storage.getFileDownloadableUrl(folderId, recording.getFileName()));
            }
            recording.setRecordingFolderId(selectFolderId(recordingId));

            return recording;
        }
        catch (IOException e) {
            throw new RuntimeException(
                    "Error while reading file " + selectFolderId(recordingId) + "/" + getMetadataFilename(
                            selectFileId(recordingId)) + ".".replaceAll(
                            "//", "/"));
        }
        catch (JDOMException e) {
            throw new RuntimeException(
                    "Error while parsing file " + selectFolderId(recordingId) + "/" + getMetadataFilename(
                            selectFileId(recordingId)) + ".".replaceAll("//", "/"));
        }
    }

    /**
     * Returns recordings info from TCS server.
     *
     * @param recordingTCSId
     * @return
     * @throws CommandException
     */
    public Recording getOriginalRecording(String recordingTCSId) throws CommandException
    {
        try {
            Element result = getRecordingRawData(recordingTCSId);
            return parseRecording(result);
        } catch (InvalidFormatException ex) {
            throw new CommandException(ex.getMessage(),ex);
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
        command.setParameter("ConferenceID", selectRecordingTCSId(recordingId));

        Element result = exec(command);

        return "IN_CALL".endsWith(
                result.getChild("GetCallInfoResponse").getChild("GetCallInfoResult").getChildText("CallState"));
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
     * @param folderId
     * @param alias             alias of an endpoint which should be recorded (it can be a virtual room)
     * @param recordingSettings recording settings
     * @return recordingId is {@link java.lang.String} composed like this "recordingFolderId:fileId:recordingTCSId"
     * @throws CommandException
     */
    @Override
    public String startRecording(String folderId, Alias alias, RecordingSettings recordingSettings)
            throws CommandException
    {
        if (!alias.getType().equals(AliasType.H323_E164)) {
            throw new TodoImplementException("TODO: implement recording for other aliases than H.323_164.");
        }

        Command command = new Command("RequestConferenceID");
        command.setParameter("owner", "admin");
        command.setParameter("password", "");
        command.setParameter("startDateTime", "0");
        command.setParameter("duration", "0");
        command.setParameter("title", makeRecordingTitle(folderId,alias.getValue()));
        command.setParameter("groupId", "");
        command.setParameter("isRecurring", "false");

        String conferenceID = exec(command).getChild("RequestConferenceIDResponse").getChildText(
                "RequestConferenceIDResult");

        command = new Command("Dial");
        command.setParameter("Number", alias.getValue());
        String bitrate = recordingSettings.getBitrate() == null ? DEFAULT_BITRATE : recordingSettings.getBitrate();
        command.setParameter("Bitrate", bitrate);
        //TODO: create alias for adhoc recording, find out if necessary
        command.setParameter("Alias", ALIAS);
        command.setParameter("ConferenceID", conferenceID);
        //TODO: set technology if SIP
        command.setParameter("CallType", "h323");
        command.setParameter("SetMetadata", true);
        command.setParameter("PIN", recordingSettings.getPin());

        Element result = exec(command);

        String recordingTCSId = result.getChild("DialResponse").getChild("DialResult").getChildText("ConferenceID");

        return getOriginalRecording(recordingTCSId).getId();
    }

    @Override
    public void stopRecording(String recordingId) throws CommandException
    {
        Command command = new Command("DisconnectCall");
        command.setParameter("ConferenceID", selectRecordingTCSId(recordingId));

        exec(command);

        // create metadata file
        Element recordingRawData = getRecordingRawData(selectRecordingTCSId(recordingId));
        createMetadataFile(recordingId, recordingRawData);
    }

    protected void moveRecording(String recordingId) throws CommandException
    {
        try {
            final HttpClient httpClient = new DefaultHttpClient();

            String recordingFolderId = selectFolderId(recordingId);
            logger.info("Moving recording (id: " + recordingId + ") to folder (id: " + recordingFolderId + ")");

            Element recordingXmlData = getRecordingRawData(selectRecordingTCSId(recordingId));
            Recording recording = parseRecording(recordingXmlData);

            Storage.File file = new Storage.File();
            file.setFileName(recording.getFileName());
            file.setFolderId(recordingFolderId);

            // create recording file
            final String recordingUrl = recording.getDownloadUrl();
            HttpGet request = new HttpGet(recordingUrl);
            HttpResponse response = httpClient.execute(request);
            InputStream inputStream = response.getEntity().getContent();
            storage.createFile(file, inputStream, new Storage.ResumeSupport()
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

            // delete existing and create new metadata file
            try {
            storage.deleteFile(recordingFolderId, getMetadataFilename(selectFileId(recordingId)));
            } catch (Exception e) {
                logger.warn("Deleting of temporary metadata file failed (recording ID: " + recordingId + ")",e);
            }
            createMetadataFile(recordingId, recordingXmlData);
        }
        catch (IOException e) {
            throw new CommandException("I/O Exception while downloading recording from TCS server.", e);
        }
        logger.debug("Deleting original recording from TCS (ID: " + recordingId + ").");
        deleteOriginalRecording(recordingId);
    }

    @Override
    public void deleteRecording(String recordingId) throws CommandException
    {
        String folderId = selectFolderId(recordingId);
        String fileId = selectFileId(recordingId);
        String recordingTCSId = selectRecordingTCSId(recordingId);
        for (Storage.File file : storage.listFiles(folderId, fileId)) {
            if (file.getFileName().contains(fileId)) {
                storage.deleteFile(folderId, file.getFileName());
            }
        }
    }

    /**
     * Delete recording from Cisco TCS server
     *
     * @param recordingId identifier of the recording to delete
     * @throws CommandException
     */
    protected void deleteOriginalRecording(String recordingId) throws CommandException
    {
        Command command = new Command("DeleteRecording");
        command.setParameter("conferenceID", selectRecordingTCSId(recordingId));

        exec(command);
    }

    protected String buildXmlTag(String unpairTag)
    {
        return "<" + unpairTag + " />";
    }

    protected String buildXmlTag(Map.Entry<String, Object> pairTag)
    {
        StringBuilder tag = new StringBuilder();

        tag.append("<" + pairTag.getKey() + ">");
        tag.append(pairTag.getValue() == null ? "" : pairTag.getValue());
        tag.append("</" + pairTag.getKey() + ">");

        return tag.toString();
    }

    protected String builExecdXml(Command command) throws CommandException
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
     * Execute command on Cisco TCS server
     *
     * @param command to execute
     * @param debug
     * @return Document Element
     * @throws CommandException
     */
    protected synchronized Element exec(Command command, boolean debug) throws CommandException
    {
        try {
            HttpClient lHttpClient = new DefaultHttpClient();
            while (true) {

                logger.debug(String.format("%s issuing command '%s' on %s",
                        CiscoTCSConnector.class, command.getCommand(), this.info.getDeviceAddress()));

                final ContextAwareAuthScheme md5Auth = new DigestScheme();

                // Setup POST request
                Address address = this.info.getDeviceAddress();
                HttpPost lHttpPost = new HttpPost(
                        "http://" + address.getHost() + ":" + address.getPort() + "/tcs/SoapServer.php");

                ConfiguredSSLContext.getInstance().addAdditionalCertificates(lHttpPost.getURI().getHost());

                // Set SOAPAction header
                lHttpPost.addHeader("SOAPAction", "http://www.tandberg.net/XML/Streaming/1.0/GetSystemInformation");

                // Add XML to request, direct in the body - no parameter name
                String xml = builExecdXml(command);

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

                        if (goodResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                            throw new CommandException(
                                    "HTTP problems posting method " + authResponse.getStatusLine().getReasonPhrase());
                        }

                        // Remove namespace NS_NS1
                        if (this.ns1 == null) {
                            Document resultDocumentTmp = saxBuilder.build(new StringReader(resultString));
                            Element rootElementTmp = resultDocumentTmp.getRootElement();
                            this.ns1 = rootElementTmp.getNamespace(NS_NS1);
                        }
                        Document resultDocument = saxBuilder.build(new StringReader(removeNamespace(resultString)));
                        Element rootElement = resultDocument.getRootElement();

                        this.info.setConnectionState(ConnectorInfo.ConnectionState.LOOSELY_CONNECTED);
                        Namespace envelopeNS = rootElement.getNamespace(NS_ENVELOPE);
                        return rootElement.getChild("Body", envelopeNS);
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
        catch (Exception ex) {
            this.info.setConnectionState(ConnectorInfo.ConnectionState.DISCONNECTED);
            throw new CommandException("Command issuing error", ex);
        }
    }

    /**
     * Exec command with debug output to std if set in option file.
     *
     * @param command
     * @return
     * @throws CommandException
     */
    protected Element exec(Command command) throws CommandException
    {
        return exec(command, this.DEBUG);
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
     * Returns filename for metadata file.
     *
     * @param fileId file identifier of the recording
     * @return
     */
    protected String getMetadataFilename(String fileId)
    {
        return "." + fileId + ".xml";
    }

    /**
     * Return {@code true} if filename is in metadata filename format, {@code false} otherwise
     *
     * @param filename
     * @return
     */
    protected boolean isMetadataFilename(String filename)
    {
        return filename.startsWith(".") && filename.endsWith(".xml");
    }

    /**
     * Returns fileId for given filename
     *
     * @param filename
     * @return
     */
    protected String getFileId(String filename)
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
     * Returns fileId for given dateTime
     *
     * @param dateTime
     * @return
     */
    public static String getFileId(DateTime dateTime)
    {
        return FILE_ID_DATE_TIME_FORMATTER.print(dateTime);
    }

    /**
     * Save metadata of recording to storage.
     *
     * @param recordingId      identifier of the recording
     * @param recordingXmlData recording xml metadata
     * @throws CommandException
     */
    protected void createMetadataFile(String recordingId, Element recordingXmlData) throws CommandException
    {
        String fileId = selectFileId(recordingId);
        String folderId = selectFolderId(recordingId);

        Storage.File metadataFile = new Storage.File();
        metadataFile.setFileName(getMetadataFilename(fileId));
        metadataFile.setFolderId(folderId);

        storage.createFile(metadataFile,
                new ByteArrayInputStream(xmlOutputter.outputString(recordingXmlData).getBytes()));
    }

    /**
     * Returns folderId - 1st part of recordingId
     *
     * @param recordingId
     * @return
     */
    protected String selectFolderId(String recordingId)
    {
        Matcher matcher = RECORDING_ID_PATTERN.matcher(recordingId);
        if (!matcher.find()) {
            throw new RuntimeException("Invalid format of recordingId: " + recordingId);
        }
        return matcher.group(1).replaceAll("::", ":");
    }

    /**
     * Returns fileId - 2nd part of recordingId
     *
     * @param recordingId
     * @return
     */
    protected String selectFileId(String recordingId)
    {
        Matcher matcher = RECORDING_ID_PATTERN.matcher(recordingId);
        if (!matcher.find()) {
            throw new RuntimeException("Invalid format of recordingId: " + recordingId);
        }
        return matcher.group(2).replaceAll("::", ":");
    }

    /**
     * Returns recordingTCSId - 3rd part of recordingId
     *
     * @param recordingId
     * @return
     */
    protected String selectRecordingTCSId(String recordingId)
    {
        Matcher matcher = RECORDING_ID_PATTERN.matcher(recordingId);
        if (!matcher.find()) {
            throw new RuntimeException("Invalid format of recordingId: " + recordingId);
        }
        return matcher.group(3).replaceAll("::", ":");
    }

    /**
     * Contruct recording ID from separate segments
     *
     * @param folderId
     * @param fileId
     * @param recordingTCSId
     * @return
     */
    protected String makeRecordingId(String folderId, String fileId, String recordingTCSId)
    {
        StringBuilder recordingIdBuilder = new StringBuilder();
        recordingIdBuilder.append(folderId.replaceAll(":", "::"));
        recordingIdBuilder.append(":");
        recordingIdBuilder.append(fileId.replaceAll(":", "::"));
        recordingIdBuilder.append(":");
        recordingIdBuilder.append(recordingTCSId.replaceAll(":", "::"));
        return recordingIdBuilder.toString();
    }

    protected String selectFolderIdFromTitle(String title) throws InvalidFormatException
    {
        Matcher matcher = RECORDING_TCS_TITLE_PATTERN.matcher(title);
        if (!matcher.find()) {
            throw new InvalidFormatException("Invalid format of recording title (" + title + ")");
        }
        return matcher.group(2);
    }

    protected String makeRecordingTitle(String folderId, String alias)
    {
        StringBuilder titleBuilder = new StringBuilder();
        titleBuilder.append("[");
        titleBuilder.append(RECORDINGS_PREFIX);
        titleBuilder.append(":flr:");
        titleBuilder.append(folderId);
        titleBuilder.append(";alias:");
        titleBuilder.append(alias);
        titleBuilder.append(";created:");
        titleBuilder.append(getFileId(DateTime.now()));
        titleBuilder.append("]");

        return titleBuilder.toString();
    }

    /**
     * Returns List of recordings by given regex (regex according Cisco TelePresence Content Server documentation of function GetConferences)
     *
     * @return
     */
    protected List<Recording> getOriginalRecordingsByName(String regex) throws CommandException
    {
        Command command = new Command("GetConferences");
        command.setParameter("SearchExpression", regex);
        command.setParameter("ResultRange", "");
        command.setParameter("DateTime", "");
        command.setParameter("UpdateTime", "");
        command.setParameter("Owner", "");
        command.setParameter("Category", "");
        command.setParameter("Sort", "DateTime");

        Element result = exec(command);

        ArrayList<Recording> recordings = new ArrayList<Recording>();
        for (Element rawRecording : result.getChild("GetConferencesResponse").getChild("GetConferencesResult").getChildren("Conference")) {
            try {
                recordings.add(parseRecording(rawRecording));
            } catch (InvalidFormatException ex) {
                logger.error("Failed to parse recording title.",ex);
                continue;
            }
        }

        return recordings;
    }

    /**
     * Check if all recordings are stored, otherwise move them to appropriate folder (asks controller for folder name)
     *
     * @throws CommandException
     */
    protected void checkRecordings() throws CommandException
    {
        ExecutorService exec = Executors.newFixedThreadPool(NUM_OF_THREADS);
        try {
            List<Recording> recordingsToCheck = new LinkedList<Recording>();

            synchronized (CiscoTCSConnector.class) {
                logger.debug("Checking recordings to be moved...");
                List<Recording> allRecordings = getOriginalRecordingsByName(":flr:");

                Set<String> existingFolderNames = new HashSet<String>();
                for (Storage.Folder folder : storage.listFolders(null, null)) {
                    existingFolderNames.add(folder.getFolderId());
                }

                for (Recording recording : allRecordings) {
                    try{
                        if (recording.getDownloadUrl() == null) {
                            continue;
                        }
                        if (recordingsToMove.contains(recording)) {
                            continue;
                        }
                        if (!existingFolderNames.contains(recording.getRecordingFolderId())) {
                            continue;
                        }

                        recordingsToCheck.add(recording);
                        recordingsToMove.add(recording);
                    } catch (Exception e) {
                        logger.warn("Recordings CheckAndMove failed.", e);
                        continue;
                    }
                }
            }

            for (final Recording recording : recordingsToCheck) {
                exec.submit(new Runnable()
                {
                    @Override
                    public void run()
                    {
                            String recordingId = recording.getId();

                            try {
                                moveRecording(recordingId);
                                recordingsToMove.remove(recording);
                            }
                            catch (Exception exception) {
                                logger.error("Error while moving recording (recordingId: " + recordingId + ").",
                                        exception);

                                NotifyTarget notifyTarget = new NotifyTarget(Service.NotifyTargetType.RESOURCE_ADMINS,
                                        null);
                                notifyTarget.addMessage("en",
                                        "Moving recording from TCS failed",
                                        "Error ocured while moving recording.\n"
                                                + "Recording TCS ID: " + selectRecordingTCSId(recordingId) + "\n"
                                                + "Recording folder ID: " + selectFolderId(recordingId) + "\n"
                                                + "Recording filename: " + recording.getFileName() + "\n\n"
                                                + "Thrown exception: " + exception);
                                notifyTarget.addMessage("cs",
                                        "Přesunutí nahrávky z TCS selhalo",
                                        "Nastala chyba při přesouvání nahrávky.\n"
                                                + "TCS ID nahrávky: " + selectRecordingTCSId(recordingId) + "\n"
                                                + "ID složky: " + selectFolderId(recordingId) + "\n"
                                                + "Název souboru nahrávky: " + recording.getFileName() + "\n\n"
                                                + "Vyhozená výjimka: " + exception);

                                try {
                                    performControllerAction(notifyTarget);
                                }
                                catch (CommandException e1) {
                                    logger.error("Failure report sending of recording moving has failed.", e1);
                                }
                            }
                    }
                });
            }
        }
        finally {
            exec.shutdown();
        }

    }

    private synchronized void setRecordingChecking(boolean value)
    {
        this.recordingChecking = value;
    }

    private class InvalidFormatException extends CommandException
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
