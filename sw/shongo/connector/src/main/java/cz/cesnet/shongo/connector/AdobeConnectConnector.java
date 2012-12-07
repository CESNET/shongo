package cz.cesnet.shongo.connector;

import com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl;
import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.connector.api.*;
import cz.cesnet.shongo.util.HostTrustManager;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.JDOMParseException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;


/**
 * Created with IntelliJ IDEA.
 * User: opicak
 * Date: 10/9/12
 * Time: 1:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class AdobeConnectConnector extends AbstractConnector implements MultipointService
{
    private static Logger logger = LoggerFactory.getLogger(AdobeConnectConnector.class);

    /**
     * Root folder for meetings
     */
    protected String meetingsFolderID;


    /**
     * This is the user log in name, typically the user email address.
     */
    private String login;

    /**
     * The password of the user.
     */
    private String password;

    /**
     * The Java session ID that is generated upon successful login.  All calls
     * except login must provide this ID for authentication.
     */
    protected String breezesession;

    /*
     * @param serverUrl     the base URL of the Breeze server, including the
     *                      trailing slash http://www.breeze.example/ is a typical
     *                      example.  Most Breeze installations will not need any
     *                      path except that of the host.
     * @param login         the login of the user as whom the adapter will act on
     *                      the Breeze system.  This is often an administrator but
     *                      Breeze will properly apply permissions for any user.
     * @param password      The password of the user who's logging in.
     * @param breezesession The Java session ID created by the Breeze server upon
     *                      successful login.
     *
    public AdobeConnectConnector()
    {
        this.serverUrl = serverUrl;
        this.login = login;
        this.password = password;
        this.breezesession = breezesession;
    }*/

    @java.lang.Override
    public void connect(Address address, String username, String password) throws CommandException
    {
        this.info.setDeviceAddress(address);
        this.login = username;
        this.password = password;

        this.login();

        this.info.setConnectionState(ConnectorInfo.ConnectionState.LOOSELY_CONNECTED);
    }

    @java.lang.Override
    public void disconnect() throws CommandException
    {
        this.info.setConnectionState(ConnectorInfo.ConnectionState.DISCONNECTED);
        this.logout();
    }

     /**
      *  Creates Adobe Connect user.
      *
      @return user identification (principal-id)
      */
    protected String createAdobeConnectUser(String eppn) throws CommandException
    {
        HashMap<String, String> userSearchAttributes = new HashMap<String, String>();
        userSearchAttributes.put("filter-login",eppn);
        Element principalList = request("principal-list", userSearchAttributes);

        if (principalList.getChild("principal-list").getChild("principal") != null) {
            String principalId = principalList.getChild("principal-list").getChild("principal").getAttributeValue("principal-id");
            return principalId;
        }

        HashMap<String, String> newUserAttributes = new HashMap<String, String>();
        newUserAttributes.put("first-name", "test");
        newUserAttributes.put("last-name", "test");
        newUserAttributes.put("login", eppn);
//        newUserAttributes.put("email", "");
        newUserAttributes.put("type", "user");
        newUserAttributes.put("has-children", "false");

        Element response = request("principal-update", newUserAttributes);
        return response.getChild("principal").getAttributeValue("principal-id");
    }

    /**
     * This method is not supported, cause the AC XML API (secret one) is not working
     *
     * @throws CommandUnsupportedException
     */
    @java.lang.Override
    public void muteParticipant(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(
                "Adobe Connect does not support this function. This setting is accessible in Adobe Connect virtual room.");
    }

    /**
     * This method is not supported, cause the AC XML API (secret one) is not working
     *
     * @throws CommandUnsupportedException
     */    @java.lang.Override
    public void unmuteParticipant(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(
                "Adobe Connect does not support this function. This setting is accessible in Adobe Connect virtual room.");
    }

    @java.lang.Override
    public void setParticipantMicrophoneLevel(String roomId, String roomUserId, int level)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(
                "Adobe Connect does not support changing microphone level. This setting is accessible in Adobe Connect virtual room.");
    }

    @java.lang.Override
    public void setParticipantPlaybackLevel(String roomId, String roomUserId, int level)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support changing playback level.");
    }

    @java.lang.Override
    public void enableParticipantVideo(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(
                "Adobe Connect does not support this function. This setting is accessible in Adobe Connect virtual room.");
    }

    @java.lang.Override
    public void disableParticipantVideo(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException(
                "Adobe Connect does not support this function. This setting is accessible in Adobe Connect virtual room.");
    }

    @java.lang.Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException, CommandUnsupportedException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public UsageStats getUsageStats() throws CommandException, CommandUnsupportedException
    {
        //report-bulk-consolidated-transactions
        //report-meeting....
        return null;  //TODO
    }

    @java.lang.Override
    public Collection<RoomSummary> getRoomList() throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("filter-type", "meeting");

        Element response = request("report-bulk-objects", attributes);

        List<RoomSummary> meetings = new ArrayList<RoomSummary>();

        for (Element room : response.getChild("report-bulk-objects").getChildren("row")) {
            if (room.getChildText("name").matches("(?i).*Template"))
                continue;

            RoomSummary roomSummary = new RoomSummary();

            roomSummary.setIdentifier(room.getAttributeValue("sco-id"));
            roomSummary.setName(room.getChildText("name"));
            roomSummary.setDescription(room.getChildText("description"));

            //TODO: element URL

            meetings.add(roomSummary);
        }

        return Collections.unmodifiableList(meetings);
    }

    @java.lang.Override
    public MediaData getReceivedVideoSnapshot(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function.");
    }

    @java.lang.Override
    public MediaData getSentVideoSnapshot(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function.");
    }

    @java.lang.Override
    public int startRecording(String roomId, ContentType format, RoomLayout layout)
            throws CommandException, CommandUnsupportedException
    {
        //TODO: AC maybe does not support
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public void stopRecording(int recordingId) throws CommandException, CommandUnsupportedException
    {
        //TODO: AC maybe does not support
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public String getRecordingDownloadURL(int recordingId) throws CommandException, CommandUnsupportedException
    {
        //TODO: is even possible?
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public Collection<String> notifyParticipants(int recordingId) throws CommandException, CommandUnsupportedException
    {
        //TODO: ???
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public void downloadRecording(String downloadURL, String targetPath)
            throws CommandException, CommandUnsupportedException
    {
        //TODO: ???
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public void deleteRecording(int recordingId) throws CommandException
    {
        deleteSCO(Integer.toString(recordingId));
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public MediaData getRoomContent(String roomId) throws CommandException, CommandUnsupportedException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", roomId);

        Element response = request("sco-contents", attributes);

        for (Element child : response.getChild("scos").getChildren("sco")) {
            //TODO: archive all
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public void addRoomContent(String roomId, String name, MediaData data)
            throws CommandException, CommandUnsupportedException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public void removeRoomContentFile(String roomId, String name) throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", roomId);
        attributes.put("filter-name", name);

        Element response = request("sco-contents", attributes);

        if (response.getChild("scos").getChild("sco") != null) {
            deleteSCO(response.getChild("scos").getChild("sco").getAttributeValue("sco-id"));
        }
    }

    @java.lang.Override
    public void clearRoomContent(String roomId) throws CommandException, CommandUnsupportedException
    {
        // TODO: erase and re-create room?
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public Room getRoom(String roomId) throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", roomId);

        Element response = request("sco-info", attributes);

        Room room = new Room();
        room.setIdentifier(roomId);
        room.setName(response.getChild("sco").getChildText("name"));

        room.setOption(Room.Option.DESCRIPTION,response.getChild("sco").getChildText("description"));




        List<Alias> aliasList = new ArrayList<Alias>();
        String uri = "https://" + info.getDeviceAddress().getHost() + ":" + info.getDeviceAddress().getPort() + response.getChild("sco").getChildText("url-path");
        aliasList.add(new Alias(AliasType.ADOBE_CONNECT_URI, uri));

        room.setAliases(aliasList);


        // TODO: technology
        // TODO: roomInfo.setOwner();
        // TODO: roomInfo.setCreation();
        // TODO: roomInfo.setReservation();

        return room;
    }

    @java.lang.Override
    public String createRoom(Room room) throws CommandException
    {
        HashMap<String,String> attributes = new HashMap<String, String>();
        attributes.put("folder-id",
                (this.meetingsFolderID != null ? this.meetingsFolderID : this.getMeetingsFolderID()));
        attributes.put("name", room.getName());
        attributes.put("type","meeting");
        if (room.getAliase(AliasType.ADOBE_CONNECT_NAME) != null)
            attributes.put("url-path",room.getAliase(AliasType.ADOBE_CONNECT_NAME).getValue());
        if (room.getOption(Room.Option.DESCRIPTION) != null)
            attributes.put("description",room.getOption(Room.Option.DESCRIPTION).toString());

        Element respose = request("sco-update", attributes);
        String scoId = respose.getChild("sco").getAttributeValue("sco-id");

//        room.setIdentifier(respose.getChild("sco").getAttributeValue("sco-id"));
//        room.setOption(Room.Option.DESCRIPTION,this.serverUrl + respose.getChild("sco").getChildText("url-path"));

        for (String eppn : (List<String>) room.getOption(Room.Option.PARTICIPANTS)) {
            String principalId = this.createAdobeConnectUser(eppn);

//            String principalId = roomUser.getUserIdentity().getIdentifier();
            HashMap<String,String> userAttributes = new HashMap<String, String>();
            userAttributes.put("acl-id", scoId);
            userAttributes.put("principal-id",principalId);
            userAttributes.put("permission-id","host");

            request("permissions-update", userAttributes);
        }

//        importRoomSettings(respose.getChild("sco").getAttributeValue("sco-id"),room.getConfiguration());

        return scoId;
    }

    @java.lang.Override
    public String modifyRoom(Room room)
            throws CommandException, CommandUnsupportedException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public void deleteRoom(String roomId) throws CommandException
    {
        deleteSCO(roomId);
    }

    @java.lang.Override
    public String exportRoomSettings(String roomId) throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", roomId);

        Element response = request("sco-info", attributes);
        Document document = response.getDocument();

        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        String xmlString = outputter.outputString(document);

        return xmlString;
    }

    @java.lang.Override
    public void importRoomSettings(String roomId, String settings) throws CommandException
    {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document document = null;
        try {
            document = saxBuilder.build(new StringReader(settings));
        }
        catch (Exception exception) {
            throw new CommandException(exception.getMessage(), exception);
        }

        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        String xmlString = outputter.outputString(document);


        HashMap<String, String> attributes = new HashMap<String, String>();

        attributes.put("sco-id", roomId);
//        attributes.put("date-begin", document.getRootElement().getChild("sco").getChild("date-begin").getText());
//        attributes.put("date-end", document.getRootElement().getChild("sco").getChild("date-end").getText());
        if (document.getRootElement().getChild("sco").getChild("description") != null) {
            attributes.put("description", document.getRootElement().getChild("sco").getChild("description").getText());
        }
        attributes.put("url-path", document.getRootElement().getChild("sco").getChild("url-path").getText());
        attributes.put("name", document.getRootElement().getChild("sco").getChild("name").getText());

        request("sco-update", attributes);
    }

    @java.lang.Override
    public String dialParticipant(String roomId, String address) throws CommandException, CommandUnsupportedException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public String dialParticipant(String roomId, Alias alias) throws CommandException, CommandUnsupportedException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public Collection<RoomUser> listParticipants(String roomId) throws CommandException, CommandUnsupportedException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public RoomUser getParticipant(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public void modifyParticipant(String roomId, String roomUserId, Map attributes)
            throws CommandException, CommandUnsupportedException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public void disconnectParticipant(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public void enableContentProvider(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function. Use user role instead.");
    }

    @java.lang.Override
    public void disableContentProvider(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Adobe Connect does not support this function. Use user role instead.");
    }

    protected void deleteSCO(String scoID) throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", scoID);

        request("sco-delete", attributes);
    }

    /**
     * Retrieves the appropriate Breeze URL.
     *
     * @param action    the action to perform
     * @param atributes the map os parameters for the action
     * @return the URL to perform the action
     */
    protected URL breezeUrl(String action, Map<String, String> atributes) throws IOException, CommandException
    {
				if (action == null || action.isEmpty())
					throw new CommandException("Action of AC call cannot be empty.");

        String queryString = "";

        if (atributes != null) {
            for (Map.Entry<String, String> entry : atributes.entrySet()) {
                queryString += '&' + entry.getKey() + '=' + entry.getValue();
            }
        }

        return new URL("https://" + info.getDeviceAddress().getHost() + ":" + info.getDeviceAddress().getPort() + "/api/xml?" + "action=" + action
                + queryString);
    }

    protected String getBreezesession() throws Exception
    {

        if (breezesession == null) {
            login();
        }
        return breezesession;
    }

    /**
     * Sets and returns SCO-ID of folder for meetings.
     *
     * @return meeting folder SCO-ID
     * @throws CommandException
     */
    protected String getMeetingsFolderID() throws CommandException
    {
        if (meetingsFolderID == null) {
            Element response = request("sco-shortcuts",null);
            for (Element sco : response.getChild("shortcuts").getChildren("sco")) {
                if (sco.getAttributeValue("type").equals("meetings")) {
                    meetingsFolderID = sco.getAttributeValue("sco-id");

                    break;
                }
            }
        }

        return meetingsFolderID;
    }

    /**
     * Performs the action to log into Adobe Connect server. Stores the breezeseession ID.
     */
    protected void login() throws CommandException
    {
        if (this.breezesession != null) {
            logout();
        }

        HostTrustManager.initSsl(info.getDeviceAddress().getHost());

        HashMap<String, String> loginAtributes = new HashMap<String, String>();
        loginAtributes.put("login", login);
        loginAtributes.put("password", password);

        URLConnection conn;
        try {
            URL loginUrl = breezeUrl("login", loginAtributes);
            conn = loginUrl.openConnection();
            conn.connect();

            InputStream resultStream = conn.getInputStream();
            Document doc = new SAXBuilder().build(resultStream);

            if (this.isError(doc)) {
                logger.info(String.format("Login to server %s failed", info.getDeviceAddress()));

                throw new RuntimeException("Login to server " + info.getDeviceAddress() + " failed");
            } else {
                logger.info(String.format("Login on server %s succeeded", info.getDeviceAddress()));
            }
        }
        catch (Exception exception) {
            throw new CommandException(exception.getMessage(), exception);
        }

        String breezesessionString = (String) (conn.getHeaderField("Set-Cookie"));

        StringTokenizer st = new StringTokenizer(breezesessionString, "=");
        String sessionName = null;

        if (st.countTokens() > 1) {
            sessionName = st.nextToken();
        }

        if (sessionName != null &&
                (sessionName.equals("JSESSIONID") ||
                         sessionName.equals("BREEZESESSION"))) {

            String breezesessionNext = st.nextToken();
            int semiIndex = breezesessionNext.indexOf(';');
            this.breezesession = breezesessionNext.substring(0, semiIndex);


            this.meetingsFolderID = this.getMeetingsFolderID();
        }


        if (breezesession == null) {
            throw new RuntimeException("Could not log in to Adobe Connect server: " + info.getDeviceAddress());
        }
    }

    /**
     * Logout of the server, clearing the session as well.
     */
    public void logout() throws CommandException
    {
        request("logout", null);
        this.breezesession = null;
    }

    protected Element request(String action, Map<String, String> attributes) throws CommandException
    {
        try {
            if (this.breezesession == null) {
                if (action.equals("logout")) {
                    return null;
                }
                else {
                    login();
                }
            }

            URL url = breezeUrl(action, attributes);

            URLConnection conn = url.openConnection();
            conn.setRequestProperty("Cookie", "BREEZESESSION=" + this.breezesession);
            conn.connect();

            InputStream resultStream = conn.getInputStream();

            Document doc = new SAXBuilder().build(resultStream);

            if (this.isError(doc)) {
                Element status = doc.getRootElement().getChild("status");

                List<Attribute> statusAttributes = status.getAttributes();
                String errorMsg = new String();
                for (Attribute attribute : statusAttributes) {
                    errorMsg += " " + attribute.getName() + ": " + attribute.getValue();
                }

                if (status.getChild(status.getAttributeValue("code")) != null) {
                    List<Attribute> childAttributes = status.getChild(status.getAttributeValue("code")).getAttributes();
                    for (Attribute attribute : childAttributes) {
                        errorMsg += ", " + attribute.getName() + ": " + attribute.getValue() + " ";
                    }
                }

                logger.info(String.format("Command %s failed on %s: %s", action, info.getDeviceAddress(),errorMsg));

                throw new RuntimeException(errorMsg + ". URL: " + url);
            } else {
                logger.info(String.format("Command %s succeeded on %s", action, info.getDeviceAddress()));
            }
            return doc.getRootElement();
        }
        catch (IOException e) {
            throw new RuntimeException("Command issuing error", e);
        }
        catch (JDOMParseException e) {
            throw new RuntimeException("Command result parsing error", e);
        }
        catch (JDOMException e) {
            throw new RuntimeException("Error initializing parser", e);
        }
        catch (Exception exception) {
            throw new CommandException(exception.getMessage(), exception);
        }
    }

    private boolean isError(Document result) {
        Element status = result.getRootElement().getChild("status");

        return !status.getAttributeValue("code").equals("ok");
    }

    public static void main(String[] args) throws Exception
    {
        try {
            String server = "actest-w3.cesnet.cz";

            new SAXParserFactoryImpl();

            AdobeConnectConnector acc = new AdobeConnectConnector();
            Address address = new Address(server, 443);

            acc.connect(address,"admin","cip9skovi3t2");

            System.out.println(acc.getConnectorInfo());
//            String str = acc.exportRoomSettings("42108");
//            acc.importRoomSettings("42108",str);

//            System.out.println(acc.getSupportedMethods());



//            acc.deleteRoom(scoId);
/*            Room r = new Room("test",0);
            acc.createRoom(r);

            System.out.println(r.getIdentifier());
            System.out.println("mezera");
            System.out.println(r.getOptions());

            acc.deleteRoom(r.getIdentifier());

            System.out.println(acc.getRoomList());
*/

            acc.disconnect();

        }
        catch (ExceptionInInitializerError exception) {
            logger.error("Cannot initialize adobe connect", exception);
        }
    }

    public static void printInputStream(InputStream inputStream) throws IOException
    {
        System.out.println();
        System.out.println("-- SOURCE DATA --");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        String line = new String();

        while (bufferedReader.ready()) {
            line = bufferedReader.readLine();
            System.out.println(line);
        }
        System.out.println("-- SOURCE DATA --");
        System.out.println();
    }
}
