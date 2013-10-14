 package cz.cesnet.shongo.connector;

import com.sun.xml.internal.messaging.saaj.SOAPExceptionImpl;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.DeviceLoadInfo;
import cz.cesnet.shongo.api.Recording;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.connector.api.ConnectorInfo;
import cz.cesnet.shongo.connector.api.RecordingService;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpRequest;
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
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException, CommandUnsupportedException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String startRecording(String folderId, Alias alias) throws CommandException, CommandUnsupportedException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void stopRecording(String recordingId) throws CommandException, CommandUnsupportedException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Recording getRecording(String recordingId) throws CommandException, CommandUnsupportedException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<Recording> listRecordings(String folderId) throws CommandException, CommandUnsupportedException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void deleteRecording(String recordingId) throws CommandException, CommandUnsupportedException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void moveRecording(String recordingId, String folderId) throws CommandException, CommandUnsupportedException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    protected void request(String action, Map<String, String> attributes) throws CommandException
    {
        try {
            SOAPMessage soapMessage = this.soapConnection.call(createSOAPRequest(),new Object());
        }
        catch (SOAPException e) {
            throw new CommandException("TODO: deal with this exception in request()" ,e);
        }
    }

    protected SOAPMessage createSOAPRequest() throws SOAPException
    {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();

        String serverURI = "https://" + this.info.getDeviceAddress().getHost() + ":" + this.info.getDeviceAddress().getPort() + "/tcs/SoapServer.php";

        ConfiguredSSLContext.getInstance().addAdditionalCertificates(info.getDeviceAddress().getHost());

        // SOAP Envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.setPrefix("soap");
        envelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
        envelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");

        /*
        Constructed SOAP Request Message:
        <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:example="http://ws.cdyne.com/">
            <SOAP-ENV:Header/>
            <SOAP-ENV:Body>
                <example:VerifyEmail>
                    <example:email>mutantninja@gmail.com</example:email>
                    <example:LicenseKey>123</example:LicenseKey>
                </example:VerifyEmail>
            </SOAP-ENV:Body>
        </SOAP-ENV:Envelope>
         */

        // SOAP Body
        SOAPBody soapBody = envelope.getBody();

        // set header
        SOAPElement header = soapBody.addBodyElement(new QName("Header"));

        SOAPElement security = header.addChildElement(new QName("Security"));
        SOAPElement usernameToken= security.addChildElement(new QName("UsernameToken"));
        SOAPElement username= usernameToken.addChildElement(new QName("Username"));
        SOAPElement password= usernameToken.addChildElement(new QName("Password"));

        //enter the username and password
        /*username.addTextNode("opicak");
        password.addTextNode("kalamita42");
        */
        SOAPElement soapBodyElem = soapBody.addChildElement("RequestConferenceID");
        soapBodyElem.addNamespaceDeclaration("test","http://www.tandberg.net/XML/Streaming/1.0");

        //soapBodyElem.addAttribute("xmlns","http://www.tandberg.net/XML/Streaming/1.0");
        SOAPElement soapBodyElem1 = soapBodyElem.addChildElement("email");
        soapBodyElem1.addTextNode("mutantninja@gmail.com");
        SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("LicenseKey");
        soapBodyElem2.addTextNode("123");

        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", serverURI  + "VerifyEmail");

        soapMessage.saveChanges();

        /* Print the request message */
        System.out.print("Request SOAP Message = ");
        try {
            soapMessage.writeTo(System.out);
        }
        catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return soapMessage;
    }

    public static void main(String[] args) throws Exception
    {
        Address address = new Address("195.113.151.188",443);

        //ConfiguredSSLContext.getInstance().addAdditionalCertificates(address.getHost());


        CiscoTCSConnector tcs = new CiscoTCSConnector();
        tcs.connect(address, "opicak", "kalamita42");
        /*
        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection soapConnection = soapConnectionFactory.createConnection();

        String url = "https://" + address.getHost() + ":" + address.getPort() + "/tcs/Helium.wsdl";
        String tcsUrl = "https://" + address.getHost() + ":" + address.getPort() + "/tcs/SoapServer.php";

        try {
            URL link = new URL("http","195.113.151.188","/tcs/SoapServer.php");
            SOAPMessage message = tcs.createSOAPRequest();
            System.out.println(message.getMimeHeaders().getAllHeaders().next());
            SOAPMessage response = soapConnection.call(message, link);
            response.getContentDescription();
        } catch (SOAPExceptionImpl ex) {
            ex.printStackTrace();
        }

//        printSOAPResponse(response);

        soapConnection.close();
        */


        HttpClient lHttpClient = new DefaultHttpClient();

        // A org.apache.http.impl.auth.DigestScheme instance is
// what will process the challenge from the web-server
        final DigestScheme md5Auth = new DigestScheme();

// Setup proxy server to call through
//        HttpHost lProxy = new HttpHost("hostname", 1234);
//        lHttpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, lProxy);

        // Setup POST request
        HttpPost lHttpPost = new HttpPost("http://195.113.151.188/tcs/SoapServer.php");

// Set SOAPAction header
        lHttpPost.addHeader("SOAPAction", "http://www.tandberg.net/XML/Streaming/1.0/RequestConferenceID");

        // Add XML to request, direct in the body - no parameter name
        StringEntity lEntity = new StringEntity(tcs.getSoapMessage().toString(), "text/xml", "utf-8");
        lHttpPost.setEntity(lEntity);

        System.out.println(lHttpPost.toString());
// Execute POST
        HttpResponse lHttpResponse = lHttpClient.execute(lHttpPost);

// This should return an HTTP 401 Unauthorized with
// a challenge to solve.
        final HttpResponse authResponse = lHttpResponse;

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
                        new UsernamePasswordCredentials("opicak", "kalamita42"),
                        new BasicHttpRequest(HttpPost.METHOD_NAME,new URL("https://195.113.151.188/tcs/SoapServer.php").getPath()));

                // Authentication header as generated by HttpClient.
                lHttpPost.setHeader(solution);

                final HttpResponse goodResponse =  new DefaultHttpClient().execute(lHttpPost);
                        //doPost(url, postBody, contentType, solution);

                System.out.println(goodResponse);
            } else {
                throw new Error("Web-service responded with Http 401, " +
                        "but didn't send us a usable WWW-Authenticate header.");
            }
        } else {
            throw new Error("Didn't get an Http 401 " +
                    "like we were expecting.");
        }

        if (lHttpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
        {
            throw new RuntimeException("HTTP problems posting method " + lHttpResponse.getStatusLine().getReasonPhrase());
        }

// Get hold of response XML
        String lResponseXml = EntityUtils.toString(lHttpResponse.getEntity());



    }

    private static void printSOAPResponse(SOAPMessage soapResponse) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        Source sourceContent = soapResponse.getSOAPPart().getContent();
        System.out.print("\nResponse SOAP Message = ");
        StreamResult result = new StreamResult(System.out);
        transformer.transform(sourceContent, result);
    }







