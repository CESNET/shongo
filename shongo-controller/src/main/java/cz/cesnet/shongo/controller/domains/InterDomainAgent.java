package cz.cesnet.shongo.controller.domains;

import com.google.common.base.Strings;
import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.EmailSender;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ReservationServiceImpl;

import javax.persistence.EntityManagerFactory;

/**
 * InterDomain agent for Domain Controller
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class InterDomainAgent {
    private static InterDomainAgent instance;

    private DomainService domainService;

    private EntityManagerFactory entityManagerFactory;

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

        this.entityManagerFactory = entityManagerFactory;

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

    protected DomainService getDomainService() {
        return domainService;
    }

    public CachedDomainsConnector getConnector() {
        return connector;
    }

    protected DomainAuthentication getAuthentication() {
        return authentication;
    }

    protected EntityManagerFactory getEntityManagerFactory()
    {
        return entityManagerFactory;
    }
}
