package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.ForeignDomainConnectException;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.domains.InterDomainProtocol;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import org.joda.time.Duration;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.List;

/**
 * InterDomain agent for Domain Controller
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class InterDomainAgent implements InterDomainProtocol
{
    public static final Duration OPTION_TIMEOUT_DEFAULT = Duration.standardSeconds(30);

    private static InterDomainAgent instance;

    private static final Logger logger = LoggerFactory.getLogger(InterDomainAgent.class);

    private ResourceService resourceService;

    private ReservationService reservationService;

    private List<Domain> domains;

    protected InterDomainAgent(ControllerConfiguration configuration) {
        setDomains(configuration);
    }

    public static synchronized InterDomainAgent create(ControllerConfiguration configuration)
    {
        if (instance != null) {
            throw new IllegalStateException("Another instance of InterDomain Agent already exists.");
        }
        InterDomainAgent interDomainAgent = new InterDomainAgent(configuration);
        instance = interDomainAgent;
        return instance;
    }

    public static InterDomainAgent getInstance()
    {
        if (instance == null) {
            throw new IllegalStateException("Cannot get instance of a domain controller, "
                    + "because no Inter Domain Agent has been created yet.");
        }
        return instance;
    }

    private void setDomains(ControllerConfiguration configuration)
    {

    }

    public void setResourceService(ResourceService resourceService)
    {
        this.resourceService = resourceService;
    }

    public void setReservationService(ReservationService reservationService)
    {
        this.reservationService = reservationService;
    }

    public static ReservationService getReservationService()
    {
        return getInstance().getReservationService();
    }

    public static ResourceService getResourceService()
    {
        return getInstance().getResourceService();
    }

    public JSONObject performRequest(HttpMethod method, String action, JSONObject jsonObject)
    {
        String actionUrl = action;

        HttpsURLConnection connection;
        try {
            System.setProperty("javax.net.ssl.trustStore", "keystore/keystore.jks");
            System.setProperty("javax.net.ssl.keyStorePassword", "ShoT42paIn");
            KeyStore ks = KeyStore.getInstance("JKS");
            FileInputStream fis = new FileInputStream("keystore/keystore.jks");
            ks.load(fis, "ShoT42paIn".toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, "ShoT42paIn".toCharArray());
//            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
//            tmf.init(ks);
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(kmf.getKeyManagers(), null, null);

            connection = (HttpsURLConnection) new URL(actionUrl).openConnection();
            //connection.setSSLSocketFactory(sc.getSocketFactory());
            connection.setConnectTimeout((int) OPTION_TIMEOUT_DEFAULT.getMillis());
        }
        catch (MalformedURLException e) {
            String message = "Malformed URL \"" + actionUrl + "\".";
            logger.error(message);
            throw new ForeignDomainConnectException(actionUrl,e);
        }
        catch (IOException e) {
            String message = "Failed to initialize connection for action: " + actionUrl;
            logger.error(message);
            throw new ForeignDomainConnectException(actionUrl,e);
        } catch (GeneralSecurityException e) {
            String message = "Failed to load client certificate.";
            logger.error(message);
            throw new ForeignDomainConnectException(actionUrl,e);
        }
        try {
            connection.setRequestMethod(method.getValue());
            if (method.equals(HttpMethod.GET)) {
                connection.setDoInput(true);
                connection.setRequestProperty("Accept", "application/json");
                processError(connection);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String responseLine;
                while ((responseLine = bufferedReader.readLine()) != null) {
                    stringBuilder.append(responseLine);
                }
                JSONObject jsonResponse = new JSONObject(stringBuilder.toString());
                return jsonResponse;
            } else {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                if (jsonObject != null) {
                    connection.getOutputStream().write(jsonObject.toString().getBytes());
                }
                processError(connection);
            }
            connection.disconnect();
            logger.info("Action: " + action + " was successful.");
        } catch (IOException e) {
            String message = "EXECUTION ERROR: " + e + " ON ACTION: " + actionUrl;
            logger.error(message);
            throw new ForeignDomainConnectException(actionUrl, e);
        }
        return null;

    }

    public void processError(HttpsURLConnection connection)
    {
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
                    throw new ForeignDomainConnectException(connection.getURL().toString(), "400 Bad Request - The API call failed because of an error in the input" +
                            " arguments; a JSON error is returned.");
                }
                case 401: {
                    throw new ForeignDomainConnectException(connection.getURL().toString(), "401 Unauthorized - Authentication failed.");
                }
                case 404: {
                    throw new ForeignDomainConnectException(connection.getURL().toString(), "404 Not Found - The API call failed because the endpoint was not found;" +
                            " a JSON error is returned. Refer to Error.");
                }
                case 500: {
                    throw new ForeignDomainConnectException(connection.getURL().toString(), "500 Internal Server Error - An internal error occurred while processing" +
                            " the API call; an error is returned.");
                }
                default: {
                    if (errorCode > 400) {
                        throw new ForeignDomainConnectException(connection.getURL().toString(), errorCode + " " + connection.getResponseMessage());
                    }
                }
            }
        } catch (IOException e) {
            String message = "Error code could not be read from the connection.";
            logger.error(message);
            throw new ForeignDomainConnectException(connection.getURL().toString(), e);
        }

    }

    public enum HttpMethod
    {
        GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE");

        private final String value;

        HttpMethod(String value) {
            this.value = value;
        }

        public String getValue()
        {
            return value;
        }
    }
}