private static final String TECHNET_NAMESPACE_PREFIX = "shongo";
private static final String WEBSERVICE_SECURE_URL =
"https://195.113.151.188:443/tcs/SoapServer.php";
private static final String WEBSERVICE_INSECURE_URL =
"http://195.113.151.188:443/tcs/SoapServer.php";


/**
* Get the login token
*
* @param username
* @param password
* @return The authentication ticket
* @throws SOAPException
*/
private String login( final String username, final String password) throws SOAPException {
final SOAPMessage soapMessage = getSoapMessage();
final SOAPBody soapBody = soapMessage.getSOAPBody();
final SOAPElement loginElement = soapBody.addChildElement("Login");

loginElement.addChildElement("Username").addTextNode(username);
loginElement.addChildElement("Password").addTextNode(password);

soapMessage.saveChanges();
    System.out.println("sjem tu");
final SOAPConnection soapConnection = getSoapConnection();
    System.out.println("sjem tu");

    final SOAPMessage soapMessageReply = soapConnection.call(soapMessage,WEBSERVICE_INSECURE_URL);
    System.out.println("sjem tu");

final String textContent = soapMessageReply.getSOAPHeader().getFirstChild().getTextContent();

soapConnection.close();

return textContent;
}

/**
* Returns the price
*
* @param authenticationTicket
* @param shape
* @param size
* @param color
* @param clarity
* @throws SOAPException
*
private void getPrice( final String authenticationTicket, final String shape, final float size, final String color,
final String clarity) throws SOAPException {
final SOAPMessage soapMessage = getSoapMessage();

addAuthenticationTicket(authenticationTicket, soapMessage);

final SOAPBody soapBody = soapMessage.getSOAPBody();
final SOAPElement getPriceElement = soapBody.addChildElement("GetPrice", TECHNET_NAMESPACE_PREFIX);
getPriceElement.addChildElement("shape", TECHNET_NAMESPACE_PREFIX).addTextNode(shape);
getPriceElement.addChildElement("size", TECHNET_NAMESPACE_PREFIX).addTextNode(String.valueOf(size));
getPriceElement.addChildElement("color", TECHNET_NAMESPACE_PREFIX).addTextNode(color);
getPriceElement.addChildElement("clarity", TECHNET_NAMESPACE_PREFIX).addTextNode(clarity);

soapMessage.saveChanges();

final SOAPConnection soapConnection = getSoapConnection();

final SOAPMessage soapMessageReply = soapConnection.call(soapMessage,WEBSERVICE_INSECURE_URL);

final SOAPBody replyBody = soapMessageReply.getSOAPBody();
final Node getPriceResponse = replyBody.getFirstChild();
final Node getPriceResult = getPriceResponse.getFirstChild();

final NodeList childNodes = getPriceResult.getChildNodes();
final String replyShape = childNodes.item(0).getTextContent();
final String lowSize = childNodes.item(1).getTextContent();

// ... etc etc
// You can create a bean that will encompass all elements

soapConnection.close();
}

/**
* Gets the price sheet
*
* @param authenticationTicket
* @param shapes
* @throws SOAPException
* @throws TransformerException
*
private void getPriceSheet( final String authenticationTicket, final Shapes shapes)
throws SOAPException, TransformerException {
final SOAPMessage soapMessage = getSoapMessage();

addAuthenticationTicket(authenticationTicket, soapMessage);

final SOAPBody soapBody = soapMessage.getSOAPBody();

final SOAPElement getPriceSheetElement =
soapBody.addChildElement("GetPriceSheet", TECHNET_NAMESPACE_PREFIX);

getPriceSheetElement.addChildElement(
"shape", TECHNET_NAMESPACE_PREFIX).addTextNode(shapes.enumString);

soapMessage.saveChanges();

final SOAPConnection soapConnection = getSoapConnection();
final SOAPMessage soapMessageReply = soapConnection.call(soapMessage, WEBSERVICE_INSECURE_URL);

// this will print out the result
// Create transformer

final TransformerFactory tff = TransformerFactory.newInstance();
final Transformer tf = tff.newTransformer();

// Get reply content
final Source sc = soapMessageReply.getSOAPPart().getContent();

// Set output transformation
final StreamResult result = new StreamResult(System.out);
tf.transform(sc, result);
System.out.println();

// Close connection
soapConnection.close();
}

/**
* Create a SOAP Connection
*
* @return
* @throws UnsupportedOperationException
* @throws SOAPException
*/
private SOAPConnection getSoapConnection() throws UnsupportedOperationException, SOAPException {
final SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
final SOAPConnection soapConnection = soapConnectionFactory.createConnection();

return soapConnection;
}

