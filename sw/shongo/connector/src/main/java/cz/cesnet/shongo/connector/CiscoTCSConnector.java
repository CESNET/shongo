 package cz.cesnet.shongo.connector;


import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.DeviceLoadInfo;
import cz.cesnet.shongo.api.Recording;
import cz.cesnet.shongo.api.RecordingFolder;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.connector.api.ConnectorInfo;
import cz.cesnet.shongo.connector.api.RecordingService;
import cz.cesnet.shongo.connector.api.RecordingSettings;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
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

import java.io.StringReader;
import java.util.Collection;
import java.util.Map;

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
    private final String DEFAULT_BITRATE = "768";


    @Override
    public void connect(Address address, String username, String password) throws CommandException
    {
        this.info.setDeviceAddress(address);
        this.login = username;
        this.password = password;

        Command command = new Command("GetSystemInformation");
        Element result = exec(command);
        Namespace ns = result.getNamespace(NS_NS1);
        if (!"true".equals(result.getChild("GetSystemInformationResponse",ns).getChild("GetSystemInformationResult",ns).getChildText("EngineOK",ns)))
        {
            this.info.setConnectionState(ConnectorInfo.ConnectionState.DISCONNECTED);

            throw new CommandException("Server " + this.info.getDeviceAddress().getHost() + " is not working. Check it's status.");
        }

        this.info.setConnectionState(ConnectorInfo.ConnectionState.LOOSELY_CONNECTED);
    }

    @Override
    public void disconnect() throws CommandException
    {
        this.info.setConnectionState(ConnectorInfo.ConnectionState.DISCONNECTED);
    }

    @Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException, CommandUnsupportedException
    {
        throw new TodoImplementException("CiscoTCSConnector.getDeviceLoadInfo");
    }

    @Override
    public String createRecordingFolder(RecordingFolder recordingFolder) throws CommandException
    {
        FTPClient ftp = new FTPClient();
        /*ftp.connect();
        ftp.login();

        ftp.makeDirectory("" + recordingFolder.getName());

        ftp.disconnect();*/
        // throw new TodoImplementException("CiscoTCSConnector.createRecordingFolder");
        return "";
    }

    @Override
    public void modifyRecordingFolder(RecordingFolder recordingFolder) throws CommandException
    {
        throw new TodoImplementException("CiscoTCSConnector.modifyRecordingFolder");
    }

    @Override
    public void deleteRecordingFolder(String recordingFolderId) throws CommandException
    {
        throw new TodoImplementException("CiscoTCSConnector.deleteRecordingFolder");
    }

    @Override
    public Collection<Recording> listRecordings(String folderId) throws CommandException, CommandUnsupportedException
    {
        throw new TodoImplementException("CiscoTCSConnector.listRecordings");
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
        Command command = new Command("GetConference");
        command.setParameter("ConferenceID",recordingId);

        Element result = exec(command);
        Namespace ns = result.getNamespace(NS_NS1);
        Element recordingData = result.getChild("GetConferenceResponse",ns).getChild("GetConferenceResult",ns);

        return parseRecording(recordingData, ns);
    }

    @Override
    public Recording getActiveRecording(Alias alias) throws CommandException
    {
        Command command = new Command("GetConferences");
        command.setParameter("SearchExpression","999");
        command.setParameter("ResultRange",null);
        command.setParameter("DateTime",null);
        command.setParameter("UpdateTime",null);
        command.setParameter("Owner",null);
        command.setParameter("Category",null);
        command.setParameter("Sort","DateTime");

        Element result = exec(command,true);
        Namespace ns = result.getNamespace(NS_NS1);
        for (Element recordingData : result.getChild("GetConferencesResponse",ns).getChild("GetConferencesResult",ns).getChildren("Conference",ns)) {
            System.out.println(parseRecording(recordingData,ns));
        }

        System.out.println(
                result.getChild("GetConferencesResponse", ns).getChild("GetConferencesResult", ns).getChildren().size());
        System.out.println(exec(new Command("GetConferenceCount"),true));

        Recording recording = new Recording();
        return recording;
    }

    @Override
    public String startRecording(String folderId, Alias alias, RecordingSettings recordingSettings)
            throws CommandException
    {
        Command command = new Command("Dial");
        command.setParameter("Number", alias.getValue());
        command.setParameter("Bitrate", DEFAULT_BITRATE);
        //TODO: set aliasis
        command.setParameter("Alias", "999");
        //TODO: set technology
        command.setParameter("CallType", "h323");
        command.setParameter("SetMetadata", true);

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

    private void moveRecording(String recordingId, String recordingFolderId) throws CommandException
    {
        //TODO:
    }

    @Override
    public void deleteRecording(String recordingId) throws CommandException
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


    protected Element exec(Command command, boolean debug) throws CommandException{
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
                if(authResponse.getStatusLine().getStatusCode() ==
                        HttpStatus.SC_UNAUTHORIZED) {
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

                        if (goodResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                            throw new CommandException("HTTP problems posting method " + authResponse.getStatusLine().getReasonPhrase());
                        }

                        String resultString = EntityUtils.toString(goodResponse.getEntity());

                        if (debug) {
                            System.out.println("==========");
                            System.out.println("OUTPUT");
                            System.out.println("==========");
                            System.out.println(resultString);
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
                    throw new Error("Didn't get an Http 401 " +
                            "like we were expecting.");
                }
            }
        } catch (Exception ex) {
            this.info.setConnectionState(ConnectorInfo.ConnectionState.DISCONNECTED);
            throw new RuntimeException("Command issuing error", ex);
        }
    }

    protected Element exec(Command command) throws CommandException{
        return exec(command,false);
    }

    public static void main(String[] args) throws Exception
    {
        Address address = new Address("195.113.151.188",80);

        CiscoTCSConnector tcs = new CiscoTCSConnector();
        tcs.connect(address, "admin", "nahr8vadloHesl94ko1AP1");

        Alias alias = new Alias(AliasType.H323_E164,"950087999");

        //String id = tcs.startRecording(null, alias, null);
        //Thread.sleep(20000);

        tcs.getActiveRecording(alias);

        //Thread.sleep(10000);
        //tcs.stopRecording(id);
        //Thread.sleep(10000);
        //tcsording(id);

        tcs.disconnect();

    }
}
