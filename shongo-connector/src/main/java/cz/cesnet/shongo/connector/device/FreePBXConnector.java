package cz.cesnet.shongo.connector.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.UsageStats;
import cz.cesnet.shongo.connector.common.AbstractMultipointConnector;
import cz.cesnet.shongo.connector.common.RequestAttributeList;
import cz.cesnet.shongo.connector.util.HttpReqUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class FreePBXConnector extends AbstractMultipointConnector {

    private static Logger logger = LoggerFactory.getLogger(FreePBXConnector.class);

    /**
     * This is the server token.
     */
    private String token;

    /**
     * The token key for specified token.
     */
    private String tokenKey;

    /**
     * Prefix of the conference number.
     */
    private String prefix;

    public static final String ROOM_NUMBER_EXTRACTION_FROM_FREEPBX_NUMBER = "room-number-extraction-from-freepbx-number";
    public static final String CONFERENCE_NUMBER_PREFIX = "conference-number-prefix";

    private Pattern roomNumberFromFreePBXNumber = null;
    private Pattern reservationNumberFromDescription = Pattern.compile(":(\\d*)");


    @Override
    public RoomParticipant getRoomParticipant(String roomId, String roomParticipantId) throws CommandException, CommandUnsupportedException {
        return null; // TODO??
    }

    @Override
    public void addRoomContent(String roomId, String name, MediaData data) throws CommandException, CommandUnsupportedException {
        throw new CommandUnsupportedException();
    }

    @Override
    public void disconnect() throws CommandException {}

    @Override
    public MediaData getRoomContent(String roomId) throws CommandException, CommandUnsupportedException {
        throw new CommandUnsupportedException();
    }

    @Override
    public void deleteRoom(String roomId) throws CommandException {
        logger.debug("PUT " + getCallUrl("/conferences/" + roomId, null));
        //generate random number for room pin while not reserved
        //workaround for FreePBX api bug - not reloading config. only using the room attributes from not commited config
        RequestAttributeList attributes = new RequestAttributeList();
        Random r = new Random();
        Random s = new Random();
        String randomAdminPin = String.format("%04d", (Object) Integer.valueOf(r.nextInt(1001)));
        attributes.add("userpin", randomAdminPin);
        String randomUserPin = String.format("%04d", (Object) Integer.valueOf(s.nextInt(1001)));
        attributes.add("adminpin", randomUserPin);

        execApi("/conferences/" + roomId, attributes, "PUT");
    }

    @Override
    public String dialRoomParticipant(String roomId, Alias alias) throws CommandException, CommandUnsupportedException {
        throw new CommandUnsupportedException();
    }

    @Override
    public String createRoom(Room room) throws CommandException {

        RequestAttributeList attributes = new RequestAttributeList();


        String roomId = setRoomAttributes(attributes, room);

        // Room name and id must be filled
        if (attributes.getValue("name") == null) {
            throw new RuntimeException("Room name must be filled for the new room.");
        }
        if (roomId == null) {
            throw new RuntimeException("Room number must be filled for the new room.");
        }

        logger.debug("PUT " + getCallUrl("/conferences/" + roomId, attributes));

        execApi("/conferences/" + roomId , attributes, "PUT");

        return roomId;
    }

    public String setRoomAttributes (RequestAttributeList attributes, Room room) throws CommandException {

        Matcher m;
        if (room.getDescription() != null) {
            String description = room.getDescription();
            //remember reservation number
            m = reservationNumberFromDescription.matcher(description);
            String reservationNumber = null;
            if (m.find()) {
                reservationNumber = m.group(1);
            }
            //remove machine prefix from description
            description = description.replaceAll("\\[.*\\] ", "");
            //remove all non-alpha numeric characters - may cause undefined behaviour in FreePBX
            description = description.replaceAll("[^\\p{IsAlphabetic}^\\p{IsDigit}]", "");
            //add reservation number to front
            description = "exe" + reservationNumber + " " + description;
            attributes.add("name", description);
        }
        String roomId = null;
        if (room.getAliases() != null) {
            for (Alias alias : room.getAliases()) {
                switch (alias.getType()) {
                    case FREEPBX_CONFERENCE_NUMBER:
                        m = roomNumberFromFreePBXNumber.matcher(alias.getValue());
                        if (!m.find()) {
                            throw new CommandException("Invalid E164 number: " + alias.getValue());
                        }
                        roomId = m.group(1);
                        break;
                    default:
                        throw new RuntimeException("Unrecognized alias: " + alias.toString());
                }
            }
        }

        String adminPin = "";
        String userPin = "";
        FreePBXRoomSetting freePBXRoomSetting = room.getRoomSetting(FreePBXRoomSetting.class);
        if (freePBXRoomSetting != null) {
            adminPin = freePBXRoomSetting.getAdminPin() == null ? "" : freePBXRoomSetting.getAdminPin();
            userPin = freePBXRoomSetting.getUserPin() == null ? "" : freePBXRoomSetting.getUserPin();
        }

        attributes.add("userpin", userPin);
        attributes.add("adminpin", adminPin);

        //overriding default option settings
        //c - user count, I - user join/leave, M - music on hold, s - allow menu
/*      EXAMPLE HTTPS request:
        ?display=conferences  &action=add &options=cIMs &account=955 &name=test &userpin=1234 &adminpin=1233 &joinmsg_id=
        &opt%23w= &opt%23o= &opt%23T=T &opt%23q=
        &opt%23c=c &opt%23I=I &opt%23M=M &music=default
        &opt%23s=s  &opt%23r= &opt%23m=*/
        attributes.add("options", "cIMs");
        attributes.add("opt%23c", "c");     //enabled
        attributes.add("opt%23I", "I");     //enabled
        attributes.add("opt%23M", "M");     //enabled
        attributes.add("opt%23s", "s");     //enabled
        attributes.add("music", "default");

        return roomId;
    }

    @Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException, CommandUnsupportedException {
        throw new CommandUnsupportedException();
    }

    @Override
    public void disconnectRoomParticipant(String roomId, String roomParticipantId) throws CommandException, CommandUnsupportedException {
        throw new CommandUnsupportedException();
    }

    public static void main(String[] args) throws Exception {
        String token = "token";
        String tokenKey = "<tokenKey>";

        DeviceAddress address = new DeviceAddress("http://localhost", 33080);
        FreePBXConnector conn = new FreePBXConnector();
        conn.connect(address, token, tokenKey);
        //conn.getRoom("");

    }

    @Override
    public Room getRoom(String roomId) throws CommandException {
        Room room = new Room();
        try {
            logger.debug("GET " + getCallUrl("/conferences/" + roomId, null));
            InputStream response =  execApi("/conferences/" + roomId, null, "GET");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonMap = mapper.readValue(response, Map.class);
            String description = (String)jsonMap.get("description");
            String userpin = (String)jsonMap.get("userpin");
            String adminpin = (String)jsonMap.get("adminpin");


            room.setId(roomId);
            room.addAlias(AliasType.ROOM_NAME, "TODO name"); //TODO get this working
            room.setDescription(description);

            room.setLicenseCount(1);

            room.addTechnology(Technology.FREEPBX);

            room.addAlias(new Alias(AliasType.FREEPBX_CONFERENCE_NUMBER, prefix + roomId));

            // options
            FreePBXRoomSetting freePBXRoomSetting = new FreePBXRoomSetting();
            if (userpin != null) {
                freePBXRoomSetting.setUserPin(userpin);
            }
            if (adminpin != null) {
                freePBXRoomSetting.setAdminPin(adminpin);
            }

            room.addRoomSetting(freePBXRoomSetting);


        } catch (IOException e) {
            //TODO handle exception

        }
        return room;
    }

    @Override
    public Collection<RoomSummary> listRooms() throws CommandException, CommandUnsupportedException {
        throw new CommandUnsupportedException();
    }

    @Override
    public Collection<RoomParticipant> listRoomParticipants(String roomId) throws CommandException, CommandUnsupportedException {
        throw new CommandUnsupportedException();
    }

    @Override
    public void importRoomSettings(String roomId, String settings) throws CommandException, CommandUnsupportedException {
        // TODO?
    }

    @Override
    public Map<String, MediaData> getRoomParticipantSnapshots(String roomId, Set<String> roomParticipantIds) throws CommandException, CommandUnsupportedException {
        return null;
    }

    @Override
    public void clearRoomContent(String roomId) throws CommandException, CommandUnsupportedException {
        throw new CommandUnsupportedException();
    }

    @Override
    public String exportRoomSettings(String roomId) throws CommandException, CommandUnsupportedException {
        throw new CommandUnsupportedException();
    }

    @Override
    public ConnectionState getConnectionState()
    {
        try {
            execApi("/conferences", null, "GET");
            return ConnectionState.CONNECTED;
        }
        catch (Exception exception) {
            logger.warn("Not connected", exception);
            return ConnectionState.DISCONNECTED;
        }
    }
    @Override
    public UsageStats getUsageStats() throws CommandException, CommandUnsupportedException {
        throw new CommandUnsupportedException();
    }

    @Override
    public void modifyRoomParticipants(RoomParticipant roomParticipantConfiguration) throws CommandException, CommandUnsupportedException {
        // TODO
    }

    @Override
    public void modifyRoomParticipant(RoomParticipant roomParticipant) throws CommandException, CommandUnsupportedException {
        // TODO
    }

    @Override
    public void removeRoomContentFile(String roomId, String name) throws CommandException, CommandUnsupportedException {
        throw new CommandUnsupportedException();
    }

    @Override
    protected void onModifyRoom(Room room) throws CommandException {
        createRoom(room);
    }




    /**
     * Connects to the FreePBX server and sets up device information.
     * The communication protocol is stateless, though, so it just gets some info and does not hold the line.
     *
     * @param deviceAddress  device address to connect to
     * @param token token for authentication on the device
     * @param tokenKey token key for authentication on the device
     * @throws cz.cesnet.shongo.api.jade.CommandException
     *
     */
    @Override
    public void connect(DeviceAddress deviceAddress, String token, String tokenKey) throws CommandException {

        // not standard basic auth - using HMAC
        this.token = token;
        this.tokenKey = tokenKey;
        this.deviceAddress = deviceAddress;

        // Load options
        this.prefix = configuration.getOptionStringRequired(CONFERENCE_NUMBER_PREFIX);
        this.roomNumberFromFreePBXNumber = configuration.getOptionPattern(ROOM_NUMBER_EXTRACTION_FROM_FREEPBX_NUMBER);

        //Try to fetch conferences list
        try {
            execApi("/conferences", null, "GET");
        } catch (Exception e) {
            throw new CommandException(e.getMessage(), e);
        }
        logger.info(String.format("Connection to server %s succeeded", deviceAddress));
    }


    /**
     * Retrieves the appropriate URL for specific rest call.
     *
     * @param callPath   to the action to perform
     * @return the URL to perform the action
     */
    protected String getCallUrl(String callPath, RequestAttributeList attributes) throws CommandException
    {
        if (callPath.isEmpty()) {
            throw new CommandException("FreePBX call path cannot be empty.");
        }
        return deviceAddress.getFullUrl() + "/restapi/rest.php/rest" + HttpReqUtils.getCallUrl(callPath, attributes);
    }

    /**
     * Execute command on FreePBX server and returns {@link InputStream} with response.
     *
     * @param actionPath
     * @return response in {@link InputStream}
     * @throws IOException
     */
    protected InputStream execApi(String actionPath, RequestAttributeList attributes, String reqMethod) throws CommandException
    {

        try {
            String callUrl = getCallUrl(actionPath, attributes);
            int retryCount = 5;
            while (retryCount > 0) {
                try {
                    logger.debug(String.format("Calling action %s on %s", actionPath, deviceAddress));
                    HttpURLConnection connection = execApi(callUrl, reqMethod);
                    if (!(200 <= connection.getResponseCode() && connection.getResponseCode() <= 200)) {
                        throw new RequestFailedCommandException("Response: " + connection.getResponseCode() + " " + connection.getResponseMessage());
                    }
                    return connection.getInputStream();
                } catch (IOException exception) {
                    if (isRequestApiRetryPossible(exception)) {
                        retryCount--;
                        logger.warn("{}: Trying again...", exception.getMessage());
                        continue;
                    } else {
                        throw exception;
                    }
                }
            }
            throw new CommandException(String.format("Command %s failed.", actionPath));

        } catch (IOException e) {
            throw new RuntimeException("Command issuing error.", e);
        } catch (SignatureException e) {
            throw new RuntimeException(String.format("Error computing signature for command %s.", actionPath), e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error computing hash functions.", e);
        }

    }

    protected HttpURLConnection execApi(String actionUrl, String reqMethod) throws IOException, SignatureException, NoSuchAlgorithmException {
        String nonce = generateNonce();
        String urlWithoutProtocol;
        if (deviceAddress.isSsl()) {
            urlWithoutProtocol = actionUrl.replace("https://", "");
        } else {
            urlWithoutProtocol = actionUrl.replace("http://", "");

        }
        String dataHash = getDataHash(token, urlWithoutProtocol, reqMethod, nonce, "");
        String signature = computeSignature(tokenKey, dataHash);

        HttpURLConnection connection;
        connection = (HttpURLConnection) new URL(actionUrl).openConnection();
        connection.setRequestMethod(reqMethod);
        connection.setRequestProperty("Signature", signature);
        connection.setRequestProperty("Nonce", nonce);
        connection.setRequestProperty("Token", token);
        connection.connect();
        return connection;
    }

    public static class RequestFailedCommandException extends CommandException
    {

        public RequestFailedCommandException(String message)
        {
            super(message);
        }

        public RequestFailedCommandException(String message, Throwable cause)
        {
            super(message, cause);
        }


    }

    /**
     * @param exception
     * @return true whether given {@code exception} allows to retry the API request, false otherwise
     */
    protected boolean isRequestApiRetryPossible(Exception exception)
    {
        Throwable cause = exception.getCause();
        return (cause instanceof SocketException && cause.getMessage().equals("Connection reset"));
    }



    private  String getSHA256(String data) throws NoSuchAlgorithmException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return byteToHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new NoSuchAlgorithmException("Hashing algorithm not supported.", exception);
        }

    }

    private  String getDataHash(String token, String url, String verb, String nonce, String body) throws NoSuchAlgorithmException {
        String a = getSHA256(url + ":" + verb.toLowerCase());
        String b = getSHA256(token + ":" + nonce);
        String c = getSHA256(Base64.encodeBase64String(body.getBytes()).toString());
        return getSHA256(a + ":" + b + ":" + c );
    }

    /**
     * Generate random nonce.
     * @return
     */
    public String generateNonce() throws NoSuchAlgorithmException{
        try {
            SecureRandom rand = SecureRandom.getInstance("SHA1PRNG");
            Integer randInt = rand.nextInt(9998999) + 1000;
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.update(randInt.toString().getBytes());
            return byteToHex(crypt.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new NoSuchAlgorithmException("Algorithm not supported.", exception);
        }
    }

    private String byteToHex(byte[] hash)
    {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    private String computeSignature(String secretKey, String data) throws SignatureException {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "SHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            byte[] hashBytes = mac.doFinal(data.getBytes());
            return Hex.encodeHexString(hashBytes);
        } catch (Exception exception) {
                throw new SignatureException("Failed to compute signature: ", exception);
        }
    }
}
