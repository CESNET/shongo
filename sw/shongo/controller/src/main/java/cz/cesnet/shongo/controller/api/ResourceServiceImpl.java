package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.cache.AvailableVirtualRoom;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Resource service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceServiceImpl extends Component
        implements ResourceService, Component.EntityManagerFactoryAware, Component.DomainAware
{
    /**
     * @see Cache
     */
    private Cache cache;

    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.Domain
     */
    private cz.cesnet.shongo.controller.Domain domain;

    /**
     * @param cache sets the {@link #cache}
     */
    public void setCache(Cache cache)
    {
        this.cache = cache;
    }

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void setDomain(cz.cesnet.shongo.controller.Domain domain)
    {
        this.domain = domain;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(cache, Cache.class);
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
        checkDependency(domain, cz.cesnet.shongo.controller.Domain.class);
        super.init(configuration);
    }

    @Override
    public String getServiceName()
    {
        return "Resource";
    }

    @Override
    public String createResource(SecurityToken token, Resource resource) throws FaultException
    {
        resource.setupNewEntity();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        // Create resource from API
        cz.cesnet.shongo.controller.resource.Resource resourceImpl =
                cz.cesnet.shongo.controller.resource.Resource.createFromApi(resource, entityManager, domain);

        // Save it
        ResourceManager resourceManager = new ResourceManager(entityManager);
        resourceManager.create(resourceImpl);

        entityManager.getTransaction().commit();

        // Add resource to the cache
        if (cache != null) {
            cache.addResource(resourceImpl, entityManager);
        }

        entityManager.close();

        // Return resource identifier
        return domain.formatIdentifier(resourceImpl.getId());
    }

    @Override
    public void modifyResource(SecurityToken token, Resource resource) throws FaultException
    {
        Long resourceId = domain.parseIdentifier(resource.getIdentifier());

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        ResourceManager resourceManager = new ResourceManager(entityManager);

        // Get reservation request
        cz.cesnet.shongo.controller.resource.Resource resourceImpl = resourceManager.get(resourceId);

        // Synchronize from API
        resourceImpl.fromApi(resource, entityManager, domain);

        resourceManager.update(resourceImpl);

        entityManager.getTransaction().commit();

        // Update resource in the cache
        if (cache != null) {
            cache.updateResource(resourceImpl, entityManager);
        }

        entityManager.close();
    }

    @Override
    public void deleteResource(SecurityToken token, String resourceIdentifier) throws EntityNotFoundException
    {
        Long resourceId = domain.parseIdentifier(resourceIdentifier);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        ResourceManager resourceManager = new ResourceManager(entityManager);

        // Get the resource
        cz.cesnet.shongo.controller.resource.Resource resourceImpl = resourceManager.get(resourceId);

        // Delete the resource
        resourceManager.delete(resourceImpl);

        // Remove resource from the cache
        if (cache != null) {
            cache.removeResource(resourceImpl);
        }

        entityManager.getTransaction().commit();
        entityManager.close();
    }

    @Override
    public Collection<ResourceSummary> listResources(SecurityToken token)
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);

        List<cz.cesnet.shongo.controller.resource.Resource> list = resourceManager.list();
        List<ResourceSummary> summaryList = new ArrayList<ResourceSummary>();
        for (cz.cesnet.shongo.controller.resource.Resource resource : list) {
            ResourceSummary summary = new ResourceSummary();
            summary.setIdentifier(domain.formatIdentifier(resource.getId()));
            summary.setName(resource.getName());
            if (resource instanceof DeviceResource) {
                StringBuilder stringBuilder = new StringBuilder();
                for (Technology technology : ((DeviceResource) resource).getTechnologies()) {
                    if (stringBuilder.length() > 0) {
                        stringBuilder.append(",");
                    }
                    stringBuilder.append(technology.getCode());
                }
                summary.setTechnologies(stringBuilder.toString());
            }
            cz.cesnet.shongo.controller.resource.Resource parentResource = resource.getParentResource();
            if (parentResource != null) {
                summary.setParentIdentifier(domain.formatIdentifier(parentResource.getId()));
            }
            summaryList.add(summary);
        }

        entityManager.close();

        return summaryList;
    }

    @Override
    public Resource getResource(SecurityToken token, String resourceIdentifier) throws EntityNotFoundException
    {
        Long resourceId = domain.parseIdentifier(resourceIdentifier);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);

        cz.cesnet.shongo.controller.resource.Resource resourceImpl = resourceManager.get(resourceId);
        Resource resourceApi = resourceImpl.toApi(entityManager, domain);

        entityManager.close();

        return resourceApi;
    }

    @Override
    public ResourceAllocation getResourceAllocation(SecurityToken token, String resourceIdentifier, Interval interval)
            throws EntityNotFoundException
    {
        Long resourceId = domain.parseIdentifier(resourceIdentifier);
        if (interval == null) {
            interval = cache.getWorkingInterval();
            if (interval == null) {
                interval = new Interval(DateTime.now(), Period.days(31));
            }
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);

        cz.cesnet.shongo.controller.resource.Resource resourceImpl = resourceManager.get(resourceId);
        cz.cesnet.shongo.controller.resource.VirtualRoomsCapability virtualRoomsCapability =
                resourceImpl.getCapability(cz.cesnet.shongo.controller.resource.VirtualRoomsCapability.class);

        // Setup resource allocation
        ResourceAllocation resourceAllocation = null;
        if (resourceImpl instanceof DeviceResource && virtualRoomsCapability != null) {
            AvailableVirtualRoom availableVirtualRoom =
                    cache.getResourceCache().getAvailableVirtualRoom(
                            (cz.cesnet.shongo.controller.resource.DeviceResource) resourceImpl, interval);
            VirtualRoomsResourceAllocation allocation = new VirtualRoomsResourceAllocation();
            allocation.setMaximumPortCount(availableVirtualRoom.getMaximumPortCount());
            allocation.setAvailablePortCount(availableVirtualRoom.getAvailablePortCount());
            resourceAllocation = allocation;
        }
        else {
            resourceAllocation = new ResourceAllocation();
        }
        resourceAllocation.setIdentifier(domain.formatIdentifier(resourceId));
        resourceAllocation.setName(resourceImpl.getName());
        resourceAllocation.setInterval(interval);

        // Fill resource allocations
        Collection<cz.cesnet.shongo.controller.allocationaold.AllocatedResource> resourceAllocations =
                resourceManager.listAllocatedResourcesInInterval(resourceId, interval);
        for (cz.cesnet.shongo.controller.allocationaold.AllocatedResource allocatedResourceImpl : resourceAllocations) {
            resourceAllocation.addAllocation(allocatedResourceImpl.toApi(domain));
        }

        // Fill alias allocations
        List<cz.cesnet.shongo.controller.resource.AliasProviderCapability> aliasProviders =
                resourceImpl.getCapabilities(cz.cesnet.shongo.controller.resource.AliasProviderCapability.class);
        for (cz.cesnet.shongo.controller.resource.AliasProviderCapability aliasProvider : aliasProviders) {
            List<cz.cesnet.shongo.controller.allocationaold.AllocatedAlias> allocatedAliasImpls =
                    resourceManager.listAllocatedAliasesInInterval(aliasProvider.getId(), interval);
            for (cz.cesnet.shongo.controller.allocationaold.AllocatedAlias allocatedAliasImpl : allocatedAliasImpls) {
                resourceAllocation.addAllocation(allocatedAliasImpl.toApi(domain));
            }
        }

        return resourceAllocation;
    }
}
