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
     * Set of existing {@link AliasProviderCapability}.
     */
    private Map<Long, AliasProviderCapability> aliasProviderById = new HashMap<Long, AliasProviderCapability>();

    /**
     * Set of existing {@link RoomProviderCapability}.
     */
    private Map<Long, RoomProviderCapability> roomProviderById = new HashMap<Long, RoomProviderCapability>();

    /**
     * Set of existing {@link RecordingCapability}.
     */
    private Map<Long, RecordingCapability> recordersById = new HashMap<Long, RecordingCapability>();

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
                List<Resource> resourceList = resourceManager.list(null, null);
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

        // Add room provider capability
        RoomProviderCapability roomProvider = resource.getCapability(RoomProviderCapability.class);
        if (roomProvider != null) {
            // Load lazy collections
            roomProvider.loadLazyProperties();
            // Add room provider to the set of existing room providers
            roomProvider.checkPersisted();
            roomProviderById.put(roomProvider.getId(), roomProvider);
        }

        // Add recording capability
        RecordingCapability recorder = resource.getCapability(RecordingCapability.class);
        if (recorder != null) {
            // Load lazy collections
            recorder.loadLazyProperties();
            // Add recorder to the set of existing recorders
            recorder.checkPersisted();
            recordersById.put(recorder.getId(), recorder);
        }

        // Process all alias providers in the resource
        List<AliasProviderCapability> aliasProviders = resource.getCapabilities(AliasProviderCapability.class);
        for (AliasProviderCapability aliasProvider : aliasProviders) {
            // Load lazy collections
            aliasProvider.loadLazyProperties();
            // Add alias provider to the set of existing alias providers
            aliasProvider.checkPersisted();
            aliasProviderById.put(aliasProvider.getId(), aliasProvider);
        }
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

        // Remove room provider capability
        RoomProviderCapability roomProvider = resource.getCapability(RoomProviderCapability.class);
        if (roomProvider != null) {
            roomProviderById.remove(roomProvider.getId());
        }

        // Remove recording capability
        RecordingCapability recorder = resource.getCapability(RecordingCapability.class);
        if (recorder != null) {
            recordersById.remove(recorder.getId());
        }

        // Process all alias providers in the resource
        List<AliasProviderCapability> aliasProviders = resource.getCapabilities(AliasProviderCapability.class);
        for (AliasProviderCapability aliasProvider : aliasProviders) {
            // Remove alias provider from the set of existing alias providers
            aliasProviderById.remove(aliasProvider.getId());
        }
    }

    /**
     * @return collection of existing {@link AliasProviderCapability}s
     */
    public synchronized Collection<AliasProviderCapability> getAliasProviders()
    {
        return aliasProviderById.values();
    }

    /**
     * @return collection of existing {@link RoomProviderCapability}s
     */
    public Collection<RoomProviderCapability> getRoomProviders()
    {
        return roomProviderById.values();
    }

    /**
     * @return collection of existing {@link RoomProviderCapability}s
     */
    public Collection<RecordingCapability> getRecorders()
    {
        return recordersById.values();
    }

    /**
     * @param technologies to be lookup-ed
     * @return list of {@link RoomProviderCapability}s which supports given {@code technologies}
     */
    public Collection<RoomProviderCapability> getRoomProviders(Set<Technology> technologies)
    {
        Set<RoomProviderCapability> roomProviders = new HashSet<RoomProviderCapability>();
        for (RoomProviderCapability roomProvider : roomProviderById.values()) {
            if (technologies == null || roomProvider.getDeviceResource().hasTechnologies(technologies)) {
                roomProviders.add(roomProvider);
            }
        }
        return roomProviders;
    }

    /**
     * @param roomProviderId
     * @return {@link RoomProviderCapability} with given {@code roomProviderId}
     */
    public RoomProviderCapability getRoomProvider(Long roomProviderId)
    {
        return roomProviderById.get(roomProviderId);
    }
}
