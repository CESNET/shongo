package cz.cesnet.shongo.connector.device;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */

import com.google.common.base.Strings;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import cz.cesnet.shongo.AliasType;
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
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;


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
        //TODO send service_tag <- represents link to the room in meetings unique for each room
        //TODO set host(must set)/guest(doesnt have to be set) pin and participant limit
        //TODO lock room with participant limit 0


        addConferenceParamsToJson(json, room);
        String jsonString = json.toString();

        execApi("/api/admin/configuration/v1/conference/", null, jsonString, "POST");

        return room.getId();
    }

    private void addConferenceParamsToJson (JSONObject json, Room room) throws CommandException {

        json.put("name", room.getName());

        JSONArray aliases = new JSONArray();

        if (room.getAliases() != null) {
            for (Alias alias : room.getAliases()) {
                switch (alias.getType()) {
                    case ROOM_NAME:
                        aliases.put(new JSONObject().put("alias", alias.getValue()));
                        break;
                    case SIP_URI:
                        //TODO check for alias format
                        aliases.put(new JSONObject().put("alias", alias.getValue()));
                        break;
                    default:
                        throw new CommandException("Unrecognized alias: " + alias.toString());
                }
            }
            json.put("aliases", aliases);
        }

        printPrettyJson(json.toString());

    }

    @Override
    public UsageStats getUsageStats() throws CommandException, CommandUnsupportedException {
        return null;
    }

    @Override
    public void deleteRoom(String roomId) throws CommandException {

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
        return null;
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

    }

    @Override
    public void importRoomSettings(String roomId, String settings) throws CommandException, CommandUnsupportedException {

    }

    @Override
    public Collection<RoomParticipant> listRoomParticipants(String roomId) throws CommandException, CommandUnsupportedException {
        return null;
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

    }

    @Override
    public void connect(DeviceAddress deviceAddress, String username, String password) throws CommandException {
        if (deviceAddress.getPort() == DeviceAddress.DEFAULT_PORT) {
            deviceAddress.setPort(DEFAULT_PORT);
        }

        // Load options
        /*roomNumberFromH323Number = configuration.getOptionPattern(ROOM_NUMBER_EXTRACTION_FROM_H323_NUMBER);
        roomNumberFromSIPURI = configuration.getOptionPattern(ROOM_NUMBER_EXTRACTION_FROM_SIP_URI);*/

        //TODO configure hidden participants

        //standard basic auth
        this.authUsername = username;
        this.authPassword = password;
        this.deviceAddress = deviceAddress;

        // Create HttpClient for Http communication
        httpClient = ConfiguredSSLContext.getInstance().createHttpClient(requestTimeout);


        //Try to fetch conferences list
        try {
            JSONObject response = execApi("/api/admin/status/v1/worker_vm/", null, null,"GET");
            JSONArray conferenceNodes = response.getJSONArray("objects");
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
    protected JSONObject execApi(String actionPath, RequestAttributeList attributes, String body, String reqMethod) throws CommandException {
        String authString = authUsername + ":" + authPassword;
        String authStringEnc = Base64.encode(authString.getBytes(StandardCharsets.UTF_8));
        String command = getCallUrl(actionPath, attributes);
        logger.debug(String.format("Issuing command '%s'", command));
        HttpRequestBase request = getRequest(command, reqMethod);
        request.setHeader("Authorization", "Basic " + authStringEnc);
        HttpResponse response;
        JSONObject jsonObject = null;
        try {
            //adding json data
            if (body != null && (request instanceof HttpEntityEnclosingRequestBase) ) {
                StringEntity jsonBody = new StringEntity(body);
                jsonBody.setContentType("application/json;charset=UTF-8");
                jsonBody.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,"application/json;charset=UTF-8"));
                ((HttpEntityEnclosingRequestBase)request).setEntity(jsonBody);
            }
            response = httpClient.execute(request);
            System.out.println("Response Code : " + response.getStatusLine().getStatusCode() + response.getStatusLine()); // remove line

            //reading response
            InputStream in = response.getEntity().getContent();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            in));
            StringBuilder responseBody = new StringBuilder();
            String currentLine;

            while ((currentLine = br.readLine()) != null)
                responseBody.append(currentLine);
            in.close();

            // json object from response
            if (!Strings.isNullOrEmpty(responseBody.toString())) {
                jsonObject = new JSONObject(responseBody.toString());
                System.out.println(printPrettyJson(responseBody.toString())); // remove line
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    // TODO create enum for Http methods
    private HttpRequestBase getRequest(String url, String method){
        switch(method){
            case "DELETE":
                return new HttpDelete(url);
            case "GET":
                return new HttpGet(url);
            case "PATCH":
                return new HttpPatch(url);
            case "POST":
                return new HttpPost(url);
            case "PUT":
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
        return null;
    }

    public static void main(String[] args) throws Exception {
        final String username = "";
        final String password = "";
        final String server = "https://pexman.cesnet.cz";

        DeviceAddress address = new DeviceAddress(server, 443);
        PexipConnector conn = new PexipConnector();
        conn.connect(address, username, password);

        //Test create new room
        Room room = new Room();
        room.setId(50L);
        room.addAlias(AliasType.ROOM_NAME, "CREATE_ROOM_TEST_ALIAS");
        room.addAlias(AliasType.SIP_URI, "test@test.cz");
        conn.createRoom(room);
    }
}
