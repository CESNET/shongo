 package cz.cesnet.shongo.connector;


import com.sun.prism.impl.Disposer;
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
import cz.cesnet.shongo.controller.api.jade.GetRecordingFolderId;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import org.apache.http.*;
import org.apache.http.auth.ContextAwareAuthScheme;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.tika.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.*;

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
     * FTP Client for storing movies
     */
    private FTPClient ftpClient;

    /**
     * Namespace constant for Cisco TCS
     */
    private String NS_ENVELOPE = "SOAP-ENV";

    /**
     * Namespace constant for Cisco TCS
     */
    private String NS_NS1 = "ns1";

    /**
     * Default bitrate for recordings.
     */
    private String DEFAULT_BITRATE = "768";


    /**
     * TCS Alias for shongo recordings.
     */
    private String ALIAS;

    /**
     * Storage unit for recordings
     */
    private Storage storage;

    public Storage getStorage() {return storage;};

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


        this.ALIAS = "999";
        storage = new ApacheStorage("/tmp/shongo",new AbstractStorage.UserInformationProvider()
        {
            @Override
            public UserInformation getUserInformation(String userId) throws CommandException
            {
                return getUserInformationById(userId);
            }
        } );


/*        if (getOption("alias") != null) {
            this.ALIAS = getOption("alias");
        } else {
            throw new RuntimeException("Option alias must be set in connector config file.");
        }

        if (getOption("storage-url") == null) {
            throw new RuntimeException("Option storage-url must be set in connector config file.");
        }


            storage = new ApacheStorage(getOption("storage-url"),new AbstractStorage.UserInformationProvider()
        {
            @Override
            public UserInformation getUserInformation(String userId) throws CommandException
            {
                return getUserInformationById(userId);
            }
        } );
        */

        checkServerVitality();

        this.info.setConnectionState(ConnectorInfo.ConnectionState.LOOSELY_CONNECTED);
    }

    @Override
    public void disconnect() throws CommandException
    {
        this.info.setConnectionState(ConnectorInfo.ConnectionState.DISCONNECTED);
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
        Namespace ns = result.getNamespace(NS_NS1);
        if (!"true".equals(result.getChild("GetSystemInformationResponse",ns).getChild("GetSystemInformationResult",ns).getChildText("EngineOK",ns)))
        {
            this.info.setConnectionState(ConnectorInfo.ConnectionState.DISCONNECTED);

            throw new CommandException("Server " + this.info.getDeviceAddress().getHost() + " is not working. Check its status.");
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
        Storage.Folder folder = new Storage.Folder(null,recordingFolder.getName());
        String folderId = storage.createFolder(folder);
        storage.setFolderPermissions(folderId,recordingFolder.getUserPermissions());

        return folderId;
    }

    @Override
    public void modifyRecordingFolder(RecordingFolder recordingFolder) throws CommandException
    {
        storage.setFolderPermissions(recordingFolder.getId(),recordingFolder.getUserPermissions());
    }

    @Override
    public void deleteRecordingFolder(String recordingFolderId) throws CommandException
    {
        storage.deleteFolder(recordingFolderId);
    }

    @Override
    public Collection<Recording> listRecordings(String folderId) throws CommandException, CommandUnsupportedException
    {
        List<Recording> recordings = new ArrayList<Recording>();
        for (Storage.File file : storage.listFiles(folderId,null))
        {
            if (!file.getFileName().startsWith(".")) {
                Recording recording = getRecording(folderId+":"+file.getFileName().split("\\.")[0]+":");
                recordings.add(recording);
            }
        }

        return Collections.unmodifiableList(recordings);
    }

    /**
     *  Returns xml {@link org.jdom2.Element} of recording info
     * @param recordingTCSId identifier on Cisco TCS server
     * @return
     * @throws CommandException
     */
    protected Element getRecordingRawData(String recordingTCSId) throws CommandException
    {
        Command command = new Command("GetConference");
        command.setParameter("ConferenceID", recordingTCSId);

        Element result = exec(command);
        Namespace ns = result.getNamespace(NS_NS1);
        return result;
    }

    /**
     * Convers from recording info from source {@link org.jdom2.Element} to {@link cz.cesnet.shongo.api.Recording}
     * @param recordingData raw xml data in {@link org.jdom2.Element}
     * @param ns namespace
     * @return recording info
     */
    protected Recording parseRecording(Element recordingData, Namespace ns)
    {
        Recording recording = new Recording();
        recording.setId(recordingData.getChildText("ConferenceID", ns));
        recording.setName(recordingData.getChildText("Title", ns));
        recording.setBeginDate(new DateTime(Long.decode(recordingData.getChildText("DateTime", ns))*1000));
        recording.setDuration(new Period(Long.decode(recordingData.getChildText("Duration",ns)).longValue()));
        //TODO: recording.setDescription(recordingData.getChildText("Description",ns));
        recording.setUrl(recordingData.getChildText("URL",ns));
        if ("true".equals(recordingData.getChildText("HasDownloadableMovie", ns))) {
            recording.setDownloadableUrl(recordingData.getChild("DownloadableMovies",ns).getChild("DownloadableMovie",ns).getChildText(
                    "URL", ns));
            String extension = recording.getDownloadableUrl().split("\\.")[recording.getDownloadableUrl().split("\\.").length - 1];
            recording.setFileName(DateTimeFormat.forPattern("YYYYMMddHHmmss").print(recording.getBeginDate()) + "." + extension);
        }

        return recording;
    }

    @Override
    public Recording getRecording(String recordingId) throws CommandException
    {
        String folderId = selectFolderId(recordingId);
        String fileId = selectFileId(recordingId);

        storage.getFileContent(folderId,"." + fileId + ".data");
        Recording recording = new Recording();

        //recording.setDownloadableUrl(storage.getFileDownloadableUrl(folderId, fileId));
        throw new TodoImplementException("TODO: implement CiscoTCSConnector.getRecording()");
    }

    /**
     * Returns recordings info from TCS server.
     * @param recordingTCSId
     * @return
     * @throws CommandException
     */
    public Recording getOriginalRecording(String recordingTCSId) throws CommandException
    {
        Element result = getRecordingRawData(recordingTCSId);
        Namespace ns = result.getNamespace(NS_NS1);
        return parseRecording(result.getChild("GetConferenceResponse", ns).getChild("GetConferenceResult", ns), ns);
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
        command.setParameter("ConferenceID",selectRecordingTCSId(recordingId));

        Element result = exec(command);

        Namespace ns = result.getNamespace(NS_NS1);
        return result.getChild("GetCallInfoResponse",ns).getChild("GetCallInfoResult",ns).getChildText("CallState",ns).equals("IN_CALL");
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
     *
     * @param folderId
     * @param alias             alias of an endpoint which should be recorded (it can be a virtual room)
     * @param recordingSettings recording settings
     * @return recordingId is {@link java.lang.String} composed like this "recordingFolderId:fileId:recordingTCSId", where fileId is begining of the recording in format "YYYYMMddHHmmss"
     * @throws CommandException
     */
    @Override
    public String startRecording(String folderId, Alias alias, RecordingSettings recordingSettings)
            throws CommandException
    {
        Command command = new Command("Dial");
        command.setParameter("Number", alias.getValue());
        String bitrate = recordingSettings.getBitrate() == null ? DEFAULT_BITRATE : recordingSettings.getBitrate();
        command.setParameter("Bitrate", bitrate);
        //TODO: create alias for adhoc recording, find out if necessary
        command.setParameter("Alias", ALIAS);
        //TODO: set technology if SIP
        command.setParameter("CallType", "h323");
        command.setParameter("SetMetadata", true);
        command.setParameter("PIN",recordingSettings.getPin());

        Element result = exec(command);

        Namespace ns = result.getNamespace(NS_NS1);
        String recordingTCSId = result.getChild("DialResponse", ns).getChild("DialResult",ns).getChildText("ConferenceID",ns);
        String fileId = DateTimeFormat.forPattern("YYYYMMddHHmmss").print(getOriginalRecording(recordingTCSId).getBeginDate());
        return folderId + ":" + fileId + ":" + recordingTCSId;
    }

    /**
     * Returns fileId - 2nd part of recordingId
     * @param recordingId
     * @return
     */
    protected String selectFileId(String recordingId)
    {
        return recordingId.split(":")[1];
    }

    /**
     * Returns folderId - 1st part of recordingId
     * @param recordingId
     * @return
     */
    protected String selectFolderId(String recordingId)
    {
        return recordingId.split(":")[0];
    }

    /**
     * Returns recordingTCSId - 3rd part of recordingId
     * @param recordingId
     * @return
     */
    protected String selectRecordingTCSId(String recordingId)
    {
        return recordingId.split(":")[2];
    }

    @Override
    public void stopRecording(String recordingId) throws CommandException
    {
        Command command = new Command("DisconnectCall");
        command.setParameter("ConferenceID",selectRecordingTCSId(recordingId));

        exec(command);
    }

    protected void moveRecording(String recordingId, String recordingFolderId) throws CommandException
    {
        try {
            Element result = getRecordingRawData(selectRecordingTCSId(recordingId));
            Namespace ns = result.getNamespace(NS_NS1);
            Recording recording = parseRecording(
                    result.getChild("GetConferenceResponse", ns).getChild("GetConferenceResult", ns), ns);

            Storage.File file = new Storage.File();
            file.setFileName(recording.getFileName());
            file.setFolderId(recordingFolderId);

            Storage.File metadata = new Storage.File();
            metadata.setFileName("." + selectFileId(recordingId) + ".data");
            metadata.setFolderId(recordingFolderId);

            // metadata file
            storage.createFile(metadata, new ByteArrayInputStream(xmlOutputter.outputString(result).getBytes()));
            // recording
            storage.createFile(file,new URL(recording.getDownloadableUrl()).openStream());
        }
        catch (IOException e) {
            throw new CommandException("I/O Exception while downloading recording from TCS server.",e);
        }
    }

    @Override
    public void deleteRecording(String recordingId) throws CommandException
    {
        String folderId = selectFolderId(recordingId);
        String fileId = selectFileId(recordingId);
        String recordingTCSId = selectRecordingTCSId(recordingId);
        //TODO: delete all files storage.deleteFile(folderId,fileId);
        for(Storage.File file:storage.listFiles(folderId,fileId)) {
            System.out.println(file.getFileName());
            //storage.deleteFile(folderId,fileId);
        }
        //deleteOriginalRecording(tcsId);
    }

    protected void deleteOriginalRecording(String recordingId) throws CommandException
    {
        Command command = new Command("DeleteRecording");
        command.setParameter("conferenceID",selectRecordingTCSId(recordingId));

        exec(command);
    }

    protected String buildXmlTag(String unpairTag)
    {
        return "<" + unpairTag + " />";
    }

    protected String buildXmlTag(Map.Entry<String,Object> pairTag)
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

        for (Map.Entry<String,Object> entry : command.getParameters().entrySet()) {
            xml.append(buildXmlTag(entry));
        }

        // Footers
        xml.append("</" + command.getCommand() + ">");

        xml.append("</soap:Body>\n" +
                "</soap:Envelope>");

        return xml.toString();
    }


    protected Element exec(Command command, boolean debug) throws CommandException
    {
        try {
            while (true) {

                logger.debug(String.format("%s issuing command '%s' on %s",
                        CiscoTCSConnector.class, command.getCommand(), this.info.getDeviceAddress()));

                HttpClient lHttpClient = new DefaultHttpClient();

                final ContextAwareAuthScheme md5Auth = new DigestScheme();


                // Setup POST request
                HttpPost lHttpPost = new HttpPost("http://" + this.info.getDeviceAddress().getHost() + ":" + this.info.getDeviceAddress()
                        .getPort() + "/tcs/SoapServer.php");

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
                if(authResponse.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                    if(authResponse.containsHeader("WWW-Authenticate")) {
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
                                new BasicHttpRequest(HttpPost.METHOD_NAME,"/tcs/SoapServer.php"),
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

                        final HttpResponse goodResponse =  lHttpClient.execute(lHttpPost);

                        String resultString = EntityUtils.toString(goodResponse.getEntity());

                        if (debug) {
                            System.out.println("==========");
                            System.out.println("OUTPUT");
                            System.out.println("==========");
                            System.out.println(resultString);
                        }

                        if (goodResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                            throw new CommandException("HTTP problems posting method " + authResponse.getStatusLine().getReasonPhrase());
                        }

                        Document resultDocument = saxBuilder.build(new StringReader(resultString));
                        Element rootElement = resultDocument.getRootElement();

                        this.info.setConnectionState(ConnectorInfo.ConnectionState.LOOSELY_CONNECTED);

                        Namespace envelopeNS = rootElement.getNamespace(NS_ENVELOPE);
                        return rootElement.getChild("Body",envelopeNS);
                    } else {
                        throw new Error("Web-service responded with Http 401, " +
                                "but didn't send us a usable WWW-Authenticate header.");
                    }
                } else {
                    throw new Error("Didn't get an Http 401 like we were expecting, but (" + authResponse.getStatusLine().getStatusCode() + ").");
                }
            }
        } catch (Exception ex) {
            this.info.setConnectionState(ConnectorInfo.ConnectionState.DISCONNECTED);
            throw new CommandException("Command issuing error", ex);
        }
    }

    protected Element exec(Command command) throws CommandException{
        return exec(command,false);
    }

    public static void main(String[] args) throws Exception
    {
        Address address = new Address("195.113.151.188",80);

        CiscoTCSConnector tcs = new CiscoTCSConnector();
        tcs.connect(address, "user", "pass");


        tcs.disconnect();

    }
}
