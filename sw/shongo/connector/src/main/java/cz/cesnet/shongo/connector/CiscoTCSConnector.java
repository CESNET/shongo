 package cz.cesnet.shongo.connector;


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
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * {@link AbstractConnector} for Cisco TelePresence Content Server
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class CiscoTCSConnector extends AbstractConnector implements RecordingService
{
     private static Logger logger = LoggerFactory.getLogger(AdobeConnectConnector.class);

    /**
     * This is the user log in name, typically the user email address.
     */
    private String login;

    /**
     * The password of the user.
     */
    private String password;

    /**
     *
     */
    private SOAPConnection soapConnection;


    @Override
    public void connect(Address address, String username, String password) throws CommandException
    {
        this.info.setDeviceAddress(address);
        this.login = username;
        this.password = password;

        //TODO: check if accessable
        this.info.setConnectionState(ConnectorInfo.ConnectionState.LOOSELY_CONNECTED);


        // Setup options
        //TODO: what needed

        try {
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            this.soapConnection = soapConnectionFactory.createConnection();

            String url = "https://" + this.info.getDeviceAddress().getHost() + ":" + this.info.getDeviceAddress().getPort() + "/tcs/Helium.wsdl";
        }
        catch (SOAPException e) {
            //TODO: process
            throw new CommandException("TODO: process request exceptions", e);
        }

        this.info.setConnectionState(ConnectorInfo.ConnectionState.CONNECTED);
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
        throw new TodoImplementException("CiscoTCSConnector.createRecordingFolder");
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

    @Override
    public Recording getRecording(String recordingId) throws CommandException, CommandUnsupportedException
    {
        throw new TodoImplementException("CiscoTCSConnector.getRecording");
    }

    @Override
    public Recording getActiveRecording(Alias alias) throws CommandException, CommandUnsupportedException
    {
        throw new TodoImplementException("CiscoTCSConnector.getActiveRecording");
    }

    @Override
    public String startRecording(String folderId, Alias alias) throws CommandException, CommandUnsupportedException
    {
        throw new TodoImplementException("CiscoTCSConnector.startRecording");
    }

    @Override
    public void stopRecording(String recordingId) throws CommandException, CommandUnsupportedException
    {
        throw new TodoImplementException("CiscoTCSConnector.stopRecording");
    }

    @Override
    public void deleteRecording(String recordingId) throws CommandException, CommandUnsupportedException
    {
        throw new TodoImplementException("CiscoTCSConnector.deleteRecording");
    }

    protected String buildXmlTag(String unpairTag)
    {
        return "<" + unpairTag + " />";
    }

    protected String buildXmlTag(Map.Entry<String,Object> pairTag)
    {
        StringBuilder tag = new StringBuilder();

        tag.append("<" + pairTag.getKey() + " >");
        tag.append(pairTag.getValue());
        tag.append("</" + pairTag.getKey() + " >");

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

            xml.append((String) argument);
        }

        for (Map.Entry<String,Object> entry : command.getParameters().entrySet()) {
            xml.append(entry);
        }

        // Footers
        xml.append("</" + command.getCommand() + ">");

        xml.append("</soap:Body>\n" +
                "</soap:Envelope>)");

        return xml.toString();
    }

    protected HttpEntity exec(Command command) throws CommandException{
        try {
            while (true) {

                logger.debug(String.format("%s issuing command '%s' on %s",
                        CiscoTCSConnector.class, command, this.info.getDeviceAddress()));


                //CloseableHttpClient httpclient = HttpClients.createDefault();
                HttpClient lHttpClient = new DefaultHttpClient();
                //lHttpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_0);

                // A org.apache.http.impl.auth.DigestScheme instance is
// what will process the challenge from the web-server
                final DigestScheme md5Auth = new DigestScheme();


                // Setup POST request
                HttpPost lHttpPost = new HttpPost("http://" + this.info.getDeviceAddress().getHost() + ":" + this.info.getDeviceAddress()
                        .getPort() + "/tcs/SoapServer.php");
//        HttpPost lHttpPost = new HttpPost("http://195.113.151.188/tcs/SoapServer.php");

                ConfiguredSSLContext.getInstance().addAdditionalCertificates(lHttpPost.getURI().getHost());

// Set SOAPAction header
                lHttpPost.addHeader("SOAPAction", "http://www.tandberg.net/XML/Streaming/1.0/GetSystemInformation");

                // Add XML to request, direct in the body - no parameter name tcs.getSoapMessage().toString()
                // String xml = builExecdXml(command);
                String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "<soap:Body>\n" +
                        "<GetSystemInformation xmlns=\"http://www.tandberg.net/XML/Streaming/1.0\"/>\n" +
                        "</soap:Body>\n" +
                        "</soap:Envelope>";
                StringEntity lEntity = new StringEntity(xml, "text/xml", "utf-8");
                lHttpPost.setEntity(lEntity);

                // Protocol version should be 1.0 because of compatibility with TCS
                lHttpPost.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_0);
// Execute POST
                HttpResponse authResponse = lHttpClient.execute(lHttpPost);

// This should return an HTTP 401 Unauthorized with
// a challenge to solve.
                //final HttpResponse authResponse = lHttpResponse;

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
//                        new UsernamePasswordCredentials("admin", "nahr8vadloHesl94ko1AP1"),
                                new BasicHttpRequest(HttpPost.METHOD_NAME,"/tcs/SoapServer.php"));

                        // Authentication header as generated by HttpClient.
                        lHttpPost.setHeader(solution);

                        lHttpPost.releaseConnection();

                        System.out.println("===================");
                        System.out.println(lHttpPost.getURI());
                        System.out.println("===================");
                        for (Header header : lHttpPost.getAllHeaders()){
                            System.out.println(header.toString() + " : " + header.getName() + " --- " + header.getValue());
                        }
                        System.out.println("===================");

                        final HttpResponse goodResponse =  lHttpClient.execute(lHttpPost);
                        //doPost(url, postBody, contentType, solution);

                        EntityUtils.toString(goodResponse.getEntity());
                    } else {
                        throw new Error("Web-service responded with Http 401, " +
                                "but didn't send us a usable WWW-Authenticate header.");
                    }
                } else {
                    throw new Error("Didn't get an Http 401 " +
                            "like we were expecting.");
                }

                if (authResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                {
                    throw new RuntimeException("HTTP problems posting method " + authResponse.getStatusLine().getReasonPhrase());
                }

// Get hold of response XML
                String lResponseXml = EntityUtils.toString(authResponse.getEntity());

            }
        } catch (Exception ex) {
            throw new RuntimeException("Command issuing error", ex);
        }
    }

    public static void main(String[] args) throws Exception
    {
        Address address = new Address("195.113.151.188",80);



        CiscoTCSConnector tcs = new CiscoTCSConnector();
        tcs.connect(address, "admin", "nahr8vadloHesl94ko1AP1");

        Command command = new Command("");

        tcs.exec(command);

        tcs.disconnect();

    }

    private static void printSOAPResponse(SOAPMessage soapResponse) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        Source sourceContent = soapResponse.getSOAPPart().getContent();
        System.out.print("\nResponse SOAP Message = ");
        StreamResult result = new StreamResult(System.out);
        transformer.transform(sourceContent, result);
    }

}
