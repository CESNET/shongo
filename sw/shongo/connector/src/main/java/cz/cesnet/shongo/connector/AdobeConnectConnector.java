package cz.cesnet.shongo.connector;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
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
            if (room.getChildText("name").matches("(?i).*Template")) {
                continue;
            }

            RoomSummary roomSummary = new RoomSummary();

            roomSummary.setId(room.getAttributeValue("sco-id"));
            roomSummary.setCode(room.getChildText("name"));
            //TODO: not specified yet
            roomSummary.setName(room.getChildText("description"));

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
            throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", roomId);
        attributes.put("active", "true");

        request("meeting-recorder-activity-update", attributes);

        HashMap<String, String> recAttributes = new HashMap<String, String>();
        recAttributes.put("sco-id", roomId);

        Element response = request("meeting-recorder-activity-info",recAttributes);

        int recScoId = Integer.valueOf(
                response.getChild("meeting-recorder-activity-info").getChildText("recording-sco-id"));

        return recScoId;
    }

    @java.lang.Override
    public void stopRecording(int recordingId) throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", Integer.toString(recordingId));
        attributes.put("active", "false");

        request("meeting-recorder-activity-update", attributes);
    }

    @java.lang.Override
    public String getRecordingDownloadURL(int recordingId) throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", Integer.toString(recordingId));

        Element response = request("sco-info", attributes);

        String url = "https://" + info.getDeviceAddress().getHost() + ":" + info.getDeviceAddress().getPort() + response.getChild("sco").getChildText("url-path");

        return url;
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
        // TODO: erase content and re-create room?
    }

    @java.lang.Override
    public Room getRoom(String roomId) throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", roomId);

        Element response = request("sco-info", attributes);

        Room room = new Room();
        room.setId(roomId);
        room.setCode(response.getChild("sco").getChildText("name"));
        room.setName(response.getChild("sco").getChildText("description"));




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
        try {
        HashMap<String,String> attributes = new HashMap<String, String>();
        attributes.put("folder-id",
                (this.meetingsFolderID != null ? this.meetingsFolderID : this.getMeetingsFolderID()));
        attributes.put("name", URLEncoder.encode(room.getCode(),"UTF8"));
        attributes.put("type","meeting");
        if (room.getAlias(AliasType.ADOBE_CONNECT_NAME) != null) {
            attributes.put("url-path", room.getAlias(AliasType.ADOBE_CONNECT_NAME).getValue());
        }
        if (room.getName() != null) {
            attributes.put("description", URLEncoder.encode(room.getName(),"UTF8"));
        }


        Element respose = request("sco-update", attributes);
        String scoId = respose.getChild("sco").getAttributeValue("sco-id");

//        room.setId(respose.getChild("sco").getAttributeValue("sco-id"));
//        room.setOption(room.getName(),this.serverUrl + respose.getChild("sco").getChildText("url-path"));

        for (String eppn : (List<String>) room.getOption(Room.Option.PARTICIPANTS)) {
            String principalId = this.createAdobeConnectUser(eppn);

//            String principalId = roomUser.getUserIdentity().getId();
            HashMap<String,String> userAttributes = new HashMap<String, String>();
            userAttributes.put("acl-id", scoId);
            userAttributes.put("principal-id",principalId);
            userAttributes.put("permission-id","host");

            request("permissions-update", userAttributes);
        }

//        importRoomSettings(respose.getChild("sco").getAttributeValue("sco-id"),room.getConfiguration());

        return scoId;

        } catch (UnsupportedEncodingException ex) {
            throw new CommandException("Error while URL encoding.",ex);
        }
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
        HashMap<String,String> attributes = new HashMap<String, String>();
        attributes.put("sco-id",roomId);

        this.request("meeting-stop",attributes);

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
    public Collection<RoomUser> listParticipants(String roomId) throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", roomId);

        Element response =  request("meeting-usermanager-user-list", attributes);

        ArrayList<RoomUser> participantList = new ArrayList<RoomUser>();

        for (Element userDetails : response.getChild("meeting-usermanager-user-list").getChildren()) {
            System.out.println(userDetails.getChildText("principal-id"));

            RoomUser roomUser = new RoomUser();
            roomUser.setAudioMuted(Boolean.parseBoolean(userDetails.getChildText("mute")));
            roomUser.setDisplayName(userDetails.getChildText("username"));
            roomUser.setRoomId(roomId);
            roomUser.setUserId(userDetails.getChildText("user-id"));
            roomUser.setUserIdentity(new UserIdentity("principal-id"));

            participantList.add(roomUser);
        }

        return Collections.unmodifiableList(participantList);
    }

    @java.lang.Override
    public RoomUser getParticipant(String roomId, String roomUserId) throws CommandException
    {
        Collection<RoomUser> participants = this.listParticipants(roomId);
        for (RoomUser roomUser : participants) {
            if (roomUser.getUserId() == roomUserId) {
                return roomUser;
            }
        }

        return null;
    }

    @java.lang.Override
    public void modifyParticipant(String roomId, String roomUserId, Map attributes)
            throws CommandException, CommandUnsupportedException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @java.lang.Override
    public void disconnectParticipant(String roomId, String roomUserId)
            throws CommandException
    {
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("sco-id", roomId);
        attributes.put("user-id", roomUserId);

        Element response =  request("meeting-usermanager-remove-user", attributes);
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
        if (action == null || action.isEmpty()) {
            throw new CommandException("Action of AC call cannot be empty.");
        }

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

        HostTrustManager.addTrustedHost(info.getDeviceAddress().getHost());

        HashMap<String, String> loginAtributes = new HashMap<String, String>();
        loginAtributes.put("login", this.login);
        loginAtributes.put("password", this.password);

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
                logger.info(String.format("Login to server %s succeeded", info.getDeviceAddress()));
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

    /**
     * Execute command on Adobe Connect server and returns XML response. Throws CommandException when some error on Adobe Connect server occured or some parser error occured.
     *
     * @param action name of Adobe Connect action
     * @param attributes atrtributes of action
     * @return XML action response
     * @throws CommandException
     */
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
            } else {
                if (!action.equals("common-info")) {
                    this.reconnect();
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

    /**
     *  If login, password and address is present check if connector is still connected. If necessary reconnects.
     */
    private void reconnect() throws CommandException
    {
        if (this.getConnectorInfo().getConnectionState() == ConnectorInfo.ConnectionState.LOOSELY_CONNECTED) {
            Element response = this.request("common-info",null);

            if (response.getChild("common").getChild("user") == null)
                logger.info(String.format("Reconnecting to server %s", info.getDeviceAddress()));
                this.breezesession = null;
                this.login();
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

            AdobeConnectConnector acc = new AdobeConnectConnector();
            Address address = new Address(server, 443);

            acc.connect(address,"admin","cip9skovi3t2");

/*            Room room = new Room(String.format("Shongo%d [exec:%d]",123,456),5);
            room.setAliases(new ArrayList<Alias>() {{ add(new Alias(AliasType.ADOBE_CONNECT_NAME,"testi")); }});
            room.setOption(Room.Option.PARTICIPANTS,new ArrayList<String>() {{ add("pavelka@cesnet.cz"); }});

            String scoId = acc.createRoom(room);

            System.out.println(scoId);
            Thread.sleep(15000);
            acc.deleteRoom(scoId); */



            acc.disconnect();

        }
        catch (ExceptionInInitializerError exception) {
            logger.error("Cannot initialize adobe connect", exception);
        }
    }

    /*public static void printInputStream(InputStream inputStream) throws IOException
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
    }*/
}
