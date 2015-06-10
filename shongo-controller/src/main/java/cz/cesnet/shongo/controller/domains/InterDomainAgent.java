package cz.cesnet.shongo.controller.domains;

import com.google.common.base.Strings;
import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.EmailSender;
import cz.cesnet.shongo.controller.ForeignDomainConnectException;
import cz.cesnet.shongo.controller.LocalDomain;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.domains.InterDomainAction;
import cz.cesnet.shongo.controller.api.domains.response.DomainLogin;
import cz.cesnet.shongo.controller.api.domains.response.DomainStatus;
import cz.cesnet.shongo.ssl.SSLCommunication;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.ws.commons.util.Base64;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.joda.time.DateTime;
import org.joda.time.Duration;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * InterDomain agent for Domain Controller
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class InterDomainAgent {
    private static InterDomainAgent instance;

    private DomainService domainService;

    private final CachedDomainsConnector connector;

    private final DomainAuthentication authentication;

    /**
     * Constructor
     * @param configuration
     */
    protected InterDomainAgent(EntityManagerFactory entityManagerFactory, ControllerConfiguration configuration,
                               EmailSender emailSender) {
        if (configuration == null || !configuration.isInterDomainConfigured()) {
            throw new IllegalStateException("Inter Domain connection is not configured.");
        }

        domainService = new DomainService(entityManagerFactory);
        domainService.init(configuration);

        this.authentication = new DomainAuthentication(entityManagerFactory, configuration, emailSender);
        this.connector = new CachedDomainsConnector(entityManagerFactory, configuration, emailSender);
//        this.connector = new DomainsConnector(entityManagerFactory, configuration, emailSender);
    }

    public static synchronized InterDomainAgent create(EntityManagerFactory entityManagerFactory,
                                                       ControllerConfiguration configuration, EmailSender emailSender) {
        if (instance != null) {
            throw new IllegalStateException("Another instance of InterDomainAgent already exists.");
        }
        instance = new InterDomainAgent(entityManagerFactory, configuration, emailSender);
        return instance;
    }

    public static InterDomainAgent getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Cannot get instance of a domain controller, "
                    + "because no InterDomainAgent instance has been created yet.");
        }
        return instance;
    }

    public static Boolean isInitialized() {
        if (instance != null) {
            return true;
        }
        return false;
    }

    public static void destroy() {
        if (instance != null) {
            instance.getConnector().getExecutor().shutdownNow();
            instance = null;
        }
    }

    public DomainService getDomainService() {
        return domainService;
    }

    public CachedDomainsConnector getConnector() {
        return connector;
    }

    public DomainAuthentication getAuthentication() {
        return authentication;
    }
}
