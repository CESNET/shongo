package cz.cesnet.shongo.connector.device;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.AliasService;
import cz.cesnet.shongo.connector.common.AbstractDeviceConnector;
import cz.cesnet.shongo.controller.api.jade.NotifyTarget;
import cz.cesnet.shongo.controller.api.jade.Service;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Created on 10/02/15.
 * @author Martin Kocisky
 */
public class LifeSizeUVCClearSea extends AbstractDeviceConnector implements AliasService {

    private static final Logger logger = LoggerFactory.getLogger(LifeSizeUVCClearSea.class);
    private static ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);

    /**
     * baseURL for the REST requests, and IP
     */
    private static String baseURL;
    private static String gatekeeperIP;

    /**
     * REST requests
     */
    private static final String API_V2 = "/api/v2/rest/"; 
    private static final String ACTION_ACCOUNTS = "service/accounts";
    private static final String ACTION_ENDPOINTS = "service/endpoints";
    private static final String ACTION_STATUS = "status";
    private static final String ACCESS_TOKEN_BASE = "?access_token=";

    /**
     * access token for the session
     */
    private static String accessToken;

    /**
     * service user credentials used to retrieve access tokens
     */
    private static String serviceUserID;
    private static String serviceUserPassword;

    /**
     * state of the connection
     */
    private static ConnectionState connectionState;

    /**
     * regular expression describing allowed values for the room name.
     */
    private static final String ROOM_NAME_REG_EXP = "[a-zA-Z0-9-_.!~*()&$]{1,32}";

    /**
     * Runnable will attempt to login every 3 hours and once connection is established
     * it will remove itself from the executors pool queue.
     */
    private final Runnable RUNNABLE = new Runnable() {
        @Override
        public void run() {
            try {
                if (connectionState == ConnectionState.DISCONNECTED) {
                    logger.info("Connection attempted.");
                    login();
                    if (connectionState == ConnectionState.LOOSELY_CONNECTED) {
                        logger.info("Connection to ClearSea established.");
                        scheduledThreadPoolExecutor.remove(this);
                    } else {
                        logger.error("Failed to establish connection to ClearSea.");
                    }
                }
            } catch (CommandException e) {
                String message = "Error during ClearSea login attempt.";
                logger.error(message + e.getMessage());
//                    NotifyTarget notifyTarget = new NotifyTarget(Service.NotifyTargetType.RESOURCE_ADMINS);
//                    notifyTarget.addMessage("en",
//                            "Error during ClearSea login attempt.",
//                            "There was an exception during ClearSea login attempt: " + e.getMessage());
//                    notifyTarget.addMessage("cs",
//                            "Chyba počas pokusu o připojení na ClearSea.",
//                            "Byla vyhozena výjimka počas pokusu o připojení na ClearSea:" + e.getMessage());
//                    try {
//                        performControllerAction(notifyTarget);
//                    } catch (CommandException ex) {
//                        logger.error("Failed to send notification" + ex);
//                    }
            }
        }
    };

    /**
     * Constructor.
     * @param gatekeeperIP IP address of gatekeeper
     */
    public LifeSizeUVCClearSea(String gatekeeperIP) {
        this.gatekeeperIP = gatekeeperIP;
        scheduledThreadPoolExecutor.setKeepAliveTime(1, TimeUnit.MINUTES);
    }

    @Override
    public String createAlias(AliasType aliasType, String e164Number, String roomName) throws CommandException {
        if (!Pattern.matches(ROOM_NAME_REG_EXP, roomName)) {
            String message = "Name of the room '" + roomName + "' does not match: " + ROOM_NAME_REG_EXP;
            logger.warn(message);
            throw new IllegalArgumentException(message);
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "User");
        jsonObject.put("userID", roomName);
        jsonObject.put("groupName", "Aliases");
        performRequest(RequestType.POST, ACTION_ACCOUNTS, jsonObject);

        assignEndpoint(roomName, aliasType, e164Number);
        return roomName;
    }

    @Override
    public String getFullAlias(String aliasId) throws CommandException {
        performRequest(RequestType.GET, ACTION_ACCOUNTS + "/" + aliasId, null);
        try {
            return aliasId + "@" + new URL(baseURL).getHost();
        } catch (MalformedURLException e) {
            String message = "Incorrect base URL.";
            logger.error(message);
            throw new CommandException(message, e);
        }
    }

    @Override
    public void deleteAlias(String aliasId) throws CommandException {
        performRequest(RequestType.DELETE, ACTION_ACCOUNTS + "/" + aliasId, null);
    }

    @Override
    public void modifyAlias(String roomName, String newRoomName, AliasType aliasType, String e164Number,
                            String newE164Number) throws CommandException {
        if (roomName == null) {
            String message = "oldRoomName is null, null room cannot be modified";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }
        // modify name
        if (newRoomName != null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userID", newRoomName);
            performRequest(RequestType.POST, ACTION_ACCOUNTS + "/" + roomName, jsonObject);
        }
        // delete old endpoint number
        if (e164Number != null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userID", roomName);
            performRequest(RequestType.DELETE, ACTION_ENDPOINTS + "/" + roomName, jsonObject);
        }
        // add new endpoint number
        if (newE164Number != null) {
            if (aliasType == null) {
                throw new IllegalArgumentException("Provide alias type for new e164 number.");
            }
            assignEndpoint(roomName, aliasType, newE164Number);
        }

    }

    @Override
    public void connect(DeviceAddress deviceAddress, String username, String password) throws CommandException {
        if (configuration != null) {
            this.requestTimeout = (int) configuration.getOptionDuration(OPTION_TIMEOUT, OPTION_TIMEOUT_DEFAULT).getMillis();
        }
        else {
            this.requestTimeout = (int) OPTION_TIMEOUT_DEFAULT.getMillis();
        }

        this.deviceAddress = deviceAddress;
        baseURL = deviceAddress.getUrl() + ":" + deviceAddress.getPort();
        serviceUserID = username;
        serviceUserPassword = password;
        connectionState = ConnectionState.DISCONNECTED;
        attemptConnection();
    }

    @Override
    public ConnectionState getConnectionState() {
        try {
            checkTokenValidity();
        } finally {
            return connectionState;
        }
    }

    @Override
    public void disconnect() throws CommandException {
        baseURL = null;
        serviceUserID = null;
        serviceUserPassword = null;
        accessToken = null;
        connectionState = ConnectionState.DISCONNECTED;
    }

    /**
     * Retrieve new access token.
     * @throws CommandException
     */
    private void login() throws CommandException {
        accessToken = (String) performRequest(RequestType.GET, "&username=" + serviceUserID +
                "&password=" + serviceUserPassword, null).get("access_token");
    }

    /**
     * Retrieves new token if status request returns 401 HTTP response. If 200 OK is returned, the token is still valid.
     * @throws CommandException
     */
    private void checkTokenValidity() throws CommandException {
        try {
            String actionUrl = buildURLString(ACTION_STATUS);
            HttpsURLConnection connection = (HttpsURLConnection)new URL(actionUrl).openConnection();
            connection.setConnectTimeout(this.requestTimeout);
            int errorCode = connection.getResponseCode();
            if (errorCode == 200) {
                return;
            }
            attemptConnection();
            checkError(connection);
        } catch (SocketTimeoutException e) {
            String message = "Timeout in checkTokenValidity.";
            logger.warn(message, e);
            attemptConnection();
        } catch (IOException e) {
            String message = "Failed to initialize connection.";
            logger.warn(message, e);
            attemptConnection();
        }
    }

    /**
     * performRequest function performs the REST request with given action,
     * depending on request converts the attribute map to JSON string and sends the data
     * @param requestType REST request GET, POST or DELETE
     * @param action requested information source -> entire URL
     * @param jsonObject JSON object
     * @return JSON object
     * @throws CommandException
     */
    private JSONObject performRequest(RequestType requestType, String action, JSONObject jsonObject)
            throws CommandException {
        if (connectionState != ConnectionState.DISCONNECTED) {
            checkTokenValidity();
            logger.info("Performing action: " + action + " ...");
        }
        else {
            logger.info("Performing action: login on server " + this.deviceAddress.getHost() + " ...");
        }
        String actionUrl = buildURLString(action);

        HttpsURLConnection connection;
        try {
            connection = (HttpsURLConnection) new URL(actionUrl).openConnection();
            connection.setConnectTimeout(this.requestTimeout);
        }
        catch (MalformedURLException e) {
            String message = "Malformed URL \"" + actionUrl + "\".";
            logger.error(message);
            throw new CommandException(message,e);
        }
        catch (IOException e) {
            String message = "Failed to initialize url connection for action: " + actionUrl;
            logger.error(message);
            throw new CommandException(message,e);
        }
        try {
            connection.setRequestMethod(requestType.toString());
            JSONObject jsonResponse = null;
            if (requestType == RequestType.GET) {
                connection.setRequestProperty("Accept", "application/json");
                checkError(connection);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String responseLine;
                while ((responseLine = bufferedReader.readLine()) != null) {
                    stringBuilder.append(responseLine);
                }
                jsonResponse = new JSONObject(stringBuilder.toString());
            } else {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                if (jsonObject != null) {
                    connection.getOutputStream().write(jsonObject.toString().getBytes());
                }
                checkError(connection);
            }
            connection.disconnect();
            logger.info("Action: " + action + " was successful.");
            return jsonResponse;
        } catch (SocketTimeoutException e) {
            logger.warn("Timeout in performRequest.", e);
        } catch (IOException e) {
            String message = "EXECUTION ERROR: " + e + " ON ACTION: " + actionUrl;
            logger.error(message);
            throw new CommandException(message, e);
        } finally {
            connection.disconnect();
            attemptConnection();
        }
        return null;
    }

    /**
     * Assigns dial string (endpoint) to a user.
     * @param userID user id
     * @param aliasType specifying SIP or H323 protocol
     * @param e164Number possibly a phone number
     * @throws CommandException
     */
    private void assignEndpoint(String userID, AliasType aliasType, String e164Number) throws CommandException {
        String dialString;
        switch (aliasType) {
            case H323_E164: {
                dialString = "h323:";
                break;
            }
            default: {
                String message = "Alias type must be H323_URI (SIP_URI)";
                logger.error(message);
                throw new CommandException(message);
            }
        }
        dialString += e164Number + "@" + gatekeeperIP;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userID", userID);
        jsonObject.put("dialString", dialString);
        performRequest(RequestType.POST, ACTION_ENDPOINTS, jsonObject);
    }

    /**
     * Function throws exceptions for different HTTP connection error response codes.
     * @param connection https connection
     * @throws CommandException
     */
    private void checkError(HttpsURLConnection connection) throws CommandException {
        try {
            int errorCode = connection.getResponseCode();
            switch (errorCode) {
                case 200: {
                    // 200 OK - The API call succeeded with no errors; a JSON Result list of objects is returned.
                    break;
                }
                case 204: {
                    // 204 No Content - The API call succeeded with no errors.
                    break;
                }
                case 400: {
                    throw new CommandException("400 Bad Request - The API call failed because of an error in the input" +
                            " arguments; a JSON error is returned. May have attempted to assign endpoint" +
                            " to a non-existing alias.");
                }
                case 401: {
                    throw new CommandException("401 Unauthorized - Authentication failed.");
                }
                case 404: {
                    throw new CommandException("404 Not Found - The API call failed because the endpoint was not found;" +
                            " a JSON error is returned. Refer to Error. Alias may have been already deleted.");
                }
                case 500: {
                    throw new CommandException("500 Internal Server Error - An internal error occurred while processing" +
                            " the API call; an error is returned.");
                }
                default: {
                    if (errorCode > 400) {
                        throw new CommandException(errorCode + " " + connection.getResponseMessage());
                    }
                }
            }
        } catch (IOException e) {
            String message = "Error code could not be read from the connection.";
            logger.error(message);
            throw new CommandException(message, e);
        }
    }

    /**
     * REST request types enumeration
     */
    public enum RequestType {
        GET, POST, PUT, DELETE
    }

    /**
     * Forms a URL request. If the connection is disconnected request for a token is made.
     * Disconnected state is ensured by connect() and by checkTokenValidity() functions.
     * @param action either request token action or standard action
     * @return url string
     */
    private String buildURLString(String action) {
        if (connectionState == ConnectionState.DISCONNECTED) {
            return baseURL + "/api/v1/access-token/?grant_type=password" + action;
        } else {
            return baseURL + API_V2 + action + ACCESS_TOKEN_BASE + accessToken;
        }
    }

    /**
     * Adds a Runnable method to ScheduledThreadPoolExecutor if not already there.
     */
    private void attemptConnection() {
        connectionState = ConnectionState.DISCONNECTED;
        scheduledThreadPoolExecutor.schedule(RUNNABLE, 3, TimeUnit.HOURS);
//        if (!threadRunning) {
//            connectionState = ConnectionState.DISCONNECTED;
//            threadRunning = true;
//            new Thread() {
//                public void run() {
//                    while (true) {
//                        try {
//                            if (connectionState == ConnectionState.DISCONNECTED) {
//                                // wait between login attempts 3 hours
//                                login();
//                                Thread.sleep(3 * 3600000);
//                            } else {
//                                threadRunning = false;
//                                break;
//                            }
//                        } catch (InterruptedException e) {
//                            Thread.currentThread().interrupt();
//                            logger.info("ClearSea reconnection thread interrupted." + e);
//                        } catch (CommandException e) {
//                            String message = "Error during ClearSea login attempt.";
//                            logger.error(message + e.getMessage());
//
//                            NotifyTarget notifyTarget = new NotifyTarget(Service.NotifyTargetType.RESOURCE_ADMINS);
//                            notifyTarget.addMessage("en",
//                                    "Error during ClearSea login attempt.",
//                                    "There was an exception during ClearSea login attempt: " + e.getMessage());
//                            notifyTarget.addMessage("cs",
//                                    "Chyba počas pokusu o připojení na ClearSea.",
//                                    "Byla vyhozena výjimka počas pokusu o připojení na ClearSea:" + e.getMessage());
//
//                            try {
//                                performControllerAction(notifyTarget);
//                            } catch (CommandException ex) {
//                                logger.error("Failed to send notification" + ex);
//                            }
//                        }
//                    }
//                }
//            }.start();
//        }
    }
}
