package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.domains.InterDomainAction;
import cz.cesnet.shongo.controller.api.domains.response.DomainLogin;
import cz.cesnet.shongo.controller.api.domains.response.DomainResource;
import cz.cesnet.shongo.controller.api.domains.response.DomainStatus;
import cz.cesnet.shongo.ssl.SSLCommunication;
import org.apache.ws.commons.util.Base64;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.persistence.EntityManagerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Foreign domains connector for Inter Domain Agent
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DomainsConnector {

    private final Logger logger = LoggerFactory.getLogger(InterDomainAgent.class);

    private final ConcurrentMap<String, String> clientAccessTokens = new ConcurrentHashMap<>();

    private final ObjectMapper mapper = new ObjectMapper();

    private final ControllerConfiguration configuration;

    private final DomainService domainService;

    private final DomainAdminNotifier notifier;

    private final int COMMAND_TIMEOUT;

    private final int THREAD_TIMEOUT = 500;

    public DomainsConnector(EntityManagerFactory entityManagerFactory, ControllerConfiguration configuration, EmailSender emailSender) {
        domainService = new DomainService(entityManagerFactory);
        this.configuration = configuration;
        COMMAND_TIMEOUT = configuration.getInterDomainCommandTimeout();
        this.notifier = new DomainAdminNotifier(emailSender, configuration);
    }

    protected <T> Map<String, T> performSingleRequest(final InterDomainAction.HttpMethod method, final String action, final Collection<Domain> domains, Class<T> objectClass) {
        final ConcurrentHashMap<String, T> resultMap = new ConcurrentHashMap<>();
        ObjectReader reader = mapper.reader(objectClass);
        performRequest(method, action, domains, reader, resultMap, objectClass);
        return resultMap;
    }

    protected <T> Map<String, List<T>> performListRequest(final InterDomainAction.HttpMethod method, final String action, final Collection<Domain> domains, Class<T> objectClass) {
        final ConcurrentHashMap<String, List<T>> resultMap = new ConcurrentHashMap<>();
        ObjectReader reader = mapper.reader(mapper.getTypeFactory().constructCollectionType(List.class, objectClass));
        performRequest(method, action, domains, reader, resultMap, List.class);
        return resultMap;
    }

    /**
     * Returns map of given domains with positive result, or does not add it at all to the map.
     * @param method of the request
     * @param action to preform
     * @param domains for which the request will be performed and will be returend in map
     * @param reader to parse JSON
     * @param result collection to store the result
     * @param returnClass {@link Class<T>} of the object to return
     * @return result object as instance of given {@code clazz}
     */
    private final synchronized <T> void performRequest(final InterDomainAction.HttpMethod method, final String action,
                                                       final Collection<Domain> domains, final ObjectReader reader,
                                                       final ConcurrentHashMap result, final Class<T> returnClass) {
        final ConcurrentHashSet<Domain> failed = new ConcurrentHashSet<>();

        ExecutorService executor = Executors.newFixedThreadPool(domains.size());
        for (final Domain domain : domains) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        T response = performRequest(method, action, domain, reader, returnClass);
                        if (response != null) {
                            result.put(domain.getCode(), response);
                        }
                    }
                    catch (Exception e) {
                        String message = "Failed to perform request to domain " + domain.getName();
                        logger.warn(message, e);
                        notifier.notifyDomainAdmin(message, e);
                    }
                    finally {
                        if (!result.containsKey(domain.getCode())) {
                            failed.add(domain);
                        }
                    }
                }
            };
            executor.submit(task);
        }

        while (result.size() + failed.size() < domains.size()) {
            try {
                Thread.sleep(THREAD_TIMEOUT);
            } catch (InterruptedException e) {
                continue;
            }
        }
    }

    protected <T> T performSingleRequest(final InterDomainAction.HttpMethod method, final String action, final Domain domain, Class<T> objectClass) {
        return performRequest(method, action, domain, mapper.reader(objectClass), objectClass);
    }

    protected <T> List<T> performListRequest(final InterDomainAction.HttpMethod method, final String action, final Domain domain, Class<T> objectClass) {
        ObjectReader reader = mapper.reader(mapper.getTypeFactory().constructCollectionType(List.class, objectClass));
        return performRequest(method, action, domain, reader, List.class);
    }

    /**
     * Perform request on one foreign domain, returns {@link JSONObject} or throws {@link ForeignDomainConnectException}.
     * @param method {@link cz.cesnet.shongo.controller.api.domains.InterDomainAction.HttpMethod}
     * @param action to perform, uses static variables from {@link InterDomainAction}
     * @param domain for which perform the request
     * @param clazz {@link Class<T>} of the object to return
     * @return result object as instance of given {@code clazz}
     */
    private <T> T performRequest(final InterDomainAction.HttpMethod method, final String action, final Domain domain, final ObjectReader reader, Class<T> clazz) {
        if (action == null || domain == null || reader == null) {
            throw new IllegalArgumentException("Action, domain and reader cannot be null.");
        }

        URL actionUrl = buildRequestUrl(domain, action);
        HttpsURLConnection connection = buildConnection(domain, actionUrl);
        // If basic auth is required
        //TODO !configuration.hasInterDomainPKI() &&
        if (configuration.hasInterDomainBasicAuth()) {
            String accessToken = this.clientAccessTokens.get(domain.getCode());
            if (accessToken == null) {
                accessToken = login(domain);
            }
            String basicAuth = "Basic " + encodeCredentials(accessToken);
            connection.setRequestProperty("Authorization", basicAuth);
        }

        boolean success = true;
        try {
            connection.setRequestMethod(method.getValue());
            switch (method) {
                case GET:
                    connection.setDoInput(true);
                    connection.setRequestProperty("Accept", "application/json");
                    processError(connection, domain);
//                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                    StringBuilder stringBuilder = new StringBuilder();
//                    String responseLine;
//                    while ((responseLine = bufferedReader.readLine()) != null) {
//                        stringBuilder.append(responseLine);
//                    }
                    return reader.readValue(connection.getInputStream());
//                    JSONObject jsonResponse = new JSONObject(stringBuilder.toString());
//                    return jsonResponse;
                case POST:
//                    connection.setDoOutput(true);
//                    connection.setRequestProperty("Content-Type", "application/json");
//                    if (jsonObject != null) {
//                        connection.getOutputStream().write(jsonObject.toString().getBytes());
//                    }
//                    processError(connection, domain);
//                    break;
                case PUT:
                case DELETE:
                    throw new TodoImplementException();
                default:
                    throw new ForeignDomainConnectException(domain, actionUrl.toString(), "Unsupported http method");
            }
        } catch (IOException e) {
            String message = "Connection to " + actionUrl + " failed.";
            logger.error(message, e);
            success = false;
            throw new ForeignDomainConnectException(domain, actionUrl.toString(), e);
        } finally {
            if (success) {
                logger.info("Action: " + actionUrl + " was successful.");
            }
            connection.disconnect();
        }
    }

    protected URL buildRequestUrl(final Domain domain, String action) {
        action = action.trim();
        while (action.startsWith("/")) {
            action = action.substring(1,action.length());
        }
        String actionUrl = domain.getDomainAddress().getFullUrl() + "/" + action;
        try {
            return new URL(actionUrl);
        } catch (MalformedURLException e) {
            String message = "Malformed URL " + actionUrl + ".";
            logger.error(message);
            throw new ForeignDomainConnectException(domain, actionUrl, e);
        }
    }

    protected HttpsURLConnection buildConnection(Domain domain, URL url) {
        HttpsURLConnection connection;
        try {
            connection = (HttpsURLConnection) url.openConnection();
            // For secure connection
            if("HTTPS".equals(url.getProtocol().toUpperCase())) {
                TrustManagerFactory trustManagerFactory = null;
                String certificatePath = domain.getCertificatePath();
                if (certificatePath != null) {
                    KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    trustStore.load(null);
                    trustStore.setCertificateEntry(certificatePath.substring(0, certificatePath.lastIndexOf('.')),
                            SSLCommunication.readPEMCert(certificatePath));
                    trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
                    trustManagerFactory.init(trustStore);
                }

                KeyManagerFactory keyManagerFactory = InterDomainAgent.getInstance().getAuthentication().getKeyManagerFactory();
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory == null ? null : trustManagerFactory.getTrustManagers(), null);

                connection.setSSLSocketFactory(sslContext.getSocketFactory());
            }
            connection.setConnectTimeout(COMMAND_TIMEOUT);
            return connection;
        } catch (IOException e) {
            String message = "Failed to initialize connection for action: " + url;
            logger.error(message, e);
            throw new ForeignDomainConnectException(domain, url.toString(), e);
        } catch (GeneralSecurityException e) {
            String message = "Failed to load client certificate.";
            logger.error(message, e);
            throw new ForeignDomainConnectException(domain, url.toString(), e);
        }
    }

    protected void processError(HttpsURLConnection connection, final Domain domain) {
        String actionUrl = connection.getURL().toString();
        try {
            int errorCode = connection.getResponseCode();
            switch (errorCode) {
                case 400:
                    throw new ForeignDomainConnectException(domain, actionUrl, "400 Bad Request " + connection.getResponseMessage());
                case 401:
                    throw new ForeignDomainConnectException(domain, actionUrl, "401 Unauthorized " + connection.getResponseMessage());
                case 403:
                    throw new ForeignDomainConnectException(domain, actionUrl, "401 Forbidden " + connection.getResponseMessage());
                case 404:
                    throw new ForeignDomainConnectException(domain, actionUrl, "404 Not Found " + connection.getResponseMessage());
                case 500:
                    throw new ForeignDomainConnectException(domain, actionUrl, "500 Internal Server Error " + connection.getResponseMessage());
                default:
                    if (errorCode > 400) {
                        throw new ForeignDomainConnectException(domain, actionUrl, errorCode + " " + connection.getResponseMessage());
                    }
            }
        } catch (IOException e) {
            String message = "Failed to get connection respose code for " + actionUrl;
            logger.error(message);
            throw new ForeignDomainConnectException(domain, actionUrl, e);
        }
    }

    public static String encodeCredentials(String credentials) {
        return new String(new Base64().encode(credentials.getBytes()).replaceAll("\n",""));
    }

    /**
     * Section of Inter Domain Connector actions
     */

    /**
     * Login to foreign {@code domain} with basic authentication
     * @return access token
     */
    public String login(Domain domain) {
        URL loginUrl = buildRequestUrl(domain, InterDomainAction.DOMAIN_LOGIN);
        HttpsURLConnection connection = buildConnection(domain, loginUrl);
        DomainLogin domainLogin = null;
        try {
            String passwordHash = configuration.getInterDomainBasicAuthPasswordHash();

            String userCredentials = LocalDomain.getLocalDomainCode() + ":" + passwordHash;
            String basicAuth = "Basic " + encodeCredentials(userCredentials);
            connection.setRequestProperty("Authorization", basicAuth);
            connection.setRequestMethod(InterDomainAction.HttpMethod.GET.getValue());
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            processError(connection, domain);
            ObjectReader reader = mapper.reader(DomainLogin.class);
            InputStream inputStream = connection.getInputStream();
            domainLogin = reader.readValue(inputStream);
        }
        catch (IOException e) {

        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        String accessToken = domainLogin.getAccessToken();
        this.clientAccessTokens.put(domain.getCode(), accessToken);
        return accessToken;

    }

//TODO smazat
//    public Map<String, Domain> listForeignDomain() {
//        Map<String, Domain> domainsByCode = new HashMap<>();
//        for (Domain domain : domainService.listDomains()) {
//            domainsByCode.put(domain.getCode(), domain);
//        }
//        return domainsByCode;
//    }

    public List<Domain> listForeignDomains() {
        return this.domainService.listDomains(true);
    }

    /**
     * Returns unmodifiable list of statuses of all foreign domains
     * @return
     */
    public List<Domain> getForeignDomainsStatuses() {
        List<Domain> foreignDomains = listForeignDomains();
        Map<String, DomainStatus> response = performSingleRequest(InterDomainAction.HttpMethod.GET, InterDomainAction.DOMAIN_STATUS, foreignDomains, DomainStatus.class);
//        for (Map.Entry<String, DomainStatus> entry : response.entrySet()) {
//            entry.getKey().setStatus(entry.getValue().toStatus());
//        }
        for (Domain domain : foreignDomains) {
            domain.setStatus(response.get(domain.getCode()).toStatus());
        }

        return foreignDomains;
    }

    public Map<String, List<DomainResource>> listForeignResources() {
        Map<String, List<DomainResource>> domainResources = performListRequest(InterDomainAction.HttpMethod.GET, InterDomainAction.DOMAIN_RESOURCES_LIST, listForeignDomains(), DomainResource.class);
        return domainResources;
    }
}
