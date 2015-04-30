package cz.cesnet.shongo.controller.domains;

import com.google.common.base.Strings;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.EmailSender;
import cz.cesnet.shongo.controller.ForeignDomainConnectException;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.domains.InterDomainProtocol;

import cz.cesnet.shongo.ssl.SSLComunication;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.persistence.EntityManagerFactory;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * InterDomain agent for Domain Controller
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class InterDomainAgent implements InterDomainProtocol {
    private static InterDomainAgent instance;

    private final ControllerConfiguration configuration;

    private EmailSender emailSender;

    private final Logger logger = LoggerFactory.getLogger(InterDomainAgent.class);

    private final KeyManagerFactory keyManagerFactory;

    private DomainService domainService;

    private int COMMAND_TIMEOUT;
    /**
     * Constructor
     * @param configuration
     */
    protected InterDomainAgent(ControllerConfiguration configuration) {
        if (configuration == null || !configuration.isInterDomainConfigured()) {
            throw new IllegalStateException("Inter Domain connection is not configured.");
        }
        try {
            KeyStore keyStore = KeyStore.getInstance(configuration.getInterDomainSslKeyStoreType());
            FileInputStream keyStoreFile = new FileInputStream(configuration.getInterDomainSslKeyStore());
            keyStore.load(keyStoreFile, configuration.getInterDomainSslKeyStorePassword().toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, configuration.getInterDomainSslKeyStorePassword().toCharArray());
            this.keyManagerFactory = keyManagerFactory;
            this.configuration = configuration;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to load keystore " + configuration.getInterDomainSslKeyStore(), e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read keystore " + configuration.getInterDomainSslKeyStore(), e);
        }
        COMMAND_TIMEOUT = configuration.getInterDomainCommandTimeout();
//TODO: nejaka inicializace domen???
    }

    public static synchronized InterDomainAgent create(ControllerConfiguration configuration) {
        if (instance != null) {
            throw new IllegalStateException("Another instance of InterDomain Agent already exists.");
        }
        InterDomainAgent interDomainAgent = new InterDomainAgent(configuration);
        instance = interDomainAgent;
        return instance;
    }

    public static InterDomainAgent getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Cannot get instance of a domain controller, "
                    + "because no Inter Domain Agent has been created yet.");
        }
        return instance;
    }

    public void init(EntityManagerFactory entityManagerFactory, EmailSender emailSender) {
        domainService = new DomainService(entityManagerFactory);
        this.emailSender = emailSender;
        domainService.init(configuration);
    }

    public Map<X509Certificate, Domain> listForeignDomainCertificates() {
        Map<X509Certificate, Domain> domainsByCert = new HashMap<X509Certificate, Domain>();
        for (Domain domain : domainService.listDomains()) {
            String certificate = domain.getCertificatePath();
            if (Strings.isNullOrEmpty(certificate)) {
                if (domain.getStatus() != null) {
                    continue;
                }
                if (configuration.isInterDomainServerClientAuthForced()) {
                    String message = "Cannot connect to domain " + domain.getName()
                            + ", certificate file does not exist or is not configured.";
                    logger.error(message);
                    notifyDomainAdmin(message, null);
                }
                continue;
            }
            try {
                domainsByCert.put(SSLComunication.readPEMCert(certificate), domain);
            } catch (CertificateException e) {
                String message = "Failed to load certificate file " + certificate;
                logger.error(message, e);
                notifyDomainAdmin(message, e);
            } catch (IOException e) {
                String message = "Cannot read certificate file " + certificate;
                logger.error(message, e);
                notifyDomainAdmin(message, e);
            }
        }
        return domainsByCert;
    }

    public Domain getDomain(X509Certificate certificate) {
        return listForeignDomainCertificates().get(certificate);
    }

    public Domain.Status getStatus(Domain domain) {
        JSONObject response = performRequest(InterDomainAction.HttpMethod.GET, InterDomainAction.DOMAIN_STATUS, domain, null);
        if (Domain.Status.AVAILABLE.toString().equals(response.get("status"))) {
            return Domain.Status.AVAILABLE;
        }
        return Domain.Status.NOT_AVAILABLE;
    }

    private void notifyDomainAdmin(String message, Throwable exception) {
        if (!Strings.isNullOrEmpty(message)) {
            throw new IllegalArgumentException("Message cannot be null or epmty.");
        }
        String subject = "Error in InterDomainAgent";
        if (exception != null) {
            message += "\n";
            message += exception.toString();
        }
        EmailSender.Email emailNotification = new EmailSender.Email(configuration.getAdministratorEmails(), subject, message);
        try {
            emailSender.sendEmail(emailNotification);
        } catch (MessagingException e) {
            logger.error("Failed to send error to domain admins.", e);
        }
    }

    public JSONObject performRequest(InterDomainAction.HttpMethod method, String action, Domain domain, JSONObject jsonObject) {
        if (action == null || domain == null) {
            throw new IllegalArgumentException("Action and domain cannot be null.");
        }
        URL actionUrl = buildRequestUrl(domain, action);
        HttpsURLConnection connection;
        try {
            connection = (HttpsURLConnection) actionUrl.openConnection();
            // For secure connection
            String certificatePath = domain.getCertificatePath();
            if("HTTPS".equals(actionUrl.getProtocol().toUpperCase())) {
                TrustManagerFactory trustManagerFactory = null;
                if (certificatePath != null) {
                    KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    trustStore.load(null);
                    trustStore.setCertificateEntry(certificatePath.substring(0, certificatePath.lastIndexOf('.')),
                            SSLComunication.readPEMCert(certificatePath));
                    trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
                    trustManagerFactory.init(trustStore);
                }

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory == null ? null : trustManagerFactory.getTrustManagers(), null);

                connection.setSSLSocketFactory(sslContext.getSocketFactory());
            }
            connection.setConnectTimeout(COMMAND_TIMEOUT);
        } catch (IOException e) {
            String message = "Failed to initialize connection for action: " + actionUrl;
            logger.error(message, e);
            throw new ForeignDomainConnectException(domain, actionUrl.toString(), e);
        } catch (GeneralSecurityException e) {
            String message = "Failed to load client certificate.";
            logger.error(message, e);
            throw new ForeignDomainConnectException(domain, actionUrl.toString(), e);
        }
        try {
            connection.setRequestMethod(method.getValue());
            switch (method) {
                case GET:
                    connection.setDoInput(true);
                    connection.setRequestProperty("Accept", "application/json");
                    processError(connection, domain);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String responseLine;
                    while ((responseLine = bufferedReader.readLine()) != null) {
                        stringBuilder.append(responseLine);
                    }
                    JSONObject jsonResponse = new JSONObject(stringBuilder.toString());
                    return jsonResponse;
                case POST:
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json");
                    if (jsonObject != null) {
                        connection.getOutputStream().write(jsonObject.toString().getBytes());
                    }
                    processError(connection, domain);
                    break;
                case PUT:
                case DELETE:
                    throw new TodoImplementException();
                default:
                    throw new ForeignDomainConnectException(domain, actionUrl.toString(), "Unsupported http method");
            }
            logger.info("Action: " + actionUrl + " was successful.");
        } catch (IOException e) {
            String message = "Connection to " + actionUrl + " failed.";
            logger.error(message, e);
            throw new ForeignDomainConnectException(domain, actionUrl.toString(), e);
        } finally {
            connection.disconnect();
        }
        return null;

    }

    private URL buildRequestUrl(final Domain domain, String action) {
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

    public void processError(HttpsURLConnection connection, final Domain domain) {
        String actionUrl = connection.getURL().toString();
        try {
            int errorCode = connection.getResponseCode();
            switch (errorCode) {
                case 400:
                    throw new ForeignDomainConnectException(domain, actionUrl, "400 Bad Request " + connection.getResponseMessage());
                case 401:
                    throw new ForeignDomainConnectException(domain, actionUrl, "401 Unauthorized " + connection.getResponseMessage());
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
}
