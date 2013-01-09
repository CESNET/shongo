package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.cache.*;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.reservation.*;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import cz.cesnet.shongo.util.TemporalHelper;
import org.joda.time.DateTime;
import org.joda.time.Interval;
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
     * Maximum duration of a {@link ResourceReservation}.
     */
    private Period resourceReservationMaximumDuration;

    /**
     * Maximum duration of a {@link AliasReservation}.
     */
    private Period aliasReservationMaximumDuration;

    /**
     * @see ResourceCache
     */
    private ResourceCache resourceCache = new ResourceCache();

    /**
     * @see AliasCache
     */
    private AliasCache aliasCache = new AliasCache();

    /**
     * @see ReusedReservationCache
     */
    private ReusedReservationCache reusedReservationCache = new ReusedReservationCache();

    /**
     * Working interval for which are loaded allocated virtual rooms.
     */
    private Interval workingInterval;

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
     * @return {@link #aliasReservationMaximumDuration}
     */
    public Period getAliasReservationMaximumDuration()
    {
        return aliasReservationMaximumDuration;
    }

    /**
     * @return {@link #resourceCache}
     */
    public ResourceCache getResourceCache()
    {
        return resourceCache;
    }

    /**
     * @return {@link #aliasCache}
     */
    public AliasCache getAliasCache()
    {
        return aliasCache;
    }

    /**
     * @return {@link #workingInterval}
     */
    public Interval getWorkingInterval()
    {
        return workingInterval;
    }

    /**
     * @param workingInterval sets the {@link #workingInterval}
     * @param entityManager   used for reloading allocations of resources for the new interval
     */
    public void setWorkingInterval(Interval workingInterval, EntityManager entityManager)
    {
        if (!workingInterval.equals(this.workingInterval)) {
            logger.info("Setting new working interval '{}' to cache...",
                    TemporalHelper.formatInterval(workingInterval));
            this.workingInterval = workingInterval;

            DateTime referenceDateTime = workingInterval.getStart();

            Interval resourceWorkingInterval = new Interval(
                    workingInterval.getStart().minus(resourceReservationMaximumDuration),
                    workingInterval.getEnd().plus(resourceReservationMaximumDuration));
            resourceCache.setWorkingInterval(resourceWorkingInterval, referenceDateTime, entityManager);

            Interval aliasWorkingInterval = new Interval(
                    workingInterval.getStart().minus(aliasReservationMaximumDuration),
                    workingInterval.getEnd().plus(aliasReservationMaximumDuration));
            aliasCache.setWorkingInterval(aliasWorkingInterval, referenceDateTime, entityManager);

            Interval maxWorkingInterval = new Interval(
                    TemporalHelper.min(resourceWorkingInterval.getStart(), aliasWorkingInterval.getStart()),
                    TemporalHelper.max(resourceWorkingInterval.getEnd(), aliasWorkingInterval.getEnd()));
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
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void init(Configuration configuration)
    {
        super.init(configuration);

        resourceReservationMaximumDuration = configuration.getPeriod(Configuration.RESERVATION_RESOURCE_MAX_DURATION);
        aliasReservationMaximumDuration = configuration.getPeriod(Configuration.RESERVATION_ALIAS_MAX_DURATION);

        logger.debug("Starting cache...");

        reset();
    }

    @Override
    public void destroy()
    {
        logger.debug("Stopping cache...");

        super.destroy();
    }

    /**
     * Reload cache from given {@code entityManager}.
     */
    public void reset()
    {
        resourceCache.clear();
        aliasCache.clear();
        if (entityManagerFactory != null) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            resourceCache.loadObjects(entityManager);
            aliasCache.loadObjects(entityManager);
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
     * @throws FaultException when the creating in the database fails
     */
    public void addResource(Resource resource, EntityManager entityManager) throws FaultException
    {
        // Create resource in the database if it wasn't created yet
        if (entityManager != null && !resource.isPersisted()) {
            ResourceManager resourceManager = new ResourceManager(entityManager);
            resourceManager.create(resource);
        }

        // Add resource to resource cache
        checkPersisted(resource);
        resourceCache.addObject(resource, entityManager);

        // Add all alias provider capabilities to alias manager
        List<AliasProviderCapability> aliasProviderCapabilities =
                resource.getCapabilities(AliasProviderCapability.class);
        for (AliasProviderCapability aliasProviderCapability : aliasProviderCapabilities) {
            checkPersisted(aliasProviderCapability);
            aliasCache.addObject(aliasProviderCapability, entityManager);
        }
    }

    /**
     * @see {@link #addResource(Resource, EntityManager)}
     */
    public void addResource(Resource resource) throws FaultException
    {
        addResource(resource, null);
    }

    /**
     * Update resource in the cache.
     *
     * @param resource
     * @param entityManager entity manager used for loading of resource allocations from the database
     */
    public void updateResource(Resource resource, EntityManager entityManager)
    {
        removeResource(resource);
        try {
            addResource(resource, entityManager);
        }
        catch (FaultException exception) {
            throw new IllegalStateException("Failed to update resource in the resource cache.", exception);
        }
    }

    /**
     * Remove resource from the {@link Cache}.
     *
     * @param resource to be removed
     */
    public void removeResource(Resource resource)
    {
        // Remove resource from resource cache
        resourceCache.removeObject(resource);

        // Remove all alias providers for the resource
        aliasCache.removeAliasProviders(resource);
    }

    /**
     * @param reservation to be added to the {@link Cache}
     */
    public void addReservation(Reservation reservation, EntityManager entityManager)
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
        else if (reservation instanceof AliasReservation) {
            AliasReservation aliasReservation = (AliasReservation) reservation;
            aliasCache.addReservation(aliasReservation.getAliasProviderCapability(), aliasReservation);
        }
    }

    /**
     * @see {@link #addResource(Resource, EntityManager)}
     */
    public void addReservation(Reservation reservation)
    {
        addReservation(reservation, null);
    }

    /**
     * @param reservation to be removed from the cache
     */
    public void removeReservation(Reservation reservation)
    {
        if (reservation instanceof ExistingReservation) {
            ExistingReservation existingReservation = (ExistingReservation) reservation;
            reusedReservationCache.removeReservation(existingReservation.getReservation(), existingReservation);
        }
        else if (reservation instanceof ResourceReservation) {
            ResourceReservation resourceReservation = (ResourceReservation) reservation;
            resourceCache.removeReservation(resourceReservation.getResource(), resourceReservation);
        }
        else if (reservation instanceof AliasReservation) {
            AliasReservation aliasReservation = (AliasReservation) reservation;
            aliasCache.removeReservation(aliasReservation.getAliasProviderCapability(), aliasReservation);
        }
    }

    /**
     * Checks whether given {@code resource} and all it's dependent resource are available.
     * Device resources with {@link cz.cesnet.shongo.controller.resource.RoomProviderCapability} are always available (if theirs capacity is fully used).
     *
     * @param resource
     * @param interval
     * @return true if given {@code resource} is available, false otherwise
     */
    public boolean isResourceAvailable(Resource resource, Interval interval, Transaction transaction)
    {
        return resourceCache.isResourceAvailable(resource, interval, transaction.getResourceCacheTransaction())
                && resourceCache.isDependentResourcesAvailable(resource, interval,
                transaction.getResourceCacheTransaction());
    }

    /**
     * @param interval
     * @param technologies
     * @param transaction
     * @return list of available terminals
     */
    public List<DeviceResource> findAvailableTerminal(Interval interval, Set<Technology> technologies,
            Transaction transaction)
    {
        Set<Long> terminals = resourceCache.getDeviceResourcesByCapabilityTechnologies(TerminalCapability.class,
                technologies);

        List<DeviceResource> deviceResources = new ArrayList<DeviceResource>();
        for (Long terminalId : terminals) {
            DeviceResource deviceResource = (DeviceResource) resourceCache.getObject(terminalId);
            if (deviceResource == null) {
                throw new IllegalStateException("Device resource should be added to the cache.");
            }
            if (isResourceAvailable(deviceResource, interval, transaction)) {
                deviceResources.add(deviceResource);
            }
        }
        return deviceResources;
    }

    /**
     * Find {@link cz.cesnet.shongo.controller.cache.AvailableRoom}s in given {@code interval} which have
     * at least {@code requiredLicenseCount} available licenses and which supports given {@code technologies}.
     *
     * @param interval
     * @param requiredLicenseCount
     * @param technologies
     * @return list of {@link cz.cesnet.shongo.controller.cache.AvailableRoom}
     */
    public List<AvailableRoom> findAvailableRooms(Interval interval, int requiredLicenseCount,
            Set<Technology> technologies, Transaction transaction)
    {
        Set<Long> deviceResourceIds = resourceCache.getDeviceResourcesByCapabilityTechnologies(
                RoomProviderCapability.class,
                technologies);
        List<AvailableRoom> availableRooms = new ArrayList<AvailableRoom>();
        for (Long deviceResourceId : deviceResourceIds) {
            DeviceResource deviceResource = (DeviceResource) resourceCache.getObject(deviceResourceId);
            AvailableRoom availableRoom = resourceCache.getAvailableRoom(deviceResource,
                    interval, (transaction != null ? transaction.getResourceCacheTransaction() : null));
            if (availableRoom.getAvailableLicenseCount() >= requiredLicenseCount) {
                availableRooms.add(availableRoom);
            }
        }
        return availableRooms;
    }

    /**
     * @see {@link #findAvailableRooms}
     */
    public List<AvailableRoom> findAvailableRooms(Interval interval, int requiredLicenseCount,
            Technology[] technologies, Transaction transaction)
    {
        Set<Technology> technologySet = new HashSet<Technology>();
        Collections.addAll(technologySet, technologies);
        return findAvailableRooms(interval, requiredLicenseCount, technologySet, transaction);
    }

    /**
     * @see {@link #findAvailableRooms}
     */
    public List<AvailableRoom> findAvailableRooms(Interval interval, int requiredLicenseCount,
            Transaction transaction)
    {
        return findAvailableRooms(interval, requiredLicenseCount, (Set<Technology>) null, transaction);
    }

    /**
     * @see {@link ReusedReservationCache#isProvidedReservationAvailable(cz.cesnet.shongo.controller.reservation.Reservation, org.joda.time.Interval)}
     */
    public boolean isProvidedReservationAvailable(Reservation providedReservation, Interval interval)
    {
        return reusedReservationCache.isProvidedReservationAvailable(providedReservation, interval);
    }

    /**
     * Transaction for the {@link Cache}.
     */
    public static class Transaction
    {
        /**
         * Interval for which the task is performed.
         */
        private final Interval interval;

        /**
         * @see {@link ResourceCache.Transaction}
         */
        private ResourceCache.Transaction resourceCacheTransaction = new ResourceCache.Transaction();

        /**
         * @see {@link AliasCache.Transaction}
         */
        private AliasCache.Transaction aliasCacheTransaction = new AliasCache.Transaction();

        /**
         * Map of provided {@link Executable}s by {@link Reservation} which allocates them.
         */
        private Map<Executable, Reservation> providedReservationByExecutable = new HashMap<Executable, Reservation>();

        /**
         * Constructor.
         */
        public Transaction(Interval interval)
        {
            this.interval = interval;
        }

        /**
         * @return {@link #interval}
         */
        public Interval getInterval()
        {
            return interval;
        }

        /**
         * @param resource to be added to the {@link ResourceCache.Transaction#referencedResources}
         */
        public void addReferencedResource(Resource resource)
        {
            resourceCacheTransaction.addReferencedResource(resource);
        }

        /**
         * @param reservation to be added to the {@link Transaction} as already allocated.
         */
        public void addAllocatedReservation(Reservation reservation)
        {
            if (reservation.getSlot().contains(getInterval())) {
                if (reservation instanceof ResourceReservation) {
                    ResourceReservation resourceReservation = (ResourceReservation) reservation;
                    Resource resource = resourceReservation.getResource();
                    resourceCacheTransaction.addAllocatedReservation(resource.getId(), resourceReservation);
                    addReferencedResource(resource);
                }
                else if (reservation instanceof AliasReservation) {
                    AliasReservation aliasReservation = (AliasReservation) reservation;
                    aliasCacheTransaction.addAllocatedReservation(
                            aliasReservation.getAliasProviderCapability().getId(), aliasReservation);
                }
            }
        }

        /**
         * @param reservation to be added to the {@link Transaction} as provided (the resources allocated by
         *                    the {@code reservation} are considered as available).
         */
        public void addProvidedReservation(Reservation reservation)
        {
            if (reservation.getSlot().contains(getInterval())) {
                Executable executable = reservation.getExecutable();
                if (executable != null) {
                    providedReservationByExecutable.put(executable, reservation);
                }
                if (reservation instanceof ExistingReservation) {
                    throw new TodoImplementException("Providing already provided reservation is not implemented yet.");
                    // It will be necessary to evaluate existing reservation to target reservation and to keep the
                    // reference to the existing reservation.
                    // In the end the existing reservation must be used as child reservation for any newly allocated
                    // reservations and not the target reservation itself (a collision would occur).
                }
                if (reservation instanceof ResourceReservation) {
                    ResourceReservation resourceReservation = (ResourceReservation) reservation;
                    resourceCacheTransaction.addProvidedReservation(
                            resourceReservation.getResource().getId(), resourceReservation);
                }
                else if (reservation instanceof AliasReservation) {
                    AliasReservation aliasReservation = (AliasReservation) reservation;
                    aliasCacheTransaction.addProvidedReservation(
                            aliasReservation.getAliasProviderCapability().getId(), aliasReservation);
                }
            }

            // Add all child reservations
            for (Reservation childReservation : reservation.getChildReservations()) {
                addProvidedReservation(childReservation);
            }
        }

        /**
         * @param reservation to be removed from the provided {@link Reservation}s from the {@link Transaction}
         */
        public void removeProvidedReservation(Reservation reservation)
        {
            Executable executable = reservation.getExecutable();
            if (executable != null) {
                providedReservationByExecutable.remove(executable);
            }
            if (reservation instanceof ExistingReservation) {
                throw new TodoImplementException("Providing already provided reservation is not implemented yet.");
            }
            if (reservation instanceof ResourceReservation) {
                ResourceReservation resourceReservation = (ResourceReservation) reservation;
                resourceCacheTransaction.removeProvidedReservation(
                        resourceReservation.getResource().getId(), resourceReservation);
            }
            else if (reservation instanceof AliasReservation) {
                AliasReservation aliasReservation = (AliasReservation) reservation;
                aliasCacheTransaction.removeProvidedReservation(
                        aliasReservation.getAliasProviderCapability().getId(), aliasReservation);
            }
        }

        /**
         * @param executableType
         * @return collection of provided {@link Executable}s of given {@code executableType}
         */
        public <T extends Executable> Collection<T> getProvidedExecutables(Class<T> executableType)
        {
            Set<T> providedExecutables = new HashSet<T>();
            for (Executable providedExecutable : providedReservationByExecutable.keySet()) {
                if (executableType.isInstance(providedExecutable)) {
                    providedExecutables.add(executableType.cast(providedExecutable));
                }
            }
            return providedExecutables;
        }

        /**
         * @param executable
         * @return provided {@link Reservation} for given {@code executable}
         */
        public Reservation getProvidedReservationByExecutable(Executable executable)
        {
            Reservation providedReservation = providedReservationByExecutable.get(executable);
            if (providedReservation == null) {
                throw new IllegalArgumentException("Provided reservation doesn't exists for given executable!");
            }
            return providedReservation;
        }

        /**
         * @return {@link #resourceCacheTransaction}
         */
        public ResourceCache.Transaction getResourceCacheTransaction()
        {
            return resourceCacheTransaction;
        }

        /**
         * @param resource for which the provided {@link ResourceReservation}s should be returned
         * @return provided {@link ResourceReservation}s for given {@code resource}
         */
        public Set<ResourceReservation> getProvidedResourceReservations(Resource resource)
        {
            return resourceCacheTransaction.getProvidedReservations(resource.getId());
        }

        /**
         * @return {@link #aliasCacheTransaction}
         */
        public AliasCache.Transaction getAliasCacheTransaction()
        {
            return aliasCacheTransaction;
        }

        /**
         * @param resource to be checked
         * @return true if given resource was referenced by any {@link ResourceReservation} added to the transaction,
         *         false otherwise
         */
        public boolean containsResource(Resource resource)
        {
            return resourceCacheTransaction.containsResource(resource);
        }
    }
}
