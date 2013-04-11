package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.cache.ReusedReservationCache;
import cz.cesnet.shongo.controller.cache.RoomCache;
import cz.cesnet.shongo.controller.cache.ValueCache;
import cz.cesnet.shongo.controller.reservation.*;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.controller.resource.value.ValueProvider;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a component for a domain controller that holds cached data.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Cache extends Component implements Component.EntityManagerFactoryAware
{
    private static Logger logger = LoggerFactory.getLogger(Cache.class);

    /**
     * Maximum duration of a {@link ResourceReservation}.
     */
    private Period resourceReservationMaximumDuration;

    /**
     * Maximum duration of a {@link AliasReservation}.
     */
    private Period valueReservationMaximumDuration;

    /**
     * @see ResourceCache
     */
    private ResourceCache resourceCache;

    /**
     * @see ValueCache
     */
    private ValueCache valueCache;

    /**
     * @see RoomCache
     */
    private RoomCache roomCache;

    /**
     * @see ReusedReservationCache
     */
    private ReusedReservationCache reusedReservationCache = new ReusedReservationCache();

    /**
     * Set of existing {@link AliasProviderCapability}.
     */
    private Map<Long, AliasProviderCapability> aliasProviderById = new HashMap<Long, AliasProviderCapability>();

    /**
     * Working interval for which are loaded allocated virtual rooms.
     */
    private Interval workingInterval;

    /**
     * Represents a reference data time which is a rounded now().
     */
    private DateTime referenceDateTime;

    /**
     * {@link EntityManagerFactory} used to load resources in {@link #init(Configuration)} method.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * Specifies whether resources and allocations don't have to be persisted before they are added to the resource
     * database (useful for testing purposes).
     */
    private boolean generateTestingIds = false;

    /**
     * Constructor.
     */
    public Cache()
    {
        resourceCache = new ResourceCache();
        valueCache = new ValueCache();
        roomCache = new RoomCache(resourceCache);
    }

    /**
     * @return new instance of {@link Cache} for testing purposes (without connection to the database)
     */
    public static Cache createTestingCache()
    {
        Cache cache = new Cache();
        cache.setGenerateTestingIds();
        cache.init();
        return cache;
    }

    /**
     * @return {@link #resourceReservationMaximumDuration}
     */
    public Period getResourceReservationMaximumDuration()
    {
        return resourceReservationMaximumDuration;
    }

    /**
     * @return {@link #valueReservationMaximumDuration}
     */
    public Period getValueReservationMaximumDuration()
    {
        return valueReservationMaximumDuration;
    }

    /**
     * @return {@link #resourceCache}
     */
    public ResourceCache getResourceCache()
    {
        return resourceCache;
    }

    /**
     * @return {@link #valueCache}
     */
    public ValueCache getValueCache()
    {
        return valueCache;
    }

    /**
     * @return {@link #roomCache}
     */
    public RoomCache getRoomCache()
    {
        return roomCache;
    }

    /**
     * @return {@link #workingInterval}
     */
    public Interval getWorkingInterval()
    {
        return workingInterval;
    }

    /**
     * @return {@link #referenceDateTime}
     */
    public DateTime getReferenceDateTime()
    {
        if (referenceDateTime == null) {
            return DateTime.now();
        }
        return referenceDateTime;
    }

    /**
     * @param workingInterval sets the {@link #workingInterval}
     * @param entityManager   used for reloading allocations of resources for the new interval
     */
    public synchronized void setWorkingInterval(Interval workingInterval, EntityManager entityManager)
    {
        if (!workingInterval.equals(this.workingInterval)) {
            logger.debug("Setting new working interval '{}' to cache...",
                    Temporal.formatInterval(workingInterval));
            this.workingInterval = workingInterval;

            referenceDateTime = workingInterval.getStart();

            Interval resourceWorkingInterval = new Interval(
                    workingInterval.getStart().minus(resourceReservationMaximumDuration),
                    workingInterval.getEnd().plus(resourceReservationMaximumDuration));
            resourceCache.setWorkingInterval(resourceWorkingInterval, referenceDateTime, entityManager);
            roomCache.setWorkingInterval(resourceWorkingInterval, referenceDateTime, entityManager);

            Interval aliasWorkingInterval = new Interval(
                    workingInterval.getStart().minus(valueReservationMaximumDuration),
                    workingInterval.getEnd().plus(valueReservationMaximumDuration));
            valueCache.setWorkingInterval(aliasWorkingInterval, referenceDateTime, entityManager);

            Interval maxWorkingInterval = new Interval(
                    Temporal.min(resourceWorkingInterval.getStart(), aliasWorkingInterval.getStart()),
                    Temporal.max(resourceWorkingInterval.getEnd(), aliasWorkingInterval.getEnd()));
            reusedReservationCache.setWorkingInterval(maxWorkingInterval, referenceDateTime, entityManager);
        }
    }

    /**
     * Enable {@link #generateTestingIds}
     */
    protected void setGenerateTestingIds()
    {
        generateTestingIds = true;
    }

    @Override
    public synchronized void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public synchronized void init(Configuration configuration)
    {
        super.init(configuration);

        resourceReservationMaximumDuration = configuration.getPeriod(Configuration.RESERVATION_RESOURCE_MAX_DURATION);
        valueReservationMaximumDuration = configuration.getPeriod(Configuration.RESERVATION_VALUE_MAX_DURATION);

        logger.debug("Starting cache...");

        reset();
    }

    @Override
    public synchronized void destroy()
    {
        logger.debug("Stopping cache...");

        super.destroy();
    }

    /**
     * Reload cache from given {@code entityManager}.
     */
    public synchronized void reset()
    {
        resourceCache.clear();
        valueCache.clear();
        roomCache.clear();
        reusedReservationCache.clear();
        if (entityManagerFactory != null) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();

            logger.debug("Loading resources...");
            ResourceManager resourceManager = new ResourceManager(entityManager);
            List<Resource> resourceList = resourceManager.list(null, null);
            for (Resource resource : resourceList) {
                try {
                    addResource(resource, entityManager);
                }
                catch (Exception exception) {
                    throw new RuntimeException("Failed to add resource to the cache.", exception);
                }
            }
            reusedReservationCache.loadObjects(entityManager);
            entityManager.close();
        }
    }

    /**
     * @param persistentObject to be checked whether it is persisted (has {@link PersistentObject#id} filled)
     */
    private void checkPersisted(PersistentObject persistentObject)
    {
        if (!persistentObject.isPersisted()) {
            // For testing purposes assign testing id
            if (generateTestingIds) {
                persistentObject.generateTestingId();
                return;
            }
            // Throw error that object is not persisted
            persistentObject.checkPersisted();
        }
    }

    /**
     * @param resource
     * @param entityManager
     */
    public synchronized void addResource(Resource resource, EntityManager entityManager)
    {
        // Create resource in the database if it wasn't created yet
        if (entityManager != null && !resource.isPersisted()) {
            ResourceManager resourceManager = new ResourceManager(entityManager);
            resourceManager.create(resource);
        }

        // Add resource to resource cache
        checkPersisted(resource);
        resource.loadLazyCollections();
        resourceCache.addObject(resource, entityManager);

        // Add value provider
        ValueProviderCapability valueProviderCapability = resource.getCapability(ValueProviderCapability.class);
        if (valueProviderCapability != null) {
            ValueProvider valueProvider = valueProviderCapability.getValueProvider();
            checkPersisted(valueProvider);
            valueCache.addObject(valueProvider, entityManager);
        }

        // Add room provider capability
        RoomProviderCapability roomProviderCapability = resource.getCapability(RoomProviderCapability.class);
        if (roomProviderCapability != null) {
            checkPersisted(roomProviderCapability);
            roomCache.addObject(roomProviderCapability, entityManager);
        }

        // Process all alias providers in the resource
        List<AliasProviderCapability> aliasProviders = resource.getCapabilities(AliasProviderCapability.class);
        for (AliasProviderCapability aliasProvider : aliasProviders) {
            // Load lazy collections
            aliasProvider.loadLazyCollections();
            // Add alias provider to the set of existing alias providers
            checkPersisted(aliasProvider);
            aliasProviderById.put(aliasProvider.getId(), aliasProvider);

            // Add new value provider (but only when the alias provider owns the value provider)
            ValueProvider valueProvider = aliasProvider.getValueProvider().getTargetValueProvider();
            if (valueProvider.getCapability().equals(aliasProvider)) {
                checkPersisted(valueProvider);
                valueCache.addObject(valueProvider, entityManager);
            }
        }
    }

    /**
     * @see {@link #addResource(Resource, EntityManager)}
     */
    public synchronized void addResource(Resource resource)
    {
        addResource(resource, null);
    }

    /**
     * Update resource in the cache.
     *
     * @param resource
     * @param entityManager entity manager used for loading of resource allocations from the database
     */
    public synchronized void updateResource(Resource resource, EntityManager entityManager)
    {
        removeResource(resource);
        try {
            addResource(resource, entityManager);
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

        // Remove all value providers for the resource
        valueCache.removeValueProviders(resource);

        // Remove room provider for the resource
        roomCache.removeRoomProviderCapability(resource);

        // Process all alias providers in the resource
        List<AliasProviderCapability> aliasProviders = resource.getCapabilities(AliasProviderCapability.class);
        for (AliasProviderCapability aliasProvider : aliasProviders) {
            // Remove alias provider from the set of existing alias providers
            aliasProviderById.remove(aliasProvider.getId());
        }
    }

    /**
     * @param reservation to be added to the {@link Cache}
     */
    public synchronized void addReservation(Reservation reservation, EntityManager entityManager)
    {
        // Create reservation in the database if it wasn't created yet
        if (entityManager != null && !reservation.isPersisted()) {
            ReservationManager reservationManager = new ReservationManager(entityManager);
            reservationManager.create(reservation);
        }

        checkPersisted(reservation);
        if (reservation instanceof ExistingReservation) {
            ExistingReservation existingReservation = (ExistingReservation) reservation;
            reusedReservationCache.addReservation(existingReservation.getReservation(), existingReservation);
        }
        else if (reservation instanceof ResourceReservation) {
            ResourceReservation resourceReservation = (ResourceReservation) reservation;
            resourceCache.addReservation(resourceReservation.getResource(), resourceReservation);
        }
        else if (reservation instanceof ValueReservation) {
            ValueReservation valueReservation = (ValueReservation) reservation;
            valueCache.addReservation(valueReservation.getValueProvider(), valueReservation);
        }
        else if (reservation instanceof RoomReservation) {
            RoomReservation roomReservation = (RoomReservation) reservation;
            roomCache.addReservation(roomReservation.getRoomProviderCapability(), roomReservation);
        }

        // Add child reservations
        for (Reservation childReservation : reservation.getChildReservations()) {
            addReservation(childReservation, entityManager);
        }
    }

    /**
     * @see {@link #addResource(Resource, EntityManager)}
     */
    public synchronized void addReservation(Reservation reservation)
    {
        addReservation(reservation, null);
    }

    /**
     * @param reservation to be removed from the cache (and all child reservations)
     */
    public synchronized void removeReservation(Reservation reservation)
    {
        if (reservation instanceof ExistingReservation) {
            ExistingReservation existingReservation = (ExistingReservation) reservation;
            reusedReservationCache.removeReservation(existingReservation.getReservation(), existingReservation);
        }
        else if (reservation instanceof ResourceReservation) {
            ResourceReservation resourceReservation = (ResourceReservation) reservation;
            resourceCache.removeReservation(resourceReservation.getResource(), resourceReservation);
        }
        else if (reservation instanceof ValueReservation) {
            ValueReservation valueReservation = (ValueReservation) reservation;
            valueCache.removeReservation(valueReservation.getValueProvider(), valueReservation);
        }
        else if (reservation instanceof RoomReservation) {
            RoomReservation roomReservation = (RoomReservation) reservation;
            roomCache.removeReservation(roomReservation.getRoomProviderCapability(), roomReservation);
        }

        // Remove child reservations
        for (Reservation childReservation : reservation.getChildReservations()) {
            removeReservation(childReservation);
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
     * @see {@link ReusedReservationCache#isProvidedReservationAvailable(cz.cesnet.shongo.controller.reservation.Reservation, org.joda.time.Interval)}
     */
    public boolean isProvidedReservationAvailable(Reservation providedReservation, Interval interval)
    {
        return reusedReservationCache.isProvidedReservationAvailable(providedReservation, interval);
    }

}
