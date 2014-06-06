package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability;
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
     * Maximum duration of a {@link cz.cesnet.shongo.controller.booking.room.RoomReservation}.
     */
    private Period roomReservationMaximumDuration;

    /**
     * @see ResourceCache
     */
    private ResourceCache resourceCache;

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
    }

    /**
     * Constructor.
     */
    public Cache(EntityManagerFactory entityManagerFactory)
    {
        this.resourceCache = new ResourceCache();
        this.entityManagerFactory = entityManagerFactory;
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
}
