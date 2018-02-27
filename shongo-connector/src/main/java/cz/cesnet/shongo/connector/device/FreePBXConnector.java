package cz.cesnet.shongo.connector.device;

import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.UsageStats;
import cz.cesnet.shongo.connector.common.AbstractMultipointConnector;
import cz.cesnet.shongo.connector.common.RequestAttributeList;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Collection;
import java.util.Formatter;
import java.util.Map;
import java.util.Set;

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
        RequestAttributeList attributes = new RequestAttributeList();
        attributes.add("id", roomId);
        logger.debug("DELETE " + getCallUrl("/conferences/", attributes));
        //execApi("/conferences/", attributes, "DELETE");
    }

    @Override
    public String dialRoomParticipant(String roomId, Alias alias) throws CommandException, CommandUnsupportedException {
        throw new CommandUnsupportedException();
    }

    @Override
    public String createRoom(Room room) throws CommandException {
        RequestAttributeList attributes = new RequestAttributeList();


        setRoomAttributes(attributes, room);

        // Room name and id must be filled
        if (attributes.getValue("name") == null) {
            throw new RuntimeException("Room name must be filled for the new room.");
        }
        if (attributes.getValue("id") == null) {
            throw new RuntimeException("Room number must be filled for the new room.");
        }

        logger.debug("PUT " + getCallUrl("/conferences/", attributes));

        //execApi("/conferences/" , attributes, "PUT"); //TODO add conference ID to create
        return null; // TODO return ID of created room
    }

    public void setRoomAttributes (RequestAttributeList attributes, Room room) throws CommandException {

        if (room.getDescription() != null) {
            attributes.add("name", room.getDescription());
        }

        if (room.getAliases() != null) {
            for (Alias alias : room.getAliases()) {
                switch (alias.getType()) {
/*                    case ROOM_NAME: //TODO how to set name of conference room? description vs special name input
                        attributes.add("name", alias.getValue());
                        break;*/
                    case FREEPBX_CONFERENCE_NUMBER:
                        attributes.add("id", alias.getValue());
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
            userPin = freePBXRoomSetting.getUserPin();
        }

        attributes.add("userpin", userPin);
        attributes.add("adminpin", adminPin);

    }

    @Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException, CommandUnsupportedException {
        return null; // TODO??
    }

    @Override
    public void disconnectRoomParticipant(String roomId, String roomParticipantId) throws CommandException, CommandUnsupportedException {
        // TODO??
    }

    @Override
    public Room getRoom(String roomId) throws CommandException {
        return null;
    }

    @Override
    public Collection<RoomSummary> listRooms() throws CommandException, CommandUnsupportedException {
        return null; // TODO?
    }

    @Override
    public Collection<RoomParticipant> listRoomParticipants(String roomId) throws CommandException, CommandUnsupportedException {
        return null;
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
        // TODO
    }


    public static void main(String[] args) throws Exception {
        String token = "token";
        String tokenKey = "<tokenKey>";

        DeviceAddress address = new DeviceAddress("http://localhost", 33080);
        FreePBXConnector conn = new FreePBXConnector();
        conn.connect(address, token, tokenKey);

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

        this.token = token;
        this.tokenKey = tokenKey;
        this.deviceAddress = deviceAddress;

        //Try to fetch conferences list
        try {
            execApi("/conferences", null, "GET");
        } catch (Exception e) {
            throw new CommandException(e.getMessage(), e);
        }
        logger.debug(String.format("Connection to server %s succeeded", deviceAddress));
    }


    /**
     * Retrieves the appropriate URL for specific rest call.
     *
     * @param callPath   to the action to perform
     * @return the URL to perform the action
     */
    protected String getCallUrl(String callPath, RequestAttributeList attributes) throws CommandException
    {
        if (callPath == null || callPath.isEmpty()) {
            throw new CommandException("FreePBX rest call path cannot be empty.");
        }

        String queryString = "";
        if (attributes != null) {
            try {
                queryString = "?" + attributes.getAttributesQuery();

            } catch (UnsupportedEncodingException e) {
                throw new CommandException("Failed to process command " + callPath + ": ", e);
            }
        }
        return deviceAddress.getFullUrl() + "/restapi/rest.php/rest" + callPath + queryString;
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
                    return connection.getInputStream(); //TODO solve 401 authorizatioon error (should get errorStream)
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

    /**
     * @param result Document returned by AC API call
     * @return true if the given {@code result} represents and error,
     *         false otherwise
     */
    private boolean isError(Document result)
    {
        return isError(result.getRootElement());
    }

    /**
     * @param response Element returned by AC API call
     * @return true if the given {@code result} represents and error,
     *         false otherwise
     */
    private boolean isError(Element response)
    {
        Element status = response.getChild("status");
        if (status != null) {
            String code = status.getAttributeValue("code");
            if ("ok".equals(code)) {
                return false;
            }
        }
        return true;
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