/**
* Create the SOAP Message
*
* @return
* @throws SOAPException
*/
private SOAPMessage getSoapMessage() throws SOAPException {
final MessageFactory messageFactory = MessageFactory.newInstance();
final SOAPMessage soapMessage = messageFactory.createMessage();

// Object for message parts
final SOAPPart soapPart = soapMessage.getSOAPPart();
final SOAPEnvelope envelope = soapPart.getEnvelope();
                                                                                       /*
envelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
envelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
envelope.addNamespaceDeclaration("enc", "http://schemas.xmlsoap.org/soap/encoding/");
envelope.addNamespaceDeclaration("env", "http://schemas.xmlsoap.org/soap/envelop/"); */
envelope.addNamespaceDeclaration("URL", WEBSERVICE_SECURE_URL);


// add the technet namespace as "technet"
envelope.addNamespaceDeclaration(TECHNET_NAMESPACE_PREFIX, "http://technet.rapaport.com/");

envelope.setEncodingStyle("http://schemas.xmlsoap.org/soap/encoding/");

return soapMessage;
}

private void addAuthenticationTicket( final String authenticationTicket, final SOAPMessage soapMessage)
throws SOAPException {
final SOAPHeader header = soapMessage.getSOAPHeader();
final SOAPElement authenticationTicketHeader =
header.addChildElement("AuthenticationTicketHeader", TECHNET_NAMESPACE_PREFIX);
authenticationTicketHeader.addChildElement(
"Ticket", TECHNET_NAMESPACE_PREFIX).addTextNode(authenticationTicket);
}


}
