package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * InterDomain agent for Domain Controller
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class InterDomainAgent {
    private static InterDomainAgent instance;

    private final DomainService domainService;

    private final EntityManagerFactory entityManagerFactory;

    private final CachedDomainsConnector connector;

    private final DomainAuthentication authentication;

    private final DomainAdminNotifier notifier;

    private final Logger logger = LoggerFactory.getLogger(InterDomainAgent.class);

    /**
     * Constructor
     * @param configuration
     */
    protected InterDomainAgent(EntityManagerFactory entityManagerFactory, ControllerConfiguration configuration,
                               EmailSender emailSender) {
        if (configuration == null || !configuration.isInterDomainConfigured()) {
            throw new IllegalStateException("Inter Domain connection is not configured.");
        }

        this.entityManagerFactory = entityManagerFactory;

        domainService = new DomainService(entityManagerFactory);
        domainService.init(configuration);

        this.notifier = new DomainAdminNotifier(logger, emailSender, configuration);
        this.authentication = new DomainAuthentication(entityManagerFactory, configuration, notifier);
        this.connector = new CachedDomainsConnector(entityManagerFactory, configuration, notifier);
//        this.connector = new DomainsConnector(entityManagerFactory, configuration, emailSender);
    }

    synchronized public static InterDomainAgent create(EntityManagerFactory entityManagerFactory,
                                                       ControllerConfiguration configuration, EmailSender emailSender) {
        if (instance != null) {
            throw new IllegalStateException("Another instance of InterDomainAgent already exists.");
        }
        instance = new InterDomainAgent(entityManagerFactory, configuration, emailSender);
        return instance;
    }

    synchronized public static InterDomainAgent getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Cannot get instance of a domain controller, "
                    + "because no InterDomainAgent instance has been created yet.");
        }
        return instance;
    }

    synchronized public static Boolean isInitialized() {
        if (instance != null) {
            return true;
        }
        return false;
    }

    synchronized public static void destroy() {
        if (instance != null) {
            instance.getConnector().getExecutor().shutdownNow();
            instance = null;
        }
    }

    protected DomainService getDomainService() {
        return domainService;
    }

    public CachedDomainsConnector getConnector() {
        return connector;
    }

    protected DomainAuthentication getAuthentication() {
        return authentication;
    }

    protected EntityManager createEntityManager()
    {
        return entityManagerFactory.createEntityManager();
    }

    public void notifyDomainAdmins(String message, Throwable exception)
    {
        this.notifier.notifyDomainAdmins(message, exception);
    }

    public Logger getLogger()
    {
        return this.logger;
    }

    public void logAndNotifyDomainAdmins(String message, Throwable exception)
    {
        this.notifier.logAndNotifyDomainAdmins(message, exception);
    }
}
