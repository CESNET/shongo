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
import cz.cesnet.shongo.controller.api.jade.GetUserInformation;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.net.ftp.FTPClientConfig;
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
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
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
     *
     */
    private Storage storage;


    @Override
    public void connect(Address address, String username, String password) throws CommandException
    {
        this.info.setDeviceAddress(address);
        this.login = username;
        this.password = password;

        if (getOption("default-bitrate") != null) {
            this.DEFAULT_BITRATE = getOption("default-bitrate");
        }

        if (getOption("alias") != null) {
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
            System.out.println("FILE: " + file.getFileName());
            if (!file.getFileName().startsWith(".")) {
                Recording recording = new Recording();
                recording.setName(file.getFileName());
                //TODO: read metadata
                //recording.setDownloadableUrl(storage.);
                recordings.add(recording);
            }
        }

        return Collections.unmodifiableList(recordings);
    }

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
        }

        return recording;
    }

    @Override
    public Recording getRecording(String recordingId) throws CommandException
    {
        //TODO: get metadata
        //recordingId: folderid:20140122T133001:tcsid

        throw new TodoImplementException("TODO: implement CiscoTCSConnector.getRecording()");
    }

    /**
     * Returns recordings info from TCS server.
     * @param recordingId
     * @return
     * @throws CommandException
     */
    public Recording getOriginalRecording(String recordingId) throws CommandException
    {
        Command command = new Command("GetConference");
        command.setParameter("ConferenceID", recordingId);

        Element result = exec(command,true);
        Namespace ns = result.getNamespace(NS_NS1);
        Element recordingData = result.getChild("GetConferenceResponse",ns).getChild("GetConferenceResult",ns);

        return parseRecording(recordingData, ns);
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
        command.setParameter("ConferenceID",recordingId);

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
        return result.getChild("DialResponse", ns).getChild("DialResult",ns).getChildText("ConferenceID",ns);
    }

    @Override
    public void stopRecording(String recordingId) throws CommandException
    {
        Command command = new Command("DisconnectCall");
        command.setParameter("ConferenceID",recordingId);

        exec(command);
    }

    protected void moveRecording(String recordingId, String recordingFolderId) throws CommandException
    {
        try {
            Recording recording = getRecording(recordingId);
            Storage.File file = new Storage.File();
            file.setFileName(recording.getName());
            file.setFolderId(recordingFolderId);

            storage.createFile(file,new URL(recording.getDownloadableUrl()).openStream());
            //TODO: metadata file
        }
        catch (IOException e) {
            throw new CommandException("I/O Exception while downloading recording from TCS server.",e);
        }
    }

    @Override
    public void deleteRecording(String recordingId) throws CommandException
    {
        //TODO: mazat z uloziste
        deleteOriginalRecording(recordingId);
    }

    protected void deleteOriginalRecording(String recordingId) throws CommandException
    {
        Command command = new Command("DeleteRecording");
        command.setParameter("conferenceID",recordingId);

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

                        Document resultDocument = new SAXBuilder().build(new StringReader(resultString));
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
