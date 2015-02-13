package cz.cesnet.shongo.connector.device;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.AliasService;
import cz.cesnet.shongo.connector.common.AbstractDeviceConnector;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created on 10/02/15.
 * @author Martin Kocisky
 */
public class LifeSizeUVCClearSea extends AbstractDeviceConnector implements AliasService {
    /**
     * baseURL for the REST requests, and IP
     */
    private static String baseURL;
    private static String gatekeeperIP;

    /**
     * REST requests
     */
    private static final String actionAccounts = "/api/v2/rest/service/accounts";
    private static final String actionEndpoints = "/api/v2/rest/service/endpoints";
    private static final String actionStatus = "/api/v2/rest/status";

    /**
     * access token for the session
     */
    private static final String accessTokenBase = "?access_token=";
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
        Map<String,String> newUserData = new LinkedHashMap<String, String>();
        newUserData.put("type", "User");
        newUserData.put("userID", roomName);
        newUserData.put("groupName", "Aliases");
        performRequest(RequestType.POST, baseURL + actionAccounts + accessTokenBase + accessToken, newUserData);
        assignEndpoint(roomName, aliasType, e164Number);
        return roomName;
    }

    @Override
    public String getFullAlias(String aliasId) throws CommandException {
        performRequest(RequestType.GET, baseURL + actionAccounts + "/" + aliasId + accessTokenBase + accessToken, null);
        try {
            return aliasId + "@" + new URL(baseURL).getHost();
        } catch (MalformedURLException e) {
            throw new CommandException("incorrect baseURL");
        }
    }

    @Override
    public void deleteAlias(String aliasId) throws CommandException {
        performRequest(RequestType.DELETE, baseURL + actionAccounts + "/" + aliasId + accessTokenBase + accessToken, null);
    }

    @Override
    public void connect(DeviceAddress deviceAddress, String username, String password) throws CommandException {
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
        accessToken = performRequest(RequestType.GET,
                baseURL + "/api/v1/access-token/?grant_type=password" +
                        "&username=" + serviceUserID + "&password=" + serviceUserPassword, null).get("access_token");
        connectionState = ConnectionState.LOOSELY_CONNECTED;
    }

    /**
     * Retrieves new token if status request returns 401 HTTP response. If 200 OK is returned, the token is still valid.
     * @throws CommandException
     */
    private void checkTokenValidity() throws CommandException {
        try {
            HttpsURLConnection connection = (HttpsURLConnection)new URL(
                    baseURL + actionStatus + accessTokenBase + accessToken).openConnection();
            int errorCode = connection.getResponseCode();
            if (errorCode == 200) {
                return;
            }
            if (errorCode == 401) {
                connectionState = ConnectionState.DISCONNECTED;
                login();
                return;
            }
            isError(connection);
        } catch (IOException e) {
            throw new CommandException("IO exception");
        }
    }

    /**
     * performRequest function performs the REST request with given action,
     * depending on request converts the attribute map to JSON string and sends the data
     * @param requestType REST request GET, POST or DELETE
     * @param action requested information source -> entire URL
     * @param attributeMap attribute / value map
     * @return string in JSON format or action name that has no return
     * @throws CommandException
     */
    private Map<String, String> performRequest(RequestType requestType, String action, Map<String, String> attributeMap)
            throws CommandException {
        try {
            checkTokenValidity();
            HttpsURLConnection connection = (HttpsURLConnection)new URL(action).openConnection();
            connection.setRequestMethod(requestType.toString());
            if (requestType == RequestType.GET) {
                connection.setDoInput(true);
                connection.setRequestProperty("Accept", "application/json");
                isError(connection);
                return JSONStringToAttributeMap(
                        new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine());
            } else {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                if (attributeMap != null) {
                    connection.getOutputStream().write(attributeMapToJSONString(attributeMap).getBytes());
                }
                isError(connection);
            }
            connection.disconnect();
        } catch (IOException e) {
            throw new CommandException("unknown error: " + requestType.toString() + " " + action);
        }
        return null;
    }

    /**
     * Assigns dial string (endpoint) to a user.
     * @param userID 
     * @param aliasType 
     * @param e164Number
     * @throws CommandException
     */
    private void assignEndpoint(String userID, AliasType aliasType, String e164Number) throws CommandException {
        String dialString;
        switch (aliasType) {
            case H323_URI: {
                dialString = "h323:";
                break;
            }
            case SIP_URI: {
                dialString = "sip:";
                break;
            }
            default: {
                throw new CommandException("alias type must be H323_URI or SIP_URI");
            }
        }
        dialString += e164Number + "@" + gatekeeperIP;
        Map<String, String> newEndpointData = new LinkedHashMap<String, String>();
        newEndpointData.put("userID", userID);
        newEndpointData.put("dialString", dialString);
        performRequest(RequestType.POST, baseURL + actionEndpoints + accessTokenBase + accessToken, newEndpointData);
    }

    /**
     * Function throws exceptions for different HTTP connection error response codes.
     * @param connection
     * @throws CommandException
     */
    private void isError(HttpsURLConnection connection) throws CommandException {
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
     * Converts the attribute, value map to JSON string.
     * @param attributeMap
     * @return
     */
    private String attributeMapToJSONString(Map<String, String> attributeMap) {
        int mapSize = attributeMap.size() - 1;
        StringBuilder jsonString = new StringBuilder();
        jsonString.append("{");
        for (Map.Entry entry : attributeMap.entrySet()) {
            jsonString.append("\"").append(entry.getKey()).append("\"");
            jsonString.append(":");
            jsonString.append("\"").append(entry.getValue()).append("\"");
            if (mapSize > 0) {
                jsonString.append(",");
                mapSize--;
            }
        }
        jsonString.append("}");
        return jsonString.toString();
    }

    /**
     * Parses JSON string to a map of attributes and values.
     * @param jsonString
     * @return
     */
    private Map<String, String> JSONStringToAttributeMap(String jsonString) {
        Map<String, String> attributeMap = new LinkedHashMap<>();
        jsonString = jsonString.replace(" ", "");
        jsonString = jsonString.replace("{", "");
        jsonString = jsonString.replace("}", "");
        String[] jsonSplit = jsonString.split(",");
        for (String jsonPair : jsonSplit) {
            String[] attributeSplit = jsonPair.split(":");
            String key = attributeSplit[0].replace("\"", "");
            String value = attributeSplit[1].replace("\"", "");
            attributeMap.put(key, value);
        }
        return attributeMap;
    }
}
