package cz.cesnet.shongo.connector.device;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.UsageStats;
import cz.cesnet.shongo.connector.common.AbstractMultipointConnector;
import cz.cesnet.shongo.connector.common.RequestAttributeList;
import cz.cesnet.shongo.connector.util.HttpReqUtils;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class PexipConnector extends AbstractMultipointConnector {

    private static Logger logger = LoggerFactory.getLogger(PexipConnector.class);

    public static final int DEFAULT_PORT = 443;

    /**
     * Authentication for the device.
     */
    private String authUsername;
    private String authPassword;

    /**
     * Request timeout to device in milliseconds.
     */
    protected int requestTimeout;

    /**
     * Client used for the Http communication with the device.
     */
    private HttpClient httpClient;

    /**
     * Option for the {@link PexipConnector}.
     */

    /**
     * Patterns for options.
     */
    private Pattern roomNumberFromH323Number = null;

    public static final String ROOM_NUMBER_EXTRACTION_FROM_H323_NUMBER = "room-number-extraction-from-h323-number";

    private static final Pattern E164_PATTERN = Pattern.compile("^\\+?\\d{9,14}$");

    @Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException, CommandUnsupportedException {
        return null;
    }

    @Override
    public Collection<RoomSummary> listRooms() throws CommandException, CommandUnsupportedException {
        return null;
    }

    @Override
    public String createRoom(final Room room) throws CommandException {

        JSONObject json = new JSONObject()
                .put("service_type", "conference");

        addConferenceParamsToJson(json, room);

        // Room name must be filled
        if (!json.has("name") ) {
            throw new RuntimeException("Room name must be filled for the new room.");
        }

        String jsonString = json.toString();

        HttpResponse response = execApiToResponse("/api/admin/configuration/v1/conference/", null, jsonString, HttpMethod.POST);

        // Extract roomId from response
        String location = response.getLastHeader("Location").getValue();
        String path = location.substring(0, location.length() - 1); //remove extra slash
        String roomId = path.substring(path.lastIndexOf('/') + 1);

        // Important for releasing the connection
        EntityUtils.consumeQuietly(response.getEntity());

        return roomId;
    }

    private void addConferenceParamsToJson (JSONObject json, Room room) throws CommandException {

        json.put("name", room.getName());

        if (!Strings.isNullOrEmpty(room.getDescription())) {
            json.put("description", room.getDescription());
        }

        if (room.getAliases() != null && room.getAliases().size() != 0) {
            JSONArray aliases = new JSONArray();
            String roomName = null;
            String roomNumber = null;
            Matcher m;

            for (Alias alias : room.getAliases()) {
                switch (alias.getType()) {
                    case ROOM_NAME:
                        roomName = alias.getValue();
                        break;
                    case SIP_URI:
                        aliases.put(new JSONObject().put("alias", alias.getValue()));
                        break;
                    case H323_E164:
                        aliases.put(new JSONObject().put("alias", alias.getValue()));
                        if (roomNumberFromH323Number == null) {
                            throw new CommandException(String.format(
                                    "Cannot set H.323 E164 number - missing connector device option '%s'",
                                    ROOM_NUMBER_EXTRACTION_FROM_H323_NUMBER));
                        }
                        m = roomNumberFromH323Number.matcher(alias.getValue());
                        if (!m.find()) {
                            throw new CommandException("Invalid E164 number: " + alias.getValue());
                        }
                        roomNumber = m.group(1);
                        break;
                    case SKYPE_URI:
                        aliases.put(new JSONObject().put("alias", alias.getValue()));
                        break;
                    case H323_IP:
                        break;
                    case WEB_CLIENT_URI:
                        break;
                    default:
                        throw new CommandException("Unrecognized alias: " + alias.toString());
                }
            }
            if (roomNumber != null) {
                //add also an numeric alias - which is just the room number
                aliases.put(new JSONObject().put("alias", roomNumber));
                //and an universal alias for H323,SIP and S4B
                aliases.put(new JSONObject().put("alias", roomNumber + "vc.cesnet.cz"));
            }
            json.put("aliases", aliases);
            if (roomName != null) {
                aliases.put(new JSONObject().put("alias", roomName));
            }
        }
        // Set license count
        json.put("participant_limit", (room.getLicenseCount() > 0 ? room.getLicenseCount() : 0));

        // Set options
        PexipRoomSetting pexipRoomSetting = room.getRoomSetting(PexipRoomSetting.class);
        if (pexipRoomSetting != null) {
            if (pexipRoomSetting.getHostPin() != null) {
                json.put("pin", pexipRoomSetting.getHostPin());
            }
            if (!Strings.isNullOrEmpty(pexipRoomSetting.getGuestPin())) {
                json.put("allow_guests", true);
                json.put("guest_pin", pexipRoomSetting.getGuestPin());
            } else {
                json.put("allow_guests", false);
            }
        }
    }

    @Override
    public UsageStats getUsageStats() throws CommandException, CommandUnsupportedException {
        return null;
    }

    @Override
    public void deleteRoom(String roomId) throws CommandException {
        if (Strings.isNullOrEmpty(roomId)) {
            throw new CommandException("This command would remove all VMRs.");
        }
        HttpDelete request = new HttpDelete();
        execApi("/api/admin/configuration/v1/conference/" + roomId + "/", null, null, HttpMethod.DELETE);
    }

    @Override
    public MediaData getRoomContent(String roomId) throws CommandException, CommandUnsupportedException {
        return null;
    }

    @Override
    public void disconnect() throws CommandException {
        // no real operation - the communication protocol is stateless
        httpClient = null; // just for sure the attributes are not used anymore

    }

    @Override
    public Room getRoom(String roomId) throws CommandException {
        JSONObject roomJson = execApi("/api/admin/configuration/v1/conference/" + roomId, null, null, HttpMethod.GET);
        if (roomJson == null) {
            return null;
        }
        Room room = new Room ();
        room.setId(roomJson.getLong("id"));
        room.setTechnologies(Sets.newHashSet(Technology.H323, Technology.RTMP, Technology.SIP, Technology.SKYPE_FOR_BUSINESS, Technology.WEBRTC));
        if (roomJson.has("participant_limit")) {
            room.setLicenseCount(roomJson.getInt("participant_limit"));
        }
        if (roomJson.has("description")) {
            room.setDescription(roomJson.getString("description"));
        }

        // aliases
        if (roomJson.has("name")) {
            Alias nameAlias = new Alias(AliasType.ROOM_NAME, roomJson.getString("name"));
            room.addAlias(nameAlias);
        }

        // options
        PexipRoomSetting pexipRoomSetting = new PexipRoomSetting();
        if (roomJson.has("pin")) {
            pexipRoomSetting.setHostPin(roomJson.getString("pin"));
        }
        if (roomJson.has("guest_pin")) {
            pexipRoomSetting.setGuestPin(roomJson.getString("guest_pin"));
        }
        room.addRoomSetting(pexipRoomSetting);

        return room;
    }

    @Override
    public void addRoomContent(String roomId, String name, MediaData data) throws CommandException, CommandUnsupportedException {

    }

    @Override
    public void removeRoomContentFile(String roomId, String name) throws CommandException, CommandUnsupportedException {

    }

    @Override
    public void clearRoomContent(String roomId) throws CommandException, CommandUnsupportedException {

    }

    @Override
    public String exportRoomSettings(String roomId) throws CommandException, CommandUnsupportedException {
        return null;
    }

    @Override
    protected void onModifyRoom(final Room room) throws CommandException {

        String roomId;
        // Room id must be filled
        if (room.getId() == null ) {
            throw new RuntimeException("Room id must be filled for the modifying room.");
        } else {
            roomId = room.getId();
        }

        JSONObject json = new JSONObject();

        addConferenceParamsToJson(json, room);

        String jsonString = json.toString();

        execApi("/api/admin/configuration/v1/conference/" + roomId + "/", null, jsonString, HttpMethod.PATCH);
    }

    @Override
    public void importRoomSettings(String roomId, String settings) throws CommandException, CommandUnsupportedException {

    }

    @Override
    public Collection<RoomParticipant> listRoomParticipants(String roomId) throws CommandException, CommandUnsupportedException {
        String roomName = getRoomName(roomId);

        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("conference", roomName);
        JSONObject response = execApi("/api/admin/status/v1/participant/", attributes, null, HttpMethod.GET);
        JSONArray participants = response.getJSONArray("objects");
        if (participants.length() == 0)
            return null;
        List<RoomParticipant> resultList = new ArrayList<RoomParticipant>();
        for (int i = 0; i < participants.length(); i++) {
            JSONObject participant = participants.getJSONObject(i);
            resultList.add(extractRoomParticipant(participant, roomId));
        }
        return Collections.unmodifiableList(resultList);
    }

    private String getRoomName (String roomId) throws CommandException {
        JSONObject room = execApi("/api/admin/configuration/v1/conference/" + roomId, null, null, HttpMethod.GET);

        String roomName = room.getString("name");

        return roomName;
    }

    /**
     * Extracts a single {@link RoomParticipant} out of status API result of participants.
     *
     * @param participant participant structure, as defined in the MCU API, command participant.status
     * @return {@link RoomParticipant} extracted from the participant structure
     */
    private RoomParticipant extractRoomParticipant (JSONObject participant, String roomId) {
        RoomParticipant roomParticipant = new RoomParticipant();


        roomParticipant.setId(participant.getString("id"));
        roomParticipant.setRoomId(roomId);
        roomParticipant.setDisplayName(participant.getString("display_name"));


        String role = participant.getString("role");
        if ("guest".equals(role)) {
            roomParticipant.setRole(ParticipantRole.PARTICIPANT);
        } else if ("chair".equals(role)) {
            roomParticipant.setRole(ParticipantRole.ADMINISTRATOR);
        }

        String alias =  participant.getString("participant_alias");
        String protocol = participant.getString("protocol");
        AliasType aliasType;
        if (protocol.equals("sip")) {
            aliasType = AliasType.SIP_URI;
        } else if (protocol.equalsIgnoreCase("h323")) {
            if (E164_PATTERN.matcher(alias).matches()) {
                aliasType = AliasType.H323_E164;
            } else {
                aliasType = AliasType.H323_URI;
            }
        } else if (protocol.equalsIgnoreCase("rtmp")) {
            aliasType = AliasType.RTMP_NAME;
        } else if (protocol.equalsIgnoreCase("mssip")) {
            aliasType = AliasType.SKYPE_URI;
        } else if (protocol.equalsIgnoreCase("webrtc")) {
            aliasType = AliasType.WEB_RTC_NAME;
        } else {
            throw new TodoImplementException("Protocol " + protocol + " not implemented yet.");
        }
        roomParticipant.setAlias(new Alias(aliasType, alias));

        return roomParticipant;
    }

    @Override
    public RoomParticipant getRoomParticipant(String roomId, String roomParticipantId) throws CommandException, CommandUnsupportedException {
        return null;
    }

    @Override
    public Map<String, MediaData> getRoomParticipantSnapshots(String roomId, Set<String> roomParticipantIds) throws CommandException, CommandUnsupportedException {
        return null;
    }

    @Override
    public void modifyRoomParticipant(RoomParticipant roomParticipant) throws CommandException, CommandUnsupportedException {

    }

    @Override
    public void modifyRoomParticipants(RoomParticipant roomParticipantConfiguration) throws CommandException, CommandUnsupportedException {

    }

    @Override
    public String dialRoomParticipant(String roomId, Alias alias) throws CommandException, CommandUnsupportedException {
        return null;
    }

    @Override
    public void disconnectRoomParticipant(String roomId, String roomParticipantId) throws CommandException, CommandUnsupportedException {
        String roomName = getRoomName(roomId);
        execApi("/api/client/v2/conferences/" + roomName +"/participants/" + roomParticipantId + "/disconnect", null, null, HttpMethod.POST);
    }

    @Override
    public void connect(DeviceAddress deviceAddress, String username, String password) throws CommandException {

        if (deviceAddress.getPort() == DeviceAddress.DEFAULT_PORT) {
            deviceAddress.setPort(DEFAULT_PORT);
        }

        roomNumberFromH323Number = configuration.getOptionPattern(ROOM_NUMBER_EXTRACTION_FROM_H323_NUMBER);


        //standard basic auth
        this.authUsername = username;
        this.authPassword = password;
        this.deviceAddress = deviceAddress;

        // Create HttpClient for Http communication
        httpClient = ConfiguredSSLContext.getInstance().createHttpClient(requestTimeout);

        //Try to fetch nodes list
        try {
            JSONObject jsonResponse = execApi("/api/admin/status/v1/worker_vm/", null, null, HttpMethod.GET);
            JSONArray conferenceNodes = jsonResponse.getJSONArray("objects");
            int confNodesCount = conferenceNodes.length();
            if (confNodesCount == 0) {
                throw new CommandException("No conference nodes available");
            }
            for (int i = 0; i < confNodesCount; i++) {
                try {
                    JSONObject confNode = conferenceNodes.getJSONObject(i);
                    //get version of conferencing node
                    Integer apiVersion = new Integer(confNode.getString("version").substring(0, confNode.getString("version").indexOf(" ")));
                    if (apiVersion < 18) {
                        throw new CommandException(String.format(
                                "Device API %.1f too old. The connector only works with API 18 or higher.", apiVersion));
                    }
                }
                catch (Exception exception) {
                    throw new CommandException("Cannot determine the device API version.", exception);
                }
            }
        } catch (Exception e) {
            throw new CommandException("Error setting up connection to the device.", e);
        }
        logger.info(String.format("Connection to server %s succeeded", deviceAddress));
    }

    protected String getCallUrl(String callPath, RequestAttributeList attributes) throws CommandException {
        return deviceAddress.getFullUrl() + HttpReqUtils.getCallUrl(callPath, attributes);
    }

    // TODO allow for 5 consecutive repetitions if unsuccessful

    /**
     * Executes command on Pexip server and returns {@link HttpResponse}.
     * If using directly this method take care of releasing the connection
     * for example with EntityUtils.consumeQuietly(response.getEntity()).
     * @param actionPath    path of the Pexip action
     * @param attributes    attributes of the action
     * @param body          request body
     * @param reqMethod     request method
     * @return HttpResponse
     * @throws CommandException
     */
    private HttpResponse execApiToResponse(String actionPath, RequestAttributeList attributes, String body, HttpMethod reqMethod) throws CommandException {
        String authString = authUsername + ":" + authPassword;
        String authStringEnc = Base64.encode(authString.getBytes(StandardCharsets.UTF_8));
        String command = getCallUrl(actionPath, attributes);
        logger.debug(String.format("Issuing request " + reqMethod + " '%s'", command));
        HttpRequestBase request = getRequest(command, reqMethod);
        request.setHeader("Authorization", "Basic " + authStringEnc);
        HttpResponse response;

        try {
            addBodyToRequest(request, body);
            response = httpClient.execute(request);
            StatusLine responseStatusLine = response.getStatusLine();

            if (responseStatusLine.getStatusCode() >= 400) {
                throw new RuntimeException("Wrong status " + responseStatusLine + ". " + EntityUtils.toString(response.getEntity()));
            }
        } catch (IOException e) {
            throw new CommandException(e.getMessage(), e.getCause());
        }
        return response;
    }

    /**
     * Executes command on Pexip server and returns Json response. Throws CommandException when some error on server occured or some parser error occured.
     * @param actionPath    path of the Pexip action
     * @param attributes    attributes of the action
     * @param body          request body
     * @param reqMethod     request method
     * @return JSONObject of the response if any, otherwise null
     * @throws CommandException
     */
    private JSONObject execApi (String actionPath, RequestAttributeList attributes, String body, HttpMethod reqMethod) throws CommandException {
        HttpResponse response = execApiToResponse(actionPath, attributes, body, reqMethod);
        JSONObject jsonObject = null;
        String responseBody;
        try {
            // json object from response
            if (response.getEntity() != null) {
                responseBody = EntityUtils.toString(response.getEntity());
                if (!Strings.isNullOrEmpty(responseBody)) {
                    jsonObject = new JSONObject(responseBody);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Command issuing error", e);
        } catch (JSONException e) {
            throw new RuntimeException("Command result parsing error", e);
        }
        return jsonObject;

    }

    private void addBodyToRequest (HttpRequestBase request, String body) throws UnsupportedEncodingException {
        if (body != null && (request instanceof HttpEntityEnclosingRequestBase) ) {
            StringEntity jsonBody = new StringEntity(body);
            jsonBody.setContentType("application/json;charset=UTF-8");
            jsonBody.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,"application/json;charset=UTF-8"));
            ((HttpEntityEnclosingRequestBase)request).setEntity(jsonBody);
        }
    }


    // roomname
    // roomname@vc.cesnet.cz
    // tie 3 vygenerovane cisla
    // 3 vygenernovane cisla@vc.cesnet.cz

    private HttpRequestBase getRequest(String url, HttpMethod method){
        switch(method){
            case DELETE:
                return new HttpDelete(url);
            case GET:
                return new HttpGet(url);
            case PATCH:
                return new HttpPatch(url);
            case POST:
                return new HttpPost(url);
            case PUT:
                return new HttpPut(url);
            default:
                throw new IllegalArgumentException("Invalid or null HttpMethod: " + method);
        }
    }

    private String printPrettyJson (String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        return json.toString(4); // Print with specified indentation
    }

    @Override
    public ConnectionState getConnectionState() {
        try {
            execApi("/api/admin/status/v1/worker_vm/", null, null, HttpMethod.GET);
            return ConnectionState.CONNECTED;
        }
        catch (Exception exception) {
            logger.warn("Not connected", exception);
            return ConnectionState.DISCONNECTED;
        }
    }

    public static void main(String[] args) throws Exception {
        final String username = "";
        final String password = "";
        final String server = "https://pexman.cesnet.cz";

        DeviceAddress address = new DeviceAddress(server, 443);
        PexipConnector conn = new PexipConnector();
        conn.connect(address, username, password);


        conn.listRoomParticipants("2");
/*
        //Test create new room and delete
        Room room = new Room();
        //room.setId(50L);
        room.addAlias(AliasType.ROOM_NAME, "CREATE_ROOM_TEST_ALIAS");
        room.addAlias(AliasType.SIP_URI, "test@test.cz");
        String roomId = conn.createRoom(room);
        System.out.println("Created room id:" + roomId);
        conn.deleteRoom(roomId);

        //Test onModifyRoom
        Room room = new Room();
        PexipRoomSetting pexipRoomSetting = new PexipRoomSetting();
        pexipRoomSetting.setHostPin("1234");
        pexipRoomSetting.setGuestPin("3414");
        room.addRoomSetting(pexipRoomSetting);
        room.setId("49");
        room.setLicenseCount(2);
        conn.onModifyRoom(room);*/

    }


    private enum HttpMethod {
        GET,
        POST,
        PATCH,
        DELETE,
        PUT
    }
}
