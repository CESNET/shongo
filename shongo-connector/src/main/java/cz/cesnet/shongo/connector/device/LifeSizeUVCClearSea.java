package cz.cesnet.shongo.connector.device;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.AliasService;
import cz.cesnet.shongo.connector.common.AbstractDeviceConnector;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

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
     * constructor
     * @param gatekeeperIP IP address of gatekeeper
     */
    public LifeSizeUVCClearSea(String gatekeeperIP) {
        this.gatekeeperIP = gatekeeperIP;
    }

    @Override
    public String createAlias(AliasType alias, String e164Number, String roomName) throws CommandException {
        checkTokenValidity();
        String newUser = "{\"type\":\"User\",\"userID\":\"" + roomName + "\",\"groupName\":\"Aliases\"}";
        performRequest("POST", baseURL + actionAccounts + accessToken, newUser);
        String endP = null;
        //TODO: predelat na SWITCH pouze pro prefix, prejmenovat endP
        if (alias.getTechnology() == Technology.H323) {
            endP = "h323:" + e164Number + "@" + gatekeeperIP;
        } else if (alias.getTechnology() == Technology.SIP) {
            endP = "sip:" + e164Number + "@" + gatekeeperIP;
        }
        assignEndpoint(roomName, endP);
        return null;
    }

    @Override
    //TODO: vracet naformatovany alias ve tvaru jmenoMistnosti@server
    public String getFullAlias(String aliasId) throws CommandException {
        checkTokenValidity();
        return performRequest("GET", baseURL + actionAccounts + "/" + aliasId + accessToken, null);
    }

    @Override
    public void deleteAlias(String aliasId) throws CommandException {
        checkTokenValidity();
        performRequest("DELETE", baseURL + actionAccounts + "/" + aliasId + accessToken, null);
    }

    @Override
    public void connect(DeviceAddress deviceAddress, String username, String password) throws CommandException {
        baseURL = deviceAddress.getUrl() + ":" + deviceAddress.getPort();
        serviceUserID = username;
        serviceUserPassword = password;
        connectionState = ConnectionState.DISCONNECTED;
        getToken();
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
     */
    //TODO: prejmenovat treba na login
    private void getToken() throws CommandException {
        accessToken = accessTokenBase + performRequest("GET",
                baseURL + "/api/v1/access-token/?grant_type=password" +
                        "&username=" + serviceUserID + "&password=" + serviceUserPassword, null).substring(18, 34);
        connectionState = ConnectionState.LOOSELY_CONNECTED;
    }

    /**
     * Retrieves new token if status request returns 401 HTTP response.
     *
     * @throws CommandException
     */
    private void checkTokenValidity() throws CommandException {
        try {
            HttpsURLConnection connection =
                    (HttpsURLConnection)new URL(baseURL + "/api/v2/rest/status" + accessToken).openConnection();
            //TODO: kontrolvoat bud 200 nebo 401, ostatni vyhodit vyjimku, popsat do javadoc
            if (connection.getResponseCode() >= 401) {
                //TODO: pouze zmenit stav
                disconnect();
                getToken();
            }
        } catch (IOException e) {
            throw new CommandException("could not verify token validity");
        }
    }

    /**
     * performRequest function performs the REST action on the specified request and returns a response
     * @param action REST request action GET, POST or DELETE
     * @param request requested information source -> entire URL
     * @param dataToPost data in json format to be sent
     * @return string in JSON format or action name that has no return
     */
    //TODO: predelat checkTokenValidity do performRequest  /  predelat hlavicku performRequest(ENUM requestType, action, JSONConteiner/Map attributes)  /  formatovat atributy zde, ne v metodach
    private String performRequest(String action, String request, String dataToPost) throws CommandException {
        try {
            HttpsURLConnection connection = (HttpsURLConnection)new URL(request).openConnection();
            connection.setRequestMethod(action);
            switch (action) {
                case "GET": {
                    connection.setDoInput(true);
                    connection.setRequestProperty("Accept", "application/json");
                    checkResponseCode(connection.getResponseCode());
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String response = in.readLine();
                    connection.disconnect();
                    return response;
                }
                case "POST": {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json");
                    OutputStream os = connection.getOutputStream();
                    os.write(dataToPost.getBytes());
                    checkResponseCode(connection.getResponseCode());
                    connection.disconnect();
                    return null;
                }
                case "DELETE": {
                    connection.setRequestProperty("Content-Type", "application/json");
                    checkResponseCode(connection.getResponseCode());
                    connection.connect();
                    connection.disconnect();
                    return null;
                }
                default: {
                    throw new CommandException("unknown command" + action);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Assigns endpoint to a user.
     * @param userID user id
     * @param dialString dial string
     */
    private void assignEndpoint(String userID, String dialString) throws CommandException {
        checkTokenValidity();
        String newEndpoint = "{\"userID\":\"" + userID + "\",\"dialString\":\"" + dialString + "\"}";
        performRequest("POST", baseURL + actionEndpoints + accessToken, newEndpoint);
    }

    /**
     * Function throws exceptions for different HTTP error response codes.
     * @param rCode response code
     * @throws CommandException
     */
    //TODO: prejmenovat napriklad na isError(result)
    private void checkResponseCode(int rCode) throws CommandException {
        switch (rCode) {
            case 200: {
                // OK - The API call succeeded with no errors; a JSON Result list of objects is returned.
                break;
            }
            case 204: {
                // No Content - The API call succeeded with no errors.
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
                if (rCode > 400) {
                    throw new CommandException(rCode + " Unknown Error");
                }
            }
        }
    }
}