package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.domains.InterDomainAction;
import cz.cesnet.shongo.controller.api.domains.response.DomainLogin;
import cz.cesnet.shongo.controller.api.domains.response.DomainCapability;
import cz.cesnet.shongo.controller.api.domains.response.DomainStatus;
import cz.cesnet.shongo.controller.api.domains.response.Reservation;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.resource.ForeignResources;
import cz.cesnet.shongo.controller.scheduler.SchedulerContext;
import cz.cesnet.shongo.ssl.SSLCommunication;
import org.apache.ws.commons.util.Base64;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.SerializationConfig;
import org.joda.time.Interval;
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
import java.util.*;
import java.util.concurrent.*;

/**
 * Foreign domains connector for Inter Domain Agent
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DomainsConnector
{
    private final Integer CORE_POOL_SIZE = 10;

    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(CORE_POOL_SIZE);

    private final Logger logger = LoggerFactory.getLogger(InterDomainAgent.class);

    private final ConcurrentMap<String, String> clientAccessTokens = new ConcurrentHashMap<>();

    protected final ObjectMapper mapper = new ObjectMapper();

    private final EntityManagerFactory entityManagerFactory;

    private final ControllerConfiguration configuration;

    private final DomainService domainService;

    private final DomainAdminNotifier notifier;

    private final int COMMAND_TIMEOUT;

    private final int THREAD_TIMEOUT = 500;

    public DomainsConnector(EntityManagerFactory entityManagerFactory, ControllerConfiguration configuration, EmailSender emailSender)
    {
        this.entityManagerFactory = entityManagerFactory;
        domainService = new DomainService(entityManagerFactory);
        this.configuration = configuration;
        COMMAND_TIMEOUT = configuration.getInterDomainCommandTimeout();
        this.notifier = new DomainAdminNotifier(emailSender, configuration);
        mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    protected ScheduledThreadPoolExecutor getExecutor()
    {
        return executor;
    }

    public ControllerConfiguration getConfiguration()
    {
        return configuration;
    }

    protected <T> Map<String, T> performTypedRequests(final InterDomainAction.HttpMethod method, final String action,
                                                      final Map<String, String> parameters, final Collection<Domain> domains,
                                                      Class<T> objectClass)
    {
        final Map<String, T> resultMap = new HashMap<>();
        ObjectReader reader = mapper.reader(objectClass);
        performRequests(method, action, parameters, domains, reader, resultMap, objectClass);
        return resultMap;
    }

    protected <T> Map<String, List<T>> performTypedListRequests(final InterDomainAction.HttpMethod method, final String action,
                                                                final Map<String, String> parameters, final Collection<Domain> domains,
                                                                Class<T> objectClass)
    {
        final Map<String, List<T>> resultMap = new HashMap<>();
        ObjectReader reader = mapper.reader(mapper.getTypeFactory().constructCollectionType(List.class, objectClass));
        performRequests(method, action, parameters, domains, reader, resultMap, List.class);
        return resultMap;
    }

    /**
     * Returns map of given domains with positive result, or does not add it at all to the map.
     *
     * @param method      of the request
     * @param action      to preform
     * @param domains     for which the request will be performed and will be returned in map
     * @param reader      to parse JSON
     * @param result      collection to store the result
     * @param returnClass {@link Class<T>} of the object to return
     * @return result object as instance of given {@code clazz}
     */
    protected synchronized <T> void performRequests(final InterDomainAction.HttpMethod method, final String action,
                                                    final Map<String, String> parameters, final Collection<Domain> domains,
                                                    final ObjectReader reader, final Map<String, ?> result,
                                                    final Class<T> returnClass)
    {
        final ConcurrentMap<String, Future<T>> futureTasks = new ConcurrentHashMap<>();

        for (final Domain domain : domains) {
            Callable<T> task = new DomainTask<T>(method, action, parameters, domain, reader, returnClass, result, null);
            futureTasks.put(domain.getName(), executor.submit(task));
        }

        while (!futureTasks.isEmpty()) {
            try {
                Iterator<Map.Entry<String, Future<T>>> i = futureTasks.entrySet().iterator();
                while (i.hasNext()) {
                    Map.Entry<String, Future<T>> entry = i.next();
                    if (entry.getValue().isDone()) {
                        futureTasks.remove(entry.getKey());
                    }
                }
                Thread.sleep(THREAD_TIMEOUT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                continue;
            }
        }
    }

//    protected <T> T performRequest(final InterDomainAction.HttpMethod method, final String action, final Map<String, String> parameters, final Domain domain, Class<T> objectClass)
//    {
//        return performRequest(method, action, parameters, domain, mapper.reader(objectClass), objectClass);
//    }
//
//    protected <T> List<T> performRequest(final InterDomainAction.HttpMethod method, final String action, final Map<String, String> parameters, final Domain domain, Class<T> objectClass)
//    {
//        ObjectReader reader = mapper.reader(mapper.getTypeFactory().constructCollectionType(List.class, objectClass));
//        return performRequest(method, action, parameters, domain, reader, List.class);
//    }

    /**
     * Perform request on one foreign domain, returns {@link JSONObject} or throws {@link ForeignDomainConnectException}.
     *
     * @param method {@link cz.cesnet.shongo.controller.api.domains.InterDomainAction.HttpMethod}
     * @param action to perform, uses static variables from {@link InterDomainAction}
     * @param domain for which perform the request
     * @param clazz  {@link Class<T>} of the object to return
     * @return result object as instance of given {@code clazz}
     */
    private <T> T performRequest(final InterDomainAction.HttpMethod method, final String action, final Map<String, String> parameters, final Domain domain, final ObjectReader reader, Class<T> clazz)
    {
        if (action == null || domain == null || reader == null) {
            throw new IllegalArgumentException("Action, domain and reader cannot be null.");
        }
        URL actionUrl = buildRequestUrl(domain, action, parameters);
        logger.debug(String.format("Calling action %s on domain %s", actionUrl, domain.getName()));
        HttpsURLConnection connection = buildConnection(domain, actionUrl);
        // If basic auth is required
        //TODO !configuration.hasInterDomainPKI() &&
        if (configuration.hasInterDomainBasicAuth()) {
            String accessToken = this.clientAccessTokens.get(domain.getName());
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
//                    DEBUG:
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String responseLine;
                    while ((responseLine = bufferedReader.readLine()) != null) {
                        stringBuilder.append(responseLine);
                    }
                    return reader.readValue(stringBuilder.toString());
//                    return reader.readValue(connection.getInputStream());
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
        } catch (Exception e) {
            String message = "Failed to perform request (" + actionUrl + ") to domain " + domain.getName();
            logger.error(message, e);
            success = false;
            throw new ForeignDomainConnectException(domain, actionUrl.toString(), e);
        } finally {
            if (success) {
                logger.debug("Action: " + actionUrl + " was successful.");
            }
            connection.disconnect();
        }
    }

    protected URL buildRequestUrl(final Domain domain, String action, Map<String, String> parameters)
    {
        action = action.trim();
        while (action.startsWith("/")) {
            action = action.substring(1, action.length());
        }
        StringBuilder parametersBuilder = new StringBuilder();
        if (parameters != null) {
            parametersBuilder.append("?");
            boolean first = true;
            for (Map.Entry<String, String> parameter : parameters.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    parametersBuilder.append("&");
                }
                parametersBuilder.append(parameter.getKey() + "=" + parameter.getValue());
            }
        }
        String actionUrl = domain.getDomainAddress().getFullUrl() + "/" + action + parametersBuilder.toString();
        try {
            return new URL(actionUrl);
        } catch (MalformedURLException e) {
            String message = "Malformed URL " + actionUrl + ".";
            logger.error(message);
            throw new ForeignDomainConnectException(domain, actionUrl, e);
        }
    }

    protected HttpsURLConnection buildConnection(Domain domain, URL url)
    {
        HttpsURLConnection connection;
        try {
            connection = (HttpsURLConnection) url.openConnection();
            // For secure connection
            if ("HTTPS".equals(url.getProtocol().toUpperCase())) {
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

    protected void processError(HttpsURLConnection connection, final Domain domain)
    {
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

    public static String encodeCredentials(String credentials)
    {
        return new String(new Base64().encode(credentials.getBytes()).replaceAll("\n", ""));
    }

    /**
     * Section of Inter Domain Connector actions
     */

    /**
     * Login to foreign {@code domain} with basic authentication
     *
     * @return access token
     */
    public String login(Domain domain)
    {
        URL loginUrl = buildRequestUrl(domain, InterDomainAction.DOMAIN_LOGIN, null);
        HttpsURLConnection connection = buildConnection(domain, loginUrl);
        DomainLogin domainLogin = null;
        try {
            String passwordHash = configuration.getInterDomainBasicAuthPasswordHash();

            String userCredentials = LocalDomain.getLocalDomainShortName() + ":" + passwordHash;
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
        } catch (IOException e) {
            logger.error("Failed to perform login to domain.", e);
            throw new ForeignDomainConnectException(domain, loginUrl.toString(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        String accessToken = domainLogin.getAccessToken();
        this.clientAccessTokens.put(domain.getName(), accessToken);
        return accessToken;

    }

    /**
     * @return all domains, even the ones that are not allocatable
     */
    public List<Domain> listForeignDomains()
    {
        return this.domainService.listForeignDomains();
    }

    /**
     * @return all foreign domains used for allocation
     */
    public List<Domain> listAllocatableForeignDomains()
    {
        return this.domainService.listDomains(true, true);
    }

    /**
     * Test if domain by given name exists and is allocatable.
     * @param domainName
     * @return
     */
    public boolean isDomainAllocatable(String domainName)
    {
        try {
            Domain domain = this.domainService.findDomainByName(domainName);
            return domain.isAllocatable();
        }
        catch (CommonReportSet.ObjectNotExistsException ex) {
            return false;
        }
    }

    /**
     * Returns unmodifiable list of statuses of all foreign domains
     *
     * @return
     */
    public List<Domain> getForeignDomainsStatuses()
    {
        List<Domain> foreignDomains = listForeignDomains();
        Map<String, DomainStatus> response = performTypedRequests(InterDomainAction.HttpMethod.GET, InterDomainAction.DOMAIN_STATUS, null, foreignDomains, DomainStatus.class);
        for (Domain domain : foreignDomains) {
            DomainStatus status = response.get(domain.getName());
            domain.setStatus(status == null ? Domain.Status.NOT_AVAILABLE : status.toStatus());
        }

        return foreignDomains;
    }

    public Map<String, List<DomainCapability>> listForeignCapabilities(DomainCapabilityListRequest request)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("type", request.getCapabilityType().toString());
        if (request.getInterval() != null) {
            parameters.put("interval", request.getInterval().toString());
        }
        if (request.getTechnology() != null) {
            parameters.put("technology", request.getTechnology().toString());
        }
        List<Domain> domains;
        if (request.getDomain() == null) {
            domains = listAllocatableForeignDomains();
        }
        else {
            domains = new ArrayList<>();
            domains.add(request.getDomain());
        }
        // Resource IDs are not filtered by inter domain protocol
        Map<String, List<DomainCapability>> domainResources = performTypedListRequests(InterDomainAction.HttpMethod.GET,
                InterDomainAction.DOMAIN_CAPABILITY_LIST, parameters, domains, DomainCapability.class);
        return domainResources;
    }

    /**
     *
     *
     * @return
     */
    public Reservation allocateResource(SchedulerContext schedulerContext, Interval slot, ForeignResources foreignResources)
    {
        Domain domain = foreignResources.getDomain().toApi();
        ObjectReader reader = mapper.reader(Reservation.class);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("slot", slot.toString());
        parameters.put("type", DomainCapabilityListRequest.Type.RESOURCE.toString());
        parameters.put("resourceId", ObjectIdentifier.formatId(foreignResources));
        parameters.put("userId", schedulerContext.getUserId());

        Reservation reservation = performRequest(InterDomainAction.HttpMethod.GET, InterDomainAction.DOMAIN_ALLOCATE, parameters, domain, reader, Reservation.class);
        if (reservation.getSlot() == null) {
            reservation.setSlot(slot);
        }
        return reservation;
    }

    /**
     *
     *
     * @return
     */
    public Reservation getReservationByRequest(Domain domain, String foreignReservationRequestId)
    {
        ObjectReader reader = mapper.reader(Reservation.class);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("reservationRequestId", foreignReservationRequestId);

        Reservation reservation = performRequest(InterDomainAction.HttpMethod.GET, InterDomainAction.DOMAIN_RESERVATION_DATA, parameters, domain, reader, Reservation.class);

        return reservation;
//        switch (reservation.getType()) {
//            case RESOURCE:
//            case VIRTUAL_ROOM:
//            default:
//                throw new TodoImplementException("Unsupported type of reservation for inter domain protocol.");
//        }
    }

    /**
     * Represents action to be called on domain. Returns result after successful call. If {@code result} or
     * {@code unavailableDomains} are set, result or failed action will be written to them (synchronized on the {@code result}).
     * @param <T> class of the result
     */
    protected class DomainTask<T> implements Callable<T>, Runnable
    {
        /**
         * Http method of the action
         */
        private InterDomainAction.HttpMethod method;

        /**
         * URL path of the action to call
         */
        private String action;

        /**
         * GET parameters of the action
         */
        private Map<String, String> parameters;

        /**
         * Domain for which will the action be called
         */
        private Domain domain;

        /**
         * Reader to parse the JSON result
         */
        private ObjectReader reader;

        /**
         * Class of the result to be returned
         */
        private Class<T> returnClass;

        /**
         * Result map to be filled
         */
        private Map<String, ?> result;

        /**
         * Set of domains for which the action fails
         */
        private Set<String> unavailableDomains;

        public DomainTask(final InterDomainAction.HttpMethod method, final String action,
                          final Map<String, String> parameters, final Domain domain,
                          final ObjectReader reader, final Class<T> returnClass,
                          final Map<String, ?> result, final Set<String> unavailableDomains)
        {
            this.method = method;
            this.action = action;
            this.parameters = parameters;
            this.domain = domain;
            this.reader = reader;
            this.returnClass = returnClass;
            this.result = result;
            this.unavailableDomains = unavailableDomains;
        }

        /**
         * Callable will be terminated (throws {@link IllegalStateException}) only if domains does not exist.
         *
         * @return
         */
        @Override
        public T call()
        {
            if (!Thread.currentThread().getName().contains("domainTask")) {
                Thread.currentThread().setName(Thread.currentThread().getName() + "-domainTask-" + domain.getName());
            }
            boolean failed = true;
            try {
                if (InterDomainAgent.getInstance().getDomainService().getDomain(domain.getId()) == null) {
                    terminateDomainTask();
                }
                T response = performRequest(method, action, parameters, domain, reader, returnClass);
                if (result != null && response != null) {
                    synchronized (result) {
                        ((Map<String, T>) result).put(domain.getName(), response);
                        if (unavailableDomains != null) {
                            unavailableDomains.remove(domain.getName());
                        }
                    }
                    failed = false;
                }
                return response;
            } catch (IllegalStateException e) {
                // Thread will be terminated (in {@link ScheduledThreadPoolExecutor}).
                throw e;
            } catch (Exception e) {
                try {
                    notifier.notifyDomainAdmin("Failed to perform request to domain " + domain.getName(), e);
                } catch (Exception notifyEx) {
                    logger.error("Notification has failed.", notifyEx);
                }
                return null;
            } finally {
                // If {@code unavailableDomains} is set and request failed add it and also to {@link result}
                // with empty {@code ArrayList} if possible.
                // {@code unavailableDomains} is used only by CachedDomainsConnector.
                if (unavailableDomains != null && failed) {
                    synchronized (result) {
                        if (!result.containsKey(domain.getName())) {
                            try {
                                result.put(domain.getName(), null);
                            }
                            catch (ClassCastException ex) {
                                // Ignore
                            }
                        }
                        unavailableDomains.add(domain.getName());
                    }
                }
            }
        }

        /**
         * Runnable will be terminated (throws {@link IllegalStateException}) if domain does not exist or is no allocatable.
         */
        @Override
        public void run()
        {
            if (!Thread.currentThread().getName().contains("domainTask")) {
                Thread.currentThread().setName(Thread.currentThread().getName() + "-domainTask-" + domain.getName());
            }
            Domain internalDomain = InterDomainAgent.getInstance().getDomainService().getDomain(domain.getId());
            if (internalDomain == null || !internalDomain.isAllocatable()) {
                terminateDomainTask();
            }
            call();
        }

        private void terminateDomainTask() throws IllegalStateException
        {
            synchronized (result) {
                if (result != null) {
                    result.remove(domain.getName());
                }
                if (unavailableDomains != null) {
                    unavailableDomains.remove(domain.getName());
                }
            }
            logger.info("Domain '" + domain.getName() + "' does not exist or is not allocatable. Domain task terminated.");
            throw new IllegalStateException("Domain '" + domain.getName() + "' does not exist or is not allocatable.");
        }
    }
}
