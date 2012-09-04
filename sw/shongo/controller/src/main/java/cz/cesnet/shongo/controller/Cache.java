package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.allocation.AllocatedAlias;
import cz.cesnet.shongo.controller.allocation.AllocatedItem;
import cz.cesnet.shongo.controller.allocation.AllocatedResource;
import cz.cesnet.shongo.controller.cache.AliasCache;
import cz.cesnet.shongo.controller.cache.AvailableAlias;
import cz.cesnet.shongo.controller.cache.AvailableVirtualRoom;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.util.TemporalHelper;
import org.joda.time.Duration;
import org.joda.time.Interval;
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
     * Maximum duration of a resource allocation.
     */
    private Duration allocatedResourceMaximumDuration = Duration.standardDays(1);

    /**
     * Maximum duration of a alias allocation.
     */
    private Duration allocatedAliasMaximumDuration = Duration.standardDays(365);

    /**
     * @see {@link ResourceCache}
     */
    private ResourceCache resourceCache = new ResourceCache();

    /**
     * @see {@link AliasCache}
     */
    private AliasCache aliasCache = new AliasCache();

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
     * @return {@link #allocatedResourceMaximumDuration}
     */
    public Duration getAllocatedResourceMaximumDuration()
    {
        return allocatedResourceMaximumDuration;
    }

    /**
     * @param maximumDuration sets the {@link #allocatedResourceMaximumDuration}
     */
    public void setAllocatedResourceMaximumDuration(Duration maximumDuration)
    {
        this.allocatedResourceMaximumDuration = maximumDuration;
    }

    /**
     * @return {@link #allocatedAliasMaximumDuration}
     */
    public Duration getAllocatedAliasMaximumDuration()
    {
        return allocatedAliasMaximumDuration;
    }

    /**
     * @param maximumDuration sets the {@link #allocatedAliasMaximumDuration}
     */
    public void setAllocatedAliasMaximumDuration(Duration maximumDuration)
    {
        this.allocatedAliasMaximumDuration = maximumDuration;
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

            Interval resourceWorkingInterval = new Interval(
                    workingInterval.getStart().minus(allocatedResourceMaximumDuration),
                    workingInterval.getEnd().plus(allocatedResourceMaximumDuration));
            resourceCache.setWorkingInterval(resourceWorkingInterval, entityManager);

            Interval aliasWorkingInterval = new Interval(
                    workingInterval.getStart().minus(allocatedAliasMaximumDuration),
                    workingInterval.getEnd().plus(allocatedAliasMaximumDuration));
            aliasCache.setWorkingInterval(aliasWorkingInterval, entityManager);
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

        logger.debug("Starting cache...");

        Duration resourceMaxDuration = configuration.getDuration(Configuration.ALLOCATION_RESOURCE_MAX_DURATION);
        if (resourceMaxDuration != null) {
            setAllocatedResourceMaximumDuration(resourceMaxDuration);
        }
        Duration aliasMaxDuration = configuration.getDuration(Configuration.ALLOCATION_ALIAS_MAX_DURATION);
        if (aliasMaxDuration != null) {
            setAllocatedAliasMaximumDuration(aliasMaxDuration);
        }

        if (entityManagerFactory != null) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            resourceCache.loadResources(entityManager);
        }
    }

    @Override
    public void destroy()
    {
        logger.debug("Stopping cache...");

        super.destroy();
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
        checkPersisted(resource);

        // Add resource to resource cache
        resourceCache.addResource(resource, entityManager);

        // Add all alias provider capabilities to alias manager
        List<AliasProviderCapability> aliasProviderCapabilities =
                resource.getCapabilities(AliasProviderCapability.class);
        for (AliasProviderCapability aliasProviderCapability : aliasProviderCapabilities) {
            aliasCache.addAliasProvider(aliasProviderCapability);
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
     * Update resource in the resource database.
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
            throw new IllegalStateException("Failed to update resource in the resource database.", exception);
        }
    }

    public void removeResource(Resource resource)
    {
        // Remove resource from resource cache
        resourceCache.removeResource(resource);

        // Remove all alias providers for the resource
        aliasCache.removeAliasProviders(resource);
    }

    /**
     * @param allocatedItem to be added to the resource database
     */
    public void addAllocatedItem(AllocatedItem allocatedItem)
    {
        checkPersisted(allocatedItem);
        if (allocatedItem instanceof AllocatedResource) {
            resourceCache.addAllocatedResource((AllocatedResource) allocatedItem);
        }
        if (allocatedItem instanceof AllocatedAlias) {
            aliasCache.addAllocatedAlias((AllocatedAlias) allocatedItem);
        }
    }

    /**
     * @param allocatedItem to be removed from the resource database
     */
    public void removeAllocatedItem(AllocatedItem allocatedItem)
    {
        if (allocatedItem instanceof AllocatedResource) {
            resourceCache.removeAllocatedResource((AllocatedResource) allocatedItem);
        }
        if (allocatedItem instanceof AllocatedAlias) {
            aliasCache.removeAllocatedAlias((AllocatedAlias) allocatedItem);
        }
    }

    /**
     * Checks whether given {@code resource} and all it's dependent resource are available.
     * Device resources with {@link cz.cesnet.shongo.controller.resource.VirtualRoomsCapability} are always available (if theirs capacity is fully used).
     *
     * @param resource
     * @param interval
     * @return true if given {@code resource} is available, false otherwise
     */
    public boolean isResourceAvailable(Resource resource, Interval interval)
    {
        // Get top parent resource and checks whether it is available
        Resource parentResource = resource;
        while (parentResource.getParentResource() != null) {
            parentResource = parentResource.getParentResource();
        }
        return resourceCache.isResourceAndChildResourcesAvailableRecursive(parentResource, interval);
    }

    /**
     * @param interval
     * @param technologies
     * @return list of available terminals
     */
    public List<DeviceResource> findAvailableTerminal(Interval interval, Set<Technology> technologies)
    {
        Set<Long> terminals = resourceCache.getDeviceResourcesByCapabilityTechnologies(TerminalCapability.class,
                technologies);

        List<DeviceResource> deviceResources = new ArrayList<DeviceResource>();
        for (Long terminalId : terminals) {
            DeviceResource deviceResource = (DeviceResource) resourceCache.getResource(terminalId);
            if (deviceResource == null) {
                throw new IllegalStateException("Device resource should be added to the resource database.");
            }
            if (isResourceAvailable(deviceResource, interval)) {
                deviceResources.add(deviceResource);
            }
        }
        return deviceResources;
    }

    /**
     * Find {@link cz.cesnet.shongo.controller.cache.AvailableVirtualRoom}s in given {@code interval} which have at least {@code requiredPortCount}
     * available ports and which supports given {@code technologies}.
     *
     * @param interval
     * @param requiredPortCount
     * @param technologies
     * @return list of {@link cz.cesnet.shongo.controller.cache.AvailableVirtualRoom}
     */
    public List<AvailableVirtualRoom> findAvailableVirtualRooms(Interval interval, int requiredPortCount,
            Set<Technology> technologies)
    {
        Set<Long> deviceResources = resourceCache.getDeviceResourcesByCapabilityTechnologies(
                VirtualRoomsCapability.class,
                technologies);
        return resourceCache.findAvailableVirtualRoomsInDeviceResources(interval, requiredPortCount, deviceResources);
    }

    /**
     * @see {@link #findAvailableVirtualRooms}
     */
    public List<AvailableVirtualRoom> findAvailableVirtualRooms(Interval interval, int requiredPortCount,
            Technology[] technologies)
    {
        Set<Technology> technologySet = new HashSet<Technology>();
        Collections.addAll(technologySet, technologies);
        return findAvailableVirtualRooms(interval, requiredPortCount, technologySet);
    }

    /**
     * @see {@link #findAvailableVirtualRooms}
     */
    public List<AvailableVirtualRoom> findAvailableVirtualRooms(Interval interval, int requiredPortCount)
    {
        return findAvailableVirtualRooms(interval, requiredPortCount, (Set<Technology>) null);
    }

    /**
     * Find {@link cz.cesnet.shongo.controller.cache.AvailableVirtualRoom}s in given {@code interval} which have at least {@code requiredPortCount}
     * available ports and which supports given {@code technologies}.
     *
     * @param interval
     * @param requiredPortCount
     * @param technologiesVariants
     * @return list of {@link cz.cesnet.shongo.controller.cache.AvailableVirtualRoom}
     */
    public List<AvailableVirtualRoom> findAvailableVirtualRoomsByVariants(Interval interval, int requiredPortCount,
            Collection<Set<Technology>> technologiesVariants)
    {
        Set<Long> deviceResources = resourceCache.getDeviceResourcesByCapabilityTechnologies(
                VirtualRoomsCapability.class,
                technologiesVariants);
        return resourceCache.findAvailableVirtualRoomsInDeviceResources(interval, requiredPortCount, deviceResources);
    }

    /**
     * Find available alias in given {@code aliasProviderCapability}.
     *
     * @param aliasProviderCapability
     * @param interval
     * @return available alias for given {@code interval} from given {@code aliasProviderCapability}
     */
    public AvailableAlias getAvailableAlias(AliasProviderCapability aliasProviderCapability, Interval interval)
    {
        Resource resource = aliasProviderCapability.getResource();
        Long resourceId = resource.getId();

        // todo:

        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    /**
     * Find available alias in all resources in the database.
     *
     * @param technology
     * @param interval
     * @return available alias for given {@code technology} and {@code interval}
     */
    public AvailableAlias getAvailableAlias(Technology technology, Interval interval)
    {
        // todo:

        return null;  //To change body of created methods use File | Settings | File Templates.
    }
}
