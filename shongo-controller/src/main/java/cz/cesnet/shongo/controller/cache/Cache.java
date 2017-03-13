package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability;
import cz.cesnet.shongo.controller.booking.domain.Domain;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.booking.room.RoomProviderCapability;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Represents a component for a domain controller that holds cached data.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Cache extends Component implements Component.EntityManagerFactoryAware
{
    private static Logger logger = LoggerFactory.getLogger(Cache.class);

    /**
     * iCalendar reservation data for resources with public calendar.
     */
    private Map<String,String> iCalReservationsByResourceId = new HashMap<>();

    /**
     * Maximum duration of a {@link cz.cesnet.shongo.controller.booking.room.RoomReservation}.
     */
    private Period roomReservationMaximumDuration;
    /**
     * @see ResourceCache
     */
    private ResourceCache resourceCache;

    /**
     * @see DomainCache
     */
    private DomainCache domainCache;

    /**
     * {@link EntityManagerFactory} used to load resources in {@link #init(cz.cesnet.shongo.controller.ControllerConfiguration)} method.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * Constructor.
     */
    public Cache()
    {
        resourceCache = new ResourceCache();
        domainCache = new DomainCache();
    }

    /**
     * Constructor.
     */
    public Cache(EntityManagerFactory entityManagerFactory)
    {
        this.resourceCache = new ResourceCache();
        domainCache = new DomainCache();
        this.entityManagerFactory = entityManagerFactory;
    }

    public void addICalReservation (String resourceId, String iCalendarData)
    {
        iCalReservationsByResourceId.put(resourceId, iCalendarData);
    }

    public String getICalReservation (String resourceId)
    {
        return iCalReservationsByResourceId.get(resourceId);

    }

    public void removeICalReservation (String resourceId)
    {
        iCalReservationsByResourceId.remove(resourceId);
    }

    public void refreshICalReservation (String resourceId) {
        removeICalReservation(resourceId);

    }

    /**
     * @return {@link #roomReservationMaximumDuration}
     */
    public Period getRoomReservationMaximumDuration()
    {
        return roomReservationMaximumDuration;
    }

    /**
     * @return {@link #resourceCache}
     */
    public ResourceCache getResourceCache()
    {
        return resourceCache;
    }

    /**
     * @return {@link #domainCache}
     */
    public DomainCache getDomainCache()
    {
        return domainCache;
    }

    @Override
    public synchronized void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public synchronized void init(ControllerConfiguration configuration)
    {
        super.init(configuration);

        roomReservationMaximumDuration = configuration.getPeriod(ControllerConfiguration.RESERVATION_ROOM_MAX_DURATION);

        logger.debug("Starting cache...");

        if (entityManagerFactory != null) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            try {
                logger.debug("Loading resources...");
                ResourceManager resourceManager = new ResourceManager(entityManager);
                List<Resource> resourceList = resourceManager.list();
                for (Resource resource : resourceList) {
                    try {
                        addResource(resource);
                    }
                    catch (Exception exception) {
                        throw new RuntimeException("Failed to add resource to the cache.", exception);
                    }
                }

                logger.debug("Loading domains...");
                List<Domain> domainList = resourceManager.listAllDomains();
                for (Domain domain : domainList) {
                    try {
                        addDomain(domain);
                    }
                    catch (Exception exception) {
                        throw new RuntimeException("Failed to add domain to the cache.", exception);
                    }
                }
            }
            finally {
                entityManager.close();
            }
        }
    }

    @Override
    public synchronized void destroy()
    {
        logger.debug("Stopping cache...");

        super.destroy();
    }

    /**
     * @param resource
     */
    public synchronized void addResource(Resource resource)
    {
        // Add resource to resource cache
        resource.checkPersisted();
        resource.loadLazyProperties();
        resourceCache.addObject(resource);
    }

    /**
     * @param resource
     */
    public synchronized void addResource(Resource resource, EntityManager entityManager)
    {
        ResourceManager resourceManager = new ResourceManager(entityManager);
        resourceManager.create(resource);
        addResource(resource);
    }

    /**
     * Update resource in the cache.
     *
     * @param resource
     */
    public synchronized void updateResource(Resource resource)
    {
        removeResource(resource);
        try {
            addResource(resource);
        }
        catch (Exception exception) {
            throw new RuntimeException("Failed to update resource in the resource cache.", exception);
        }
    }

    /**
     * Remove resource from the {@link Cache}.
     *
     * @param resource to be removed
     */
    public synchronized void removeResource(Resource resource)
    {
        // Remove resource from resource cache
        resourceCache.removeObject(resource);
    }

    /**
     * @param domain
     */
    public synchronized void addDomain(Domain domain)
    {
        // Add domain to resource cache
        domain.checkPersisted();
        domain.loadLazyProperties();
        domainCache.addObject(domain);
    }

    /**
     * Update domain in the cache.
     *
     * @param domain
     */
    public synchronized void updateDomain(Domain domain)
    {
        removeDomain(domain);
        try {
            addDomain(domain);
        }
        catch (Exception exception) {
            throw new RuntimeException("Failed to update domain in the domain cache.", exception);
        }
    }

    /**
     * Remove domain from the {@link Cache}.
     *
     * @param domain to be removed
     */
    public synchronized void removeDomain(Domain domain)
    {
        // Remove domain from domain cache
        domainCache.removeObject(domain);
    }
}
