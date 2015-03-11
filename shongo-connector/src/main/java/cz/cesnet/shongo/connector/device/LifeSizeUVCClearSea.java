package cz.cesnet.shongo.connector.device;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.AliasService;
import cz.cesnet.shongo.connector.common.AbstractDeviceConnector;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created on 10/02/15.
 * @author Martin Kocisky
 */
public class LifeSizeUVCClearSea extends AbstractDeviceConnector implements AliasService {

    private static Logger logger = LoggerFactory.getLogger(LifeSizeUVCClearSea.class);

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
     * Constructor.
     * @param gatekeeperIP IP address of gatekeeper
     */
    public LifeSizeUVCClearSea(String gatekeeperIP) {
        this.gatekeeperIP = gatekeeperIP;
    }

    @Override
    public String createAlias(AliasType aliasType, String e164Number, String roomName) throws CommandException {
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
            throw new CommandException("incorrect baseURL");
        }
    }

    @Override
    public void deleteAlias(String aliasId) throws CommandException {
        performRequest(RequestType.DELETE, ACTION_ACCOUNTS + "/" + aliasId, null);
    }

    @Override
    public void connect(DeviceAddress deviceAddress, String username, String password) throws CommandException {
        this.requestTimeout = (int) configuration.getOptionDuration(OPTION_TIMEOUT, OPTION_TIMEOUT_DEFAULT).getMillis();

        baseURL = deviceAddress.getUrl() + ":" + deviceAddress.getPort();
        serviceUserID = username;
        serviceUserPassword = password;
        connectionState = ConnectionState.DISCONNECTED;
        login();
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
        connectionState = ConnectionState.LOOSELY_CONNECTED;
    }

    /**
     * Retrieves new token if status request returns 401 HTTP response. If 200 OK is returned, the token is still valid.
     * @throws CommandException
     */
    private void checkTokenValidity() throws CommandException {
        try {
            HttpsURLConnection connection = (HttpsURLConnection)new URL(ACTION_STATUS).openConnection();
            int errorCode = connection.getResponseCode();
            if (errorCode == 200) {
                return;
            }
            if (errorCode == 401) {
                connectionState = ConnectionState.DISCONNECTED;
                login();
                return;
            }
            checkError(connection);
        } catch (IOException e) {
            throw new CommandException("IO exception");
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
        String actionUrl = buildURLString(action);
        logger.info("Performing action: " + actionUrl + " ...");

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
            String message = "Failed to initialized connection for action: " + actionUrl;
            logger.error(message);
            throw new CommandException(message,e);
        }

        try {
            //TODO: kde se rozlisuje jestli se jedna o login, nebo jiny request?
            //checkTokenValidity();
            connection.setRequestMethod(requestType.toString());
            if (requestType == RequestType.GET) {
                connection.setDoInput(true);
                connection.setRequestProperty("Accept", "application/json");
                //TODO: libovolny stream se cte cely, ne jen jeden radek, ackoliv se ti vraci JSON
                //TODO: ale i tak se ti obecne nemusi vracet json, ale napr nejaka chyba
                JSONObject jsonResponse = new JSONObject(new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine());
                checkError(connection);
                return jsonResponse;
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
        } catch (IOException e) {
            //TODO: vracet chybu, kterou vraci connection a i puvodni vyjimku, a zalogovat chybu
            throw new CommandException("EXECUTION ERROR: " + e + " ON ACTION: " + actionUrl);
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
                throw new CommandException("alias type must be H323_URI or SIP_URI");
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
                            " arguments; a JSON error is returned.");
                }
                case 401: {
                    throw new CommandException("401 Unauthorized - Authentication failed.");
                }
                case 404: {
                    throw new CommandException("404 Not Found - The API call failed because the endpoint was not found;" +
                            " a JSON error is returned. Refer to Error.");
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
            throw new CommandException("error code could not be read from the connection");
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
}
